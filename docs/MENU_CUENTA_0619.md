# Menú de cuenta — 0.6.19-preview (34)

El responsable informó que no aparecía «Eliminar cuenta». El diálogo de MainActivity combinaba setMessage y setItems: el mensaje puede ocupar el contenido que debe mostrar la lista. Se retiró el mensaje del diálogo, conservando el título «Tu cuenta» y las siete acciones existentes. No cambian el flujo de eliminación, sus confirmaciones, la autenticación ni los datos.

Se compiló APK/AAB release y lintVitalRelease con éxito. En el emulador aislado se ejecutó AccountMenuTests sobre el diálogo real: lista adjunta y visible, siete opciones, texto «Eliminar cuenta» y fila alcanzable al desplazar. Resultado INSTRUMENTATION_CODE -1. No se solicitó ninguna eliminación.

El primer intento de prueba falló durante el arranque del emulador; la repetición con Android arrancado pasó. No se afirma que se haya probado todavía en el teléfono del responsable.

Artefactos: Bio-0.6.19-preview.apk y Bio-0.6.19-preview.aab; huellas en Bio-0.6.19-preview.sha256.

Distribución: el 6 de noviembre Play confirmó «Disponible para verificadores internos», versión más reciente 0.6.19-preview, código 34. Evidencia: `play-bio-0619-publicada.png`. Solo se publicó en prueba interna. No resuelve ni vuelve a enviar el rechazo de publicación por el tipo de cuenta.

## Peso

El responsable aclaró que OKOK International no está enviando las mediciones a Google Fit/Health Connect. No se cambió la lectura de peso de Bio a partir de ese síntoma. Pendiente marca/modelo de báscula y confirmar si la medición llega a OKOK, para evaluar la compatibilidad del tramo anterior a Bio.

El responsable confirmó después que el nuevo peso sí aparece en OKOK y que esta misma conexión funcionaba anteriormente. Por tanto, no se concluye incompatibilidad de la báscula. Se indicó revisar/restablecer el enlace OKOK–Google Fit siguiendo la ayuda oficial de Google y comprobar luego el registro en Health Connect. El restablecimiento aún no ha sido confirmado por el usuario.

Actualización del 7 de noviembre: el responsable confirmó que probó la sincronización con Health Connect y funciona muy bien. No se atribuye una causa definitiva al fallo previo de OKOK.
