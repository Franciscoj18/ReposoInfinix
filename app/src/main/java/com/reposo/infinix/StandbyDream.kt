package com.reposo.infinix

import android.service.dreams.DreamService
import android.view.View

/** La misma pantalla de reposo como protector de pantalla del sistema (si XOS lo permite). */
class StandbyDream : DreamService() {

    private var controller: StandbyController? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true
        isScreenBright = true
        setContentView(R.layout.standby)
        val root = findViewById<View>(R.id.root)
        controller = StandbyController(this, root, window).also { it.start() }
    }

    override fun onDetachedFromWindow() {
        controller?.stop()
        controller = null
        super.onDetachedFromWindow()
    }
}
