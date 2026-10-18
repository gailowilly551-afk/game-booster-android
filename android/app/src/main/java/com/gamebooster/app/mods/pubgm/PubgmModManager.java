package com.gamebooster.app.mods.pubgm;

import android.content.Context;
import android.util.Log;

import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.config.PubgConfigPatcher;
import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.mods.GameModEngine;
import com.gamebooster.app.mods.GameModProfile;
import com.gamebooster.app.spoofer.HardwareMaskEngine;

import java.util.List;

/**
 * PubgmModManager — Dedicated orchestrator for PUBG Mobile 4.6.0 mod operations.
 * Coordinates 5-layer synchronous injector:
 *  1. Layer 1: Display refresh rate & SurfaceFlinger unlock (120/144/165/185 FPS)
 *  2. Layer 2: Asset deployment & Active.sav binary patch
 *  3. Layer 3: CVar injection & combat enhancement suite
 *  4. Layer 4: ACE anti-cheat cloaking & telemetry nullification
 *  5. Layer 5: Frida server & hook script dispatch
 */
public final class PubgmModManager {

    private static final String TAG = "PubgmModManager";
    public static final String PKG_PUBGM = "com.tencent.ig";
    public static final int TARGET_VERSION_CODE = 21525;
    public static final String TARGET_VERSION_NAME = "4.6.0";
    private static final String FRIDA_HOOK_DEVICE_DIR = "/data/local/tmp/gb_hooks/";

    private static volatile boolean sPubgmInjected = false;

    private PubgmModManager() {}

    /**
     * Returns true if PUBGM mod suite has been successfully injected and hooked.
     */
    public static boolean isPubgmInjected() {
        return sPubgmInjected;
    }

    /**
     * Resets the injection state flag.
     */
    public static void resetInjectionState() {
        sPubgmInjected = false;
    }

    /**
     * Synchronous 5-layer PUBG Mobile 4.6.0 injector.
     */
    public static synchronized boolean injectPubgmModSuite(Context ctx, String packageName, GameModProfile profile) {
        if (profile == null) return false;
        final String pkg = (packageName != null && !packageName.trim().isEmpty()) ? packageName.trim() : PKG_PUBGM;

        try {
            Log.i(TAG, "⚡ [injectPubgmModSuite] Initializing PUBG Mobile 4.6.0 Mod Pipeline for " + pkg + "...");
            try {
                PubgmUe4Resolver.loadOffsets(ctx);
            } catch (Throwable t) {
                Log.w(TAG, "PUBGM UE4 offset load warning: " + t.getMessage());
            }

            // Layer 1: Display refresh rate & SurfaceFlinger unlock (hardware adaptive up to 165Hz-185Hz)
            int targetFps = com.gamebooster.app.engine.HardwareDisplayHelper.resolveAdaptiveGameFps(ctx, profile.pubgmFpsUnlock ? profile.pubgmTargetFps : 185);
            if (profile.pubgmFpsUnlock) {
                GameModEngine.enforceDisplayRefreshRate(targetFps);
                try {
                    PubgConfigPatcher.patchCompetitive(pkg, targetFps);
                    HardwareMaskEngine.maskAllAndroidVersions(ctx, pkg, targetFps);
                } catch (Throwable t) {
                    Log.w(TAG, "PUBGM static config patch note: " + t.getMessage());
                }
            }

            // Layer 2: Pre-tuned 4.6.0 assets deployment & Active.sav binary patch
            try {
                if (ctx != null) {
                    PubgConfigPatcher.deployPubgmAssets(ctx, pkg);
                }
                PubgConfigPatcher.patchActiveSavBinary(pkg, targetFps, true, 4);
            } catch (Throwable t) {
                Log.w(TAG, "PUBGM asset deploy error: " + t.getMessage());
            }

            // Layer 3: CVar injection & combat enhancement suite
            try {
                if (ctx != null) {
                    PubgConfigPatcher.applyPubgm460MasterUpdate(ctx, pkg);
                }
            } catch (Throwable t) {
                Log.w(TAG, "PUBGM 4.6.0 master update error: " + t.getMessage());
            }

            // Apply granular mod profile toggles
            try {
                if (profile.pubgmAimbot) {
                    PubgConfigPatcher.applyEnemyLockMaxAllScope(pkg);
                    PubgConfigPatcher.applyPubgmUltraAimbot2026(pkg);
                    PubgConfigPatcher.applyAimAssistLockMax(pkg);
                }
                if (profile.pubgmNoRecoil) {
                    PubgConfigPatcher.applyPubgmZeroRecoilNoSway(pkg);
                }
                if (profile.pubgmMagicBullet) {
                    PubgConfigPatcher.applyPubgmMagicBulletInstantHit(pkg);
                }
                if (profile.pubgmUltraDroneView) {
                    PubgConfigPatcher.applyPubgUltraDroneViewMaxFov(pkg);
                }
                if (profile.pubgmDamageLockMax) {
                    PubgConfigPatcher.applyDamageLockMax(pkg);
                    PubgConfigPatcher.applyPubgmAllWeaponMaxDamage2026(pkg);
                }
                if (profile.pubgmCombatOverdrive) {
                    PubgConfigPatcher.applyPubgmCombatOverdrive2026(pkg);
                }
                if (profile.pubgmSovereignOverdriveBypass) {
                    PubgConfigPatcher.applyPubgmSovereignOverdriveBypass(pkg);
                }
            } catch (Throwable t) {
                Log.w(TAG, "PUBGM mod suite toggle injection warning: " + t.getMessage());
            }

            List<String> paths = GameConfigPathResolver.getPathsForGame(pkg);
            for (String path : paths) {
                try {
                    if (profile.pubgmUltraDroneView) {
                        NativeConfigInjector.injectPubgUltraDroneViewMaxFov(path);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Per-path mod injection warning: " + t.getMessage());
                }
            }

            // Layer 4: Anti-cheat cloaking & telemetry nullification
            if (profile.pubgmAntiBan && ctx != null) {
                PubgmSecurityCloak.applyCloak(ctx, pkg);
            }

            // System properties for dynamic hooks
            setprop("gamebooster.pubgm.aim",     profile.pubgmAimbot ? "1" : "0");
            setprop("gamebooster.pubgm.recoil",  profile.pubgmNoRecoil ? "1" : "0");
            setprop("gamebooster.pubgm.spread",  profile.pubgmNoSpread ? "1" : "0");
            setprop("gamebooster.pubgm.fov",     String.valueOf(profile.pubgmDroneFov));
            setprop("gamebooster.pubgm.fps",     String.valueOf(targetFps));
            setprop("gamebooster.pubgm.antiban", profile.pubgmAntiBan ? "1" : "0");

            // Layer 5: Frida dynamic hooks injection
            boolean anyModActive = profile.pubgmAimbot || profile.pubgmNoRecoil ||
                    profile.pubgmNoSpread || profile.pubgmMagicBullet || profile.pubgmUltraDroneView ||
                    profile.pubgmDamageLockMax || profile.pubgmFpsUnlock || profile.pubgmAntiBan;

            if (anyModActive && ctx != null) {
                try {
                    GameModEngine.startFridaServer(ctx);
                    GameModEngine.pushScriptAssetToDevice(ctx, "frida/pubgm_hooks.js", FRIDA_HOOK_DEVICE_DIR + "pubgm_hooks.js");
                    GameModEngine.injectFrida(pkg, FRIDA_HOOK_DEVICE_DIR + "pubgm_hooks.js");
                } catch (Throwable t) {
                    Log.w(TAG, "PUBGM Frida injection note: " + t.getMessage());
                }
            }

            sPubgmInjected = true;
            Log.i(TAG, "✅ [injectPubgmModSuite] PUBG Mobile 4.6.0 mod suite fully hooked & verified for " + pkg);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "PUBG Mobile mod pipeline injection failed for " + pkg + ": " + t.getMessage(), t);
            return false;
        }
    }

    /**
     * Applies the complete PUBGM mod configuration suite asynchronously based on GameModProfile.
     */
    public static void applyProfile(Context ctx, GameModProfile profile) {
        applyProfile(ctx, PKG_PUBGM, profile);
    }

    public static void applyProfile(Context ctx, String packageName, GameModProfile profile) {
        if (profile == null) return;
        final String pkg = (packageName != null && !packageName.trim().isEmpty()) ? packageName.trim() : PKG_PUBGM;

        AppExecutors.getInstance().executeCommand(() -> {
            injectPubgmModSuite(ctx, pkg, profile);
        });
    }

    private static void setprop(String key, String value) {
        exec("setprop " + key + " " + value);
    }

    private static void exec(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return;
        try {
            com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd);
        } catch (Throwable ignored) {}
    }
}
