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

## Estado del proyecto (pausado en la versión 1.2)

Funciona: la pantalla de reposo (reloj, batería, fecha, clima, alarma, música de
cualquier app, fondo y color), la vista previa y los permisos.

Falla a veces en un Infinix GT 30 Pro con XOS:
- Al conectar el cargador o al bloquear el teléfono, a veces no aparece.
- Apagar y volver a encender «Abrir al conectar el cargador» lo arregla por un rato.

Causa más probable: XOS «congela» el servicio en segundo plano después de un tiempo.
El servicio sigue existiendo (la notificación se ve), pero deja de recibir los avisos
de «cargador conectado» y «pantalla apagada». Al apagar y encender el interruptor, el
servicio se reinicia y vuelve a escuchar.

Ideas para retomarlo:
1. Revisar con `adb logcat` qué pasa en el teléfono cuando no aparece (si llega el
   aviso del sistema y si XOS bloquea abrir la pantalla).
2. Reiniciar el servicio solo cada cierto tiempo con AlarmManager, o al desbloquear.
3. Cerrar por completo la pantalla de reposo al desbloquear (onStop + finish).
4. Probar el camino del protector de pantalla del sistema (DreamService), si XOS lo permite.
