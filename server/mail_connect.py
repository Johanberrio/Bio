"""Read-only OAuth mail adapters. Private collections are inaccessible to Firebase clients.

No mailbox passwords, message bodies, attachments or access tokens are persisted here.
Refresh tokens and OAuth client secrets use Firestore's encryption at rest and server IAM.
"""
import base64
import datetime as dt
import hashlib
import json
import re
import secrets
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from firebase_admin import firestore

BASE = "https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api"
PROVIDERS = {
    "google": {"authorize": "https://accounts.google.com/o/oauth2/v2/auth",
               "token": "https://oauth2.googleapis.com/token",
               "scope": "https://www.googleapis.com/auth/gmail.readonly"},
    "microsoft": {"authorize": "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                  "token": "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                  "scope": "offline_access https://graph.microsoft.com/Mail.Read https://graph.microsoft.com/User.Read"},
}
MIMES = {"application/pdf": b"%PDF-", "image/png": b"\x89PNG\r\n\x1a\n", "image/jpeg": b"\xff\xd8\xff"}
MAX_FILE = 10 * 1024 * 1024

class MailError(Exception):
    def __init__(self, message, status=400):
        super().__init__(message)
        self.status = status

def digest(value):
    return hashlib.sha256(value.encode()).hexdigest()

def remote(url, token=None, form=None, limit=16 * 1024 * 1024):
    # All callers construct URLs from fixed provider hosts; redirects cannot carry credentials.
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *args):
            return None
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None if form is None else urllib.parse.urlencode(form).encode()
    try:
        request = urllib.request.Request(url, data=data, headers=headers)
        with urllib.request.build_opener(NoRedirect).open(request, timeout=30) as response:
            raw = response.read(limit + 1)
        if len(raw) > limit:
            raise MailError("El proveedor devolvió demasiados datos.", 413)
        return json.loads(raw)
    except urllib.error.HTTPError as error:
        # Do not expose upstream bodies, tokens, subjects or patient data.
        if error.code in (400, 401, 403):
            raise MailError("Vuelve a conectar esta cuenta de correo para renovar el permiso.", 409) from None
        raise MailError("El proveedor de correo no respondió. Se reintentará.", 503) from None
    except (urllib.error.URLError, TimeoutError, ValueError):
        raise MailError("No se pudo contactar con el proveedor de correo.", 503) from None

def config(db, provider):
    if provider not in PROVIDERS:
        raise MailError("Proveedor no compatible. Usa Gmail u Outlook/Hotmail.")
    value = db.collection("_mailConfig").document(provider).get().to_dict() or {}
    if not value.get("enabled") or not value.get("clientId") or not value.get("clientSecret"):
        raise MailError("La conexión de este proveedor aún no está habilitada para Bio.", 503)
    return value

def accounts(db, uid):
    return db.collection("_mailUsers").document(uid).collection("accounts")

def public_account(snapshot):
    value = snapshot.to_dict()
    return {"id": snapshot.id, "provider": value["provider"], "email": value["email"]}

def status(db, uid):
    providers = {}
    for p in PROVIDERS:
        try:
            config(db, p)
            providers[p] = True
        except MailError:
            providers[p] = False
    return {"providers": providers, "accounts": [public_account(s) for s in accounts(db, uid).stream()]}

def start(db, uid, provider):
    cfg = config(db, provider)
    state, verifier = secrets.token_urlsafe(32), secrets.token_urlsafe(48)
    # A single pending flow per owner prevents unbounded private token documents.
    state = uid + "." + state
    ref = db.collection("_mailPending").document(uid)
    ref.set({"stateHash": digest(state), "provider": provider, "verifier": verifier,
             "expires": time.time() + 600})
    params = {"client_id": cfg["clientId"], "redirect_uri": BASE + "/mail/callback/" + provider,
              "response_type": "code", "scope": PROVIDERS[provider]["scope"], "state": state,
              "code_challenge": base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).decode().rstrip("="),
              "code_challenge_method": "S256", "prompt": "consent"}
    if provider == "google":
        params.update(access_type="offline", include_granted_scopes="true")
    else:
        params["response_mode"] = "query"
    return {"url": PROVIDERS[provider]["authorize"] + "?" + urllib.parse.urlencode(params)}

def callback(db, provider, args):
    state = args.get("state", "")
    if len(state) > 200 or "." not in state:
        raise MailError("Autorización inválida o vencida.")
    uid = state.split(".", 1)[0]
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,128}", uid):
        raise MailError("Autorización inválida.")
    ref = db.collection("_mailPending").document(uid)
    @firestore.transactional
    def claim(transaction):
        value = ref.get(transaction=transaction).to_dict() or {}
        if value.get("provider") != provider or value.get("expires", 0) < time.time() or not secrets.compare_digest(value.get("stateHash", ""), digest(state)):
            raise MailError("Autorización inválida o vencida. Conecta de nuevo desde Bio.")
        transaction.delete(ref)
        return value
    pending = claim(db.transaction())
    if args.get("error") or not args.get("code"):
        raise MailError("Conexión cancelada. Puedes volver a Bio.")
    cfg = config(db, provider)
    token = remote(PROVIDERS[provider]["token"], form={"client_id": cfg["clientId"],
        "client_secret": cfg["clientSecret"], "grant_type": "authorization_code", "code": args["code"],
        "code_verifier": pending["verifier"], "redirect_uri": BASE + "/mail/callback/" + provider})
    required = "gmail.readonly" if provider == "google" else "mail.read"
    if not any(s.lower().endswith("/" + required) or s.lower() == required for s in token.get("scope", "").split()):
        raise MailError("Falta el permiso de lectura del correo.")
    if not token.get("refresh_token") or not token.get("access_token"):
        raise MailError("El proveedor no autorizó acceso continuo. Vuelve a conectar.")
    access = token["access_token"]
    if provider == "google":
        profile = remote("https://gmail.googleapis.com/gmail/v1/users/me/profile", access)
        email = profile.get("emailAddress", "")
        identity = email.lower()
    else:
        profile = remote("https://graph.microsoft.com/v1.0/me?$select=id,mail,userPrincipalName", access)
        email = profile.get("mail") or profile.get("userPrincipalName", "")
        identity = profile.get("id", "")
    if not identity or not email or len(email) > 320:
        raise MailError("No se pudo identificar la cuenta autorizada.")
    key = digest(provider + ":" + identity)
    accounts(db, uid).document(key).set({"provider": provider, "email": email,
        "refresh": token["refresh_token"], "generation": secrets.token_hex(16)})

def account_ref(db, uid, key):
    if not isinstance(key, str) or not re.fullmatch(r"[a-f0-9]{64}", key):
        raise MailError("Cuenta inválida.")
    return accounts(db, uid).document(key)

def access(db, uid, key):
    ref = account_ref(db, uid, key)
    value = ref.get().to_dict()
    if not value:
        raise MailError("Esta cuenta ya no está conectada.", 404)
    cfg = config(db, value["provider"])
    token = remote(PROVIDERS[value["provider"]]["token"], form={"client_id": cfg["clientId"],
        "client_secret": cfg["clientSecret"], "refresh_token": value["refresh"], "grant_type": "refresh_token"})
    @firestore.transactional
    def retain(transaction):
        current = ref.get(transaction=transaction).to_dict() or {}
        if current.get("generation") != value["generation"]:
            raise MailError("La conexión cambió. Vuelve a intentarlo.", 409)
        if token.get("refresh_token"):
            transaction.update(ref, {"refresh": token["refresh_token"]})
    retain(db.transaction())
    if not token.get("access_token"):
        raise MailError("Vuelve a conectar el correo.", 409)
    return value["provider"], token["access_token"]

def disconnect(db, uid, key):
    # Delete server access first. In-flight requests may finish, but cannot restore credentials.
    ref = account_ref(db, uid, key)
    value = ref.get().to_dict()
    ref.delete()
    db.collection("_mailPending").document(uid).delete()
    if value and value["provider"] == "google":
        try:
            remote("https://oauth2.googleapis.com/revoke", form={"token": value["refresh"]})
        except MailError:
            pass
    return {"disconnected": True}

def clinical_hint(subject, name):
    # Subject deliberately ignored: each attachment qualifies on its own name.
    value = re.sub(r"([a-záéíóúñ])([A-ZÁÉÍÓÚÑ])", r"\1 \2", str(name))
    value = unicodedata.normalize("NFKD", value)
    value = "".join(c for c in value if unicodedata.category(c) != "Cf")
    value = "".join(c for c in value if not unicodedata.combining(c)).lower()
    value = re.sub(r"(?<=[a-z])(?=\d)|(?<=\d)(?=[a-z])", " ", value)
    value = re.sub(r"[_\W]+", " ", value)
    # This is candidate selection, not a clinical classification. A history may
    # be named with only the patient's name and HISTORIA; its content and owner
    # must still pass the document analysis before anything is imported.
    return bool(re.search(r"\b(examen\w*|exam\w*|historias?|historial(?:es)?|ayudas?\s+diagnostic\w*|informacion\s+clinica|document\w*\s+clinic\w*|clinical|medical|medic[oa]\w*|laborator\w*|resultado\w*|results?|hemograma|resonancia|radiolog\w*|ecograf\w*|biopsia|epicrisis|tomograf\w*|electrocardiograma|patolog\w*)\b", value))

def file_meta(message, attachment, name, mime, size, subject, inline=False):
    if inline or mime not in MIMES or type(size) is not int or not 0 < size <= MAX_FILE or not clinical_hint(subject, name):
        return None
    return {"message": message, "attachment": attachment, "name": str(name)[:240], "mime": mime, "size": size,
            "key": digest(message + ":" + attachment)}

def gmail_parts(message, payload, subject):
    out = []
    def walk(part, depth=0):
        if depth > 12:
            return
        headers = {h.get("name", "").lower(): h.get("value", "") for h in part.get("headers", [])}
        body = part.get("body", {})
        if part.get("filename") and body.get("attachmentId"):
            item = file_meta(message, body["attachmentId"], part["filename"], part.get("mimeType"), body.get("size", 0), subject,
                             headers.get("content-disposition", "").lower().startswith("inline"))
            if item:
                out.append(item)
        for child in part.get("parts", []):
            walk(child, depth + 1)
    walk(payload)
    return out

def graph_cursor(url):
    if not url:
        return ""
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme != "https" or parsed.netloc != "graph.microsoft.com" or parsed.path != "/v1.0/me/messages" or parsed.fragment:
        raise MailError("El proveedor devolvió una paginación inválida.", 502)
    return url

def scan(db, uid, data):
    provider, token = access(db, uid, data.get("account"))
    since, until = data.get("since"), data.get("until")
    now = int(time.time())
    if type(since) is not int or type(until) is not int or not now - 32 * 86400 <= since < until <= now + 60:
        raise MailError("Intervalo inválido. Se consultan como máximo los últimos 30 días.")
    cursor = data.get("cursor", "")
    if not isinstance(cursor, str) or len(cursor) > 12000:
        raise MailError("Paginación inválida.")
    attachments = []
    if provider == "google":
        # A message sent to oneself can have both SENT and INBOX labels.
        params = {"q": f"has:attachment -in:spam -in:trash -in:drafts {{-in:sent in:inbox}} after:{since} before:{until}", "maxResults": 5}
        if cursor:
            params["pageToken"] = cursor
        page = remote("https://gmail.googleapis.com/gmail/v1/users/me/messages?" + urllib.parse.urlencode(params), token)
        for item in page.get("messages", []):
            mid = urllib.parse.quote(item["id"], safe="")
            message = remote(f"https://gmail.googleapis.com/gmail/v1/users/me/messages/{mid}?format=full", token)
            payload = message.get("payload", {})
            subject = next((h.get("value", "") for h in payload.get("headers", []) if h.get("name", "").lower() == "subject"), "")
            attachments.extend(gmail_parts(item["id"], payload, subject))
        next_page = page.get("nextPageToken", "")
    else:
        stamp = lambda t: dt.datetime.fromtimestamp(t, dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        params = {"$filter": f"receivedDateTime ge {stamp(since)} and receivedDateTime lt {stamp(until)} and hasAttachments eq true and isDraft eq false",
                  "$select": "id,subject", "$top": 5}
        url = graph_cursor(cursor) if cursor else "https://graph.microsoft.com/v1.0/me/messages?" + urllib.parse.urlencode(params)
        page = remote(url, token)
        for message in page.get("value", []):
            mid = urllib.parse.quote(message["id"], safe="")
            files = remote(f"https://graph.microsoft.com/v1.0/me/messages/{mid}/attachments?$select=id,name,contentType,size,isInline", token)
            if files.get("@odata.nextLink"):
                # Never advance past a partially enumerated message.
                raise MailError("Un mensaje tiene demasiados adjuntos. Revísalo en el correo.", 413)
            for f in files.get("value", []):
                if f.get("@odata.type") != "#microsoft.graph.fileAttachment":
                    continue
                item = file_meta(message["id"], f["id"], f.get("name"), f.get("contentType"), f.get("size"), message.get("subject", ""), f.get("isInline", False))
                if item:
                    attachments.append(item)
        next_page = graph_cursor(page.get("@odata.nextLink", ""))
    return {"attachments": attachments, "cursor": next_page}

def download(db, uid, data):
    provider, token = access(db, uid, data.get("account"))
    ids = [data.get("message"), data.get("attachment")]
    if any(not isinstance(s, str) or not 1 <= len(s) <= 2048 for s in ids):
        raise MailError("Adjunto inválido.")
    mid, aid = [urllib.parse.quote(s, safe="") for s in ids]
    if provider == "google":
        result = remote(f"https://gmail.googleapis.com/gmail/v1/users/me/messages/{mid}/attachments/{aid}", token)
        encoded = result.get("data", "")
        try:
            raw = base64.b64decode(encoded + "=" * (-len(encoded) % 4), altchars=b"-_", validate=True)
        except ValueError:
            raise MailError("El adjunto no es válido.") from None
    else:
        result = remote(f"https://graph.microsoft.com/v1.0/me/messages/{mid}/attachments/{aid}", token)
        if result.get("@odata.type") != "#microsoft.graph.fileAttachment" or result.get("isInline"):
            raise MailError("Solo se admiten archivos adjuntos.")
        try:
            raw = base64.b64decode(result.get("contentBytes", ""), validate=True)
        except ValueError:
            raise MailError("El adjunto no es válido.") from None
    mime = next((m for m, signature in MIMES.items() if raw.startswith(signature)), None)
    if not mime or not 0 < len(raw) <= MAX_FILE:
        raise MailError("Usa PDF, JPG o PNG de hasta 10 MB.", 415)
    return {"mime": mime, "data": base64.b64encode(raw).decode()}

def dispatch(db, uid, path, data):
    if path == "/mail/status":
        return status(db, uid)
    if path == "/mail/connect":
        return start(db, uid, data.get("provider"))
    if path == "/mail/disconnect":
        return disconnect(db, uid, data.get("account"))
    if path == "/mail/scan":
        return scan(db, uid, data)
    if path == "/mail/download":
        return download(db, uid, data)
    raise MailError("No encontrado.", 404)
