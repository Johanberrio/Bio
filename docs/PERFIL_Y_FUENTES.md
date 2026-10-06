# Perfil y fuentes de Bio 0.5.2

## Modelo corporal desde 0.6.5

En el registro/perfil, «Ilustraciones de mi cuerpo» permite elegir Mujer u Hombre. Mi cuerpo ofrece «Cambiar» para editarlo. bodyModel se conserva en el perfil local cifrado y en su respaldo Firebase, con los mismos controles de acceso. La elección solo afecta al mapa; no se envía al análisis como un dato clínico.

Los perfiles anteriores sin elección mantienen la vista previa masculina. Mujer utiliza siete PNG originales: nervioso, circulatorio, respiratorio, urinario (última versión enviada), endocrino, digestivo y muscular. Desde 0.6.6 el reproductivo usa las dos nuevas láminas específicas de mujer y hombre. No se recibió un sistema óseo femenino: se muestra pendiente y no se mezcla la imagen masculina. Las combinaciones y la iluminación de síntomas usan las capas del modelo elegido. Las ilustraciones y máscaras son aproximadas, no estudios médicos.
## Perfil de salud

Después de crear la cuenta o del primer acceso tras actualizar, Bio solicita nombre completo, fecha de nacimiento, estatura en centímetros, peso en kilogramos y grupo sanguíneo. «No lo sé» es una opción válida. Se puede editar en Ajustes → Mi perfil de salud.

El perfil se guarda cifrado en el dispositivo y en `users/{uid}/profile/main` de Firestore, con acceso restringido al propietario. Si falla internet, queda pendiente y se reintenta. La interpretación de documentos y la investigación reciben edad actual, estatura, peso y grupo sanguíneo como datos declarados, no como resultados confirmados. El contexto del perfil excluye nombre y fecha de nacimiento. Los documentos originales pueden contener esos datos, como antes. No se reanalizan automáticamente documentos ya interpretados al editar el perfil.

## Dispositivos

Desde 0.5.2, «Conectar / revisar permisos» consulta el acceso real, incluido el autorizado desde Ajustes. Con todos los permisos concedidos muestra «Health Connect autorizado», con opciones para sincronizar o revisar ajustes. «Sincronizar mediciones» consulta solamente signos vitales, sin esperar el análisis de documentos. El resultado queda visible junto a los botones; si no hay mediciones nuevas, lo indica y recuerda revisar que la aplicación del dispositivo comparta datos con Health Connect.

En Salud → Conectar dispositivos (también Ajustes → Fuentes y dispositivos), autorizar lectura en Health Connect. La aplicación del fabricante debe compartir allí las mediciones. Bio no se conecta directamente por Bluetooth ni ofrece compatibilidad universal.

Desde 0.5.1, primero se autorizan las mediciones. La lectura en segundo plano se pide por separado con «Permitir sincronización en segundo plano», después de conectar. «Abrir ajustes de Health Connect» permite revisar la configuración del sistema. Cancelar la solicitud vuelve a Fuentes sin cerrar la sesión. Hay una ventana de retorno de cinco minutos para las pantallas externas iniciadas desde esta sección; no se conserva al reiniciar el proceso.

Se leen pulso, presión sistólica/diastólica, saturación de oxígeno, temperatura, frecuencia respiratoria y glucosa. Se conservan el instante de medición y el paquete de la aplicación de origen; se identifican los registros para evitar duplicados. Los registros eliminados en Bio no vuelven a aparecer en la siguiente lectura. Las eliminaciones realizadas en la aplicación del fabricante no borran automáticamente las copias de Bio.

Health Connect requiere Android 9 o posterior; está integrado desde Android 14 y se instala por separado en versiones anteriores compatibles. La importación consulta los últimos 30 días. Sin permiso de lectura en segundo plano, se consulta al abrir Bio o al pulsar Revisar fuentes. Con permiso y función disponibles, WorkManager programa revisiones cada 15 minutos como mínimo; Android puede retrasarlas. No es monitorización en tiempo real ni un sistema de alarmas médicas. El ingreso manual permanece disponible.

Las mediciones importadas se almacenan localmente cifradas. Se envían al análisis de IA cuando el usuario elige incluir mediciones; se limita el contexto a las 2000 más recientes. El historial en pantalla muestra hasta 200 lecturas recientes para evitar sobrecargar la vista. Esta versión no añade respaldo en Firestore de las mediciones.

## Carpetas de documentos

Elegir una carpeta exclusiva de exámenes o de historia clínica mediante el selector de Android. La autorización incluye archivos ya presentes y futuros en esa carpeta, sin recorrer subcarpetas. Los documentos se analizan y respaldan en la cuenta de Bio; deben ser del titular del perfil.

Formatos: PDF, JPG y PNG, hasta 10 MB por archivo. Cada revisión incorpora hasta 10 archivos nuevos por carpeta. Una huella SHA-256 evita duplicados entre las fuentes. Un documento eliminado de Bio no se reimporta automáticamente mientras se conserve el registro local de importación. El original externo no se borra ni se renombra. En Bio, el análisis determina nombre y fecha clínica antes de publicar el documento como listo. Los archivos ilegibles permanecen pendientes con su error. Se mantiene el orden por fecha clínica y las cuotas existentes de análisis.

La configuración de fuentes y los permisos son propios de cada dispositivo. Desconectar una carpeta detiene nuevas importaciones; los documentos ya preparados siguen disponibles y pueden completar su carga. Los permisos de archivos también pueden revocarse desde Android. Cerrar sesión impide iniciar nuevas sincronizaciones; una operación ya enviada al servidor puede terminar.

## Conexiones pendientes

- **Outlook / Hotmail:** no está conectado en esta entrega. Microsoft Entra denegó el acceso al directorio universitario y la cuenta alternativa no pertenece al inquilino. Hace falta un directorio administrable y un registro propio de Bio que admita cuentas personales, antes de implementar y probar la autorización de Microsoft Graph. No se reutilizan credenciales de otras aplicaciones.
- **SURA:** acceso al portal y a archivos descargados en la carpeta autorizada. No hay extracción automática del portal. La API encontrada corresponde a integraciones sanitarias autorizadas y no demuestra acceso a los resultados del portal de pacientes.
- Falta prueba con un reloj/pulsera/dispositivo físico específico; el modelo aún no fue indicado.

Referencias oficiales: [Health Connect](https://developer.android.com/health-and-fitness/health-connect/get-started), [lectura en segundo plano](https://developer.android.com/health-and-fitness/health-connect/read-data), [directorio Microsoft Entra](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-create-new-tenant), [API de SURA](https://apicenter.plataformadigital.sura.co/api/45).


## Peso automático desde 0.6.7

WeightRecord se importa como métrica 7 en kg, con fecha, origen e identidad del registro. El peso válido más reciente actualiza el perfil y se respalda en Firebase. Se respetan propietario, fecha manual y correcciones del mismo registro. Valores inválidos o futuros se excluyen del perfil. Guardar otros campos del perfil no cambia la fecha del peso.

Requiere que Samsung Health u otra app escriba peso en Health Connect y que el usuario autorice Peso. La sincronización usa el trabajo periódico existente (mínimo 15 minutos, sujeto a Android) con permiso de lectura en segundo plano; también se ejecuta al abrir Bio. El perfil y el Inicio muestran el último dato recibido. La imagen femenina del Inicio se conserva exactamente como fue suministrada.


## Corrección 0.6.8

El peso no aparece en signos vitales ni en su formulario manual. Health Connect lo guarda en la colección local cifrada `weights`, separada de `vitals`, y actualiza automáticamente `profile.weightKg` para Inicio y el respaldo del perfil. Los registros de peso antiguos se trasladan sin perder su identidad; se mantiene la deduplicación. El identificador interno heredado 7 se conserva solo como compatibilidad de datos, no como categoría visible.

Los botones del perfil usan el mismo componente que las acciones principales de Bio, sin tintes nativos. Los selectores tienen tipografía y colores explícitos en ambos temas.
