# Declaración de Seguridad de datos de Bio

Guardada en Play Console para revisión durante la preparación del 1–2 de noviembre de 2026. No enviada a revisión ni publicada.

## Alcance

Auditados `app/build.gradle.kts`, FirebaseCloud, Health Connect, consultas del cliente, servidor y política pública. Incluye datos enviados fuera del dispositivo por Firebase Authentication/Firestore/Storage, el servidor y Vertex AI. Los proveedores actúan por cuenta de Bio; se aplicó la excepción de proveedores de servicio de Play a «Compartidos». Esto **no significa que los datos permanezcan en el teléfono**. Las consultas con Google Search son iniciadas por el usuario con aviso de transmisión.

No se declaró procesamiento efímero: no se puede garantizar que todos los proveedores eliminen el contenido inmediatamente de la memoria. No se declararon publicidad, venta de datos, revisión de seguridad independiente ni certificación médica.

| Tipo | Recopilación | Finalidades |
|---|---|---|
| Nombre | Necesaria | Funciones, cuenta, comprobación del titular de documentos |
| Correo | Necesaria | Autenticación, cuenta, seguridad |
| ID de usuario | Necesaria | Funciones, cuenta, seguridad y aislamiento |
| Otra información personal | Necesaria | Fecha de nacimiento/perfil, funciones y personalización |
| Información sanitaria | Necesaria | Perfil inicial, funciones, personalización y cuenta; documentos/Health Connect son opcionales |
| Historial de compras | Opcional | Premium, verificación y cuenta; Bio no recibe números de tarjeta |
| Ubicación aproximada | Opcional | Ciudad escrita para consultas/contexto; no GPS |
| Correos electrónicos | Opcional | Importación autorizada de Gmail |
| Fotos | Opcional | Documentos clínicos JPG/PNG |
| Archivos y documentos | Opcional | Almacenamiento/importación/análisis de PDF y adjuntos |
| Interacciones | Opcional | Operaciones de IA y control de cupos/abuso |
| Búsquedas en la app | Opcional | Consultas de investigación |
| Otro contenido generado por usuarios | Opcional | Notas/contexto que el usuario incluye en sus consultas |
| Diagnóstico técnico | Necesaria | Metadatos de SDK/plataforma para funcionamiento y mejora de Firebase; no significa diagnóstico médico |
| Dispositivo u otros IDs | Necesaria | Identificadores técnicos de conexión/IP para operación y prevención de abuso; no se usa ID publicitario |

No se declararon contactos, SMS, audio, video, navegación web ni inventario de aplicaciones. Las citas y signos vitales locales no se respaldan automáticamente como tales; el contexto elegido por el usuario puede salir con una consulta de IA. El peso sí actualiza el perfil respaldado.

## Eliminación y acceso

- HTTPS en tránsito. No afirmar cifrado de extremo a extremo.
- Correo y contraseña; desbloqueo biométrico opcional gestionado por Android. Bio no recibe plantillas biométricas. OAuth Gmail es una fuente opcional, no un método para crear la cuenta Bio.
- Eliminación de cuenta: https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/delete-account
- Eliminación parcial/instrucciones/contacto: https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/privacy
- La eliminación dentro de Android requiere distribuir la versión 0.6.17 (32), aún pendiente de cargar en Play. La eliminación web ya está disponible.
- Retención y recuperación técnica descritas en la política: Storage 7 días, Firestore una hora, marcador mínimo de eliminación 30 días. No se prometió eliminación instantánea de copias técnicas.

## Referencias

- https://support.google.com/googleplay/android-developer/answer/10787469
- https://firebase.google.com/docs/android/play-data-disclosure
- Política pública y código de Bio; dependencias sin Analytics, Ads ni Crashlytics explícitos.

Revisar la declaración ante cambios de SDK, nuevas fuentes, analítica o proveedores. Antes de revisión pública, completar OAuth Gmail y las pruebas reales pendientes; esta declaración no acredita su aprobación.
