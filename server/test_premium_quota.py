import datetime as dt
import threading
import unittest
from concurrent.futures import ThreadPoolExecutor
from unittest.mock import Mock, patch
import premium_quota as quota
import prompt_budget


class Ref:
    def __init__(self, db, path): self.db, self.path = db, path
    @property
    def id(self): return self.path.rsplit('/', 1)[-1]
    def collection(self, name): return Ref(self.db, self.path + '/' + name)
    def document(self, name): return Ref(self.db, self.path + '/' + name)
    def get(self, transaction=None): return Mock(to_dict=lambda: self.db.data.get(self.path, {}).copy())


class Db:
    def __init__(self): self.data, self.lock = {}, threading.Lock()
    def collection(self, name): return Ref(self, name)
    def transaction(self): return self
    def set(self, ref, value, merge=False):
        self.data[ref.path] = {**(self.data.get(ref.path, {}) if merge else {}), **value}


def atomic(fn):
    def wrapped(tx):
        with tx.lock: return fn(tx)
    return wrapped


class QuotaTests(unittest.TestCase):
    def setUp(self):
        self.db = Db()
        self.now = dt.datetime(2026, 12, 31, 23, 59, tzinfo=dt.timezone.utc)
        stub = patch.object(quota.firestore, 'transactional', atomic)
        stub.start(); self.addCleanup(stub.stop)

    def test_month_rollover_including_december(self):
        self.assertEqual(quota.period(self.now), ('2026-12', '2027-01-01T00:00:00+00:00'))
        self.assertEqual(quota.period(self.now + dt.timedelta(minutes=2))[0], '2027-01')

    def test_concurrent_limit_and_separate_accounts_and_types(self):
        with ThreadPoolExecutor(max_workers=10) as pool:
            results = list(pool.map(lambda _: quota.reserve(self.db, 'a', 'analysis', self.now), range(45)))
        self.assertEqual(sum(bool(r) for r in results), 30)
        for _ in range(20): self.assertTrue(quota.reserve(self.db, 'a', 'research', self.now))
        self.assertIsNone(quota.reserve(self.db, 'a', 'research', self.now))
        self.assertTrue(quota.reserve(self.db, 'b', 'analysis', self.now))
        self.assertTrue(quota.reserve(self.db, 'a', 'analysis', self.now + dt.timedelta(days=1)))

    def test_refund_is_idempotent_and_stays_in_original_month(self):
        ticket = quota.reserve(self.db, 'a', 'analysis', self.now)
        quota.reserve(self.db, 'a', 'analysis', self.now + dt.timedelta(days=1))
        quota.refund(self.db, 'a', ticket); quota.refund(self.db, 'a', ticket)
        self.assertEqual(quota.summary(self.db, 'a', self.now)['analysis']['remaining'], 30)
        self.assertEqual(quota.summary(self.db, 'a', self.now + dt.timedelta(days=1))['analysis']['used'], 1)

    def test_other_user_cannot_refund_ticket(self):
        ticket = quota.reserve(self.db, 'a', 'analysis', self.now)
        quota.refund(self.db, 'b', ticket)
        self.assertEqual(quota.summary(self.db, 'a', self.now)['analysis']['used'], 1)

    def test_input_limit_never_truncates_source(self):
        client = Mock(); client.models.count_tokens.return_value.total_tokens = 20001
        parts = [{'text': 'whole document'}]
        with self.assertRaises(prompt_budget.TooLarge): prompt_budget.check(client, 'model', parts, 'instructions')
        self.assertEqual(parts, [{'text': 'whole document'}])
        client.models.generate_content.assert_not_called()
        client.models.count_tokens.return_value.total_tokens = 20000
        prompt_budget.check(client, 'model', parts, 'instructions')
        client.models.count_tokens.return_value.total_tokens = None
        with self.assertRaises(ValueError): prompt_budget.check(client, 'model', parts, 'instructions')
