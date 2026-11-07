"""Server-only subscription verification. Never trust a client premium flag."""
import datetime as dt
import hashlib
import os
from urllib.parse import quote
import google.auth
from google.auth.transport.requests import AuthorizedSession
from firebase_admin import firestore

PACKAGE = "com.alvia.health"
PRODUCT = "bio_premium"
PLAN = "mensual"
PROJECT = "bio-project-15d15"


class BillingError(Exception):
    pass


def account_id(uid):
    # Android hashes FirebaseCloud.owner(): project + ':' + Firebase UID.
    return hashlib.sha256((PROJECT + ":" + uid).encode()).hexdigest()


def purchases_enabled(db, uid, now=None):
    if os.environ.get("BIO_PREMIUM_READY") == "true":
        return True
    # Admin-only allowlist enables checkout, never grants a subscription or AI.
    entry = db.collection("_billingTesters").document(uid).get().to_dict() or {}
    expiry = entry.get("expiresAt")
    now = now or dt.datetime.now(dt.timezone.utc)
    return (entry.get("enabled") is True and isinstance(expiry, dt.datetime)
            and expiry.tzinfo is not None and expiry > now)


def entitlement(purchase, uid, now=None):
    now = now or dt.datetime.now(dt.timezone.utc)
    identifier = purchase.get("externalAccountIdentifiers", {}).get("obfuscatedExternalAccountId")
    if identifier != account_id(uid):
        raise BillingError("La compra pertenece a otra cuenta de Bio.")
    allowed = purchase.get("subscriptionState") in {
        "SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_CANCELED"}
    expiry = now
    valid = False
    for line in purchase.get("lineItems", []):
        if line.get("productId") != PRODUCT or line.get("offerDetails", {}).get("basePlanId") != PLAN:
            continue
        try:
            end = dt.datetime.fromisoformat(line["expiryTime"].replace("Z", "+00:00"))
            if end.tzinfo is not None and end > now:
                expiry = max(expiry, end)
                valid = True
        except (ValueError, KeyError, TypeError):
            continue
    return {"premium": allowed and valid, "expiresAt": expiry.isoformat() if valid else None}


def verify(db, uid, token):
    if not isinstance(token, str) or not 10 <= len(token) <= 4096:
        raise BillingError("Compra no válida.")
    credentials, _ = google.auth.default(scopes=["https://www.googleapis.com/auth/androidpublisher"])
    base = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/" + PACKAGE
    with AuthorizedSession(credentials) as session:
        response = session.get(base + "/purchases/subscriptionsv2/tokens/" + quote(token, safe=""), timeout=25)
        if response.status_code != 200:
            raise BillingError("No se pudo verificar la suscripción con Google Play.")
        purchase = response.json()
        result = entitlement(purchase, uid)
        ref = db.collection("_playSubscriptions").document(uid)
        # Only this backend writes here; client Firestore rules deny all access.
        ref.set({"token": token, **result, "checkedAt": firestore.SERVER_TIMESTAMP})
        if result["premium"] and purchase.get("acknowledgementState") == "ACKNOWLEDGEMENT_STATE_PENDING":
            ack = session.post(base + "/purchases/subscriptions/" + PRODUCT + "/tokens/" + quote(token, safe="") + ":acknowledge", json={}, timeout=25)
            if ack.status_code not in (200, 204):
                raise BillingError("Compra recibida; vuelve a restaurarla para completar la activación.")
        return result


def status(db, uid):
    # Dedicated, admin-provisioned review accounts only. No client flag, email
    # matching, purchase spoofing or relaxed quotas. Revocation takes effect on
    # the next request, exactly as for a Play subscription.
    review = db.collection("_reviewAccounts").document(uid).get().to_dict() or {}
    if (review.get("enabled") is True and review.get("purpose") == "google_play_review"
            and review.get("uid") == uid and review.get("syntheticOnly") is True):
        return {"premium": True, "accessSource": "google_play_review"}
    stored = db.collection("_playSubscriptions").document(uid).get().to_dict() or {}
    # Revalidate on every AI request: expiry, refunds and holds cannot use stale flags.
    return verify(db, uid, stored["token"]) if stored.get("token") else {"premium": False}
