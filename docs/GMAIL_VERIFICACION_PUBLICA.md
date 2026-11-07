# Preparación de verificación pública de Gmail

Estado observado: 5 de noviembre de 2026. Documento de preparación, no aprobación de Google.

## Configuración comprobada

- Proyecto propio: `bio-project-15d15`. Nombre del consentimiento: Bio.
- Público externo en modo Prueba. Único permiso restringido declarado: `https://www.googleapis.com/auth/gmail.readonly`.
- Política guardada en Google Auth Platform: https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/privacy . Evidencia: `oauth-privacidad-guardada.png`.
- Falta la página principal pública y la verificación de propiedad del dominio. El responsable confirmó que no dispone de dominio propio. No se compró ni contrató ninguno.
- El centro de verificación falló al cargar; Marca y Acceso a los datos sí pudieron consultarse. No se envió una solicitud de verificación ni se cambió a producción.
- Una dirección gratuita de Hosting no debe presentarse como dominio verificado sin comprobar su aceptación por Google. El alojamiento gratuito por sí solo no resuelve la verificación OAuth.

## Justificación propuesta del permiso (para revisar antes de enviar)

Bio is an Android app that helps adults organize their own health documents. Users explicitly connect a Gmail account to import supported medical PDF, JPG and PNG attachments. Bio reads attachment metadata to identify candidate filenames, then retrieves eligible attachments for classification and import. It does not filter eligibility by email subject, send emails, modify labels, mark messages as read, move messages or delete emails. The gmail.readonly scope is required to retrieve attachments; metadata-only access cannot provide attachment contents. Imported documents remain private to the authenticated Bio user. Users can disconnect Gmail and request deletion of their Bio account.

Esta justificación debe acompañarse de la descripción completa del tratamiento en el servidor y Vertex AI; no afirmar que todo se procesa exclusivamente en el teléfono. La política y la pantalla de consentimiento deben describir el mismo flujo.

## Demostración pendiente, con datos ficticios

1. Mostrar Bio, su identidad y el acceso a la política de privacidad.
2. Conectar una cuenta de prueba autorizada desde «Tu correo en Bio»; mostrar consentimiento y permiso solicitado, sin grabar contraseñas ni códigos.
3. Recibir un PDF sintético con nombre `historia_clinica_prueba.pdf` y asunto genérico.
4. Sincronizar, mostrar clasificación/nombre/fecha y abrir el original ficticio.
5. Repetir la sincronización para comprobar ausencia de duplicados.
6. Mostrar desconexión y las opciones de eliminación de documentos y cuenta.

No se ha grabado ni enviado este vídeo. No usar historias reales, credenciales de revisión ni datos personales en la grabación.

## Condiciones antes de solicitar acceso público

- Publicar una página principal que explique Bio y enlace la misma política declarada en OAuth; verificar propiedad con una cuenta propietaria del proyecto.
- Confirmar todos los dominios de la configuración y las redirecciones del cliente OAuth. No cambiar el callback existente sin actualizar y probar el servidor.
- Presentar justificación, demostración y prácticas de tratamiento según el formulario de Google.
- Confirmar con Google la evaluación de seguridad aplicable al permiso restringido procesado en servidor. No asumir exención, coste cero ni aprobación automática; no contratar una evaluación sin autorización.
- Mantener Gmail en pruebas hasta resolver los requisitos. La aprobación de Google Play no aprueba automáticamente Gmail OAuth.

Fuentes oficiales:
- https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification
- https://support.google.com/cloud/answer/13807376
- https://support.google.com/cloud/answer/13804266
