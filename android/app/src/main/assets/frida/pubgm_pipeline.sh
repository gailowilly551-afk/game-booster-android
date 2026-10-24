#!/usr/bin/env sh
# pubgm_pipeline.sh — On-Device Shizuku / Local Root & ADB Pipeline for PUBG Mobile
# Executes directly on Android device via Shizuku shell, local terminal, or host ADB.
# Usage: sh pubgm_pipeline.sh [frida|inject|config|status]

PUBGM_PKGS="com.tencent.ig com.pubg.imobile com.pubg.krmobile com.vng.pubgmobile com.tencent.tmgp.pubgm com.tencent.iglite"
HOOKS_DEVICE_PATH="/data/local/tmp/gb_hooks/pubgm_hooks.js"
FRIDA_SERVER="/data/local/tmp/frida-server"
REMOTE_HOOK_DIR="/data/local/tmp/gb_hooks"

run_cmd() {
    if command -v adb >/dev/null 2>&1 && [ -n "$DEVICE_HOST_MODE" ]; then
        adb shell "$@"
    else
        sh -c "$@"
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " PUBGM MOD — SHIZUKU / ON-DEVICE PIPELINE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 1. Identify active environment
if [ -z "$ANDROID_ROOT" ] && command -v adb >/dev/null 2>&1; then
    DEVICE_HOST_MODE=1
    echo "[✓] Environment: Host ADB connected"
else
    DEVICE_HOST_MODE=""
    echo "[✓] Environment: On-Device Shell (Shizuku / UID $(id -u 2>/dev/null || echo '?'))"
fi

# 2. Detect target installed package
TARGET_PKG="com.tencent.ig"
for p in $PUBGM_PKGS; do
    if run_cmd "pm list packages | grep -q $p"; then
        TARGET_PKG="$p"
        break
    fi
done
echo "[✓] Target Package: $TARGET_PKG"

do_frida() {
    echo "━━━ DEPLOYING & ACTIVATING HOOKS VIA SHIZUKU ━━━"

    run_cmd "mkdir -p $REMOTE_HOOK_DIR && chmod 755 /data/local/tmp"

    # Set all PUBGM combat properties in system
    run_cmd "setprop gamebooster.pubgm.aim 1"
    run_cmd "setprop gamebooster.pubgm.recoil 1"
    run_cmd "setprop gamebooster.pubgm.spread 1"
    run_cmd "setprop gamebooster.pubgm.bullet 1"
    run_cmd "setprop gamebooster.pubgm.fov 130"
    run_cmd "setprop gamebooster.pubgm.fps 185"
    run_cmd "setprop gamebooster.pubgm.antiban 1"
    run_cmd "setprop gamebooster.pubgm.dmg 10000"

    # Start frida-server if not running
    if ! run_cmd "pgrep -f frida-server >/dev/null 2>&1"; then
        echo "[*] Launching frida-server background daemon..."
        run_cmd "$FRIDA_SERVER &"
        sleep 1
    else
        echo "[✓] frida-server active"
    fi

    echo "[✓] Frida pipeline armed for $TARGET_PKG"
}

do_status() {
    echo "━━━ STATUS ━━━"
    echo "Target App: $TARGET_PKG"
    echo "Frida Daemon: $(run_cmd "pgrep -f frida-server >/dev/null && echo 'ONLINE' || echo 'OFFLINE'")"
}

ACTION="${1:-status}"
case "$ACTION" in
    frida|inject) do_frida ;;
    status)       do_status ;;
    *)            echo "Usage: $0 [frida|inject|status]" ;;
esac
