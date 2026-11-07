# Correo directo de Bio

## Estado

Actualización del 5 de noviembre de 2026: Gmail está configurado y el responsable confirmó una importación real funcional después de identificar un PDF protegido/ilegible. El servidor rechaza estos archivos antes de usar IA o cupo; la versión 0.6.18-preview (33), ya publicada en prueba interna, permite continuar con los demás adjuntos. Ese último recorrido de cola todavía requiere comprobación en el teléfono. OAuth sigue en modo Prueba, sin aprobación de uso público. La política de privacidad ya está guardada en la marca de Google; faltan página principal, propiedad de dominio y requisitos de verificación. Véase `GMAIL_VERIFICACION_PUBLICA.md`.

Outlook/Hotmail permanece pospuesto por decisión del responsable; no continuar el alta de Azure ni solicitar tarjeta para Microsoft.

Implementados los adaptadores Gmail API y Microsoft Graph, autorización OAuth con PKCE, pantalla de cuentas y revisión periódica Android. Gmail tiene su cliente configurado; Microsoft sigue deshabilitado hasta registrar y habilitar su cliente OAuth propio de Bio. Un proveedor sin configuración aparece deshabilitado; no se simula una conexión exitosa.

Gmail y Google Workspace usan Google. Outlook, Hotmail y Microsoft 365 usan Microsoft. No se ha implementado IMAP ni compatibilidad universal con otros proveedores. No se necesita reenviar mensajes.

## Uso y límites

Fuentes y dispositivos → Conectar y gestionar correos. Cada cuenta autoriza lectura en el navegador oficial; Bio no pide la contraseña del correo. Regresar a Bio actualiza las conexiones. Si el flujo dura más de cinco minutos o Android termina el proceso, será necesario desbloquear Bio de nuevo y actualizar conexiones.

Primera revisión: últimos 30 días. Después usa ventanas temporales y paginación persistente, con un día de solapamiento para evitar huecos de indexación. Revisa cuentas por turnos. La revisión se programa con WorkManager, intervalo mínimo de 15 minutos sujeto a Android, conexión y batería. No es entrega instantánea ni un servicio de correo permanente en el servidor; el teléfono debe poder ejecutar Bio. Cerrar sesión detiene nuevas revisiones.

Se selecciona cada adjunto exclusivamente por su nombre, sin usar el asunto del correo. Se reconocen historia/historía, historias, historial, ayudas diagnósticas, resultado/resultados, examen/exámenes y términos de pruebas como hemograma, resonancia o ecografía. La normalización admite mayúsculas, tildes, puntuación, guiones bajos, números, caracteres de ancho completo y separadores invisibles habituales. Las palabras pueden acompañarse de nombres u otros términos. Un asunto médico no hace elegible un archivo sin palabras clave. El contenido determina su categoría final y titular; el filtro no confirma que un documento sea clínico. Nombres genéricos como archivo.pdf pueden requerir carga manual. Gmail excluye spam, papelera, borradores y enviados que no estén también en Recibidos; así admite pruebas enviadas a la misma cuenta. Graph consulta mensajes recibidos con adjuntos. Solo se admiten PDF/JPG/PNG no inline de hasta 10 MB; enlaces a portales, archivos comprimidos o protegidos requieren importación manual. No se siguen enlaces de los correos.

El análisis confirma tipo de documento (examen/historia), nombre y fecha clínica. Compara el nombre completo y fecha de nacimiento del perfil con el titular explícito del documento. No importa automáticamente identidades desconocidas/diferentes ni documentos no clínicos/ambiguos; el nombre original queda en «Para revisar en tu correo», sin guardar el archivo rechazado. Esta comparación es de IA, no una verificación de identidad certificada. Los documentos dudosos pueden importarse manualmente después de revisarlos.

El original aceptado queda cifrado localmente y pasa por la misma carga Firebase. No se publica listo hasta completar el guardado. La fecha de recepción no sustituye a la fecha clínica. IDs de mensaje/adjunto y hash del archivo evitan duplicados, incluso tras borrar un documento ya importado. Hay un máximo de un análisis nuevo de correo por revisión, compartiendo el cupo Premium de 30 análisis por cuenta y mes calendario UTC con la carga manual. Las 20 consultas de IA mensuales son un cupo separado. Las cuotas provisionales de 10/50 al día ya no describen el plan actual. El resto continúa en revisiones posteriores.

## Servidor y aislamiento

Todas las operaciones `/mail/*`, salvo el callback OAuth, requieren un ID token Firebase validado y una cuenta registrada. La identidad viene del token, nunca de un UID enviado por Android.

- Configuración privada: `_mailConfig/google` y `_mailConfig/microsoft`: `clientId`, `clientSecret`, `enabled`.
- Tokens de actualización: `_mailUsers/{uid}/accounts/{id}`. Nunca se devuelven a Android ni se incluyen en APK, documentos o respuestas de estado.
- Un único estado pendiente por usuario en `_mailPending/{uid}`, validado por hash, proveedor, caducidad de diez minutos y consumo transaccional único. PKCE S256; redirección HTTPS fija por proveedor.
- Firestore cifra en reposo. Las reglas existentes deniegan a todos los SDK de cliente estas colecciones privadas; solo el servicio con IAM y administradores autorizados acceden. No hay contraseñas de buzón ni claves de servicio en la app.
- La app no envía, mueve, marca como leído ni borra correos. Desconectar desactiva la revisión local y elimina el token del servidor. Google intenta además revocarlo; en Microsoft se puede retirar también el consentimiento desde la cuenta Microsoft. Una petición ya iniciada puede finalizar.
- Adjuntos, cuerpos, asuntos y tokens de acceso no se persisten en el adaptador de correo. Los adjuntos candidatos pasan por el servidor y Vertex AI para clasificación; los aceptados se respaldan en Firebase, según el funcionamiento existente de Bio.

## Activación y comprobaciones pendientes

Gmail API, cliente OAuth y permiso gmail.readonly están configurados. Callback actual: `https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/mail/callback/google`. No cambiarlo de forma aislada.

- Completar la verificación pública descrita en `GMAIL_VERIFICACION_PUBLICA.md`; el consentimiento de usuarios de prueba no equivale a aprobación pública.
- Comprobar en teléfono actualizado: importación sin duplicados, continuación tras PDF rechazado, interrupción/reinicio, desconexión y revocación.
- Microsoft continúa deshabilitado; no es un requisito de esta salida inicial.
- Los secretos siguen en configuración privada y no deben compartirse en chat ni archivos versionados.

Google considera `gmail.readonly` un permiso restringido. El uso público y la transmisión de estos datos al servidor pueden requerir verificación OAuth y evaluación de seguridad; las cuentas de prueba no equivalen a habilitación general. Las cuentas organizativas Microsoft pueden exigir aprobación de su administrador.

Referencias: [Gmail scopes](https://developers.google.com/workspace/gmail/api/auth/scopes), [OAuth web Google](https://developers.google.com/identity/protocols/oauth2/web-server), [Microsoft OAuth](https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow), [Graph Mail.Read](https://learn.microsoft.com/en-us/graph/permissions-reference#mailread).
