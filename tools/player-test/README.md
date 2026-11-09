# Headless player test rig (built 2026-11-08 for the v2.0.4 black-screen fix)

Runs the EXACT shipped player page (`buildYouTubePlayerHtml`, generated
from the Kotlin source — no hand copy) end-to-end in headless Chromium
with a real network path to YouTube, and reports the real player state.

## Why it exists
Unit tests + lint never caught the player failures (v2.0.1 error 152,
v2.0.2 script 404, v2.0.3 black screen / error 150). This rig runs the
player and watches it play: a control video must reach PLAYING with
currentTime advancing; a blocked video must surface its error code.

## Files
- `gen_test_pages.py` — generates test pages from the Kotlin template
  (only the origin is swapped to match the local server, replicating the
  app's matched-origin condition) + a stub AureliusBridge recording
  every callback.
- `drive.mjs` — CDP driver (Node built-in WebSocket, no deps): loads a
  page, waits 35s, prints the probe, saves a screenshot.
- `auth_proxy.py` — local forward proxy adding Proxy-Authorization for
  the VM egress proxy (Chrome rejects credentials in --proxy-server;
  the plain loopback forwarder fails for youtube.com). 127.0.0.1 only.
- `netcheck.mjs` — minimal Chrome network check (is youtube reachable?).
- `check_loop.sh <n>` — one full verification pass: gradle tests+lint+
  release build, headless Alfaaz + control runs, apksigner + SHA.
- `package_v204.sh` — package a verified release APK + source ZIP.

## Run
```
cd /tmp/ytplayer_test   # or copy this dir to /tmp first
python3 auth_proxy.py & # background session, not nohup
python3 -m http.server 8931 --bind 127.0.0.1 &  # background session
python3 gen_test_pages.py
node drive.mjs "http://127.0.0.1:8931/v204_alfaaz.html" "" "run1"
node drive.mjs "http://127.0.0.1:8931/v204_control.html" "" "run2"
```

## Measured results (2026-11-08)
- Alfaaz `cQq6iLF66Gc`: READY + IFrame error 150; plain nocookie embed
  renders "This video is unavailable". Data API says embeddable=true —
  that flag does not reflect owner/licensor embed blocks. No app code
  can play such a video in-app.
- Control `aqz-KE-bpKQ` (Big Buck Bunny): READY + PLAYING, currentTime
  advancing 16-30s, screenshot shows real frames.
- His phone screenshot: player area 0.0% non-black pixels — nothing
  ever painted (black-surface / hidden-overlay failure).
