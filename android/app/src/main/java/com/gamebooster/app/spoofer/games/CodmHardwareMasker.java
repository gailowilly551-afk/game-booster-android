package com.gamebooster.app.spoofer.games;

import android.util.Base64;
import android.util.Log;
import com.gamebooster.app.config.ConfigFileHelper;
import com.gamebooster.app.config.CodmAceBypassPatcher;
import com.gamebooster.app.config.CodmConfigPatcher;
import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.GameSecurityBypassEngine;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.shizuku.ShizukuFileManager;
import com.gamebooster.app.spoofer.SpoofProfile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CodmHardwareMasker — Dedicated Hardware Masker & Profile Injector for Call of Duty: Mobile.
 *
 * Exclusively manages:
 *  - Unity IW8 / Config directories (/files/Config/HardwareProfile.json, device_profile.json)
 *  - ACE Anti-cheat suppression and MSDK / Garena playerprefs XML overrides
 *  - Unity boot.config frame rate directives
 *  - Zero crossover with MLBB or PUBGM configs.
 */
public final class CodmHardwareMasker {

    private static final String TAG = "CodmHardwareMasker";

    private CodmHardwareMasker() {}

    public static boolean inject(String packageName, SpoofProfile profile) {
        if (packageName == null || profile == null) return false;
        final String pkg = packageName.trim().toLowerCase();
        final int targetFps = profile.maxRefreshRateHz > 0 ? profile.maxRefreshRateHz : 185;

        Log.i(TAG, "⚡ [CodmHardwareMasker] Injecting dedicated CODM hardware profile for " + pkg + " @ " + targetFps + " FPS");

        try {
            // 1. Generate CODM-tailored JSON and INI profiles
            String jsonProfile = profile.generateJsonHardwareProfile(targetFps);

            // 2. Discover and patch dynamic CODM file paths
            List<String> paths = GameConfigPathResolver.getPathsForGame(pkg);
            for (String p : paths) {
                if (p.contains("HardwareProfile.json") || p.contains("device_profile.json") || p.endsWith(".json")) {
                    if (!NativeConfigInjector.injectHardwareMaskProfile(p, profile, targetFps)) {
                        ShizukuFileManager.ensureParentDirectory(p);
                        ShizukuFileManager.writeFile(p, jsonProfile, "666");
                    }
                } else if (p.endsWith("boot.config")) {
                    NativeConfigInjector.injectUnityBootConfig(p, targetFps);
                } else if (p.endsWith(".xml")) {
                    NativeConfigInjector.injectHardwareMaskProfile(p, profile, targetFps);
                } else if (p.endsWith(".ini")) {
                    String[] profileKeys = profile.generateUe4DeviceProfileKeys(targetFps);
                    ConfigFileHelper.patchKeys(p, profileKeys, "[DeviceProfile]");
                    ConfigFileHelper.patchKeys(p, profileKeys, "[/Script/ShadowTrackerExtra.UserSetting]");
                }
            }

            // 3. Guaranteed Shizuku elevated shell write fallback for CODM profiles
            try {
                byte[] jsonBytes = jsonProfile.getBytes(StandardCharsets.UTF_8);
                String jsonB64 = Base64.encodeToString(jsonBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");
                String codFallbackCmd =
                    "mkdir -p /sdcard/Android/data/" + pkg + "/files/Config 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/Config/HardwareProfile.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/Config/HardwareProfile.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/HardwareProfile.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/HardwareProfile.json 2>/dev/null";
                ShizukuExecutor.executeShizukuCommand(codFallbackCmd);
            } catch (Throwable ignored) {}

            // 4. Synchronize with CODM asset engine & apply FPS tiers
            try {
                CodmConfigPatcher.deployCodmAssets(pkg);
                if (targetFps >= 185) {
                    CodmConfigPatcher.patchUltraExtreme185(pkg);
                } else if (targetFps >= 165) {
                    CodmConfigPatcher.patchUltraExtreme165(pkg);
                } else if (targetFps >= 144) {
                    CodmConfigPatcher.patchUltraExtreme144(pkg);
                }
                CodmConfigPatcher.patch(pkg, targetFps);
                CodmAceBypassPatcher.applyBypass(pkg);
            } catch (Throwable t) {
                Log.w(TAG, "CodmConfigPatcher sync error: " + t.getMessage());
            }

            GameSecurityBypassEngine.enforceSelinuxAndOwnershipBypass(pkg, paths);
            Log.i(TAG, "✅ [CodmHardwareMasker] CODM hardware profile deployed successfully for " + pkg);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "❌ [CodmHardwareMasker] Injection failed: " + t.getMessage(), t);
            return false;
        }
    }
}
