# Bio — Política de privacidad

Versión publicada; proceso de eliminación verificado con cuentas sintéticas.

Última actualización: 30 de octubre de 2026.

## Responsable y contacto

Johan Estevan Berrio Estrada, persona natural, es responsable de Bio. Para soporte y solicitudes sobre tus datos escribe a **juandrak@msn.com**. No envíes contraseñas ni documentos médicos en una solicitud de soporte.

## Público de la aplicación

Bio está dirigida a personas de 18 años o más. No está diseñada para menores de edad. Si detectas que un menor creó una cuenta, contacta con el responsable para verificar la situación y gestionar su eliminación.

## Datos y finalidades

Bio utiliza tu correo e identificador de cuenta para autenticarte y proteger el acceso. Tu perfil puede incluir nombre completo, fecha de nacimiento, estatura, peso y grupo sanguíneo. Organiza documentos médicos, citas, registros y mediciones que introduzcas o autorices importar. Los documentos y el perfil se respaldan en Firebase para su almacenamiento y recuperación.

La aplicación protege los archivos locales mediante cifrado y utiliza HTTPS para comunicarse con los servicios. Los servicios de Google procesan los datos necesarios; no se trata de cifrado de extremo a extremo. La huella o el rostro se validan mediante Android: Bio no recibe ni almacena imágenes faciales o plantillas biométricas.

## Health Connect

Con tu autorización, Bio lee frecuencia cardíaca, presión arterial, oxígeno, temperatura, frecuencia respiratoria, glucosa y peso. Solo se leen los tipos permitidos. No escribe en Health Connect ni mide por sí misma esos valores. La última lectura de peso actualiza el perfil, que se respalda en Firebase. Las demás mediciones permanecen en el almacenamiento local, salvo el contexto que elijas incluir en una consulta de IA.

Si concedes acceso en segundo plano, Android puede ejecutar revisiones periódicas sujetas a sus restricciones. Puedes desconectar la fuente en Bio o revocar permisos en Health Connect. Esto detiene futuras lecturas, pero no borra automáticamente registros previamente importados.

## Gmail

La conexión es opcional y usa la autorización oficial de Google con acceso de lectura. Bio no solicita tu contraseña de Gmail, no envía correos, no cambia su estado y no los elimina. Guarda un token de autorización protegido en el servidor para revisar periódicamente el buzón. Busca adjuntos PDF, JPG o PNG cuyos nombres indiquen historias clínicas, resultados o exámenes; el asunto no determina si se importan.

Los adjuntos candidatos pasan temporalmente por el servidor y por los servicios de IA para identificar su tipo, titular, nombre y fecha. Los aceptados se guardan como documentos de Bio. Los rechazados no se guardan como archivos médicos; en el teléfono puede quedar un aviso y un identificador para evitar repetir su revisión. Puedes desconectar Gmail desde Bio y retirar el permiso desde tu cuenta Google.

El uso de información recibida de las API de Google debe cumplir la Política de Datos de Usuario de los Servicios de API de Google, incluidos los requisitos de Uso Limitado. Bio no usa esos datos para publicidad, venta de datos o entrenamiento de un modelo general propio. El acceso humano a contenido de correo debe limitarse a los supuestos permitidos por dicha política y a asistencia expresamente autorizada por la persona.

## Inteligencia artificial

Las funciones Premium envían a Google Cloud/Vertex AI el documento y el contexto necesario para extraer información y generar explicaciones. Las consultas de investigación pueden usar Google Search para obtener fuentes. No incluyas datos innecesarios en tus consultas. Bio no ofrece diagnósticos confirmados ni sustituye una consulta médica; las respuestas pueden contener errores.

Los resultados se asocian al documento guardado. Los controles de uso registran el identificador de cuenta, mes y tipo de operación, sin guardar contenido clínico en los contadores. La versión inicial de Premium incluye 30 análisis y 20 consultas por mes calendario UTC, sin acumulación; Gmail comparte el cupo de documentos.

## Proveedores y pagos

Firebase Authentication, Firestore, Storage y Google Cloud prestan servicios de autenticación, almacenamiento y procesamiento. Google Play gestiona las suscripciones y el pago; Bio recibe identificadores y estado de compra para verificar acceso, pero no el número de tu tarjeta. El procesamiento por estos proveedores puede realizarse fuera de tu país según la configuración y las condiciones del servicio.

Bio no incorpora anuncios ni vende tus documentos o mediciones. Los proveedores pueden tratar metadatos técnicos necesarios para seguridad y operación de sus servicios.

## Conservación, control y eliminación

Tus documentos y perfil se conservan mientras los mantengas en Bio. Puedes eliminar documentos desde la aplicación y desconectar fuentes. Desinstalar no elimina por sí solo la cuenta ni los respaldos en la nube.

Puedes solicitar la eliminación desde Bio → Tu cuenta → Eliminar cuenta, o desde la [página de eliminación](https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/delete-account) sin reinstalar la app. Se verifica la contraseña directamente con Firebase Authentication; Bio no la envía a la IA. Si no puedes acceder, escribe a juandrak@msn.com para verificar tu titularidad sin compartir contraseñas ni documentos médicos.

Al aceptar la solicitud, Bio bloquea nuevas operaciones de su servidor. La limpieza normalmente empieza durante las siguientes 24 horas: se deshabilita la cuenta y se revocan sus sesiones, se desconecta el correo y se espera al menos dos horas para que caduquen tokens ya emitidos. Después se eliminan la cuenta de autenticación, perfil, documentos y análisis almacenados en Firebase, credenciales de conexión, estado de suscripción y contadores de uso. Los errores técnicos se reintentan automáticamente; puedes contactar con soporte si necesitas comprobar el estado.

Se mantiene durante 30 días desde la deshabilitación un registro técnico con el identificador de cuenta y fechas del proceso, sin correo electrónico ni contenido de salud, para repetir la limpieza y retirar archivos de cargas que hubieran quedado en curso. Después se elimina ese registro. Firebase Storage conserva los archivos borrados en recuperación técnica durante 7 días desde cada borrado. Firestore mantiene una ventana técnica de versiones de una hora; en este proyecto no están habilitadas la recuperación ampliada a un punto en el tiempo ni las copias de seguridad programadas. Los registros operativos y los datos que Google deba conservar para seguridad, facturación u obligaciones legales se rigen por las políticas de sus servicios; no se utilizan para restaurar tu cuenta en Bio.

La eliminación solicitada dentro de la app también limpia los datos de Bio en ese teléfono. La solicitud web no puede borrar físicamente copias locales en dispositivos sin conexión: elimina los datos de Bio desde Ajustes de Android en los demás dispositivos. Los documentos exportados fuera de Bio, los correos originales y los registros de Health Connect permanecen en sus respectivas aplicaciones.

Eliminar la cuenta o desinstalar Bio **no cancela la suscripción de Google Play**. Puedes cancelar su renovación desde [Suscripciones de Google Play](https://play.google.com/store/account/subscriptions?sku=bio_premium&package=com.alvia.health). No necesitas Premium para eliminar tu cuenta. Google conserva sus registros de compra conforme a sus propias políticas.

## Cambios y dudas

Los cambios materiales en el tratamiento de datos deben informarse antes de aplicarse cuando corresponda. Puedes solicitar información, corrección o eliminación mediante el contacto indicado. Esta política está accesible desde Bio y desde su página pública.
