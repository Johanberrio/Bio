"""Structural PDF checks before quota reservation or calls to the AI provider."""
from io import BytesIO
import logging
from pypdf import PdfReader

# Parser warnings can contain fragments of the document. Never forward them to
# Cloud Logging; the API emits only fixed error codes and fixed user messages.
_logger = logging.getLogger("pypdf")
_logger.addHandler(logging.NullHandler())
_logger.propagate = False


class DocumentInputError(ValueError):
    def __init__(self, code, message):
        super().__init__(message)
        self.code = code


def validate_pdf(data):
    try:
        reader = PdfReader(BytesIO(data))
        if reader.is_encrypted:
            raise DocumentInputError("document_password_required",
                "El PDF está protegido. Necesitas una copia sin contraseña para importarlo. El archivo original no se ha modificado.")
        if not len(reader.pages):
            raise ValueError("No pages")
        for page in reader.pages:
            # Resolve each page dictionary without extracting clinical text or
            # decompressing potentially large image/content streams.
            if page.get("/Type") != "/Page":
                raise ValueError("Invalid page")
    except DocumentInputError:
        raise
    except Exception:
        raise DocumentInputError("document_unreadable",
            "El PDF está dañado o no se puede abrir correctamente. Descarga una copia que abra todas sus páginas y vuelve a subirla.") from None
