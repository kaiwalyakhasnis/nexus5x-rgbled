#!/bin/bash
# Builds rgbled-sdk.jar (compile-only) from the platform's generated public SDK stubs.
# Prerequisite, in the LineageOS tree after changing the API:  m update-api
set -euo pipefail

TOP=${ANDROID_BUILD_TOP:?run "source build/envsetup.sh && lunch" in the LineageOS tree first}
STUB_SRC=$TOP/out/target/common/obj/JAVA_LIBRARIES/android_stubs_current_intermediates/src
REAL_SRC=$TOP/frameworks/base/core/java/android/hardware/rgbled/RgbLedManager.java
OUT_DIR=$(cd "$(dirname "$0")" && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

mkdir -p "$WORK/classes" "$WORK/sources/android/hardware/rgbled"
javac -source 1.8 -target 1.8 -nowarn -d "$WORK/classes" \
    "$STUB_SRC"/android/hardware/rgbled/*.java
jar cf "$OUT_DIR/rgbled-sdk.jar" -C "$WORK/classes" android

# Javadoc for IDEs: the real source, attached as a sources jar.
cp "$REAL_SRC" "$WORK/sources/android/hardware/rgbled/"
jar cf "$OUT_DIR/rgbled-sdk-sources.jar" -C "$WORK/sources" android

sed -n '/^package android.hardware.rgbled {/,/^}/p' "$TOP/frameworks/base/api/current.txt" \
    > "$OUT_DIR/api.txt"

unzip -l "$OUT_DIR/rgbled-sdk.jar"

if [ -d "$OUT_DIR/../RgbLedDemo/app/libs" ]; then
    cp "$OUT_DIR/rgbled-sdk.jar" "$OUT_DIR/../RgbLedDemo/app/libs/"
fi
