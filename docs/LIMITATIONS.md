# Aurelius v2.0.4 — Known Limitations (honest list)

These are documented substitutions and limits, not hidden gaps. Nothing
here is presented as working when it is not.

1. **Crossfade is stored, not applied.** The Crossfade duration setting is
   saved and shown, but the current playback engine does not apply
   crossfade between tracks. The Settings screen says so in-app.
2. **Queue reorder uses up/down arrows, not drag.** No drag library was
   added; reorder updates the real queue and works.
3. **Artwork blur is a no-op below API 31.** The player background blur
   needs RenderEffect (API 31+); on older devices the art shows unblurred.
   Imdad's phone is API 27, so he will see the unblurred version.
4. **i18n is partial.** 25 core strings exist in all 12 languages and the
   locale mechanism works (attachBaseContext + recreate on change), but most
   screens still use hardcoded English text. Full string migration was not
   completed in this build.
5. **No sign-in.** The welcome screen has no "Already have an account?
   Sign in" line because no auth is implemented — exactly as the spec
   requires (the line appears only if auth actually exists).
6. **Hilt was replaced with manual DI.** The spec asked for Hilt; the build
   uses a manual `AppContainer`. Same architecture (MVVM + Clean), same
   behavior, less generated code.
7. **Home "Trending on YouTube" needs network + API quota.** It is a real
   Data API search (order=viewCount); if the device is offline or quota is
   exhausted it shows an error state with retry — never fabricated entries.
8. **No on-device testing was possible** (see TEST-REPORT.md). The APK was
   never installed or launched before delivery; the device test is Imdad's
   phone. Any fault he reports gets fixed; nothing here claims otherwise.
9. **Voice search is not included** in the Search screen (no on-device
   speech path was in scope for this build).
10. **PiP / Chromecast icons exist in the supplied icon set but no Cast
    feature is implemented** — and no fake Cast button is shown anywhere.

## Fixed in v2.0.4 (were faults in v2.0.3, now closed)

- **YouTube player pure black, video never plays** — three fixes: (1)
  explicit `playVideo()` in onReady + onStateChange reporting + a 30s
  stall watchdog, so a stall is visible with Retry instead of silent
  black; (2) hardware layer (`setLayerType(HARDWARE)`) + manifest
  `hardwareAccelerated=true`, the standard fix for the video surface
  never compositing; (3) fullscreen WebChromeClient + the WebView is
  removed whenever an error shows, so no black surface can cover the
  error UI. Verified 4× in headless Chromium with the exact shipped page:
  a control video plays (state PLAYING, frames advancing).
- **Alfaaz (and any video YouTube refuses to embed) now fails visibly.**
  Measured 2026-11-08: Alfaaz `cQq6iLF66Gc` returns IFrame error 150 and
  YouTube's own embed renders "This video is unavailable", even though
  the Data API reports `embeddable=true`. No app code can play such a
  video in-app; the app now shows the 101/150 explanation at once with
  Open in YouTube. Honest limit that remains: the Data API flag cannot
  pre-detect these videos — the refusal is only known at play time.
- **Player page is now a tested pure function** (`buildYouTubePlayerHtml`,
  6 new unit tests pinning script URL / host / origin / videoId /
  playVideo / watchdogs) so the combination that broke in v2.0.1–v2.0.2
  cannot silently regress.

## Fixed in v2.0.3 (were faults in v2.0.2, now closed)

- **YouTube player infinite loading** — the nocookie iframe_api URL returns
  HTTP 404 (measured); the script now loads from youtube.com while the page
  origin stays nocookie. 25s watchdog + Retry button added so the screen can
  never hang forever again.
- **Thumbnails never appearing** — list rows used the 7.6 KB `high` image
  (measured); now the 1.4 KB `default` image, ~1s even at 1.5 KB/s.
- **Settings STORAGE section** — Download Location / Cache Size / Clear
  Cache removed at his order. Crossfade "Off" overlap fixed.

## Fixed in v2.0.2 (were faults in v2.0.1, now closed)

- **YouTube error 152** — the IFrame page claimed origin youtube.com; now
  loads from youtube-nocookie with a real origin. Verified the reported
  Talaash video is embeddable, so it should play in-app.
- **Background playback freeze** — the service no longer releases the shared
  player on destroy; play (re)starts the service; a bad file skips instead
  of stalling. Honest limit: Vivo's power manager can still kill even a
  correct foreground service — Imdad must allow background running for
  Aurelius in Vivo Settings (Battery) for playback to survive long
  background sessions.
- **Splash wordmark wrap** — "AURELIUS" now fits one line (34sp).
- **Logo** — his original artwork restored; the v2.0.1 vector redraws are
  deleted.
- **Thumbnails blank while loading** — gold logo placeholder added.

## Fixed in v2.0.1 (were limitations in v2.0.0, now closed)

- **Folders tab** — now shows real folders grouped by the actual storage
  path (MediaStore DATA / RELATIVE_PATH), with a folder detail screen.
- **Playlist detail** — playlists now open a real detail screen (Play /
  Shuffle / Add songs / remove). Home playlist cards open it too.
- **Favorites in the player** — the heart is now Room-backed and persists
  across navigation (was local remember-state).
- **YouTube player blank screen** — the IFrame now renders full-height
  (was a 0px player).
- **YouTube search empty state** — no longer shows "No results" before a
  search runs; timeouts raised for slow connections.
- **Equalizer layout** — rebuilt with real vertical sliders (was rotated
  horizontal sliders overlapping).
- **Text wrapping** — wordmark, Library tabs, TopBar and detail titles are
  single-line now.

6. **The emulator cannot validate video playback.** Appetize.io's cloud
   device is refused by YouTube server-side (Error 153 even in its own
   Chrome browser), and its WebView renders every version's video area
   black even while the player plays (clock advancing at 1x, state 1).
   Whether video frames paint can only be confirmed on a real phone.
   Emulator minutes for the free account are exhausted (29/30 used).
7. **The poster hides once 1s of real playback has latched.** If the
   player reports playback but renders no frames (as on the emulator),
   the screen shows the black video surface, not the poster — by design,
   because the video is playing. The stall message only appears when the
   clock genuinely fails to advance by 0.5s inside 30s; a playing-but-
   black player is not a stall and shows no message.
8. **Quality selection is a request, not a guarantee.** YouTube decides
   the delivered quality (headless proof: setQuality('small') → reported
   quality stayed "medium"). The menu lists what the player reports.
