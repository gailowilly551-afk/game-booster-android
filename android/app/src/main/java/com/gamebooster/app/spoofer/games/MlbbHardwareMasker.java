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

            // 5. Deploy directly to Dragon2017 and dragon2017 Document folders
            String[] docRoots = {
                "/storage/emulated/0/Android/data/" + pkg + "/files/dragon2017/assets/Document",
                "/storage/emulated/0/Android/data/" + pkg + "/files/dragon2017/assets/Document/android",
                "/storage/emulated/0/Android/data/" + pkg + "/files/Dragon2017/assets/Document",
                "/storage/emulated/0/Android/data/" + pkg + "/files/Dragon2017/assets/Document/android",
                "/sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document",
                "/sdcard/Android/data/" + pkg + "/files/dragon2017/assets/Document/android",
                "/sdcard/Android/data/" + pkg + "/files/Dragon2017/assets/Document",
                "/sdcard/Android/data/" + pkg + "/files/Dragon2017/assets/Document/android"
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

            // 5b. Deploy into dynamically discovered MLBB mini-patch slots so in-game updates never bypass hardware spoofing
            try {
                List<String> miniPatchSlots = MlbbDroneViewPatcher.discoverActiveMiniPatchSlots(pkg);
                for (String slot : miniPatchSlots) {
                    String slotDoc = slot + "/assets/Document";
                    String slotDocAndroid = slot + "/assets/Document/android";
                    ShizukuFileManager.ensureParentDirectory(slotDocAndroid + "/DeviceHardware.ini");
                    ShizukuFileManager.writeFile(slotDoc + "/DeviceHardware.ini", mlbbIni, "666");
                    ShizukuFileManager.writeFile(slotDocAndroid + "/DeviceHardware.ini", mlbbIni, "666");
                    ShizukuFileManager.writeFile(slotDoc + "/QualityConfig.json", jsonProfile, "666");
                    ShizukuFileManager.writeFile(slotDocAndroid + "/QualityConfig.json", jsonProfile, "666");
                    ShizukuFileManager.writeFile(slotDoc + "/GraphicSetting.json", jsonProfile, "666");
                    ShizukuFileManager.writeFile(slotDocAndroid + "/GraphicSetting.json", jsonProfile, "666");
                    ShizukuFileManager.writeFile(slotDoc + "/HighFPSConfig.json", jsonProfile, "666");
                    ShizukuFileManager.writeFile(slotDocAndroid + "/HighFPSConfig.json", jsonProfile, "666");
                }
            } catch (Throwable t) {
                Log.w(TAG, "miniPatchSlots write warning: " + t.getMessage());
            }

            // 6. Elevated Shizuku shell write fallback across both case variants
            try {
                byte[] iniBytes = mlbbIni.getBytes(StandardCharsets.UTF_8);
                String iniB64 = Base64.encodeToString(iniBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");
                byte[] jsonBytes = jsonProfile.getBytes(StandardCharsets.UTF_8);
                String jsonB64 = Base64.encodeToString(jsonBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");

                String prefsInt =
                    "HighFPSMode:3\n" +
                    "high_fps_mode:3\n" +
                    "HighFPSEnable:1\n" +
                    "FrameRateLevel:3\n" +
                    "__FrameRateLevel__:3\n" +
                    "__MaxFrameRateLevel__:3\n" +
                    "fps_level:3\n" +
                    "UltraFrameRate:1\n" +
                    "SuperFrameRate:1\n" +
                    "HighFrameRate:1\n" +
                    "Unlock185Hz:1\n" +
                    "Unlock165Hz:1\n" +
                    "Unlock144Hz:1\n" +
                    "Unlock120Hz:1\n" +
                    "Unlock90Hz:1\n" +
                    "FPS:" + targetFps + "\n" +
                    "MaxFPS:" + targetFps + "\n" +
                    "TargetFPS:" + targetFps + "\n" +
                    "FrameRateLimit:" + targetFps + "\n" +
                    "QualitySetting:3\n" +
                    "GraphicLevel:3\n" +
                    "QualityLevel:3\n" +
                    "GraphicsQuality:3\n" +
                    "ShadowQuality:2\n" +
                    "HDMode:1\n" +
                    "CreepOutline:1\n";
                String prefsB64 = Base64.encodeToString(prefsInt.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP).replace("\n", "").replace("\r", "");

                StringBuilder shizukuCmd = new StringBuilder();
                String[] basePrefixes = {
                    "/storage/emulated/0/Android/data/" + pkg + "/files",
                    "/sdcard/Android/data/" + pkg + "/files"
                };

                for (String base : basePrefixes) {
                    for (String dName : new String[]{"dragon2017", "Dragon2017"}) {
                        String docAnd = base + "/" + dName + "/assets/Document/android";
                        String doc = base + "/" + dName + "/assets/Document";
                        String ast = base + "/" + dName + "/assets";

                        shizukuCmd.append("mkdir -p '").append(docAnd).append("' '").append(doc).append("' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(iniB64).append("' | base64 -d > '").append(doc).append("/DeviceHardware.ini' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(doc).append("/DeviceHardware.ini' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(iniB64).append("' | base64 -d > '").append(docAnd).append("/DeviceHardware.ini' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(docAnd).append("/DeviceHardware.ini' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(jsonB64).append("' | base64 -d > '").append(doc).append("/GraphicSetting.json' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(doc).append("/GraphicSetting.json' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(jsonB64).append("' | base64 -d > '").append(docAnd).append("/GraphicSetting.json' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(docAnd).append("/GraphicSetting.json' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(jsonB64).append("' | base64 -d > '").append(doc).append("/HighFPSConfig.json' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(doc).append("/HighFPSConfig.json' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(jsonB64).append("' | base64 -d > '").append(docAnd).append("/HighFPSConfig.json' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(docAnd).append("/HighFPSConfig.json' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(jsonB64).append("' | base64 -d > '").append(doc).append("/QualityConfig.json' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(doc).append("/QualityConfig.json' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(jsonB64).append("' | base64 -d > '").append(docAnd).append("/QualityConfig.json' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(docAnd).append("/QualityConfig.json' 2>/dev/null; ");
                        shizukuCmd.append("echo '").append(prefsB64).append("' | base64 -d > '").append(ast).append("/prefs_int' 2>/dev/null; ");
                        shizukuCmd.append("chmod 666 '").append(ast).append("/prefs_int' 2>/dev/null; ");
                    }
                }

                // Patch playerprefs XML in both /data/data and /sdcard/Android/data
                String[] prefFiles = {
                    "/data/data/" + pkg + "/shared_prefs/" + pkg + ".v2.playerprefs.xml",
                    "/data/data/" + pkg + "/shared_prefs/" + pkg + ".xml",
                    "/storage/emulated/0/Android/data/" + pkg + "/files/shared_prefs/" + pkg + ".v2.playerprefs.xml",
                    "/sdcard/Android/data/" + pkg + "/files/shared_prefs/" + pkg + ".v2.playerprefs.xml"
                };
                String[] mlbbXmlKeys = {
                    "<string name=\"SystemInfo_deviceModel\">" + profile.model + "</string>",
                    "<string name=\"SystemInfo_graphicsDeviceName\">" + profile.glRenderer + "</string>",
                    "<string name=\"SystemInfo_graphicsDeviceVendor\">" + profile.glVendor + "</string>",
                    "<string name=\"SystemInfo_processorType\">" + profile.socModel + "</string>",
                    "<string name=\"DeviceModel\">" + profile.model + "</string>",
                    "<int name=\"HighFpsMode\" value=\"3\" />",
                    "<int name=\"HighFPSMode\" value=\"3\" />",
                    "<int name=\"high_fps_mode\" value=\"3\" />",
                    "<int name=\"FrameRateLevel\" value=\"3\" />",
                    "<int name=\"__FrameRateLevel__\" value=\"3\" />",
                    "<int name=\"__MaxFrameRateLevel__\" value=\"3\" />",
                    "<int name=\"FPS\" value=\"" + targetFps + "\" />",
                    "<int name=\"TargetFrameRate\" value=\"" + targetFps + "\" />",
                    "<int name=\"GraphicLevel\" value=\"3\" />",
                    "<int name=\"QualitySetting\" value=\"3\" />"
                };
                for (String pref : prefFiles) {
                    try {
                        com.gamebooster.app.config.ConfigFileHelper.patchKeys(pref, mlbbXmlKeys, "<map>");
                    } catch (Throwable ignored) {}
                    shizukuCmd.append("if [ -f '").append(pref).append("' ]; then ");
                    shizukuCmd.append("sed -i 's/<int name=\"HighFpsMode\" value=\"[0-9]*\" \\/>/<int name=\"HighFpsMode\" value=\"3\" \\/>/g' '").append(pref).append("' 2>/dev/null; ");
                    shizukuCmd.append("sed -i 's/<int name=\"FPS\" value=\"[0-9]*\" \\/>/<int name=\"FPS\" value=\"").append(targetFps).append("\" \\/>/g' '").append(pref).append("' 2>/dev/null; ");
                    shizukuCmd.append("sed -i 's/<int name=\"TargetFrameRate\" value=\"[0-9]*\" \\/>/<int name=\"TargetFrameRate\" value=\"").append(targetFps).append("\" \\/>/g' '").append(pref).append("' 2>/dev/null; ");
                    shizukuCmd.append("sed -i 's/<int name=\"GraphicLevel\" value=\"[0-9]*\" \\/>/<int name=\"GraphicLevel\" value=\"3\" \\/>/g' '").append(pref).append("' 2>/dev/null; ");
                    shizukuCmd.append("chmod 666 '").append(pref).append("' 2>/dev/null; ");
                    shizukuCmd.append("fi; ");
                }

                com.gamebooster.app.engine.PrivilegeBridgeEngine.executePrivileged(shizukuCmd.toString());
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
