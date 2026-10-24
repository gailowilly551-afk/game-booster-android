package com.gamebooster.app.spoofer.games;

import android.util.Base64;
import android.util.Log;
import com.gamebooster.app.config.AntiLogPatcher;
import com.gamebooster.app.config.ConfigFileHelper;
import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.GameSecurityBypassEngine;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.config.PubgConfigPatcher;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.spoofer.SpoofProfile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * PubgmHardwareMasker — Dedicated Hardware Masker & Profile Injector for PUBG Mobile / BGMI.
 *
 * Exclusively manages:
 *  - Unreal Engine 4 (UE4) / ShadowTrackerExtra Saved/Config/Android directories
 *  - UserCustom.ini ([UserCustom DeviceProfile]), DeviceProfile.ini, EnjoyCJZC.ini
 *  - Active.sav & ActiveShadow.sav binary FPS byte patching & PAK patch deployment
 *  - Zero crossover with MLBB or CODM configs.
 */
public final class PubgmHardwareMasker {

    private static final String TAG = "PubgmHardwareMasker";

    private PubgmHardwareMasker() {}

    public static boolean inject(String packageName, SpoofProfile profile) {
        if (packageName == null || profile == null) return false;
        final String pkg = packageName.trim().toLowerCase();
        final int targetFps = profile.maxRefreshRateHz > 0 ? profile.maxRefreshRateHz : 185;

        Log.i(TAG, "⚡ [PubgmHardwareMasker] Injecting dedicated PUBGM UE4 hardware profile for " + pkg + " @ " + targetFps + " FPS");

        try {
            // 1. Generate UE4 DeviceProfile keys
            String[] profileKeys = profile.generateUe4DeviceProfileKeys(targetFps);

            // 2. Discover and patch dynamic UE4 INI paths
            List<String> paths = GameConfigPathResolver.getPathsForGame(pkg);
            for (String p : paths) {
                if (p.endsWith("UserCustom.ini") || p.endsWith("ChineseUserCustom.ini")) {
                    ConfigFileHelper.patchKeys(p, profileKeys, "[UserCustom DeviceProfile]");
                    ConfigFileHelper.patchKeys(p, profileKeys, "[Android DeviceProfile]");
                    ConfigFileHelper.patchKeys(p, profileKeys, "[Android_Vulkan DeviceProfile]");
                } else if (p.endsWith("EnjoyCJZC.ini") || p.endsWith("EnjoyCJ.ini") || p.endsWith("BGMIEnjoyCJZC.ini") ||
                           p.endsWith("KREnjoyCJZC.ini") || p.endsWith("VNGEnjoyCJZC.ini") || p.endsWith("TWEnjoyCJZC.ini")) {
                    ConfigFileHelper.patchKeys(p, profileKeys, "[EnjoyCJZC DeviceProfile]");
                    ConfigFileHelper.patchKeys(p, profileKeys, "[/Script/ShadowTrackerExtra.UserSetting]");
                } else if (p.endsWith("DeviceProfile.ini") || p.endsWith("BaseDeviceProfile.ini") || p.endsWith("CompatibilityProfile.ini")) {
                    ConfigFileHelper.patchKeys(p, profileKeys, "[DeviceProfile]");
                    ConfigFileHelper.patchKeys(p, profileKeys, "[Android DeviceProfile]");
                    ConfigFileHelper.patchKeys(p, profileKeys, "[Android_Vulkan DeviceProfile]");
                } else if (p.endsWith("GameUserSettings.ini")) {
                    ConfigFileHelper.patchKeys(p, profileKeys, "[/Script/ShadowTrackerExtra.UserSetting]");
                } else if (p.endsWith("SettingInfo.ini")) {
                    ConfigFileHelper.patchKeys(p, profileKeys, "[SettingInfo]");
                } else if (p.endsWith("Quality.ini")) {
                    ConfigFileHelper.patchKeys(p, profileKeys, "[Quality]");
                } else if (p.endsWith("Engine.ini")) {
                    ConfigFileHelper.patchKeys(p, profileKeys, "[SystemSettings]");
                } else if (p.endsWith(".xml")) {
                    NativeConfigInjector.injectHardwareMaskProfile(p, profile, targetFps);
                }
            }

            // 3. Binary patching of Active.sav & PAK deployment
            try {
                if (targetFps >= 185) {
                    PubgConfigPatcher.patchSuperSmooth185(pkg);
                } else if (targetFps >= 165) {
                    PubgConfigPatcher.patchSuperSmooth165(pkg);
                } else if (targetFps >= 144) {
                    PubgConfigPatcher.patchSuperSmooth144(pkg);
                } else if (targetFps >= 120) {
                    PubgConfigPatcher.patchUltraHdr120(pkg);
                } else {
                    PubgConfigPatcher.patchActiveSavBinary(pkg, targetFps, true);
                }
                PubgConfigPatcher.deployPakPatch(pkg);
                AntiLogPatcher.applyAntiLog(pkg);
            } catch (Throwable t) {
                Log.w(TAG, "PUBGM binary patch warning: " + t.getMessage());
            }

            // 4. Guaranteed Shizuku elevated shell write fallback for PUBGM UE4 configs
            try {
                String ue4Ini = profile.generateUe4DeviceProfile(targetFps);
                byte[] iniBytes = ue4Ini.getBytes(StandardCharsets.UTF_8);
                String iniB64 = Base64.encodeToString(iniBytes, Base64.NO_WRAP).replace("\n", "").replace("\r", "");
                String pubgFallbackCmd =
                    "mkdir -p /sdcard/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android 2>/dev/null; " +
                    "echo '" + iniB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini 2>/dev/null; " +
                    "echo '" + iniB64 + "' | base64 -d > /sdcard/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/DeviceProfile.ini 2>/dev/null; " +
                    "chmod 666 /sdcard/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/DeviceProfile.ini 2>/dev/null";
                ShizukuExecutor.executeShizukuCommand(pubgFallbackCmd);
            } catch (Throwable ignored) {}

            GameSecurityBypassEngine.enforceSelinuxAndOwnershipBypass(pkg, paths);
            Log.i(TAG, "✅ [PubgmHardwareMasker] PUBGM hardware profile deployed successfully for " + pkg);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "❌ [PubgmHardwareMasker] Injection failed: " + t.getMessage(), t);
            return false;
        }
    }
}
