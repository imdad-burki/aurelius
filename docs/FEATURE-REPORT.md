# Aurelius v2.0.3 — Feature Completion Report

All 18 screens from the design set are implemented, plus 3 detail screens
added in v2.0.1 (Artist, Folder, Playlist). Design references live in
`~/workspace/aurelius-v2/design/`. The Aurum Dark tokens are applied
throughout: Gold #D4AF37, Gold Light #E8C468, Gold Dark #A88A2E, Background
#0A0A0A, Surface #1A1A1A, Elevated #242424, Border #2E2E2E, Text #F5F5F5 /
#B0B0B0 / #6E6E6E. Fonts bundled locally: Poppins SemiBold (headings),
Inter (body), JetBrains Mono (timers/numbers), Cormorant Garamond SemiBold
(wordmark). Logo and illustrations are Imdad's own supplied artwork
(restored in v2.0.2 from the v2.0.0 sources — the v2.0.1 vector redraws
were rejected by him: "Logo wahi mera wala sahi tha, usko change mat karo").

## v2.0.3 changes (Imdad's on-device feedback round 3)

- **YouTube player actually fixed.** v2.0.2's nocookie change broke playback
  completely: `youtube-nocookie.com/iframe_api` returns HTTP 404, so the
  player never initialized and the screen spun on "Loading video…" forever.
  Measured 2026-11-08. Now the API script loads from youtube.com (HTTP 200,
  proven on his phone in v2.0.1) while the page origin stays nocookie (no
  error 152). The reported Alfaaz video is embeddable=true. A 25s watchdog
  reports a timeout error instead of spinning forever, and the error state
  has a Retry button that rebuilds the WebView.
- **Original thumbnails show.** Search results used `high` (480x360, ~7.6 KB)
  first — at his 1.5 KB/s that never arrived, so every row showed the logo
  placeholder. List rows now use `default` (120x90, ~1.4 KB, ~1s at 1.5 KB/s);
  wide Trending cards use `medium` (320x180). Placeholder stays only while
  the real image loads.
- **Settings STORAGE section removed** at his order: Download Location,
  Cache Size, Clear Cache are gone (the app downloads nothing). Crossfade
  row fixed: shorter subtitle + padded value so "Off" can never run into
  the description text.

## v2.0.2 changes (Imdad's on-device feedback round 2)

- **YouTube error 152 fixed at the root.** v2.0.1 loaded the IFrame page with
  base URL https://www.youtube.com, so YouTube identified the embedder as
  YouTube itself and refused every video (error 152). The page now loads
  from https://www.youtube-nocookie.com with matching host/origin. The
  Talaash video he reported is confirmed embeddable (Data API
  status.embeddable=true), so it should now play in-app.
- **Non-embeddable videos handled honestly.** Search results now carry the
  Data API `status.embeddable` flag; a video whose uploader disabled
  embedding never opens a dead player — the screen goes straight to a plain
  explanation plus "Open in YouTube". Player errors are plain-language
  (101/150 = uploader turned off embedding; 152 = YouTube refused the
  embed; 100 = not found/private).
- **Background playback freeze fixed.** The service no longer releases the
  shared ExoPlayer in onDestroy (that was the freeze: service stopped on
  song-end/idle or by the phone's power manager, player died, buttons went
  dead until app restart). Play now (re)starts the service so the
  notification returns; one bad file skips to the next track instead of
  stalling the queue.
- **Original logo restored.** His supplied logo + 4 illustrations are back
  (v2.0.1 vectors deleted); nothing of his artwork is redrawn.
- **Splash wordmark fits one line.** "AURELIUS" (34sp, 6sp tracking,
  maxLines=1) instead of the spaced 40sp string that wrapped to "A U R E L I U / S".
- **Thumbnails show a placeholder.** Search results, Trending cards and
  recently-played art show the gold logo while the image loads (his
  connection was 0.10 KB/s, so thumbnails took very long to appear).

## v2.0.1 changes (Imdad's on-device feedback round)

- API key is backend-only: no Settings section, no dialog, no override,
  no "connected" status — BuildConfig key only.
- Real folders (MediaStore DATA / RELATIVE_PATH) with a folder detail
  screen; artists open a detail screen; playlists open a detail screen
  with Add songs / remove.
- Player favorite is Room-backed and persists.
- YouTube player renders full-height with ready/error reporting, loading
  spinner and an open-in-YouTube fallback; search no longer shows "No
  results" before searching and uses 30s timeouts.
- Equalizer rebuilt with real Canvas vertical sliders; Bass Boost /
  Virtualizer / enable switch start from persisted settings.
- Text single-line throughout (wordmark, Library tabs, TopBar, titles);
  greeting has no name; `<unknown>` normalized to "Unknown Artist".
- Settings: "Rate on Play Store" removed; About is a short centered block.

## Screens (design image → implementation)

| # | Screen | Design ref | Status |
|---|---|---|---|
| 01 | Splash — gold A + waveform, "AURELIUS — Music. Mastered.", loader | 22-splash.jpg | Done |
| 02 | Onboarding Welcome — "Your Music. Your Rules.", column+vinyl art, Skip/Continue | 21-onboarding-welcome.jpg | Done — no sign-in line (no auth implemented, per spec) |
| 03 | Permissions — Read Music Files / Notifications / Background cards, Allow All, Not Now | 19-onboarding-permissions.jpg | Done — requested only when needed, correct per-API permission APIs |
| 04 | Feature Tour — "Everything You Need." (gestures / EQ / playback / video panels) | 18-onboarding-features.jpg | Done |
| 05 | Home — greeting "Good evening" (no name), Continue Listening (real history), Trending on YouTube (real Data API, order=viewCount), Recently Added, Your Playlists, mini player, bottom nav | 20-home.jpg | Done |
| 06 | Search — field, YouTube/Local tabs, history + clear, loading/empty/error states | 17-search-empty.jpg | Done (voice search not included — no on-device speech path in scope) |
| 07 | Search Results — real Data API v3, thumbnails/title/channel/duration, skeleton, retry, pagination | 16-search-results.jpg | Done — no silent external redirect; empty state only after a search runs |
| 08 | Library — Songs/Albums/Artists/Folders/Playlists tabs, real MediaStore discovery, sort A→Z, grid/list toggle, filtering, playlist create/edit, empty + permission-denied states | 15-library.jpg | Done — Folders are real (grouped by storage path); Artists and Playlists open detail screens |
| 09 | Album Detail — art, artist, metadata, PLAY gold + SHUFFLE outline, numbered tracklist, current-track indicator | 14-album-detail.jpg | Done |
| 09a | Artist Detail — art, song count, PLAY + SHUFFLE, numbered tracklist | — (new in v2.0.1) | Done |
| 09b | Folder Detail — art, song count, PLAY + SHUFFLE, numbered tracklist | — (new in v2.0.1) | Done |
| 09c | Playlist Detail — PLAY + SHUFFLE, Add songs picker (library, excludes songs already in the playlist), remove per song | — (new in v2.0.1) | Done |
| 10 | Main Player — blur-bg from art, big gold circular play/pause, shuffle/prev/next/repeat with labels, seek bar + JetBrains Mono times, favorite, queue, EQ, lyrics, sleep, speed; side rails volume/brightness; "Swipe to Seek" | 12-player.jpg | Done — gestures: tap show/hide controls, double-tap play/pause, horizontal swipe seek, left-vertical volume, right-vertical window brightness, feedback pills (Volume 65% / Seek +10s / Brightness 40%) |
| 11 | Equalizer — bottom sheet, enable toggle, presets Flat/Bass Boost/Vocal/Rock/Jazz/Classical/Custom, 10 bands 31/62/125/250/500/1k/2k/4k/8k/16k ±12dB, Bass Boost %, Virtualizer %, Reverb dropdown | 13-equalizer.jpg | Done — real platform Equalizer/BassBoost/Virtualizer on the player's audio session; settings persisted; capability checked; v2.0.1 rebuilt with real Canvas vertical sliders (one fixed column per band, value above, frequency below) |
| 12 | Lyrics — synced LRC, current line gold + bold, auto-scroll, manual-scroll pause, local .lrc + LRCLIB, missing/error states with retry | 11-lyrics.jpg | Done |
| 13 | Settings — Playback (Gapless, Crossfade, Audio Focus, Pitch Correction), Storage (Download Location, Cache Size, Clear Cache), Appearance (Aurum Dark, Language, UI Sounds, Haptics), About (short centered block: version, developer, email, Feedback, Bug report, Licenses, Privacy) | 10-settings.jpg | Done — every setting works or shows unsupported status (see LIMITATIONS.md for Crossfade); v2.0.1 removed the YouTube section (key is backend-only) and the Play Store row |
| 14 | Language — 12 languages with native-script names, radio selection, persistent, RTL | 09-language.jpg | Done — 25 core strings × 12 languages in resources; locale applies via attachBaseContext (see LIMITATIONS.md — most screens still use hardcoded English text) |
| 15 | Mini Player — gold-bordered pill above bottom nav, art thumb, title/artist, gold progress line, gold circular pause, next; swipe-up expands | 08-mini-player.jpg | Done — synced with the playback service |
| 16 | Sleep Timer — big JetBrains Mono countdown, "Ends at …", red-outline Cancel, chips 15/30/45/60 min + End of Track + Custom, selected chip gold-filled | 07-sleep-timer.jpg | Done — lifecycle-safe, incl. fade-out and end-of-track |
| 17 | Empty Library — gold lyre, "Your library is empty.", Scan Device + Search YouTube buttons | 06-empty-library.jpg | Done — both buttons functional |
| 18 | Queue — "Up Next" sheet, NOW PLAYING card with gold equalizer-bars animation, numbered upcoming rows, reorder + remove, Clear | 05-queue-up-next.jpg | Done — reorder via up/down arrows (see LIMITATIONS.md) |

## Systems

- **Playback:** single centralized Media3 ExoPlayer in a MediaSessionService;
  foreground notification + lock-screen controls; audio focus;
  headphone-unplug/Bluetooth handling; background playback; seek/queue/
  repeat/shuffle/speed; error recovery. Formats: MP3/AAC/FLAC/WAV/OGG as the
  device supports.
- **Data:** Room (tracks, playlists, favorites, play history), DataStore
  (all settings), MediaStore scanner (incl. DATE_ADDED).
- **YouTube:** Data API v3 search + batched durations + ISO-8601 parser;
  in-app player is the official IFrame API in a WebView; fallback opens the
  YouTube app with an explanation. No extraction, no download.
- **DI:** manual DI (`core/di/AppContainer.kt`) — Hilt was substituted (see
  LIMITATIONS.md).
- **Motion:** artwork 5° idle rotation, gold waveform in player, home→player
  shared-element-style transition, gesture feedback overlay pills.
