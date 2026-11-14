package com.reposo.infinix

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max

/** Registra un receptor de avisos del sistema (bateria, hora, cargador). */
fun registerCompat(ctx: Context, receiver: BroadcastReceiver, filter: IntentFilter): Intent? =
    if (Build.VERSION.SDK_INT >= 33) {
        ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
        ctx.registerReceiver(receiver, filter)
    }

object Perms {
    fun hasNotificationAccess(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            ?: return false
        return flat.contains(ctx.packageName)
    }

    fun canOverlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun ignoresBatteryOptimizations(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun hasLocation(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun canPostNotifications(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
}

object ColorUtil {
    /** Saca un color que combine con la imagen: el tono dominante, en version clara para que se lea. */
    fun accentFrom(src: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(src, 48, 48, true)
        val weight = FloatArray(36)
        val hueSum = FloatArray(36)
        val hsv = FloatArray(3)
        for (y in 0 until 48) {
            for (x in 0 until 48) {
                Color.colorToHSV(small.getPixel(x, y), hsv)
                if (hsv[1] < 0.18f || hsv[2] < 0.22f) continue
                val w = hsv[1] * hsv[2]
                val i = (hsv[0] / 10f).toInt().coerceIn(0, 35)
                weight[i] += w
                hueSum[i] += hsv[0] * w
            }
        }
        if (small !== src) small.recycle()

        var best = -1
        var bestW = 0f
        for (i in 0 until 36) {
            if (weight[i] > bestW) {
                bestW = weight[i]
                best = i
            }
        }
        if (best < 0 || bestW < 2f) return Prefs.DEFAULT_ACCENT
        val hue = hueSum[best] / weight[best]
        return Color.HSVToColor(floatArrayOf(hue, 0.5f, 1f))
    }
}

object ImageUtil {
    /** Copia la imagen elegida a la app, reducida y con la rotacion correcta. */
    fun importTo(ctx: Context, uri: Uri, out: File) {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val maxSide = 2560
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp: Bitmap = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IOException("No se pudo leer la imagen")

        val orientation = try {
            cr.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees != 0f) {
            val m = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated !== bmp) bmp.recycle()
            bmp = rotated
        }

        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bmp.recycle()
    }
}
