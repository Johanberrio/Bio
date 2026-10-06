import base64
import copy
import unittest
from document_analysis import request, validate
from server import build_request

def sample():
    return {"readable":True,"name":"Hemograma","clinical_date":"2024-02-29","date_evidence":"Toma: 29/02/2024","summary":"Resultados disponibles","findings":[{"label":"Anemia en estudio","quote":"Se sospecha anemia","page":1,"status":"suspected"}],"possible_interpretations":[],"limitations":"Requiere valoración"}

class DocumentTests(unittest.TestCase):
    def test_no_web_or_storage(self):
        body=request({"document":{"mime":"application/pdf","data":base64.b64encode(b"%PDF-1.4 fake fixture").decode()}},build_request)
        self.assertNotIn("tools",body)
        self.assertFalse(body["store"])
        self.assertTrue(body["text"]["format"]["strict"])
    def test_preserves_suspicion(self):
        self.assertEqual(validate(sample())["findings"][0]["status"],"suspected")
    def test_invalid_date(self):
        value=sample();value["clinical_date"]="2023-02-29"
        with self.assertRaises(ValueError):validate(value)
    def test_missing_date_evidence(self):
        value=sample();value["date_evidence"]=""
        with self.assertRaises(ValueError):validate(value)
    def test_unknown_date_allowed(self):
        value=sample();value["clinical_date"]=None;value["date_evidence"]=""
        self.assertIsNone(validate(value)["clinical_date"])
    def test_unsupported_finding(self):
        value=sample();value["findings"][0]["quote"]=""
        with self.assertRaises(ValueError):validate(value)
    def test_unreadable_no_inferences(self):
        value=sample();value["readable"]=False
        with self.assertRaises(ValueError):validate(value)
    def test_foreign_extra_fields_rejected(self):
        value=sample();value["diagnosis_confirmed"]=True
        with self.assertRaises(ValueError):validate(value)
    def test_future_date_rejected(self):
        value=sample();value["clinical_date"]="9999-01-01"
        with self.assertRaises(ValueError):validate(value)
    def test_non_pdf_rejected(self):
        with self.assertRaises(ValueError):request({"document":{"mime":"application/pdf","data":base64.b64encode(b"not a pdf").decode()}},build_request)
