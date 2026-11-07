# Acceso de revisión de Google Play

Preparación: 1 de noviembre de 2026. No se ha enviado la app a revisión.

Cuenta dedicada con un perfil adulto completamente ficticio (Bio Review Test). No contiene datos del propietario, documentos médicos reales ni un buzón conectado. La copia local de la contraseña aleatoria está en `.secrets/play-review.json`, excluido de Git. Con autorización explícita del responsable, se guardó también en los detalles de acceso privados de Play Console. No copiarla a documentación pública, registros ni capturas.

El acceso Premium se concede exclusivamente desde `_reviewAccounts/{uid}`, colección privada sin reglas de lectura/escritura para clientes. Exige UID coincidente, propósito `google_play_review`, `syntheticOnly: true` y `enabled: true`. No simula una compra de Play, no habilita compras públicas y conserva los cupos habituales de 30 análisis y 20 consultas al mes. Desactivar el registro revoca el acceso en la siguiente petición. Al eliminar la cuenta, el trabajador también elimina este registro.

El acceso y la contraseña no vencen automáticamente durante la revisión, de acuerdo con las instrucciones de acceso reutilizable de Play. Mantener la cuenta solo para revisión y revocarla si se retira esta distribución. No conectar correos personales ni introducir documentos reales en una cuenta compartida con revisores.

## Texto preparado para Play Console (inglés)

Nombre: `Bio Premium reviewer - synthetic account`

Instrucciones (menos de 500 caracteres):

> Sign in with the supplied email and password; do not create an account. Leave biometrics unchecked. No OTP is required. The adult profile is synthetic. Premium is enabled without a purchase, with 30 document analyses and 20 AI queries per calendar month. Use synthetic files for Bio Review Test. Gmail and Health Connect are optional sources, not sign-in requirements. Account menu: Premium and account deletion. Gmail production OAuth approval is still pending.

El responsable autorizó expresamente compartir esta cuenta ficticia con Google. Play Console confirmó el guardado del usuario, contraseña e instrucciones. Se desactivó el uso opcional de estas credenciales para pruebas en dispositivos de socios de confianza. Evidencia sin contraseña: `play-review-access-saved.png`. Todavía no se ha enviado la app a revisión; esta declaración de acceso no sustituye las comprobaciones pendientes de Gmail y Health Connect indicadas abajo.

## Verificaciones y límites

- 94 pruebas unitarias del servidor aprobadas; incluyen acceso limitado al UID, revocación del permiso de revisión y verificación normal de compras para los demás usuarios.
- `tools/prepare-play-review.cjs --verify` comprueba inicio de sesión, Premium, cupos 30/20, compras deshabilitadas y denegación de acceso directo a los permisos privados. Solo envía solicitudes vacías a las rutas de IA para comprobar su validación; no ejecuta modelos.
- Evidencia sin credenciales: `play-review-verification.json`.
- Verificación posterior al despliegue con otra cuenta temporal: Normal sigue bloqueado en ambas rutas de IA, no puede modificar cuotas y no tiene compras habilitadas. La cuenta temporal se eliminó; la cuenta de revisión se conserva para Google.
- El perfil remoto tiene los campos y propietario que usa Android. Aún debe verificarse el recorrido completo de esta cuenta en la versión distribuida por Play.
- Gmail sigue en configuración de pruebas OAuth; no afirmar que está aprobado para cualquier cuenta. La revisión pública de Gmail y la prueba real de importación siguen pendientes. Para revisar una importación sin un buzón personal hará falta un buzón ficticio autorizado en OAuth o completar la verificación pública de Google.
- Health Connect depende de registros y permisos existentes en un dispositivo compatible; las pruebas del servidor no sustituyen su verificación física.

Referencia de cuentas Firebase: https://docs.cloud.google.com/identity-platform/docs/reference/rest/v1/accounts/signUp
