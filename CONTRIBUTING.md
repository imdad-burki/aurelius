# Contributing to Aurelius

Thanks for helping improve Aurelius. This guide covers the basics — please read it before opening a pull request.

## Building

- JDK 17 (`JAVA_HOME` set to a JDK 17 install) and an Android SDK with platform 37 + build-tools 36 (`ANDROID_HOME`).
- Copy `gradle.properties.example` to `gradle.properties` and fill in your own `YOUTUBE_API_KEY` (a YouTube Data API v3 key from Google Cloud Console, restricted to that API). The app reads it at build time; it never appears in any UI.
- Build and verify:

```bash
./build.sh :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

Unit tests use JUnit4. The APK lands in `app/build/outputs/apk/release/`.

## Never commit secrets

- `gradle.properties`, `*.keystore`, `*.jks`, and `local.properties` are git-ignored for a reason. Generate your own release keystore — never reuse or publish someone else's.
- Never paste API keys, passwords, or tokens into issues, PRs, or code.

## Scope

- No downloading, no stream extraction, no offline YouTube playback. Search uses the official YouTube Data API v3 and playback uses the officially supported IFrame embed.
- Keep the Aurum Dark design (gold `#D4AF37` on near-black `#0A0A0A`) and the original logo/illustrations untouched.

## Pull requests

- Small, focused PRs: one change per PR.
- Describe what changed and how you tested it (unit tests, lint, and — if you ran it — on which device/emulator).
- Update the README or docs when behaviour changes.
