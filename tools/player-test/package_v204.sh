#!/bin/bash
# Package Aurelius v2.0.4 for delivery (run AFTER the 4x loop passes).
# Copies the release APK + a source ZIP (no local-maven, no build dirs)
# to ~/workspace/your_files/ and the goal files dir, prints SHA-256 +
# apksigner verification. Fails loudly on any missing piece.
set -euo pipefail
SRC_APK=~/workspace/aurelius-v2/app/build/outputs/apk/release/app-release.apk
[ -f "$SRC_APK" ] || { echo "MISSING APK: $SRC_APK"; exit 1; }
# The APK must be newer than the last source edit (guards against a stale build).
SRC_MTIME=$(stat -c %Y ~/workspace/aurelius-v2/app/src/main/java/com/imdadburki/aurelius/feature/youtube/YouTubePlayerScreen.kt)
APK_MTIME=$(stat -c %Y "$SRC_APK")
[ "$APK_MTIME" -ge "$SRC_MTIME" ] || { echo "STALE APK: older than latest source edit"; exit 1; }
cp "$SRC_APK" ~/workspace/your_files/Aurelius-v2.0.4.apk
cp "$SRC_APK" ~/workspace/goals/aurelius-music-app/files/Aurelius-v2.0.4.apk
cd ~/workspace/aurelius-v2
rm -f ~/workspace/your_files/Aurelius-v2.0.4-source.zip
zip -r -q ~/workspace/your_files/Aurelius-v2.0.4-source.zip . \
  -x "local-maven" "local-maven/*" "app/build/*" ".gradle/*" "*/build/*" "gradle/*"
cp ~/workspace/your_files/Aurelius-v2.0.4-source.zip ~/workspace/goals/aurelius-music-app/files/
export PATH="$HOME/workspace/jdk/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH"
echo "--- APK ---"
ls -l ~/workspace/your_files/Aurelius-v2.0.4.apk
sha256sum ~/workspace/your_files/Aurelius-v2.0.4.apk
~/workspace/android-sdk/build-tools/36.0.0/apksigner verify --print-certs ~/workspace/your_files/Aurelius-v2.0.4.apk
echo "--- source ZIP ---"
ls -l ~/workspace/your_files/Aurelius-v2.0.4-source.zip
unzip -l ~/workspace/your_files/Aurelius-v2.0.4-source.zip | tail -3
echo "--- key/secret scan (expect: key 1x in dex, passwords 0x) ---"
~/workspace/android-sdk/build-tools/36.0.0/aapt2 dump strings ~/workspace/your_files/Aurelius-v2.0.4.apk 2>/dev/null | grep -c "AIza" || true
