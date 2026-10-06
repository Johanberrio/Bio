# Imágenes de sistemas de Bio

Los PNG se copiaron sin modificar de los archivos aportados por el usuario. Android los escala proporcionalmente en pantalla, sin recortarlos. El límite de caché es 16 MB y la decodificación de pantalla utiliza muestreo a media resolución. La versión 0.2.1 extrae máscaras aproximadas de los tejidos en memoria para superponer los sistemas y aplicar iluminación amarilla o roja, sin modificar los PNG originales.

| Vista | Archivo original (25 sept 2026) | Recurso |
| --- | --- | --- |
| Nervioso | 07_09_54 p.m. | system_nervous.png |
| Circulatorio | 07_11_44 p.m. | system_circulatory.png |
| Endocrino | 07_55_50 p.m. | system_endocrine.png |
| Óseo | 07_58_12 p.m. | system_skeletal.png |
| Respiratorio | 08_02_21 p.m. | system_respiratory.png |
| Digestivo | 08_08_08 p.m. | system_digestive.png |
| Urinario y reproductivo | 08_21_38 p.m. | system_urinary.png |
| Muscular | 08_35_40 p.m. | system_muscular.png |

Ubicación: `app/src/main/res/drawable-nodpi/`. Las imágenes son ilustraciones proporcionadas por el usuario, no una fuente validada de anatomía ni imágenes de su cuerpo.

Portada 0.2.2: `bio_home_body.png`, copia sin modificar de «ChatGPT Image 26 sept 2026, 03_29_55 p.m..png». Coincidencia SHA-256 verificada. El corazón del inicio es el recurso vectorial `ic_health_heart.xml`; el lanzador sigue usando `bio_logo.png`.

Encabezado 0.2.4: `bio_header.jpg`, copia idéntica por SHA-256 de `1000451132.jpg`. Se muestra proporcionalmente a 90 × 60 dp en todas las secciones, incluyendo Inicio. Conserva el fondo negro del JPG original. No cambia el icono del lanzador.

Encabezado 0.2.5: `bio_header_transparent.png`, copia idéntica del PNG del 26 sept 2026 a las 08_47_51 p.m. Se verificó canal alfa RGBA (0–255) y esquina completamente transparente. Reemplaza al JPG en todas las secciones; contenedor reducido de 90 a 76 dp de ancho, con ajuste proporcional sin recorte.
