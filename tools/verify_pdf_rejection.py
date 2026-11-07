"""Exercise deployed PDF preflight with synthetic files; never request generation."""
import json
from pathlib import Path
import sys
import requests

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "server"))
from test_document_input import synthetic_pdf, payload


def main():
    config = json.loads(Path("app/src/main/assets/firebase-config.json").read_text())
    reviewer = json.loads(Path(".secrets/play-review.json").read_text())
    assert config["projectId"] == reviewer["project"] == "bio-project-15d15"
    session = requests.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword",
        params={"key": config["apiKey"]}, json={"email": reviewer["email"],
        "password": reviewer["password"], "returnSecureToken": True}, timeout=30)
    assert session.status_code == 200, "Reviewer login failed"
    assert session.json()["localId"] == reviewer["uid"]
    headers = {"Authorization": "Bearer " + session.json()["idToken"]}
    base = "https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api"
    def status():
        response = requests.post(base + "/billing/status", headers=headers, json={}, timeout=45)
        assert response.status_code == 200, "Billing unavailable"
        body = response.json()
        assert body["premium"] and body["accessSource"] == "google_play_review"
        return body["usage"]
    before = status()
    for raw, code in ((synthetic_pdf(encrypted=True), "document_password_required"),
                      (b"%PDF-1.4 broken synthetic fixture", "document_unreadable")):
        response = requests.post(base + "/analyze-document", headers=headers,
                                 json=payload(raw), timeout=45)
        assert response.status_code == 422, "Expected PDF preflight rejection"
        assert response.json().get("code") == code, "Unexpected rejection code"
        print("PASS " + code, flush=True)
    assert before == status(), "Usage changed unexpectedly"
    print("PASS monthly allowances unchanged; synthetic files only", flush=True)


if __name__ == "__main__":
    main()
