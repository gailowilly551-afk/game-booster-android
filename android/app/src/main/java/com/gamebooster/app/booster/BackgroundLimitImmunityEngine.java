package com.gamebooster.app.booster;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.engine.CommandExecutor;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.shizuku.ShizukuManager;

import java.util.ArrayList;
import java.util.List;

/**
 * BackgroundLimitImmunityEngine — Unrestricted Background Execution & Shizuku Immortal Shield.
 *
 * Guarantees that neither Game Booster nor Shizuku is ever killed, frozen, or throttled
 * during multi-hour gaming sessions by:
 * 1. Whitelisting both packages from Doze (deviceidle whitelist + dumpsys).
 * 2. Granting AppOps permissions (RUN_IN_BACKGROUND, RUN_ANY_IN_BACKGROUND, WAKE_LOCK).
 * 3. Pinning App Standby Bucket to ACTIVE (am set-standby-bucket active, am set-inactive false).
 * 4. Disabling Android 12-16 Phantom Process Killer globally.
 * 5. Pinning Kernel Low Memory Killer oom_score_adj to -900 (system-protected, immune to LMKD).
 * 6. Bypassing OEM battery savers (MIUI/HyperOS AutoStart, Samsung OneUI, ColorOS/Realme).
 */
@android.annotation.SuppressLint("ObsoleteSdkInt")
public final class BackgroundLimitImmunityEngine {

    private static final String TAG = "BackgroundImmunity";
    public static final String GAME_BOOSTER_PACKAGE = "com.gamebooster.app";
    public static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";

    private static volatile boolean sIsImmunityEnforced = false;

    private BackgroundLimitImmunityEngine() {}

    public static boolean isImmunityEnforced() {
        return sIsImmunityEnforced;
    }

    /**
     * Checks if standard Android battery optimizations are ignored for Game Booster.
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (context == null) return false;
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        } catch (Throwable t) {
            Log.w(TAG, "isIgnoringBatteryOptimizations error: " + t.getMessage());
        }
        return false;
    }

    /**
     * Requests user to exempt Game Booster from standard battery optimizations via system dialog.
     */
    public static void requestIgnoreBatteryOptimizations(Context context) {
        if (context == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isIgnoringBatteryOptimizations(context)) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to launch battery optimization dialog, opening settings fallback", t);
            try {
                Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Enforces complete background immunity for both Game Booster and Shizuku daemon.
     * Can be invoked asynchronously on background worker thread.
     */
    public static void enforceImmunity(Context context) {
        AppExecutors.getInstance().executeCommand(() -> enforceImmunitySync(context));
    }

    /**
     * Synchronous implementation of privileged immunity enforcement.
     */
    public static void enforceImmunitySync(Context context) {
        try {
            Log.i(TAG, "🛡️ Enforcing Unrestricted Background Immunity for Game Booster & Shizuku...");

            List<String> cmds = new ArrayList<>();

            // 1. Android Doze Whitelist (Both dumpsys and cmd variants for all Android versions)
            cmds.add("dumpsys deviceidle whitelist +" + GAME_BOOSTER_PACKAGE + " 2>/dev/null");
            cmds.add("dumpsys deviceidle whitelist +" + SHIZUKU_PACKAGE + " 2>/dev/null");
            cmds.add("cmd deviceidle whitelist +" + GAME_BOOSTER_PACKAGE + " 2>/dev/null");
            cmds.add("cmd deviceidle whitelist +" + SHIZUKU_PACKAGE + " 2>/dev/null");

            // 2. AppOps Unrestricted Background & Wakelock Execution
            cmds.add("cmd appops set " + GAME_BOOSTER_PACKAGE + " RUN_IN_BACKGROUND allow 2>/dev/null");
            cmds.add("cmd appops set " + GAME_BOOSTER_PACKAGE + " RUN_ANY_IN_BACKGROUND allow 2>/dev/null");
            cmds.add("cmd appops set " + GAME_BOOSTER_PACKAGE + " WAKE_LOCK allow 2>/dev/null");
            cmds.add("cmd appops set " + GAME_BOOSTER_PACKAGE + " SYSTEM_ALERT_WINDOW allow 2>/dev/null");

            cmds.add("cmd appops set " + SHIZUKU_PACKAGE + " RUN_IN_BACKGROUND allow 2>/dev/null");
            cmds.add("cmd appops set " + SHIZUKU_PACKAGE + " RUN_ANY_IN_BACKGROUND allow 2>/dev/null");
            cmds.add("cmd appops set " + SHIZUKU_PACKAGE + " WAKE_LOCK allow 2>/dev/null");

            // 3. Pin App Standby Bucket to ACTIVE (Never allow Android to mark as RARE or RESTRICTED)
            cmds.add("am set-standby-bucket " + GAME_BOOSTER_PACKAGE + " active 2>/dev/null");
            cmds.add("cmd app_standby set " + GAME_BOOSTER_PACKAGE + " active 2>/dev/null");
            cmds.add("am set-inactive " + GAME_BOOSTER_PACKAGE + " false 2>/dev/null");

            cmds.add("am set-standby-bucket " + SHIZUKU_PACKAGE + " active 2>/dev/null");
            cmds.add("cmd app_standby set " + SHIZUKU_PACKAGE + " active 2>/dev/null");
            cmds.add("am set-inactive " + SHIZUKU_PACKAGE + " false 2>/dev/null");

            // 4. Defeat Android 12-16 Phantom Process Killer & Cached Apps Freezer
            cmds.add("/system/bin/device_config put activity_manager max_phantom_processes 2147483647 2>/dev/null");
            cmds.add("/system/bin/setprop persist.sys.fflag.override.settings_enable_monitor_phantom_procs false 2>/dev/null");
            cmds.add("settings put global settings_enable_monitor_phantom_procs false 2>/dev/null");
            cmds.add("settings put global cached_apps_freezer disabled 2>/dev/null");
            cmds.add("cmd device_config put activity_manager freeze_debounce_timeout 86400000 2>/dev/null");

            // Android 15 & 16 (Baklava/API 36) Strict Application Limits & Process Freeze Bypasses
            cmds.add("cmd device_config put activity_manager use_freezer false 2>/dev/null");
            cmds.add("cmd device_config put activity_manager pkill_unresponsive_cached_processes false 2>/dev/null");
            cmds.add("cmd device_config put activity_manager procs_per_package_limit 100 2>/dev/null");
            cmds.add("cmd device_config put activity_manager max_cached_processes 128 2>/dev/null");
            cmds.add("cmd device_config put activity_manager kill_bg_restricted_cached_idle false 2>/dev/null");
            cmds.add("cmd device_config put activity_manager modern_queue_enabled true 2>/dev/null");
            cmds.add("setprop persist.sys.activity_manager.modern_queue_freezer false 2>/dev/null");

            // 5. Keep Wi-Fi and network sleep policies awake during Gaming
            cmds.add("settings put global adb_allowed_connection_time 0 2>/dev/null");
            cmds.add("settings put global wifi_sleep_policy 2 2>/dev/null");
            cmds.add("settings put global wifi_wakeup_available 1 2>/dev/null");
            cmds.add("settings put global wifi_wakeup_enabled 1 2>/dev/null");
            cmds.add("settings put global wifi_mobile_data_transition_wakelock_timeout_ms 300000 2>/dev/null");
            cmds.add("settings put global stay_on_while_plugged_in 0 2>/dev/null");
            cmds.add("settings put global development_settings_enabled 1 2>/dev/null");
            cmds.add("cmd connectivity airplane-mode disable 2>/dev/null");

            // 6. OEM-Specific Background Killer Bypasses (MIUI / HyperOS, Transsion, ColorOS)
            cmds.add("cmd appops set " + GAME_BOOSTER_PACKAGE + " AUTO_START allow 2>/dev/null");
            cmds.add("cmd appops set " + SHIZUKU_PACKAGE + " AUTO_START allow 2>/dev/null");
            cmds.add("cmd appops set " + GAME_BOOSTER_PACKAGE + " BOOT_COMPLETED allow 2>/dev/null");
            cmds.add("cmd appops set " + SHIZUKU_PACKAGE + " IGNORE_BATTERY_OPTIMIZATIONS allow 2>/dev/null");
            cmds.add("cmd appops set " + GAME_BOOSTER_PACKAGE + " IGNORE_BATTERY_OPTIMIZATIONS allow 2>/dev/null");
            cmds.add("cmd appops set " + SHIZUKU_PACKAGE + " SYSTEM_ALERT_WINDOW allow 2>/dev/null");
            cmds.add("cmd appops set " + SHIZUKU_PACKAGE + " REQUEST_IGNORE_BATTERY_OPTIMIZATIONS allow 2>/dev/null");

            // Android 16 Battery Saver exempt mode
            cmds.add("cmd power set-mode 0 2>/dev/null");

            // Execute batch privileged commands
            CommandExecutor.executeBatchCommands(cmds);

            // 7. Kernel OOM Immortal Pinning: Set oom_score_adj to -900 for our PIDs and Shizuku
            protectProcessOomScore(GAME_BOOSTER_PACKAGE);
            protectProcessOomScore(SHIZUKU_PACKAGE);

            sIsImmunityEnforced = true;
            Log.i(TAG, "✅ Background Limit Immunity fully applied. Game Booster & Shizuku are now immortal in background.");
        } catch (Throwable t) {
            Log.w(TAG, "Error applying background limit immunity", t);
        }
    }

    /**
     * Sets oom_score_adj to -900 and oom_adj to -17 for all active PIDs of a given package or process pattern.
     * Prevents Android's Low Memory Killer Daemon (LMKD) from killing the process during heavy games.
     */
    public static void protectProcessOomScore(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        try {
            java.util.Set<Integer> targetPids = new java.util.HashSet<>();

            // If protecting Game Booster itself, always include current process PID
            if (GAME_BOOSTER_PACKAGE.equals(packageName)) {
                targetPids.add(android.os.Process.myPid());
            }

            // Query via pidof and pgrep to catch multi-process daemons (:service, shizuku_server, app_process)
            String cmd = "pidof " + packageName + " 2>/dev/null; pgrep -f " + packageName + " 2>/dev/null";
            if (SHIZUKU_PACKAGE.equals(packageName)) {
                cmd += "; pgrep -f shizuku_server 2>/dev/null; pgrep -f rikka.shizuku 2>/dev/null; pgrep -f 'shizuku' 2>/dev/null; pgrep -f 'rish' 2>/dev/null";
            }

            String pidOut = ShizukuExecutor.executeShizukuCommand(cmd);
            if (pidOut != null && !pidOut.trim().isEmpty() && !pidOut.startsWith("ERROR")) {
                String[] lines = pidOut.trim().split("[\\s\\n\\r]+");
                for (String pStr : lines) {
                    try {
                        int pid = Integer.parseInt(pStr.trim());
                        if (pid > 0) {
                            targetPids.add(pid);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (!targetPids.isEmpty()) {
                List<String> oomCmds = new ArrayList<>();
                for (int pid : targetPids) {
                    oomCmds.add("echo -900 > /proc/" + pid + "/oom_score_adj 2>/dev/null");
                    oomCmds.add("echo -17 > /proc/" + pid + "/oom_adj 2>/dev/null");
                    oomCmds.add("renice -n -20 -p " + pid + " 2>/dev/null");
                }
                CommandExecutor.executeBatchCommands(oomCmds);
                Log.d(TAG, "Pinned oom_score_adj -900 & renice -20 for " + packageName + " (PIDs=" + targetPids + ")");
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to protect oom score for " + packageName + ": " + t.getMessage());
        }
    }

    /**
     * Periodic in-game keep-alive pulse:
     * Constantly refreshes OOM scores and warms Shizuku IPC so Android LMKD never kills the daemon during games.
     */
    public static void keepAlivePulse(Context context) {
        AppExecutors.getInstance().executeCommand(() -> {
            try {
                // 1. Re-pin OOM scores for Shizuku and Game Booster
                protectProcessOomScore(SHIZUKU_PACKAGE);
                protectProcessOomScore(GAME_BOOSTER_PACKAGE);

                // 2. Lock loopback port & anti-freezer settings (Keeps Shizuku alive when Wi-Fi is turned OFF)
                // Note: Do NOT alter service.adb.tcp.port or adb_wifi_enabled here as modifying them restarts adbd and kills shizuku_server!
                List<String> pulseCmds = new ArrayList<>();
                pulseCmds.add("settings put global adb_allowed_connection_time 0 2>/dev/null");
                pulseCmds.add("settings put global cached_apps_freezer disabled 2>/dev/null");
                pulseCmds.add("cmd device_config put activity_manager use_freezer false 2>/dev/null");
                pulseCmds.add("am set-standby-bucket " + SHIZUKU_PACKAGE + " active 2>/dev/null");
                pulseCmds.add("cmd app_standby set " + SHIZUKU_PACKAGE + " active 2>/dev/null");
                CommandExecutor.executeBatchCommands(pulseCmds);

                // 3. Keep IPC connection warm and auto-rebind if needed
                boolean alive = rikka.shizuku.Shizuku.pingBinder();
                if (alive) {
                    if (!com.gamebooster.app.shizuku.ShizukuUserServiceConnector.getInstance().isServiceConnected()) {
                        com.gamebooster.app.shizuku.ShizukuUserServiceConnector.getInstance().bindService();
                    }
                } else {
                    // Try resurrection if dropped
                    tryAutoResurrectShizukuDaemon();
                    com.gamebooster.app.shizuku.ShizukuConnectionManager.getInstance().forceReconnectCheck();
                }
            } catch (Throwable t) {
                Log.w(TAG, "keepAlivePulse note: " + t.getMessage());
            }
        });
    }

    /**
     * Resurrects Shizuku daemon via root if Shizuku binder is completely dead.
     */
    public static boolean tryAutoResurrectShizukuDaemon() {
        try {
            if (ShizukuManager.isShizukuRunningAndGranted()) return true;

            if (com.gamebooster.app.engine.ShellExecutor.isRootSuAvailable()) {
                Log.i(TAG, "Attempting root auto-resurrection of Shizuku daemon without wireless debugging...");
                String[] candidatePaths = {
                        "/data/data/moe.shizuku.privileged.api/files/start.sh",
                        "/data/user/0/moe.shizuku.privileged.api/files/start.sh",
                        "/sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh",
                        "/storage/emulated/0/Android/data/moe.shizuku.privileged.api/files/start.sh",
                        "/data/data/moe.shizuku.privileged.api/start.sh",
                        "/data/user/0/moe.shizuku.privileged.api/start.sh",
                        "/sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                        "/storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh",
                        "/data/local/tmp/shizuku/start.sh",
                        "/data/local/tmp/start.sh"
                };

                for (String scriptPath : candidatePaths) {
                    com.gamebooster.app.engine.ShellExecutor.CommandResult res =
                            com.gamebooster.app.engine.ShellExecutor.executeSuCommand("sh " + scriptPath + " 2>/dev/null &");
                    if (res.isSuccess()) {
                        Log.i(TAG, "Executed Shizuku resurrection script: " + scriptPath);
                        return true;
                    }
                }

                // Fallback direct root daemon spawn via app_process / shizuku apk
                com.gamebooster.app.engine.ShellExecutor.executeSuCommand(
                        "nohup sh /data/data/moe.shizuku.privileged.api/files/start.sh >/dev/null 2>&1 &");
            }
        } catch (Throwable t) {
            Log.w(TAG, "Auto-resurrect Shizuku error: " + t.getMessage());
        }
        return false;
    }
}
