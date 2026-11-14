package com.reposo.infinix

import android.content.Context
import java.io.File

/** Ajustes guardados de la app. */
class Prefs(context: Context) {
    private val appCtx = context.applicationContext
    private val sp = appCtx.getSharedPreferences("reposo", Context.MODE_PRIVATE)

    /** Abrir la pantalla de reposo al conectar el cargador. */
    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) { sp.edit().putBoolean("enabled", v).apply() }

    /** Solo abrir si el telefono esta bloqueado o con la pantalla apagada (como iPhone). */
    var onlyWhenLocked: Boolean
        get() = sp.getBoolean("onlyWhenLocked", true)
        set(v) { sp.edit().putBoolean("onlyWhenLocked", v).apply() }

    /** "fill" = rellenar, "fit" = ajustar completa, "manual" = acomodada con los dedos. */
    var fitMode: String
        get() = sp.getString("fitMode", "fill") ?: "fill"
        set(v) { sp.edit().putString("fitMode", v).apply() }

    var bgScale: Float
        get() = sp.getFloat("bgScale", 1f)
        set(v) { sp.edit().putFloat("bgScale", v).apply() }

    var bgOffX: Float
        get() = sp.getFloat("bgOffX", 0f)
        set(v) { sp.edit().putFloat("bgOffX", v).apply() }

    var bgOffY: Float
        get() = sp.getFloat("bgOffY", 0f)
        set(v) { sp.edit().putFloat("bgOffY", v).apply() }

    /** "auto" = color tomado del fondo, "manual" = color elegido. */
    var colorMode: String
        get() = sp.getString("colorMode", "auto") ?: "auto"
        set(v) { sp.edit().putString("colorMode", v).apply() }

    var manualColor: Int
        get() = sp.getInt("manualColor", DEFAULT_ACCENT)
        set(v) { sp.edit().putInt("manualColor", v).apply() }

    var autoColor: Int
        get() = sp.getInt("autoColor", DEFAULT_ACCENT)
        set(v) { sp.edit().putInt("autoColor", v).apply() }

    /** Oscurecido del fondo, 0 a 80 (%). */
    var dim: Int
        get() = sp.getInt("dim", 35)
        set(v) { sp.edit().putInt("dim", v).apply() }

    var use24h: Boolean
        get() = sp.getBoolean("use24h", android.text.format.DateFormat.is24HourFormat(appCtx))
        set(v) { sp.edit().putBoolean("use24h", v).apply() }

    var showWeather: Boolean
        get() = sp.getBoolean("showWeather", true)
        set(v) { sp.edit().putBoolean("showWeather", v).apply() }

    var nightDim: Boolean
        get() = sp.getBoolean("nightDim", true)
        set(v) { sp.edit().putBoolean("nightDim", v).apply() }

    var weatherText: String
        get() = sp.getString("weatherText", "") ?: ""
        set(v) { sp.edit().putString("weatherText", v).apply() }

    var weatherTime: Long
        get() = sp.getLong("weatherTime", 0L)
        set(v) { sp.edit().putLong("weatherTime", v).apply() }

    fun bgFile(): File = File(appCtx.filesDir, "fondo.jpg")

    fun hasBackground(): Boolean = bgFile().exists()

    /** Color del reloj que toca usar ahora mismo. */
    fun accent(): Int = when {
        colorMode == "manual" -> manualColor
        hasBackground() -> autoColor
        else -> DEFAULT_ACCENT
    }

    fun resetManualTransform() {
        bgScale = 1f
        bgOffX = 0f
        bgOffY = 0f
    }

    companion object {
        /** Azul lavanda parecido al del modo reposo de iPhone. */
        val DEFAULT_ACCENT: Int = 0xFFA9B8FF.toInt()
    }
}
