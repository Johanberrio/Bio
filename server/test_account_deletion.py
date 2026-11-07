import datetime as dt
import unittest
from unittest.mock import MagicMock, patch
from google.api_core.exceptions import AlreadyExists
from flask import Flask, request
import account_deletion as deletion
import main
import public_pages

NOW = dt.datetime(2026, 10, 29, tzinfo=dt.timezone.utc)


class DeletionTests(unittest.TestCase):
    def setUp(self):
        self.db = MagicMock()
        self.refs = {}
        def collection(name):
            col = MagicMock()
            col.document.side_effect = lambda uid: self.refs.setdefault((name, uid), MagicMock())
            col.list_documents.return_value = []
            return col
        self.db.collection.side_effect = collection
        self.config = self.db.collection('_accountConfig').document('deletion')
        self.config.get.return_value.to_dict.return_value = {'enabled': True}
        self.ref = deletion.reference(self.db, 'owner')
        self.identity = {'uid': 'owner', 'auth_time': NOW.timestamp(), 'firebase': {'sign_in_provider': 'password'}}

    def test_requires_exact_confirmation_fresh_password_and_readiness(self):
        cases = [({}, self.identity), ({'confirm': 1}, self.identity), ({'confirm': True, 'uid': 'victim'}, self.identity),
                 ({'confirm': True}, {**self.identity, 'auth_time': NOW.timestamp()-301}),
                 ({'confirm': True}, {**self.identity, 'firebase': {'sign_in_provider': 'anonymous'}})]
        for body, identity in cases:
            with self.assertRaises(deletion.DeletionError): deletion.request(self.db, identity, body, NOW)
        self.config.get.return_value.to_dict.return_value = {}
        with self.assertRaises(deletion.DeletionError): deletion.request(self.db, self.identity, {'confirm': True}, NOW)
        self.ref.create.assert_not_called()

    def test_retry_preserves_request_date_and_no_sensitive_data(self):
        self.ref.create.side_effect = AlreadyExists('exists')
        self.assertTrue(deletion.request(self.db, self.identity, {'confirm': True}, NOW)['requested'])
        self.ref.set.assert_not_called()
        self.assertEqual(set(self.ref.create.call_args.args[0]), {'requestedAt', 'nextRunAt', 'state'})

    def test_disable_revoke_then_wait_before_erasing(self):
        self.ref.get.return_value.to_dict.return_value = {'nextRunAt': NOW}
        with patch.object(deletion.auth, 'update_user') as disable, patch.object(deletion.auth, 'revoke_refresh_tokens') as revoke, patch.object(deletion.mail_connect, 'accounts') as accounts, patch.object(deletion, 'purge') as purge:
            accounts.return_value.stream.return_value = []
            self.assertTrue(deletion.process(self.db, MagicMock(), 'owner', NOW))
            disable.assert_called_once_with('owner', disabled=True)
            revoke.assert_called_once_with('owner'); purge.assert_not_called()
        saved = self.ref.set.call_args.args[0]
        self.assertEqual(saved['nextRunAt'], NOW + dt.timedelta(hours=2))
        self.ref.get.return_value.to_dict.return_value = {'disabledAt': NOW, 'nextRunAt': NOW}
        with patch.object(deletion, 'purge') as purge:
            self.assertFalse(deletion.process(self.db, MagicMock(), 'owner', NOW+dt.timedelta(minutes=119)))
            purge.assert_not_called()

    def test_failure_never_marks_account_erased(self):
        self.ref.get.return_value.to_dict.return_value = {'disabledAt': NOW-dt.timedelta(hours=3)}
        with patch.object(deletion, 'purge', side_effect=RuntimeError), patch.object(deletion.auth, 'delete_user') as delete:
            with self.assertRaises(RuntimeError): deletion.process(self.db, MagicMock(), 'owner', NOW)
            delete.assert_not_called(); self.ref.set.assert_not_called()

    def test_marker_sweeps_for_thirty_days(self):
        self.ref.get.return_value.to_dict.return_value = {'disabledAt': NOW-dt.timedelta(hours=3)}
        with patch.object(deletion, 'purge') as purge, patch.object(deletion.auth, 'delete_user'):
            deletion.process(self.db, MagicMock(), 'owner', NOW)
            self.assertEqual(self.ref.set.call_args.args[0]['state'], 'erased')
            self.ref.delete.assert_not_called()
            deletion.process(self.db, MagicMock(), 'owner', NOW+dt.timedelta(days=30))
            self.ref.delete.assert_called_once(); self.assertEqual(purge.call_count, 2)

    def test_purge_only_owner_prefix_and_legacy_quota_children(self):
        bucket = MagicMock(); blob = MagicMock(); blob.generation = 123
        bucket.list_blobs.side_effect = [[blob], []]
        day = MagicMock()
        def collection(name):
            col = MagicMock()
            col.document.side_effect = lambda uid: self.refs.setdefault((name, uid), MagicMock())
            col.list_documents.return_value = [day] if name == '_analysisQuota' else []
            return col
        self.db.collection.side_effect = collection
        refs = [self.db.collection(root).document('owner') for root in deletion.ROOTS]
        refs += [day.collection('users').document('owner')]
        for ref in refs:
            ref.get.return_value.exists = False
            ref.collections.return_value = []
        deletion.purge(self.db, bucket, 'owner')
        for args in bucket.list_blobs.call_args_list:
            self.assertEqual(args.kwargs, {'prefix': 'users/owner/', 'versions': True})
        blob.delete.assert_called_once_with(if_generation_match=123)
        self.assertEqual([c.args[0] for c in self.db.recursive_delete.call_args_list], refs)
        with self.assertRaises(deletion.DeletionError): deletion.purge(self.db, bucket, '../victim')

    def test_incomplete_bulk_delete_is_retried(self):
        bucket = MagicMock(); bucket.list_blobs.return_value = []
        self.db.collection('users').document('owner').get.return_value.exists = True
        with self.assertRaises(RuntimeError): deletion.purge(self.db, bucket, 'owner')

    def test_pending_account_cannot_use_service(self):
        with patch.object(main.auth, 'verify_id_token', return_value=self.identity), patch.object(main.firestore, 'client'), patch.object(deletion, 'pending', return_value=True), patch.object(main.play_billing, 'status') as billing, patch.object(main.mail_connect, 'dispatch') as mail:
            for path in ('/research', '/analyze-document', '/billing/verify', '/mail/connect'):
                with Flask(__name__).test_request_context(path, method='POST', json={}, headers={'Authorization': 'Bearer test'}):
                    self.assertEqual(main.bio_api(request).status_code, 410)
            billing.assert_not_called(); mail.assert_not_called()

    def test_public_pages_are_complete_and_no_inline_script(self):
        for path in public_pages.PATHS:
            body, mime = public_pages.get(path)
            self.assertTrue(body)
            if mime.startswith('text/html'):
                self.assertNotIn('<script>', body)
                self.assertIn('Bio', body)
