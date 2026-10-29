#!/system/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# unlock_165fps_spoof.sh — 165 FPS Unlock & Device Profile Spoofer
# Targets:
#   - Unreal Engine 4 (PUBG Mobile: com.tencent.ig, com.pubg.krmobile, etc.)
#   - Unity / Proprietary (Call of Duty: Mobile: com.activision.callofduty.shooter, com.garena.game.codm)
#   - Unity / Kite (Mobile Legends: Bang Bang: com.mobile.legends)
# ─────────────────────────────────────────────────────────────────────────────

set -e

# --- Named Configuration Entries ---
TARGET_HZ="165"
TARGET_HZ_FLOAT="165.0"
UE4_FRAME_BUDGET="6.0606"

# Hardware Whitelist Spoof: ASUS ROG Phone 7 Ultimate (Snapdragon 8 Gen 2)
SPOOF_MANUFACTURER="asus"
SPOOF_BRAND="asus"
SPOOF_MODEL="ASUS_AI2205_D"
SPOOF_DEVICE="ASUS_AI2205"
SPOOF_PRODUCT="WW_AI2205"
SPOOF_BOARD="kalama"
SPOOF_HARDWARE="qcom"
SPOOF_FINGERPRINT="asus/WW_AI2205/ASUS_AI2205:13/TKQ1.220807.001/33.0820.0820.156:user/release-keys"

# Target Packages
PUBGM_PACKAGES="com.tencent.ig com.pubg.krmobile com.pubg.imobile com.vng.pubgmobile com.rekoo.pubgm"
CODM_PACKAGES="com.activision.callofduty.shooter com.garena.game.codm com.vng.codmvn"
MLBB_PACKAGES="com.mobile.legends"

log_info() {
    echo "[165-UNLOCK] $1"
}

# ─────────────────────────────────────────────────────────────────────────────
# 1. Device Profile Spoofing (System Properties)
# ─────────────────────────────────────────────────────────────────────────────
spoof_device_profile() {
    log_info "Injecting 165Hz-whitelisted device profile ($SPOOF_MODEL)..."
    
    PROP_TOOL="setprop"
    if command -v resetprop >/dev/null 2>&1; then
        PROP_TOOL="resetprop -n"
    fi

    $PROP_TOOL ro.product.manufacturer "$SPOOF_MANUFACTURER" 2>/dev/null || true
    $PROP_TOOL ro.product.brand "$SPOOF_BRAND" 2>/dev/null || true
    $PROP_TOOL ro.product.model "$SPOOF_MODEL" 2>/dev/null || true
    $PROP_TOOL ro.product.device "$SPOOF_DEVICE" 2>/dev/null || true
    $PROP_TOOL ro.product.name "$SPOOF_PRODUCT" 2>/dev/null || true
    $PROP_TOOL ro.product.board "$SPOOF_BOARD" 2>/dev/null || true
    $PROP_TOOL ro.hardware "$SPOOF_HARDWARE" 2>/dev/null || true
    $PROP_TOOL ro.build.product "$SPOOF_DEVICE" 2>/dev/null || true
    $PROP_TOOL ro.build.fingerprint "$SPOOF_FINGERPRINT" 2>/dev/null || true
}

# ─────────────────────────────────────────────────────────────────────────────
# 2. SurfaceFlinger & Display Pipeline Force (165Hz)
# ─────────────────────────────────────────────────────────────────────────────
force_display_pipeline() {
    log_info "Synchronizing Android SurfaceFlinger & display modes to ${TARGET_HZ}Hz..."

    # Settings provider refresh rate clamps
    settings put system peak_refresh_rate "$TARGET_HZ_FLOAT" 2>/dev/null || true
    settings put system min_refresh_rate "$TARGET_HZ_FLOAT" 2>/dev/null || true
    settings put secure user_refresh_rate "$TARGET_HZ" 2>/dev/null || true
    settings put global game_mode_config 0 2>/dev/null || true

    # SurfaceFlinger dynamic clamp override
    setprop debug.sf.fps_limit "$TARGET_HZ" 2>/dev/null || true
    setprop debug.sf.dynamic_fps 0 2>/dev/null || true
    setprop persist.sys.NV_FPSLIMIT "$TARGET_HZ" 2>/dev/null || true
    setprop persist.game_mode.performance.fps "$TARGET_HZ" 2>/dev/null || true
    setprop persist.sys.game.fps "$TARGET_HZ" 2>/dev/null || true

    # SurfaceFlinger direct binder transactions (1034 = set refresh rate, 1035 = override mode)
    service call SurfaceFlinger 1034 i32 "$TARGET_HZ" 2>/dev/null || true
    service call SurfaceFlinger 1035 i32 "$TARGET_HZ" 2>/dev/null || true

    # Kernel sysfs nodes
    for node in \
        /sys/devices/virtual/graphics/fb0/dynamic_fps \
        /sys/class/graphics/fb0/dynamic_fps \
        /sys/devices/platform/mtk_disp_mgr.0/refresh_rate \
        /proc/mtk_display/fps; do
        if [ -w "$node" ]; then
            echo "$TARGET_HZ" > "$node" 2>/dev/null || true
        fi
    done
}

# ─────────────────────────────────────────────────────────────────────────────
# 3. Unreal Engine 4 (PUBG Mobile) 165 FPS Patching
# ─────────────────────────────────────────────────────────────────────────────
patch_unreal_engine_pubgm() {
    for pkg in $PUBGM_PACKAGES; do
        BASE_DIR="/storage/emulated/0/Android/data/${pkg}/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra"
        if [ ! -d "/storage/emulated/0/Android/data/${pkg}" ]; then
            continue
        fi

        log_info "Applying Unreal Engine 165 FPS patch to ${pkg}..."
        am force-stop "$pkg" 2>/dev/null || true

        # Clean corrupted authentication locks (prevent infinite loading bug)
        rm -f "${BASE_DIR}/Saved/SaveGames/loginInfoFile.json" 2>/dev/null || true
        rm -f "${BASE_DIR}/Saved/*.tmp*" 2>/dev/null || true
        rm -f "${BASE_DIR}/Saved/*lock*" 2>/dev/null || true

        # Target Config Directories
        CFG_DIR="${BASE_DIR}/Saved/Config/Android"
        mkdir -p "$CFG_DIR" 2>/dev/null || true

        # 1. UserCustom.ini: Inject CVar Engine Overrides into [UserCustom DeviceProfile]
        USER_CUSTOM="${CFG_DIR}/UserCustom.ini"
        if [ -f "$USER_CUSTOM" ]; then
            sed -i '/t.MaxFPS/d' "$USER_CUSTOM" 2>/dev/null || true
            sed -i '/r.PUBGDeviceFPSPolicy/d' "$USER_CUSTOM" 2>/dev/null || true
            sed -i '/r.PUBGDeviceFPSSupport165/d' "$USER_CUSTOM" 2>/dev/null || true
            sed -i '/r.PUBGMaxFPS/d' "$USER_CUSTOM" 2>/dev/null || true
            sed -i '/r.MaxFPS/d' "$USER_CUSTOM" 2>/dev/null || true
            sed -i '/r.VSync/d' "$USER_CUSTOM" 2>/dev/null || true
            sed -i '/Unlock165Hz/d' "$USER_CUSTOM" 2>/dev/null || true
        fi

        cat <<EOF >> "$USER_CUSTOM"

[UserCustom DeviceProfile]
+CVars=r.PUBGDeviceFPSPolicy=2
+CVars=r.PUBGDeviceFPSSupport165=1
+CVars=r.PUBGMaxFPS=${TARGET_HZ}
+CVars=r.MaxFPS=${TARGET_HZ}
+CVars=t.MaxFPS=${TARGET_HZ}
+CVars=r.Unlock165Hz=1
+CVars=r.VSync=0
+CVars=r.DynamicRes.FrameTimeBudget=${UE4_FRAME_BUDGET}
EOF

        # 2. GameUserSettings.ini: Unlock frame rate limit and levels
        GUS="${CFG_DIR}/GameUserSettings.ini"
        if [ -f "$GUS" ]; then
            sed -i "s/FrameRateLimit=[0-9.]*/FrameRateLimit=${TARGET_HZ}.000000/g" "$GUS" 2>/dev/null || true
            sed -i "s/FrameRateLevel=[0-9]*/FrameRateLevel=9/g" "$GUS" 2>/dev/null || true
            sed -i "s/BattleFPS=[0-9]*/BattleFPS=9/g" "$GUS" 2>/dev/null || true
            sed -i "s/LobbyFPS=[0-9]*/LobbyFPS=9/g" "$GUS" 2>/dev/null || true
        fi

        # 3. Active.sav GVAS Binary Patch (Enforce Level 9 = 165 FPS)
        for sav in "${BASE_DIR}/Saved/SaveGames/Active.sav" "${BASE_DIR}/Saved/SaveGames/ActiveShadow.sav"; do
            if [ -f "$sav" ]; then
                if command -v python3 >/dev/null 2>&1; then
                    python3 -c "
import sys
sav_path = '$sav'
fields = [b'FPSLevel', b'BattleFPS', b'LobbyFPS', b'BattleFPSLevel', b'LobbyFPSLevel', b'HighFPSMode']
prop_type = b'IntProperty\x00'
target_val = 9

with open(sav_path, 'rb') as f:
    data = bytearray(f.read())

modified = False
for fld in fields:
    idx = 0
    while True:
        idx = data.find(fld, idx)
        if idx == -1: break
        prop_idx = data.find(prop_type, idx + len(fld))
        if prop_idx != -1 and (prop_idx - idx) <= 48:
            val_offset = prop_idx + len(prop_type) + 9
            if val_offset + 3 < len(data):
                val = 1 if fld == b'HighFPSMode' else target_val
                data[val_offset:val_offset+4] = val.to_bytes(4, 'little')
                modified = True
        idx += len(fld)

if modified:
    with open(sav_path, 'wb') as f:
        f.write(data)
" 2>/dev/null || true
                else
                    # Pure POSIX shell binary patching for Android devices without python3
                    for fld in "FPSLevel" "BattleFPS" "LobbyFPS" "BattleFPSLevel" "LobbyFPSLevel"; do
                        for off in $(strings -t d "$sav" 2>/dev/null | grep "$fld" | awk '{print $1}'); do
                            val_off=$((off + ${#fld} + 1 + 4 + 12 + 9))
                            printf '\x09\x00\x00\x00' | dd of="$sav" bs=1 seek="$val_off" count=4 conv=notrunc 2>/dev/null || true
                        done
                    done
                    for off in $(strings -t d "$sav" 2>/dev/null | grep "HighFPSMode" | awk '{print $1}'); do
                        val_off=$((off + 11 + 1 + 4 + 12 + 9))
                        printf '\x01\x00\x00\x00' | dd of="$sav" bs=1 seek="$val_off" count=4 conv=notrunc 2>/dev/null || true
                    done
                fi
            fi
        done

        chmod -R 777 "/storage/emulated/0/Android/data/${pkg}/files" 2>/dev/null || true
    done
}

# ─────────────────────────────────────────────────────────────────────────────
# 4. Call of Duty: Mobile (CODM) 165 FPS Patching
# ─────────────────────────────────────────────────────────────────────────────
patch_codm() {
    for pkg in $CODM_PACKAGES; do
        DATA_ROOT="/storage/emulated/0/Android/data/${pkg}/files"
        APP_ROOT="/data/data/${pkg}/files"

        if [ ! -d "/storage/emulated/0/Android/data/${pkg}" ] && [ ! -d "/data/data/${pkg}" ]; then
            continue
        fi

        log_info "Applying CODM 165 FPS patch to ${pkg}..."
        am force-stop "$pkg" 2>/dev/null || true

        for root in "$DATA_ROOT" "$APP_ROOT"; do
            [ -d "$root" ] || continue

            # boot.config: Unity engine frame cadence
            BOOT_CFG="${root}/boot.config"
            if [ -f "$BOOT_CFG" ]; then
                sed -i '/target-frame-rate/d' "$BOOT_CFG" 2>/dev/null || true
                echo "target-frame-rate=${TARGET_HZ}" >> "$BOOT_CFG"
                echo "wait-for-native-debugger=0" >> "$BOOT_CFG"
            fi

            # GraphicsSettings.ini & UserSetting.json
            GFX_INI="${root}/Config/GraphicsSettings.ini"
            if [ -f "$GFX_INI" ]; then
                sed -i "s/MaxFrameRate=[0-9]*/MaxFrameRate=${TARGET_HZ}/g" "$GFX_INI" 2>/dev/null || true
                sed -i "s/TargetFPS=[0-9]*/TargetFPS=${TARGET_HZ}/g" "$GFX_INI" 2>/dev/null || true
                sed -i "s/t.MaxFPS=[0-9]*/t.MaxFPS=${TARGET_HZ}/g" "$GFX_INI" 2>/dev/null || true
            fi
        done
    done
}

# ─────────────────────────────────────────────────────────────────────────────
# 5. Mobile Legends: Bang Bang (MLBB) 165 FPS Patching
# ─────────────────────────────────────────────────────────────────────────────
patch_mlbb() {
    for pkg in $MLBB_PACKAGES; do
        PREFS_FILE="/data/data/${pkg}/shared_prefs/${pkg}.v2.playerprefs.xml"
        DATA_PREFS="/storage/emulated/0/Android/data/${pkg}/files/shared_prefs/${pkg}.v2.playerprefs.xml"

        if [ ! -d "/storage/emulated/0/Android/data/${pkg}" ] && [ ! -d "/data/data/${pkg}" ]; then
            continue
        fi

        log_info "Applying MLBB 165 FPS patch to ${pkg}..."
        am force-stop "$pkg" 2>/dev/null || true

        for pref in "$PREFS_FILE" "$DATA_PREFS"; do
            if [ -f "$pref" ]; then
                sed -i 's/<int name="HighFpsMode" value="[0-9]*" \/>/<int name="HighFpsMode" value="1" \/>/g' "$pref" 2>/dev/null || true
                sed -i "s/<int name=\"FPS\" value=\"[0-9]*\" \\/>/<int name=\"FPS\" value=\"${TARGET_HZ}\" \\/>/g" "$pref" 2>/dev/null || true
                sed -i 's/<int name="HighFps" value="[0-9]*" \/>/<int name="HighFps" value="1" \/>/g' "$pref" 2>/dev/null || true
                sed -i "s/<int name=\"RefreshRate\" value=\"[0-9]*\" \\/>/<int name=\"RefreshRate\" value=\"${TARGET_HZ}\" \\/>/g" "$pref" 2>/dev/null || true
            fi
        done
    done
}

# ─────────────────────────────────────────────────────────────────────────────
# Main Execution Flow
# ─────────────────────────────────────────────────────────────────────────────
main() {
    log_info "Initiating Full 165 FPS Unlock & Device Spoof Sequence..."
    spoof_device_profile
    force_display_pipeline
    patch_unreal_engine_pubgm
    patch_codm
    patch_mlbb
    log_info "165 FPS Unlock & Device Spoofing Completed Successfully."
}

main "$@"
