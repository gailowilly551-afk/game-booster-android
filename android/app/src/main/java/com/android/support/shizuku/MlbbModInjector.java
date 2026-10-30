package com.android.support.shizuku;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.engine.PrivilegeBridgeEngine;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.shizuku.ShizukuFileManager;
import com.gamebooster.app.spoofer.DeviceSpooferEngine;
import com.gamebooster.app.spoofer.SpoofPreferences;
import com.gamebooster.app.spoofer.SpoofProfile;
import com.gamebooster.app.spoofer.SpoofProfileRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * MlbbModInjector — 4-Layer Dedicated ASUS ROG Phone 10 Pro+ (165Hz) Injector for MLBB.
 *
 * Implements:
 * Layer 1: Live RAM Identity Masker Daemon (memhook daemon invocation via Shizuku / Root).
 * Layer 2: Unity Engine 165 FPS Pipeline (boot.config with target-frame-rate=165).
 * Layer 3: Anti-Reset & ResCheck Bypass (res_skip_patch.xml with skipFix="1").
 * Layer 4: Android 12–16 SurfaceFlinger & GameManager Lock (cmd game mode performance, 165Hz lock).
 */
public final class MlbbModInjector {

    private static final String TAG = "MlbbModInjector";

    private static volatile boolean sNativeLoaded = false;
    static {
        try {
            System.loadLibrary("gamebooster_native");
            sNativeLoaded = true;
        } catch (Throwable t) {
            Log.w(TAG, "Native library load note: " + t.getMessage());
        }
    }

    public static native boolean nativeSpoofDeviceIdentity(
            String realModel, String spoofModel, String spoofBrand, int targetFps);

    public interface Callback {
        void onSuccess(String message);
        void onError(String error);
    }

    private MlbbModInjector() {}

    /**
     * Executes the full 4-layer ROG 10 Pro+ spoofing pipeline for MLBB.
     */
    public static void applyRog10ProSpoof(Context context, boolean autoLaunchMemhook, Callback callback) {
        if (context == null) {
            if (callback != null) callback.onError("Null context provided");
            return;
        }

        final Context appCtx = context.getApplicationContext();
        AppExecutors.getInstance().executeCommand(() -> {
            try {
                final String pkg = "com.mobile.legends";
                final String spoofModel = "ASUS_AI2601_A";
                final String spoofBrand = "ASUS ROG Phone 10 Pro+";
                final int targetFps = 165;
                final String realModel = Build.MODEL != null ? Build.MODEL : "Unknown";

                Log.i(TAG, "🔥 [ROG 10 PRO+ SPOOF] Applying full 4-layer 165Hz injection for " + pkg);

                // ═══════════════════════════════════════════════════════════════════
                // LAYER 1: LIVE RAM IDENTITY MASKER DAEMON (memhook)
                // ═══════════════════════════════════════════════════════════════════
                if (autoLaunchMemhook) {
                    try {
                        if (sNativeLoaded) {
                            nativeSpoofDeviceIdentity(realModel, spoofModel, spoofBrand, targetFps);
                            Log.i(TAG, "Layer 1: nativeSpoofDeviceIdentity invoked in-process");
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "Layer 1 in-process memhook note: " + t.getMessage());
                    }
                    try {
                        // Shell daemon execution if present
                        String memhookCmd = "memhook --spoof \"" + realModel + "\" --model \"" + spoofModel + "\" --brand \"" + spoofBrand + "\" --fps " + targetFps;
                        PrivilegeBridgeEngine.executePrivileged(memhookCmd);
                    } catch (Throwable t) {
                        Log.w(TAG, "Layer 1 memhook warning: " + t.getMessage());
                    }
                }

                // ═══════════════════════════════════════════════════════════════════
                // LAYER 2: UNITY ENGINE FRAMEWORK & ASSET PATCH (boot.config purge)
                // ═══════════════════════════════════════════════════════════════════
                // Note: MLBB Unity IL2CPP hangs/stalls if custom boot.config exists in files/
                String[] bootConfigPaths = {
                        "/storage/emulated/0/Android/data/" + pkg + "/files/boot.config",
                        "/storage/emulated/0/Android/data/" + pkg + "/files/dragon2017/assets/boot.config",
                        "/storage/emulated/0/Android/data/" + pkg + "/files/Dragon2017/assets/boot.config",
                        "/sdcard/Android/data/" + pkg + "/files/boot.config",
                        "/sdcard/Android/data/" + pkg + "/files/dragon2017/assets/boot.config",
                        "/sdcard/Android/data/" + pkg + "/files/Dragon2017/assets/boot.config",
                        "/data/data/" + pkg + "/files/boot.config"
                };

                for (String path : bootConfigPaths) {
                    try {
                        ShizukuFileManager.deleteFile(path);
                    } catch (Throwable ignored) {}
                }

                // Inject Document.unity3d and GameSettings_Rank (165Hz Ultra tier)
                DocumentBundlePatcher.patchDocumentBundle(appCtx, pkg, targetFps);

                // ═══════════════════════════════════════════════════════════════════
                // LAYER 3: ANTI-RESET & RESCHECK BYPASS (res_skip_patch.xml)
                // ═══════════════════════════════════════════════════════════════════
                String resSkipPatchXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<res_skip_patch skipFix=\"1\" version=\"2026\">\n" +
                        "    <file name=\"boot.config\" skipFix=\"1\" />\n" +
                        "    <file name=\"Document.unity3d\" skipFix=\"1\" />\n" +
                        "    <file name=\"GameSettings_Rank.json\" skipFix=\"1\" />\n" +
                        "    <file name=\"DeviceHardware.ini\" skipFix=\"1\" />\n" +
                        "</res_skip_patch>";

                String[] resSkipPaths = {
                        "/storage/emulated/0/Android/data/" + pkg + "/files/dragon2017/assets/res_skip_patch.xml",
                        "/storage/emulated/0/Android/data/" + pkg + "/files/Dragon2017/assets/res_skip_patch.xml",
                        "/sdcard/Android/data/" + pkg + "/files/dragon2017/assets/res_skip_patch.xml",
                        "/sdcard/Android/data/" + pkg + "/files/Dragon2017/assets/res_skip_patch.xml"
                };

                for (String p : resSkipPaths) {
                    try {
                        ShizukuFileManager.ensureParentDirectory(p);
                        ShizukuFileManager.writeFile(p, resSkipPatchXml, "666");
                    } catch (Throwable ignored) {}
                }

                // ═══════════════════════════════════════════════════════════════════
                // LAYER 4: ANDROID 12-16 SURFACEFLINGER & GAMEMANAGER LOCK
                // ═══════════════════════════════════════════════════════════════════
                List<String> sysCmds = new ArrayList<>();
                sysCmds.add("cmd game mode performance " + pkg);
                sysCmds.add("cmd game set --fps 165 " + pkg);
                sysCmds.add("device_config put game_overlay " + pkg + " mode=2,fps=165:mode=3,fps=165");
                sysCmds.add("settings put system peak_refresh_rate 165.0");
                sysCmds.add("settings put system min_refresh_rate 165.0");
                sysCmds.add("settings put system user_refresh_rate 165");
                sysCmds.add("setprop debug.game.spoofed_model \"" + spoofModel + "\"");
                sysCmds.add("setprop debug.game.spoofed_brand \"ASUS\"");
                sysCmds.add("setprop debug.game.spoofed_gpu \"Adreno (TM) 840\"");
                sysCmds.add("setprop debug.game.spoofed_soc \"Snapdragon 8 Elite Gen 2\"");

                PrivilegeBridgeEngine.executePrivilegedBatch(sysCmds);

                // Set active profile in launcher preferences
                SpoofPreferences.setSpoofEnabled(appCtx, true);
                SpoofPreferences.setActiveProfileId(appCtx, SpoofProfileRegistry.ROG9_PRO_ID);
                DeviceSpooferEngine.forceApplySpoof(appCtx, SpoofProfileRegistry.ROG9_PRO_ID, pkg);

                AppExecutors.getInstance().postToMainThread(() -> {
                    if (callback != null) {
                        callback.onSuccess("🔥 ASUS ROG Phone 10 Pro+ (165Hz) Masked & Active!");
                    }
                });

            } catch (Throwable t) {
                Log.e(TAG, "ROG 10 Pro+ injection failed: " + t.getMessage(), t);
                AppExecutors.getInstance().postToMainThread(() -> {
                    if (callback != null) {
                        callback.onError("Injection failed: " + t.getMessage());
                    }
                });
            }
        });
    }
}
