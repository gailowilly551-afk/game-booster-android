package com.gamebooster.app.engine;

import android.content.Context;
import android.util.Log;
import com.gamebooster.app.config.ConfigFileHelper;
import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.MlbbConfigPatcher;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.shizuku.ShizukuExecutor;

import java.util.List;

/**
 * MlbbFpsUnlockEngine — Dedicated, Isolated 165 FPS Unlocker for Mobile Legends: Bang Bang.
 * 
 * Handles all MLBB-specific:
 *  - PlayerPrefs XML frame rate levels (FrameRateLevel 6 = 165/185 FPS Ultra Extreme)
 *  - HighFPSMode 6 and HighFPSEnable overrides
 *  - Unity boot.config target-frame-rate=165 injection
 *  - Real-time native memory bypasses for MLBB FPS limiters
 */
public final class MlbbFpsUnlockEngine {

    private static final String TAG = "MlbbFpsUnlockEngine";

    private MlbbFpsUnlockEngine() {}

    /**
     * Unlocks 165 FPS exclusively for MLBB across all config paths, PlayerPrefs, and native engine limits.
     */
    public static boolean unlock165Fps(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            packageName = "com.mobile.legends";
        }
        final String pkg = packageName.trim();
        final int targetFps = 165;
        Log.i(TAG, "⚡ [MlbbFpsUnlockEngine] Unlocking dedicated 165 FPS for " + pkg);

        boolean anySuccess = false;

        // 1. Apply MLBB specialized 165 FPS preset
        try {
            boolean pOk = MlbbConfigPatcher.patchUltraExtreme165(pkg);
            if (pOk) anySuccess = true;
        } catch (Throwable t) {
            Log.w(TAG, "MlbbConfigPatcher.patchUltraExtreme165 note: " + t.getMessage());
        }

        // 2. High-speed native JNI graphics injection (FPS 165, HighFPSMode 6, FrameRateLevel 6)
        try {
            List<String> paths = MlbbConfigPatcher.getConfigPaths(pkg);
            for (String path : paths) {
                if (NativeConfigInjector.injectMlbb165FpsGraphics(path, targetFps, 6)) {
                    anySuccess = true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "NativeConfigInjector.injectMlbb165FpsGraphics note: " + t.getMessage());
        }

        // 3. Shell / Shizuku PlayerPrefs & SharedPrefs injection
        String[] prefDirs = {
            "/data/data/" + pkg + "/shared_prefs",
            "/data/user/0/" + pkg + "/shared_prefs",
            "/sdcard/Android/data/" + pkg + "/shared_prefs",
            "/storage/emulated/0/Android/data/" + pkg + "/shared_prefs"
        };
        StringBuilder sb = new StringBuilder();
        for (String dir : prefDirs) {
            String p1 = dir + "/" + pkg + ".v4.playerprefs.xml";
            String p2 = dir + "/" + pkg + ".v3.playerprefs.xml";
            String p3 = dir + "/" + pkg + ".v2.playerprefs.xml";
            String p4 = dir + "/" + pkg + "_preferences.xml";
            String[] xmlFiles = {p1, p2, p3, p4};
            for (String prefs : xmlFiles) {
                sb.append("if [ -f '").append(prefs).append("' ]; then ")
                  .append("sed -i 's/name=\"HighFPSMode\" value=\"[0-9]*\"/name=\"HighFPSMode\" value=\"6\"/g' '").append(prefs).append("' 2>/dev/null; ")
                  .append("sed -i 's/name=\"HighFPSEnable\" value=\"[0-9]*\"/name=\"HighFPSEnable\" value=\"1\"/g' '").append(prefs).append("' 2>/dev/null; ")
                  .append("sed -i 's/name=\"FrameRateLevel\" value=\"[0-9]*\"/name=\"FrameRateLevel\" value=\"6\"/g' '").append(prefs).append("' 2>/dev/null; ")
                  .append("sed -i 's/name=\"MaxFPS\" value=\"[0-9]*\"/name=\"MaxFPS\" value=\"165\"/g' '").append(prefs).append("' 2>/dev/null; ")
                  .append("sed -i 's/name=\"FrameRateLimit\" value=\"[0-9]*\"/name=\"FrameRateLimit\" value=\"165\"/g' '").append(prefs).append("' 2>/dev/null; ")
                  .append("sed -i 's/name=\"Unlock165Hz\" value=\"[0-9]*\"/name=\"Unlock165Hz\" value=\"1\"/g' '").append(prefs).append("' 2>/dev/null; ")
                  .append("sed -i 's/name=\"Unlock165FPS\" value=\"[0-9]*\"/name=\"Unlock165FPS\" value=\"1\"/g' '").append(prefs).append("' 2>/dev/null; ")
                  .append("grep -q 'Unlock165Hz' '").append(prefs).append("' || sed -i 's|</map>|<int name=\"Unlock165Hz\" value=\"1\" /></map>|' '").append(prefs).append("' 2>/dev/null; ")
                  .append("grep -q 'Unlock165FPS' '").append(prefs).append("' || sed -i 's|</map>|<int name=\"Unlock165FPS\" value=\"1\" /></map>|' '").append(prefs).append("' 2>/dev/null; ")
                  .append("grep -q 'targetFrameRate' '").append(prefs).append("' || sed -i 's|</map>|<int name=\"targetFrameRate\" value=\"165\" /></map>|' '").append(prefs).append("' 2>/dev/null; ")
                  .append("fi; ");
            }
        }
        executeShell(sb.toString());

        // 4. Native MLBB Unity 185/165 FPS patch trigger
        try {
            NativeConfigInjector.nativeApplyMlbb185FpsPatch();
        } catch (Throwable ignored) {}

        Log.i(TAG, "✅ [MlbbFpsUnlockEngine] MLBB 165 FPS unlock pipeline executed for " + pkg);
        return anySuccess;
    }

    private static void executeShell(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return;
        try {
            if (ShizukuExecutor.hasShizukuPermission()) {
                ShizukuExecutor.executeShizukuCommand(cmd);
            } else {
                CommandExecutor.executeSystemCommand(cmd);
            }
        } catch (Throwable ignored) {}
    }
}
