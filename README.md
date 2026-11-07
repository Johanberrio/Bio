# Bio · Android MVP

## Estado actual: 0.6.19-preview (34)

Actualizado el 7 de noviembre de 2026. Código Android, servidor, reglas Firebase, pruebas e imágenes de la app. APK y AAB de esta versión se distribuyen como archivos de la versión en GitHub, fuera del historial del código.

- Google Play: disponible en prueba interna. La publicación fue rechazada por el requisito de cuenta de organización; no está aprobada para producción.
- Gmail: importación confirmada por el responsable con un PDF legible; verificación OAuth para acceso público pendiente.
- Health Connect: el responsable confirmó que la sincronización funciona. El peso mantiene decimales y se presenta en Inicio, separado de los signos vitales.
- Premium: 30 análisis de documentos y 20 consultas por cuenta y mes calendario UTC. Gmail comparte el cupo de análisis. COP 13.000/mes en Colombia y USD 5/mes en los mercados USD configurados.
- La versión 34 corrige el menú de cuenta para mostrar «Eliminar cuenta». Compilaciones APK/AAB, lint de release y prueba del menú en emulador aprobados; ver [verificación](docs/MENU_CUENTA_0619.md).

### Configuración al clonar

Usa Android Studio, JDK 17 o superior y Android SDK 36. Antes de ejecutar, completa localmente:

1. `app/src/main/assets/firebase-config.json`, a partir de `firebase/firebase-config.example.json`.
2. `app/src/main/assets/backend-config.json`, a partir del ejemplo de la misma carpeta.
3. `server/public/account-config.json`, a partir de `account-config.example.json`, para la página de eliminación de cuenta.
4. Variables y secretos del servidor según [Firebase y documentos](docs/FIREBASE_Y_DOCUMENTOS.md), [correo](docs/CORREO_DIRECTO.md) y [Premium](docs/PREMIUM.md).

Las claves de firma, credenciales OAuth, archivos de usuarios, configuraciones de producción y capturas privadas de las consolas están excluidos de Git. La firma de release requiere `.secrets/upload-signing.properties` y su almacén de claves local. No compartas esos archivos.

```powershell
.\gradlew.bat assembleDebug
python -m pip install -r server/requirements.txt
python -m unittest discover -s server
```

Los siguientes apartados conservan el historial del desarrollo. Las afirmaciones sobre pendientes y servicios no desplegados corresponden a su versión histórica; para el estado actual consulta el resumen anterior y [PUBLICACION_PLAY](docs/PUBLICACION_PLAY.md).

## Historial: preparación 0.6.17-preview (32)

APK y AAB generados: `Bio-0.6.17-preview.apk` / `Bio-0.6.17-preview.aab`. Aún no subidos a Google Play; la pista interna conserva 0.6.15 (30). Las instalaciones con firma de Play deben actualizarse desde Play, sin desinstalar para cambiar a este APK.

Servidor desplegado con 30 análisis y 20 consultas de IA por cuenta y mes calendario UTC. Gmail comparte los análisis; las reservas son transaccionales y los errores del servicio devuelven el cupo. La pantalla Premium muestra el saldo. Precios actuales: COP 13.000 en Colombia y USD 5 en los mercados USD configurados.

Estado y pendientes de publicación: [PUBLICACION_PLAY](docs/PUBLICACION_PLAY.md). Guion para reunir y coordinar verificadores: [PRUEBA_CERRADA](docs/PRUEBA_CERRADA.md). Política pública registrada en Play y eliminación de cuenta implementada y verificada: [detalle](docs/ELIMINACION_CUENTA.md). Público inicial confirmado: mayores de 18 años. Faltan formularios y verificaciones de Google, materiales de ficha y pruebas reales en el teléfono.


Aplicación nativa para Android Studio, escrita en Java 17 y Kotlin, con blanco como color principal, morado secundario y verde terciario. Interfaz en español, sin anuncios ni datos clínicos de demostración precargados.

## Actualización 0.6.15-preview (30)

Colombia mantiene un precio fijo de COP 13.000/mes. Premium consulta el precio localizado directamente a Google Play al abrir la pantalla, sin importes de referencia escritos en el APK. El 28 de octubre se habilitó el plan en 75 países o regiones que Play cobra en USD, a US$5,00/mes; las demás monedas siguen pendientes. Esta configuración no publica la app en producción. APK: `Bio-0.6.15-preview.apk`; paquete de Play: `Bio-0.6.15-preview.aab`.

## Historial: 0.6.14-preview (29)

Precio de referencia de Bio Premium actualizado a US$4 al mes. Google Play confirma el importe localizado antes de pagar. APK: `Bio-0.6.14-preview.apk`; paquete de Play: `Bio-0.6.14-preview.aab`.

## Historial: 0.6.13-preview (28)

Análisis de documentos organizado en Resumen, Resultados y Limitaciones. Cada hallazgo muestra su cita y página; los avisos extensos se pueden desplegar. Conserva los análisis existentes sin volver a llamar a la IA. Incluye mejoras de restauración de Premium. APK: `Bio-0.6.13-preview.apk`; paquete de Play: `Bio-0.6.13-preview.aab`.

Premium se verifica en el servidor con Google Play; la compra de prueba y el análisis de un documento sintético ya fueron comprobados. Estado y límites en [docs/PREMIUM.md](docs/PREMIUM.md).

## Historial: 0.6.11 — Medidas en Inicio

APK: `Bio-0.6.11.apk`. El último peso leído de Health Connect sustituye el valor manual del perfil y mantiene los decimales. Inicio muestra peso, estatura y grupo sanguíneo en una misma fila, sin textos de procedencia ni estado debajo. Se actualiza antes de la importación de correo. Márgenes adaptables, contenido centrado en pantallas amplias, tarjetas que se apilan en poco espacio o con texto grande, navegación y calendario sin alturas de texto rígidas, e ilustraciones ajustadas al ancho. Botones de perfil unificados con Bio, sin tintes nativos de Android; Cambiar perfil contrasta con la tarjeta de Inicio. El peso queda fuera de la lista y formulario de signos vitales, con almacenamiento independiente y migración de registros anteriores. Inicio usa la nueva ilustración femenina cuando el perfil es Mujer y contiene «Cambiar perfil». El perfil incorpora tarjetas y modo oscuro. Health Connect importa peso (kg), conserva su historial y actualiza el perfil con la lectura más reciente, sustituyendo el valor manual con la lectura de Health Connect. Requiere autorizar READ_WEIGHT y, para la app cerrada, lectura en segundo plano. Peso y estatura del perfil aparecen junto al cuerpo en Inicio. Los avisos de adjuntos rechazados se pueden descartar, caducan a los siete días y se ocultan si su huella coincide con un documento importado; se conserva la deduplicación. El perfil permite elegir Mujer/Hombre para el mapa visual. Se añaden siete láminas femeninas originales; se añaden láminas reproductivas propias para mujer y hombre y el óseo femenino queda pendiente de imagen. La elección se respalda con el perfil y no se usa como dato clínico inferido. Tu correo en Bio incorpora tarjetas, logo, botones morados y modo oscuro. El saludo de inicio de sesión es «Cuida tu salud» y Mi cuerpo muestra «CONOCE TU CUERPO» sobre «Tu mapa personal». Fuentes y dispositivos incorpora el logo, tarjetas redondeadas, tipografía y colores de Bio, y respeta el modo claro/oscuro seleccionado. Se retira temporalmente la sección y enlace de SURA. Se conservan Health Connect, carpetas y gestión de correos.

Gmail está configurado; el usuario confirmó conexión, pero la importación real sigue pendiente de verificar. El filtro selecciona adjuntos por su nombre y mantiene cuotas de análisis. Outlook/Hotmail queda pospuesto por decisión del usuario. Ver `docs/CORREO_DIRECTO.md`. No se ha verificado un reloj o dispositivo médico físico.
## Acceso y biometría (desde 0.4.0)

APK: `Bio-0.4.0.apk`. Inicio de sesión con correo y contraseña, registro, recuperación de contraseña y desbloqueo con huella o rostro seguro compatible. Crear una cuenta vincula la sesión anterior para conservar los documentos. Firebase y el análisis de documentos continúan automáticos después de entrar.

Ver `docs/ACCESO_Y_BIOMETRIA.md` para activación y compatibilidad. Instalar como actualización sin borrar datos. La biometría se activa después de autenticar la cuenta; Android elige la modalidad compatible. El APK es una compilación de desarrollo, no una publicación de Google Play.

## Abrir y ejecutar

1. En Android Studio, selecciona **Open** y abre esta carpeta `salud`.
2. Usa el JDK integrado de Android Studio (17 o superior), Android SDK 36 y Build Tools 35.0.0. Acepta la sincronización de Gradle.
3. Selecciona un emulador o dispositivo con Android 8.0/API 26 o superior y pulsa **Run** en el módulo `app`.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

APK de desarrollo: `app/build/outputs/apk/debug/app-debug.apk`. No es una versión firmada para Google Play. El wrapper de Gradle está incluido (8.13, AGP 8.13.2).

## Funciones entregadas

| Área | Implementación |
| --- | --- |
| Inicio | Resumen, siguiente cita, últimas mediciones y acceso al mapa corporal. Estados vacíos reales. |
| Citas | Crear, editar y eliminar; profesional, ubicación, notas, fecha y hora; agenda futura y pasada. |
| Recordatorios | Día anterior a las 18:00, mismo día a las 08:00, una hora antes; aviso adaptado para citas tempranas. Cancelación al borrar y reprogramación al editar, reiniciar o cambiar la zona horaria. |
| Mi cuerpo | Nueve sistemas con las ocho imágenes aportadas: nervioso, circulatorio, respiratorio, urinario, endocrino, digestivo, óseo, reproductivo y muscular. Vistas individuales y superposición sobre un solo cuerpo. El reproductivo tiene una imagen propia para cada modelo corporal. Iluminación intermitente amarilla para incomodidad y roja para dolor sobre los tejidos de la zona. |
| Signos vitales | Registro manual y lectura autorizada de Health Connect para pulso, presión sistólica y diastólica, SpO₂, temperatura, respiración y glucosa. Fecha y procedencia, eliminación sin reimportación y gráfica de las últimas 12 lecturas por tipo. |
| Historia | Subida de archivos de historia clínica, almacenamiento por cuenta en Firebase, análisis de hallazgos, nombre y fecha clínica. Los síntomas se registran desde Mi cuerpo. |
| Exámenes | Importación de PDF/JPG/PNG, máximo 10 MB por archivo, copia cifrada independiente del original, apertura mediante lector externo y eliminación. |
| IA | Servidor desplegado en el Firebase exclusivo de Bio, con Gemini: análisis de documentos y perfil declarado; investigación web con fuentes y contexto opcional de registros y mediciones. |
| Privacidad | AES-GCM con Android Keystore, escritura atómica de registros, almacenamiento privado, copias y transferencia del almacén deshabilitadas, capturas bloqueadas, HTTPS y consentimiento antes del envío. |

**Alcance clínico:** el mapa utiliza ilustraciones de referencia aportadas por el usuario; no son estudios del paciente ni una evaluación del estado real de los órganos. Los colores de las imágenes son decorativos. La iluminación intermitente amarilla o roja representa incomodidad o dolor registrados. No representan gravedad, enfermedad ni lesión confirmada. No hay diagnóstico automático validado.

### Asociación automática de síntomas (0.2.0)

`SymptomAssociator` utiliza reglas locales de texto en español, sin llamadas de IA, para proponer sistemas y zonas aproximadas. Reconoce referencias anatómicas, algunas sensaciones, negaciones por cláusula y lateralidad. La clasificación es limitada y orientativa, no un modelo clínicamente validado ni una interpretación universal del lenguaje. Un dolor de pecho o abdomen puede relacionarse con varios sistemas; la app no elige una causa definitiva. Si falta información, el registro se conserva y se solicita precisar zona y contexto. Los síntomas desconocidos no se asignan por defecto al sistema nervioso.

El usuario describe la sensación y puede aclarar si es incomodidad, dolor u otro síntoma. Esto no requiere conocimientos sobre sistemas corporales. Bio ilumina los tejidos de la región aproximada descrita; cuando no hay ubicación, conserva el registro sin colorear una zona inventada. Izquierda y derecha corresponden al cuerpo, no a la pantalla. En una vista frontal la izquierda corporal aparece a la derecha. Si falta el lado, se muestran las dos posiciones posibles y el registro lo indica. La espalda usa una proyección anterior, explícitamente rotulada.

Solo los síntomas activos generan iluminación animada. Los diagnósticos, antecedentes, tratamientos y síntomas resueltos no parpadean. La animación es un pulso gradual de 1,8 segundos y se puede desactivar; respeta además la desactivación de animaciones de Android. No se dibujan letras, círculos ni halos; se conservan las descripciones para lectores de pantalla. La asociación se recalcula al editar; los síntomas antiguos se leen automáticamente a partir del texto, sin conservar una selección manual errónea. Los registros históricos no sintomáticos mantienen su sistema anterior cuando no hay evidencia textual para reclasificarlos.

La vista combinada superpone capas de los sistemas seleccionados en un único marco corporal. Las máscaras de tejidos se calculan en memoria a partir de los colores y la silueta de las ilustraciones; son aproximadas, no segmentaciones médicas. Las imágenes originales tienen diferencias de postura y anatomía que pueden producir desajustes de contorno. La representación anatómica de los archivos es masculina. El muscular se añadió como noveno sistema sin cambiar los identificadores de los ocho originales.

Para probar: registra «Dolor en rodilla izquierda» (posibles sistemas óseo y muscular, tejido rojo), «Incomodidad al orinar» (urinario, tejido amarillo), «Sin dolor de cabeza» (sin marcador) y «Me siento raro» (requiere detalles). Marca el síntoma como resuelto para apagar la iluminación. El botón **Ver en mi cuerpo** abre los sistemas asociados.

La app **no mide signos vitales por sí sola**: importa mediciones compartidas con Health Connect. No se conecta directamente por Bluetooth ni ofrece monitorización continua. Los límites de entrada evitan errores de formato evidentes; no constituyen umbrales clínicos.

Los recordatorios usan alarmas inexactas permitidas en reposo. Android puede retrasarlos por batería, restricciones del fabricante, permisos o cierre forzado. No son alarmas médicas críticas. El día anterior se refiere a la fecha del calendario local; la cita se guarda como instante y su hora visible cambia si cambia la zona horaria. Para citas antes de las 08:00 el aviso del día se adelanta 30 minutos, o coincide con la cita si adelantarlo cambiaría de día.

## Activar investigación con IA

El servidor utiliza la API Responses, búsqueda web y `gpt-6-astra` como modelo configurable. La documentación oficial consultada muestra este modelo con `web_search`. Es una elección inicial por capacidad general; **no demuestra superioridad clínica** y depende de acceso, coste, latencia y evaluaciones del caso de uso.

Requiere Python 3.11 o superior, sin paquetes externos:

```powershell
# Establecer en un entorno privado o gestor de secretos, nunca en el APK.
$env:OPENAI_API_KEY = '<clave-de-tu-proyecto>'
$env:ALVIA_ACCESS_TOKEN = '<token-aleatorio-de-al-menos-24-caracteres>'
$env:OPENAI_MODEL = 'gpt-6-astra'
python server/server.py
```

El archivo `server/.env.example` documenta las variables, pero no se carga automáticamente. Genera un token con `python -c "import secrets; print(secrets.token_urlsafe(32))"` y guárdalo de forma privada.

El servidor escucha en `127.0.0.1:8787`. Para conectar Android, colócalo detrás de un proxy HTTPS con certificado válido de una autoridad confiable. Alternativamente, configura `HOST`, `TLS_CERT` y `TLS_KEY` para HTTPS directo. El cliente no acepta HTTP ni certificados sin confianza y no sigue redirecciones del endpoint autenticado.

En la app: **engranaje → Conexión de investigación**, introduce la URL base HTTPS y el token del servidor. La clave de OpenAI se conserva exclusivamente en el servidor. Un token compartido sirve para desarrollo de un solo usuario: **no es autenticación multiusuario de producción**.

En **IA**, escribe la pregunta y, si deseas buscar centros, ciudad y país. La inclusión de registros y de exámenes está desactivada por defecto. La app muestra qué datos enviará y a qué servidor antes de solicitar confirmación. Los exámenes seleccionados se envían completos; el límite conjunto es 10 MB y 20 archivos. No se envía automáticamente el historial.

El servidor solicita fuentes actuales y diferencia evidencia, incertidumbre, tratamientos aprobados y experimentales. Devuelve citas desde las anotaciones de la API. No inventa una respuesta local cuando faltan credenciales o falla la red. Los resultados se muestran en la sesión y no se guardan como diagnósticos.

`store: false` evita almacenar la respuesta para recuperación posterior por API; **no equivale a retención cero del proveedor**. Antes de operar con pacientes debe revisarse el tratamiento de datos con el proveedor y los acuerdos aplicables.

## Estructura

```text
app/src/main/java/com/alvia/health/
  MainActivity.java       Pantallas y flujos de usuario
  BodyView.java           Capas anatómicas e iluminación animada por zona
  SymptomAssociator.java  Asociación local orientativa de descripciones
  RecordAssociations.java Compatibilidad con registros anteriores
  TrendView.java          Gráfica de lecturas registradas
  Vault.java              Cifrado y persistencia local
  Reminders.java          Alarmas y canales de notificación
  ReminderTimes.java      Cálculo de fechas, independiente de Android
  ReminderReceiver.java   Notificaciones con contenido privado
  BootReceiver.java       Restauración de alarmas
  DocumentProvider.java   Acceso temporal de lectura a exámenes descifrados
server/server.py          Gateway autenticado de investigación
server/test_server.py     Validación, citas, adjuntos y autenticación
tests/ReminderTimesTest.java
app/src/androidTest/      Pruebas de integración en Android
```

## Verificación

```powershell
python -m unittest discover -s server -v
.\gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug
javac -d app/build/reminder-tests app/src/main/java/com/alvia/health/ReminderTimes.java tests/ReminderTimesTest.java
java -cp app/build/reminder-tests com.alvia.health.ReminderTimesTest
javac -encoding UTF-8 -d app/build/association-tests app/src/main/java/com/alvia/health/SymptomAssociator.java tests/SymptomAssociatorTest.java
java -cp app/build/association-tests com.alvia.health.SymptomAssociatorTest
```

Con un emulador iniciado:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.alvia.health.test/com.alvia.health.SmokeTests
```

La suite Android utiliza registros sintéticos temporales y los elimina; verifica persistencia, rechazo de cifrado manipulado, documentos y navegación. Las imágenes de comprobación se renderizan desde las vistas de la propia app de prueba, sin desactivar `FLAG_SECURE` en el producto.

## Antes de publicar una versión comercial

Esta entrega es un MVP técnico. La estética es premium; no incluye cobros, suscripciones ni validación de compras. No se ha desplegado el servidor, llamado a una API de pago ni publicado en Google Play.

Se necesita: autenticación individual y autorización, recuperación/exportación segura de la historia, integración de dispositivos médicos, pruebas de usabilidad y accesibilidad con usuarios, validación clínica independiente, revisión de requisitos sanitarios y de protección de datos del mercado elegido, evaluación de IA con casos representativos, monitorización operativa, firma de release y facturación si el producto se comercializa mediante suscripción.

Al desinstalar se pierden los datos locales y su clave. No existe sincronización ni recuperación en la nube. Abrir un examen concede acceso de lectura al visor elegido, sujeto a su propio manejo del documento. El servidor de desarrollo no registra preguntas ni documentos; el proveedor de IA recibe únicamente lo que se autorice enviar.

## Referencias técnicas consultadas

- [OpenAI: búsqueda web](https://developers.openai.com/api/docs/guides/tools-web-search)
- [OpenAI: selección de modelos](https://developers.openai.com/api/docs/guides/model-selection)
- [OpenAI: archivos como entrada](https://developers.openai.com/api/docs/guides/file-inputs)
- [Android: alarmas](https://developer.android.com/develop/background-work/services/alarms)
- [Android: permiso de notificaciones](https://developer.android.com/about/versions/13/behavior-changes-all)
- [MedlinePlus: dolor torácico y diversidad de causas](https://medlineplus.gov/ency/article/003079.htm)
- [MedlinePlus: dolor abdominal y límites de inferir la causa por ubicación](https://medlineplus.gov/abdominalpain.html)

Consulta `VERIFICACION.md` para los resultados concretos de esta entrega.

### Apariencia (0.2.2)

El encabezado permite alternar entre modo claro y oscuro; la preferencia se conserva en el dispositivo y se aplica también a formularios y diálogos. La portada utiliza la imagen anatómica del 26 de septiembre y un corazón verde con trazo de pulso. El icono del lanzador permanece igual. Mi cuerpo muestra únicamente el título grande «TU MAPA PERSONAL».

### Agenda mensual (0.2.3)

Calendario completo de lunes a domingo, navegación anterior/siguiente, botón Hoy y selector directo de mes y año (1–9999). Cada día muestra el icono de su primera cita y un contador adicional si hay más; al tocarlo se listan todas sus citas con horario y acciones. El formulario permite seleccionar la especialidad o detectarla por el motivo. Las citas existentes conservan sus datos y reciben un icono orientativo según el texto. Se retira la palabra Bio del encabezado; desde la versión 0.2.4 todos los encabezados usan la imagen 1000451132.jpg, sin texto adicional al lado. El icono del lanzador se conserva.

En 0.2.5 el encabezado utiliza el nuevo PNG transparente, con el logo más pequeño en todas las secciones.

### Documentos clínicos (0.3.0, pendiente de conectar servicios)

Historia recibe archivos clínicos y Exámenes recibe estudios. Hay organización por fecha clínica, análisis con nombre y hallazgos vinculados al original y SDK de Firebase Auth/Firestore/Storage. La configuración del proyecto y del proveedor de IA todavía es necesaria; no se han desplegado servicios ni probado subidas reales. Consulta [la integración y sus límites](docs/FIREBASE_Y_DOCUMENTOS.md).

## Configuración local al clonar

Las credenciales y configuraciones de producción no se versionan. Copia `firebase/firebase-config.example.json` a `app/src/main/assets/firebase-config.json` y completa los valores de tu proyecto. Copia `app/src/main/assets/backend-config.example.json` a `backend-config.json` en la misma carpeta y configura el servidor. Conserva `app/google-services.json` y los secretos OAuth únicamente en tu entorno local. Los APK se generan con `gradlew.bat assembleDebug`; no forman parte del historial Git.
