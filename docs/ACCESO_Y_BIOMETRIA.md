# Bio 0.4.0 — cuenta y acceso biométrico

El primer acceso requiere correo y contraseña. Crear cuenta vincula la identidad anónima anterior de Firebase con las credenciales, conservando el UID y el acceso a documentos. Si ya existe una cuenta en esta instalación se solicita su contraseña. La cuenta existente y los datos locales no se mezclan con otra identidad.

Después del acceso, marcar “Activar acceso con huella o rostro”, o abrir Ajustes → Activar huella o rostro, introduce la contraseña y confirma el diálogo de Android. Android decide qué modalidad segura ofrece. Se requiere biometría clase 3 (BIOMETRIC_STRONG): algunas cámaras de desbloqueo facial no cumplen ese nivel. Esos equipos pueden usar huella compatible o contraseña. Bio no captura fotos ni almacena plantillas biométricas.

La activación genera una clave AES en Android Keystore que exige autenticación por cada uso. La credencial local está vinculada al propietario Firebase. Cambiar las biometrías registradas puede invalidar la clave y exigir contraseña y nueva activación. La contraseña nunca se guarda en preferencias ni en el almacén de Bio. Firebase administra la sesión de cuenta.

Al salir de Bio se bloquea la interfaz; volver requiere contraseña o la biometría ya activada. Desde 0.5.1 el bloqueo sigue el estado de toda la aplicación, sin bloquear al navegar entre sus secciones. Las pantallas externas iniciadas desde Fuentes (permisos de Health Connect, ajustes y selector de carpeta) tienen una ventana de retorno de cinco minutos, únicamente en memoria. Al volver se consume esa excepción; cerrar sesión, reiniciar el proceso o superar ese tiempo mantiene el bloqueo. Cancelar o fallar la biometría conserva el bloqueo. Cerrar sesión desactiva la biometría y conserva el almacén local para el mismo propietario.

“Olvidé mi contraseña” solicita un correo de recuperación a Firebase. Con la misma cuenta se restauran automáticamente los documentos remotos al entrar, sin botón de sincronización. Citas, signos vitales y síntomas siguen siendo datos locales. El acceso biométrico es desbloqueo de la cuenta ya vinculada en el dispositivo, no registro de una cuenta nueva.

Instalar como actualización de 0.3.2, sin borrar datos. En una instalación anterior con archivos anónimos, elegir Crear cuenta para conservarlos. Vincularlos a un correo que ya pertenece a otra cuenta no se permite automáticamente; no se eliminan ni reasignan documentos.

Referencias: https://developer.android.com/identity/sign-in/biometric-auth y https://firebase.google.com/docs/auth/android/anonymous-auth.
