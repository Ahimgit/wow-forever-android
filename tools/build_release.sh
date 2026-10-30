#!/usr/bin/env bash
# Builds the app and optionally installs it on the connected adb device.
#   tools/build_release.sh              release APK
#   tools/build_release.sh debug        debug APK
#   tools/build_release.sh --install    release APK, then adb install -r
# The SDK comes from local.properties, else ANDROID_HOME / ANDROID_SDK_ROOT.
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_TYPE=release
INSTALL=0
for arg in "$@"; do
    case "$arg" in
        release|debug) BUILD_TYPE="$arg" ;;
        -i|--install) INSTALL=1 ;;
        *) echo "usage: $0 [release|debug] [--install]" >&2; exit 2 ;;
    esac
done

cd "$REPO_DIR"

if [ ! -f local.properties ]; then
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [ -z "$SDK" ]; then
        echo "No local.properties and ANDROID_HOME is unset; set one to your Android SDK path" >&2
        exit 1
    fi
    echo "sdk.dir=${SDK//\\//}" > local.properties
    echo "wrote local.properties (sdk.dir=$SDK)"
fi
SDK_DIR="$(sed -n 's/^sdk\.dir=//p' local.properties)"

if [ ! -f app/keystores/keystore.properties ] && [ "$BUILD_TYPE" = release ]; then
    echo "warning: app/keystores/keystore.properties missing, release will be debug-signed"
fi

tools/fetch_components.sh

TASK="assembleModern${BUILD_TYPE^}"
./gradlew "$TASK"

APK="app/build/outputs/apk/modern/$BUILD_TYPE/app-modern-$BUILD_TYPE.apk"
echo "built $APK"

if [ "$INSTALL" = 1 ]; then
    ADB="$(command -v adb || echo "$SDK_DIR/platform-tools/adb")"
    "$ADB" install -r "$APK"
fi
