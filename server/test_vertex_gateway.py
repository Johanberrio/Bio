import base64
import json
import unittest
from vertex_gateway import prepare, extract
from test_document_analysis import sample

class VertexTests(unittest.TestCase):
    def test_pdf_and_no_tools_for_analysis(self):
        encoded = base64.b64encode(b"%PDF-1.4 synthetic").decode()
        parts, config = prepare({"document": {"mime": "application/pdf", "data": encoded}}, True)
        self.assertEqual(parts[1]["inline_data"]["data"], b"%PDF-1.4 synthetic")
        self.assertNotIn("tools", config)
        self.assertEqual(config["response_mime_type"], "application/json")

    def test_invalid_attachment_never_reaches_provider(self):
        with self.assertRaises(ValueError):
            prepare({"document": {"mime": "image/png", "data": "ZmFrZQ=="}}, True)

    def test_truncated_json_rejected(self):
        with self.assertRaises(ValueError):
            extract({"candidates": [{"finish_reason": "MAX_TOKENS"}]}, True, "model")

    def test_validated_findings(self):
        result = {"candidates": [{"finish_reason": "STOP", "content": {"parts": [{"text": json.dumps(sample())}]}}]}
        self.assertEqual(extract(result, True, "model")["findings"][0]["status"], "suspected")

    def test_sources_filtered(self):
        result = {"candidates": [{"finish_reason": "STOP", "content": {"parts": [{"text": "Informe"}]},
            "grounding_metadata": {"grounding_chunks": [{"web": {"uri": "javascript:bad"}}, {"web": {"uri": "https://example.org"}}]}}]}
        self.assertEqual(len(extract(result, False, "model")["sources"]), 1)
