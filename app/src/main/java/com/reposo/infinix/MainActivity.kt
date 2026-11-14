package com.reposo.infinix

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** Pantalla de ajustes. */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs

    private lateinit var swEnabled: Switch
    private lateinit var btnNotif: Button
    private lateinit var btnOverlay: Button
    private lateinit var btnMedia: Button
    private lateinit var btnBattery: Button
    private lateinit var btnLocation: Button
    private lateinit var bgPreview: ImageView
    private lateinit var bgPreviewDim: View
    private lateinit var colorPreview: TextView
    private lateinit var rgFit: RadioGroup
    private lateinit var rgColor: RadioGroup
    private lateinit var swatches: LinearLayout

    private val colors = intArrayOf(
        Prefs.DEFAULT_ACCENT,
        0xFFFFFFFF.toInt(),
        0xFFFF9F0A.toInt(),
        0xFFFF453A.toInt(),
        0xFFFF6FB5.toInt(),
        0xFFBF5AF2.toInt(),
        0xFF64D2FF.toInt(),
        0xFF30D158.toInt(),
        0xFFFFD60A.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        swEnabled = findViewById(R.id.sw_enabled)
        btnNotif = findViewById(R.id.btn_perm_notif)
        btnOverlay = findViewById(R.id.btn_perm_overlay)
        btnMedia = findViewById(R.id.btn_perm_media)
        btnBattery = findViewById(R.id.btn_perm_battery)
        btnLocation = findViewById(R.id.btn_perm_location)
        bgPreview = findViewById(R.id.bg_preview)
        bgPreviewDim = findViewById(R.id.bg_preview_dim)
        colorPreview = findViewById(R.id.color_preview)
        rgFit = findViewById(R.id.rg_fit)
        rgColor = findViewById(R.id.rg_color)
        swatches = findViewById(R.id.swatches)

        setupActivation()
        setupPermissions()
        setupBackground()
        setupColor()
        setupOthers()

        if (prefs.enabled) ChargeService.start(this)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        refreshBackground()
        refreshColor()
        checkFitRadio()
    }

    // ---------- Activacion ----------

    private fun setupActivation() {
        swEnabled.isChecked = prefs.enabled
        swEnabled.setOnCheckedChangeListener { _, on ->
            prefs.enabled = on
            if (on) {
                ChargeService.start(this)
                if (!Perms.canOverlay(this)) {
                    toast("Da el permiso «Mostrar sobre otras apps» para que se abra sola")
                }
            } else {
                ChargeService.stop(this)
            }
        }

        val swLocked = findViewById<Switch>(R.id.sw_locked)
        swLocked.isChecked = prefs.onlyWhenLocked
        swLocked.setOnCheckedChangeListener { _, on -> prefs.onlyWhenLocked = on }

        findViewById<Button>(R.id.btn_preview).setOnClickListener {
            startActivity(
                Intent(this, StandbyActivity::class.java).putExtra(StandbyActivity.EXTRA_PREVIEW, true)
            )
        }

        findViewById<Button>(R.id.btn_dream).setOnClickListener {
            if (!open(Intent(Settings.ACTION_DREAM_SETTINGS))) {
                toast("Tu sistema no muestra este ajuste. Usa «Abrir al conectar el cargador».")
            }
        }
    }

    // ---------- Permisos ----------

    private fun setupPermissions() {
        btnNotif.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33 && !Perms.canPostNotifications(this)) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            } else {
                openAppNotificationSettings()
            }
        }
        btnOverlay.setOnClickListener {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        btnMedia.setOnClickListener {
            if (!open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))) openAppInfo()
        }
        btnBattery.setOnClickListener {
            if (Perms.ignoresBatteryOptimizations(this)) {
                open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } else {
                @Suppress("BatteryLife")
                val i = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                if (!open(i)) open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        btnLocation.setOnClickListener {
            if (!Perms.hasLocation(this)) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQ_LOCATION)
            } else {
                openAppInfo()
            }
        }
        findViewById<Button>(R.id.btn_perm_autostart).setOnClickListener { openAutostart() }
        findViewById<Button>(R.id.btn_app_info).setOnClickListener { openAppInfo() }
    }

    private fun refreshPermissions() {
        btnNotif.text = status(Perms.canPostNotifications(this), "Notificaciones")
        btnOverlay.text = status(Perms.canOverlay(this), "Mostrar sobre otras apps (abrir sola)")
        btnMedia.text = status(Perms.hasNotificationAccess(this), "Acceso a notificaciones (música)")
        btnBattery.text = status(Perms.ignoresBatteryOptimizations(this), "Sin restricción de batería")
        btnLocation.text = status(Perms.hasLocation(this), "Ubicación aproximada (clima)")
    }

    private fun status(ok: Boolean, label: String): String =
        if (ok) "✅  $label" else "⚠️  $label — tocar para activar"

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshPermissions()
        if (requestCode == REQ_NOTIF && !Perms.canPostNotifications(this)) openAppNotificationSettings()
        if (requestCode == REQ_LOCATION && Perms.hasLocation(this)) prefs.weatherTime = 0L
    }

    /** Intenta abrir el administrador de inicio automatico de Infinix / Tecno (XOS / HiOS). */
    private fun openAutostart() {
        val candidates = listOf(
            ComponentName("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity"),
            ComponentName("com.transsion.phonemanager", "com.itel.autobootmanager.activity.AutoBootMgrActivity")
        )
        for (c in candidates) {
            if (open(Intent().setComponent(c))) return
        }
        toast("Busca «Inicio automático» en Ajustes o en el Administrador del teléfono")
        openAppInfo()
    }

    private fun openAppInfo() {
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun openAppNotificationSettings() {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        if (!open(i)) openAppInfo()
    }

    // ---------- Fondo ----------

    private fun setupBackground() {
        findViewById<Button>(R.id.btn_pick).setOnClickListener { pickImage() }
        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            prefs.bgFile().delete()
            prefs.resetManualTransform()
            if (prefs.fitMode == "manual") prefs.fitMode = "fill"
            refreshBackground()
            refreshColor()
            checkFitRadio()
        }

        rgFit.setOnCheckedChangeListener { _, id ->
            prefs.fitMode = when (id) {
                R.id.rb_fit -> "fit"
                R.id.rb_manual -> "manual"
                else -> "fill"
            }
        }

        findViewById<Button>(R.id.btn_adjust).setOnClickListener {
            if (!prefs.hasBackground()) {
                toast("Primero elige una imagen")
            } else {
                startActivity(
                    Intent(this, StandbyActivity::class.java).putExtra(StandbyActivity.EXTRA_ADJUST, true)
                )
            }
        }

        val sb = findViewById<SeekBar>(R.id.sb_dim)
        sb.progress = prefs.dim
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.dim = progress
                bgPreviewDim.alpha = progress / 100f
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    private fun checkFitRadio() {
        val id = when (prefs.fitMode) {
            "fit" -> R.id.rb_fit
            "manual" -> R.id.rb_manual
            else -> R.id.rb_fill
        }
        if (rgFit.checkedRadioButtonId != id) rgFit.check(id)
    }

    private fun pickImage() {
        val i = if (Build.VERSION.SDK_INT >= 33) {
            Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*")
        } else {
            Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(i, REQ_PICK)
        } catch (e: Exception) {
            toast("No se encontró una galería")
        }
    }

    @Deprecated("Activity de Android sin AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        toast("Guardando fondo…")
        Thread {
            try {
                val file = prefs.bgFile()
                ImageUtil.importTo(this, uri, file)
                val bmp = BitmapFactory.decodeFile(file.path)
                if (bmp != null) {
                    prefs.autoColor = ColorUtil.accentFrom(bmp)
                    bmp.recycle()
                }
                prefs.resetManualTransform()
                if (prefs.fitMode == "manual") prefs.fitMode = "fill"
                runOnUiThread {
                    refreshBackground()
                    refreshColor()
                    checkFitRadio()
                    toast("Fondo listo")
                }
            } catch (e: Exception) {
                runOnUiThread { toast("No se pudo abrir esa imagen") }
            }
        }.start()
    }

    private fun refreshBackground() {
        val f = prefs.bgFile()
        if (f.exists()) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            bgPreview.setImageBitmap(BitmapFactory.decodeFile(f.path, opts))
        } else {
            bgPreview.setImageDrawable(null)
        }
        bgPreviewDim.alpha = prefs.dim / 100f
    }

    // ---------- Color ----------

    private fun setupColor() {
        rgColor.check(if (prefs.colorMode == "manual") R.id.rb_color_manual else R.id.rb_color_auto)
        rgColor.setOnCheckedChangeListener { _, id ->
            prefs.colorMode = if (id == R.id.rb_color_manual) "manual" else "auto"
            refreshColor()
        }
        buildSwatches()
    }

    private fun buildSwatches() {
        swatches.removeAllViews()
        val d = resources.displayMetrics.density
        val size = (40 * d).toInt()
        val margin = (6 * d).toInt()
        for (c in colors) {
            val v = View(this)
            val selected = prefs.colorMode == "manual" && prefs.manualColor == c
            v.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(c)
                if (selected) setStroke((3 * d).toInt(), 0xFFFFFFFF.toInt())
                else setStroke((1 * d).toInt(), 0x55FFFFFF)
            }
            v.contentDescription = "Color"
            v.setOnClickListener {
                prefs.manualColor = c
                prefs.colorMode = "manual"
                rgColor.check(R.id.rb_color_manual)
                refreshColor()
            }
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(margin, margin, margin, margin)
            swatches.addView(v, lp)
        }
    }

    private fun refreshColor() {
        colorPreview.setTextColor(prefs.accent())
        buildSwatches()
    }

    // ---------- Otros ----------

    private fun setupOthers() {
        val sw24 = findViewById<Switch>(R.id.sw_24h)
        sw24.isChecked = prefs.use24h
        sw24.setOnCheckedChangeListener { _, on -> prefs.use24h = on }

        val swWeather = findViewById<Switch>(R.id.sw_weather)
        swWeather.isChecked = prefs.showWeather
        swWeather.setOnCheckedChangeListener { _, on ->
            prefs.showWeather = on
            if (on && !Perms.hasLocation(this)) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQ_LOCATION)
            }
        }

        val swNight = findViewById<Switch>(R.id.sw_night)
        swNight.isChecked = prefs.nightDim
        swNight.setOnCheckedChangeListener { _, on -> prefs.nightDim = on }
    }

    // ---------- Utilidades ----------

    private fun open(i: Intent): Boolean = try {
        startActivity(i)
        true
    } catch (e: Exception) {
        false
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val REQ_PICK = 10
        private const val REQ_NOTIF = 11
        private const val REQ_LOCATION = 12
    }
}
