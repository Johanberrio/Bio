# Gmail: PDF protegido o ilegible — 3 noviembre 2026

El diagnóstico seguro del proveedor mostró un rechazo HTTP 400 durante el conteo de entrada, antes de la generación. Las comprobaciones con un PDF sintético legible funcionaron. El usuario confirmó que el adjunto requiere contraseña o no abre correctamente.

## Corrección

- `document_input.py` comprueba estructura, cifrado y páginas del PDF antes de reservar cuota o llamar al proveedor. No extrae texto clínico ni registra mensajes del parser. Responde HTTP 422 con `document_password_required` o `document_unreadable`.
- La respuesta pide una copia sin protección o que abra correctamente. No solicita contraseñas y no altera el original.
- En Android, `MailApi.ApiException` conserva el estado HTTP y el código. `MailSync` acusa recibo únicamente de esos dos rechazos definitivos, guarda un aviso temporal en pendientes y continúa con los demás adjuntos. Los errores transitorios, de sesión, de cuota o del proveedor siguen siendo reintentables.
- Los fallos de sincronización se muestran como revisión incompleta, en lugar de limitarse a «0 documentos nuevos».
- Una copia corregida puede subirse manualmente o enviarse como un nuevo adjunto para sincronizar. El adjunto rechazado conserva su marca de revisión para evitar ciclos.

## Verificación

- 98 pruebas de servidor aprobadas, incluidos PDF cifrado, vacío, dañado y válido. Los rechazos no reservan cuota ni instancian el cliente de IA.
- APK y AAB 0.6.18-preview (33) compilados y firmados; `lintVitalRelease` aprobado. Huellas actualizadas en `Bio-0.6.18-preview.sha256`.
- El salto de adjuntos necesita esta nueva versión Android. Los archivos locales todavía no actualizan la versión instalada desde Google Play. No se realizó una prueba en el teléfono del usuario.
- `tools/verify_pdf_rejection.py` permite comprobar los dos códigos en el servicio publicado usando exclusivamente PDF ficticios y la cuenta aislada de revisión; verifica que el cupo no cambie.
- `bio_api` publicado correctamente. La verificación real confirmó ambos códigos HTTP 422 y el saldo mensual sin cambios. No se enviaron documentos del usuario ni se ejecutaron generaciones de IA.

## Distribución — 5 noviembre 2026

Se publicó el AAB 0.6.18-preview (código 33) en la prueba interna de Google Play. La consola confirmó «Disponible para verificadores internos» y lo mostró como versión más reciente a las 14:27 (Colombia). Sustituye al código 30; no cambia producción ni los verificadores. Evidencia: `play-bio-0618-publicada.png`.

Se verificó el SHA-256 del AAB antes de subirlo contra `Bio-0.6.18-preview.sha256`. Play aceptó el paquete sin errores bloqueantes; mostró únicamente avisos sobre archivo de desofuscación (la compilación no usa minificación) y símbolos de depuración nativos. La disponibilidad en el teléfono puede tardar en propagarse. No se hizo ninguna compra ni llamada de generación de IA durante la publicación.
