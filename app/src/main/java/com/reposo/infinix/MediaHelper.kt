package com.reposo.infinix

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper

/**
 * Lee la musica que suena en cualquier app (YouTube, Spotify, YT Music...)
 * y permite pausar, adelantar y regresar. Requiere "acceso a notificaciones".
 */
class MediaHelper(
    private val ctx: Context,
    private val onChange: (MediaController?) -> Unit
) {
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(ctx, MediaListener::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var current: MediaController? = null
    private var listening = false

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            onChange(current)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            refresh()
        }

        override fun onSessionDestroyed() {
            refresh()
        }
    }

    fun start() {
        if (!Perms.hasNotificationAccess(ctx)) {
            onChange(null)
            return
        }
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            listening = true
            pick(msm.getActiveSessions(component))
        } catch (e: SecurityException) {
            onChange(null)
        }
    }

    fun stop() {
        if (listening) {
            try {
                msm.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (e: Exception) {
                // ignorar
            }
        }
        listening = false
        current?.unregisterCallback(callback)
        current = null
    }

    fun playPause() {
        val c = current ?: return
        if (isPlaying(c)) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() {
        current?.transportControls?.skipToNext()
    }

    fun previous() {
        current?.transportControls?.skipToPrevious()
    }

    private fun refresh() {
        try {
            pick(msm.getActiveSessions(component))
        } catch (e: SecurityException) {
            onChange(null)
        }
    }

    private fun pick(list: List<MediaController>?) {
        val sessions = list ?: emptyList()
        val currentToken = current?.sessionToken
        val best = sessions.firstOrNull { isPlaying(it) }
            ?: sessions.firstOrNull { currentToken != null && it.sessionToken == currentToken }
            ?: sessions.firstOrNull()

        if (best?.sessionToken != currentToken) {
            current?.unregisterCallback(callback)
            current = best
            best?.registerCallback(callback, handler)
        }
        onChange(current)
    }

    companion object {
        fun isPlaying(c: MediaController): Boolean {
            val s = c.playbackState?.state
            return s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
        }
    }
}
