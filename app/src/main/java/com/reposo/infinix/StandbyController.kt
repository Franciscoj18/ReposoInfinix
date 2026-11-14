package com.reposo.infinix

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadata
import android.media.session.MediaController
import android.os.BatteryManager
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * Toda la logica de la pantalla de reposo. La usan tanto la app (StandbyActivity)
 * como el protector de pantalla del sistema (StandbyDream).
 */
class StandbyController(
    private val ctx: Context,
    private val root: View,
    private val window: Window?
) {
    private val prefs = Prefs(ctx)
    private val locale = Locale("es", "MX")
    private val random = Random()

    private val bg: ImageView = root.findViewById(R.id.bg)
    private val dimView: View = root.findViewById(R.id.dim)
    private val content: LinearLayout = root.findViewById(R.id.content)
    private val clock: TextClock = root.findViewById(R.id.clock)
    private val rightCol: LinearLayout = root.findViewById(R.id.right_col)
    private val dayName: TextView = root.findViewById(R.id.day_name)
    private val dayNum: TextView = root.findViewById(R.id.day_num)
    private val weather: TextView = root.findViewById(R.id.weather)
    private val battery: BatteryRingView = root.findViewById(R.id.battery)
    private val alarm: TextView = root.findViewById(R.id.alarm)
    private val mediaCard: View = root.findViewById(R.id.media_card)
    private val mediaArt: ImageView = root.findViewById(R.id.media_art)
    private val mediaApp: TextView = root.findViewById(R.id.media_app)
    private val mediaTitle: TextView = root.findViewById(R.id.media_title)
    private val mediaArtist: TextView = root.findViewById(R.id.media_artist)
    private val btnPrev: ImageButton = root.findViewById(R.id.btn_prev)
    private val btnPlay: ImageButton = root.findViewById(R.id.btn_play)
    private val btnNext: ImageButton = root.findViewById(R.id.btn_next)

    private var bitmap: Bitmap? = null
    private var userScale = 1f
    private var offX = 0f
    private var offY = 0f
    private var manualOverride = false
    private var lastLandscape: Boolean? = null

    private var media: MediaHelper? = null
    private var weatherHelper: WeatherHelper? = null
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_BATTERY_CHANGED -> updateBattery(i)
                AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> updateAlarm()
                else -> onMinute()
            }
        }
    }

    private val layoutListener =
        View.OnLayoutChangeListener { _, l, t, r, b, ol, ot, orr, ob ->
            if (r - l != orr - ol || b - t != ob - ot) {
                root.post {
                    applyOrientation()
                    if (bg.scaleType == ImageView.ScaleType.MATRIX) updateMatrix()
                }
            }
        }

    fun start() {
        userScale = prefs.bgScale
        offX = prefs.bgOffX
        offY = prefs.bgOffY

        val fmt = if (prefs.use24h) "HH:mm" else "h:mm"
        clock.format12Hour = fmt
        clock.format24Hour = fmt

        loadBackground()
        applyColors()
        dimView.alpha = prefs.dim / 100f
        mediaArt.clipToOutline = true

        root.addOnLayoutChangeListener(layoutListener)
        root.post { applyOrientation() }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }
        val sticky = registerCompat(ctx, receiver, filter)
        registered = true
        sticky?.let { if (it.action == Intent.ACTION_BATTERY_CHANGED) updateBattery(it) }

        updateDate()
        updateAlarm()
        applyNight()

        media = MediaHelper(ctx) { updateMedia(it) }.also { it.start() }
        btnPrev.setOnClickListener { media?.previous() }
        btnPlay.setOnClickListener { media?.playPause() }
        btnNext.setOnClickListener { media?.next() }

        if (prefs.showWeather) {
            weatherHelper = WeatherHelper(ctx, prefs) { text ->
                weather.text = text
                weather.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
            }.also { it.start() }
        } else {
            weather.visibility = View.GONE
        }
    }

    fun stop() {
        if (registered) {
            try {
                ctx.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // ya estaba quitado
            }
        }
        registered = false
        root.removeOnLayoutChangeListener(layoutListener)
        media?.stop()
        media = null
        weatherHelper?.stop()
        weatherHelper = null
    }

    // ---------- Fondo ----------

    private fun loadBackground() {
        val file = prefs.bgFile()
        bitmap = if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.path)
            } catch (e: OutOfMemoryError) {
                null
            }
        } else null

        val b = bitmap
        if (b == null) {
            bg.setImageDrawable(null)
            return
        }
        bg.setImageBitmap(b)
        when (prefs.fitMode) {
            "fit" -> bg.scaleType = ImageView.ScaleType.FIT_CENTER
            "manual" -> {
                bg.scaleType = ImageView.ScaleType.MATRIX
                bg.post { updateMatrix() }
            }
            else -> bg.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    fun hasBackground(): Boolean = bitmap != null

    /** Calcula la posicion de la imagen: ajustada al ancho * zoom del usuario + desplazamiento. */
    private fun updateMatrix() {
        val b = bitmap ?: return
        val vw = bg.width
        val vh = bg.height
        if (vw == 0 || vh == 0) return
        val base = vw.toFloat() / b.width
        val s = base * userScale
        val m = Matrix()
        m.setScale(s, s)
        val dx = (vw - b.width * s) / 2f + offX * vw
        val dy = (vh - b.height * s) / 2f + offY * vh
        m.postTranslate(dx, dy)
        bg.imageMatrix = m
    }

    fun enterManualAdjust() {
        manualOverride = true
        bg.scaleType = ImageView.ScaleType.MATRIX
        bg.post { updateMatrix() }
    }

    fun adjust(scaleFactor: Float, dxPx: Float, dyPx: Float) {
        if (!manualOverride || bg.width == 0 || bg.height == 0) return
        userScale = (userScale * scaleFactor).coerceIn(0.3f, 8f)
        offX += dxPx / bg.width
        offY += dyPx / bg.height
        updateMatrix()
    }

    fun resetAdjust() {
        userScale = 1f
        offX = 0f
        offY = 0f
        updateMatrix()
    }

    fun saveManualAdjust() {
        prefs.bgScale = userScale
        prefs.bgOffX = offX
        prefs.bgOffY = offY
        prefs.fitMode = "manual"
    }

    // ---------- Colores y orientacion ----------

    private fun applyColors() {
        val accent = prefs.accent()
        clock.setTextColor(accent)
        dayName.setTextColor(accent)
    }

    /** En horizontal: reloj a la izquierda y datos a la derecha. En vertical: uno arriba del otro. */
    private fun applyOrientation() {
        val w = root.width
        val h = root.height
        if (w == 0 || h == 0) return
        val landscape = w >= h
        if (landscape == lastLandscape) return
        lastLandscape = landscape

        if (landscape) {
            content.orientation = LinearLayout.HORIZONTAL
            clock.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.55f)
            rightCol.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        } else {
            content.orientation = LinearLayout.VERTICAL
            clock.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            rightCol.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    // ---------- Datos ----------

    private fun onMinute() {
        updateDate()
        shiftForBurnIn()
        applyNight()
        weatherHelper?.maybeRefresh()
    }

    private fun updateDate() {
        val now = Date()
        dayName.text = SimpleDateFormat("EEE", locale).format(now).replace(".", "").uppercase(locale)
        dayNum.text = SimpleDateFormat("d", locale).format(now)
    }

    private fun updateBattery(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        battery.set(level * 100 / scale, charging)
    }

    private fun updateAlarm() {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val next = am.nextAlarmClock
        if (next == null) {
            alarm.visibility = View.GONE
            return
        }
        val fmt = if (prefs.use24h) "HH:mm" else "h:mm a"
        alarm.text = "⏰ " + SimpleDateFormat(fmt, locale).format(Date(next.triggerTime))
        alarm.visibility = View.VISIBLE
    }

    private fun updateMedia(c: MediaController?) {
        val md = c?.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (c == null || md == null || title.isNullOrBlank()) {
            mediaCard.visibility = View.GONE
            return
        }
        mediaTitle.text = title
        mediaArtist.text = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""

        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (art != null) {
            mediaArt.setImageBitmap(art)
            mediaArt.setPadding(0, 0, 0, 0)
        } else {
            mediaArt.setImageResource(R.drawable.ic_music)
            val p = (12 * ctx.resources.displayMetrics.density).toInt()
            mediaArt.setPadding(p, p, p, p)
        }

        mediaApp.text = try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        } catch (e: Exception) {
            ""
        }

        btnPlay.setImageResource(if (MediaHelper.isPlaying(c)) R.drawable.ic_pause else R.drawable.ic_play)
        mediaCard.visibility = View.VISIBLE
    }

    // ---------- Cuidado de la pantalla AMOLED ----------

    /** Mueve todo unos pixeles cada minuto para que no se marque la pantalla. */
    private fun shiftForBurnIn() {
        val d = ctx.resources.displayMetrics.density
        content.translationX = (random.nextInt(13) - 6) * d
        content.translationY = (random.nextInt(13) - 6) * d
    }

    /** De 23:00 a 6:00 baja el brillo y atenua el contenido. */
    private fun applyNight() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val night = prefs.nightDim && (hour >= 23 || hour < 6)
        content.alpha = if (night) 0.55f else 1f
        window?.let {
            val lp = it.attributes
            lp.screenBrightness =
                if (night) 0.02f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            it.attributes = lp
        }
    }
}
