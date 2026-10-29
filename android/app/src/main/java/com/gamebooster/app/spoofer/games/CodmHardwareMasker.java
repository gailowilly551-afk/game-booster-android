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
            // 1. Synchronize with CODM asset engine & apply FPS tiers FIRST
            try {
                CodmConfigPatcher.deployCodmAssets(pkg);
                if (targetFps >= 185) {
                    CodmConfigPatcher.patchUltraExtreme185(pkg);
                } else if (targetFps >= 165) {
                    CodmConfigPatcher.patchUltraExtreme165(pkg);
                } else if (targetFps >= 144) {
                    CodmConfigPatcher.patchUltraExtreme144(pkg);
                } else {
                    CodmConfigPatcher.patch(pkg, targetFps);
                }
                CodmAceBypassPatcher.applyBypass(pkg);
            } catch (Throwable t) {
                Log.w(TAG, "CodmConfigPatcher sync error: " + t.getMessage());
            }

            // 2. Generate CODM-tailored JSON and INI profiles
            String jsonProfile = profile.generateJsonHardwareProfile(targetFps);

            // 3. Discover and patch dynamic CODM file paths
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
                    String[] codmKeys = {
                        "MaxFrameRate=" + targetFps,
                        "TargetFPS=" + targetFps,
                        "FrameRateLevel=4",
                        "HighFPSMode=1",
                        "Quality=3",
                        "GraphicLevel=3",
                        "DeviceModel=" + profile.model,
                        "GPU=" + profile.glRenderer,
                        "SoC=" + profile.socModel
                    };
                    ConfigFileHelper.patchKeys(p, codmKeys, "[Graphics]");
                    ConfigFileHelper.patchKeys(p, codmKeys, "[HardwareProfile]");
                }
            }

            // 4. Guaranteed Shizuku elevated shell write fallback for CODM profiles
            try {
                byte[] jsonBytes = jsonProfile.getBytes(StandardCharsets.UTF_8);
                String jsonB64 = Base64.encodeToString(jsonBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");
                String bootConfig = "target-frame-rate=" + targetFps + "\nwait-for-native-debugger=0\ngfx-enable-gles-20-shaders=0\n";
                String bootB64 = Base64.encodeToString(bootConfig.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP).replace("\n", "").replace("\r", "");

                String codFallbackCmd =
                    "mkdir -p /storage/emulated/0/Android/data/" + pkg + "/files/Config 2>/dev/null; " +
                    "mkdir -p /sdcard/Android/data/" + pkg + "/files/Config 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /storage/emulated/0/Android/data/" + pkg + "/files/Config/HardwareProfile.json 2>/dev/null; " +
                    "chmod 666 /storage/emulated/0/Android/data/" + pkg + "/files/Config/HardwareProfile.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /storage/emulated/0/Android/data/" + pkg + "/files/Config/device_profile.json 2>/dev/null; " +
                    "chmod 666 /storage/emulated/0/Android/data/" + pkg + "/files/Config/device_profile.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /storage/emulated/0/Android/data/" + pkg + "/files/HardwareProfile.json 2>/dev/null; " +
                    "chmod 666 /storage/emulated/0/Android/data/" + pkg + "/files/HardwareProfile.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /storage/emulated/0/Android/data/" + pkg + "/files/device_profile.json 2>/dev/null; " +
                    "chmod 666 /storage/emulated/0/Android/data/" + pkg + "/files/device_profile.json 2>/dev/null; " +
                    "echo '" + bootB64 + "' | base64 -d > /storage/emulated/0/Android/data/" + pkg + "/files/boot.config 2>/dev/null; " +
                    "chmod 666 /storage/emulated/0/Android/data/" + pkg + "/files/boot.config 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/Config/HardwareProfile.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/Config/HardwareProfile.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/Config/device_profile.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/Config/device_profile.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/HardwareProfile.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/HardwareProfile.json 2>/dev/null; " +
                    "echo '" + jsonB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/device_profile.json 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/device_profile.json 2>/dev/null; " +
                    "echo '" + bootB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/boot.config 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/boot.config 2>/dev/null";
                com.gamebooster.app.engine.PrivilegeBridgeEngine.executePrivileged(codFallbackCmd);

                String[] codmPrefs = {
                    "/data/data/" + pkg + "/shared_prefs/" + pkg + ".v2.playerprefs.xml",
                    "/data/data/" + pkg + "/shared_prefs/" + pkg + ".xml",
                    "/storage/emulated/0/Android/data/" + pkg + "/files/shared_prefs/" + pkg + ".v2.playerprefs.xml",
                    "/sdcard/Android/data/" + pkg + "/files/shared_prefs/" + pkg + ".v2.playerprefs.xml"
                };
                String[] codmXmlKeys = {
                    "<string name=\"DeviceModel\">" + profile.model + "</string>",
                    "<string name=\"GPU\">" + profile.glRenderer + "</string>",
                    "<string name=\"SoC\">" + profile.socModel + "</string>",
                    "<int name=\"FrameRate\" value=\"5\" />",
                    "<int name=\"FrameRateLevel\" value=\"5\" />",
                    "<int name=\"MaxFrameRate\" value=\"" + targetFps + "\" />",
                    "<int name=\"TargetFPS\" value=\"" + targetFps + "\" />",
                    "<int name=\"GraphicQuality\" value=\"4\" />"
                };
                for (String pref : codmPrefs) {
                    try {
                        ConfigFileHelper.patchKeys(pref, codmXmlKeys, "<map>");
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            GameSecurityBypassEngine.enforceSelinuxAndOwnershipBypass(pkg, paths);
            Log.i(TAG, "✅ [CodmHardwareMasker] CODM hardware profile deployed successfully for " + pkg);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "❌ [CodmHardwareMasker] Injection failed: " + t.getMessage(), t);
            return false;
        }
    }
}
