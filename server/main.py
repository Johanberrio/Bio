"""Firebase HTTPS entrypoint. Credentials stay in Google's service identity."""
import datetime
import json
import os
import html
import mail_connect
from firebase_functions import https_fn, options
from firebase_admin import initialize_app, auth, firestore
from google import genai
from google.genai import types
from vertex_gateway import prepare, extract

PROJECT = "bio-project-15d15"
MODEL = os.environ.get("BIO_MODEL", "gemini-3.1-pro-preview")
initialize_app(options={"projectId": PROJECT})


def reserve_quota(db, uid):
    # Atomic global and per-user ceilings persist across cold starts and instances.
    day = datetime.datetime.now(datetime.timezone.utc).date().isoformat()
    refs = [db.collection("_analysisQuota").document(day),
            db.collection("_analysisQuota").document(day).collection("users").document(uid)]
    @firestore.transactional
    def reserve(transaction):
        counts = [(r.get(transaction=transaction).to_dict() or {}).get("count", 0) for r in refs]
        if counts[0] >= 50 or counts[1] >= 10:
            return False
        for ref, count in zip(refs, counts):
            transaction.set(ref, {"count": count + 1})
        return True
    return reserve(db.transaction())


@https_fn.on_request(region="us-east1", timeout_sec=300, memory=options.MemoryOption.MB_512,
                    min_instances=0, max_instances=1, concurrency=2,
                    service_account="bio-analysis@bio-project-15d15.iam.gserviceaccount.com")
def bio_api(req: https_fn.Request) -> https_fn.Response:
    def reply(code, value):
        return https_fn.Response(json.dumps(value, ensure_ascii=False), status=code,
                                 content_type="application/json; charset=utf-8",
                                 headers={"Cache-Control": "no-store", "X-Content-Type-Options": "nosniff"})
    if req.method == "GET" and req.path == "/health":
        return reply(200, {"status": "ok", "project": PROJECT})
    if req.method == "GET" and req.path in ("/mail/callback/google", "/mail/callback/microsoft"):
        try:
            mail_connect.callback(firestore.client(), req.path.rsplit("/", 1)[1], req.args)
            message = "Correo conectado. Vuelve a Bio para iniciar la sincronización."
        except mail_connect.MailError as error:
            message = str(error)
        except Exception:
            message = "No se completó la conexión. Vuelve a Bio e inténtalo de nuevo."
        return https_fn.Response("<!doctype html><html lang='es'><meta charset='utf-8'><meta name='viewport' content='width=device-width'><title>Correo · Bio</title><body><h1>Bio</h1><p>" + html.escape(message) + "</p></body></html>",
            headers={"Cache-Control": "no-store", "Referrer-Policy": "no-referrer", "Content-Security-Policy": "default-src 'none'; frame-ancestors 'none'", "X-Content-Type-Options": "nosniff"}, content_type="text/html; charset=utf-8")
    mail_path = req.path in ("/mail/status", "/mail/connect", "/mail/disconnect", "/mail/scan", "/mail/download")
    if req.method != "POST" or (not mail_path and req.path not in ("/analyze-document", "/research")):
        return reply(404, {"error": "No encontrado"})
    header = req.headers.get("Authorization", "")
    if not header.startswith("Bearer "):
        return reply(401, {"error": "Inicia sesión en Bio"})
    try:
        identity = auth.verify_id_token(header[7:], check_revoked=True)
    except Exception:
        return reply(401, {"error": "Sesión no válida; vuelve a iniciar sesión"})
    if req.mimetype != "application/json":
        return reply(415, {"error": "Usa application/json"})
    if not req.content_length or not 0 < req.content_length <= 15 * 1024 * 1024:
        return reply(413, {"error": "Documento demasiado grande o vacío"})
    if mail_path:
        if identity.get("firebase", {}).get("sign_in_provider") == "anonymous":
            return reply(403, {"error": "Crea una cuenta de Bio para conectar el correo."})
        try:
            if req.content_length > 32768 or not isinstance(req.get_json(), dict):
                return reply(400, {"error": "Solicitud inválida"})
            return reply(200, mail_connect.dispatch(firestore.client(), identity["uid"], req.path, req.get_json()))
        except mail_connect.MailError as error:
            return reply(error.status, {"error": str(error)})
        except Exception:
            return reply(503, {"error": "No se pudo completar la conexión de correo. Inténtalo de nuevo."})
    analysis = req.path == "/analyze-document"
    try:
        parts, config = prepare(req.get_json(), analysis)
    except (ValueError, TypeError, KeyError):
        return reply(400, {"error": "Revisa el documento y los datos enviados"})
    try:
        if not reserve_quota(firestore.client(), identity["uid"]):
            return reply(429, {"error": "Se alcanzó el límite diario de análisis"})
        with genai.Client(vertexai=True, project=PROJECT, location="global",
                          http_options=types.HttpOptions(timeout=240000)) as client:
            result = client.models.generate_content(model=MODEL,
                contents=[{"role": "user", "parts": parts}], config=config)
        return reply(200, extract(result.model_dump(mode="json", exclude_none=True), analysis, MODEL))
    except Exception:
        # Never expose/log clinical payloads, credentials or provider response bodies.
        return reply(502, {"error": "No se completó el análisis; el documento original se conserva"})
