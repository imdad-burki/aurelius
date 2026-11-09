# Aurelius — Aurum Dark Music Player for Android

Aurelius is an Android music player with an **Aurum Dark** design (gold `#D4AF37` on near-black `#0A0A0A`): local music library, YouTube search and playback, playlists, equalizer, lyrics, sleep timer, and background playback with a media notification.

Built with Kotlin + Jetpack Compose (Material 3), Media3 ExoPlayer, Room, DataStore, Coil 3, and Retrofit/OkHttp. Minimum SDK 26, target SDK 36.

## Features

- **Local library** — real folders, artists, albums, and playlists scanned from the device
- **YouTube search** — official YouTube Data API v3 (search + video details); thumbnails are the originals from YouTube
- **YouTube player** — official IFrame player in a WebView, with a native MX Player-style control bar: play/pause, ±10s seek, seek bar, speed (0.5x–2x, hold-for-2x), quality menu, fullscreen, and screen lock; tap toggles the bar, double-tap seeks, vertical drag adjusts brightness/volume
- **Playback** — Media3 ExoPlayer + MediaSessionService for background playback and notification controls
- **Equalizer** — 5-band EQ with presets
- **Lyrics** — LRC parsing and synced display
- **Sleep timer, queue, 12-language UI** (partial i18n)

## What it is NOT

- No downloading, no stream extraction, no offline YouTube playback. Search uses the official Data API v3 and playback uses the officially supported IFrame embed. Videos whose owners disabled embedding show an explanation with an **Open in YouTube** button instead of a black screen.

## Setup

1. Clone the repo and open it in Android Studio (or use the command line).
2. Copy `gradle.properties.example` to `gradle.properties` and fill in:
   - `YOUTUBE_API_KEY` — a YouTube Data API v3 key from Google Cloud Console (restricted to that API). The app reads it at build time; it never appears in any UI.
   - `AURELIUS_STORE_PASSWORD` / `AURELIUS_KEY_PASSWORD` — your release keystore passwords (only needed for a signed release build).
3. Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to your Android SDK.
4. Build and verify:

```bash
./build.sh :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

The APK lands in `app/build/outputs/apk/release/`. Unit tests use JUnit4.

## Headless player test rig

`tools/player-test/` runs the exact shipped player HTML end-to-end in headless Chrome via CDP and reports real player state (READY / STATE / currentTime / error). See its README. It needs a proxy only if your network requires one; on a normal machine it runs directly.

## Known limitations

- The Data API `embeddable` flag does **not** reflect owner/licensor embedding blocks (e.g. licensed music videos return IFrame error 150 at play time). The app surfaces the 101/150 message with Open in YouTube.
- Quality selection is a request — YouTube decides the delivered quality.
- Crossfade is stored, not applied; queue reorder uses arrows, not drag; artwork blur is a no-op below API 31; i18n is partial.
- Vivo/power-manager devices may kill background playback unless the app is allowed to run in the background (Settings → Battery).

## License

MIT — see [LICENSE](LICENSE).
