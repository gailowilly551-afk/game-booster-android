package com.gamebooster.app.engine;

import android.content.Context;
import android.util.Log;
import com.gamebooster.app.config.ConfigFileHelper;
import com.gamebooster.app.config.FpsUnlockTier;
import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.config.PubgConfigPatcher;
import com.gamebooster.app.shizuku.ShizukuExecutor;

import java.util.List;

/**
 * PubgmFpsUnlockEngine — Dedicated, Isolated 165 FPS Unlocker for PUBG Mobile (all variants).
 *
 * Handles all PUBG Mobile-specific:
 *  - Unreal Engine 4 Engine.ini (t.MaxFPS=165, r.PUBGDeviceFPSPolicy=2, r.PUBGMaxFPS=165)
 *  - UserCustom.ini [UserCustom DeviceProfile] +CVars=r.PUBGBattleFPS=9
 *  - GameUserSettings.ini FrameRateLimit=165.000000 and UpperBound=166.0
 *  - Binary Active.sav FPS tier patching (FPS level 9 / 165Hz)
 */
public final class PubgmFpsUnlockEngine {

    private static final String TAG = "PubgmFpsUnlockEngine";

    private PubgmFpsUnlockEngine() {}

    /**
     * Unlocks 165 FPS exclusively for PUBG Mobile across all Engine.ini, UserCustom.ini,
     * GameUserSettings.ini, and Active.sav binary saves.
     */
    public static boolean unlock165Fps(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            packageName = "com.tencent.ig";
        }
        final String pkg = packageName.trim();
        final int targetFps = 165;
        Log.i(TAG, "⚡ [PubgmFpsUnlockEngine] Unlocking dedicated 165 FPS for " + pkg);

        boolean anySuccess = false;

        // 1. Invoke specialized 165 FPS patcher
        try {
            boolean pOk = PubgConfigPatcher.patchUltraExtreme165(pkg);
            if (pOk) anySuccess = true;
        } catch (Throwable t) {
            Log.w(TAG, "PubgConfigPatcher.patchUltraExtreme165 note: " + t.getMessage());
        }

        // 2. Battle config overdrive at 165 FPS
        try {
            PubgConfigPatcher.applyPubgmBattleConfigOverdrive(pkg, targetFps);
            anySuccess = true;
        } catch (Throwable t) {
            Log.w(TAG, "PubgConfigPatcher.applyPubgmBattleConfigOverdrive note: " + t.getMessage());
        }

        // 3. High-speed native JNI graphics injection (FPS 165, Level 9)
        try {
            List<String> paths = GameConfigPathResolver.getPathsForGame(pkg);
            for (String path : paths) {
                if (NativeConfigInjector.injectPubgm165FpsGraphics(path, targetFps, 9)) {
                    anySuccess = true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "NativeConfigInjector.injectPubgm165FpsGraphics note: " + t.getMessage());
        }

        // 4. Shell / Shizuku Engine.ini, GameUserSettings.ini & UserCustom.ini enforcement
        String[] enginePaths = {
            "/sdcard/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/Engine.ini",
            "/storage/emulated/0/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/Engine.ini",
            "/sdcard/Android/data/" + pkg + "/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/Engine.ini",
            "/storage/emulated/0/Android/data/" + pkg + "/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/Engine.ini"
        };
        String[] gusPaths = {
            "/sdcard/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/GameUserSettings.ini",
            "/storage/emulated/0/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/GameUserSettings.ini",
            "/sdcard/Android/data/" + pkg + "/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/GameUserSettings.ini",
            "/storage/emulated/0/Android/data/" + pkg + "/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/GameUserSettings.ini"
        };
        String[] userCustomPaths = {
            "/sdcard/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini",
            "/storage/emulated/0/Android/data/" + pkg + "/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini",
            "/sdcard/Android/data/" + pkg + "/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini",
            "/storage/emulated/0/Android/data/" + pkg + "/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini"
        };

        StringBuilder sb = new StringBuilder();
        for (String engine : enginePaths) {
            sb.append("if [ -f '").append(engine).append("' ]; then ")
              .append("sed -i 's/t\\.MaxFPS=[0-9.]*/t.MaxFPS=165/g' '").append(engine).append("' 2>/dev/null; ")
              .append("sed -i 's/r\\.PUBGMaxFPS=[0-9.]*/r.PUBGMaxFPS=165/g' '").append(engine).append("' 2>/dev/null; ")
              .append("sed -i 's/r\\.PUBGDeviceFPSPolicy=[0-9.]*/r.PUBGDeviceFPSPolicy=2/g' '").append(engine).append("' 2>/dev/null; ")
              .append("grep -q 't.MaxFPS' '").append(engine).append("' || echo 't.MaxFPS=165' >> '").append(engine).append("' 2>/dev/null; ")
              .append("grep -q 'r.VSync' '").append(engine).append("' || echo 'r.VSync=0' >> '").append(engine).append("' 2>/dev/null; ")
              .append("fi; ");
        }
        for (String gus : gusPaths) {
            sb.append("if [ -f '").append(gus).append("' ]; then ")
              .append("sed -i 's/FrameRateLimit=[0-9.]*/FrameRateLimit=165.000000/g' '").append(gus).append("' 2>/dev/null; ")
              .append("sed -i 's/UpperBound=(Type=Exclusive,Value=[0-9.]*)/UpperBound=(Type=Exclusive,Value=166.0)/g' '").append(gus).append("' 2>/dev/null; ")
              .append("fi; ");
        }
        for (String uc : userCustomPaths) {
            sb.append("if [ -f '").append(uc).append("' ]; then ")
              .append("sed -i 's/\\+CVars=r\\.PUBGBattleFPS=[0-9]*/+CVars=r.PUBGBattleFPS=9/g' '").append(uc).append("' 2>/dev/null; ")
              .append("sed -i 's/\\+CVars=r\\.PUBGDeviceFPSPolicy=[0-9]*/+CVars=r.PUBGDeviceFPSPolicy=2/g' '").append(uc).append("' 2>/dev/null; ")
              .append("else ")
              .append("printf '[UserCustom DeviceProfile]\\n+CVars=r.PUBGBattleFPS=9\\n+CVars=r.PUBGDeviceFPSPolicy=2\\n+CVars=t.MaxFPS=165\\n' > '").append(uc).append("' 2>/dev/null; ")
              .append("fi; ");
        }
        executeShell(sb.toString());

        // 5. Binary Active.sav 165 FPS patch
        try {
            PubgConfigPatcher.patchActiveSavBinary(pkg, 165, true);
        } catch (Throwable ignored) {}

        Log.i(TAG, "✅ [PubgmFpsUnlockEngine] PUBGM 165 FPS unlock pipeline executed for " + pkg);
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
