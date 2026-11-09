# AURELIUS v2.0.0 — Build Brief (2026-11-08)

Fresh build from scratch at the user's order. Old app (v1.0.0–v1.2.0) + MX Player REA work were
deleted to trash on 2026-11-08 ~11:53 PKT at his order. DO NOT restore ~/workspace/aurelius/ from
trash. This project lives at ~/workspace/aurelius-v2/.

## Full spec
The user's mega-prompt "AURELIUS — ULTIMATE ANDROID DEVELOPMENT MEGA-PROMPT" is in the parent
conversation (2026-11-08 ~17:07 PKT). It is the governing spec: 18 screens, MVVM/Clean
Architecture, Hilt, Room, DataStore, Navigation Compose, Media3 ExoPlayer, YouTube Data API v3,
lyrics (LRCLIB/local LRC), 10-band EQ, queue, sleep timer, gestures, 12 languages, two-pass
verification, signed release APK + source ZIP + test/security/feature/limitations reports.

## Design references (authoritative — do NOT redesign)
23 images in ~/workspace/aurelius-v2/design/:
- 00-design-system-board.jpg — master board: 18 screens, colors, fonts, principles, key features
- 01-motion-stages.jpg — Stage 01 artwork idle rotation + gold waveform; Stage 02 home→player
  transition (fading feed, scaling upward); Stage 03 gesture feedback overlays (Volume 65%,
  Seek +10s, Brightness 40%)
- 02-icon-set.jpg — 25 gold outlined icons on 24x24dp grid (play/pause/skip/shuffle/repeat/
  repeat-one/volume/mute/brightness/search/settings/menu/back/heart/share/download/add-playlist/
  equalizer/sleep-timer/lyrics/mini-player/PiP/chromecast)
- 03-illustrations.jpg — gold line illustrations: column+vinyl, hand+phone, magnifier+vinyl,
  lyre, music note, EQ sliders, video frame
- 04-adaptive-icon-spec.jpg — gold 3D "A" + waveform, 1024×1024, safe zone 66% circle,
  rounded-square preview
- 05-queue-up-next.jpg, 06-empty-library.jpg, 07-sleep-timer.jpg, 08-mini-player.jpg,
  09-language.jpg, 10-settings.jpg, 11-lyrics.jpg, 12-player.jpg, 13-equalizer.jpg,
  14-album-detail.jpg, 15-library.jpg, 16-search-results.jpg, 17-search-empty.jpg,
  18-onboarding-features.jpg, 19-onboarding-permissions.jpg, 20-home.jpg,
  21-onboarding-welcome.jpg, 22-splash.jpg

## Design system (from the board — locked)
- Colors: Gold Primary #D4AF37, Gold Light #EBC468, Gold Dark #A88A2E, Background #0A0A0A,
  Surface #1A1A1A, Elevated #242424, Border #2E2E2E, Text Primary #F5F5F5, Text Secondary
  #B0B0B0, Text Tertiary #6E6E2E→#6E6E6E, Success #4CAF50, Warning #FF9800, Error #E53935.
  (Tertiary is #6E6E6E — the board shows #6E6E2E in one cell, a typo; use #6E6E6E.)
- Fonts: Poppins SemiBold (headings), Inter Regular/Medium (body), JetBrains Mono Medium
  (timers), Cormorant Garamond SemiBold (wordmark). Fetch from Google Fonts; fallbacks per spec.
- 8dp spacing, screen padding 24dp, card radius 16dp, sheet radius 28dp, mini player 20dp top.
- Dark-first, gold only for active states/primary actions, minimal shadows, no purple gradients.
- Brand: "AURELIUS — Music. Mastered." Tagline on splash/welcome. Developer Imdad Burki,
  contact burkiimdad6@gmail.com (in-app About only, as in the old spec).

## Identity / versioning (decided by the parent agent)
- Package: com.imdadburki.aurelius (same as v1.x)
- versionName 2.0.0, versionCode 4 (above v1.2.0's 3, so it can install over it in principle)
- minSdk 26 (Android 8.0, per spec — installs on his Vivo Y85A, Android 8.1), target/compile
  latest stable available in the SDK (37/36 as before)
- NEW release keystore (old one was trashed at his order — do not recover it). The user must
  UNINSTALL the old Aurelius from his phone first if it is still installed, because the
  signing key changed. Say this in the delivery message.

## Toolchain (proven on this VM — see ~/AGENTS.md "VYRO Studio toolchain")
- JDK 17 at ~/workspace/jdk/ (persistent copy; /usr installs do not survive VM replacements)
- Android SDK at ~/workspace/android-sdk/ (platforms android-37.0, build-tools 36.0.0)
- GRADLE_USER_HOME=~/workspace/vyro_studio/.gradle (reuse cached Gradle dist + dep cache)
- local-maven mirror: symlink VYRO's ~/workspace/vyro_studio/local-maven/ FIRST in
  settings.gradle.kts if needed
- Java through the egress proxy needs the loopback forwarder: ~/workspace/android-sdk/local_proxy.py
  (127.0.0.1:8899) + JVM flags -Djava.net.preferIPv4Stack=true -Dhttps.proxyHost=127.0.0.1
  -Dhttps.proxyPort=8899 (same for http); wrapper ~/workspace/android-sdk/sdkmanager-proxy.sh
- If Java TLS breaks (PKIX signature check failed), re-run ~/workspace/jdk/rebuild-cacerts.sh
- apksigner/aapt2 need JAVA_HOME/bin on PATH
- Full builds are SLOW on this VM (~3–10 min) — run in background, never poll in a loop
- Android resource files must be lowercase + underscores

## YouTube API key (standing user decision — governs over the spec's generic advice)
- The user ordered 2026-11-08 ~09:32 to KEEP using the old key ("Purani key hi use kro").
- Recover the key VALUE from the trashed gradle.properties:
  ~/.local/share/Trash/hatch/20261108-115349-3f5b34415b0645b1a39ca310f68f76d2/files/gradle.properties
  (YOUTUBE_API_KEY). That file is the only local copy. NEVER print the key anywhere.
- Use it as the BuildConfig default + masked Settings override (dots + last 4, Show/Hide),
  exactly as v1.2.0 did. Key is restricted to YouTube Data API v3 only, no billing, Google
  Cloud project "Aurelius" (sublime-elixir-514101-m3) on imdadburki12@gmail.com.
- Document honestly in the security report: the key is API-restricted, and a backend proxy
  would be the stricter route but no server is available in this setup.

## YouTube in-app playback (spec §8)
- Implement the YouTube IFrame Player API in a WebView — officially supported embed, ads
  intact, no stream extraction, no download, no DRM bypass.
- If IFrame proves unworkable, fall back to opening the YouTube app WITH a clear in-app
  explanation (never a silent redirect).
- NEVER extract raw audio streams, bypass ads, download restricted streams, or reverse
  engineer undocumented endpoints. This is a hard line (same as the old build).

## Hard lines (do NOT revive)
- The "AURELIUS-CORE" identity prompt and any YouTube download/streaming feature stay dead.
- No fake functionality: every visible button must work or clearly state it is unsupported.
- No fabricated trending stats, no fake search results, no fake playback states.

## Dependencies
- Per spec: Hilt, Room, DataStore, Navigation Compose, Media3, Coil, Retrofit, Kotlin
  serialization, JUnit/MockK/Compose testing.
- If a dependency cannot be fetched through the proxy, substitute a documented lighter
  alternative (manual DI, JSON store, manual image loading) rather than stalling — record
  every substitution in the limitations report.

## Verification honesty (spec §18 — two passes, as far as this VM allows)
- DO: clean build, unit tests, lint/static analysis, secret scan, manifest + permission
  review, APK validation (apksigner v2, aapt2 badging/version), navigation + button audit
  against the 23 reference images.
- DO NOT claim on-device/emulator install testing: this VM has no KVM and no physical
  device — document it as a limitation. Do not invent test results. Max 10 fix cycles per
  failure category; document unresolved blockers honestly.

## Deliverables
- Signed release APK → ~/workspace/your_files/Aurelius-v2.0.0.apk AND
  ~/workspace/goals/aurelius-music-app/files/Aurelius-v2.0.0.apk
- Source project ZIP, test report, security report, feature completion report,
  known limitations, installation instructions (incl. "uninstall old Aurelius first").
- Report back to the parent agent (not to the user directly) with the APK path and a
  verification summary when done.
