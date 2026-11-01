#!/data/data/com.termux/files/usr/bin/sh
set -eu
cd "$(dirname "$0")/.."
python tools/prepare_cloudflared.py
/data/data/com.termux/files/usr/share/gradle-8.7/bin/gradle -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2 assembleDebug
mkdir -p apk
cp app/build/outputs/apk/debug/app-debug.apk apk/RetroPlayer.apk
