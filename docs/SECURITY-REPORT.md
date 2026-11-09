# Aurelius v2.0.3 — Security Report

## Permissions (from `aapt2 dump badging`, all justified)

| Permission | Why it is needed |
|---|---|
| INTERNET | YouTube Data API v3 search, LRCLIB lyrics lookup |
| ACCESS_NETWORK_STATE | Check connectivity before network calls (error states) |
| FOREGROUND_SERVICE + FOREGROUND_SERVICE_MEDIA_PLAYBACK | Background playback via MediaSessionService |
| WAKE_LOCK | Keep playback alive while the screen is off |
| POST_NOTIFICATIONS | Playback notification / lock-screen controls (Android 13+) |
| READ_MEDIA_AUDIO | Scan the device's own music files (MediaStore) |
| READ_EXTERNAL_STORAGE (maxSdkVersion 32) | Same, on Android 12 and below |
| MODIFY_AUDIO_SETTINGS | Equalizer / BassBoost / Virtualizer audio effects |

No location, contacts, camera, microphone, SMS, or phone-state permission is
requested. No permission is requested that the app does not use.

## Network and data

- **Cleartext traffic: off** (default; no `usesCleartextTraffic` override).
- **No third-party tracking, analytics, crash SDK, or ad SDK** is bundled.
- Network calls go only to: `youtube.googleapis.com` (Data API v3 search),
  `lrclib.net` (synced lyrics), and the YouTube IFrame embed
  (`youtube.com`/`youtube-nocookie` via WebView) for in-app playback.
- No user data leaves the device except the search text itself, sent to the
  YouTube API and LRCLIB as part of the user's own search/lyrics request.
- Playlists, favorites, history, and settings live in on-device Room /
  DataStore only.

## YouTube API key — handling and honest tradeoff

- The key is Imdad's own key (Google Cloud project "Aurelius",
  `sublime-elixir-514101-m3`, on imdadburki12@gmail.com), created 2026-11-08
  at his explicit order and **reused at his standing order** ("Purani key hi
  use kro"). It is restricted to the YouTube Data API v3 only, with no
  billing or trial on the project.
- **v2.0.1: the key is backend-only.** There is no Settings field, dialog,
  masked row, override, or "connected" status anywhere in the app — the
  key lives only in BuildConfig and is used by the search code directly.
  The key value is never printed in chat, logs, or memory, and never shown
  on screen in any form.
- The key is compiled into the APK as a BuildConfig string (same as v1.x).
  **Secret scan of the final APK (all dex files, by exact match): the
  key appears exactly once; the keystore/store passwords appear zero times.**
  Anyone who decompiles the APK can read the key — that is the tradeoff of
  embedding it. A backend proxy would be stricter, but there is no server
  here; the API restriction (YouTube Data API v3 only) is the mitigation.
- Key rotation was cancelled by Imdad 2026-11-08 ~09:32 — not re-raised.

## Keystore

- NEW release keystore `aurelius-release.keystore` (RSA 2048, 10,000-day
  validity, CN=Imdad Burki, Peshawar, PK), generated 2026-11-08. Passwords
  live only in the project's `gradle.properties` — never in chat or memory.
- The source ZIP delivered with the APK **contains the keystore and
  gradle.properties** so Imdad (the owner) can rebuild signed updates with
  the same key. Keep that ZIP private — anyone with it can sign updates as
  this app.
- Same signing key as v2.0.0, so **v2.0.1 installs OVER v2.0.0 as an update**;
  no uninstall needed. (The old v1.x key was different — that one did need
  an uninstall, which is why v2.0.0 said so.)

## What the app does NOT do (per the spec's own rules)

- No YouTube stream extraction, no downloading of YouTube content, no
  bypass of any limit. YouTube search uses the official Data API v3;
  playback uses the official IFrame embed in a WebView, with a fallback
  that opens the YouTube app.
- No fake functionality: every visible button was traced to a real handler;
  where a feature is honestly unavailable it says so (see LIMITATIONS.md).
  No sign-in exists in the app (the spec's sign-in line was omitted because
  no auth is implemented).
