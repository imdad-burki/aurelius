#!/bin/bash
# One full verification pass for Aurelius v2.0.8 (run 4x; a pass counts only
# when EVERY section below says PASS).
# Usage: check_loop.sh <pass-number>
set -u
PASS=$1
LOG=/tmp/ytplayer_test/check_pass${PASS}.log
{
echo "=== PASS $PASS — $(date -u +%FT%TZ) ==="
echo "--- 1. unit tests + lint + release build ---"
cd ~/workspace/aurelius-v2
./build.sh :app:testDebugUnitTest :app:lintDebug :app:assembleRelease > /tmp/ytplayer_test/gradle_p${PASS}.log 2>&1
G=$?
echo "gradle exit: $G"
grep -E "FAILED|BUILD" /tmp/ytplayer_test/gradle_p${PASS}.log | tail -5
if [ $G -eq 0 ]; then
  find app/build/test-results/testDebugUnitTest -name "*.xml" -exec grep -h "testsuite name" {} \; | sed -E 's/.*tests="([0-9]+)".*failures="([0-9]+)".*errors="([0-9]+)".*/tests=\1 failures=\2 errors=\3/' | awk -F'[= ]' '{t+=$2; f+=$4; e+=$6} END {print "TOTAL tests="t" failures="f" errors="e}'
  grep -oE "lint.*[0-9]+ fatal.*[0-9]+ error" app/build/reports/lint-results-debug.html 2>/dev/null | head -1 || echo "lint report: check manually"
fi
echo "--- 2. headless player: Alfaaz (expect READY + ERR=150) ---"
cd /tmp/ytplayer_test
node drive.mjs "http://127.0.0.1:8931/v208_alfaaz.html" "" "p${PASS}_alfaaz" 2>&1
echo "--- 3. headless player: control BBB (expect READY + STATE=1, TIME>0, ERR=none) ---"
node drive.mjs "http://127.0.0.1:8931/v208_control.html" "" "p${PASS}_control" 2>&1
echo "--- 4. headless control bridge (expect playing + pause/play/seek/rate ok) ---"
node control_test.mjs "http://127.0.0.1:8931/v208_control.html" "p${PASS}_ctrl" 2>&1
echo "--- 5. APK signing + hash ---"
APK=~/workspace/aurelius-v2/app/build/outputs/apk/release/app-release.apk
if [ -f "$APK" ]; then
  ls -l "$APK"
  sha256sum "$APK"
  export PATH="$HOME/workspace/jdk/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH"
  ~/workspace/android-sdk/build-tools/36.0.0/apksigner verify --print-certs "$APK" 2>&1 | head -6
else echo "APK MISSING"; fi
echo "=== END PASS $PASS ==="
} | tee "$LOG"
