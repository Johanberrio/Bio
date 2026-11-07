"""Bound paid model work without cutting off clinical source material."""
class TooLarge(ValueError):
    pass


def check(client, model, parts, instruction, limit=20000):
    result = client.models.count_tokens(model=model, contents=[{
        "role": "user", "parts": [{"text": instruction}, *parts]}])
    total = result.total_tokens
    if type(total) is not int or total <= 0:
        raise ValueError("Unable to measure input")
    if total > limit:
        raise TooLarge("El contenido supera la extensión admitida para un análisis. Divide el documento en partes completas más pequeñas.")
