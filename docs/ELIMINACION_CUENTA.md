# Eliminación de cuenta de Bio

Implementación verificada el 30 de octubre de 2026, Android 0.6.17-preview (32).

## Accesos

- App: Tu cuenta → Eliminar cuenta. Se requiere confirmar la contraseña y marcar la casilla.
- Web pública: https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/delete-account
- Política: https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/privacy
- Recuperación de acceso: pantalla de inicio de sesión; alternativa de soporte juandrak@msn.com.

## Flujo y alcance

1. Firebase verifica una sesión de contraseña de menos de cinco minutos. El servidor toma el UID del token, nunca de datos proporcionados por el cliente. La solicitud es idempotente y no requiere Premium.
2. `_accountDeletion/{uid}` bloquea nuevas solicitudes al servidor y la finalización de conexiones OAuth. `bio_erase_accounts`, privado y ejecutado cada hora mediante Cloud Scheduler autenticado, deshabilita Auth, revoca sesiones y desconecta correo.
3. Espera al menos dos horas desde la deshabilitación para que expiren tokens emitidos antes de eliminar datos. Este intervalo evita que sincronizaciones en curso repongan datos inmediatamente después de borrarlos.
4. Elimina todos los objetos activos y versiones bajo `users/{uid}/`, así como documentos y subcolecciones de `users`, `_mailUsers`, `_mailPending`, `_playSubscriptions`, `_billingTesters` y `_premiumUsage`. Retira los antiguos contadores `_analysisQuota/{día}/users/{uid}` sin eliminar agregados de otros usuarios. Finalmente elimina la cuenta Auth.
5. Conserva solo UID/fechas/estado durante 30 días, repite limpieza diariamente y después elimina el marcador. Si falla Storage, Firestore o Auth, reintenta y no presenta el trabajo como terminado.
6. Android limpia sus datos con `clearApplicationUserData()` después de recibir la aceptación. Una marca persistida permite reintentar esa limpieza al arrancar si el proceso se interrumpe. La terminación del proceso durante la prueba de este paso es esperada; se comprobó además que el directorio `files` desaparece y que la solicitud existe en el servidor.

No elimina los correos originales, los registros originales de Health Connect ni archivos exportados a otras apps. La web no puede borrar físicamente copias locales sin conexión de otros teléfonos; la política explica cómo hacerlo desde Ajustes. La eliminación de Bio no cancela la suscripción de Play.

## Retención comprobada

- Storage: soft delete de 604800 segundos (7 días). Sin política de retención obligatoria ni versionado habilitado en la consulta.
- Firestore: recuperación ampliada deshabilitada, ventana de versiones de 3600 segundos, sin programación de backups.
- El marcador de eliminación no contiene correo, documento ni mediciones.
- La política distingue estas retenciones técnicas de los registros propios de Google Play y otros servicios.

## Seguridad y operación

Cuenta de servicio dedicada `bio-erasure`: Firestore y Firebase Auth en el proyecto Bio; Storage Object Admin únicamente en el bucket de Bio. No se crearon claves. La identidad de análisis conserva sus permisos anteriores. La activación comprueba que el trabajador no permita invocación pública y que el programador use OIDC.

`_accountConfig/deletion.enabled` se activó después de verificar el flujo real. Los clientes no pueden leer ni escribir estas colecciones privadas. Los errores de ejecución se registran sin UID ni datos médicos y se reintentan; revisar los fallos de Cloud Scheduler/Cloud Logging durante la operación.

## Evidencia

- `eliminacion-verificacion.json`: prueba real contra Firebase con cuenta y archivos sintéticos; verificación de cuenta borrada, generaciones de objetos, subcolecciones y caducidad del marcador. No se accedió a correos reales ni se llamó a modelos de IA.
- `deletion-light.png`, `deletion-dark.png`: pantallas reales del emulador, únicamente cuenta ficticia.
- Prueba Android: casilla obligatoria, contraseña oculta, solicitud vacía rechazada, contraseña incorrecta rechazada, contraseña limpiada, solicitud aceptada, datos locales retirados. La cuenta ficticia y su marcador se retiraron también del servidor.
- 92 pruebas unitarias del servidor; se incluye rechazo de callbacks OAuth que intenten recrear conexiones durante una eliminación.
- Compilación APK/AAB y lint de 0.6.17 completados. Esta versión no se ha subido aún a la pista interna de Play.
