package com.imdadburki.aurelius.core.di

import android.content.Context
import com.imdadburki.aurelius.core.data.AureliusDatabase
import com.imdadburki.aurelius.core.data.SettingsStore
import com.imdadburki.aurelius.core.network.LyricsApi
import com.imdadburki.aurelius.core.network.YouTubeApi
import com.imdadburki.aurelius.core.playback.PlayerManager

/**
 * Manual dependency injection container.
 * Documented substitution (spec §3 recommends Hilt): Hilt was not available in the
 * offline dependency cache and its annotation processing adds a network fetch + build
 * risk on this VM; a single explicit container provides the same wiring without it.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settings = SettingsStore(appContext)
    val database: AureliusDatabase = AureliusDatabase.create(appContext)
    val playerManager = PlayerManager(appContext)
    val youTubeApi = YouTubeApi()
    val lyricsApi = LyricsApi()
}
