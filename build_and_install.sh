#!/usr/bin/env bash
# Builds the app, installs it on the Android device this chroot runs on, and launches it.
#   ./build_and_install.sh            release APK
#   ./build_and_install.sh debug      debug APK
# Installs through Android's pm as root (via init's mount namespace), so no adb
# is needed. Requires passwordless sudo.
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_TYPE="${1:-release}"
case "$BUILD_TYPE" in
    release|debug) ;;
    *) echo "usage: $0 [release|debug]" >&2; exit 2 ;;
esac

cd "$REPO_DIR"

# shellcheck source=/dev/null
source "$HOME/android-dev/env.sh"

if [ ! -f local.properties ]; then
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    echo "wrote local.properties (sdk.dir=$ANDROID_HOME)"
fi

# Gradle's bundled aapt2 is x86-only; use the ARM build from the SDK.
AAPT2="$ANDROID_HOME/build-tools/36.0.0/aapt2"

bash tools/fetch_components.sh
./gradlew "assembleModern${BUILD_TYPE^}" -Pandroid.aapt2FromMavenOverride="$AAPT2"

APK="app/build/outputs/apk/modern/$BUILD_TYPE/app-modern-$BUILD_TYPE.apk"
echo "built $APK"

# Runs a command in Android's own environment (pm, am, ...), as root.
android_sh() {
    sudo -n nsenter -t 1 -m -- /system/bin/sh -c "
        export \$(tr '\0' '\n' < /proc/1/environ | grep -E '^(ANDROID_|BOOTCLASSPATH|DEX2OAT|SYSTEMSERVER|PATH)' | xargs)
        $1
    "
}

SIZE="$(stat -c %s "$APK")"
android_sh "pm install -r -S $SIZE" < "$APK"

# monkey only echoes its arguments in this setup, so start the launcher activity directly.
android_sh "am force-stop app.wowforever; am start -W -n app.wowforever/app.gamenative.MainActivityAliasDefault"
