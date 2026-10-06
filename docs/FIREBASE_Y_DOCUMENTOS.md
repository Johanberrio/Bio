# Acceso desde Bio 0.4.0

La versión actual requiere cuenta. Consulta `ACCESO_Y_BIOMETRIA.md`. El registro vincula la identidad anónima anterior; las sesiones ya registradas requieren contraseña o biometría previamente activada. Después de entrar, la sincronización y el análisis siguen automáticos. Las instrucciones de acceso anónimo siguientes describen la versión 0.3.2.

# Bio 0.3.2 — Firebase y análisis desplegados

## Estado al 27 de septiembre de 2026

Proyecto exclusivo `bio-project-15d15`, plan Blaze activo. Android `com.alvia.health` registrado como Bio Android. No se utilizó ni modificó Lentespro.

- Authentication: acceso anónimo habilitado para sesión automática; correo/contraseña conserva las sesiones existentes.
- Firestore Standard `(default)` y bucket `bio-project-15d15.firebasestorage.app`: `us-east1`.
- Reglas de Firestore y Storage desplegadas y probadas: acceso propio permitido; otra cuenta y acceso sin autenticar denegados.
- Función Python 3.12 de segunda generación `bio_api` desplegada en `us-east1`.
- Endpoint: `https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api`.
- Gemini `gemini-3.1-pro-preview`, mediante Vertex AI en ubicación global. Es un modelo preliminar; no implica validación clínica. El procesamiento del modelo no queda restringido a us-east1.
- Configuración incluida en el APK: `firebase-config.json` y `backend-config.json`. No necesita introducir URL ni token compartido. No hay claves privadas ni claves de OpenAI en Android.

## Uso

En Mi salud → Historia o Exámenes → Subir, elegir PDF, JPG o PNG (máximo 10 MB). Se abre directamente el selector de archivos. Bio prepara automáticamente una identidad Firebase privada para esta instalación, analiza contenido y deduce nombre/fecha antes de publicar el documento completado. El análisis y la carga no requieren botones adicionales. Historia sirve para documentos clínicos; los síntomas se registran desde Mi cuerpo.

Mientras se procesa, aparece “Preparando documento”. Si falla, permanece pendiente con un mensaje y opción de reintento; el original cifrado se conserva. La app reintenta mientras está abierta y al volver a abrirla, con espera progresiva hasta 15 minutos. Al alcanzar el límite diario, espera al siguiente día UTC. Un archivo ilegible requiere reemplazo o reintento manual. Si ya se analizó y solo falló Firebase, se reutiliza el análisis. No hay servicio de reintento con la app cerrada.

La sesión automática persiste entre aperturas y actualizaciones. No equivale a una cuenta recuperable entre dispositivos: desinstalar o borrar datos puede perder el acceso a esa identidad. No está habilitada la eliminación automática de usuarios anónimos. Las sesiones anteriores existentes se respetan.

La lista se ordena por fecha clínica descendente; las fechas desconocidas quedan al final. Se conserva el archivo original y su nombre original. Las fechas no se deducen de la fecha de carga. Hallazgos documentados, sospechas, negaciones y antecedentes se distinguen mediante etiquetas y citas. Las posibles interpretaciones no se presentan como diagnósticos confirmados.

## Protección y costos

Cada archivo y metadato se guarda bajo `users/{uid}/documents/{id}`. Las reglas autorizan solo al propietario. El servidor verifica tokens Firebase y su revocación antes de analizar; usa la identidad `bio-analysis`, sin archivos de clave privada. Tiene permisos de Vertex AI, Firestore para contadores y lectura de Authentication para verificar revocaciones.

El servidor no registra documentos ni respuestas clínicas. La copia local está cifrada con Android Keystore. Los servicios de Google procesan la información; esto no es cifrado de extremo a extremo. Investigación puede usar búsquedas web; el análisis de un documento no utiliza herramientas web.

Límites iniciales: 10 solicitudes diarias por usuario y 50 globales, contando intentos aceptados para procesamiento, con reinicio a medianoche UTC. Contadores atómicos en Firestore, inaccesibles al cliente. Una instancia máxima, concurrencia 2, cero instancias mínimas. Limpieza de imágenes de compilación tras 7 días. Estos controles no son un límite monetario: Blaze y Gemini pueden generar cargos.

## Verificación real

En 0.3.2 pasaron 62 comprobaciones Android y 19 comprobaciones de integración real desde el emulador: selección directa, sesión automática, análisis Gemini, nombre/fecha inferidos y guardado final. Dos PDFs sintéticos terminaron como “Hemograma completo · 2026-09-01.pdf” y “Consulta de neurologia · 2026-08-25.pdf”. Capturas en `docs/screenshots/v032/`. Se eliminaron documentos y cuenta de prueba. Compilación y Lint aprobados.

Verificación previa del servidor (0.3.1):

Se usaron cuentas temporales y un PDF sintético, sin pacientes reales. Se verificó carga y descarga exacta, aislamiento entre dos usuarios, rechazo de solicitudes sin autenticar y eliminación de archivos/cuentas de prueba. La IA devolvió “Hemograma completo”, fecha `2026-09-01`, citas de los dos resultados y ninguna enfermedad inventada. Otra prueba de investigación devolvió un informe con tres fuentes HTTPS. Además, pasaron 53 comprobaciones Android en emulador, 29 pruebas Python y Android Lint. Estas pruebas funcionales no validan exactitud diagnóstica.

## Despliegue y mantenimiento

Desde la raíz del proyecto, usar Firebase CLI autenticado con el proyecto explícito:

```
firebase deploy --only firestore:rules,storage,functions:bio --project bio-project-15d15
```

El código Python está en `server/main.py`; el adaptador Vertex en `server/vertex_gateway.py`. Crear `server/venv` e instalar `server/requirements.txt` antes del despliegue. `server/server.py` conserva el gateway local anterior para desarrollo; no es la entrada desplegada. El modelo se controla con `BIO_MODEL` en el servidor y debe verificarse antes de cambiarlo.

## Límites actuales

No hay resolución de conflictos entre dispositivos: la sincronización restaura archivos ausentes y envía cambios pendientes, sin sobrescribir automáticamente los documentos locales existentes. El borrado remoto requiere conexión; no hay cola de borrado sin conexión. No hay validación clínica ni evaluación médica del modelo, y no se habilitó App Check obligatorio todavía. Las citas y los signos vitales siguen locales; esta integración almacena documentos de Historia y Exámenes.
