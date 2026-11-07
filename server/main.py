"""Firebase HTTPS entrypoint. Credentials stay in Google's service identity."""
import datetime
import json
import os
import html
import mail_connect
import play_billing
import medical_pipeline
import premium_quota
import prompt_budget
import account_deletion
import public_pages
from server import clinical_profile
from firebase_functions import https_fn, options, scheduler_fn
from firebase_admin import initialize_app, auth, firestore, storage
from google import genai
from google.genai import types
from vertex_gateway import prepare, extract
from document_input import DocumentInputError

PROJECT = "bio-project-15d15"
MODEL = os.environ.get("BIO_MODEL", "gemini-3.1-flash-lite")
REVIEW_MODEL = os.environ.get("BIO_REVIEW_MODEL", "gemini-3.1-flash-lite")
EXTRACTION_MODEL = os.environ.get("BIO_EXTRACTION_MODEL", "gemini-3.1-flash-lite")
initialize_app(options={"projectId": PROJECT})


def reserve_quota(db, uid, kind):
    return premium_quota.reserve(db, uid, kind)


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
    if req.method == "GET" and req.path in public_pages.PATHS:
        body, mime = public_pages.get(req.path)
        return https_fn.Response(body, content_type=mime, headers=public_pages.HEADERS)
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
    if req.method != "POST" or (not mail_path and req.path not in ("/analyze-document", "/research", "/billing/status", "/billing/verify", "/account/deletion-request")):
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
    if req.path == "/account/deletion-request":
        try:
            if req.content_length > 1024:
                return reply(400, {"error": "Solicitud inválida"})
            return reply(200, account_deletion.request(firestore.client(), identity, req.get_json()))
        except account_deletion.DeletionError as error:
            return reply(403, {"error": str(error)})
        except Exception:
            return reply(503, {"error": "No se pudo registrar la eliminación. Inténtalo de nuevo."})
    try:
        if account_deletion.pending(firestore.client(), identity["uid"]):
            return reply(410, {"error": "Esta cuenta tiene una solicitud de eliminación. No se admite nueva actividad."})
    except Exception:
        return reply(503, {"error": "No se pudo comprobar el estado de tu cuenta."})
    if req.path.startswith("/billing/"):
        if identity.get("firebase", {}).get("sign_in_provider") == "anonymous":
            return reply(403, {"error": "Inicia sesión con tu cuenta de Bio."})
        try:
            data = req.get_json()
            if req.content_length > 8192 or not isinstance(data, dict):
                return reply(400, {"error": "Solicitud inválida"})
            result = (play_billing.verify(firestore.client(), identity["uid"], data.get("purchaseToken"))
                      if req.path == "/billing/verify" else play_billing.status(firestore.client(), identity["uid"]))
            result["purchasesEnabled"] = play_billing.purchases_enabled(firestore.client(), identity["uid"])
            result["usage"] = premium_quota.summary(firestore.client(), identity["uid"])
            return reply(200, result)
        except play_billing.BillingError as error:
            return reply(403, {"error": str(error)})
        except Exception:
            return reply(503, {"error": "No se pudo consultar Google Play. Inténtalo de nuevo."})
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
    try:
        if not play_billing.status(firestore.client(), identity["uid"])["premium"]:
            return reply(402, {"error": "La IA está disponible con Bio Premium.", "code": "premium_required"})
    except Exception:
        return reply(503, {"error": "No se pudo verificar tu suscripción. Inténtalo de nuevo."})
    analysis = req.path == "/analyze-document"
    try:
        parts, config = prepare(req.get_json(), analysis)
        profile = clinical_profile(req.get_json().get("profile", {}))
    except DocumentInputError as error:
        return reply(422, {"error": str(error), "code": error.code})
    except (ValueError, TypeError, KeyError):
        return reply(400, {"error": "Revisa el documento y los datos enviados"})
    if analysis:
        config["system_instruction"] += medical_pipeline.EXTRACTION_ONLY
    selected_model = EXTRACTION_MODEL if analysis else MODEL
    reservation = None
    stage = "quota"
    try:
        reservation = reserve_quota(firestore.client(), identity["uid"], "analysis" if analysis else "research")
        if not reservation:
            reset = premium_quota.period()[1]
            return reply(429, {"error": "Agotaste tus análisis de este mes. Se renovarán el " + reset[:10] + "." if analysis else "Agotaste tus consultas de este mes. Se renovarán el " + reset[:10] + ".",
                               "code": "monthly_quota_exhausted", "resetAt": reset})
        stage = "client"
        with genai.Client(vertexai=True, project=PROJECT, location="global",
                          http_options=types.HttpOptions(timeout=120000,
                              retry_options=types.HttpRetryOptions(attempts=1))) as client:
            stage = "input_size"
            prompt_budget.check(client, selected_model, parts, config["system_instruction"])
            stage = "generation"
            result = client.models.generate_content(model=selected_model,
                contents=[{"role": "user", "parts": parts}], config=config)
            stage = "validation"
            output = extract(result.model_dump(mode="json", exclude_none=True), analysis, selected_model)
            if analysis:
                stage = "review"
                output = medical_pipeline.enrich(output, profile,
                    provider=lambda evidence: medical_pipeline.interpret(evidence, client, REVIEW_MODEL))
        return reply(200, output)
    except Exception as error:
        if reservation:
            try:
                premium_quota.refund(firestore.client(), identity["uid"], reservation)
            except Exception:
                pass  # Never mask the original error or log clinical content.
        if isinstance(error, prompt_budget.TooLarge):
            return reply(413, {"error": str(error), "code": "input_too_large"})
        # Fixed fields only: never log exception messages, tracebacks, request
        # bodies, account IDs or provider bodies (which may contain patient data).
        status_code = getattr(error, "code", None)
        print(json.dumps({"severity": "ERROR", "event": "bio_analysis_failed",
                          "stage": stage, "operation": "analysis" if analysis else "research",
                          "providerCode": status_code if type(status_code) is int else None}), flush=True)
        return reply(502, {"error": "No se completó el análisis; el documento original se conserva"})


@scheduler_fn.on_schedule(schedule="0 * * * *", region="us-east1", timeout_sec=540,
                          memory=options.MemoryOption.MB_512, min_instances=0, max_instances=1,
                          service_account="bio-erasure@bio-project-15d15.iam.gserviceaccount.com")
def bio_erase_accounts(event: scheduler_fn.ScheduledEvent) -> None:
    account_deletion.run_batch(firestore.client(), storage.bucket(account_deletion.BUCKET))
