package com.reposo.infinix

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

/** La pantalla de reposo: se abre al conectar el cargador y se cierra al desconectarlo. */
class StandbyActivity : Activity() {

    private lateinit var controller: StandbyController
    private var preview = false
    private var adjusting = false

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (!preview) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT < 27) {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        setContentView(R.layout.standby)
        hideSystemBars()
        getSystemService(NotificationManager::class.java).cancel(Launcher.FULLSCREEN_ID)

        adjusting = intent.getBooleanExtra(EXTRA_ADJUST, false)
        preview = adjusting || intent.getBooleanExtra(EXTRA_PREVIEW, false)

        controller = StandbyController(this, findViewById(R.id.root), window)
        controller.start()
        if (adjusting) setupAdjust()

        registerCompat(this, powerReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
    }

    override fun onResume() {
        super.onResume()
        visible = true
        resumedAt = SystemClock.elapsedRealtime()
        hideSystemBars()
        getSystemService(NotificationManager::class.java).cancel(Launcher.FULLSCREEN_ID)
    }

    override fun onPause() {
        visible = false
        pausedAt = SystemClock.elapsedRealtime()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Al salir con el boton de inicio, se cierra (no se queda escondida). */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!preview) finish()
    }

    override fun onDestroy() {
        controller.stop()
        try {
            unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            // ignorar
        }
        super.onDestroy()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    /** Modo para acomodar el fondo con los dedos. */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupAdjust() {
        if (!controller.hasBackground()) {
            finish()
            return
        }
        findViewById<View>(R.id.adjust_bar).visibility = View.VISIBLE
        controller.enterManualAdjust()

        val scaleDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    controller.adjust(detector.scaleFactor, 0f, 0f)
                    return true
                }
            })
        val panDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                controller.adjust(1f, -distanceX, -distanceY)
                return true
            }
        })

        findViewById<View>(R.id.root).setOnTouchListener { _, ev ->
            scaleDetector.onTouchEvent(ev)
            if (!scaleDetector.isInProgress) panDetector.onTouchEvent(ev)
            true
        }

        findViewById<View>(R.id.btn_reset).setOnClickListener { controller.resetAdjust() }
        findViewById<View>(R.id.btn_cancel).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_save).setOnClickListener {
            controller.saveManualAdjust()
            finish()
        }
    }

    companion object {
        const val EXTRA_PREVIEW = "preview"
        const val EXTRA_ADJUST = "adjust"

        /** Momento en que la pantalla de reposo dejo de verse por ultima vez. */
        @Volatile
        private var pausedAt = 0L

        @Volatile
        private var resumedAt = 0L

        /** true si la pantalla de reposo se mostro despues de ese momento. */
        fun shownSince(time: Long): Boolean = visible || resumedAt >= time

        /**
         * true si el modo reposo se estaba viendo justo cuando se apago la pantalla:
         * significa que tu la apagaste con el boton y no hay que volver a encenderla.
         */
        fun userTurnedItOff(): Boolean =
            visible || SystemClock.elapsedRealtime() - pausedAt < 2000

        /** true mientras la pantalla de reposo se ve al frente. */
        @Volatile
        var visible = false
    }
}
