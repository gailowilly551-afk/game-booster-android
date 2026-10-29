#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# deploy_device.sh — One-Touch ADB Deploy & Verification Script
# Target: Connected Android device (USB or Wireless Debugging)
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

APK_PATH="android/app/build/outputs/apk/debug/Game_Space_Debug.apk"
SPOOF_SCRIPT="unlock_165fps_spoof.sh"
PKG_NAME="com.gamebooster.app"

log_info() {
    echo "▶ [DEPLOY] $1"
}

log_ok() {
    echo "✅ [SUCCESS] $1"
}

# 1. Device Connection Check
log_info "Scanning for connected ADB devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}' || true)

if [ -z "$DEVICES" ]; then
    echo "⚠️  No active ADB device detected."
    echo "   Ensure USB debugging is enabled, or connect via Wi-Fi:"
    echo "   $ adb connect <device_ip>:<port>"
    exit 0
fi

DEVICE_ID=$(echo "$DEVICES" | head -n 1)
log_ok "Target device identified: $DEVICE_ID"

# 2. Query Display Capabilities
log_info "Querying hardware display modes on $DEVICE_ID..."
MODES=$(adb -s "$DEVICE_ID" shell dumpsys display | grep -E "mBaseDisplayInfo|refreshRate|supportedModes" | head -n 10 || true)
echo "$MODES"

# 3. Install Game Launcher APK
if [ -f "$APK_PATH" ]; then
    log_info "Installing $APK_PATH on $DEVICE_ID..."
    adb -s "$DEVICE_ID" install -r -d -g "$APK_PATH"
    log_ok "Game Launcher APK successfully installed."
else
    echo "❌ APK not found at $APK_PATH. Run ./auto_execute_pipeline.sh first."
    exit 1
fi

# 4. Grant Advanced Privileges
log_info "Granting WRITE_SECURE_SETTINGS & SYSTEM_ALERT_WINDOW permissions..."
adb -s "$DEVICE_ID" shell pm grant "$PKG_NAME" android.permission.WRITE_SECURE_SETTINGS 2>/dev/null || true
adb -s "$DEVICE_ID" shell appops set "$PKG_NAME" SYSTEM_ALERT_WINDOW allow 2>/dev/null || true
adb -s "$DEVICE_ID" shell appops set "$PKG_NAME" PACKAGE_USAGE_STATS allow 2>/dev/null || true

# 5. Push 165 FPS Spoofer
if [ -f "$SPOOF_SCRIPT" ]; then
    log_info "Pushing 165 FPS spoofing engine to device..."
    adb -s "$DEVICE_ID" push "$SPOOF_SCRIPT" /data/local/tmp/unlock_165fps_spoof.sh
    adb -s "$DEVICE_ID" shell chmod +x /data/local/tmp/unlock_165fps_spoof.sh
    log_ok "Spoofer staged at /data/local/tmp/unlock_165fps_spoof.sh"
fi

log_ok "Deployment complete. Launching Game Launcher..."
adb -s "$DEVICE_ID" shell monkey -p "$PKG_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
log_ok "Application launched on target device."
