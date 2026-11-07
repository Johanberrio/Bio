import copy
import datetime as dt
import hashlib
import unittest
from unittest.mock import Mock, patch
from play_billing import entitlement, account_id, BillingError, purchases_enabled, verify, status


class SubscriptionTests(unittest.TestCase):
    def test_review_access_is_scoped_and_revocable(self):
        db = Mock()
        entries = {'_reviewAccounts': {}, '_playSubscriptions': {}}
        db.collection.side_effect = lambda name: Mock(document=lambda uid: Mock(
            get=lambda: Mock(to_dict=lambda: entries[name])))
        grant = {'enabled': True, 'purpose': 'google_play_review', 'uid': 'reviewer', 'syntheticOnly': True}
        with patch('play_billing.verify') as verify_purchase:
            for entry in ({}, dict(grant, enabled=False), dict(grant, enabled=1),
                          dict(grant, uid='other'), dict(grant, purpose='tester'),
                          dict(grant, syntheticOnly=False)):
                entries['_reviewAccounts'] = entry
                self.assertFalse(status(db, 'reviewer')['premium'])
            entries['_reviewAccounts'] = grant
            self.assertEqual(status(db, 'reviewer'), {'premium': True, 'accessSource': 'google_play_review'})
            verify_purchase.assert_not_called()
            entries['_reviewAccounts'] = dict(grant, enabled=False)
            self.assertFalse(status(db, 'reviewer')['premium'])

    def test_regular_accounts_still_verify_play_purchase(self):
        db = Mock()
        entries = {'_reviewAccounts': {}, '_playSubscriptions': {'token': 'real-purchase-token'}}
        db.collection.side_effect = lambda name: Mock(document=lambda uid: Mock(
            get=lambda: Mock(to_dict=lambda: entries[name])))
        with patch('play_billing.verify', return_value={'premium': False}) as verify_purchase:
            self.assertFalse(status(db, 'customer')['premium'])
            verify_purchase.assert_called_once_with(db, 'customer', 'real-purchase-token')

    @patch('play_billing.AuthorizedSession')
    @patch('play_billing.google.auth.default', return_value=(Mock(), 'bio-project-15d15'))
    def test_android_purchase_is_stored_and_acknowledged(self, credentials, session_type):
        purchase = copy.deepcopy(self.purchase)
        purchase['lineItems'][0]['expiryTime'] = (dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=1)).isoformat()
        purchase['acknowledgementState'] = 'ACKNOWLEDGEMENT_STATE_PENDING'
        purchase['externalAccountIdentifiers']['obfuscatedExternalAccountId'] = hashlib.sha256(b'bio-project-15d15:user-a').hexdigest()
        session = session_type.return_value.__enter__.return_value
        session.get.return_value = Mock(status_code=200, json=lambda: purchase)
        session.post.return_value = Mock(status_code=204)
        db = Mock()
        self.assertTrue(verify(db, 'user-a', 'fake-purchase-token')['premium'])
        saved = db.collection.return_value.document.return_value.set.call_args.args[0]
        self.assertTrue(saved['premium'])
        session.post.assert_called_once()
        db.reset_mock()
        session.post.reset_mock()
        with self.assertRaises(BillingError):
            verify(db, 'other-user', 'fake-purchase-token')
        db.collection.assert_not_called()
        session.post.assert_not_called()

    def test_android_owner_hash_matches_server(self):
        android_owner = "bio-project-15d15:user-a"
        android_hash = hashlib.sha256(android_owner.encode("utf-8")).hexdigest()
        self.assertEqual(account_id("user-a"), android_hash)
        self.purchase["externalAccountIdentifiers"]["obfuscatedExternalAccountId"] = android_hash
        self.assertTrue(entitlement(self.purchase, "user-a", self.now)["premium"])
        with self.assertRaises(BillingError):
            entitlement(self.purchase, "user-b", self.now)

    def test_uid_only_hash_and_wrong_project_are_rejected(self):
        identifiers = self.purchase["externalAccountIdentifiers"]
        identifiers["obfuscatedExternalAccountId"] = hashlib.sha256(b"user-a").hexdigest()
        with self.assertRaises(BillingError):
            entitlement(self.purchase, "user-a", self.now)
        identifiers["obfuscatedExternalAccountId"] = hashlib.sha256(b"other-project:user-a").hexdigest()
        with self.assertRaises(BillingError):
            entitlement(self.purchase, "user-a", self.now)

    def test_checkout_allowlist_expires_and_does_not_grant_premium(self):
        db = Mock()
        now = dt.datetime(2026, 10, 26, tzinfo=dt.timezone.utc)
        with patch.dict('os.environ', {'BIO_PREMIUM_READY': 'false'}):
            for entry, expected in (({}, False), ({'enabled': True}, False),
                ({'enabled': True, 'expiresAt': now}, False),
                ({'enabled': True, 'expiresAt': now + dt.timedelta(days=1)}, True)):
                db.collection.return_value.document.return_value.get.return_value.to_dict.return_value = entry
                self.assertEqual(purchases_enabled(db, 'tester', now), expected)
            db.collection.assert_called_with('_billingTesters')

    def setUp(self):
        self.now = dt.datetime(2026, 10, 23, tzinfo=dt.timezone.utc)
        self.purchase = {"externalAccountIdentifiers": {"obfuscatedExternalAccountId": account_id("user-a")},
                         "subscriptionState": "SUBSCRIPTION_STATE_ACTIVE",
                         "lineItems": [{"productId": "bio_premium", "offerDetails": {"basePlanId": "mensual"},
                                        "expiryTime": "2026-11-23T00:00:00Z"}]}

    def test_paid_states_only(self):
        for state, expected in [("ACTIVE", True), ("IN_GRACE_PERIOD", True), ("CANCELED", True),
                                ("ON_HOLD", False), ("PAUSED", False), ("PENDING", False), ("EXPIRED", False)]:
            with self.subTest(state=state):
                self.purchase["subscriptionState"] = "SUBSCRIPTION_STATE_" + state
                self.assertEqual(entitlement(self.purchase, "user-a", self.now)["premium"], expected)

    def test_expiry_and_product(self):
        for change in [{"expiryTime": "2026-10-23T00:00:00Z"}, {"expiryTime": "bad"},
                       {"productId": "other"}, {"offerDetails": {"basePlanId": "other"}}]:
            p = copy.deepcopy(self.purchase)
            p["lineItems"][0].update(change)
            self.assertFalse(entitlement(p, "user-a", self.now)["premium"])

    def test_purchase_cannot_transfer_accounts(self):
        with self.assertRaises(BillingError):
            entitlement(self.purchase, "user-b", self.now)
        del self.purchase["externalAccountIdentifiers"]
        with self.assertRaises(BillingError):
            entitlement(self.purchase, "user-a", self.now)


if __name__ == "__main__":
    unittest.main()
