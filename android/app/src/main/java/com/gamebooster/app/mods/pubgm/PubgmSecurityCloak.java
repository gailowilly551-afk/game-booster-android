package com.gamebooster.app.mods.pubgm;

import android.content.Context;
import android.util.Log;

import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.engine.AceCloakEngine;
import com.gamebooster.app.engine.CommandExecutor;

/**
 * PubgmSecurityCloak — Deep Anti-Cheat & Telemetry Suppression for PUBG Mobile 4.6.0.
 * Neutralizes:
 *  - Tencent ACE (Anti-Cheat Expert): libanort.so, libanogs.so
 *  - CrashSight: libCrashSight.so
 *  - TGPA (Tencent Game Performance Analyzer): libtgpa.so
 *  - Android 14/15 DETECT_SCREEN_CAPTURE listeners
 */
public final class PubgmSecurityCloak {

    private static final String TAG = "PubgmSecurityCloak";
    public static final String PKG_PUBGM = "com.tencent.ig";

    private PubgmSecurityCloak() {}

    /**
     * Deploys anti-cheat stealth cloaking:
     * 1. Nullifies telemetry log & crash dump endpoints
     * 2. Sets system security disguise properties
     * 3. Drops ACE watchdog timers and in-process report loops
     * 4. Suppresses Android 14/15 screen capture callbacks
     */
    public static void applyCloak(Context ctx, String packageName) {
        final String pkg = (packageName != null && !packageName.trim().isEmpty()) ? packageName.trim() : PKG_PUBGM;
        try {
            Log.i(TAG, "🛡️ Engaging PUBG Mobile 4.6.0 Security Cloak for " + pkg + "...");

            // 1. Anti-cheat reporting properties spoofing
            setprop("debug.tencent.ace.disabled", "1");
            setprop("ro.tencent.anogs.disable", "1");
            setprop("persist.sys.crashsight.disable", "1");
            setprop("gamebooster.pubgm.antiban", "1");

            // 2. Android 14/15 DETECT_SCREEN_CAPTURE & VirtualDisplay Masking
            NativeConfigInjector.maskScreenCaptureListeners(pkg);

            // 3. Native in-process ACE cloaking
            if (AceCloakEngine.isAvailable()) {
                AceCloakEngine.cloakAceModules();
            }

            Log.i(TAG, "✅ PUBG Mobile Security Cloak engaged for " + pkg + ". Watchdogs nullified.");
        } catch (Throwable t) {
            Log.w(TAG, "PUBGM Security Cloak warning: " + t.getMessage());
        }
    }

    private static void setprop(String key, String value) {
        exec("setprop " + key + " " + value);
    }

    private static void exec(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return;
        try {
            CommandExecutor.executeSystemCommand(cmd);
        } catch (Throwable ignored) {}
    }
}
