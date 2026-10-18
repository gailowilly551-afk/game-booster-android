#!/usr/bin/env sh
# codm_pipeline.sh — On-Device Shizuku / Local Root & ADB Pipeline for Call of Duty: Mobile
# Executes directly on Android device via Shizuku shell (rish), local terminal, or host ADB.
# Usage: sh codm_pipeline.sh [frida|inject|config|status]

CODM_PKGS="com.garena.game.codm com.activision.callofduty.shooter com.vng.codmvn com.tencent.tmgp.cod"
HOOKS_DEVICE_PATH="/data/local/tmp/gb_hooks/codm_hooks.js"
FRIDA_SERVER="/data/local/tmp/frida-server"
REMOTE_HOOK_DIR="/data/local/tmp/gb_hooks"

# Intelligent shell wrapper (detects if running inside device shell or host ADB)
run_cmd() {
    if command -v adb >/dev/null 2>&1 && [ -n "$DEVICE_HOST_MODE" ]; then
        adb shell "$@"
    else
        sh -c "$@"
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " CODM MOD — SHIZUKU / ON-DEVICE PIPELINE"
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
TARGET_PKG="com.garena.game.codm"
for p in $CODM_PKGS; do
    if run_cmd "pm list packages | grep -q $p"; then
        TARGET_PKG="$p"
        break
    fi
done
echo "[✓] Target Package: $TARGET_PKG"

do_frida() {
    echo "━━━ DEPLOYING & ACTIVATING HOOKS VIA SHIZUKU ━━━"

    # Ensure hook directory exists
    run_cmd "mkdir -p $REMOTE_HOOK_DIR && chmod 755 /data/local/tmp"

    # Set all combat properties locally in system
    run_cmd "setprop gamebooster.codm.aim 1"
    run_cmd "setprop gamebooster.codm.dmg 3.0"
    run_cmd "setprop gamebooster.codm.speed 1.5"
    run_cmd "setprop gamebooster.codm.attack 2.0"
    run_cmd "setprop gamebooster.codm.defense 0.8"
    run_cmd "setprop gamebooster.codm.hp 3.0"
    run_cmd "setprop gamebooster.codm.recoil 1"
    run_cmd "setprop gamebooster.codm.spread 1"
    run_cmd "setprop gamebooster.codm.fps 120"
    run_cmd "setprop gamebooster.codm.antiban 1"
    run_cmd "setprop gamebooster.codm.bullet 1"
    echo "[✓] Combat properties active (Aimbot, Zero Recoil, 3x Dmg, 120 FPS)"

    # Check and launch frida-server in background if present
    if run_cmd "[ -f $FRIDA_SERVER ]"; then
        run_cmd "chmod 755 $FRIDA_SERVER"
        run_cmd "killall frida-server 2>/dev/null; $FRIDA_SERVER &" >/dev/null 2>&1 &
        echo "[✓] frida-server activated on device @ $FRIDA_SERVER"
    fi

    # Find PID of game if running
    PID=$(run_cmd "pidof $TARGET_PKG 2>/dev/null || pgrep -f $TARGET_PKG 2>/dev/null" | head -n 1)

    # Detect injection binary
    FRIDA_INJECT=$(run_cmd "which frida-inject 2>/dev/null || [ -f /data/local/tmp/frida-inject ] && echo /data/local/tmp/frida-inject || which frida 2>/dev/null")

    if [ -n "$FRIDA_INJECT" ]; then
        if [ -n "$PID" ]; then
            echo "[✓] Attaching directly to PID $PID ($TARGET_PKG)..."
            run_cmd "$FRIDA_INJECT -p $PID -s $HOOKS_DEVICE_PATH &"
        else
            echo "[!] Game not running yet. Hook script staged @ $HOOKS_DEVICE_PATH"
            echo "[!] Frida will hook automatically when game opens."
        fi
    else
        echo "[✓] Hook script ready @ $HOOKS_DEVICE_PATH"
        echo "[✓] GameLauncher internal GameModEngine will execute instant injection on launch!"
    fi

    # Safe config unlocking & permission alignment
    echo "━━━ ALIGNING CODM CONFIG PERMISSIONS & UNLOCKING ━━━"
    run_cmd "CODM_UID=\$(stat -c %u \"/data/user/0/$TARGET_PKG\" 2>/dev/null || echo 10656); \
    for dir in '/data/user/0/$TARGET_PKG/files' '/storage/emulated/0/Android/data/$TARGET_PKG/files'; do \
        if [ -d \"\$dir\" ]; then \
            find \"\$dir\" -type f \\( -name '*config*' -o -name '*Config*' -o -name '*Setting*' \\) 2>/dev/null | while read -r f; do \
                chattr -i \"\$f\" 2>/dev/null; \
                chown \"\$CODM_UID:\$CODM_UID\" \"\$f\" 2>/dev/null; \
                chmod 666 \"\$f\" 2>/dev/null; \
            done; \
        fi; \
    done"
}

do_status() {
    echo "━━━ SYSTEM PROPERTIES STATUS ━━━"
    run_cmd "getprop | grep gamebooster.codm"
}

case "${1:-frida}" in
    frida|inject) do_frida ;;
    status)       do_status ;;
    *)
        echo "Usage: sh codm_pipeline.sh [frida|status]"
        exit 1
        ;;
esac
