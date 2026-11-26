#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
BT="$ANDROID_HOME/build-tools/34.0.0"
AJ="$ANDROID_HOME/platforms/android-34/android.jar"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/classes" "$TMP/dex" output-windowlab
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" -d "$TMP/classes" windowlab/src/*.java
find "$TMP/classes" -name '*.class' > "$TMP/classes.txt"
"$BT/d8" --min-api 26 --lib "$AJ" --output "$TMP/dex" @"$TMP/classes.txt"
"$BT/aapt2" link -I "$AJ" --manifest windowlab/AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 33 -o "$TMP/base.apk"
(cd "$TMP/dex" && zip -q "$TMP/base.apk" classes*.dex)
"$BT/zipalign" -f 4 "$TMP/base.apk" "$TMP/aligned.apk"
keytool -genkeypair -keystore "$TMP/test.jks" -alias lab -storepass lab12345 -keypass lab12345 -keyalg RSA -keysize 2048 -validity 365 -dname "CN=Air2023 WindowLab" >/dev/null 2>&1
OUT=output-windowlab/Air2023-WindowLab-D1.apk
"$BT/apksigner" sign --ks "$TMP/test.jks" --ks-pass pass:lab12345 --key-pass pass:lab12345 --out "$OUT" "$TMP/aligned.apk"
"$BT/apksigner" verify --verbose "$OUT"
"$BT/aapt2" dump badging "$OUT" > output-windowlab/manifest.txt
grep -F "package: name='dev.air2023.windowlab' versionCode='50' versionName='0.5-D1'" output-windowlab/manifest.txt
(cd output-windowlab && sha256sum *.apk > SHA256SUMS.txt)
cp windowlab/README.md output-windowlab/
git rev-parse HEAD > output-windowlab/build-commit.txt
