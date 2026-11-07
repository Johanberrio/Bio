import base64
from io import BytesIO
import unittest
from pypdf import PdfWriter
from document_input import DocumentInputError, validate_pdf


def synthetic_pdf(encrypted=False, empty=False):
    writer = PdfWriter()
    if not empty:
        writer.add_blank_page(width=200, height=200)
    if encrypted:
        writer.encrypt("synthetic-test-password")
    target = BytesIO()
    writer.write(target)
    return target.getvalue()


def payload(data):
    return {"document": {"mime": "application/pdf", "data": base64.b64encode(data).decode()}}


class PdfInputTests(unittest.TestCase):
    def test_valid_pdf_preserved(self):
        from vertex_gateway import prepare
        raw = synthetic_pdf()
        parts, _ = prepare(payload(raw), True)
        self.assertEqual(parts[1]["inline_data"]["data"], raw)

    def test_rejects_protected_empty_and_broken(self):
        for raw, code in ((synthetic_pdf(encrypted=True), "document_password_required"),
                          (synthetic_pdf(empty=True), "document_unreadable"),
                          (b"%PDF-1.4 broken", "document_unreadable")):
            with self.subTest(code=code), self.assertRaises(DocumentInputError) as error:
                validate_pdf(raw)
            self.assertEqual(error.exception.code, code)
