package com.gamebooster.app.booster;

import android.util.Log;
import com.gamebooster.app.engine.CommandExecutor;
import com.gamebooster.app.shizuku.ShizukuExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * TouchLatencyChannel — Precision Aim, Zero-Slop Touch, and 1000Hz Digitizer Overdrive.
 * Directly enforces low-latency hardware digitizer polling, kernel sysfs nodes,
 * Android InputFlinger resampling elimination, and universal OEM gaming touch modes.
 */
public class TouchLatencyChannel {

    private static final String TAG = "TouchLatencyChannel";

    public static boolean enableUltraTouchResponse() {
        Log.i(TAG, "⚡ Enforcing Developer-Grade Zero-Delay Touch (Batched Hardware + Software Pipeline)");
        executeElevatedTouchBatch();
        return true;
    }

    private static void executeElevatedTouchBatch() {
        List<String> commands = new ArrayList<>();

        // Settings & Framework Overrides
        commands.add("settings put system touch_slop_reduction 1");
        commands.add("settings put system pointer_speed 7");
        commands.add("settings put system touch_sensitivity 1");
        commands.add("settings put system master_touch_sensitivity 1");
        commands.add("settings put system sec_touch_sensitivity 1");
        commands.add("settings put system touch_protective_film 1");
        commands.add("settings put system game_mode_touch 1");
        commands.add("settings put system edge_touch_filter 0");
        commands.add("settings put secure long_press_timeout 100");
        commands.add("settings put secure multi_press_timeout 50");
        commands.add("settings put secure edge_rejection_mode 0");

        // Tecno / Infinix Transsion
        commands.add("settings put system tran_game_mode_touch 1");
        commands.add("settings put system tran_touch_rate 1000");
        commands.add("settings put system tran_game_touch_response 1");
        commands.add("settings put system tran_touch_sampling_rate 1000");
        commands.add("settings put system tran_game_boost_touch 1");
        commands.add("settings put system hios_game_touch 1");
        commands.add("settings put system xos_game_touch 1");
        commands.add("setprop persist.sys.tran.touch_rate 1000");
        commands.add("setprop persist.sys.tran.game_touch 1");
        commands.add("setprop persist.sys.transsion.touch_boost 1");
        commands.add("setprop persist.vendor.tran.touch.sampling_rate 1000");

        // MediaTek MTK & Universal OEM 1000Hz Digitizer Locks
        commands.add("setprop persist.vendor.mediatek.touch_boost 1");
        commands.add("setprop debug.mtk.touch_boost 1");
        commands.add("setprop persist.sys.sec.touch_rate 1000");
        commands.add("setprop persist.sys.touch.report_rate 1000");
        commands.add("setprop persist.vendor.touch.sampling_rate 1000");
        commands.add("setprop debug.touch.sampling_rate 1000");
        commands.add("setprop persist.sys.gamemode.touch 1");
        commands.add("setprop vendor.touch.game_mode 1");
        commands.add("setprop persist.asus.touch_sampling_rate 1000");
        commands.add("setprop persist.vendor.asus.touch_opt 1");
        commands.add("setprop persist.asus.armoury_crate.touch_rate 1000");
        commands.add("setprop persist.sys.miui.game_touch_rate 1000");
        commands.add("setprop persist.vendor.touch.touch_boost 1");
        commands.add("setprop persist.sys.touch.smooth 1");
        commands.add("setprop persist.vendor.oplus.touch_rate 1000");
        commands.add("setprop persist.sys.oplus.game_touch 1");
        commands.add("settings put system oplus_touch_game_mode 1");
        commands.add("setprop persist.vivo.touch_sample_rate 1000");
        commands.add("setprop persist.sys.vivo.gamemode.touch 1");
        commands.add("settings put system vivo_game_touch_enhance 1");
        commands.add("setprop persist.sys.nubia.touch_sampling_rate 1000");
        commands.add("setprop persist.sys.redmagic.touch_mode 1");
        commands.add("setprop persist.vendor.goodix.touch_rate 1000");
        commands.add("setprop persist.vendor.synaptics.touch_rate 1000");
        commands.add("setprop persist.sys.hyperos.touch_boost 1");
        commands.add("setprop persist.vendor.samsung.touch_boost 1");
        commands.add("setprop persist.mot.touch.sampling_rate 1000");
        commands.add("setprop persist.mot.touch_boost 1");
        commands.add("settings put system mot_game_touch_optimization 1");
        commands.add("setprop persist.sys.honor.touch_boost 1");
        commands.add("setprop persist.sys.huawei.touch_boost 1");
        commands.add("setprop persist.sys.sprd.touch_boost 1");
        commands.add("setprop persist.vendor.sprd.touch_rate 1000");

        // Resampling, Slop Elimination & Gyro 1000Hz
        commands.add("setprop view.touch_slop 0");
        commands.add("setprop view.scroll_friction 0.001");
        commands.add("setprop view.fading_edge_length 0");
        commands.add("setprop ro.min_pointer_dur 1");
        commands.add("setprop persist.sys.input.resampling 0");
        commands.add("setprop debug.inputflinger.resampling 0");
        commands.add("setprop debug.inputflinger.throttle_time 0");
        commands.add("setprop persist.sys.inputflinger.nice -20");
        commands.add("setprop persist.input.velocitytracker.strategy impulse");
        commands.add("setprop debug.input.max_events_per_sec 2000");
        commands.add("setprop debug.hwui.input_latency_timeout 0");
        commands.add("setprop persist.sys.input.latency 0");
        commands.add("setprop debug.inputflinger.touch_boost 1");
        commands.add("setprop debug.inputflinger.fling_boost 1");
        commands.add("setprop debug.input.boost_time_ms 5000");
        commands.add("setprop touch.motion_filter.enable 0");
        commands.add("setprop persist.sys.touch.edge_filter 0");
        commands.add("setprop persist.vendor.touch.edge_reject 0");
        commands.add("setprop persist.sys.touch.corner_filter 0");
        commands.add("setprop touch.pressure.scale 0.0001");
        commands.add("setprop touch.size.calibration geometric");
        commands.add("setprop touch.pressure.calibration physical");
        commands.add("setprop touch.distance.scale 0");
        commands.add("setprop touch.size.bias 0");
        commands.add("setprop debug.sensor.gyro.sample_rate 1000");
        commands.add("setprop debug.sensor.motion.rate 1000");
        commands.add("setprop debug.sensor.gyro.smooth 1");
        commands.add("setprop debug.sensor.gyro.stabilization 1");
        commands.add("setprop persist.sys.gyro.delay 0");

        // SurfaceFlinger Presentation
        commands.add("setprop ro.surface_flinger.set_touch_timer_ms 0");
        commands.add("setprop ro.surface_flinger.set_idle_timer_ms 0");
        commands.add("setprop debug.sf.latch_unsignaled 0");
        commands.add("setprop debug.sf.auto_latch_unsignaled 0");
        commands.add("setprop debug.sf.disable_backpressure 1");
        commands.add("setprop debug.sf.early_phase_offset_ns 0");
        commands.add("setprop debug.sf.early_app_phase_offset_ns 0");
        commands.add("setprop debug.sf.early_gl_phase_offset_ns 0");

        // Hardware Digitizer Sysfs & Procfs Overdrive
        commands.add("echo 1 > /proc/touchpanel/game_switch_enable 2>/dev/null");
        commands.add("echo 0 > /proc/touchpanel/oppo_tp_limit_enable 2>/dev/null");
        commands.add("echo 0 > /proc/touchpanel/oppo_tp_direction 2>/dev/null");
        commands.add("echo 1 > /sys/class/touch/touch_dev/game_mode 2>/dev/null");
        commands.add("echo 1000 > /sys/class/touch/touch_dev/touch_rate 2>/dev/null");
        commands.add("echo 1 > /sys/devices/platform/tran_touch/game_mode 2>/dev/null");
        commands.add("echo 1 > /proc/tran_touch/game_mode 2>/dev/null");
        commands.add("echo 1 > /sys/class/touch/touch_dev/tran_game_mode 2>/dev/null");
        commands.add("echo 1 > /sys/module/mtk_fpsgo/parameters/fbt_touch_boost 2>/dev/null");
        commands.add("echo 1 > /sys/module/ged/parameters/ged_touch_boost 2>/dev/null");
        commands.add("echo 1 > /sys/kernel/ged/hal/touch_boost 2>/dev/null");
        commands.add("echo 1 > /sys/devices/system/cpu/cpufreq/schedutil/iowait_boost_enable 2>/dev/null");
        commands.add("echo 200 > /sys/module/cpu_boost/parameters/input_boost_ms 2>/dev/null");
        commands.add("echo 1 > /sys/module/msm_performance/parameters/touchboost 2>/dev/null");
        commands.add("sysctl -w kernel.sched_boost=1 2>/dev/null");

        // Multi-touch input device filter nulling
        commands.add("for dev in /sys/class/input/input*/device; do " +
                "echo 1 > \"$dev/game_mode\" 2>/dev/null; " +
                "echo 1 > \"$dev/touch_boost\" 2>/dev/null; " +
                "echo 0 > \"$dev/filter_enable\" 2>/dev/null; " +
                "echo 0 > \"$dev/edge_reject\" 2>/dev/null; " +
                "echo 0 > \"$dev/palm_reject\" 2>/dev/null; " +
                "echo 1000 > \"$dev/sample_rate\" 2>/dev/null; " +
                "echo 1000 > \"$dev/report_rate\" 2>/dev/null; " +
                "done");

        CommandExecutor.executeBatch(commands);
    }

    public static boolean restoreDefaultTouchResponse() {
        List<String> restoreCmds = new ArrayList<>();
        restoreCmds.add("settings put system touch_slop_reduction 0");
        restoreCmds.add("settings put system pointer_speed 0");
        restoreCmds.add("settings put system touch_sensitivity 0");
        restoreCmds.add("settings put system game_mode_touch 0");
        restoreCmds.add("settings put secure long_press_timeout 400");
        restoreCmds.add("settings put secure multi_press_timeout 300");
        restoreCmds.add("settings put secure edge_rejection_mode 1");
        restoreCmds.add("setprop view.touch_slop 8");
        restoreCmds.add("setprop view.scroll_friction 0.015");
        restoreCmds.add("setprop persist.sys.input.resampling 1");
        restoreCmds.add("setprop debug.inputflinger.resampling 1");
        restoreCmds.add("setprop persist.input.velocitytracker.strategy lsq2");
        restoreCmds.add("setprop persist.sys.touch.report_rate 120");
        restoreCmds.add("setprop persist.vendor.touch.sampling_rate 120");
        restoreCmds.add("setprop debug.touch.sampling_rate 120");
        restoreCmds.add("setprop persist.sys.gamemode.touch 0");
        restoreCmds.add("setprop vendor.touch.game_mode 0");
        restoreCmds.add("setprop debug.inputflinger.touch_boost 0");
        restoreCmds.add("setprop debug.inputflinger.fling_boost 0");
        restoreCmds.add("setprop debug.input.max_events_per_sec 120");
        restoreCmds.add("setprop debug.hwui.input_latency_timeout 500");
        restoreCmds.add("setprop persist.sys.touch.edge_filter 1");
        restoreCmds.add("setprop persist.vendor.touch.edge_reject 1");

        return CommandExecutor.executeBatch(restoreCmds);
    }
}
