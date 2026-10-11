#!/usr/bin/env bash
# mlbb_pipeline.sh — Full USB ADB pull → patch → push pipeline
# Run on Linux laptop with Android device connected via USB
# Usage: bash mlbb_pipeline.sh [pull|patch|push|install|logs|revert|all]

set -e

MLBB_PKG="com.mobile.legends"
MOD_PKG="com.mlbbmod.mlbb_mod"
OBB_DIR="$HOME/mlbb_patch/obb"
BACKUP_DIR="$HOME/mlbb_patch/original_backup"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK_PATH="$HOME/Downloads/IOS OS/mlbb_mod/build/app/outputs/flutter-apk/app-debug.apk"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[✓]${NC} $*"; }
warn() { echo -e "${YELLOW}[!]${NC} $*"; }
err()  { echo -e "${RED}[✗]${NC} $*"; exit 1; }
info() { echo -e "    $*"; }

# ─── CHECK ADB ─────────────────────────────────────────────────────

check_device() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo " MLBB MOD — USB ADB PIPELINE"
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

# ─── PHASE 1: PULL ─────────────────────────────────────────────────

do_pull() {
    echo "━━━ PHASE 1: PULLING FILES ━━━"
    mkdir -p "$OBB_DIR" "$BACKUP_DIR"

    # OBB files
    OBB_REMOTE="/sdcard/Android/obb/$MLBB_PKG"
    OBB_LIST=$(adb shell ls "$OBB_REMOTE/" 2>/dev/null | tr -d '\r')
    if [ -z "$OBB_LIST" ]; then
        warn "OBB dir empty or not accessible: $OBB_REMOTE"
        warn "Make sure MLBB is installed and has been launched at least once."
    else
        echo "  OBB files found:"
        echo "$OBB_LIST" | while read -r f; do
            SIZE=$(adb shell stat -c %s "$OBB_REMOTE/$f" 2>/dev/null | tr -d '\r')
            info "$f  (${SIZE} bytes)"
        done
        echo ""
        warn "Pulling OBB (may take 5-10 min for large files)..."
        adb pull "$OBB_REMOTE/" "$OBB_DIR/"
        ok "OBB pulled → $OBB_DIR"

        # Backup
        if [ ! "$(ls -A "$BACKUP_DIR" 2>/dev/null)" ]; then
            cp -r "$OBB_DIR/." "$BACKUP_DIR/"
            ok "Backup created → $BACKUP_DIR"
        else
            ok "Backup already exists — skipping"
        fi
    fi

    # Pull APK for libmoba.so extraction
    echo ""
    info "Pulling APK (for libmoba.so extraction)..."
    APK_REMOTE=$(adb shell pm path "$MLBB_PKG" 2>/dev/null | head -1 | cut -d: -f2 | tr -d '\r ')
    if [ -n "$APK_REMOTE" ]; then
        mkdir -p "$HOME/mlbb_patch/apk"
        adb pull "$APK_REMOTE" "$HOME/mlbb_patch/apk/base.apk" 2>/dev/null && \
            ok "APK pulled → ~/mlbb_patch/apk/base.apk" || \
            warn "APK pull failed (may need root)"
    fi

    echo ""
    ok "PULL COMPLETE"
    info "Next: bash mlbb_pipeline.sh patch"
}

# ─── PHASE 2: PATCH ────────────────────────────────────────────────

do_patch() {
    echo "━━━ PHASE 2: PATCHING OBB ━━━"
    if [ ! -d "$OBB_DIR" ]; then
        err "OBB dir not found. Run 'pull' first: bash mlbb_pipeline.sh pull"
    fi
    python3 "$SCRIPT_DIR/patch_obb.py" --mode apply
    echo ""
    ok "PATCH COMPLETE"
    info "Next: bash mlbb_pipeline.sh push"
}

# ─── PHASE 3: PUSH ─────────────────────────────────────────────────

do_push() {
    echo "━━━ PHASE 3: PUSHING FILES ━━━"
    OBB_REMOTE="/sdcard/Android/obb/$MLBB_PKG"

    for obb in "$OBB_DIR"/*.obb; do
        [ -f "$obb" ] || continue
        FNAME=$(basename "$obb")
        warn "Pushing $FNAME (may take several minutes)..."
        adb push "$obb" "$OBB_REMOTE/$FNAME"
        ok "Pushed: $FNAME"
    done

    # Push bypass assets to accessible sdcard location
    BYPASS_SRC="$HOME/Downloads/IOS OS/mlbb_mod/assets/bypass"
    if [ -d "$BYPASS_SRC" ]; then
        BYPASS_REMOTE="/sdcard/Android/data/$MLBB_PKG/files/mlbbmod_bypass"
        adb shell mkdir -p "$BYPASS_REMOTE" 2>/dev/null || true
        adb push "$BYPASS_SRC/." "$BYPASS_REMOTE/" 2>/dev/null && \
            ok "Bypass assets pushed → $BYPASS_REMOTE" || \
            warn "Bypass push failed (Android 13+ restricts /sdcard/Android/data)"
    fi

    echo ""
    ok "PUSH COMPLETE"
    info "Next: bash mlbb_pipeline.sh install"
}

# ─── PHASE 4: INSTALL MOD APP ──────────────────────────────────────

do_install() {
    echo "━━━ PHASE 4: INSTALLING MOD APK ━━━"
    if [ ! -f "$APK_PATH" ]; then
        err "APK not found: $APK_PATH\nBuild first: cd mlbb_mod && flutter build apk --debug"
    fi

    adb install -r "$APK_PATH"
    ok "Mod app installed"

    # Grant permissions
    adb shell pm grant "$MOD_PKG" android.permission.MANAGE_EXTERNAL_STORAGE 2>/dev/null || true
    adb shell pm grant "$MOD_PKG" android.permission.READ_EXTERNAL_STORAGE   2>/dev/null || true
    ok "Permissions granted"

    # Launch mod app
    adb shell am start -n "$MOD_PKG/.MainActivity"
    ok "Mod app launched"

    echo ""
    info "1. Open MLBB first → wait for title screen"
    info "2. Switch to mod app → [ DIAG ] tab → RE-RUN DIAGNOSTICS"
    info "3. [ FEATURES ] tab → toggle ON"
    echo ""
    ok "INSTALL COMPLETE"
}

# ─── PHASE 5: LOGS ─────────────────────────────────────────────────

do_logs() {
    echo "━━━ LIVE LOGS ━━━"
    echo "  (Ctrl+C to stop)"
    echo ""
    adb logcat -s MLBBMemBridge:D MLBBMain:D ShizukuBridge:D LibPatcher:D "*:S"
}

# ─── PHASE 6: REVERT ───────────────────────────────────────────────

do_revert() {
    echo "━━━ REVERTING — RESTORE ORIGINAL OBB ━━━"
    if [ ! "$(ls -A "$BACKUP_DIR" 2>/dev/null)" ]; then
        err "No backup found at $BACKUP_DIR"
    fi
    OBB_REMOTE="/sdcard/Android/obb/$MLBB_PKG"
    for obb in "$BACKUP_DIR"/*.obb; do
        [ -f "$obb" ] || continue
        FNAME=$(basename "$obb")
        warn "Restoring $FNAME..."
        adb push "$obb" "$OBB_REMOTE/$FNAME"
        ok "Restored: $FNAME"
    done
    ok "REVERT COMPLETE — original OBB restored"
}

# ─── PHASE 7: FRIDA HOOK RUNTIME ──────────────────────────────────

do_frida() {
    echo "━━━ FRIDA RUNTIME HOOKS ━━━"
    FRIDA_SERVER_SRC="$SCRIPT_DIR/../../frida/frida-server-arm64"
    HOOKS_SRC="$SCRIPT_DIR/../../frida/mlbb_hooks.js"

    info "1. Checking frida-server on device..."
    adb shell "mkdir -p /data/local/tmp/gb_hooks"
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
        info "Pushing mlbb_hooks.js..."
        adb push "$HOOKS_SRC" /data/local/tmp/gb_hooks/mlbb_hooks.js
        adb shell chmod 666 /data/local/tmp/gb_hooks/mlbb_hooks.js
        ok "Hook script deployed to /data/local/tmp/gb_hooks/mlbb_hooks.js"
    fi

    if command -v frida &>/dev/null; then
        ok "Launching Frida hook session directly into $MLBB_PKG..."
        frida -U -f "$MLBB_PKG" -l "$HOOKS_SRC" --no-pause
    else
        warn "frida CLI not installed on host machine. (Run: pip install frida-tools)"
        info "frida-server is running on device @ /data/local/tmp/frida-server"
        info "Script ready at: /data/local/tmp/gb_hooks/mlbb_hooks.js"
    fi
}

# ─── DISPATCH ──────────────────────────────────────────────────────

check_device

case "${1:-all}" in
    pull)    do_pull ;;
    patch)   do_patch ;;
    push)    do_push ;;
    install) do_install ;;
    frida)   do_frida ;;
    logs)    do_logs ;;
    revert)  do_revert ;;
    all)
        do_pull
        echo ""
        do_patch
        echo ""
        do_push
        echo ""
        do_install
        ;;
    *)
        echo "Usage: bash mlbb_pipeline.sh [pull|patch|push|install|frida|logs|revert|all]"
        exit 1
        ;;
esac
