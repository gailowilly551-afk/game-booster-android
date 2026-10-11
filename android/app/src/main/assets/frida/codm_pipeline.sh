#!/usr/bin/env bash
# codm_pipeline.sh — USB ADB test & live injection pipeline for Call of Duty: Mobile
# Run on Linux laptop with Android device connected via USB
# Usage: bash codm_pipeline.sh [frida|inject|config|status]

set -e

CODM_PKGS=("com.garena.game.codm" "com.activision.callofduty.shooter")
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
FRIDA_SERVER_SRC="$SCRIPT_DIR/frida-server-arm64"
HOOKS_SRC="$SCRIPT_DIR/codm_hooks.js"
REMOTE_HOOK_DIR="/data/local/tmp/gb_hooks"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
warn() { echo -e "${YELLOW}[!]${NC} $*"; }
err()  { echo -e "${RED}[✗]${NC} $*"; exit 1; }
info() { echo -e "    $*"; }

check_device() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo " CODM MOD — USB ADB PIPELINE"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    if ! command -v adb &>/dev/null; then
        err "adb not found. Install: sudo apt install adb"
    fi
    DEVICE=$(adb devices | grep -v "List of" | grep "device$" | awk '{print $1}' | head -1)
    if [ -z "$DEVICE" ]; then
        err "No device found. Check USB cable and allow USB Debugging on phone."
    fi
    ok "Device: $DEVICE"
    ANDROID_VER=$(adb shell getprop ro.build.version.release | tr -d '\r')
    PHONE_MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
    ok "Model: $PHONE_MODEL  Android: $ANDROID_VER"
    echo ""
}

detect_installed_codm() {
    for pkg in "${CODM_PKGS[@]}"; do
        if adb shell pm list packages | grep -q "$pkg"; then
            echo "$pkg"
            return 0
        fi
    done
    echo "${CODM_PKGS[0]}"
}

do_frida() {
    echo "━━━ CODM FRIDA RUNTIME HOOKS ━━━"
    TARGET_PKG=$(detect_installed_codm)
    info "Target package detected: $TARGET_PKG"

    adb shell "mkdir -p $REMOTE_HOOK_DIR"
    adb shell "chmod 755 /data/local/tmp"

    if [ -f "$FRIDA_SERVER_SRC" ]; then
        SERVER_SIZE=$(adb shell stat -c %s /data/local/tmp/frida-server 2>/dev/null | tr -d '\r' || echo 0)
        if [ "$SERVER_SIZE" -lt 40000000 ]; then
            warn "Deploying frida-server-arm64 to /data/local/tmp/frida-server..."
            adb push "$FRIDA_SERVER_SRC" /data/local/tmp/frida-server
            adb shell chmod 755 /data/local/tmp/frida-server
            ok "frida-server deployed"
        else
            ok "frida-server already installed on device"
        fi
    fi

    info "Starting frida-server in background..."
    adb shell "killall frida-server 2>/dev/null; /data/local/tmp/frida-server &" &
    sleep 1

    if [ -f "$HOOKS_SRC" ]; then
        info "Pushing codm_hooks.js..."
        adb push "$HOOKS_SRC" "$REMOTE_HOOK_DIR/codm_hooks.js"
        adb shell chmod 666 "$REMOTE_HOOK_DIR/codm_hooks.js"
        ok "Hook script deployed to $REMOTE_HOOK_DIR/codm_hooks.js"
    fi

    # Set system properties for Frida engine configuration
    adb shell "setprop gamebooster.codm.aim 1"
    adb shell "setprop gamebooster.codm.dmg 3.0"
    adb shell "setprop gamebooster.codm.speed 1.5"
    adb shell "setprop gamebooster.codm.attack 2.0"
    adb shell "setprop gamebooster.codm.defense 0.8"
    adb shell "setprop gamebooster.codm.hp 3.0"
    adb shell "setprop gamebooster.codm.recoil 1"
    adb shell "setprop gamebooster.codm.spread 1"
    adb shell "setprop gamebooster.codm.fps 120"
    adb shell "setprop gamebooster.codm.antiban 1"
    ok "CODM SystemProperties configured for Master Combat Matrix"

    if command -v frida &>/dev/null; then
        ok "Launching Frida hook session directly into $TARGET_PKG..."
        frida -U -f "$TARGET_PKG" -l "$HOOKS_SRC" --no-pause
    else
        warn "frida CLI not installed on host machine. (Run: pip install frida-tools)"
        info "frida-server is running on device @ /data/local/tmp/frida-server"
        info "Script ready at: $REMOTE_HOOK_DIR/codm_hooks.js"
    fi
}

do_status() {
    TARGET_PKG=$(detect_installed_codm)
    echo "━━━ STATUS FOR $TARGET_PKG ━━━"
    adb shell "getprop | grep gamebooster.codm"
}

check_device

case "${1:-frida}" in
    frida|inject) do_frida ;;
    status)       do_status ;;
    *)
        echo "Usage: bash codm_pipeline.sh [frida|status]"
        exit 1
        ;;
esac
