#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BT="$ANDROID_HOME/build-tools/34.0.0"
AJ="$ANDROID_HOME/platforms/android-34/android.jar"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/classes" "$TMP/dex" output-windowlab3
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" -d "$TMP/classes" windowlab3/src/*.java
find "$TMP/classes" -name '*.class' > "$TMP/classes.txt"
"$BT/d8" --min-api 26 --lib "$AJ" --output "$TMP/dex" @"$TMP/classes.txt"
"$BT/aapt2" link -I "$AJ" --manifest windowlab3/AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 33 -o "$TMP/base.apk"
(cd "$TMP/dex" && zip -q "$TMP/base.apk" classes*.dex)
"$BT/zipalign" -f 4 "$TMP/base.apk" "$TMP/aligned.apk"
keytool -genkeypair -keystore "$TMP/test.jks" -alias lab -storepass lab12345 -keypass lab12345 -keyalg RSA -keysize 2048 -validity 365 -dname "CN=Air2023 WindowLab" >/dev/null 2>&1
OUT=output-windowlab3/Air2023-WindowLab-D3.apk
"$BT/apksigner" sign --ks "$TMP/test.jks" --ks-pass pass:lab12345 --key-pass pass:lab12345 --out "$OUT" "$TMP/aligned.apk"
"$BT/apksigner" verify --verbose "$OUT"
"$BT/aapt2" dump badging "$OUT" > output-windowlab3/manifest.txt
grep -F "package: name='dev.air2023.windowlab3' versionCode='53' versionName='0.5-D3'" output-windowlab3/manifest.txt
(cd output-windowlab3 && sha256sum *.apk > SHA256SUMS.txt)
cp windowlab3/README.md output-windowlab3/
git rev-parse HEAD > output-windowlab3/build-commit.txt
