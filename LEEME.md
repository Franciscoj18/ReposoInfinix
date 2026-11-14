# Reposo Infinix

Pantalla tipo "modo reposo" de iPhone para Android (probada para Infinix GT 30 Pro / XOS).

- Izquierda: reloj grande (color elegido por ti o automático según el fondo).
- Derecha: día, clima, anillo de batería, próxima alarma y reproductor de la música
  que suene en cualquier app (YouTube, Spotify, YT Music…).
- Fondo: cualquier imagen de tu galería, en modo Rellenar, Completa o Manual
  (pellizcar y arrastrar).
- Se abre al bloquear el teléfono mientras carga (o al conectar el cargador con el
  teléfono bloqueado) y se cierra al desconectarlo.
- Cuida la pantalla AMOLED: mueve el contenido unos pixeles cada minuto y atenúa de noche.

## Cómo obtener la APK

### Opción A — GitHub (sin PC)
1. Sube esta carpeta a un repositorio de GitHub.
2. GitHub compila sola la app (pestaña **Actions**, tarda ~5 min).
3. En la pestaña **Releases** aparece `ReposoInfinix.apk`: descárgala desde el celular.

### Opción B — Android Studio (con PC)
1. Abre la carpeta `ReposoInfinix` en Android Studio.
2. Menú **Build → Build APK(s)**, o en terminal: `./gradlew assembleRelease`.
3. El archivo queda en `app/build/outputs/apk/release/ReposoInfinix.apk`.

## Primer uso en el teléfono
1. Instala la APK (permite "instalar apps desconocidas" si te lo pide).
2. Abre **Reposo Infinix** y toca cada permiso con ⚠️ hasta que todos tengan ✅.
   - **Acceso a notificaciones**: necesario para ver y controlar la música. Si sale
     "Ajuste restringido": Información de la app → ⋮ → Permitir ajustes restringidos.
   - **Mostrar sobre otras apps**: para que se abra sola al cargar.
   - **Sin restricción de batería** e **Inicio automático**: para que XOS no la cierre.
3. Activa "Abrir al conectar el cargador", elige tu fondo y tu color.
4. Conecta el cargador y bloquea el teléfono. Para salir: botón atrás o inicio.

La firma (`app/reposo.keystore`) es solo para uso personal; con ella las
actualizaciones se instalan encima sin borrar tus ajustes.
