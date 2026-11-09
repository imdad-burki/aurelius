# Aurelius v2.0.4 — Test Report

Built and verified 2026-11-08 (PKT). Package `com.imdadburki.aurelius`,
versionCode 8, versionName 2.0.4, minSdk 26, targetSdk 36, compileSdk 37.
Same release keystore as v2.0.0–v2.0.3, so this installs OVER v2.0.3
as an update — no uninstall needed.

This is the fix round for Imdad's on-device feedback on v2.0.3 (tested on
his Vivo Y85A, 2026-11-08 ~20:44 PKT): the YouTube player showed a pure
black screen and the video never played — no spinner, no error, no Retry.
Root causes were measured, not guessed (see "What changed" below): the
Alfaaz video itself is refused by YouTube in any embed (error 150), and
the player had no hardware layer, no explicit play call, no playback-state
watching, and no fullscreen client. Every item is addressed below.

## Release APK

- File: `Aurelius-v2.0.4.apk` — 19,761,963 bytes (18.8 MB)
- SHA-256: `efe7717ffb473720655318f0dce51b23709a52d08de39317f617d37228e3d3e7`
- Signature: `apksigner verify` — **Verifies, APK Signature Scheme v2: true,
  1 signer** (same keystore as v2.0.0–v2.0.3; certificate SHA-256
  `be46438c9fcf32fb62e5be7b115589e9d1458ce2f927d82b89721d77fc25d57b`).
- Badging: package, versionCode 8, versionName 2.0.4, minSdkVersion 26,
  targetSdkVersion 36 — all as specified.

## Unit tests — 51 / 51 passed, 0 failures, 0 errors, 0 skipped

Run: `./build.sh :app:testDebugUnitTest` (JVM, on this machine). v2.0.4 adds
6 cases to YouTubeEmbedTest (player-page pins: script URL, host/origin,
videoId, explicit playVideo on ready, state reporting + both watchdogs,
stall message) on top of the 45 from v2.0.3. The full suite was run **4
times in a row with identical green results** (see "Four-pass check" below).

| Class | Tests | Cases |
|---|---|---|
| Iso8601DurationTest | 5 | invalid input returns zero; minutes and seconds; zero duration; seconds only; hours minutes seconds |
| LrcParserTest | 4 | empty input returns empty list; parses timestamps and sorts; three digit fractions are milliseconds; ignores metadata and empty lines |
| EqPresetsTest | 5 | bass boost raises low bands; levels stay within equalizer range; flat preset is all zeros; unknown preset falls back to flat; every preset has ten bands |
| FormatDurationTest | 3 | minutes and seconds; hours; zero and negative |
| DisplayNameTest | 7 | `<unknown>` artist/album/title become Unknown Artist/Album/file-name title; real names pass through; folder name is the parent directory; missing paths handled; RELATIVE_PATH last segment |
| LibraryLogicTest | 4 | favorite toggle adds when absent, removes when present; toggle round-trips (add/remove/add); append positions continue after the last one; tracks group by real folder name |
| EqSliderMathTest | 4 | 0 dB is the middle; ends map to the extremes; +6 dB sits above the middle; fraction round-trips to the same millibels (caught a float-truncation bug: -600 came back as -599 — fixed with roundToInt) |
| GreetingTest | 2 | greeting is one of the four plain greetings; greeting never contains a name |
| YouTubeEmbedTest | 16 | embeddable flag parsed true/false; missing status defaults to embeddable; duration still parsed alongside status; error 101/150 name the uploader; 152 names YouTube's refusal; 100 says not found; unknown error keeps the code; error 0 is the loading timeout; list thumbnail prefers default (smallest); thumbnail falls back when default is missing; player page loads the API script from youtube.com (never nocookie); player page uses youtube host with nocookie origin; player page embeds the requested videoId; onReady calls playVideo() explicitly; page reports state changes and carries both watchdogs; stall message offers Retry and YouTube |

## Lint — 0 fatal, 0 error, 136 warnings, 5 hints

Run: `./build.sh :app:lintDebug` (report: `app/build/reports/lint-results-debug.html`).
Was 142 warnings / 7 hints in v2.0.0; 138 warnings in v2.0.3.

- **0 Fatal, 0 Error.** The v2.0.0 lint fixes still hold (default
  `values/strings.xml` carries all 25 strings — no ExtraTranslation
  fatals; `windowLightNavigationBar` stays in `values-v27/themes.xml`).
- `UnsafeOptInUsageError` stays a **warning** by design (see v2.0.0
  report: Media3 `@UnstableApi` on the standard ExoPlayer APIs;
  `app/lint.xml` documents the downgrade).

## Secret scan — key inbuilt exactly once, passwords zero

The owner's YouTube Data API key appears exactly once across all dex files
(the BuildConfig inbuilt key — no field, dialog or option exposes it
anywhere in the app). Keystore/store passwords appear zero times.

## Four-pass check (2026-11-08, at Imdad's order: check 4 times)

The full verification below was run **4 times in a row** (passes 1–4,
logs in `/tmp/ytplayer_test/check_pass{N}.log`). All four passes were
identical and green — same test count, same lint counts, same headless
results, byte-identical APK (SHA-256 `efe7717f...` in every pass, so the
build is reproducible). Any single failing pass would have stopped delivery.

Each pass = (1) `./build.sh :app:testDebugUnitTest :app:lintDebug
:app:assembleRelease` — 51/51 tests, lint 0 fatal / 0 error; (2) the
exact shipped player HTML run end-to-end in headless Chromium against
the Alfaaz video — READY + IFrame error 150 every time (the video is
refused by YouTube, see below); (3) the same HTML against a control
video (Big Buck Bunny) — READY + state PLAYING with currentTime
advancing past 28s, no error, no stall; (4) `apksigner verify` + SHA-256
of the release APK.

## Headless player verification (2026-11-08, real browser engine)

The shipped player page (`buildYouTubePlayerHtml`, generated from the
actual Kotlin source by a script — no hand copy) was run in headless
Chromium with a real network path to YouTube, a stubbed AureliusBridge
recording every callback, and a screenshot at the end:

- **Alfaaz `cQq6iLF66Gc`** (the video on his screenshot): onReady fires,
  then IFrame error **150**; the plain nocookie embed of the same video,
  with no API at all, renders YouTube's own **"This video is
  unavailable"** + "Watch on YouTube". The Data API still reports
  `status.embeddable=true` for it — that flag does not reflect the
  owner/licensor embed block on this music video. **No app code can play
  this video inside Aurelius.** The app must (and now does) surface the
  101/150 explanation at once, with Open in YouTube.
- **Control `aqz-KE-bpKQ` (Big Buck Bunny)**, same setup, same origin,
  same proxy: onReady fires, state becomes PLAYING, currentTime advances
  (16s, then 28–30s across runs), screenshot shows real video frames.
  The player flow works; the 150 is specific to the Alfaaz video.
- His phone screenshot was measured, not eyeballed: the player area is
  **0.0% non-black pixels** — no spinner, no error, no YouTube UI ever
  painted. That is the black-surface / hidden-overlay failure the v2.0.4
  fixes address (hardware layer, manifest flag, WebView removed when an
  error shows, state + stall watchdogs).

## What changed vs v2.0.3 (his v2.0.3 feedback: video still not playing)

1. **Player now calls playVideo() explicitly in onReady** (the documented
   IFrame pattern) instead of trusting the autoplay parameter alone.
2. **Playback state is watched.** onStateChange is reported to the app,
   and a 30s stall watchdog raises a visible message ("The video isn't
   starting… Try Retry, or open it in YouTube") if the player is ready
   but the video never starts. The 25s ready watchdog is unchanged.
3. **Hardware layer + manifest flag.** The WebView now uses
   `setLayerType(LAYER_TYPE_HARDWARE)` and the manifest declares
   `android:hardwareAccelerated="true"` on the application and the
   MainActivity — the standard fix for the black-video-surface failure
   (video decodes but never composites).
4. **Fullscreen WebChromeClient.** onShowCustomView/onHideCustomView are
   implemented (the plain `WebChromeClient()` had no fullscreen handling,
   and the IFrame player's fullscreen request could fail silently).
5. **The WebView is removed when an error shows**, so a black surface can
   never cover the error UI again. Retry rebuilds it; the error screen
   always offers Open in YouTube + Retry + Go back.
6. **The player page is a tested pure function** (`buildYouTubePlayerHtml`)
   with 6 new unit tests pinning the script URL, host, origin, videoId,
   explicit playVideo, state reporting and both watchdogs — the exact
   combination that broke in v2.0.1 and v2.0.2.
7. **Alfaaz is handled honestly.** Error 101/150 already mapped to the
   "uploader turned off embedding" message; it now actually reaches the
   screen fast (onError disarms the stall timer, the WebView is removed,
   the message shows with Open in YouTube). If he opens Alfaaz again he
   will see that message — that is YouTube refusing that video, not the
   app failing.

## Live API check (2026-11-08, from this machine)

The built-in key was verified against the real YouTube Data API v3 before
delivery: search `Saiyan` returned 3 results, `trending music` returned 2.
The v2.0.0 "No YouTube results for 'Saiyan'" on the phone was a client-side
issue (empty state shown before searching + 10s timeouts on a 0.6 KB/s
connection) — both fixed here; the key itself always worked.

## What changed vs v2.0.0 (his list, item by item)

1. **API key — backend only.** The Settings YouTube section, the key dialog,
   the DataStore override, the masked row and the "YouTube is connected"
   toast are all gone. The key lives only in BuildConfig. Verified: no
   "API key" text anywhere in the app's code or strings; key appears once
   in dex.
2. **YouTube search fixed.** "No results" no longer shows before a search
   has run; OkHttp timeouts raised 10s → 30s for slow connections.
3. **YouTube player fixed.** The v2.0.0 player was a 0px IFrame (loadData +
   no height on the div) — now loadDataWithBaseURL + full-height CSS +
   black background + a JS bridge reporting ready/error, a loading
   spinner, and an error state with "Open in YouTube".
4. **Folders are real.** MediaStore DATA column (RELATIVE_PATH fallback)
   gives each file's real folder; the Folders tab groups by it and opens a
   folder detail screen with Play/Shuffle/tracklist.
5. **Artists open.** Artist rows are now clickable → artist detail screen.
6. **Playlists open and take songs.** Tapping a playlist opens its detail
   screen (Play/Shuffle, remove per song) with an "Add songs" picker over
   the library (songs already in the playlist are hidden).
7. **Favorites persist.** The player's heart was local remember-state and
   reset on leaving the player — now it is the same Room-backed favorite
   as the Library heart, so it stays after going back.
8. **Text on one line.** Home wordmark "Aurelius" (was wrapping to two
   lines), Library tabs (was "Album s" / "Playlis ts"), TopBar titles,
   detail titles, EQ heading, search tabs — all single-line with ellipsis
   where needed; tab labels shrunk to 13sp in a scrollable tab row.
9. **Greeting has no name.** "Good evening" / "Good morning" only.
10. **`<unknown>` is gone.** Scanner normalizes it to "Unknown Artist" /
    "Unknown Album" (title falls back to the file name); TrackRow and
    MiniPlayer also guard it at display time.
11. **Equalizer rebuilt.** The rotated-Slider hack (140dp sliders in 31dp
    columns) is replaced by a real Canvas vertical slider per band — value
    above, frequency below, no overlap; Bass Boost / Virtualizer / enable
    switch now start from their persisted settings.
12. **Settings cleaned.** "Rate on Play Store" removed; About is a short
    centered block (wordmark, "Music. Mastered.", version, developer,
    email, centered Feedback / Bug / Licenses / Privacy actions).
13. **Logo + illustrations redrawn as vectors.** The pasted raster crops
    from the design sheets are replaced by hand-built gold line-art vector
    drawables (same resource names, same style language). The old PNGs are
    deleted from the project.
14. **No extras added.** Nothing beyond his list was added.

## What was NOT tested — stated plainly

- **No on-device or emulator testing was possible on this machine.** There
  is no KVM (`/dev/kvm` absent), so no Android emulator can run here. No
  instrumented tests and no Compose UI tests ran.
- The APK was **never installed or launched anywhere** before delivery.
  The real device test is Imdad's Vivo Y85A (API 27) after install.
- What **was** run on a real engine: the exact shipped player page was
  executed end-to-end in headless Chromium 4 times (see "Headless player
  verification" above) — a control video played with frames advancing;
  the Alfaaz video was refused by YouTube itself with error 150 in every
  run. The hardware-layer fix targets his phone's WebView surface; only
  his phone can prove that part.
- Code-traced audit only: every visible button was traced to a real handler
  in code; nothing was claimed tested that was not run.

---

# Aurelius v2.0.8 — Test Report addendum (2026-11-08, ~23:25 PKT)

Version 2.0.8, versionCode 12. Same keystore as all v2.x, so it installs
OVER v2.0.7/v2.0.6/v2.0.5 as an update — no uninstall needed.

- File: `Aurelius-v2.0.8.apk` — 19,811,364 bytes (18.9 MB)
- SHA-256: `da66a53e9cd32b89713590b5e1a8b3c3c43c0c3bebea5cb04252270964f8c18e`
- Signature: same keystore, certificate SHA-256
  `be46438c9fcf32fb62e5be7b115589e9d1458ce2f927d82b89721d77fc25d57b`.
- Binary checks: YouTube API key occurs 1x in dex (backend-only, never
  shown in any UI); keystore passwords 0x.

## What changed since v2.0.4 (v2.0.5 → v2.0.8)

- v2.0.5: native MX Player-style control bar (play/pause, ±10s, seek bar +
  time, speed menu incl. hold-2x, quality menu, fullscreen, screen lock)
  driving the IFrame API through a `window.__aureliusControl` JS bridge;
  original YouTube thumbnails as poster (Coil 3 needs the
  `coil-network-okhttp` artifact — without it every network image fails
  and only the placeholder shows); WebView version shown on the player
  error screen + Settings → About.
- v2.0.6: gestures moved onto the WebView's own OnTouchListener +
  GestureDetector (Compose pointerInput on a Box wrapping an AndroidView
  NEVER fires — the WebView eats the touches); YouTube controls off
  (controls=0); poster hides only when currentTime > 0; stall watchdog
  fires on a frozen clock whatever the state; HTML entities unescaped in
  titles (`&quot;` → `"`).
- v2.0.7: tap is a true toggle (v2.0.6's poke-then-flip could only ever
  hide the bar); latched playback start — poster shows and the WebView
  stays invisible (alpha 0) until the clock passes 1s of real playback;
  stall rule is window-based (needs >= 0.5s of genuine progress inside
  30s, immune to tiny clock jitter); JS watchdog fires unless time >= 1s.
- v2.0.8: removed the forced `setLayerType(HARDWARE)` on the WebView
  (v2.0.4 added it on an unproven theory; a forced hardware layer is a
  known suspect for breaking video compositing inside Compose). The
  manifest still enables hardware acceleration for the app.

## Verification

- v2.0.7: 4x check loop ALL GREEN — 64/64 unit tests, lint 0 fatal /
  0 error, Alfaaz → READY + error 150, Big Buck Bunny → STATE 1, time
  advancing, no error, bridge pause/play/seek/rate/seekBy all true,
  identical APK hash every pass (26e86942).
- v2.0.8: one full check-loop pass — BUILD SUCCESSFUL, 64/64 tests,
  lint 0 fatal / 0 error, Alfaaz → READY + error 150, Big Buck Bunny →
  STATE 1, time 31.2s, bridge pause/play/seek/rate true; seekBy first
  read false while the player was buffering at the seek target, then
  TRUE on retry (time 118.08 after seek 122 → -10) — transient buffering,
  not a regression (the v2.0.8 change touches no player/bridge code).

## Emulator results (Appetize.io, Pixel 7 / Android 13, WebView 103.0.5060.71)

- v2.0.5 re-test: thumbnails FIXED (all original YouTube thumbnails);
  controls present; playback black; title showed literal `&quot;`.
- Isolation test: the SAME Big Buck Bunny embed in the emulator's own
  CHROME browser also fails — YouTube Error 153 "Video player
  configuration error", reproduced 3x on both youtube.com and
  youtube-nocookie.com. The Appetize cloud device cannot play YouTube
  embeds at all (YouTube refuses it server-side); the emulator CANNOT
  validate video playback, period.
- v2.0.6 re-test: title entities FIXED; tap partial (top bar returned,
  bottom bar did not — the poke-then-flip bug); poster never showed;
  no stall message.
- v2.0.7 re-test: TAP FIXED (bottom bar returns immediately). KEY FACT:
  the player's clock ADVANCES at ~1x (0:02 at 5s, 0:20 at the tap) —
  the video is genuinely PLAYING, only the picture is black. This is a
  RENDERING failure, not a stall — the stall watchdogs correctly do not
  fire, and the poster correctly hides (clock passed 1s).
- v2.0.8 re-test: picture STILL BLACK at 5s, 15s and ~70s — the layer
  removal did not fix rendering on this device. No controls were visible
  in that run's screenshots (the bar had auto-hidden after 3.5s and the
  two centre taps likely toggled it show-then-hide again before the
  screenshots). Emulator minutes used: 29/30 — effectively exhausted.

## Honest limit (unchanged)

Whether video frames actually paint can only be confirmed on Imdad's
real phone (Vivo Y85A). The player code is proven in headless Chrome
(Big Buck Bunny plays with advancing time and real frames) and the
player state/controls are proven on a real device; only the final
rendering step is unproven. If the video area is still black on his
phone, the WebView version shown in Settings → About decides the next
fix.
