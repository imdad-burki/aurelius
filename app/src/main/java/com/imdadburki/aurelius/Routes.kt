package com.imdadburki.aurelius

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val LYRICS = "lyrics"
    const val LANGUAGE = "language"
    const val ALBUM = "album/{albumName}"
    const val ARTIST = "artist/{artistName}"
    const val FOLDER = "folder/{folderName}"
    const val PLAYLIST = "playlist/{playlistId}"
    const val YOUTUBE_PLAYER = "youtubePlayer"
    fun album(name: String) = "album/${Uri.encode(name)}"
    fun artist(name: String) = "artist/${Uri.encode(name)}"
    fun folder(name: String) = "folder/${Uri.encode(name)}"
    fun playlist(id: Long) = "playlist/$id"
}
