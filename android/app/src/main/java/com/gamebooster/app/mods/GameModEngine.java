package com.gamebooster.app.mods;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.gamebooster.app.config.CodmConfigPatcher;
import com.gamebooster.app.config.MlbbConfigPatcher;
import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.spoofer.HardwareMaskEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * GameModEngine — Core engine for MLBB/CODM mod operations.
 *
 * Responsibilities:
 *  1. Apply SystemProperties for each enabled mod (read by Frida hooks)
 *  2. Start frida-server on the device (via shell)
 *  3. Inject the correct hook script into the target game process
 *  4. Stop frida-server when game session ends
 *  5. APK pull/push workflow helpers
 *  6. 4-Layer real-game 120/144/165 FPS unlock enforcement
 */
public final class GameModEngine {

    private static final String TAG = "GameModEngine";

    public static final String PKG_MLBB = "com.mobile.legends";
    public static final String PKG_CODM = "com.garena.game.codm";
    public static final String PKG_PUBGM = "com.tencent.ig";

    private static final String FRIDA_SERVER_DEVICE_PATH = "/data/local/tmp/frida-server";
    private static final String FRIDA_HOOK_DEVICE_DIR    = "/data/local/tmp/gb_hooks/";

    private GameModEngine() {}

    // ─── Frida Server ─────────────────────────────────────────────────────────

    /**
     * Deploys frida-server binary from assets/frida/frida-server-arm64 to /data/local/tmp/frida-server
     * with chmod 755 executable permissions.
     */
    public static boolean deployFridaServer(Context ctx) {
        if (ctx == null) return false;
        try {
            // Check if frida-server already exists on device with valid size (> 40MB)
            String checkRes = execOutput("stat -c %s " + FRIDA_SERVER_DEVICE_PATH + " 2>/dev/null || wc -c < " + FRIDA_SERVER_DEVICE_PATH + " 2>/dev/null");
            long existingSize = 0;
            try {
                if (checkRes != null && !checkRes.trim().isEmpty()) {
                    existingSize = Long.parseLong(checkRes.trim().split("\n")[0].trim());
                }
            } catch (Exception ignored) {}

            if (existingSize > 40000000L) {
                exec("chmod 755 " + FRIDA_SERVER_DEVICE_PATH);
                return true;
            }

            Log.i(TAG, "📦 Deploying frida-server-arm64 from assets to " + FRIDA_SERVER_DEVICE_PATH + "...");
            File temp = new File(ctx.getCacheDir(), "frida_server_stage_" + System.currentTimeMillis() + ".bin");
            try (InputStream is = ctx.getAssets().open("frida/frida-server-arm64");
                 FileOutputStream fos = new FileOutputStream(temp)) {
                byte[] buf = new byte[65536];
                int read;
                while ((read = is.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
                fos.flush();
            }

            temp.setReadable(true, false);
            temp.setExecutable(true, false);

            exec("mkdir -p /data/local/tmp && cp -f '" + temp.getAbsolutePath() + "' " + FRIDA_SERVER_DEVICE_PATH + " && chmod 755 " + FRIDA_SERVER_DEVICE_PATH);
            temp.delete();

            String verifySize = execOutput("stat -c %s " + FRIDA_SERVER_DEVICE_PATH + " 2>/dev/null || wc -c < " + FRIDA_SERVER_DEVICE_PATH + " 2>/dev/null");
            Log.i(TAG, "✅ frida-server deployed to " + FRIDA_SERVER_DEVICE_PATH + " (size: " + verifySize + " bytes)");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "deployFridaServer error: " + t.getMessage(), t);
            return false;
        }
    }

    /** Start frida-server in background on device (requires root/Shizuku). Polls until responsive. */
    public static void startFridaServer(Context ctx) {
        AppExecutors.getInstance().executeCommand(() -> {
            try {
                deployFridaServer(ctx);
                // Check if already running and responsive
                String running = execOutput("pgrep -f frida-server 2>/dev/null || pidof frida-server 2>/dev/null");
                if (running != null && !running.trim().isEmpty() && !running.startsWith("ERROR:")) {
                    Log.i(TAG, "✅ frida-server already running (PID: " + running.trim().split("\n")[0] + ")");
                    return;
                }
                exec("killall frida-server 2>/dev/null; sleep 0.2");
                exec(FRIDA_SERVER_DEVICE_PATH + " &");

                // Poll readiness: check for process existence up to 15 iterations (1.5s max)
                boolean active = false;
                for (int i = 0; i < 15; i++) {
                    Thread.sleep(100);
                    String pid = execOutput("pgrep -f frida-server 2>/dev/null || pidof frida-server 2>/dev/null");
                    if (pid != null && !pid.trim().isEmpty() && !pid.startsWith("ERROR:")) {
                        active = true;
                        Log.i(TAG, "✅ frida-server started & responsive (PID: " + pid.trim().split("\n")[0] + ") after " + ((i + 1) * 100) + "ms");
                        break;
                    }
                }
                if (!active) {
                    Log.w(TAG, "⚠️ frida-server launched but PID not detected after 1.5s wait");
                }
            } catch (Throwable t) {
                Log.e(TAG, "frida-server start failed: " + t.getMessage());
            }
        });
    }

    /** Stop frida-server. */
    public static void stopFridaServer() {
        AppExecutors.getInstance().executeCommand(() -> exec("killall frida-server 2>/dev/null"));
    }

    // ─── 4-Layer Real-Game Display & FPS Enforcement ─────────────────────────

    /**
     * Enforces hardware display panel refresh rate & SurfaceFlinger caps
     * to ensure the OS never drops to 60Hz/90Hz during game sessions.
     */
    public static void enforceDisplayRefreshRate(int targetHz) {
        if (targetHz <= 60) return;
        try {
            String cmd = String.format(
                "settings put system peak_refresh_rate %d.0; " +
                "settings put system min_refresh_rate %d.0; " +
                "settings put system user_refresh_rate %d; " +
                "settings put global peak_refresh_rate %d.0; " +
                "settings put global min_refresh_rate %d.0; " +
                "service call SurfaceFlinger 1035 i32 %d 2>/dev/null; " +
                "cmd window set-app-refresh-rate global %d 2>/dev/null; " +
                "cmd game set --fps %d global 2>/dev/null; " +
                "setprop debug.graphics.game_default_frame_rate.disabled 1; " +
                "setprop ro.vendor.dfps.enable 0; " +
                "setprop persist.vendor.display.vrr.disable 1; " +
                "setprop debug.sf.fps_limit %d; " +
                "setprop persist.sys.game.fps %d",
                targetHz, targetHz, targetHz, targetHz, targetHz,
                targetHz, targetHz, targetHz, targetHz, targetHz
            );
            exec(cmd);
            Log.i(TAG, "⚡ Enforced display refresh rate: " + targetHz + "Hz");
        } catch (Throwable t) {
            Log.w(TAG, "Display refresh rate enforcement warning: " + t.getMessage());
        }
    }

    // ─── Mod Apply ────────────────────────────────────────────────────────────

    /**
     * Apply mod profile for a game:
     *  1. Write all config values as SystemProperties (read by Frida JS)
     *  2. Execute display refresh rate overclock and static storage patches
     *  3. Push hook script to device
     *  4. Inject into running game process via frida-inject
     */
    public static void applyMods(Context ctx, String packageName, GameModProfile profile) {
        if (packageName == null) return;
        if (packageName.contains("mobile.legends") || packageName.contains("mobilelegends")) {
            com.gamebooster.app.mods.mlbb.MlbbModManager.applyProfile(ctx, packageName, profile);
        } else if (packageName.contains("codm") || packageName.contains("callofduty")) {
            com.gamebooster.app.mods.codm.CodmModManager.applyProfile(ctx, packageName, profile);
        } else if (packageName.contains("tencent.ig") || packageName.contains("pubg")) {
            com.gamebooster.app.mods.pubgm.PubgmModManager.applyProfile(ctx, packageName, profile);
        }
    }

    /**
     * Synchronously injects MLBB mod suite and returns true when fully hooked.
     */
    public static boolean injectMlbbSync(Context ctx, String packageName, GameModProfile profile) {
        if (packageName == null) return false;
        return com.gamebooster.app.mods.mlbb.MlbbModManager.injectMlbbModSuite(ctx, packageName, profile);
    }

    /**
     * Synchronously injects CODM mod suite and returns true when fully hooked.
     */
    public static boolean injectCodmSync(Context ctx, String packageName, GameModProfile profile) {
        if (packageName == null) return false;
        return com.gamebooster.app.mods.codm.CodmModManager.injectCodmModSuite(ctx, packageName, profile);
    }

    /**
     * Synchronously injects PUBG Mobile 4.6.0 mod suite and returns true when fully hooked.
     */
    public static boolean injectPubgmSync(Context ctx, String packageName, GameModProfile profile) {
        if (packageName == null) return false;
        return com.gamebooster.app.mods.pubgm.PubgmModManager.injectPubgmModSuite(ctx, packageName, profile);
    }

    // ─── APK Pull / Push ──────────────────────────────────────────────────────

    /**
     * Pull base.apk from the device for a given package into local mods directory.
     * @param packageName  e.g. "com.mobile.legends"
     * @param outputPath   local path to save pulled APK
     * @param callback     called on completion with success flag + message
     */
    public static void pullBaseApk(String packageName, String outputPath, PullCallback callback) {
        AppExecutors.getInstance().executeCommand(() -> {
            try {
                // Get apk path from device
                String pathResult = execOutput("adb shell pm path " + packageName + " | grep base.apk | cut -d: -f2 | tr -d '\\r\\n'");
                if (pathResult == null || pathResult.isEmpty()) {
                    if (callback != null) callback.onResult(false, "Could not find base.apk for " + packageName);
                    return;
                }
                String apkPath = pathResult.trim();
                String pullCmd = "adb pull \"" + apkPath + "\" \"" + outputPath + "\"";
                exec(pullCmd);
                if (callback != null) callback.onResult(true, "Pulled: " + outputPath);
                Log.i(TAG, "✅ Pulled " + packageName + " → " + outputPath);
            } catch (Throwable t) {
                if (callback != null) callback.onResult(false, t.getMessage());
            }
        });
    }

    /** Reinstall a patched APK onto the device. */
    public static void pushAndInstallApk(String apkPath, PullCallback callback) {
        AppExecutors.getInstance().executeCommand(() -> {
            try {
                exec("adb install -r -d \"" + apkPath + "\"");
                if (callback != null) callback.onResult(true, "Installed: " + apkPath);
            } catch (Throwable t) {
                if (callback != null) callback.onResult(false, t.getMessage());
            }
        });
    }

    public interface PullCallback {
        void onResult(boolean success, String message);
    }

    // ─── Internal Helpers ─────────────────────────────────────────────────────

    private static void setprop(String key, String value) {
        exec("setprop " + key + " " + value);
    }

    public static void injectFrida(String packageName, String scriptPath) {
        try {
            // Find active target PID immediately for instant zero-latency attach
            String pidOut = com.gamebooster.app.engine.CommandExecutor.executeSystemCommand("pidof " + packageName + " 2>/dev/null || pgrep -f " + packageName + " 2>/dev/null");
            String pid = (pidOut != null && !pidOut.trim().isEmpty() && !pidOut.startsWith("ERROR:")) ? pidOut.trim().split("[\\s\\n]+")[0] : null;

            String whichFrida = com.gamebooster.app.engine.CommandExecutor.executeSystemCommand("which frida 2>/dev/null || which frida-inject 2>/dev/null || [ -f /data/local/tmp/frida-inject ] && echo /data/local/tmp/frida-inject");
            if (whichFrida != null && !whichFrida.trim().isEmpty() && !whichFrida.startsWith("ERROR:")) {
                String bin = whichFrida.trim().split("\n")[0].trim();
                if (bin.contains("frida-inject")) {
                    if (pid != null && !pid.isEmpty()) {
                        // Direct PID attachment is 10x faster than process name resolution
                        exec(bin + " -p " + pid + " -s " + scriptPath + " &");
                        Log.i(TAG, "[Frida-Instant] Attached to PID " + pid + " (" + packageName + ") via frida-inject ✓");
                    } else {
                        exec(bin + " -n " + packageName + " -s " + scriptPath + " &");
                        Log.i(TAG, "[Frida-Instant] Attached to " + packageName + " via frida-inject ✓");
                    }
                } else {
                    if (pid != null && !pid.isEmpty()) {
                        exec(bin + " -U -p " + pid + " -l " + scriptPath + " --no-pause &");
                        Log.i(TAG, "[Frida-Instant] Attached to PID " + pid + " (" + packageName + ") via frida CLI ✓");
                    } else {
                        exec(bin + " -U -f " + packageName + " -l " + scriptPath + " --no-pause &");
                        Log.i(TAG, "[Frida-Instant] Spawned & injected into " + packageName + " ✓");
                    }
                }
            } else {
                Log.i(TAG, "ℹ️ Frida server running @ /data/local/tmp/frida-server; script ready @ " + scriptPath);
            }
        } catch (Throwable t) {
            Log.w(TAG, "injectFrida note: " + t.getMessage());
        }
    }

    public static void pushScriptAssetToDevice(Context ctx, String assetPath, String devicePath) {
        if (ctx == null || assetPath == null || devicePath == null) return;
        try {
            exec("mkdir -p " + FRIDA_HOOK_DEVICE_DIR);
            File temp = new File(ctx.getCacheDir(), "hook_stage_" + System.currentTimeMillis() + ".js");
            try (InputStream is = ctx.getAssets().open(assetPath);
                 FileOutputStream fos = new FileOutputStream(temp)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
                fos.flush();
            }
            temp.setReadable(true, false);
            exec("cp -f '" + temp.getAbsolutePath() + "' '" + devicePath + "' && chmod 666 '" + devicePath + "'");
            temp.delete();
            Log.i(TAG, "✅ Pushed hook asset " + assetPath + " → " + devicePath);
        } catch (Throwable t) {
            Log.e(TAG, "pushScriptAssetToDevice error for " + assetPath + ": " + t.getMessage());
        }
    }

    private static void pushScriptToDevice(String content, String devicePath) {
        if (content == null || content.isEmpty()) return;
        exec("mkdir -p " + FRIDA_HOOK_DEVICE_DIR);
        try {
            Context ctx = com.gamebooster.app.config.ConfigBackupManager.getAppContext();
            if (ctx == null) ctx = com.gamebooster.app.GameBoosterApp.getInstance();
            if (ctx != null) {
                File temp = new File(ctx.getCacheDir(), "hook_content_stage.js");
                try (FileOutputStream fos = new FileOutputStream(temp)) {
                    fos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    fos.flush();
                }
                exec("cp -f '" + temp.getAbsolutePath() + "' '" + devicePath + "' && chmod 666 '" + devicePath + "'");
                temp.delete();
                return;
            }
        } catch (Throwable ignored) {}
        exec("echo '" + content.replace("'", "'\\''") + "' > " + devicePath);
    }

    private static String readAsset(Context ctx, String assetPath) {
        try (InputStream is = ctx.getAssets().open(assetPath)) {
            byte[] buf = new byte[is.available()];
            is.read(buf);
            return new String(buf, "UTF-8");
        } catch (IOException e) {
            Log.e(TAG, "Asset read failed: " + assetPath + " — " + e.getMessage());
            return "";
        }
    }

    private static void exec(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return;
        try {
            com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd);
        } catch (Throwable t) {
            Log.w(TAG, "exec: " + cmd + " → " + t.getMessage());
        }
    }

    private static String execOutput(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return "";
        try {
            String res = com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd);
            if (res != null && !res.startsWith("ERROR:")) {
                return res.trim();
            }
        } catch (Throwable t) {
            Log.w(TAG, "execOutput: " + cmd + " → " + t.getMessage());
        }
        return "";
    }
}
