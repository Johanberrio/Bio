# Preparación de publicación — 30 de octubre de 2026

Responsable confirmado: **Johan Estevan Berrio Estrada**, persona natural.
Soporte autorizado para publicar: **juandrak@msn.com**.
Precio vigente: COP 13.000/mes en Colombia y USD 5,00/mes en los 75 mercados USD configurados. No usar el precio antiguo de COP 10.000.

## Estado comprobado en Play Console

- App `com.alvia.health`, todavía en borrador; producción inactiva.
- 0 de 11 tareas de configuración completadas al iniciar esta revisión; política, anuncios, categoría/contacto y app no gubernamental guardados posteriormente.
- Descripción breve y completa guardadas como borrador de la ficha es-419. Cuatro capturas reales de la versión 0.6.16 subidas a la ficha (1080×1920, PNG RGB). Falta subir icono de 512×512 y gráfico de funciones de 1024×500. No usar pantallas con datos personales ni presentar maquetas como capturas reales.
- Play exige prueba cerrada: al menos 12 verificadores inscritos durante 14 días continuos. Registraba 0 inscritos. Las pruebas internas previas no cumplen este requisito.
- Categoría Medicina guardada para revisión y correo público de soporte `juandrak@msn.com` guardado y confirmado. Evidencia: `play-categoria-soporte.png`.
- El responsable confirmó que todavía debe reunir a los verificadores. Guion e invitación: `PRUEBA_CERRADA.md`.
- El formulario de salud detecta los ocho permisos reales del manifiesto (siete tipos de lectura y lectura en segundo plano).

## Tareas para completar antes de enviar a revisión

1. Completado: política pública en `https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/privacy`, registrada en Play y enlazada desde Android 0.6.17. Texto: `POLITICA_PRIVACIDAD.md`.
2. Completado: eliminación desde la app y página pública, con confirmación de contraseña, cola durable, espera de tokens y limpieza local/nube. Pruebas reales solo con datos sintéticos: `ELIMINACION_CUENTA.md`. La URL de eliminación queda pendiente de incorporarse al formulario de Seguridad de datos.
3. Completar Seguridad de datos conforme a la matriz inferior, sin declarar que los datos permanecen solo en el teléfono.
4. Completar acceso para revisión con una cuenta dedicada que solo contenga datos sintéticos y acceso a las funciones Premium. No poner credenciales del propietario ni usar una compra real para el revisor.
5. Completar cuestionario IARC, público objetivo, anuncios (no hay SDK publicitario en las dependencias actuales), apps gubernamentales y funciones financieras. Elegir categoría Medicina. No tratar la suscripción como una funcionalidad de banca/inversión.
6. Declaración de salud: proponer organización de información médica y educación/referencias médicas, seguimiento de mediciones/afecciones y peso. No declarar certificación de dispositivo médico, ensayos clínicos, teleconsulta o funciones inexistentes. Revisar justificaciones y requisitos regionales antes de enviar.
7. Correo de soporte ya registrado; preparar capturas con un perfil ficticio. Definir público objetivo y revisar imágenes anatómicas con IARC, sin adivinar clasificación.
8. Gmail: completar verificación OAuth de alcance restringido `gmail.readonly`, marca/dominio/política y evaluación de seguridad que Google requiera. El procesamiento de adjuntos en servidor necesita esta revisión; el modo de prueba no es habilitación pública. No contratar una evaluación de pago sin autorización.
9. Health Connect: justificar cada permiso de lectura; demostrar valor visible de cada medición y del trabajo en segundo plano. No solicitar escritura, localización, pasos ni permisos no utilizados.
10. Tras resolver lo anterior, publicar prueba cerrada, recoger fallos y correcciones durante 14 días con 12 participantes reales, revisar informe previo al lanzamiento y solicitar acceso a producción. La aprobación la decide Google; no se puede simular el transcurso del tiempo ni la participación.

## Matriz técnica para Seguridad de datos (pendiente de declaración final)

| Dato | Tratamiento actual | Finalidad / elección |
|---|---|---|
| Correo, identificador de cuenta | Firebase Auth y llamadas autenticadas al servidor | Crear/gestionar cuenta, seguridad |
| Nombre, nacimiento, estatura, peso, grupo sanguíneo | Perfil respaldado en Firestore; contexto clínico cuando se analiza | Perfil y funcionalidades; revisar obligatoriedad del formulario |
| Documentos, imágenes y archivos médicos | Cifrado local; originales en Storage y metadatos en Firestore | Guardar/restaurar; carga voluntaria |
| Información de salud y análisis | Vertex AI recibe el documento/contexto para generar explicaciones | Funciones Premium, sin prometer diagnóstico |
| Mediciones Health Connect | Locales; peso pasa al perfil respaldado; contexto seleccionado puede ir a IA | Importación opcional, permisos por tipo |
| Gmail | OAuth y token de actualización privados; lectura temporal de mensajes/adjuntos; se guardan documentos aceptados | Importación opcional; no envío ni modificación del buzón |
| Compras | Token, estado, fechas, ID ofuscado y verificación con Google Play | Habilitar suscripción; Bio no recibe tarjeta bancaria |
| Cuotas | UID, mes, tipo de operación y reserva; sin contenido clínico | Control de uso y recuperación de errores |
| Consulta de IA y fuentes | Consulta/contexto procesados por Google Cloud; consultas pueden usar Google Search | Investigación solicitada por el usuario |

No hay Firebase Analytics ni SDK de anuncios declarados. Verificar también las dependencias transitivas y los registros operativos antes de contestar sobre diagnósticos/identificadores. Las excepciones de «compartido» para proveedores de servicio deben evaluarse según las definiciones de Play; no confundirlas con «no se transmite». HTTPS y cifrado de almacenamiento no significan cifrado de extremo a extremo.

## Plan Premium aprobado

- 30 análisis de documentos y 20 consultas de IA por cuenta y mes calendario UTC; sin acumulación ni prorrateo por fecha de compra.
- Gmail y carga manual comparten los 30 análisis. Un análisis completado cuenta aunque no sea importable; un error del servicio devuelve el cupo. La renovación del cupo no concede acceso si no hay suscripción activa.
- Se reemplaza el límite compartido de 50/día y 10/día por persona. Transacciones de Firestore evitan exceder cupos con solicitudes concurrentes.
- Límites técnicos: entrada inicial de hasta 20.000 tokens, revisión hasta 10.000; salida de extracción hasta 6.000, revisión 3.000, consulta 4.000. Los documentos que exceden la extensión se rechazan sin recortarlos y se devuelve la reserva.
- Estimación, no factura garantizada: a USD 0,25/millón de entrada y USD 1,50/millón de salida, las dos etapas máximas suman aproximadamente USD 0,021 por documento; 30 ≈ USD 0,63. Veinte consultas de 20.000/4.000 tokens ≈ USD 0,22, antes de grounding/búsquedas, almacenamiento, tráfico, funciones, impuestos y comisión de Play. Validar costos de búsqueda y métricas reales antes de abrir ventas públicas. No se hizo una inferencia facturada para esta preparación.

## Comprobación de extremo a extremo pendiente

En un teléfono con la versión nueva, verificar y anotar fecha/versión/resultados:

- Gmail: cuenta conectada, PDF sintético con nombre `HISTORÍA_clínica_prueba.pdf` y asunto sin términos médicos; llega con nombre y fecha extraídos; una segunda sincronización no duplica; rechazo de otro titular; desconexión/revocación; reconexión; cupo agotado con fecha correcta.
- Health Connect: permiso de peso y una lectura reciente de 70,8 kg; Inicio conserva decimales; nueva lectura actualiza; revocar permiso conserva valor previo sin afirmar sincronización nueva; comprobar segundo plano y cambio de cuenta.
- Suscripciones: compra con tarjeta de prueba, restauración, vencimiento, bloqueo de IA Normal, originales accesibles y cupos que no se reinician por reinstalar.
- Accesibilidad/pantallas: modo claro/oscuro, letra grande, teclado, pantallas estrechas, volver del consentimiento OAuth sin perder la sesión indebidamente.

## Referencias oficiales

- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/14738291
- https://support.google.com/googleplay/android-developer/answer/13327111
- https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification
- https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing

## Verificación técnica del 29 de octubre

- Despliegue de `bio_api` verificado ACTIVE, actualización `2026-10-29T12:48:06.024588703Z`; compras públicas siguen deshabilitadas. No se hicieron compras ni inferencias de IA.
- 82 pruebas unitarias de servidor aprobadas. Incluyen concurrencia de cuotas, separación por usuario/tipo/mes, devolución única y límites de entrada sin truncar documentos.
- Comprobación contra el servidor: estado saludable, 401 sin autenticación, 402 para IA con cuenta Normal, cuotas 30/20 y denegación de lectura/escritura de contadores desde el cliente. Cuenta temporal eliminada.
- Servicio Gmail: inicio OAuth, PKCE, callback fijo, cancelación, rechazo de reutilización y aislamiento de credenciales aprobados. No se accedió a un buzón real. Outlook permanece deshabilitado.
- Compilación de APK/AAB 0.6.16 (31), APK de pruebas y lintDebug/lintVitalRelease completados. Esto no sustituye la comprobación de importación y Health Connect en un teléfono real.
- Esta versión todavía no se ha cargado en Play; la pista interna sigue en 0.6.15 (30). Huellas: `Bio-0.6.16-preview.sha256`.
- Los errores atendidos por el servidor devuelven su reserva. Un proceso interrumpido antes de devolverla puede dejar un cupo pendiente; la reconciliación automática de estos casos aún no está implementada.

- 116 comprobaciones Android aprobadas en el emulador aislado, tanto en 1080×2424 como en 1080×1920. Capturas de inicio, citas, exámenes y mapa corporal revisadas sin datos personales en `play-capturas-0616/`.

- Play confirmó «Se guardaron los cambios» con 4/8 capturas de teléfono y la descripción que especifica UTC. Evidencia: `play-capturas-guardadas.png`. Icono y gráfico de funciones aún pendientes; la ficha permanece en borrador.

## Avance del 30 de octubre — 0.6.17-preview (32)

- 92 pruebas del servidor aprobadas. Servicio de eliminación desplegado y activado tras comprobar un trabajador privado y un programador autenticado. No se usó IA en estas verificaciones.
- Flujo Android verificado en claro/oscuro: contraseña incorrecta rechazada, solicitud vacía rechazada, reautenticación válida aceptada y almacenamiento local limpiado. Las cuentas ficticias del ensayo se eliminaron. Evidencia: `eliminacion-verificacion.json`.
- APK/AAB de 0.6.17 compilados con lint; hashes en `Bio-0.6.17-preview.sha256`. Todavía no subidos a Play.
- Política pública guardada en Play, pendiente de envío a revisión: `play-privacidad-guardada.png`. Declaración sin anuncios guardada: `play-sin-anuncios.png`.
- Titular confirmó público inicial de 18 años o más. Incorporado a la política; el formulario de público objetivo exige completar primero Detalles de acceso. Sigue pendiente crear el acceso de revisión con datos ficticios.
- Quedan ficha visual, Seguridad de datos, acceso de revisión, público objetivo/IARC, declaración de salud/Health Connect y verificación OAuth Gmail. No se ha solicitado producción ni iniciado la prueba cerrada; aún faltan 12 verificadores reales.

- Declaración «App no gubernamental» guardada para revisión: `play-no-gubernamental.png`.

- Comprobación posterior al último despliegue: Premium 30/20, bloqueo de IA Normal, contadores privados y OAuth Gmail/PKCE/cancelación/replay aprobados. Usuarios sintéticos retirados. No se accedió a buzones reales ni se llamó a IA. Panel confirmó 4/11 tareas, evidencia play-preparacion-30-oct.png.

## Avance del 1 de noviembre

- Declaración de funciones financieras guardada: Bio no ofrece banca, préstamos, seguros ni otras funciones financieras. La suscripción es una compra de contenido digital, no un servicio financiero. Evidencia: `play-sin-funciones-financieras.png`.
- Declaración de salud guardada con ocho justificaciones de lectura de Health Connect, conforme al código actual: `play-health-permissions.json` y `play-salud-guardada.png`. Categorías: Actividad y ejercicio (la definición de Google incluye medidas corporales/peso), Control de enfermedades y afecciones, Administración y servicios de atención médica y Educación y referencias médicas. No se declararon diagnóstico certificado, prescripción ni funciones de emergencia.
- 94 pruebas del servidor aprobadas. Se desplegaron `bio_api` y `bio_erase_accounts` con acceso de revisión aislado y eliminación de su permiso privado al borrar la cuenta.
- Cuenta de revisión ficticia preparada y verificada sin ejecutar IA; credenciales en `.secrets`, aún no introducidas en Play Console. Instrucciones y límites en `ACCESO_REVISION_PLAY.md`. Las compras públicas siguen deshabilitadas y los cupos siguen en 30/20.
- Estos cambios del servidor no requieren otro APK; la versión local continúa en 0.6.17 (32), pendiente de subir a Play. No se ha enviado la app a revisión ni solicitado producción.
- Verificación posterior al despliegue aprobada: IA denegada para Normal en ambas rutas, contadores privados, cupos 30/20 y compras públicas desactivadas. Cuenta temporal retirada. No se invocó ningún modelo.
- Formulario de acceso preparado con instrucciones en inglés (462 caracteres), sin introducir usuario ni contraseña ni confirmar acceso completo. Se solicitó autorización específica para compartir con Google las credenciales de la cuenta ficticia. Evidencia: `play-review-access-ready.png`. Pendiente la respuesta del responsable y la verificación del recorrido Android/Gmail antes de enviar a revisión.

## Continuación — 2 de noviembre

- El responsable autorizó compartir con Google las credenciales de la cuenta ficticia. Play confirmó su guardado con las instrucciones de revisión. Se desactivó la autorización opcional de pruebas con socios de confianza. Evidencia sin secretos: `play-review-access-saved.png`.
- Público objetivo guardado: solo mayores de 18 años, con restricción de acceso para usuarios que Google identifica como menores. No equivale a verificar la edad de todos los usuarios. Evidencia: `play-publico-adulto.png`.
- Seguridad de datos guardada para revisión: recopilación, HTTPS, acceso por correo/contraseña y biometría opcional, eliminación de cuenta y de documentos. Véase `SEGURIDAD_DATOS_PLAY.md` y `play-seguridad-datos-guardada.png`.
- El panel confirmó inicialmente **9 de 11 tareas completas**. Tras la autorización del responsable se aceptaron las condiciones IARC y se guardó la clasificación: estado «Completado». Las ilustraciones anatómicas se trataron como material científico/médico sin significado sexual, conforme a la ayuda oficial, no como desnudez general. Se declararon contenido en línea, posibles referencias clínicas a sustancias y compras digitales. Resultados: Brasil 14+, ESRB Adolescentes, PEGI 3, USK 12+, IARC genérico/Rusia/Corea 3+. Estos resultados no cambian el público objetivo elegido de 18+. Evidencia: `play-iarc-completado.png`.
- Icono 512×512 y gráfico 1024×500 cargados, junto con las cuatro capturas Android existentes. Los seis recursos quedaron etiquetados como creados o editados con IA. Play confirmó «Se guardó el cambio» y «Lista para enviar a revisión». Recursos reproducibles en `play-assets/`; evidencia `play-ficha-guardada.png`.
- El panel ya no muestra la lista inicial de configuración y presenta la prueba cerrada con **0 de 5 tareas**, producción inactiva y **0 participantes inscritos**. No se afirma que Google haya aprobado la app. Evidencia: `play-preparacion-2-nov.png`.
- La versión 0.6.17 (32) sigue pendiente de cargar en Play. No se ha enviado a revisión ni publicado en producción. Continúan pendientes las verificaciones reales de Gmail/Health Connect, OAuth público y los 12 participantes de la prueba cerrada.

Referencias para las declaraciones visuales: https://support.google.com/googleplay/android-developer/answer/6160008?hl=en (excepción científica/médica) y https://support.google.com/googleplay/android-developer/answer/17262077?hl=en (etiquetado de recursos con IA).

## Avance del 5 de noviembre

- 0.6.18-preview (33) publicada en prueba interna; consola confirmó disponibilidad a las 14:27 de Colombia. Evidencia: `play-bio-0618-publicada.png`.
- Versión 33 y notas guardadas para la prueba cerrada Alpha, inicialmente Colombia. La vista general muestra 12 cambios aún no enviados a revisión; el botón de envío está deshabilitado. No hay verificadores asignados ni días de participación iniciados. Evidencia: `play-prueba-cerrada-preparada.png`.
- Gmail: el responsable confirmó importación funcional tras resolver el PDF protegido/ilegible. Falta comprobar en teléfono la continuación de la cola de la nueva versión 33 y completar las pruebas de revocación/duplicados. No se repitieron análisis de IA en esta preparación.
- Política de privacidad guardada también en Google Auth Platform. Gmail sigue en modo Prueba; verificación pública pendiente. Preparación en `GMAIL_VERIFICACION_PUBLICA.md`. El responsable no tiene dominio propio.
- El responsable verificó por SMS el teléfono de desarrollador y autorizó explícitamente ambas declaraciones finales. Play confirmó «Se guardó el estado de comerciante». También quedó guardada para revisión su aplicación a Bio. No registrar teléfono ni dirección privada en esta documentación. No se modificaron declaraciones individuales de otras apps de la cuenta.
- Las verificaciones rápidas de Play detectaron dos bloqueos: ID de publicidad sin declarar y estado comercial de Bio sin confirmar. Ambos formularios quedaron guardados. Se declaró que Bio no usa ID de publicidad después de revisar código, dependencias y manifiesto fusionado release del código 33: sin permiso AD_ID ni SDK de anuncios. El estado de comerciante de Bio hereda el de la cuenta; no se inventó una URL de términos del servicio (campo opcional).
- Tras resolver ambos bloqueos se habilitó el envío. Se enviaron y confirmaron los 13 cambios. La consola muestra «Cambios en la etapa de revisión» y explica que pasarán a revisión al terminar correctamente las verificaciones rápidas, aún en curso. Evidencia final: `play-prueba-cerrada-en-revision.png`. No es aprobación, publicación en producción ni inicio de los 14 días; sigue faltando el grupo de verificadores.

## Rechazo consultado el 6 de noviembre

Google muestra «App rechazada», con fecha de rechazo 5 de noviembre. Motivo: requisitos de Play Console; las categorías o funciones declaradas requieren cuenta de organización. Detalle: `policy-center/issues/4985555789313940213/details`. La cuenta actual es personal. El estado de comerciante no la convierte en organización.

No se cambiaron las declaraciones de salud para ocultar las funciones reales ni se envió una apelación. Es necesario resolver el tipo de cuenta y la verificación aplicable antes de volver a solicitar publicación. Evidencia: `play-rechazo-organizacion.png`. Fuente oficial: https://support.google.com/googleplay/android-developer/answer/10788890 .
