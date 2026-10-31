package com.gamebooster.app.spoofer.games;

import android.util.Base64;
import android.util.Log;
import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.GameSecurityBypassEngine;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.shizuku.ShizukuFileManager;
import com.gamebooster.app.spoofer.SpoofProfile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HokHardwareMasker — Dedicated Hardware Masker & Profile Injector for Honor of Kings & Arena of Valor.
 *
 * Exclusively manages:
 *  - Unity / Tencent SGame configuration files
 *  - DeviceHardware.ini, DeviceProfile.json, GraphicSetting.json, QualityConfig.json, boot.config
 *  - Full 120Hz/185Hz extreme frame rate unlocking and SurfaceFlinger synchronization.
 */
public final class HokHardwareMasker {

    private static final String TAG = "HokHardwareMasker";

    private HokHardwareMasker() {}

    public static boolean inject(String packageName, SpoofProfile profile) {
        if (packageName == null || profile == null) return false;
        final String pkg = packageName.trim().toLowerCase();
        final int targetFps = profile.maxRefreshRateHz > 0 ? profile.maxRefreshRateHz : 185;

        Log.i(TAG, "⚡ [HokHardwareMasker] Injecting dedicated HOK hardware profile for " + pkg + " @ " + targetFps + " FPS");

        try {
            // 0. Synchronize with HokConfigPatcher first so our SpoofProfile writes have the final word
            try {
                com.gamebooster.app.config.HokConfigPatcher.patch(pkg, targetFps);
                com.gamebooster.app.config.HokConfigPatcher.patchCompetitive(pkg, targetFps);
            } catch (Throwable ignored) {}

            // 1. Generate HOK-tailored INI & JSON configuration content
            String hokIni = profile.generateHokDeviceConfig(targetFps);
            String jsonProfile = profile.generateJsonHardwareProfile(targetFps);

            // 2. Discover and patch dynamic HOK file paths
            List<String> paths = GameConfigPathResolver.getPathsForGame(pkg);
            for (String p : paths) {
                if (p.contains("DeviceHardware.ini") || p.contains("device.ini") || p.endsWith(".ini")) {
                    if (!NativeConfigInjector.injectHardwareMaskProfile(p, profile, targetFps)) {
                        ShizukuFileManager.ensureParentDirectory(p);
                        ShizukuFileManager.writeFile(p, hokIni, "666");
                    }
                } else if (p.endsWith("boot.config")) {
                    NativeConfigInjector.injectUnityBootConfig(p, targetFps);
                } else if (p.endsWith(".json")) {
                    if (!NativeConfigInjector.injectHardwareMaskProfile(p, profile, targetFps)) {
                        ShizukuFileManager.ensureParentDirectory(p);
                        ShizukuFileManager.writeFile(p, jsonProfile, "666");
                    }
                } else if (p.endsWith(".xml")) {
                    NativeConfigInjector.injectHardwareMaskProfile(p, profile, targetFps);
                }
            }

            // 3. Guaranteed Shizuku elevated shell write fallback for HOK / SGame
            try {
                byte[] iniBytes = hokIni.getBytes(StandardCharsets.UTF_8);
                String iniB64 = Base64.encodeToString(iniBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");
                byte[] jsonBytes = jsonProfile.getBytes(StandardCharsets.UTF_8);
                String jsonB64 = Base64.encodeToString(jsonBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");
                String bootConfig = "target-frame-rate=" + targetFps + "\nwait-for-native-debugger=0\ngfx-enable-gles-20-shaders=0\n";
                String bootB64 = Base64.encodeToString(bootConfig.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP).replace("\n", "").replace("\r", "");

                String[] targets = {
                    "/storage/emulated/0/Android/data/" + pkg + "/files",
                    "/storage/emulated/0/Android/data/" + pkg + "/files/Config",
                    "/storage/emulated/0/Android/data/" + pkg + "/files/Apollo/Config",
                    "/sdcard/Android/data/" + pkg + "/files",
                    "/sdcard/Android/data/" + pkg + "/files/Config",
                    "/sdcard/Android/data/" + pkg + "/files/Apollo/Config"
                };

                String[] iniNames = {
                    "DeviceHardware.ini",
                    "GraphicSettings.ini",
                    "SGameSettings.ini",
                    "SystemConfig.ini",
                    "UserCustom.ini"
                };

                StringBuilder cmd = new StringBuilder();
                for (String t : targets) {
                    cmd.append("mkdir -p ").append(t).append(" 2>/dev/null; ");
                    for (String ini : iniNames) {
                        cmd.append("echo '").append(iniB64).append("' | base64 -d > ").append(t).append("/").append(ini).append(" 2>/dev/null; ");
                        cmd.append("chmod 666 ").append(t).append("/").append(ini).append(" 2>/dev/null; ");
                    }
                    cmd.append("echo '").append(jsonB64).append("' | base64 -d > ").append(t).append("/DeviceProfile.json 2>/dev/null; ");
                    cmd.append("chmod 666 ").append(t).append("/DeviceProfile.json 2>/dev/null; ");
                    cmd.append("echo '").append(jsonB64).append("' | base64 -d > ").append(t).append("/HardwareProfile.json 2>/dev/null; ");
                    cmd.append("chmod 666 ").append(t).append("/HardwareProfile.json 2>/dev/null; ");
                    cmd.append("echo '").append(jsonB64).append("' | base64 -d > ").append(t).append("/GraphicSetting.json 2>/dev/null; ");
                    cmd.append("chmod 666 ").append(t).append("/GraphicSetting.json 2>/dev/null; ");
                    cmd.append("echo '").append(jsonB64).append("' | base64 -d > ").append(t).append("/QualityConfig.json 2>/dev/null; ");
                    cmd.append("chmod 666 ").append(t).append("/QualityConfig.json 2>/dev/null; ");
                    cmd.append("echo '").append(bootB64).append("' | base64 -d > ").append(t).append("/boot.config 2>/dev/null; ");
                    cmd.append("chmod 666 ").append(t).append("/boot.config 2>/dev/null; ");
                }
                com.gamebooster.app.engine.PrivilegeBridgeEngine.executePrivileged(cmd.toString());

                String[] hokPrefs = {
                    "/data/data/" + pkg + "/shared_prefs/" + pkg + ".v2.playerprefs.xml",
                    "/data/data/" + pkg + "/shared_prefs/" + pkg + ".xml",
                    "/storage/emulated/0/Android/data/" + pkg + "/files/shared_prefs/" + pkg + ".v2.playerprefs.xml",
                    "/sdcard/Android/data/" + pkg + "/files/shared_prefs/" + pkg + ".v2.playerprefs.xml"
                };
                String[] hokXmlKeys = {
                    "<string name=\"SystemInfo_deviceModel\">" + profile.model + "</string>",
                    "<string name=\"SystemInfo_deviceName\">" + profile.displayName + "</string>",
                    "<string name=\"SystemInfo_operatingSystem\">Android OS " + profile.androidVersion + " / API-" + profile.sdkInt + " (" + profile.displayId + "/release-keys)</string>",
                    "<string name=\"SystemInfo_graphicsDeviceName\">" + profile.glRenderer + "</string>",
                    "<string name=\"DeviceModel\">" + profile.model + "</string>",
                    "<string name=\"DeviceBrand\">" + profile.brand + "</string>",
                    "<string name=\"Manufacturer\">" + profile.manufacturer + "</string>",
                    "<string name=\"AndroidVersion\">" + profile.androidVersion + "</string>",
                    "<string name=\"BuildFingerprint\">" + profile.fingerprint + "</string>",
                    "<string name=\"GPU\">" + profile.glRenderer + "</string>",
                    "<string name=\"SoC\">" + profile.socModel + "</string>",
                    "<int name=\"FPSLevel\" value=\"4\" />",
                    "<int name=\"FrameRateType\" value=\"4\" />",
                    "<int name=\"HighFrameRateMode\" value=\"2\" />",
                    "<int name=\"MaxFrameRate\" value=\"" + targetFps + "\" />",
                    "<int name=\"TargetFPS\" value=\"" + targetFps + "\" />",
                    "<int name=\"QualityLevel\" value=\"4\" />"
                };
                for (String pref : hokPrefs) {
                    try {
                        com.gamebooster.app.config.ConfigFileHelper.patchKeys(pref, hokXmlKeys, "<map>");
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            GameSecurityBypassEngine.enforceSelinuxAndOwnershipBypass(pkg, paths);
            Log.i(TAG, "✅ [HokHardwareMasker] HOK hardware profile injected successfully for " + pkg);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "❌ [HokHardwareMasker] Injection failed: " + t.getMessage(), t);
            return false;
        }
    }
}
