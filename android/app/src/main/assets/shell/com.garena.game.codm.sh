#!/system/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# com.garena.game.codm.sh — Call of Duty Mobile (Garena Region) tuning
# Target: com.garena.game.codm
# Synchronized Vulkan pipeline + Dynamic Hz unlock + Safe Thermal Governor
# ─────────────────────────────────────────────────────────────────────────────

# IW8 (COD) engine — Vulkan preferred renderer
setprop debug.hwui.renderer skiavk
setprop debug.renderengine.backend vulkan
setprop debug.vulkan.enable_validation_layers 0
setprop debug.egl.swapinterval 0

# Force max CPU on all policies — IW8 is multi-threaded heavily
for p in /sys/devices/system/cpu/cpufreq/policy*; do
  echo performance > "$p/scaling_governor" 2>/dev/null
  cat "$p/cpuinfo_max_freq" > "$p/scaling_min_freq" 2>/dev/null
done

# ── Dynamic Hz Resolution ─────────────────────────────────────────────────────
TARGET_HZ="{TARGET_HZ}"
case "$TARGET_HZ" in
  *{*}*|"") TARGET_HZ=185 ;;
esac

# ── SurfaceFlinger & System Settings ──────────────────────────────────────────
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

# Kernel-level display nodes
echo $TARGET_HZ > /sys/devices/virtual/graphics/fb0/dynamic_fps 2>/dev/null
echo $TARGET_HZ > /sys/class/graphics/fb0/dynamic_fps 2>/dev/null
echo $TARGET_HZ > /sys/devices/platform/mtk_disp_mgr.0/refresh_rate 2>/dev/null
echo $TARGET_HZ > /proc/mtk_display/fps 2>/dev/null
echo $TARGET_HZ > /sys/devices/platform/exynos-drm/drm/card0/card0-DSI-1/max_fps 2>/dev/null

# COD network — latency sensitive
sysctl -w net.ipv4.tcp_congestion_control=bbr 2>/dev/null
sysctl -w net.ipv4.tcp_quickack=1 2>/dev/null
sysctl -w net.ipv4.tcp_nodelay=1 2>/dev/null

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

# Drop caches — COD map loads need max contiguous RAM
echo 3 > /proc/sys/vm/drop_caches 2>/dev/null

# ── AC Telemetry Null-Route (crash/bug reports only — Tencent ACE endpoints) ──
for HOST in bugly.qq.com crash.tencent.com androidsdk.bugly.qq.com ac.tosshub.com; do
    iptables -A OUTPUT -d "$HOST" -j DROP 2>/dev/null
    ip6tables -A OUTPUT -d "$HOST" -j DROP 2>/dev/null
done

# ── CODM Config Permission Alignment & Safe Unlocking ─────────────────────
CODM_UID=$(stat -c %u "/data/user/0/com.garena.game.codm" 2>/dev/null || stat -c %u "/storage/emulated/0/Android/data/com.garena.game.codm" 2>/dev/null || echo 10656)
for BASE in "/data/user/0/com.garena.game.codm/files" "/storage/emulated/0/Android/data/com.garena.game.codm/files"; do
    CFG_DIR="$BASE/Config"
    if [ -d "$CFG_DIR" ]; then
        chattr -R -i "$CFG_DIR" 2>/dev/null
        chmod -R 775 "$CFG_DIR" 2>/dev/null
        find "$CFG_DIR" -type f 2>/dev/null | while read -r f; do
            chown "$CODM_UID:$CODM_UID" "$f" 2>/dev/null
            chmod 666 "$f" 2>/dev/null
        done
    fi
    [ -f "$BASE/res_skip_patch" ] && chattr -i "$BASE/res_skip_patch" 2>/dev/null && chmod 666 "$BASE/res_skip_patch" 2>/dev/null
done

# GameMode API clamp removal for CODM (Garena)
cmd game set --mode 2 --user 0 com.garena.game.codm 2>/dev/null

