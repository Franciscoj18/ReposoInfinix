package com.reposo.infinix

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Temperatura actual con Open-Meteo (gratis, sin cuenta). Se actualiza cada 30 minutos. */
class WeatherHelper(
    private val ctx: Context,
    private val prefs: Prefs,
    private val onText: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var stopped = false
    private var fetching = false

    fun start() {
        if (prefs.weatherText.isNotEmpty()) onText(prefs.weatherText)
        maybeRefresh()
    }

    fun stop() {
        stopped = true
    }

    @SuppressLint("MissingPermission")
    fun maybeRefresh() {
        if (stopped || fetching) return
        val fresh = System.currentTimeMillis() - prefs.weatherTime < 30 * 60 * 1000L
        if (fresh && prefs.weatherText.isNotEmpty()) return
        if (!Perms.hasLocation(ctx)) return

        val loc = lastLocation()
        if (loc != null) {
            fetch(loc.latitude, loc.longitude)
        } else if (Build.VERSION.SDK_INT >= 30) {
            try {
                val lm = ctx.getSystemService(LocationManager::class.java)
                lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, ctx.mainExecutor) { l ->
                    if (l != null) fetch(l.latitude, l.longitude)
                }
            } catch (e: Exception) {
                // sin ubicacion disponible
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastLocation(): Location? {
        val lm = ctx.getSystemService(LocationManager::class.java)
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER
        )
        return providers.mapNotNull { p ->
            try {
                lm.getLastKnownLocation(p)
            } catch (e: Exception) {
                null
            }
        }.maxByOrNull { it.time }
    }

    private fun fetch(lat: Double, lon: Double) {
        fetching = true
        Thread {
            try {
                val url = URL(
                    String.format(
                        Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code",
                        lat, lon
                    )
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val cur = JSONObject(body).getJSONObject("current")
                val temp = Math.round(cur.getDouble("temperature_2m"))
                val code = cur.optInt("weather_code", -1)
                val text = "${icon(code)} ${temp}°C"
                prefs.weatherText = text
                prefs.weatherTime = System.currentTimeMillis()
                main.post { if (!stopped) onText(text) }
            } catch (e: Exception) {
                // sin internet: se queda el ultimo dato
            } finally {
                main.post { fetching = false }
            }
        }.start()
    }

    private fun icon(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67 -> "🌧️"
        in 71..77 -> "❄️"
        in 80..82 -> "🌦️"
        85, 86 -> "🌨️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
}
