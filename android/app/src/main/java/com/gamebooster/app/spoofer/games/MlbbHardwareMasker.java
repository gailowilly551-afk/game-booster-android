package com.gamebooster.app.spoofer.games;

import android.util.Base64;
import android.util.Log;
import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.GameSecurityBypassEngine;
import com.gamebooster.app.config.MlbbConfigPatcher;
import com.gamebooster.app.config.MlbbDroneViewPatcher;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.shizuku.ShizukuFileManager;
import com.gamebooster.app.spoofer.SpoofProfile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MlbbHardwareMasker — Dedicated Hardware Masker & Profile Injector for Mobile Legends: Bang Bang.
 *
 * Exclusively manages:
 *  - Unity IL2CPP / Dragon2017 Document assets and directories
 *  - DeviceHardware.ini, QualityConfig.json, GraphicSetting.json, HighFPSConfig.json, ResolutionConfig.json
 *  - MLBB shared prefs & boot.config
 *  - Zero crossover with CODM or PUBGM configs.
 */
public final class MlbbHardwareMasker {

    private static final String TAG = "MlbbHardwareMasker";

    private MlbbHardwareMasker() {}

    public static boolean inject(String packageName, SpoofProfile profile) {
        if (packageName == null || profile == null) return false;
        final String pkg = packageName.trim().toLowerCase();
        final int targetFps = profile.maxRefreshRateHz > 0 ? profile.maxRefreshRateHz : 185;

        Log.i(TAG, "⚡ [MlbbHardwareMasker] Injecting dedicated MLBB hardware profile for " + pkg + " @ " + targetFps + " FPS");

        try {
            // 1. Purge legacy loading locks and corrupted cache files
            GameSecurityBypassEngine.purgeCorruptedAssetCaches(pkg);
            MlbbConfigPatcher.cleanLegacyLoadingLocks(pkg);

            // 2. Apply tier-specific MLBB configuration patches
            if (targetFps >= 185) {
                MlbbConfigPatcher.patchUltraExtreme185(pkg);
            } else if (targetFps >= 165) {
                MlbbConfigPatcher.patchUltraExtreme165(pkg);
            } else if (targetFps >= 144) {
                MlbbConfigPatcher.patchUltraExtreme144(pkg);
            } else {
                MlbbConfigPatcher.patch(pkg, targetFps);
            }

            MlbbConfigPatcher.applyMlbbPrefsIntAndBootConfig(pkg, targetFps);

            try {
                MlbbDroneViewPatcher.applyDroneViewAtomic(pkg, targetFps);
            } catch (Throwable t) {
                Log.w(TAG, "MLBB drone view sync note: " + t.getMessage());
            }

            // 3. Generate MLBB-specific config content
            String mlbbIni = profile.generateMlbbDeviceConfig(targetFps);
            String jsonProfile = profile.generateJsonHardwareProfile(targetFps);

            // 4. Resolve and inject into dynamically discovered MLBB paths
            List<String> paths = GameConfigPathResolver.getPathsForGame(pkg);
            for (String p : paths) {
                if (p.contains("playerprefs") || p.endsWith(".xml")) {
                    NativeConfigInjector.injectHardwareMaskProfile(p, profile, targetFps);
                } else if (p.endsWith("boot.config")) {
                    MlbbConfigPatcher.cleanLegacyLoadingLocks(pkg);
                } else if (p.endsWith(".json")) {
                    if (!p.contains("DroneView")) {
                        NativeConfigInjector.injectHardwareMaskProfile(p, profile, targetFps);
                    }
                } else if (p.contains("DeviceHardware") || p.endsWith(".ini")) {
                    ShizukuFileManager.ensureParentDirectory(p);
                    ShizukuFileManager.writeFile(p, mlbbIni, "666");
                }
            }

            // 5. Deploy directly to Dragon2017 Document folders
            String[] docRoots = {
                "/storage/emulated/0/Android/data/" + pkg + "/files/dragon2017/assets/Document",
                "/storage/emulated/0/Android/data/" + pkg + "/files/dragon2017/assets/Document/android",
                "/sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document",
                "/sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/android"
            };

            for (String docRoot : docRoots) {
                try {
                    ShizukuFileManager.ensureParentDirectory(docRoot + "/DeviceHardware.ini");
                    ShizukuFileManager.writeFile(docRoot + "/DeviceHardware.ini", mlbbIni, "666");
                    ShizukuFileManager.writeFile(docRoot + "/QualityConfig.json", jsonProfile, "666");
                    ShizukuFileManager.writeFile(docRoot + "/GraphicSetting.json", jsonProfile, "666");
                    ShizukuFileManager.writeFile(docRoot + "/HighFPSConfig.json", jsonProfile, "666");
                    ShizukuFileManager.writeFile(docRoot + "/ResolutionConfig.json", jsonProfile, "666");
                } catch (Throwable t) {
                    Log.w(TAG, "docRoot write exception for " + docRoot + ": " + t.getMessage());
                }
            }

            // 6. Elevated Shizuku shell write fallback
            try {
                byte[] iniBytes = mlbbIni.getBytes(StandardCharsets.UTF_8);
                String iniB64 = Base64.encodeToString(iniBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");
                byte[] jsonBytes = jsonProfile.getBytes(StandardCharsets.UTF_8);
                String jsonB64 = Base64.encodeToString(jsonBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");

                String shizukuFallbackCmd =
                    "mkdir -p /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/android 2>/dev/null; " +
                    "echo '" + iniB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/DeviceHardware.ini 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/DeviceHardware.ini 2>/dev/null; " +
                    "echo '" + iniB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/android/DeviceHardware.ini 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/android/DeviceHardware.ini 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/GraphicSetting.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/GraphicSetting.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/HighFPSConfig.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/HighFPSConfig.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/ResolutionConfig.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/ResolutionConfig.json 2>/dev/null";
                ShizukuExecutor.executeShizukuCommand(shizukuFallbackCmd);
            } catch (Throwable ignored) {}

            GameSecurityBypassEngine.enforceSelinuxAndOwnershipBypass(pkg, paths);
            Log.i(TAG, "✅ [MlbbHardwareMasker] MLBB hardware profile deployed successfully for " + pkg);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "❌ [MlbbHardwareMasker] Injection failed: " + t.getMessage(), t);
            return false;
        }
    }
}
