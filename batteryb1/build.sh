#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/batteryb1"
SDK="${ANDROID_HOME:?}"
BT="$SDK/build-tools/34.0.0"
AJ="$SDK/platforms/android-34/android.jar"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/classes" "$TMP/dex" "$ROOT/output-batteryb1"
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" -d "$TMP/classes" "$SRC/src/MainActivity.java"
find "$TMP/classes" -name '*.class' > "$TMP/classes.txt"
"$BT/d8" --min-api 26 --lib "$AJ" --output "$TMP/dex" @"$TMP/classes.txt"
"$BT/aapt2" link -I "$AJ" --manifest "$SRC/AndroidManifest.xml" -o "$TMP/base.apk"
(cd "$TMP/dex" && zip -q "$TMP/base.apk" classes*.dex)
"$BT/zipalign" -f 4 "$TMP/base.apk" "$TMP/aligned.apk"
keytool -genkeypair -keystore "$TMP/test.jks" -alias preview -storepass preview123 -keypass preview123 -keyalg RSA -keysize 2048 -validity 365 -dname "CN=Air2023 Preview" >/dev/null 2>&1
OUT="$ROOT/output-batteryb1/Air2023-Battery-B1.apk"
"$BT/apksigner" sign --ks "$TMP/test.jks" --ks-pass pass:preview123 --key-pass pass:preview123 --out "$OUT" "$TMP/aligned.apk"
"$BT/apksigner" verify --verbose "$OUT"
"$BT/aapt2" dump badging "$OUT" > "$ROOT/output-batteryb1/manifest.txt"
(cd "$ROOT/output-batteryb1" && sha256sum *.apk > SHA256SUMS.txt)
cp "$SRC/README.md" "$ROOT/output-batteryb1/"
git -C "$ROOT" rev-parse HEAD > "$ROOT/output-batteryb1/build-commit.txt"
