# Bio Normal y Premium — integración en preparación

Precio vigente en Colombia: COP 13.000 por mes, cobrado por Google Play. El plan también está habilitado en 75 países o regiones que Play cobra en USD, con precio de US$5,00/mes. Las otras monedas quedan pendientes. Descarga gratuita. Android consulta el precio localizado directamente a Play.

## Estado actual — 26 de octubre de 2026

El titular aprobó sustituir la ruta MedGemma por Gemini mediante API. Esta sección
sustituye las decisiones sobre modelos descritas en el historial inferior.

- Implementado localmente: extracción y explicaciones con Gemini a través de Vertex AI,
  usando la identidad de servicio de Bio. No requiere endpoint MedGemma, GPU ni claves
  en Android. Modelo predeterminado de ambas etapas: `gemini-3.1-flash-lite`.
- Configuración de servidor: `BIO_EXTRACTION_MODEL`, `BIO_REVIEW_MODEL` y `BIO_MODEL`
  (investigación). Una variable ya establecida en producción prevalece sobre el valor
  predeterminado: revisar las tres al desplegar. No se cambió la configuración remota.
- La extracción conserva nombre, fechas, citas, rangos y estados de los hallazgos.
  Las explicaciones usan JSON estructurado con referencias obligatorias a hallazgos
  existentes. Android conserva su contrato de lista de textos; cada explicación añade
  `[Hallazgos: …]`. Referencias válidas no demuestran que la inferencia sea correcta.
- Respuestas incompletas, bloqueadas o con referencias inválidas se rechazan. No se
  reintenta automáticamente la llamada del SDK ni se cambia a otro proveedor.
  Documentos ilegibles, sin hallazgos o de otro titular omiten la segunda llamada.
- Sigue la verificación Premium en servidor antes de cualquier llamada de IA.
  Persisten los límites global de 50 solicitudes/día y de 10 por usuario/día;
  cada análisis puede hacer dos llamadas. No son límites de gasto en COP ni capacidad
  comercial suficiente para 1.000 usuarios. No se promete uso ilimitado.
- Gemini es un modelo general aplicado a documentos de salud. No anunciarlo como
  modelo médico especializado, diagnóstico confirmado ni reemplazo del profesional.
- Validación: 35 pruebas locales con respuestas simuladas, incluidas las rutas HTTP
  Premium/cuota y las dos etapas completas. Sin inferencia facturada ni datos reales.
  Falta validar calidad clínica y disponibilidad efectiva del modelo en el proyecto.
- Servidor desplegado el 26 de octubre de 2026 a las 17:07 UTC. Función `bio_api`
  ACTIVE, Python 3.12, identidad bio-analysis, mínimo 0 y máximo 1 instancia.
  Las tres variables de modelo no tienen overrides remotos; se usan los defaults Gemini
  del código. `BIO_PREMIUM_READY` sigue deshabilitado; el plan de Play y AAB 27 siguen
  en borrador. Falta completar las pruebas de compra antes de habilitar cobros.
- Pasaron las 70 pruebas locales del servidor. `tools/verify_premium_access.py`
  confirmó en el servidor real: health, rechazo sin sesión, estado Normal, compras
  deshabilitadas y HTTP 402 premium_required en ambas rutas IA. Cuenta ficticia
  temporal eliminada al terminar. No se invocó IA ni se realizó una compra.
- Verificación reproducible de configuración sin exponer secretos:
  `node tools/check-premium-deployment.cjs`. No confundir estas comprobaciones de
  acceso con una evaluación real del contenido generado por Gemini.

Documentación del contrato JSON: https://ai.google.dev/gemini-api/docs/structured-output .
Las tarifas citadas en conversación eran de Gemini Developer API; Bio usa Vertex AI.
No trasladar aquellas cifras a un presupuesto de producción sin consultar tarifas
Vertex, región, tokens, almacenamiento y demás servicios aplicables.

## Historial de preparación (decisiones anteriores)

## Implementado localmente

- Pantalla de planes, consulta del precio real en Play, checkout, restauración y enlace para gestionar/cancelar.
- Producto `bio_premium`, plan base `mensual`, periodo P1M. Sin ofertas iniciales en esta integración.
- Cuenta Firebase vinculada mediante SHA-256 del UID en `obfuscatedAccountId`. Una compra no concede acceso a otra cuenta.
- `/billing/verify` consulta purchases.subscriptionsv2 y reconoce compras activas; `/billing/status` vuelve a consultar Play. La IA consulta Play antes de cada petición. No se confía en el perfil editable ni en un booleano enviado por Android.
- Active, grace period y canceled permiten acceso hasta el vencimiento; pending, on hold, paused y expired no lo permiten. Fallos de verificación bloquean IA.
- Tokens guardados en colección privada `_playSubscriptions`, inaccesible desde el cliente por las reglas existentes. No se registran en logs.
- Normal: carga manual conserva nombre original y no ejecuta IA. Importación de correo que valida el titular con IA reservada a Premium.
- `BIO_PREMIUM_READY` debe permanecer ausente/falso hasta completar modelo, precio y pruebas. La compra está deshabilitada por defecto.

## Pendiente antes de activar o publicar

1. Bio creada y verificada en Play Console el 23 de octubre de 2026: app ID `4974123285477413409`, paquete `com.alvia.health`. El usuario completó las declaraciones. Perfil de comerciante creado por el titular y verificado. Play exige una primera compilación con Billing para habilitar creación de suscripciones; borrador interno iniciado.
2. Preparar firma de producción y subir AAB a pruebas internas. El APK debug previo no sirve como publicación de producción.
3. Mantener Colombia con precio fijo de COP 13.000/mes y los mercados USD autorizados con US$5,00/mes. No habilitar otras monedas ni regiones nuevas automáticamente. Configurar perfil de pagos si Play lo requiere.
4. Habilitar Android Publisher API y dar permisos de suscripciones a la identidad del servidor en Play Console. No generar claves privadas para Android.
5. Desplegar servidor y app de prueba juntos. El backend de producción anterior aún no tiene estos endpoints ni el control Premium. No instalar esta compilación como actualización funcional hasta ese despliegue.
6. Probar compra de licencia, pago pendiente, restauración, cambio de cuenta, cancelación con vigencia, renovación, suspensión, vencimiento, revocación y errores de Play. Las pruebas unitarias no sustituyen este recorrido real.
7. Seleccionar/desplegar/validar el modelo especializado. MedGemma es candidato, NO está integrado. El adaptador actual aún usa Gemini: PDFs, JSON estructurado y búsqueda web no son intercambiables directamente con MedGemma. No anunciar IA médica especializada ni habilitar cobros hasta resolverlo.
8. Revisar cuotas y costes: persiste límite global 50 análisis/día y 10 por usuario, insuficiente para prometer capacidad a una base grande. No se promete uso ilimitado.
9. Habilitar `BIO_PREMIUM_READY=true` solo después de pruebas integrales y disponibilidad efectiva del servicio ofrecido.

No se desplegó este cambio ni se activaron cobros. No se cambió el modelo de producción.

Referencias: https://developer.android.com/google/play/billing/integrate ; https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2 ; https://developers.google.com/health-ai-developer-foundations/medgemma/model-card

## Firma de carga para Play

Se creó una clave de carga local en `.secrets/bio-upload.jks`, con propiedades en `.secrets/upload-signing.properties`. Ambos están ignorados por Git. Conservar una copia de seguridad privada de ambos; nunca añadirlos al repositorio ni al APK. Gradle utiliza esta firma solo para release. No se cambia la firma debug de los APK anteriores.

El AAB de configuración no se debe distribuir todavía: el backend de Premium y el modelo especializado siguen pendientes. No se lanzó ninguna versión al público.

## Avance verificado — 24 de octubre de 2026

- AAB firmado 27 (0.6.12-preview) aceptado por Play y guardado en borrador de pruebas internas; todavía no distribuido.
- Producto Bio Premium (`bio_premium`) creado.
- Plan `mensual` guardado correctamente en estado Borrador: renovación automática mensual, Colombia, COP 10.000. No activado.
- Evidencia: `docs/play-premium-mensual-guardado.png`.
- Pendientes reales: permisos Android Publisher para la identidad `bio-analysis@bio-project-15d15.iam.gserviceaccount.com`, despliegue coordinado, modelo especializado y pruebas de compra. Las tareas 1–3 de la lista anterior ya tienen completada la preparación del artefacto y catálogo; queda la distribución de prueba.

Permisos Play autorizados por el titular y aplicados el 24 de octubre de 2026: bio-analysis@bio-project-15d15.iam.gserviceaccount.com aparece Activo, limitado a Bio, con lectura de información/calidad, datos financieros y administración de pedidos/suscripciones. Evidencia: docs/play-servidor-autorizado.png. Sigue pendiente comprobar Android Publisher API y una compra real de prueba; este acceso no activa el plan ni los cobros.

## Preparación del flujo de dos modelos — 24 de octubre de 2026

- Android Publisher API habilitada y verificada mediante `tools/enable-play-api.cjs`. Falta probar compras.
- Implementación local de documentos: `gemini-3.1-flash-lite` extrae los datos y el adaptador MedGemma interpreta las citas. La segunda etapa no puede sobrescribir nombre, fecha, identidad ni hallazgos originales.
- El endpoint privado se configura en servidor mediante `BIO_MEDGEMMA_ENDPOINT=projects/bio-project-15d15/locations/REGION/endpoints/ID`. Requiere una implementación compatible con chat completions, sin salida de razonamiento. No existe todavía un endpoint configurado o verificado.
- Documentos sin hallazgos, de otro titular o no clínicos no consumen la segunda etapa. Errores no sustituyen MedGemma por un modelo general de manera silenciosa.
- 24 pruebas unitarias pasaron con datos ficticios y proveedores simulados. Esto NO valida precisión clínica, latencia, coste, compatibilidad del endpoint ni comportamiento con documentos reales.
- Pendientes: aceptación de licencia HAI-DEF, alojamiento con escalado a cero validado y presupuesto de prueba, medición de coste, adaptación de `/research` (todavía usa Gemini), pruebas integrales y despliegue coordinado. No desplegar este backend hasta completar esos pasos.
- La meta de 2.000 COP por usuario/mes no es un límite monetario implementado. Persisten las cuotas diarias anteriores; no se promete capacidad ni uso ilimitado.
- Compras deshabilitadas y plan en borrador. No se contrató GPU ni se cambió producción.

### Licencia aceptada y configuración revisada

**Restricción posterior del titular: no autoriza gasto para la prueba.** La configuración de pago descrita abajo no se envió. Se abandonó el formulario navegando al cuaderno oficial gratuito de Colab. No desplegar GPU ni usar APIs facturadas para esta evaluación.

Ruta gratuita verificada: https://colab.research.google.com/github/google-health/medgemma/blob/main/notebooks/quick_start_with_hugging_face.ipynb . El cuaderno oficial indica MedGemma 4B con T4 gratuita, sujeto a disponibilidad de Colab. Requiere sesión de Hugging Face, acceso al modelo y token de lectura en secretos de Colab. La aceptación en Model Garden no concede automáticamente acceso en Hugging Face. La cuenta estaba sin sesión; pendiente del titular. No se ejecutó inferencia propia ni se solicitaron recursos de pago. Casos ficticios preparados en `docs/medgemma-casos-sinteticos.json`. La demostración pública EHR Navigator solo se exploró como ejemplo y no se cuenta como prueba de Bio. Colab gratuito es para evaluación interactiva, no alojamiento de la app.

El 24 de octubre de 2026 el titular autorizó expresamente aceptar la licencia HAI-DEF. Se pulsó la aceptación en Model Garden y se verificó acceso al formulario de despliegue. Evidencia: `docs/medgemma-licencia-aceptada.png`.

Formulario preparado, NO enviado: MedGemma 1.5 4B IT, us-central1, vLLM 32K, g2-standard-24 con 2 NVIDIA L4; mínimo 0 y máximo 1 réplica, reducción a cero activada, endpoint dedicado. La consola muestra USD 2,07/hora por réplica activa (USD 1.512,01/mes si permanece activa). El escalado a cero no es un límite monetario y el arranque/inactividad previa al apagado deben medirse. Evidencia: `docs/medgemma-prueba-configurada.png`. No se pulsó Implementar. El gasto de prueba fue rechazado; no continuar por esta ruta de pago. El adaptador local actual debe ajustarse al DNS dedicado si algún día se autoriza este despliegue.

### Evaluación gratuita ejecutada — 25 de octubre de 2026

El titular inició sesión en Hugging Face y autorizó la licencia, la creación de un token de lectura limitado, su almacenamiento como `HF_TOKEN` en Secretos de Colab y el acceso desde el cuaderno. También autorizó aceptar los términos de Colab. Estas etapas ya están completadas; sustituyen el estado pendiente descrito arriba. No hay credenciales en los artefactos de la prueba.

Se ejecutó MedGemma 1.5 4B en Tesla T4 con cinco casos ficticios: tres cumplen los criterios, uno parcial (omite rango de referencia) y uno fallido/truncado ante instrucciones incrustadas. Tiempos de 97 a 167 segundos por caso en la configuración probada. Informe y limitaciones: `docs/MEDGEMMA_EVALUACION.md`; resultados observados: `docs/medgemma-resultados-colab.json`; cuaderno reproducible: `docs/Bio-MedGemma-prueba-gratuita.ipynb`.

La sesión temporal de GPU se desconectó y eliminó después de guardar los resultados localmente. No se desplegó infraestructura de pago, no se cambió producción y Premium sigue sin habilitarse. Esta evaluación exploratoria no valida el contrato JSON del servidor ni demuestra idoneidad clínica o capacidad de producción.

Seguimiento: se añadió validación estricta de respuesta final y se verificó la conservación de fecha/rango de laboratorio (26 pruebas unitarias aprobadas). Una segunda evaluación gratuita de dos casos con el contrato JSON real falló: laboratorio rechazado por formato de interpretaciones y alergias con JSON válido pero afirmación no sustentada. Detalles en `docs/MEDGEMMA_EVALUACION.md`. El modelo en esta configuración sigue sin ser apto para activar el servicio; los cambios locales no se desplegaron.

## Verificador configurado — 26 de octubre de 2026

Se creó la lista «Bio prueba personal», con la cuenta indicada por el titular, y se
seleccionó y guardó para el segmento de prueba interna de Bio. No se seleccionó la
lista preexistente de 13 personas. Play confirmó 2 de 3 tareas completadas.
También se seleccionó esa lista en Prueba de licencia y Play confirmó «Se guardaron
los cambios», con RESPOND_NORMALLY. Las pruebas de licencia son configuración de
cuenta de desarrollador, compartida por sus apps. Evidencias:
`docs/play-bio-verificador.png` y `docs/play-bio-licencia-prueba.png`.

Esto no equivale a una compra probada. La versión interna todavía es borrador,
el plan mensual no está activo y el servidor mantiene purchasesEnabled=false.
Antes de probar el checkout se debe distribuir la versión interna y habilitar
el flujo de prueba. Usar exclusivamente el medio de pago de prueba que muestra
Google Play; no realizar una compra con una tarjeta real.

## Prueba interna distribuida — 26 de octubre de 2026

- AAB 27 (0.6.12-preview) publicado en el segmento interno, que muestra Activo y
  Disponible para verificadores internos. Solo está seleccionada «Bio prueba personal».
- Notas actualizadas: Gemini en evaluación, sin prometer modelo médico especializado.
- Enlace para el verificador: https://play.google.com/apps/internaltest/4700305961256754435 .
- Plan mensual activado en el catálogo Play para poder consultarlo en la prueba:
  Colombia, COP 10.000, renovación automática mensual. No se realizó ninguna compra.
- Servidor actualizado: purchasesEnabled es true para una cuenta con permiso temporal
  administrativo en `_billingTesters/{uid}` o si se activa globalmente BIO_PREMIUM_READY.
  Este permiso NO concede Premium: sigue siendo necesaria la compra verificada por Play.
  El acceso general permanece deshabilitado. Las reglas Firestore bloquean clientes
  para la colección de verificadores. Pasaron 71 pruebas locales y el smoke remoto
  confirmó nuevamente que cuentas Normal no pueden comprar ni usar IA.
- Pendiente: el correo indicado para Play no coincidió con una cuenta activa de Firebase
  Auth de Bio. Se solicitó el correo usado dentro de Bio. No se creó permiso temporal
  para ninguna cuenta. `tools/configure-billing-tester.cjs EMAIL` configura siete días
  una vez identificada la cuenta autorizada. No guardar su UID ni tokens en la documentación.
- Evidencia: `docs/play-bio-interna-activa.png` y `docs/play-bio-plan-activo.png`.
- Sigue pendiente probar compra/restauración desde el teléfono y generación real de IA.
  Seleccionar solo el instrumento de pago de prueba de Google, nunca una tarjeta real.

## Cuenta de prueba de Bio vinculada

El titular indicó su cuenta de inicio de sesión de Bio, distinta del Gmail de Play.
Se encontró la cuenta activa y se guardó y verificó el permiso administrativo de
checkout, con vencimiento 2026-11-03T00:32:50.508Z (2 de noviembre, 19:32 en Colombia).
No se concedió Premium ni se realizó una compra. El Gmail de Play continúa siendo
el verificador de licencias; la cuenta de Bio conserva su vínculo por UID.
Pendiente: completar el checkout con instrumento de prueba desde el teléfono y
verificar restauración y acceso. No se ejecutó inferencia de IA.

## Corrección de activación de compras — 26 octubre 2026

- Se observaron respuestas 403 de `/billing/verify` seguidas de 200 de `/billing/status`, sin suscripción guardada para la cuenta de prueba.
- Causa encontrada en código: Android calcula SHA-256 de `FirebaseCloud.owner()` (`bio-project-15d15:UID`), mientras el servidor calculaba SHA-256 del UID sin proyecto. El servidor ahora usa exactamente el identificador de Android; sigue rechazando otras cuentas y otros proyectos.
- Corrección del servidor desplegada correctamente en `bio_api` (us-east1). No requiere reinstalar la app para restaurar una compra vigente.
- Validación: suite de 73 pruebas aprobada y una prueba adicional del flujo verificar/guardar/reconocer aprobada (7 pruebas de billing en total). Comprobaciones del servidor publicado: salud, autenticación y bloqueo de ambas rutas de IA a usuarios normales aprobadas. No se ejecutó IA ni se realizaron compras.
- En el código Android se evita que la consulta de estado sobrescriba el resultado de restaurar una compra; compilación release Java correcta. Esta mejora de interfaz todavía no está distribuida en Play; la versión instalada sigue siendo 27.
- Pendiente: confirmar la restauración de la compra desde el teléfono del usuario. No se concedió Premium manualmente ni se alteraron permisos de Play.
- Tras restaurar, solo llegaron consultas de estado (200), sin compras para verificar. Se comprobó en Play Console que la única compra de prueba de las 22:06 de Colombia figuraba Reembolsado, COP 0. Se solicitó repetir la compra únicamente con la tarjeta de prueba, después del despliegue. El código Android ahora explica también cuando Play devuelve cero suscripciones restaurables.
- Confirmación final: el usuario ve «Tu suscripción Premium está activa». Consulta administrativa de solo lectura confirma `_playSubscriptions` guardada con premium=true; `/billing/verify` y `/billing/status` responden 200. Activación real con tarjeta de prueba comprobada, sin activar Premium manualmente.

## Preparación de prueba funcional — 27 octubre 2026

- 74 pruebas locales aprobadas; smoke remoto verifica autenticación y bloqueo de ambas rutas de IA para cuentas Normal. Cuenta temporal eliminada.
- PDF sintético de una página creado y revisado visualmente: `output/pdf/resultado_prueba_bio.pdf`. Sin paciente real. Resultado esperado: Hemograma completo; fecha clínica 2026-10-20 (no fecha de impresión 2026-10-27); tres mediciones explícitas. No debe inferir un diagnóstico confirmado.
- Pendiente subir manualmente desde el teléfono con sesión Premium y observar el análisis. No se ha invocado IA en este paso. No usar importación de correo para este PDF sin identidad de paciente, pues la validación de titular debe rechazarlo.

## Presentación de análisis — 0.6.13-preview (28)

Exámenes e Historia muestran Resumen, Resultados y Limitaciones. Cada hallazgo conserva su texto, estado (incluidas negaciones y antecedentes) y página, con tarjeta individual. Las interpretaciones orientativas permanecen separadas de los hallazgos documentales. Las limitaciones extensas se despliegan con un botón; solo se omiten párrafos idénticos normalizando espacios y mayúsculas, sin borrar advertencias distintas ni modificar el análisis guardado. También incluye las correcciones del estado de restauración de Premium.

APK y AAB release compilados correctamente; lint vital aprobado durante la compilación. No había dispositivo ni emulador conectado para inspección visual nativa. No requiere volver a analizar los documentos ni consumir IA.
- Publicada y verificada en el segmento interno: «0.6.13-preview · Análisis organizado», código 28, disponible para verificadores internos. Play no reportó errores bloqueantes; dos avisos sobre símbolos de depuración/desofuscación. Evidencia: `docs/play-bio-0613-publicada.png`. Actualizar desde Play para conservar la firma y los datos de la instalación de prueba.

## Verificación de vencimiento — 27 octubre 2026

- Consulta administrativa de solo lectura: cuenta de prueba guardada con premium=false; última comprobación del servidor 2026-10-27T23:24:35.769Z, con respuestas 200 de billing/status. No se alteró ni canceló la suscripción.
- El usuario confirmó que el documento continúa en Exámenes y Abrir original funciona después del vencimiento.
- 15 pruebas locales de facturación y rutas HTTP aprobadas. Nueva prueba combina un estado EXPIRED con las dos rutas de IA: ambas responden 402/premium_required antes de procesar el documento, reservar cuota o crear el cliente del modelo. Esta comprobación de bloqueo es automatizada, no una solicitud de IA realizada desde el teléfono.
- No se accedió al contenido del documento real ni se consumió IA. La actualización solo incorpora una prueba de regresión; no requiere otro despliegue.

## Precio Premium — 28 octubre 2026

- Precio de referencia solicitado: US$4/mes. En Play Console se seleccionó Colombia, se introdujo USD 4.00 y se aplicó la conversión de Play: COP 12.500/mes, sin IVA indicado por la consola. Guardado confirmado. Colombia continúa siendo la única región disponible.
- Play aplica el nuevo precio a nuevas suscripciones; no se migraron suscriptores existentes ni se realizaron compras. Evidencia: `docs/play-bio-precio-4usd.png`.
- Android 0.6.14-preview (29) reemplaza el texto anterior de COP 10.000 por US$4 como referencia. El diálogo previo a la compra continúa mostrando `getFormattedPrice()` de Google Play para el plan mensual real; no calcula ni cobra una conversión propia.
- APK y AAB release generados correctamente, incluido lint vital. No requiere cambios de servidor ni llamadas a IA.
- Publicada y verificada en la pista interna: «0.6.14-preview · Precio Premium», código 29, disponible para verificadores internos el 28 de octubre. Sin errores bloqueantes; dos advertencias de símbolos/desofuscación. Evidencia: `docs/play-bio-0614-publicada.png`. Actualizar desde Google Play para conservar firma y datos.

## Precio fijo en Colombia y precio localizado — 0.6.15-preview (30)

El titular sustituyó la referencia US$4 por COP 13.000/mes fijo para Colombia y US$5 para la futura expansión a mercados en USD. Al aclarar la disponibilidad eligió «Mantener solo Colombia por ahora». No se habilitaron otros países ni se implementó actualización automática por tipo de cambio.

- Play Console confirmó el guardado de COP 13.000/mes para nuevas suscripciones; no se migraron suscriptores anteriores. Evidencia: `docs/play-bio-colombia-13000.png`.
- Premium consulta el plan mensual al conectarse con Billing y muestra su precio localizado. La consulta inicial solo muestra el precio; el diálogo y el pago se abren exclusivamente al pulsar el botón y confirmar. Se conserva la consulta fresca antes de pagar y el control de cuenta/suscripción del servidor.
- Compilaciones release de APK y AAB, incluido lint vital, aprobadas. Sin compras, sin llamadas a IA y sin cambios de servidor. La pantalla con Billing real todavía requiere comprobación en el teléfono.
- Play mostró «No se pudo cargar» tras optimizar la primera carga; un reintento indicó código 30 ya usado. Se recuperó correctamente el mismo AAB desde «Agregar desde la biblioteca» (versión 30 / 0.6.15-preview), sin incrementar el código ni recompilar.
- Publicada y verificada en la prueba interna: «0.6.15-preview · Precio local», código 30, disponible para verificadores internos. Sin errores bloqueantes; advertencias habituales sobre desofuscación y símbolos nativos. Evidencia: `docs/play-bio-0615-publicada.png`.

## Mercados USD — 28 de octubre de 2026

El titular autorizó configurar los países que cobran en dólares a US$5/mes. Se habilitaron 75 países o regiones USD en `bio_premium` / `mensual`, conservando Colombia en COP 13.000: 76 regiones seleccionadas en total. La selección sigue la moneda de compra de Google Play, no necesariamente la moneda nacional, según https://play.google.com/supported-locations/?hl=es-419 (versión 2026/01).

- Códigos de las regiones USD: AG, AW, BS, BZ, BM, DM, EC, SV, US, GD, GT, HT, HN, KY, TC, VG, JM, NI, PA, DO, KN, LC, TT, UY, VE, AL, AO, AM, BH, BY, BA, BW, CV, TD, KM, ER, GM, GN, KG, KW, LR, LY, LB, MK, MU, MD, MZ, NA, OM, CD, CG, RW, SC, SL, SO, SR, TJ, TM, TN, UG, YE, DJ, ZM, ZW, KH, FJ, SB, LA, MV, FM, NP, PG, WS, TO, VU.
- El ajuste masivo de Play propuso redondeos y precios con impuestos distintos de US$5. Se corrigieron individualmente y se verificaron las 75 filas con USD 5.00 y Colombia con COP 13,000 antes de guardar.
- Play confirmó «Se guardaron los cambios». Evidencia: `docs/play-bio-usd5.png`. No se migraron suscriptores anteriores ni se habilitaron otras monedas o países futuros automáticamente.
- Solo se cambió la disponibilidad y el precio del plan. La app continúa en prueba interna; no se publicó en producción ni se modificó la restricción de compras del servidor. Sin nueva compilación, compras o llamadas a IA.

## Cuotas mensuales aprobadas — 29 octubre de 2026

Se sustituyeron los topes provisionales diarios por 30 análisis y 20 consultas de IA por cuenta y mes calendario UTC, sin acumulación. Gmail y carga manual comparten análisis. Reservas transaccionales en Firestore; devolución idempotente al fallar la solicitud. Los documentos demasiado extensos se rechazan sin recortar contenido. Premium muestra el saldo y la fecha de renovación; las cuentas Normal continúan sin acceso a IA. Detalles, costos estimados y límites de verificación en `PUBLICACION_PLAY.md`.

Servidor publicado y verificado sin inferencias facturadas ni compras. APK/AAB 0.6.16 (31) compilados; aún no publicados en la pista interna. El acceso público a compras permanece deshabilitado.

## Consulta de suscripción y permiso de prueba — 3 noviembre 2026

- Diagnóstico administrativo de solo lectura: cuenta del propietario con `premium=false`; las consultas recientes a `/billing/status` respondían 200. No hubo una verificación nueva de compra entre esas consultas.
- La habilitación de compras de prueba había vencido en `2026-11-03T00:32:50.508Z` (2 de noviembre, 19:32 de Colombia). Se renovó exclusivamente para la misma cuenta hasta `2026-11-11T00:19:38.149Z` (10 de noviembre, 19:19 de Colombia). Esto habilita el flujo de prueba, no concede Premium ni ejecuta una compra. Las compras públicas siguen cerradas.
- Causa del botón sin respuesta: «Consultar suscripción» era en realidad el inicio de compra y se desactivaba al recibir `purchasesEnabled=false`. En la versión ya instalada debe rehabilitarse al salir y volver a entrar en Premium, tras renovar el permiso.
- Android 0.6.18 (33) separa «Consultar suscripción» de «Suscribirme a Premium». Consultar vuelve a cargar el estado y el precio y busca compras restaurables sin abrir el pago. Consultar y Restaurar no dependen de que estén habilitadas nuevas compras. Ambos reintentan la conexión a Play; se muestran mensajes durante la consulta/restauración y al fallar. El botón de compra deshabilitado se diferencia visualmente.
- Las solicitudes de facturación tienen un timeout de lectura de 45 segundos, en lugar del timeout de tres minutos utilizado para sincronización de documentos. No se cambiaron cuotas, precios ni verificaciones de titular de compras.
- Las nueve pruebas existentes de facturación pasaron. Pendiente confirmar el resultado en el teléfono y distribuir la nueva interfaz a través de Play. No se realizó ninguna compra ni inferencia de IA.
- APK y AAB 0.6.18 (33) compilados y firmados correctamente; lint vital aprobado. Huellas en `Bio-0.6.18-preview.sha256`. Los archivos locales no sustituyen todavía la versión instalada desde Play; no desinstalar la app para instalar un APK con otra firma.
