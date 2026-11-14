package com.reposo.infinix

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings

/** Servicio en segundo plano que espera a que conectes el cargador. */
class ChargeService : Service() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val prefs = Prefs(c)
            when (i.action) {
                // Conectaste el cargador: se abre si el telefono esta bloqueado (o siempre, si asi lo elegiste)
                Intent.ACTION_POWER_CONNECTED ->
                    if (prefs.enabled && (!prefs.onlyWhenLocked || Launcher.isLockedOrOff(c))) {
                        Launcher.launch(c)
                    }
                // Bloqueaste el telefono mientras carga: igual que en iPhone
                Intent.ACTION_SCREEN_OFF ->
                    if (prefs.enabled && Launcher.isPluggedIn(c) && !StandbyActivity.running) {
                        Launcher.launch(c)
                    }
                Intent.ACTION_POWER_DISCONNECTED ->
                    c.getSystemService(NotificationManager::class.java).cancel(Launcher.FULLSCREEN_ID)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Launcher.createChannels(this)
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, Launcher.CH_SERVICE)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Reposo Infinix activo")
            .setContentText("Se mostrará al conectar el cargador")
            .setOngoing(true)
            .setContentIntent(open)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(SERVICE_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(SERVICE_ID, n)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerCompat(this, receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // ignorar
        }
        super.onDestroy()
    }

    companion object {
        private const val SERVICE_ID = 1

        fun start(ctx: Context) {
            try {
                ctx.startForegroundService(Intent(ctx, ChargeService::class.java))
            } catch (e: Exception) {
                // el sistema no dejo iniciarlo en este momento
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ChargeService::class.java))
        }
    }
}

/** Abre la pantalla de reposo desde segundo plano. */
object Launcher {
    const val CH_SERVICE = "servicio"
    const val CH_SCREEN = "pantalla"
    const val FULLSCREEN_ID = 2

    fun isLockedOrOff(ctx: Context): Boolean {
        val km = ctx.getSystemService(KeyguardManager::class.java)
        val pm = ctx.getSystemService(PowerManager::class.java)
        return km.isKeyguardLocked || !pm.isInteractive
    }

    fun isPluggedIn(ctx: Context): Boolean {
        val battery = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        return battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
    }

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Servicio en segundo plano", NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SCREEN, "Abrir pantalla de reposo", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
        )
    }

    fun launch(ctx: Context) {
        val i = Intent(ctx, StandbyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Camino 1: con el permiso "mostrar sobre otras apps" se abre directo.
        if (Settings.canDrawOverlays(ctx)) {
            try {
                ctx.startActivity(i)
                return
            } catch (e: Exception) {
                // seguir con el camino 2
            }
        }

        // Camino 2: notificacion de pantalla completa (se abre sola con la pantalla bloqueada).
        createChannels(ctx)
        val pi = PendingIntent.getActivity(
            ctx, 3, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(ctx, CH_SCREEN)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Modo reposo")
            .setContentText("Toca para abrir la pantalla de reposo")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(FULLSCREEN_ID, n)
    }
}
