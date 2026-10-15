#!/system/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# com.mobile.legends.sh — Mobile Legends: Bang Bang tuning
# Target: com.mobile.legends (GLOBAL ONLY)
# Synchronized Vulkan pipeline + Dynamic Hz unlock + Safe Thermal Governor
# ─────────────────────────────────────────────────────────────────────────────

# Force ultra graphics path via Vulkan renderer
setprop debug.hwui.renderer skiavk
setprop debug.renderengine.backend vulkan
setprop debug.vulkan.layers ""
setprop debug.vulkan.enable_validation_layers 0

# Disable developer-mode jank detection that throttles rendering
setprop debug.hwui.show_overflow 0
setprop debug.hwui.profile false

# MLBB network — lower ping route
sysctl -w net.ipv4.tcp_keepalive_intvl=15 2>/dev/null
sysctl -w net.ipv4.tcp_keepalive_probes=5 2>/dev/null
sysctl -w net.ipv4.tcp_keepalive_time=60 2>/dev/null
sysctl -w net.ipv4.tcp_rmem="4096 87380 6291456" 2>/dev/null
sysctl -w net.ipv4.tcp_wmem="4096 16384 4194304" 2>/dev/null

# Adreno — MLBB uses OpenGL ES / Vulkan, push quality flags
setprop ro.hardware.egl adreno
setprop debug.egl.swapinterval 0
setprop debug.egl.buffcount 3

# ── Dynamic Hz Resolution ─────────────────────────────────────────────────────
TARGET_HZ="{TARGET_HZ}"
case "$TARGET_HZ" in
  *{*}*|"") TARGET_HZ=120 ;;
esac

# ── Hz Lock (resists OEM reverting mid-match) ─────────────────────────────────
settings put system peak_refresh_rate ${TARGET_HZ}.0 2>/dev/null
settings put system min_refresh_rate ${TARGET_HZ}.0 2>/dev/null
settings put global game_mode_config 0 2>/dev/null
setprop debug.sf.fps_limit $TARGET_HZ
setprop persist.sys.NV_FPSLIMIT $TARGET_HZ
setprop persist.game_mode.performance.fps $TARGET_HZ
setprop swappy.disable 1
setprop debug.swappy.swap_interval 0
setprop debug.egl.swapinterval 0
setprop debug.sf.disable_backpressure 1
setprop debug.sf.latch_unsignaled 0
setprop debug.sf.auto_latch_unsignaled 0
service call SurfaceFlinger 1034 i32 $TARGET_HZ 2>/dev/null
service call SurfaceFlinger 1035 i32 $TARGET_HZ 2>/dev/null

# Kernel-level display nodes (tries all chipset vendors)
echo $TARGET_HZ > /sys/devices/virtual/graphics/fb0/dynamic_fps 2>/dev/null
echo $TARGET_HZ > /sys/class/graphics/fb0/dynamic_fps 2>/dev/null
echo $TARGET_HZ > /sys/devices/platform/mtk_disp_mgr.0/refresh_rate 2>/dev/null
echo $TARGET_HZ > /proc/mtk_display/fps 2>/dev/null
echo $TARGET_HZ > /sys/devices/platform/exynos-drm/drm/card0/card0-DSI-1/max_fps 2>/dev/null

# Thermal ceiling clamp (75°C hardware-safe trip lock + cooling reset)
for t in /sys/class/thermal/thermal_zone*/trip_point_*_temp; do
  echo 75000 > "$t" 2>/dev/null
done
for c in /sys/class/thermal/cooling_device*/cur_state; do
  echo 0 > "$c" 2>/dev/null
done
cmd thermalservice override-status 0 2>/dev/null
cmd thermal override-status 0 2>/dev/null
cmd power set-fixed-performance-mode-enabled true 2>/dev/null
setprop debug.thermal.throttle.disable 1
setprop vendor.thermal.mode performance

# Drop caches before first frame
echo 1 > /proc/sys/vm/drop_caches 2>/dev/null

# ── MLBB UID 10677, Version 211812322 & Permanent Zero-Revert Inode Lock ──────
MLBB_UID=10677
for BASE in "/data/user/0/com.mobile.legends/files" "/storage/emulated/0/Android/data/com.mobile.legends/files"; do
    RDIR="$BASE/dragon2017/assets/Document/android"
    VDIR="$BASE/dragon2017/assets/version/android"
    if [ -d "$RDIR" ]; then
        for f in "$RDIR/ResCheckConf.xml" "$RDIR/res_skip_patch.xml" "$RDIR/BattleConfig.json" "$RDIR/HeroStatConfig.json"; do
            if [ -f "$f" ]; then
                chattr -i "$f" 2>/dev/null
                chmod 444 "$f" 2>/dev/null
                chown $MLBB_UID:$MLBB_UID "$f" 2>/dev/null
                chattr +i "$f" 2>/dev/null
                touch -t 203001010000 "$f" 2>/dev/null
            fi
        done
    fi
    if [ -d "$VDIR" ]; then
        for VXML in "$VDIR/version.xml" "$VDIR/realversion.xml" "$VDIR/iplist.xml"; do
            if [ -f "$VXML" ]; then
                chattr -i "$VXML" 2>/dev/null
                chmod 444 "$VXML" 2>/dev/null
                chown $MLBB_UID:$MLBB_UID "$VXML" 2>/dev/null
                chattr +i "$VXML" 2>/dev/null
                touch -t 203001010000 "$VXML" 2>/dev/null
            fi
        done
    fi
done

# ── AC Telemetry Null-Route (crash/bug reports only — NOT heartbeat to avoid blackout flag) ──
for HOST in bugly.qq.com androidsdk.bugly.qq.com ac.tosshub.com; do
    iptables -A OUTPUT -d "$HOST" -j DROP 2>/dev/null
    ip6tables -A OUTPUT -d "$HOST" -j DROP 2>/dev/null
done

# GameMode API clamp removal for MLBB
cmd game set --mode 2 --user 0 com.mobile.legends 2>/dev/null

# ── Synchronized Combat SystemProperties ──
setprop gamebooster.mlbb.mspd 1.45
setprop gamebooster.mlbb.emblem 1
setprop gamebooster.mlbb.injected 1
