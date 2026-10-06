import unittest
from unittest.mock import patch
from flask import Flask, request
import main

class FirebaseEntryTests(unittest.TestCase):
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

    def test_invalid_file_rejected_before_quota(self):
        with patch.object(main.auth,'verify_id_token',return_value={'uid':'test'}), patch.object(main,'reserve_quota') as quota:
            r=self.invoke('/analyze-document','token',json={'document':{'mime':'application/pdf','data':'ZmFrZQ=='}})
            self.assertEqual(r.status_code,400)
            quota.assert_not_called()
