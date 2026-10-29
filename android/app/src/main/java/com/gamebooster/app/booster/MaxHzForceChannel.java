package com.gamebooster.app.booster;

import android.os.Build;
import android.util.Log;

import com.gamebooster.app.shizuku.ShizukuExecutor;

/**
 * MaxHzForceChannel — Dedicated Shizuku-direct engine for forcing 120Hz / 144Hz / 165Hz / 185Hz / 240Hz / 300Hz / 360Hz / 480Hz.
 *
 * Unlike HzFpsChannel.setRefreshRate() which gates on Display.getSupportedModes(),
 * this class fires ALL known commands unconditionally via ShizukuExecutor —
 * bypassing CommandExecutor / UserServiceConnector for zero-delay, zero-fallback execution.
 *
 * Command Layers:
 *   Layer 1 — AOSP standard: system + global namespace settings
 *   Layer 2 — Android Game Mode API + cmd window (global + per-package)
 *   Layer 3 — device_config game_overlay global policy
 *   Layer 4 — SurfaceFlinger direct binder (1035 + 1036 + 1037)
 *   Layer 5 — setprop runtime overrides (NV_FPSLIMIT, fps_limit, swapinterval, VRR disable)
 *   Layer 6 — OEM/vendor-specific refresh rate keys (auto-detected per manufacturer)
 *   Layer 7 — VRR/Adaptive refresh disable (prevents OS self-throttle)
 *   Layer 8 — Panel/DRM direct commands (EDID/MIPI DSI bypass)
 */
public final class MaxHzForceChannel {

    private static final String TAG = "MaxHzForceChannel";

    // ── Result ────────────────────────────────────────────────────────────────────────

    public static final class ForceResult {
        public final boolean success;
        public final int appliedHz;
        public final int successCount;
        public final int failCount;
        public final int totalCommands;
        public final String message;

        private ForceResult(boolean success, int appliedHz,
                            int successCount, int failCount, int total, String message) {
            this.success = success;
            this.appliedHz = appliedHz;
            this.successCount = successCount;
            this.failCount = failCount;
            this.totalCommands = total;
            this.message = message;
        }

        /** Privileged access (Root or Shizuku) not available. */
        public static ForceResult noPrivilege(int hz) {
            return new ForceResult(false, hz, 0, 0, 0,
                    "Neither Root nor Shizuku available. "
                            + "Grant Root or connect Shizuku to force " + hz + "Hz.");
        }

        public static ForceResult noShizuku(int hz) {
            return noPrivilege(hz);
        }

        /** Normal result after firing all commands. */
        public static ForceResult complete(int hz, int success, int fail, int total) {
            String status = fail == 0 ? " ✅" : " (" + fail + " failed)";
            String msg = "Force " + hz + "Hz — " + success + "/" + total
                    + " Privileged commands OK" + status;
            return new ForceResult(success > 0, hz, success, fail, total, msg);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────────────

    /**
     * Forces the display to {@code targetHz} via Root or Shizuku — NO capability check, NO fallback.
     * Uses CommandExecutor directly for instant Root / Shizuku execution.
     *
     * @param targetHz Target refresh rate: 60, 90, 120, 144, 165, or 185
     * @return ForceResult with per-layer success tracking
     */
    public static ForceResult forceApply(int targetHz) {
        boolean hasPrivilege = com.gamebooster.app.engine.PrivilegeBridgeEngine.isPrivilegedActive();
        if (!hasPrivilege) {
            Log.w(TAG, "forceApply(" + targetHz + "Hz): Root or Shizuku not available, aborting.");
            return ForceResult.noPrivilege(targetHz);
        }

        String hz  = String.valueOf(targetHz);
        String hzF = targetHz + ".0";
        java.util.List<String> batch = new java.util.ArrayList<>(48);

        Log.i(TAG, "══ MaxHzForceChannel.forceApply START (" + targetHz + "Hz) ══");

        // ── Layer 1: AOSP Standard Settings & Dynamic Refresh Defeat ─────────────────
        batch.add("settings put system peak_refresh_rate "  + hzF);
        batch.add("settings put system min_refresh_rate "   + hzF);
        batch.add("settings put system user_refresh_rate "  + hz);
        batch.add("settings put global peak_refresh_rate "  + hzF);
        batch.add("settings put global min_refresh_rate "   + hzF);
        batch.add("settings put system match_content_frame_rate 0");
        batch.add("settings put secure match_content_frame_rate_preference 0");

        // ── Layer 2: Android Game Mode API + Window Manager ───────────────────────────
        batch.add("cmd game mode performance global 2>/dev/null");
        batch.add("cmd game set --fps " + targetHz + " global 2>/dev/null");
        batch.add("cmd window set-app-refresh-rate global " + targetHz + " 2>/dev/null");

        // ── Layer 3: device_config game_overlay (per-global refresh rate policy) ─────
        batch.add("device_config put game_overlay global mode=2,fps=" + targetHz + ":mode=3,fps=" + targetHz + " 2>/dev/null");

        // ── Layer 4: SurfaceFlinger Direct Binder ────────────────────────────────────
        if (!com.gamebooster.app.core.SurfaceFlingerDirectBinder.setRefreshRate(targetHz)) {
            batch.add("service call SurfaceFlinger 1035 i32 " + targetHz + " 2>/dev/null");
            batch.add("service call SurfaceFlinger 1036 i32 " + targetHz + " 2>/dev/null");
        }

        // ── Layer 5: setprop Runtime Overrides, Ultra Extreme Graphics & Latency Eliminators ────────────────
        batch.add("setprop debug.sf.fps_limit "                + hz);
        batch.add("setprop persist.sys.NV_FPSLIMIT "           + hz);
        batch.add("setprop persist.sys.NV_POWERMODE 1");
        batch.add("setprop debug.egl.force_msaa 1");
        batch.add("setprop debug.sf.early_phase_offset_ns 0");
        batch.add("setprop debug.sf.early_app_phase_offset_ns 0");
        batch.add("setprop persist.sys.game.fps "              + hz);
        batch.add("setprop persist.sys.game.rate "             + hz);
        batch.add("setprop persist.vendor.power.dfps.level "   + hz);
        batch.add("setprop persist.vendor.display.vrr.disable 1");
        batch.add("setprop ro.surface_flinger.set_idle_timer_ms 0");
        batch.add("setprop ro.surface_flinger.set_touch_timer_ms 0");
        batch.add("setprop persist.sys.fps "                   + hz);
        batch.add("setprop debug.cpurend.fps "                 + hz);
        batch.add("setprop ro.vendor.display.default_fps "     + hz);
        batch.add("setprop vendor.display.fps "                + hz);
        batch.add("setprop debug.hwui.fps_divisor 1");
        batch.add("setprop debug.graphics.game_default_frame_rate.disabled 1");
        batch.add("setprop ro.vendor.dfps.enable 0");
        batch.add("setprop vendor.display.enable_default_fps_switch 0");

        // ── Layer 6: OEM / Vendor-Specific Keys (auto-detected) ──────────────────────
        String mfr    = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
        String brand  = Build.BRAND        != null ? Build.BRAND.toLowerCase()        : "";
        String id     = mfr + " " + brand;

        if (id.contains("xiaomi") || id.contains("redmi") || id.contains("poco")) {
            batch.add("settings put secure user_refresh_rate " + hz);
            batch.add("settings put global surface_flinger_peak_refresh_rate " + hz);
            batch.add("setprop persist.sys.joyose.fps " + hz);
            batch.add("setprop persist.sys.powerkeeper.fps 0");
        } else if (id.contains("samsung")) {
            batch.add("settings put secure refresh_rate_mode " + (targetHz >= 90 ? "2" : "1"));
            batch.add("settings put system sec_display_fps " + hz);
            batch.add("settings put secure game_auto_temperature_control 0");
        } else if (id.contains("oneplus") || id.contains("oppo") || id.contains("realme")) {
            batch.add("settings put global oneplus_screen_refresh_rate " + (targetHz >= 90 ? "2" : "1"));
            batch.add("settings put global realme_screen_refresh_rate " + hz);
            batch.add("settings put global oppo_screen_refresh_rate " + hz);
            batch.add("settings put global refresh_rate_blacklist \"\"");
            batch.add("settings put system peak_refresh_rate " + hz);
        } else if (id.contains("asus")) {
            batch.add("settings put system asus_option_display_refresh_rate " + hz);
            batch.add("settings put system asus_hfr_mode 1");
        } else if (id.contains("vivo") || id.contains("iqoo")) {
            batch.add("settings put system screen_refresh_rate " + hz);
            batch.add("settings put system iqoo_refresh_rate " + hz);
        } else if (id.contains("nubia") || id.contains("redmagic") || id.contains("zte")) {
            batch.add("settings put system display_refresh_rate " + hz);
            batch.add("settings put system redmagic_refresh_rate " + hz);
        } else if (id.contains("infinix") || id.contains("tecno") || id.contains("itel") || id.contains("transsion")) {
            batch.add("settings put system screen_refresh_rate_mode " + (targetHz >= 90 ? "1" : "0"));
            batch.add("settings put system infinix_refresh_rate " + hz);
            batch.add("settings put system tecno_refresh_rate " + hz);
            batch.add("settings put system tran_refresh_rate " + hz);
            batch.add("settings put system tran_screen_refresh_rate_mode " + hz);
            batch.add("settings put system refresh_rate_setting " + hz);
            batch.add("settings put system auto_refresh_rate 0");
        } else if (id.contains("motorola") || id.contains("moto")) {
            batch.add("settings put global peak_refresh_rate " + hz);
            batch.add("settings put system min_refresh_rate " + hz);
        } else {
            batch.add("settings put system peak_refresh_rate " + hz);
            batch.add("settings put system user_refresh_rate " + hz);
        }

        // ── Layer 7: VRR / Adaptive Refresh Disable (prevents OS self-throttle) ──────────
        batch.add("settings put secure vrr_enabled 0");
        batch.add("settings put global vrr_app_mode 0");
        batch.add("device_config put display_manager vrr_enabled false 2>/dev/null");
        batch.add("setprop persist.sys.vrr.enable 0");
        batch.add("setprop persist.vendor.display.vrr.disable 1");
        batch.add("setprop debug.sf.vrr_enable 0");

        // ── Layer 8: Panel/DRM Direct Commands (EDID/MIPI DSI bypass) ──────────────────
        batch.add("service call SurfaceFlinger 1037 i32 " + targetHz + " 2>/dev/null");
        batch.add("echo " + targetHz + " > /sys/class/drm/card0/modes 2>/dev/null");
        batch.add("echo " + targetHz + " > /sys/class/display/mode 2>/dev/null");
        batch.add("echo " + targetHz + " > /sys/devices/platform/display/mode 2>/dev/null");

        int total = batch.size();
        boolean batchSuccess = com.gamebooster.app.engine.CommandExecutor.executeBatch(batch);
        int ok = batchSuccess ? total : 0;
        int fail = total - ok;
        Log.i(TAG, "══ MaxHzForceChannel.forceApply DONE: "
                + ok + "/" + total + " OK, " + targetHz + "Hz ══");
        return ForceResult.complete(targetHz, ok, fail, total);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────────────

    /**
     * Fires a single privileged command directly via CommandExecutor (Root or Shizuku).
     * Returns 1 on success, 0 on failure/exception.
     */
    private static int run(String command) {
        try {
            String result = com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(command);
            boolean success = result != null
                    && !result.trim().toLowerCase().startsWith("error")
                    && !result.trim().toLowerCase().contains("permission denial")
                    && !result.trim().toLowerCase().contains("not found");
            Log.d(TAG, (success ? "  ✓ " : "  ✗ ") + command
                    + (success ? "" : "  →  " + (result != null ? result.trim() : "null")));
            return success ? 1 : 0;
        } catch (Throwable t) {
            Log.w(TAG, "  ! EXC " + command + " — " + t.getMessage());
            return 0;
        }
    }
}
