import base64
import json
import os
import threading
import unittest
import urllib.request
import urllib.error
from unittest.mock import patch
from http.server import ThreadingHTTPServer
from server import build_request, extract_answer, Handler


class RequestTests(unittest.TestCase):
    def test_minimal_request_disables_storage_and_enables_search(self):
        result = build_request({"query": "Opciones para conversar con mi médico"})
        self.assertFalse(result["store"])
        self.assertEqual(result["tools"], [{"type": "web_search"}])
        self.assertEqual(result["tool_choice"], "required")
        self.assertEqual(len(result["input"][0]["content"]), 1)

    def test_context_has_units_and_removes_local_ids(self):
        result = build_request({"query": "Investigar", "vitals": [{"metric": 0, "value": 74, "created": 1, "id": "secret-id"}]})
        text = result["input"][0]["content"][0]["text"]
        self.assertIn("lpm", text)
        self.assertNotIn("secret-id", text)

    def test_pdf_and_image_input(self):
        docs = [{"mime": "application/pdf", "name": "informe.pdf", "data": base64.b64encode(b"%PDF-1.7 test").decode()},
                {"mime": "image/png", "data": base64.b64encode(b"\x89PNG\r\n\x1a\ntest").decode()}]
        result = build_request({"query": "Investigar", "documents": docs})
        content = result["input"][0]["content"]
        self.assertEqual(content[1]["type"], "input_file")
        self.assertEqual(content[2]["type"], "input_image")

    def test_invalid_inputs(self):
        for payload in ([], {"query": ""}, {"query": 4}, {"query": "x", "records": "bad"},
                        {"query": "x", "documents": [{"mime": "application/pdf", "data": "!"}]},
                        {"query": "x", "documents": [{"mime": "application/pdf", "data": "YQ=="}]},
                        {"query": "x", "vitals": [{"metric": -1}]}, {"query": "x" * 4001}):
            with self.subTest(payload=str(payload)[:80]), self.assertRaises(ValueError):
                build_request(payload)

    def test_citations_preserved_and_unsafe_urls_removed(self):
        citation = {"type": "url_citation", "title": "Fuente", "url": "https://example.org/evidence"}
        result = extract_answer({"status": "completed", "output": [{"type": "message", "content": [{"type": "output_text", "text": "Informe", "annotations": [citation, citation, {"type": "url_citation", "url": "javascript:alert(1)"}]}]}]})
        self.assertEqual(result["answer"], "Informe")
        self.assertEqual(len(result["sources"]), 1)

    def test_incomplete_response_is_not_presented_as_complete(self):
        with self.assertRaises(ValueError):
            extract_answer({"status": "incomplete", "output": []})


class GatewayTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.url = f"http://127.0.0.1:{cls.server.server_port}"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()

    def test_health_without_secrets(self):
        with urllib.request.urlopen(self.url + "/health") as response:
            self.assertEqual(json.load(response), {"status": "ok"})

    @patch.dict(os.environ, {"ALVIA_ACCESS_TOKEN": "a" * 32, "OPENAI_API_KEY": ""})
    def test_authentication_required(self):
        req = urllib.request.Request(self.url + "/research", data=b'{}')
        with self.assertRaises(urllib.error.HTTPError) as caught:
            urllib.request.urlopen(req)
        self.assertEqual(caught.exception.code, 401)

    @patch.dict(os.environ, {"ALVIA_ACCESS_TOKEN": "a" * 32, "OPENAI_API_KEY": ""})
    def test_unconfigured_provider_is_explicit(self):
        req = urllib.request.Request(self.url + "/research", data=b'{}', headers={"Authorization": "Bearer " + "a" * 32})
        with self.assertRaises(urllib.error.HTTPError) as caught:
            urllib.request.urlopen(req)
        self.assertEqual(caught.exception.code, 503)


if __name__ == "__main__":
    unittest.main()
