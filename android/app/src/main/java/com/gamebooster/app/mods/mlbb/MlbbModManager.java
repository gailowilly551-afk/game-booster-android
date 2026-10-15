package com.gamebooster.app.mods.mlbb;

import android.content.Context;
import android.util.Log;

import com.gamebooster.app.config.MlbbConfigPatcher;
import com.gamebooster.app.config.MlbbHeroScriptDispatcher;
import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.mods.GameModEngine;
import com.gamebooster.app.mods.GameModProfile;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.spoofer.HardwareMaskEngine;

import java.io.IOException;
import java.io.InputStream;

/**
 * MlbbModManager — Dedicated orchestrator for Mobile Legends: Bang Bang mod operations.
 * Coordinates 4-layer injection, Hero scripting, and IL2CPP memory resolution.
 */
public final class MlbbModManager {

    private static final String TAG = "MlbbModManager";
    public static final String PKG_MLBB = "com.mobile.legends";
    public static final int TARGET_VERSION_CODE = 211812322;
    public static final int TARGET_UID = 10677;
    public static final String DIR_USER_0 = "/data/user/0/com.mobile.legends";
    public static final String DIR_ANDROID_DATA = "/storage/emulated/0/Android/data/com.mobile.legends";
    private static final String FRIDA_HOOK_DEVICE_DIR = "/data/local/tmp/gb_hooks/";

    private static volatile boolean sMlbbInjected = false;

    private MlbbModManager() {}

    /**
     * Returns true if MLBB mod suite has been successfully injected and hooked.
     */
    public static boolean isMlbbInjected() {
        return sMlbbInjected;
    }

    /**
     * Resets the injection state flag.
     */
    public static void resetInjectionState() {
        sMlbbInjected = false;
    }

    /**
     * Synchronous 4-layer MLBB injector.
     * Hooks IL2CPP methods, deploys assets, updates configs, sets runtime props, and dispatches Frida scripts.
     * Returns true and sets sMlbbInjected = true when injection completes successfully.
     */
    public static synchronized boolean injectMlbbModSuite(Context ctx, String packageName, GameModProfile profile) {
        if (profile == null) return false;
        final String pkg = (packageName != null && !packageName.trim().isEmpty()) ? packageName.trim() : PKG_MLBB;

        try {
            Log.i(TAG, "⚡ [injectMlbbModSuite] Initializing MLBB Mod Pipeline for " + pkg + "...");

            // Pre-load IL2CPP offsets and dynamic signatures from assets
            try {
                MlbbIl2cppResolver.loadOffsets(ctx);
            } catch (Throwable t) {
                Log.w(TAG, "IL2CPP offset load warning: " + t.getMessage());
            }

            // Purge any legacy corrupted files/AssetBundles causing black screen
            try {
                MlbbConfigPatcher.cleanLegacyLoadingLocks(pkg);
            } catch (Throwable ignored) {}

            // Layer 1: Display refresh rate & surface flinger unlock
            if (profile.mlbbFpsUnlock) {
                GameModEngine.enforceDisplayRefreshRate(profile.mlbbTargetFps);
                try {
                    MlbbConfigPatcher.patch(pkg, profile.mlbbTargetFps);
                    HardwareMaskEngine.maskAllAndroidVersions(ctx, pkg, profile.mlbbTargetFps);
                } catch (Throwable t) {
                    Log.w(TAG, "MLBB static config patch note: " + t.getMessage());
                }
            }

            // Layer 2: Asset deployment, Drone View, and Hero scripts
            try {
                MlbbConfigPatcher.deployMlbbAssets(ctx, pkg);
            } catch (Throwable t) {
                Log.w(TAG, "MLBB asset deploy note: " + t.getMessage());
            }

            if (profile.mlbbDamageEnabled || profile.mlbbAttackSpeedEnabled ||
                profile.mlbbDefenseEnabled || profile.mlbbCooldownReductionEnabled ||
                profile.mlbbManaEnabled || profile.mlbbEnergyEnabled ||
                profile.mlbbNoCooldown || profile.mlbbMapHack || profile.mlbbDroneViewEnabled ||
                profile.mlbbNoSpread) {
                try {
                    MlbbConfigPatcher.applyBattleConfigOverdrive(pkg);
                    MlbbConfigPatcher.applyRankedCombatFullSuite(pkg);
                    MlbbConfigPatcher.applyClassicCombatFullSuite(pkg);
                    MlbbConfigPatcher.applyMlbbRankedMastery(pkg);
                    MlbbConfigPatcher.applyMlbbMasterSuite(pkg);
                    MlbbConfigPatcher.applyMlbbCcImmunityOverdrive(pkg);
                    MlbbHeroScriptDispatcher.dispatchAllHeroes(ctx, pkg);

                    if (profile.mlbbDroneViewEnabled) {
                        com.gamebooster.app.config.MlbbDroneViewPatcher.applyDroneViewAtomic(ctx, pkg, profile.mlbbDroneTier);
                    }

                    // Apply 2026 8-Feature Master Combat Matrix & Latest 2026 Patch Engine
                    float dmg = profile.mlbbDamageEnabled ? profile.mlbbDamageMult : 1.0f;
                    float fov = profile.mlbbDroneViewEnabled ? (profile.mlbbDroneTier >= 100 ? 240.0f : 180.0f) : 0.0f;
                    float aspd = profile.mlbbAttackSpeedEnabled ? profile.mlbbAttackSpeedMult : 1.0f;
                    MlbbConfigPatcher.applyMasterCombatMatrix(pkg, dmg, fov, 1.45f, aspd, profile.mlbbMapHack);
                    MlbbConfigPatcher.applyMlbbLatest2026Patch(pkg);
                    MlbbConfigPatcher.enforcePermanentZeroRevert(pkg);
                    com.gamebooster.app.config.GameModAutoSyncEngine.applyAntiRedownloadLocks(pkg);
                } catch (Throwable t) {
                    Log.w(TAG, "MLBB master suite error: " + t.getMessage());
                }
            }

            // Layer 3: SystemProperties for runtime bridge
            setprop("gamebooster.mlbb.dmg",     profile.mlbbDamageEnabled ? String.valueOf(profile.mlbbDamageMult) : "1.0");
            setprop("gamebooster.mlbb.aspd",    profile.mlbbAttackSpeedEnabled ? String.valueOf(profile.mlbbAttackSpeedMult) : "1.0");
            setprop("gamebooster.mlbb.def",     profile.mlbbDefenseEnabled ? String.valueOf(profile.mlbbDefenseMult) : "1.0");
            setprop("gamebooster.mlbb.cdr",     profile.mlbbCooldownReductionEnabled ? String.valueOf(profile.mlbbCooldownReductionPct) : (profile.mlbbNoCooldown ? "100" : "0"));
            setprop("gamebooster.mlbb.mana",    profile.mlbbManaEnabled ? (profile.mlbbInfiniteMana ? "9999" : String.valueOf(profile.mlbbManaMult)) : "1.0");
            setprop("gamebooster.mlbb.energy",  profile.mlbbEnergyEnabled ? (profile.mlbbInfiniteEnergy ? "9999" : String.valueOf(profile.mlbbEnergyMult)) : "1.0");
            setprop("gamebooster.mlbb.nocool",  profile.mlbbNoCooldown ? "1" : "0");
            setprop("gamebooster.mlbb.map",     profile.mlbbMapHack ? "1" : "0");
            setprop("gamebooster.mlbb.spread",  profile.mlbbNoSpread ? "1" : "0");
            setprop("gamebooster.mlbb.drone",   profile.mlbbDroneViewEnabled ? String.valueOf(profile.mlbbDroneTier) : "0");
            setprop("gamebooster.mlbb.fps",     String.valueOf(profile.mlbbFpsUnlock ? profile.mlbbTargetFps : 60));
            setprop("gamebooster.mlbb.antiban", profile.mlbbAntiBan ? "1" : "0");
            setprop("gamebooster.mlbb.mspd",    "1.45");
            setprop("gamebooster.mlbb.emblem",  "1");
            setprop("gamebooster.mlbb.injected","1");

            // Layer 4: Frida dynamic hooks injection
            boolean anyModActive = profile.mlbbDamageEnabled || profile.mlbbAttackSpeedEnabled
                    || profile.mlbbDefenseEnabled || profile.mlbbCooldownReductionEnabled
                    || profile.mlbbManaEnabled || profile.mlbbEnergyEnabled
                    || profile.mlbbDroneViewEnabled
                    || profile.mlbbNoSpread
                    || profile.mlbbNoCooldown || profile.mlbbMapHack || profile.mlbbFpsUnlock || profile.mlbbAntiBan;

            if (anyModActive) {
                GameModEngine.startFridaServer(ctx);
                GameModEngine.pushScriptAssetToDevice(ctx, "frida/mlbb_hooks.js", FRIDA_HOOK_DEVICE_DIR + "mlbb_hooks.js");
                GameModEngine.injectFrida(pkg, FRIDA_HOOK_DEVICE_DIR + "mlbb_hooks.js");
            }

            sMlbbInjected = true;
            Log.i(TAG, "✅ MLBB mod pipeline fully deployed and hooked for " + pkg + "; sMlbbInjected=" + sMlbbInjected);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "MLBB mod pipeline error: " + t.getMessage(), t);
            return false;
        }
    }

    /**
     * Applies the complete MLBB mod configuration suite asynchronously based on GameModProfile.
     */
    public static void applyProfile(Context ctx, GameModProfile profile) {
        applyProfile(ctx, PKG_MLBB, profile);
    }

    public static void applyProfile(Context ctx, String packageName, GameModProfile profile) {
        if (profile == null) return;
        final String pkg = (packageName != null && !packageName.trim().isEmpty()) ? packageName.trim() : PKG_MLBB;

        AppExecutors.getInstance().executeCommand(() -> {
            injectMlbbModSuite(ctx, pkg, profile);
        });
    }

    private static void setprop(String key, String value) {
        exec("setprop " + key + " " + value);
    }

    private static void injectFrida(String packageName, String scriptPath) {
        try {
            String whichFrida = com.gamebooster.app.engine.CommandExecutor.executeSystemCommand("which frida 2>/dev/null || which frida-inject 2>/dev/null");
            if (whichFrida != null && !whichFrida.trim().isEmpty() && !whichFrida.startsWith("ERROR:")) {
                String bin = whichFrida.trim().split("\n")[0].trim();
                if (bin.contains("frida-inject")) {
                    exec(bin + " -n " + packageName + " -s " + scriptPath + " &");
                } else {
                    exec(bin + " -U -f " + packageName + " -l " + scriptPath + " --no-pause &");
                }
                Log.i(TAG, "[Frida] Injected into " + packageName + " with " + scriptPath);
            } else {
                Log.i(TAG, "ℹ️ Frida CLI not installed on device; native C++ & config layers actively maintaining mods.");
            }
        } catch (Throwable t) {
            Log.w(TAG, "injectFrida note: " + t.getMessage());
        }
    }

    private static void pushScriptToDevice(String content, String devicePath) {
        if (content == null || content.isEmpty()) return;
        exec("mkdir -p " + FRIDA_HOOK_DEVICE_DIR);
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
}
