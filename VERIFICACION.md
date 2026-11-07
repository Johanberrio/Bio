## Preparación 0.6.12-preview — suscripciones (no desplegada)

- Google Play Billing 8.3.0, pantalla de planes, restauración y verificación del servidor con cuenta ofuscada. Control de IA en ambos endpoints, carga manual sin análisis para Normal y pausa de importación de correo basada en IA.
- 18 pruebas Python de suscripciones/adaptador/documentos aprobadas. assembleDebug y lintDebug correctos. Compra real, firma de producción y recorrido Play aún pendientes.
- Añadida dependencia Fragment 1.8.9 para corregir conflicto detectado por lint tras añadir Billing.
- Sin nuevo APK entregado, sin despliegue del servidor, sin modificación del modelo de producción. BIO_PREMIUM_READY deshabilitado por defecto. Véase docs/PREMIUM.md.

## Actualización 0.6.11 — Medidas en Inicio

- Peso, estatura y grupo sanguíneo en tres columnas de una sola fila. Eliminados los textos de procedencia, fecha y estado debajo de las medidas. Sin cambios en la sincronización automática.
- assembleDebug y lintDebug: BUILD SUCCESSFUL. No se realizó comprobación visual en dispositivo en esta actualización.
- Bio-0.6.11.apk (versionCode 26), checksum docs/Bio-0.6.11.sha256.

## Actualización 0.6.10 — peso automático frente al perfil manual

- La última lectura válida de Health Connect sustituye el peso manual incluso si el perfil se editó después. Se conservan decimales, propietario, fecha y reglas de orden entre lecturas automáticas.
- Peso se lee antes de otros parámetros. Inicio muestra origen, fecha y estado; refresco tras Health Connect sin esperar al correo. Sin permiso o sin registros no se aplica la caché como si fuera una lectura nueva.
- assembleDebug, assembleDebugAndroidTest y lintDebug correctos. WeightTests incorpora 8 comprobaciones, incluido 71 manual -> 70,8 de Health Connect. Ejecución no verificada: la instalación en emulator-5554 no finalizó durante la comprobación. No se afirma validación real con Google Fit del usuario.
- Bio-0.6.10.apk (versionCode 25), checksum docs/Bio-0.6.10.sha256.

## Actualización 0.6.9 — distribución adaptable

- AdaptiveColumn calcula márgenes con el ancho real disponible y centra una columna de hasta 720 dp; aplicado a Inicio, secciones principales, autenticación, perfil, correo, fuentes y formularios.
- AdaptiveRow apila medidas, tarjetas de signos vitales y acciones de citas cuando el ancho útil es inferior a 340 dp o fontScale >= 1.3. Navegación inferior y días del calendario dejan de tener alturas rígidas. Ilustraciones ajustadas al ancho conservando proporciones mediante FIT_CENTER y el lienzo del cuerpo.
- Insets incluyen recortes de pantalla y teclado en pantallas secundarias; autenticación contempla Android 26–29. Botón de apariencia compacto con etiqueta accesible cuando aumenta la fuente.
- assembleDebug, assembleDebugAndroidTest y lintDebug: BUILD SUCCESSFUL. Emulador Medium_Phone_API_36.1: LayoutTests ejecutado, 18 combinaciones aprobadas (280, 320, 360, 411, 600 y 840 dp; fuente 1.0, 1.3 y 2.0). Comprueba componentes adaptables: ancho, apilado y altura del texto. No equivale a validación visual de todas las pantallas en todos los fabricantes.
- Bio-0.6.9.apk, versionCode 24, SHA256 docs/Bio-0.6.9.sha256. Sin cambios del servidor.

## Actualización 0.6.8 — estilo y separación del peso

- BioButton compartido por acciones de Inicio y perfil. Botones de perfil basados en TextView con semántica accesible de botón, sin tintado nativo; selectores con colores y tipografía explícitos. Cambiar perfil tiene fondo de superficie contrastante sobre la tarjeta lila.
- Peso retirado de las opciones y formulario de signos vitales. Health Connect conserva su permiso y lectura automática, guarda en weights y actualiza el perfil/Inicio. Migración idempotente desde vitals que preserva registros e identidad. Selección antigua de métrica acotada a las opciones actuales.
- assembleDebug, assembleDebugAndroidTest y lintDebug completados correctamente. Comprobaciones de migración y deduplicación compiladas; no ejecutadas en teléfono. Validación visual en dispositivo pendiente.
- APK Bio-0.6.8.apk, versionCode 23, checksum docs/Bio-0.6.8.sha256. Sin cambios de servidor.

## Actualización 0.6.7 — Inicio, perfil y peso automático

- Imagen femenina de Inicio copiada sin cambios y verificada por SHA256. Se selecciona por bodyModel. «Cambiar perfil» pasa al Inicio y desaparece del mapa corporal.
- Perfil con logo, tarjetas, campos redondeados, colores de Bio y soporte de modo oscuro.
- WeightRecord de Health Connect incorporado a permisos y lecturas, métrica 7 en kg. La medición válida más reciente actualiza el perfil y su respaldo. Conserva origen y fecha, rechaza otra cuenta y fechas futuras, respeta un peso manual posterior y admite correcciones del mismo registro.
- Se mantiene el trabajo automático periódico existente y la lectura al abrir la app. Necesita autorización de Peso y lectura en segundo plano, y que la app del dispositivo comparta mediciones con Health Connect.
- assembleDebug, assembleDebugAndroidTest y lintDebug: BUILD SUCCESSFUL confirmado el 4 de octubre de 2026. Comprobaciones instrumentadas compiladas, no ejecutadas en teléfono. Pendiente validación visual y sincronización real en el Samsung del usuario.
- Bio-0.6.7.apk, versionCode 22, checksum docs/Bio-0.6.7.sha256. Sin cambios de servidor.

## Actualización 0.6.6 — reproductivo, medidas y avisos

- Imágenes reproductivas específicas de mujer y hombre, copiadas sin modificación y verificadas por SHA256 contra los dos originales del usuario. Se ajustan las máscaras pélvicas y se elimina la referencia compartida endocrina/urinaria.
- Inicio muestra peso (kg) y estatura (cm) del perfil; valores ausentes se muestran como «Sin registrar».
- Los rechazos de correo se presentan como avisos, no como importaciones pendientes. Caducan a los siete días, se pueden descartar individualmente o en conjunto y conservan mailSeen para no volver a consumir análisis. Las nuevas entradas guardan propietario y huella; se ocultan si el archivo ya está importado. Avisos anteriores sin huella requieren descarte o caducidad; no se emparejan por nombre.
- Gradle assembleDebug, assembleDebugAndroidTest y lintDebug: BUILD SUCCESSFUL. Nuevas comprobaciones compiladas para resolución, caducidad, descarte, aislamiento por fuente, unidades y recursos reproductivos. No ejecutadas en dispositivo: adb no muestra equipos conectados. Verificación visual en teléfono pendiente.
- APK Bio-0.6.6.apk, versionCode 21. SHA256 en docs/Bio-0.6.6.sha256. Sin despliegue de servidor.

# Verificación de Bio

## Actualización 0.6.5 — sistemas femeninos

- Siete PNG suministrados copiados sin modificaciones y verificados por SHA256 contra los originales. Se usa la última de las dos versiones urinarias. No se recibió lámina ósea femenina: no se sustituye por masculina; se indica pendiente. La referencia reproductiva procede de la región pélvica endocrina femenina.
- Selector persistente bodyModel en perfil, opcional para compatibilidad con cuentas existentes, respaldado por el mecanismo Firebase del perfil. Mi cuerpo cambia recursos, capas combinadas y máscaras de iluminación según ese valor. No se añade el modelo visual al contexto clínico de IA.
- assembleDebug, assembleDebugAndroidTest y lintDebug aprobados. Comprobaciones añadidas para persistencia del selector, compatibilidad de perfiles previos, validación y recursos por modelo. Instrumentación compilada, no ejecutada: adb no encontró dispositivos. No se afirma revisión visual ni precisión anatómica de las ilustraciones/máscaras.
- APK Bio-0.6.5.apk (versionCode 20), SHA256 docs/Bio-0.6.5.sha256. Sin cambios de servidor.
## Actualización 0.6.4 — 3 de octubre de 2026

- Tu correo en Bio adopta logo, tarjetas, tipografía, botones morados, estados verdes y modo oscuro de Bio. Se conservan conexión, sincronización y desconexión.
- Inicio de sesión: Cuida tu salud. Mi cuerpo: CONOCE TU CUERPO sobre Tu mapa personal.
- assembleDebug y lintDebug aprobados. APK Bio-0.6.4.apk, versionCode 19; SHA256 en docs/Bio-0.6.4.sha256. Sin cambios de servidor. Pendiente revisión visual en dispositivo.
## Actualización 0.6.3 — Fuentes y dispositivos

- Misma paleta y tipografía de MainActivity, logo Bio transparente, tarjetas con bordes redondeados, acciones moradas y estado de dispositivos verde. Respeta la preferencia darkMode de MainActivity y los márgenes del sistema.
- Se elimina el bloque de SURA y su enlace del código de la app. Conservadas las acciones de Health Connect, selección de carpetas, sincronización y correo.
- assembleDebug y lintDebug aprobados. APK Bio-0.6.3.apk y SHA256 docs/Bio-0.6.3.sha256. Sin cambios del servidor. No se realizó revisión visual en emulador ni teléfono en esta entrega.
## Actualización 0.6.2 — filtro exclusivo por nombre del adjunto

- Se ignora el asunto para seleccionar candidatos. Se reconocen historia/historía, ayudas diagnósticas, resultados, exámenes y términos clínicos en nombres con otras palabras, tildes, puntuación, números, CamelCase y caracteres Unicode habituales.
- 55 pruebas Python aprobadas, incluida selección independiente entre varios adjuntos del mismo mensaje y rechazo de archivos genéricos aunque el asunto contenga palabras médicas.
- assembleDebug y lintDebug aprobados. APK Bio-0.6.2.apk y hash docs/Bio-0.6.2.sha256. bio:bio_api desplegado correctamente en bio-project-15d15.
- No se ha probado una importación real del buzón del usuario. Se mantienen análisis de contenido/titular, formatos PDF/JPG/PNG de hasta 10 MB y cuotas diarias actuales. El cambio de filtro no elimina un bloqueo por cuota.
## Corrección adicional del filtro Gmail — 2 de octubre de 2026

- Reporte: ESTEBAN BERRIO HISTORIA.pdf podía quedar fuera porque el filtro exigía historia clínica. Se reconocen HISTORIA, HISTORIAS e HISTORIAL como candidatos sin exigir clínica; se mantiene análisis posterior de contenido y titular.
- 54 pruebas Python aprobadas. Regresión con el nombre exacto, guiones bajos y variantes, manteniendo exclusión de adjuntos inline, tamaño excesivo y palabras no coincidentes.
- Despliegue de bio:bio_api completado correctamente en bio-project-15d15. Compatible con APK 0.6.1, sin recompilación Android. La importación real del documento del usuario sigue pendiente de comprobar.
## Corrección 0.6.1 — 2 de octubre de 2026

- Usuario confirma Gmail conectado, cero documentos y posterior desaparición del botón. El usuario precisó después el asunto «Se ha compartido información clínica» y el archivo historia_clinica_87845.pdf. Se reprodujo el rechazo del filtro anterior: no reconocía información clínica ni separadores de nombre de archivo. No se inspeccionó su buzón.
- Corregida recuperación local de cuentas confirmadas por el servidor, preservando desconexiones explícitas pendientes. Botón global «Sincronizar ahora» y consulta de conexiones antes de cada revisión manual.
- Resultado muestra estado por cuenta, adjuntos candidatos, elementos ya revisados, motivos de exclusión y perfil incompleto.
- Gmail admite mensajes marcados simultáneamente SENT e INBOX; continúa excluyendo spam, papelera y borradores.
- Servidor bio:bio_api desplegado correctamente con la corrección de nombres y asuntos clínicos.
- 53 pruebas Python aprobadas, incluida regresión con el asunto/nombre proporcionado y variantes con guiones, puntos y acentos. assembleDebug y lintDebug aprobados. APK Bio-0.6.1.apk y SHA256 en docs/Bio-0.6.1.sha256. Sin prueba visual en dispositivo ni importación real confirmada.
## Activación de Gmail — 2 de octubre de 2026

- Gmail API habilitada en el proyecto Bio. JSON OAuth web validado: proyecto correcto y URI de retorno exacta. Credenciales guardadas y verificadas en la configuración privada del servidor; no se imprimieron secretos ni se incluyeron en el APK. `.secrets/` y los archivos `client_secret_*.json` excluidos en `.gitignore`.
- Respuesta real del servidor: `google: true`, `microsoft: false`. Con cuentas temporales: inicio OAuth correcto, PKCE, URI fija, cancelación y rechazo de estado reutilizado; acceso a `_mailConfig/google` denegado (403) al usuario Firebase. Cuentas sintéticas eliminadas al finalizar.
- No se ha completado consentimiento de un buzón real ni importación de correo de extremo a extremo. Pendientes usuarios de prueba/permisos en Google y conexión desde Bio. No requiere un APK nuevo respecto a 0.6.0; la disponibilidad se obtiene del servidor.

## Actualización 0.6.0 — preparada el 2 de octubre de 2026

- Implementados adaptadores OAuth Gmail/Microsoft con PKCE, estado de un solo uso, cuentas vinculadas al UID validado y secretos fuera del APK. Pantalla de múltiples cuentas, desconexión y revisión automática Android por lotes. Clasificación de tipo, nombre, fecha e identidad del titular antes de incorporar documentos al historial. Exclusiones visibles para revisión en el correo.
- **51 pruebas Python aprobadas**, incluidas 18 pruebas nuevas de correo. Son pruebas con respuestas simuladas; no verifican el consentimiento real de los proveedores.
- `assembleDebug`, `assembleDebugAndroidTest` y `lintDebug` completados correctamente. Se añadieron seis comprobaciones Android de admisión/exclusión de documentos, compiladas pero **no ejecutadas** en esta entrega. No se da por aprobado un flujo visual ni una prueba en Samsung.
- Función `bio:bio_api` desplegada en `bio-project-15d15`. Prueba HTTP real: sin sesión 401; sesión anónima 403; cuenta registrada recibe estado y lista vacía; proveedor no configurado devuelve 503; ID de cuenta inválido rechazado. Dos cuentas sintéticas eliminadas al finalizar; no se accedió a buzones reales.
- Estado real devuelto: `google: false`, `microsoft: false`. **Activación y prueba de extremo a extremo pendientes** de credenciales OAuth y acceso a las consolas. La herramienta del navegador quedó bloqueada, incluida la apertura de Google Cloud. No se crearon registros OAuth ni consentimientos de correo.
- APK de desarrollo: `Bio-0.6.0.apk`, 31.302.424 bytes. SHA-256 en `docs/Bio-0.6.0.sha256`. Puede instalarse sobre la versión anterior sin borrar datos. Instrucciones y límites en `docs/CORREO_DIRECTO.md`.

## Actualización 0.5.2 — 1 de octubre de 2026

- Consulta los permisos reales antes de solicitar acceso. Con todos concedidos, muestra confirmación y opciones de sincronización/ajustes. Después de una solicitud vuelve a consultar Android y admite acceso parcial.
- Estado de mediciones visible junto a los botones. Sincronización de salud independiente del análisis de documentos, con resultado explícito cuando no hay datos nuevos. El estado transitorio de solicitud ya no se restaura como bloqueado.
- APK y Android Lint aprobados. APK `Bio-0.5.2.apk`, 31.156.960 bytes; SHA-256 en `docs/Bio-0.5.2.sha256`.
- Se añadió una prueba de permisos previamente concedidos y respuesta visible. **La ejecución de integración de esta versión no quedó aprobada**: dos intentos fueron interrumpidos por avisos del emulador «Pixel Launcher isn't responding» y «Process system isn't responding». La segunda comprobación exige explícitamente el paquete de Health Connect/PermissionController. Las cuentas sintéticas se limpiaron sin errores reportados. No se alcanzó la nueva comprobación de sincronización con permisos concedidos.
- Pendiente validar en Samsung A25 con Android 16. Instalar como actualización, sin borrar datos ni desinstalar.

## Actualización 0.5.1 — 1 de octubre de 2026

- Corrección del bloqueo al ocultarse MainActivity durante navegación interna. BioApplication sigue las actividades visibles de la aplicación; mantiene el bloqueo al salir normalmente. Las pantallas externas iniciadas por el usuario desde Fuentes tienen una ventana de retorno en memoria de cinco minutos.
- Fuentes deja de cerrarse en onStop. Controla indisponibilidad y errores de apertura de Health Connect; distingue Android 14+ de versiones que usan la aplicación separada. Lectura normal y permiso de segundo plano se solicitan por separado. Se añade acceso a ajustes de Health Connect.
- APK y Android Lint aprobados. **31 comprobaciones de AuthFlowTests aprobadas** con Firebase real y datos sintéticos. Incluyen abrir la pantalla real de permisos en Android 16, cancelar, regresar sin bloqueo, navegar entre pantallas, vencimiento de la ventana de retorno y bloqueo ordinario. Cuenta y perfil de prueba eliminados al terminar.
- Reporte del usuario: Samsung A25 con Android 16. La prueba se realizó en emulador Android 16; el equipo Samsung físico no estuvo conectado. El cierre específico del fabricante requiere confirmación instalando la actualización.
- Entrega: `Bio-0.5.1.apk`; hash en `docs/Bio-0.5.1.sha256`. Instalar sobre 0.5.0 sin desinstalar ni borrar datos.

## Actualización 0.5.0 — verificada el 30 de septiembre de 2026

- APK y Android Lint completados correctamente. Se resolvieron incompatibilidades de Kotlin/Firebase y Guava/WorkManager, y dos errores de constantes de orientación.
- **76 comprobaciones Android aprobadas**, incluidas validación del perfil, deduplicación de lecturas y documentos, conservación de fecha/origen, y prevención de reimportación de registros eliminados. Una ejecución inicial se interrumpió por reinicio del sistema del emulador; la repetición completa pasó.
- **23 comprobaciones de autenticación aprobadas**, con cuenta sintética real: registro conservando UID, perfil obligatorio, escritura y lectura del perfil en Firestore, contexto sin nombre/fecha de nacimiento, rechazo de contraseña incorrecta, reautenticación y apertura de Fuentes. Cuenta y perfil sintéticos eliminados al terminar.
- **33 pruebas del servidor aprobadas**, incluyendo contexto de perfil, exclusión de identidad/secretos, validación de datos y procedencia de mediciones.
- Reglas de Firestore y función `bio:bio_api` desplegadas correctamente en `bio-project-15d15`. Perfil accesible por su propietario; análisis con contexto de edad, estatura, peso y grupo sanguíneo declarado.
- APK: `Bio-0.5.0.apk`, 31.267.731 bytes. SHA-256: `02ace112ebd4dbe58d06c2545386dfb100060020d71ccb2aae59c0148f03ca23`. Firma de desarrollo; instalar como actualización sin borrar los datos.
- Capturas del perfil y de Fuentes revisadas visualmente en `docs/screenshots/v050/`. Formulario legible, campos completos y pantalla de fuentes desplazable; sin datos de pacientes reales.
- Límites: no se probó un dispositivo médico/reloj físico ni la recepción durante horas en segundo plano. La importación desde carpetas utiliza la cadena de análisis existente, pero no se hizo una nueva prueba de extremo a extremo con un proveedor SAF real y Gemini en esta versión. Outlook no está integrado por el bloqueo de registro en Microsoft Entra; SURA solo abre el portal e importa archivos descargados en una carpeta autorizada. Detalles en `docs/PERFIL_Y_FUENTES.md`.

Las secciones siguientes documentan entregas anteriores.

Fecha: 25 de septiembre de 2026.

Nombre actualizado a **Bio**. Las imágenes de `docs/screenshots/files/` corresponden a la revisión visual inicial. Las imágenes actuales están en `docs/screenshots/v022/files/`.

## Actualización 0.4.0 — inicio de sesión

- Registro y acceso con correo/contraseña; vinculación de cuentas anónimas con `linkWithCredential`, conservando el UID. Recuperación de contraseña mediante Firebase.
- Puerta de acceso antes de renderizar datos clínicos; bloqueo en memoria al salir y al reiniciar el proceso. La sincronización no comienza sin cuenta autenticada.
- Biometría AndroidX con `BIOMETRIC_STRONG` y una operación AES en Android Keystore autenticada por uso. Alternativa con contraseña, desactivación desde ajustes y eliminación de la activación al cerrar sesión.
- Compilación y Android Lint aprobados. Regresión: 62 comprobaciones Android aprobadas.
- La disponibilidad de huella y rostro depende del dispositivo. No se validó un sensor facial ni una huella física; pendiente de comprobación en hardware compatible.

- Integración real: 17 comprobaciones aprobadas en `AuthFlowTests`, incluyendo registro con vinculación al UID anónimo, puerta de acceso, contraseña oculta, contraseña incorrecta, reautenticación correcta y bloqueo al recrear la actividad. Cuenta sintética eliminada y sesión cerrada al terminar.
- Capturas: `docs/screenshots/v040/login-register.png` y `login-password.png`. No contienen datos de pacientes reales.
- APK final: `Bio-0.4.0.apk`, 29.629.303 bytes; SHA-256 en `docs/Bio-0.4.0.sha256`. Compilación de desarrollo. Instalar como actualización, sin desinstalar ni borrar datos.
- Reproducir autenticación: compilar `assembleDebugAndroidTest -PbioAuthTest` e iniciar `com.alvia.health.test/com.alvia.health.AuthFlowTests` en un emulador dedicado vacío. Las pruebas de regresión omiten la puerta de acceso explícitamente dentro de la instrumentación; no existe un bypass en el APK.

## Actualización 0.3.2 — carga automática

- Eliminados el popup previo y el botón de cuenta/sincronización de documentos. Firebase crea una sesión privada automáticamente.
- El documento se publica después de identificar contenido, nombre y fecha y completar el guardado. Los fallos conservan el original cifrado como carga pendiente.
- Reintentos con análisis reutilizado cuando solo falla Storage/Firestore; conservación del propietario del archivo para posteriores operaciones.
- 62 comprobaciones Android aprobadas: incluye conservación del original, fallo de análisis, fallo de subida, reintento sin repetir IA y publicación solo después del éxito. Compilación y Android Lint aprobados.

- Prueba real desde Android: 19 comprobaciones aprobadas con dos PDFs sintéticos elegidos desde el flujo de importación. Nombres obtenidos: “Hemograma completo · 2026-09-01.pdf” y “Consulta de neurologia · 2026-08-25.pdf”. Sesión anónima automática, análisis real, carga a Storage/Firestore y ausencia de botones manuales verificados. Documentos y cuenta temporal eliminados al terminar.
- Capturas finales inspeccionadas: `docs/screenshots/v032/automatic-exam.png` y `automatic-history.png`.
- APK final: `Bio-0.3.2.apk`, 28.616.124 bytes. SHA-256 en `docs/Bio-0.3.2.sha256`. Instalar como actualización; no borrar datos ni desinstalar.
- Reproducir integración: `gradlew assembleDebugAndroidTest -PbioLiveTest`; instalar app y APK de tests en un emulador sin documentos ni sesión, ejecutar `adb shell am instrument -w com.alvia.health.test/com.alvia.health.DocumentFlowTests`. Para regresión, compilar sin esa propiedad y ejecutar `com.alvia.health.SmokeTests`.

## Actualización 0.3.1 — 27 de septiembre de 2026

- Firebase propio `bio-project-15d15`: Blaze, correo/contraseña, Storage y Firestore activos; reglas desplegadas.
- Función `bio_api` desplegada, endpoint de estado responde 200; solicitudes de análisis sin sesión responden 401.
- Prueba real con dos cuentas temporales: subir/descargar PDF idéntico, acceso entre cuentas y anónimo denegados en Storage y Firestore. Archivos, metadatos y cuentas de prueba eliminados.
- Análisis real de un PDF sintético: nombre “Hemograma completo”, fecha 2026-09-01 y hallazgos con citas correctas. Investigación real con tres fuentes HTTPS. No se transmitieron datos de pacientes reales.
- APK incorpora endpoint y autenticación Firebase, sin token compartido ni claves privadas.
- Compilación final y Lint aprobados; 53 comprobaciones Android en emulador y 29 pruebas Python aprobadas. APK `Bio-0.3.1.apk`: 28.537.576 bytes; SHA-256 en `docs/Bio-0.3.1.sha256`. Se verificó que el APK contiene el proyecto y endpoint reales, sin token compartido.

## Actualización 0.3.0 — preparación de documentos y Firebase

- Compilación Android y Lint aprobados con SDK Firebase BoM 34.19.0; AndroidX habilitado.
- 53 comprobaciones Android aprobadas: incluye nueva pantalla Historia para archivos, renombrado desde extracción sintética, preservación del nombre original y tratamiento de fechas desconocidas.
- 19 pruebas de servidor aprobadas: salida estructurada, restricciones de evidencia, fechas imposibles/futuras y documentos ilegibles, además de las pruebas de investigación existentes.
- Las pruebas son sintéticas; NO validan exactitud médica. No se llamó a la IA real ni se conectó un proyecto Firebase. Las reglas se prepararon pero no se desplegaron ni se probaron en el emulador de Firebase.
- APK de prueba: `Bio-0.3.0-prueba.apk`. Faltan configuración pública del proyecto, despliegue de reglas y servidor IA con credenciales para activar los servicios. Ver `docs/FIREBASE_Y_DOCUMENTOS.md`.

## Actualización 0.2.5

- Nuevo PNG del encabezado con transparencia alfa verificada y copia idéntica por SHA-256.
- Logo reducido y compartido por las cinco secciones; sin texto adicional ni cambios al lanzador.
- assembleDebug y lintDebug aprobados. No se repitieron las pruebas de instrumentación para este cambio de recurso y tamaño.
- APK: `Bio-0.2.5.apk`.

## Actualización 0.2.4

- Encabezado compartido: nueva imagen JPG en las cinco secciones, sin texto Bio añadido al lado.
- Copia del original verificada por SHA-256; lanzador mantiene el recurso bio_logo.
- assembleDebug y lintDebug aprobados. Cambio visual limitado; no se repitió la instrumentación de la versión anterior.
- APK: `Bio-0.2.4.apk`.

## Actualización 0.2.3

- Compilación y Android Lint aprobados, 46 comprobaciones Android aprobadas.
- Incluye febrero bisiesto, navegación diciembre/enero, clasificación por especialidad, selección explícita sobre detección y agrupación de dos citas en un día.
- Calendario mensual revisado visualmente; capturas en `docs/screenshots/v023/files/`. Las citas de muestra se eliminan al terminar la prueba.
- APK: `Bio-0.2.3.apk`. Se quitó el texto Bio de todos los encabezados. Sustitución del icono del encabezado pendiente: el mensaje no incluía la imagen nueva.

## Actualización 0.2.2 (26 de septiembre de 2026)

- APK y pruebas compilados; Android Lint sin errores bloqueantes.
- 39 comprobaciones Android aprobadas, incluido el cambio real desde el botón a modo oscuro y la persistencia de la preferencia.
- Capturas de las cinco pantallas oscuras, inicio claro y formulario oscuro; revisión visual de portada, mapa corporal y formulario.
- Nueva imagen de portada idéntica al archivo proporcionado (SHA-256). Corazón vectorial solo en Inicio; recursos del lanzador conservados.
- Título «TU MAPA PERSONAL» a 32 sp, sin el título y subtítulo anteriores; etiqueta PREMIUM retirada.
- APK de desarrollo: `Bio-0.2.2.apk`.

## Actualización 0.2.1 (26 de septiembre de 2026)

- APK compilado, Android Lint sin errores bloqueantes.
- 34 comprobaciones Android aprobadas; se verifica además que las máscaras tengan tejido visible, rojo para dolor, amarillo para incomodidad y transparencia en el fondo.
- Revisión visual de la superposición de nueve sistemas sobre una sola figura y de la iluminación de rodilla y zona urinaria con registros sintéticos.
- Se retiran letras, círculos y vistas en mosaico. Los contornos extraídos son aproximados; las ilustraciones originales no constituyen capas anatómicas perfectamente alineadas.
- APK instalable de desarrollo: `Bio-0.2.1.apk`.

## Historial: actualización 0.2.0: imágenes y asociación de síntomas

- APK de aplicación y APK de instrumentación compilados correctamente; Android Lint sin errores bloqueantes.
- **37 comprobaciones de asociación de síntomas aprobadas**: dolor/incomodidad, múltiples sistemas, lateralidad corporal, negaciones, descripciones no reconocidas, información repetida y referencias respiratorias que requieren atención.
- **30 comprobaciones Android aprobadas**: incluyen las comprobaciones originales y, además, apertura del formulario, ausencia de selector de sistema, guardado real, asociación persistida, edición a resuelto, lectura de registros antiguos, alternativas textuales para colores, variación real de la animación, apagado del halo y carga de las nueve vistas.
- **9 pruebas del servidor aprobadas** tras ampliar el contexto con sistemas asociados, sensación y estado activo/resuelto.
- Las ocho imágenes coinciden byte a byte con los archivos entregados por el usuario (SHA-256). No se generaron ni alteraron los PNG.
- Revisión visual de `screen-2.png`, `body-associations.png` y `body-all-systems.png`. Estas dos últimas son vistas de prueba: la primera utiliza síntomas sintéticos en memoria; la segunda muestra todas las ilustraciones sin síntomas. No son datos de pacientes.

La asociación automática funciona mediante reglas locales en español y **no está validada clínicamente**. Estas pruebas verifican el comportamiento del código; no prueban que una asociación sea una explicación médica correcta. El reconocimiento lingüístico no es exhaustivo y las zonas son aproximadas. La app conserva los casos sin asociación para completar detalles. El modo combinado compara imágenes en paralelo.

La primera ejecución de la nueva prueba del formulario falló porque la prueba pulsaba Guardar antes del evento de apertura del diálogo. Se corrigió la sincronización de la prueba y la ejecución completa posterior pasó las 30 comprobaciones.

## Comprobaciones de la entrega inicial

- Compilación de APK de desarrollo y APK de instrumentación con Gradle 8.13 / AGP 8.13.2 / SDK 36.
- Android Lint: sin errores bloqueantes. Quedan advertencias de actualización de herramientas/SDK, internacionalización y optimización de vistas. La interfaz de esta versión está en español.
- **9 pruebas Python aprobadas**: autenticación, servidor sin credenciales, endpoint de salud, limpieza de IDs locales, unidades, búsqueda web y almacenamiento desactivado en la solicitud, formato de documentos, validación de entradas y extracción de fuentes.
- **5 comprobaciones de recordatorios aprobadas**: día previo, mismo día, hora anterior, cita cercana a medianoche y cambio de horario de verano.
- **13 comprobaciones Android aprobadas** en `Medium_Phone_API_36.1`, Android 16: persistencia entre aperturas, actualización sin duplicación, cifrado, descifrado, rechazo de manipulación, documento cifrado, lectura mediante ContentProvider, cinco pantallas y navegación adicional.
- Revisión visual de Inicio, Mi cuerpo y Mi salud en imágenes renderizadas desde las vistas de la app. Las imágenes de las cinco pantallas están en `docs/screenshots/files/`.

Los tests Android crean y eliminan datos sintéticos, no incluyen historias clínicas reales y no llaman a servicios de IA. El dispositivo de prueba es un emulador; no se instaló en un teléfono físico.

## Límites de la verificación

- No se efectuaron solicitudes reales a OpenAI: faltan credenciales del usuario, servidor HTTPS y token configurados. La integración está implementada y sus transformaciones/validaciones se prueban sin consumo de API.
- No se probó la entrega de notificaciones en tiempo real durante una noche, después de un reinicio ni bajo restricciones de batería de fabricantes. Se probó el cálculo de horarios; la recepción end-to-end queda pendiente en dispositivos físicos.
- La lectura del proveedor de documentos se comprobó técnicamente. Queda pendiente una matriz de lectores PDF e imágenes externos.
- Las pruebas de pantallas son de integración básica; no constituyen una auditoría de accesibilidad, pruebas completas de todos los formularios ni evaluación de usabilidad.
- No se validó clínicamente ningún algoritmo. El mapa organiza datos registrados; no evalúa función de órganos. No hay diagnóstico autónomo ni monitorización mediante sensores.
- No se implementaron pagos, suscripciones, cuentas multiusuario ni publicación en Google Play.

## Artefactos

- APK instalable de desarrollo: `app/build/outputs/apk/debug/app-debug.apk`.
- Código y configuración de Android Studio: directorio raíz del proyecto.
- Instrucciones de apertura, servidor y alcance: `README.md`.
- Informe detallado de Android Lint: `app/build/reports/lint-results-debug.html`.

La firma de desarrollo permite probar el APK, pero no sustituye una firma de publicación.


## Publicación y cuotas — 29 octubre 2026, 0.6.16-preview (31)

- Servidor: 82 pruebas unitarias aprobadas, incluidas cuotas concurrentes 30/20, aislamiento por cuenta y mes, devolución única, límites de entrada y rutas de IA.
- Despliegue activo verificado. Pruebas reales sin inferencia: autenticación, bloqueo de IA para Normal, cuotas 30/20 devueltas por billing/status, denegación de acceso cliente a contadores; cuentas temporales eliminadas. OAuth Gmail/PKCE/cancelación/replay y secretos privados aprobados, sin acceder a correo real.
- Gradle: assembleRelease, bundleRelease, assembleDebug, assembleDebugAndroidTest, lintDebug y lintVitalRelease aprobados. Lint conserva advertencias existentes; no equivale a ausencia total de avisos.
- Emulador nuevo aislado Android 16/API 36: 116 comprobaciones Android aprobadas (documentos, cifrado, perfil/peso, filtros de correo y pantallas). Se corrigió una expectativa antigua del test que todavía buscaba TU MAPA PERSONAL; el comportamiento vigente y solicitado es Tu mapa personal.
- El primer emulador carecía de espacio y no instaló el APK nuevo; el ensayo contra su APK anterior no se considera validación de esta versión. No se borraron sus datos. Emulador aislado creado en .tools/qa-avd.
- Compilados Bio-0.6.16-preview.apk y .aab. SHA256 en docs/Bio-0.6.16-preview.sha256. No subidos aún a Play.
- Ficha: textos en borrador, categoría Medicina guardada para revisión y soporte juandrak@msn.com guardado. Pendientes y política provisional en docs/PUBLICACION_PLAY.md. El titular todavía debe reunir a los 12 verificadores.
- Falta comprobación real de sincronización Gmail y Health Connect con la versión nueva; las pruebas locales simulan datos y no sustituyen dispositivos/cuentas conectadas.

- Capturas 1080×1920 RGB 24 bits verificadas y cuatro subidas/guardadas en la ficha de Play. Evidencia: docs/play-capturas-guardadas.png. La repetición de las 116 comprobaciones en ese tamaño también pasó.


## Privacidad y eliminación — 30 octubre 2026, 0.6.17-preview (32)

- 92 pruebas de servidor aprobadas. Borrado real de cuenta sintética, documentos, objetos, OAuth ficticio, cuotas y marcador final. Trabajador privado con permisos separados, programación OIDC y activación comprobados.
- Pantalla Android clara/oscura revisada; confirmación explícita, contraseña oculta y contraseña incorrecta rechazadas. La solicitud válida llegó al servidor y `clearApplicationUserData` retiró el directorio de archivos. La terminación reportada por Instrumentation es esperada por el borrado; se verificó independientemente la cola y se retiró la cuenta temporal.
- Se corrigió el enmascaramiento del campo tras el primer ensayo fallido; se repitió satisfactoriamente. No se modificaron cuentas reales ni se invocó IA.
- APK/AAB release y debug, prueba Android y lint aprobados. Artefactos `Bio-0.6.17-preview.apk` y `.aab`, hashes en `docs/Bio-0.6.17-preview.sha256`. No subidos aún a Play.
- Política publicada, enlazada en la app y guardada en Play; explicación de retención técnica Storage (7 días) y Firestore (1 hora), marcador mínimo 30 días. Mayor detalle: `docs/ELIMINACION_CUENTA.md`.
