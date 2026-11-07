"""Small public privacy/deletion surface; no cookies, telemetry or third-party JS."""
from pathlib import Path
PATHS = {"/privacy": ("privacy.html", "text/html; charset=utf-8"),
         "/delete-account": ("delete-account.html", "text/html; charset=utf-8"),
         "/account.js": ("account.js", "text/javascript; charset=utf-8"),
         "/privacy.css": ("privacy.css", "text/css; charset=utf-8"),
         "/account-config": ("account-config.json", "application/json; charset=utf-8")}
HEADERS = {"Cache-Control": "no-store", "Referrer-Policy": "no-referrer",
           "X-Content-Type-Options": "nosniff",
           "Content-Security-Policy": "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self' https://identitytoolkit.googleapis.com; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"}


def get(path):
    name, mime = PATHS[path]
    return (Path(__file__).parent / "public" / name).read_text(encoding="utf-8"), mime
