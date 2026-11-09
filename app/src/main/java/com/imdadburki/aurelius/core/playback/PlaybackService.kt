package com.imdadburki.aurelius.core.playback

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.imdadburki.aurelius.AureliusApp

/**
 * Foreground playback service. Media3 builds the notification and lock-screen
 * controls from the MediaSession automatically; no custom notification code needed.
 *
 * v2.0.2 fix — the service must NEVER release the player. The ExoPlayer is owned
 * by the app-scoped PlayerManager (one instance for the whole app). v2.0.0/2.0.1
 * released it here in onDestroy: whenever the system stopped the service (song
 * ended with repeat off, or Vivo's power manager killed it in background),
 * the player died while the app process lived on — every button went dead and
 * nothing played again until an app restart. The service now releases only
 * its own MediaSession; the player survives and can restart the service.
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as AureliusApp
        val player = app.container.playerManager.player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        // Release ONLY the session. The player belongs to PlayerManager and must
        // survive the service stopping, or playback can never recover without
        // an app restart.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
