import base64
import json
import unittest
from unittest.mock import patch
from flask import Flask, request
import main
from test_document_analysis import sample
from test_document_input import synthetic_pdf, payload

class FirebaseEntryTests(unittest.TestCase):
    def setUp(self):
        # Tests must never consult live Firestore, Play or a model.
        db = patch.object(main.firestore, "client")
        db.start()
        self.addCleanup(db.stop)
        status = patch.object(main.play_billing, "status", return_value={"premium": True})
        self.premium = status.start()
        self.addCleanup(status.stop)

    def invoke(self, path, token=None, **kwargs):
        headers = {"Authorization": "Bearer "+token} if token else {}
        with Flask(__name__).test_request_context(path, method="POST", headers=headers, **kwargs):
            return main.bio_api(request)

    def test_anonymous_never_invokes_model(self):
        with patch.object(main, "prepare") as prepare:
            self.assertEqual(self.invoke('/analyze-document',json={}).status_code,401)
            prepare.assert_not_called()

    def test_invalid_token_never_invokes_model(self):
        with patch.object(main.auth, "verify_id_token", side_effect=ValueError), patch.object(main, "prepare") as prepare:
            self.assertEqual(self.invoke('/research','bad',json={}).status_code,401)
            prepare.assert_not_called()

    def test_quota_denies_before_provider(self):
        with patch.object(main.auth,'verify_id_token',return_value={'uid':'test'}), patch.object(main.firestore,'client'), patch.object(main,'reserve_quota',return_value=False), patch.object(main.genai,'Client') as client:
            r=self.invoke('/research','token',json={'query':'Pregunta de prueba'})
            self.assertEqual(r.status_code,429)
            client.assert_not_called()

    def test_revocation_checked(self):
        with patch.object(main.auth,'verify_id_token',return_value={'uid':'test'}) as verify:
            self.assertEqual(self.invoke('/research','token',data='text',content_type='text/plain').status_code,415)
            verify.assert_called_once_with('token',check_revoked=True)

    def test_failed_provider_returns_allowance(self):
        ticket = {'id': 'ticket', 'month': '2026-10', 'kind': 'research'}
        with patch.object(main.auth, 'verify_id_token', return_value={'uid': 'test'}), \
             patch.object(main, 'reserve_quota', return_value=ticket), \
             patch.object(main.premium_quota, 'refund') as refund, \
             patch.object(main.prompt_budget, 'check'), \
             patch.object(main.genai, 'Client') as factory:
            factory.return_value.__enter__.return_value.models.generate_content.side_effect = TimeoutError
            self.assertEqual(self.invoke('/research', 'token', json={'query': 'Prueba'}).status_code, 502)
            refund.assert_called_once_with(main.firestore.client(), 'test', ticket)

    def test_failure_diagnostics_exclude_clinical_data_and_credentials(self):
        with patch.object(main.auth, 'verify_id_token', return_value={'uid': 'private-uid'}), \
             patch.object(main, 'reserve_quota', return_value={'id': 'ticket'}), \
             patch.object(main.premium_quota, 'refund'), \
             patch.object(main.prompt_budget, 'check', side_effect=ValueError('SECRET patient and token')), \
             patch.object(main.genai, 'Client'), patch('builtins.print') as log:
            response = self.invoke('/research', 'secret-token', json={'query': 'Private patient'})
            self.assertEqual(response.status_code, 502)
            fields = json.loads(log.call_args.args[0])
            self.assertEqual(fields, {'severity': 'ERROR', 'event': 'bio_analysis_failed',
                                     'stage': 'input_size', 'operation': 'research', 'providerCode': None})
            self.assertNotIn('SECRET', response.get_data(as_text=True))

    def test_excess_input_returns_allowance_before_generation(self):
        ticket = {'id': 'ticket', 'month': '2026-10', 'kind': 'research'}
        with patch.object(main.auth, 'verify_id_token', return_value={'uid': 'test'}), \
             patch.object(main, 'reserve_quota', return_value=ticket), \
             patch.object(main.premium_quota, 'refund') as refund, \
             patch.object(main.genai, 'Client') as factory:
            client = factory.return_value.__enter__.return_value
            client.models.count_tokens.return_value.total_tokens = 20001
            response = self.invoke('/research', 'token', json={'query': 'Prueba'})
            self.assertEqual(response.status_code, 413)
            self.assertEqual(response.get_json()['code'], 'input_too_large')
            client.models.generate_content.assert_not_called()
            refund.assert_called_once()

    def test_invalid_file_rejected_before_quota(self):
        with patch.object(main.auth,'verify_id_token',return_value={'uid':'test'}), patch.object(main,'reserve_quota') as quota:
            r=self.invoke('/analyze-document','token',json={'document':{'mime':'application/pdf','data':'ZmFrZQ=='}})
            self.assertEqual(r.status_code,400)
            quota.assert_not_called()

    def test_protected_or_broken_pdf_never_reserves_quota_or_calls_provider(self):
        with patch.object(main.auth, 'verify_id_token', return_value={'uid': 'test'}), \
             patch.object(main, 'reserve_quota') as quota, \
             patch.object(main.genai, 'Client') as client:
            for raw, code in ((synthetic_pdf(encrypted=True), 'document_password_required'),
                              (synthetic_pdf(empty=True), 'document_unreadable'),
                              (b'%PDF-1.4 broken SECRET', 'document_unreadable')):
                response = self.invoke('/analyze-document', 'token', json=payload(raw))
                self.assertEqual(response.status_code, 422)
                self.assertEqual(response.get_json()['code'], code)
                self.assertNotIn('SECRET', response.get_data(as_text=True))
            quota.assert_not_called()
            client.assert_not_called()

    def test_normal_and_billing_failure_never_call_model(self):
        with patch.object(main.auth, "verify_id_token", return_value={"uid": "test"}), patch.object(main.genai, "Client") as client:
            self.premium.return_value = {"premium": False}
            self.assertEqual(self.invoke("/research", "token", json={"query": "Consulta"}).status_code, 402)
            self.premium.side_effect = RuntimeError
            self.assertEqual(self.invoke("/research", "token", json={"query": "Consulta"}).status_code, 503)
            client.assert_not_called()

    def test_expired_subscription_blocks_both_ai_routes_before_processing(self):
        import datetime as dt
        import play_billing
        now = dt.datetime(2026, 10, 27, tzinfo=dt.timezone.utc)
        purchase = {
            'externalAccountIdentifiers': {'obfuscatedExternalAccountId': play_billing.account_id('test')},
            'subscriptionState': 'SUBSCRIPTION_STATE_EXPIRED',
            'lineItems': [{'productId': 'bio_premium', 'offerDetails': {'basePlanId': 'mensual'},
                           'expiryTime': (now-dt.timedelta(minutes=1)).isoformat()}]}
        self.premium.return_value = play_billing.entitlement(purchase, 'test', now)
        with patch.object(main.auth, 'verify_id_token', return_value={'uid': 'test'}), \
             patch.object(main, 'prepare') as prepare, \
             patch.object(main, 'reserve_quota') as quota, \
             patch.object(main.genai, 'Client') as client:
            for path in ('/research', '/analyze-document'):
                response = self.invoke(path, 'token', json={})
                self.assertEqual(response.status_code, 402)
                self.assertEqual(response.get_json()['code'], 'premium_required')
            prepare.assert_not_called()
            quota.assert_not_called()
            client.assert_not_called()

    def test_document_uses_two_managed_calls_preserving_original_fields(self):
        from unittest.mock import Mock
        first, second = Mock(), Mock()
        first.model_dump.return_value = {"candidates": [{"finish_reason": "STOP", "content": {"parts": [{"text": json.dumps(sample())}]}}]}
        second.model_dump.return_value = {"candidates": [{"finish_reason": "STOP", "content": {"parts": [{"text": json.dumps({"interpretations": [{"explanation": "Sospecha pendiente de confirmar.", "finding_ids": [1]}], "limitations": "Documento aislado."})}]}}]}
        with patch.object(main.auth, "verify_id_token", return_value={"uid": "test"}), patch.object(main, "reserve_quota", return_value=True) as quota, patch.object(main.genai, "Client") as factory:
            client = factory.return_value.__enter__.return_value
            client.models.count_tokens.return_value.total_tokens = 1000
            client.models.generate_content.side_effect = [first, second]
            response = self.invoke("/analyze-document", "token", json=payload(synthetic_pdf()))
            self.assertEqual(response.status_code, 200)
            output = response.get_json()
            for key in ("name", "clinical_date", "findings", "date_evidence", "summary"):
                self.assertEqual(output[key], sample()[key])
            self.assertIn("[Hallazgos: 1]", output["possible_interpretations"][0])
            self.assertEqual(client.models.generate_content.call_count, 2)
            self.assertEqual(client.models.generate_content.call_args.kwargs["model"], main.REVIEW_MODEL)
            self.assertEqual(factory.call_args.kwargs["http_options"].retry_options.attempts, 1)
            quota.assert_called_once()
