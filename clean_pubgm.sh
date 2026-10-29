#!/system/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# clean_pubgm.sh — Instant Fix for PUBG Mobile Stuck on Loading Screen
# Author: Game Launcher Team (for LO)
# ─────────────────────────────────────────────────────────────────────────────

echo "⚡ [PUBGM RESCUE] Starting PUBG Mobile unfreeze & repair..."

TARGET_PKGS="com.tencent.ig com.pubg.krmobile com.pubg.imobile com.vng.pubgmobile com.rekoo.pubgm"

for PKG in $TARGET_PKGS; do
    BASE="/storage/emulated/0/Android/data/${PKG}/files/UE4Game/ShadowTrackerExtra"
    
    if [ -d "/storage/emulated/0/Android/data/${PKG}" ]; then
        echo "🔧 Fixing package: ${PKG}..."
        
        # 1. Force stop game if running
        am force-stop "$PKG" 2>/dev/null
        
        # 2. Release immutable file locks
        chattr -R -i "/storage/emulated/0/Android/data/${PKG}/files" 2>/dev/null
        chattr -R -i "/data/data/${PKG}/files" 2>/dev/null
        
        # 3. Clean temporary lock files, dead caches & broken nomedia
        for S_DIR in "${BASE}/ShadowTrackerExtra/Saved" "${BASE}/Saved"; do
            if [ -d "$S_DIR" ]; then
                rm -f "$S_DIR"/*.tmp* "$S_DIR"/*lock* 2>/dev/null
                rm -f "$S_DIR"/SaveGames/*.tmp* 2>/dev/null
                rm -f "$S_DIR"/Pandora/*.tmp* 2>/dev/null
                rm -f "$S_DIR"/Pandora/.nomedia "$S_DIR"/SaveGames/.nomedia "$S_DIR"/.nomedia 2>/dev/null
                rm -rf "$S_DIR"/Logs/* "$S_DIR"/Crashes/* "$S_DIR"/Tmp/* "$S_DIR"/StatData/* 2>/dev/null
                rm -rf "$S_DIR"/Pandora/Logs/* "$S_DIR"/Pandora/StatsResend/* 2>/dev/null
                
                # Check for corrupted loginInfoFile.json containing +CVars
                LOGIN_JSON="$S_DIR/SaveGames/loginInfoFile.json"
                if [ -f "$LOGIN_JSON" ] && grep -q "+CVars" "$LOGIN_JSON" 2>/dev/null; then
                    echo "⚠️ Detected corrupted loginInfoFile.json (+CVars injection). Removing to restore login screen..."
                    rm -f "$LOGIN_JSON"
                fi

                # Clean invalid non-standard INI files that cause UE4 parser hang
                CFG_DIR="$S_DIR/Config/Android"
                if [ -d "$CFG_DIR" ]; then
                    for f in "$CFG_DIR"/*.ini; do
                        case "$f" in
                            *UserCustom.ini|*GameUserSettings.ini) ;;
                            *) rm -f "$f" 2>/dev/null ;;
                        esac
                    done
                fi
            fi
        done
        
        # 4. Open read/write permissions for UE4 engine
        chmod -R 777 "/storage/emulated/0/Android/data/${PKG}/files" 2>/dev/null
        
        # 5. Check Active.sav integrity
        SAV_PATH="${BASE}/ShadowTrackerExtra/Saved/SaveGames/Active.sav"
        if [ -f "$SAV_PATH" ]; then
            HEADER=$(head -c 4 "$SAV_PATH" 2>/dev/null)
            if [ "$HEADER" != "GVAS" ]; then
                echo "⚠️ Corrupted Active.sav detected. Removing..."
                rm -f "$SAV_PATH"
            fi
        fi
        
        echo "✅ Fixed ${PKG}."
    fi
done

echo "🚀 [PUBGM RESCUE] Done! You can now launch PUBG Mobile smoothly."
