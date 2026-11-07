"""Verified self-service erasure queue; worker never accepts an arbitrary client UID."""
import datetime as dt
import re
import time
from firebase_admin import auth
from google.api_core.exceptions import NotFound
import mail_connect

COLLECTION = "_accountDeletion"
BUCKET = "bio-project-15d15.firebasestorage.app"
ROOTS = ("users", "_mailUsers", "_mailPending", "_playSubscriptions", "_billingTesters", "_premiumUsage", "_reviewAccounts")


class DeletionError(Exception):
    pass


def reference(db, uid):
    if not isinstance(uid, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,128}", uid):
        raise DeletionError("Cuenta inválida.")
    return db.collection(COLLECTION).document(uid)


def pending(db, uid):
    return reference(db, uid).get().exists is True


def request(db, identity, data, now=None):
    now = now or dt.datetime.now(dt.timezone.utc)
    if (not isinstance(data, dict) or set(data) != {"confirm"} or data.get("confirm") is not True
            or identity.get("firebase", {}).get("sign_in_provider") != "password"):
        raise DeletionError("Confirma la eliminación e inicia sesión con tu contraseña.")
    signed = identity.get("auth_time", 0)
    if not isinstance(signed, (int, float)) or not 0 <= now.timestamp() - signed <= 300:
        raise DeletionError("Vuelve a introducir tu contraseña para confirmar la eliminación.")
    uid = identity["uid"]
    config = db.collection("_accountConfig").document("deletion").get().to_dict() or {}
    if config.get("enabled") is not True and uid not in config.get("testUids", []):
        raise DeletionError("La eliminación automática aún no está disponible. Contacta con juandrak@msn.com.")
    ref = reference(db, uid)
    # An immutable start time makes retries safe; no content or email in this queue.
    from google.api_core.exceptions import AlreadyExists
    try:
        ref.create({"requestedAt": now, "nextRunAt": now, "state": "pending"})
    except AlreadyExists:
        pass
    # The queue is durable before revocation. A worker retries even if auth is unavailable.
    return {"requested": True, "message": "Solicitud registrada. Se eliminarán tu cuenta y los datos de Bio. La limpieza automática normalmente comienza durante las siguientes 24 horas. La eliminación no cancela tu suscripción en Google Play."}


def purge(db, bucket, uid):
    """Only a validated UID prefix. Delete all object generations, including versions."""
    reference(db, uid)
    for blob in bucket.list_blobs(prefix="users/" + uid + "/", versions=True):
        try:
            blob.delete(if_generation_match=blob.generation)
        except NotFound:
            pass
    refs = [db.collection(root).document(uid) for root in ROOTS]
    # Old daily quotas predate the per-account monthly layout. Keep aggregate totals.
    refs.extend(day.collection("users").document(uid)
                for day in db.collection("_analysisQuota").list_documents())
    for ref in refs:
        db.recursive_delete(ref)
        # BulkWriter can exhaust per-write retries without throwing at close().
        if ref.get().exists is True or any(ref.collections()):
            raise RuntimeError("Account data deletion incomplete; retry required")
    if next(iter(bucket.list_blobs(prefix="users/" + uid + "/", versions=True)), None) is not None:
        raise RuntimeError("Account file deletion incomplete; retry required")


def process(db, bucket, uid, now=None):
    now = now or dt.datetime.now(dt.timezone.utc)
    ref = reference(db, uid)
    task = ref.get().to_dict()
    if not task or task.get("nextRunAt", now) > now:
        return False
    # Disable first and allow issued ID tokens to expire before the first data purge.
    # Daily sweeps catch late uploads/requests; only the minimal UID marker remains.
    if not task.get("disabledAt"):
        try:
            auth.update_user(uid, disabled=True)
            auth.revoke_refresh_tokens(uid)
        except auth.UserNotFoundError:
            pass
        for account in mail_connect.accounts(db, uid).stream():
            mail_connect.disconnect(db, uid, account.id)
        db.collection("_mailPending").document(uid).delete()
        ref.set({"disabledAt": now, "nextRunAt": now + dt.timedelta(hours=2), "state": "waiting"}, merge=True)
        return True
    if now < task["disabledAt"] + dt.timedelta(hours=2):
        return False
    purge(db, bucket, uid)
    try:
        auth.delete_user(uid)
    except auth.UserNotFoundError:
        pass
    if now >= task["disabledAt"] + dt.timedelta(days=30):
        ref.delete()
    else:
        ref.set({"state": "erased", "lastSweepAt": now, "nextRunAt": now + dt.timedelta(days=1)}, merge=True)
    return True


def run_batch(db, bucket, now=None):
    from google.cloud.firestore_v1.base_query import FieldFilter
    now = now or dt.datetime.now(dt.timezone.utc)
    failures = 0
    # Earliest due first; bounded batch/time, with no user IDs or medical data in logs.
    rows = db.collection(COLLECTION).where(filter=FieldFilter("nextRunAt", "<=", now)).order_by("nextRunAt").limit(100).stream()
    deadline = time.monotonic() + 480
    for row in rows:
        if time.monotonic() > deadline:
            break
        try:
            process(db, bucket, row.id, now)
        except Exception:
            failures += 1
            row.reference.set({"nextRunAt": now + dt.timedelta(hours=1)}, merge=True)
    if failures:
        raise RuntimeError("Account erasure retry required; failed tasks: " + str(failures))
