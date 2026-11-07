"""Vertex adapter; validates attachments before sending them to the model."""
import json
import base64
import document_analysis
from document_input import validate_pdf
from server import build_request


def prepare(data, analysis):
    body = document_analysis.request(data, build_request) if analysis else build_request(data)
    parts = []
    for item in body["input"][0]["content"]:
        if item["type"] == "input_text":
            parts.append({"text": item["text"]})
        else:
            url = item.get("file_data") or item["image_url"]
            prefix, encoded = url.split(",", 1)
            mime = prefix[5:].split(";")[0]
            raw = base64.b64decode(encoded, validate=True)
            if mime == "application/pdf":
                validate_pdf(raw)
            parts.append({"inline_data": {"mime_type": mime, "data": raw}})
    config = {"system_instruction": body["instructions"], "max_output_tokens": 6000 if analysis else 4000}
    if analysis:
        config.update(response_mime_type="application/json", response_json_schema=body["text"]["format"]["schema"])
    else:
        config["tools"] = [{"google_search": {}}]
    return parts, config


def extract(result, analysis, model):
    candidates = result.get("candidates", [])
    if not candidates or candidates[0].get("finish_reason") != "STOP":
        raise ValueError("Respuesta incompleta o bloqueada")
    candidate = candidates[0]
    text = "\n".join(p.get("text", "") for p in candidate.get("content", {}).get("parts", []) if not p.get("thought"))
    if not text.strip() or len(text) > 500000:
        raise ValueError("Respuesta vacía o demasiado grande")
    if analysis:
        return document_analysis.validate(json.loads(text))
    sources, seen = [], set()
    for chunk in candidate.get("grounding_metadata", {}).get("grounding_chunks", []):
        web = chunk.get("web", {})
        url = web.get("uri", "")
        if url.startswith("https://") and url not in seen:
            seen.add(url)
            sources.append({"title": web.get("title") or url, "url": url})
    return {"answer": text, "sources": sources, "model": model}
