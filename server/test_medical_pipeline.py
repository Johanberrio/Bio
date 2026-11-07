import copy
import unittest
from unittest.mock import Mock, patch
from test_document_analysis import sample
from medical_pipeline import enrich, interpret, parse_review


class MedicalPipelineTests(unittest.TestCase):
    def test_only_complete_final_json_is_accepted(self):
        final = '{"possible_interpretations": [], "limitations": "No constan alergias."}'
        expected = {"possible_interpretations": [], "limitations": "No constan alergias."}
        self.assertEqual(parse_review(final, "stop"), expected)
        self.assertEqual(parse_review("```json\n" + final + "\n```", "stop"), expected)
        self.assertEqual(parse_review("<unused94>thought\ninternal text<unused95>" + final, "stop"), expected)
        for reason in ("length", None, "error"):
            with self.assertRaises(ValueError):
                parse_review(final, reason)
        for content in ("", "<unused94>thought incomplete", "[]", "Explanation: " + final,
                        "```json\n" + final + "\n``` trailing", "```json\n" + final,
                        final + " trailing", '{"possible_interpretations": [], "limitations": "a", "limitations": "b"}',
                        '{"possible_interpretations": [""], "limitations": ""}',
                        '{"possible_interpretations": [], "limitations": null}'):
            with self.assertRaises(ValueError):
                parse_review(content, "stop")

    def test_laboratory_evidence_is_preserved_even_if_review_omits_it(self):
        item = dict(sample(), clinical_date="2026-06-05", date_evidence="Toma: 05/06/2026",
                    findings=[{"label": "Glucosa", "quote": "92 mg/dL; referencia 70–99 mg/dL",
                               "page": 1, "status": "documented"}])
        result = enrich(item, provider=Mock(return_value={"possible_interpretations": [], "limitations": "No consta ayuno."}))
        self.assertEqual(result["clinical_date"], item["clinical_date"])
        self.assertEqual(result["findings"], item["findings"])

    def test_review_cannot_change_extracted_evidence(self):
        original = sample()
        before = copy.deepcopy(original)
        provider = Mock(return_value={"possible_interpretations": ["Hallazgo que requiere valoración."], "limitations": "Documento aislado."})
        result = enrich(original, {"ageYears": 30}, provider=provider)
        self.assertEqual(original, before)
        for key in ("name", "clinical_date", "findings", "date_evidence", "summary"):
            self.assertEqual(result[key], before[key])
        self.assertEqual(provider.call_args.args[0]["profile"], {"ageYears": 30})

    def test_wrong_patient_and_nonclinical_skip_medical_cost(self):
        for kind, match in (("history", "other"), ("exam", "unknown"), ("other", "matches"), ("uncertain", "matches")):
            item = dict(sample(), document_kind=kind, patient_match=match)
            provider = Mock()
            self.assertEqual(enrich(item, provider=provider)["possible_interpretations"], [])
            provider.assert_not_called()

    def test_no_findings_skip_medical_cost(self):
        item = dict(sample(), findings=[])
        provider = Mock()
        enrich(item, provider=provider)
        provider.assert_not_called()

    def test_provider_failure_has_no_general_model_fallback(self):
        with self.assertRaises(TimeoutError):
            enrich(sample(), provider=Mock(side_effect=TimeoutError))

    def test_rejects_attempt_to_overwrite_name_or_invalid_review(self):
        for review in (
            {"name": "Inventado", "possible_interpretations": [], "limitations": ""},
            {"possible_interpretations": "diagnóstico", "limitations": ""},
            {"possible_interpretations": ["x" * 2001], "limitations": ""},
        ):
            with self.assertRaises(ValueError):
                enrich(sample(), provider=Mock(return_value=review))

    def gemini_client(self, review, reason="STOP"):
        import json
        client = Mock()
        client.models.count_tokens.return_value.total_tokens = 1000
        client.models.generate_content.return_value.model_dump.return_value = {
            "candidates": [{"finish_reason": reason, "content": {"parts": [
                {"text": "private thinking", "thought": True}, {"text": json.dumps(review)}]}}]}
        return client

    def test_gemini_evidence_references_and_no_tools(self):
        client = self.gemini_client({"interpretations": [
            {"explanation": "Sospecha pendiente de confirmar.", "finding_ids": [1]}], "limitations": "Documento aislado."})
        output = interpret(sample(), client, "gemini-3.1-flash-lite")
        self.assertIn("[Hallazgos: 1]", output["possible_interpretations"][0])
        config = client.models.generate_content.call_args.kwargs["config"]
        self.assertNotIn("tools", config)
        self.assertEqual(config["max_output_tokens"], 3000)
        self.assertEqual(config["response_mime_type"], "application/json")

    def test_gemini_rejects_bad_references_and_truncation(self):
        for refs in ([], [0], [2], [True], [1, 1], "1"):
            client = self.gemini_client({"interpretations": [
                {"explanation": "Posibilidad", "finding_ids": refs}], "limitations": ""})
            with self.assertRaises(ValueError):
                interpret(sample(), client, "model")
        for reason in ("MAX_TOKENS", "SAFETY", None):
            with self.assertRaises(ValueError):
                interpret(sample(), self.gemini_client({"interpretations": [], "limitations": ""}, reason), "model")

    def test_gemini_can_abstain_and_does_not_retry(self):
        client = self.gemini_client({"interpretations": [], "limitations": "No consta el estado de alergias."})
        self.assertEqual(interpret(sample(), client, "model")["possible_interpretations"], [])
        client.models.generate_content.assert_called_once()
        client.models.generate_content.side_effect = TimeoutError
        with self.assertRaises(TimeoutError):
            interpret(sample(), client, "model")
