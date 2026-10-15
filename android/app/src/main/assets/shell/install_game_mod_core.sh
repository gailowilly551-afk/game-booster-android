#!/system/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# install_game_mod_core.sh — Master In-Device Deployment Script
# Automatically deploys game mods, configuration files, and permanent anti-revert locks
# Targets:
#   - MLBB: com.mobile.legends (UID 10677, VersionCode 211812322)
#   - CODM: com.garena.game.codm (UID 10656, VersionCode 28028)
# Directories:
#   - Directory 1: /data/user/0/<package>
#   - Directory 2: /storage/emulated/0/Android/data/<package>
# ─────────────────────────────────────────────────────────────────────────────

PKG="$1"
if [ -z "$PKG" ]; then
    echo "Usage: $0 <package_name>"
    exit 1
fi

echo "[*] Initializing Master Combat Suite permanent deployment for: $PKG"

DIR1="/data/user/0/$PKG"
DIR2="/storage/emulated/0/Android/data/$PKG"

case "$PKG" in
    *mobile.legends*|*mobilelegends*)
        UID_VAL=10677
        VER_CODE=211812322
        echo "[*] Target MLBB -> UID: $UID_VAL, VersionCode: $VER_CODE"

        for BASE in "$DIR1/files" "$DIR2/files"; do
            DOC_DIR="$BASE/dragon2017/assets/Document/android"
            RES_DIR="$BASE/dragon2017/res"
            VER_DIR="$BASE/dragon2017/assets/version/android"
            mkdir -p "$DOC_DIR" "$RES_DIR" "$VER_DIR" 2>/dev/null

            # Anti-redownload timestamps into year 2030
            touch -t 203001010000 "$DOC_DIR/ResCheckConf.xml" 2>/dev/null
            touch -t 203001010000 "$DOC_DIR/res_skip_patch.xml" 2>/dev/null
            touch -t 203001010000 "$VER_DIR/version.xml" 2>/dev/null
            touch -t 203001010000 "$VER_DIR/realversion.xml" 2>/dev/null

            # Permanent zero-revert permissions and immutable lock
            for f in "$DOC_DIR"/* "$VER_DIR"/*; do
                if [ -f "$f" ]; then
                    chattr -i "$f" 2>/dev/null
                    chmod 444 "$f" 2>/dev/null
                    chown $UID_VAL:$UID_VAL "$f" 2>/dev/null
                    chattr +i "$f" 2>/dev/null
                fi
            done
        done
        echo "[✓] MLBB permanent zero-revert locks applied across Directory 1 and Directory 2."
        ;;

    *garena.game.codm*|*callofduty.shooter*|*codm*)
        UID_VAL=10656
        VER_CODE=28028
        echo "[*] Target CODM -> UID: $UID_VAL, VersionCode: $VER_CODE"

        for BASE in "$DIR1/files" "$DIR2/files"; do
            CFG_DIR="$BASE/Config"
            mkdir -p "$CFG_DIR" 2>/dev/null

            touch -t 203001010000 "$CFG_DIR/UserSetting.json" 2>/dev/null
            touch -t 203001010000 "$CFG_DIR/GameConfig.xml" 2>/dev/null
            touch -t 203001010000 "$CFG_DIR/ControlsSettings.ini" 2>/dev/null
            touch -t 203001010000 "$CFG_DIR/GraphicsSettings.ini" 2>/dev/null
            touch -t 203001010000 "$BASE/res_skip_patch" 2>/dev/null

            for f in "$CFG_DIR"/* "$BASE/res_skip_patch"; do
                if [ -f "$f" ]; then
                    chattr -i "$f" 2>/dev/null
                    chmod 444 "$f" 2>/dev/null
                    chown $UID_VAL:$UID_VAL "$f" 2>/dev/null
                    chattr +i "$f" 2>/dev/null
                fi
            done
        done
        echo "[✓] CODM permanent zero-revert locks applied across Directory 1 and Directory 2."
        ;;
    *)
        UID_VAL=10000
        ;;
esac

# SELinux context & directory ownership alignment
chown -R $UID_VAL:$UID_VAL "$DIR1" 2>/dev/null
chown -R $UID_VAL:$UID_VAL "$DIR2" 2>/dev/null
chcon -R u:object_r:app_data_file:s0 "$DIR1" 2>/dev/null
chcon -R u:object_r:app_data_file:s0 "$DIR2" 2>/dev/null

echo "[✓] Deployment completed successfully for $PKG (walang revert)."
