"""Count a synthetic PDF through the deployed SDK path. Never generate content."""
import base64
import json
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "server"))
from google import genai
from google.genai import types
from google.oauth2.credentials import Credentials
from vertex_gateway import prepare
import prompt_budget

stage = "prepare"
try:
    token = sys.stdin.read().strip()
    pdf = Path("output/pdf/resultado_prueba_bio.pdf").read_bytes()
    parts, config = prepare({"mailImport": True,
                            "patient": {"fullName": "Bio Review Test", "birthDate": "1995-01-01"},
                            "document": {"mime": "application/pdf", "data": base64.b64encode(pdf).decode()}}, True)
    stage = "client"
    with genai.Client(vertexai=True, project="bio-project-15d15", location="global",
                      credentials=Credentials(token=token),
                      http_options=types.HttpOptions(timeout=30000,
                          retry_options=types.HttpRetryOptions(attempts=1))) as client:
        stage = "input_size"
        prompt_budget.check(client, "gemini-3.1-flash-lite", parts, config["system_instruction"])
    print(json.dumps({"sdkInputCheck": "passed", "generationCalled": False}))
except Exception as error:
    print(json.dumps({"sdkInputCheck": "failed", "stage": stage,
                      "errorType": type(error).__name__, "providerCode": getattr(error, "code", None)}))
    sys.exit(1)
