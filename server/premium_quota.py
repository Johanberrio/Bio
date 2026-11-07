"""Per-account calendar-month allowance; no shared trial ceiling."""
import datetime as dt
import uuid
from firebase_admin import firestore

LIMITS = {"analysis": 30, "research": 20}


def period(now=None):
    now = now or dt.datetime.now(dt.timezone.utc)
    now = now.astimezone(dt.timezone.utc)
    start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
    end = (start.replace(year=start.year + 1, month=1) if start.month == 12
           else start.replace(month=start.month + 1))
    return start.strftime("%Y-%m"), end.isoformat()


def bucket(db, uid, month):
    return db.collection("_premiumUsage").document(uid).collection("months").document(month)


def summary(db, uid, now=None):
    month, reset = period(now)
    data = bucket(db, uid, month).get().to_dict() or {}
    return {"period": month, "resetAt": reset, "timezone": "UTC", **{
        kind: {"limit": limit, "used": int(data.get(kind, 0)),
               "remaining": max(0, limit - int(data.get(kind, 0)))}
        for kind, limit in LIMITS.items()}}


def reserve(db, uid, kind, now=None):
    if kind not in LIMITS:
        raise ValueError("Unknown allowance")
    month, reset = period(now)
    ref = bucket(db, uid, month)
    ticket = ref.collection("reservations").document(uuid.uuid4().hex)

    @firestore.transactional
    def run(tx):
        data = ref.get(transaction=tx).to_dict() or {}
        count = int(data.get(kind, 0))
        if count >= LIMITS[kind]:
            return None
        tx.set(ref, {kind: count + 1}, merge=True)
        tx.set(ticket, {"kind": kind, "refunded": False})
        return {"month": month, "id": ticket.id, "kind": kind, "resetAt": reset}
    return run(db.transaction())


def refund(db, uid, reservation):
    """Return one failed request once, to its original month even across midnight."""
    ref = bucket(db, uid, reservation["month"])
    ticket = ref.collection("reservations").document(reservation["id"])

    @firestore.transactional
    def run(tx):
        saved = ticket.get(transaction=tx).to_dict() or {}
        data = ref.get(transaction=tx).to_dict() or {}
        if saved.get("refunded") or saved.get("kind") != reservation["kind"]:
            return
        kind = saved["kind"]
        tx.set(ref, {kind: max(0, int(data.get(kind, 0)) - 1)}, merge=True)
        tx.set(ticket, {"refunded": True}, merge=True)
    run(db.transaction())
