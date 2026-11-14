package com.reposo.infinix

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Vuelve a encender el servicio al reiniciar el telefono o al actualizar la app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ok = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (ok && Prefs(context).enabled) ChargeService.start(context)
    }
}
