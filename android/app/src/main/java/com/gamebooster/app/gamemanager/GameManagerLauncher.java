package com.gamebooster.app.gamemanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.gamebooster.app.config.GameProfilePreferences;
import com.gamebooster.app.config.GameProfileAutoConfigurator;
import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.games.GameAppInfo;
import com.gamebooster.app.games.HomeGameScanner;

/**
 * GameManagerLauncher — The unified, authoritative Game Launch Engine.
 *
 * Starts a game with its normal framework launch intent, records the launcher
 * session, and uses a stored display preference only when it maps to a physical
 * mode reported by Android.
 */
public final class GameManagerLauncher {

    private static final String TAG = "GameManagerLauncher";

    private GameManagerLauncher() {
    }

    public interface OnGameLaunchListener {
        void onPreLaunchProgress(String step);
        void onLaunchSuccess(String packageName);
        void onLaunchFailed(String packageName, String reason);
    }

    /**
     * Launches a game from GameAppInfo object with full pre-boost pipeline.
     */
    public static void launchGame(Context context, GameAppInfo game) {
        if (context == null || game == null) return;
        launchGame(context, game.getPackageName(), game.getLaunchIntent(), game.getLabel(), null);
    }

    /**
     * Launches a game by package name with full pre-boost pipeline.
     */
    public static void launchGame(Context context, String packageName) {
        if (context == null || packageName == null) return;
        PackageManager pm = context.getPackageManager();
        Intent intent = HomeGameScanner.resolveLaunchIntent(pm, packageName);
        String label = packageName;
        try {
            label = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Throwable ignored) {}

        launchGame(context, packageName, intent, label, null);
    }

    /**
     * Instant launch pipeline with zero UI latency and parallel background optimization.
     * Guarantees that the game opens immediately upon clicking PLAY.
     */
    public static void launchGame(Context context, String packageName, Intent launchIntent,
                                  String label, OnGameLaunchListener listener) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) {
            if (listener != null) listener.onLaunchFailed("", "Null context or package name");
            return;
        }

        final Context appContext = context.getApplicationContext();
        String targetPkg = packageName.trim();

        // ═══════════════════════════════════════════════════════════
        // STEP 1: VERIFY PACKAGE INSTALLATION & RESOLVE LAUNCH INTENT
        // ═══════════════════════════════════════════════════════════
        PackageManager pm = appContext.getPackageManager();
        if (pm != null) {
            targetPkg = com.gamebooster.app.games.GameLauncherHelper.resolveInstalledFamilyPackage(appContext, targetPkg);
        }
        final String pkg = targetPkg;
        final String gameTitle = (label != null && !label.isEmpty()) ? label : pkg;

        int targetHz = GameProfilePreferences.getTargetHz(appContext, pkg);
        final int displayHz = GameProfileAutoConfigurator.clampTargetFpsToDisplay(appContext, targetHz);

        boolean isInstalled = false;
        if (pm != null) {
            isInstalled = HomeGameScanner.isPackageInstalled(pm, pkg);
        }

        if (!isInstalled) {
            Toast.makeText(appContext, "❌ Game Not Installed: " + gameTitle + " (Please install APK first)", Toast.LENGTH_LONG).show();
            if (listener != null) listener.onLaunchFailed(pkg, "Package " + pkg + " is not installed on device");
            return;
        }

        // Always prioritize the official framework launch intent with guaranteed explicit ComponentName
        Intent targetIntent = null;
        if (pm != null) {
            try {
                targetIntent = pm.getLaunchIntentForPackage(pkg);
            } catch (Throwable ignored) {}
            if (targetIntent == null) {
                try {
                    targetIntent = pm.getLeanbackLaunchIntentForPackage(pkg);
                } catch (Throwable ignored) {}
            }
        }
        if (targetIntent == null) {
            targetIntent = launchIntent;
        }
        if (targetIntent == null && pm != null) {
            targetIntent = HomeGameScanner.resolveLaunchIntent(pm, pkg);
        }

        // ═══════════════════════════════════════════════════════════
        // STEP 3: START THE GAME BEFORE ANY BOOST WORK
        //
        // The command executor is intentionally not in this path. It has a bounded
        // worker pool shared by config injection and shell work; queuing startActivity
        // there can leave a PLAY tap with no visible result. A copied intent also avoids
        // mutating the one retained by the home-card model.
        // ═══════════════════════════════════════════════════════════
        final Intent finalTargetIntent = targetIntent != null ? new Intent(targetIntent) : null;
        if (finalTargetIntent != null) {
            String lowerPkg = pkg.toLowerCase();
            if (lowerPkg.contains("pubg") || lowerPkg.contains("tencent.ig") || lowerPkg.contains("imobile") || lowerPkg.contains("krmobile") || lowerPkg.contains("vng.pubgmobile")) {
                finalTargetIntent.putExtra("DeviceProfile", "Android_High_165");
                finalTargetIntent.putExtra("-DeviceProfile", "Android_High_165");
                finalTargetIntent.putExtra("CVar", "r.PUBGDeviceFPSSupport165=1,r.PUBGMaxFPS=" + displayHz + ",r.MaxFPS=" + displayHz);
            }
        }

        // ═══════════════════════════════════════════════════════════
        // STEP 4: GUARANTEED IMMEDIATE LAUNCH ON MAIN THREAD
        // Starts the game activity immediately with ZERO UI thread blocking!
        // ═══════════════════════════════════════════════════════════
        Runnable launchNow = () -> {
            boolean launched = false;
            try {
                launched = launchWithFrameworkIntent(context, appContext, pkg, finalTargetIntent);
            } catch (Throwable t) {
                Log.w(TAG, "Framework launch error: " + t.getMessage());
            }

            if (launched) {
                try {
                    Toast.makeText(appContext, "Launching " + gameTitle + " (" + displayHz + "Hz)", Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
                if (listener != null) {
                    try { listener.onLaunchSuccess(pkg); } catch (Throwable ignored) {}
                }
            } else {
                reportLaunchFailure(appContext, pkg, gameTitle, listener);
            }
        };

        final boolean isMlbb = pkg.contains("mobile.legends") || pkg.contains("mobilelegends");

        Runnable postLaunchPipeline = () -> {
            AppExecutors.getInstance().executeCommand(() -> {
                try {
                    // 1. Hardware spoofing & profile commit (runs safely in background)
                    try {
                        com.gamebooster.app.spoofer.DeviceSpooferEngine.applyWorkingSpoofForGame(appContext, pkg, targetHz);
                    } catch (Throwable spoofErr) {
                        Log.w(TAG, "Hardware spoof note: " + spoofErr.getMessage());
                    }

                    // 2. Dynamic OEM optimization (Infinix, Tecno, Xiaomi, Vivo, Samsung, Huawei, Oppo, etc.)
                    try {
                        com.gamebooster.app.engine.OemPerformanceBooster.applyOemOptimizations(appContext, pkg, targetHz);
                    } catch (Throwable oemErr) {
                        Log.w(TAG, "OEM optimization note: " + oemErr.getMessage());
                    }

                    // 3. Floating Overlay Trainer (Safe check)
                    try {
                        if (android.provider.Settings.canDrawOverlays(appContext)) {
                            com.gamebooster.app.overlay.InGameTrainerOverlayService.startTrainer(appContext, pkg);
                        }
                    } catch (Throwable overlayErr) {
                        Log.w(TAG, "Floating overlay note: " + overlayErr.getMessage());
                    }
                    // If MLBB, inject Drone View concurrently while game splash screen is initializing
                    if (isMlbb) {
                        try {
                            android.content.SharedPreferences dronePrefs = appContext.getSharedPreferences("mlbb_drone_prefs", Context.MODE_PRIVATE);
                            boolean droneEnabled = dronePrefs.getBoolean("drone_enabled", true);
                            int rawTier = dronePrefs.getInt("drone_tier", com.gamebooster.app.config.MlbbDroneViewPatcher.TIER_2X);
                            int droneTier = com.gamebooster.app.config.MlbbDroneViewPatcher.normalizeTier(rawTier);
                            if (droneEnabled) {
                                boolean ok = com.gamebooster.app.config.MlbbDroneViewPatcher.applyDroneViewAtomic(appContext, pkg, droneTier);
                                if (ok) {
                                    AppExecutors.getInstance().postToMainThread(() -> {
                                        Toast.makeText(appContext, "🎯 MLBB Drone View " + com.gamebooster.app.config.MlbbDroneViewPatcher.getTierLabel(droneTier) + " Ready & Injected!", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        } catch (Throwable t) {
                            Log.w(TAG, "Post-launch Drone View setup: " + t.getMessage());
                        }
                        com.gamebooster.app.config.MlbbConfigPatcher.cleanLegacyLoadingLocks(pkg);
                    }

                    // Pre-Launch RAM Flush, UClamp burst & background AOT machine code warmup
                    com.gamebooster.app.engine.GameFastLoadAccelerator.triggerPreLaunchBurst(appContext, pkg);
                    com.gamebooster.app.engine.GameFastLoadAccelerator.scheduleLaunchSustainTransition(pkg);

                    // STAGE 0: Auto Shell Apply — fires GPU turbo, CPU governor, thermal bypass,
                    // all enabled tweaks (single Shizuku spawn), per-game asset script, Hz lock.
                    // Runs fully async so game startActivity() is NEVER blocked.
                    com.gamebooster.app.tweaks.GameLaunchShellExecutor.applyForGameLaunch(
                            appContext, pkg, targetHz, null);

                    // STAGE 1: Instant full 3-tier master enforcement & initial configs
                    com.gamebooster.app.engine.MasterOptimizationEnforcer.enforceGameLaunchOptimizations(appContext, pkg, targetHz);

                    // STAGE 1.5: User Custom Mod Sync (Automated MT Manager replacement)
                    try {
                        int customSynced = com.gamebooster.app.config.GameModAutoSyncEngine.syncModsForPackage(appContext, pkg);
                        if (customSynced > 0) {
                            AppExecutors.getInstance().postToMainThread(() -> {
                                Toast.makeText(appContext, "📁 " + customSynced + " Custom Files Synced (MT-Free)", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Throwable modErr) {
                        Log.w(TAG, "Custom mod sync notice: " + modErr.getMessage());
                    }

                    // STAGE 2: Direct 2026 Overdrive, Zero-Delay Combo & All Hero Script Injection
                    com.gamebooster.app.config.GameAutoInjectDispatcher.dispatchForPackage(appContext, pkg, true);

                    // STAGE 2.5: Bot Lobby & Rank Match Server Routing
                    try {
                        com.gamebooster.app.booster.BotLobbyRankMatchEngine.engageBotLobbyRankMatch(appContext, pkg);
                    } catch (Throwable botErr) {
                        Log.w(TAG, "Bot lobby & rank match server notice: " + botErr.getMessage());
                    }

                    // STAGE 3: Schedule In-Lobby Stealth Overdrive re-injection at exactly 10 seconds
                    // (Waits for splash screen / Moonton integrity check to complete, then locks combat mods)
                    com.gamebooster.app.config.LobbyInjectionEngine.scheduleLobbyInjection(appContext, pkg, targetHz, 10);

                    AppExecutors.getInstance().postToMainThread(() -> {
                        Toast.makeText(appContext, "⚡ " + gameTitle + " 100% Injected & Overdrive Active!", Toast.LENGTH_SHORT).show();
                    });

                    // STAGE 5: Purge game diagnostic logs for stealth
                    try {
                        com.gamebooster.app.config.AntiBanStealthEngine.purgeGameLogs(pkg);
                    } catch (Throwable stealthErr) {
                        Log.w(TAG, "Stealth log purge notice: " + stealthErr.getMessage());
                    }

                    GameManagerSessionEngine.beginSession(appContext, pkg);
                    com.gamebooster.app.gamespace.GameSpaceAnalyticsManager.onSessionStart(appContext, pkg, gameTitle);
                    com.gamebooster.app.overlay.GameSessionRecorder.getInstance()
                            .startSession(appContext, pkg, gameTitle);
                } catch (Throwable t) {
                    Log.w(TAG, "Game session start warning for " + pkg + ": " + t.getMessage());
                }
            });
        };

        // ═══════════════════════════════════════════════════════════
        // STEP 4: GUARANTEED IMMEDIATE LAUNCH ON MAIN THREAD
        // Always launch the game first without ANY blocking operations
        // so Android receives the foreground launch within the tap window!
        // ═══════════════════════════════════════════════════════════
        if (Looper.myLooper() == Looper.getMainLooper()) {
            launchNow.run();
        } else {
            AppExecutors.getInstance().postToMainThread(launchNow);
        }
        postLaunchPipeline.run();
    }

    /**
     * Starts a launchable game immediately. This method must only be called from the
     * main thread so an Activity caller retains its foreground-launch allowance.
     */
    private static boolean launchWithFrameworkIntent(Context sourceContext, Context appContext,
                                                     String packageName, Intent launchIntent) {
        // Priority 1: Use provided launchIntent if valid and has executable action or component
        if (launchIntent != null && (launchIntent.getComponent() != null || launchIntent.getPackage() != null || launchIntent.getAction() != null)) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            try {
                sourceContext.startActivity(launchIntent);
                Log.i(TAG, "Framework launch started for " + packageName);
                return true;
            } catch (Throwable sourceError) {
                Log.w(TAG, "Caller-context launch failed for " + packageName + ": " + sourceError.getMessage());
                try {
                    if (appContext != null) {
                        Intent cloneIntent = new Intent(launchIntent);
                        cloneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        appContext.startActivity(cloneIntent);
                        Log.i(TAG, "Application-context launch started for " + packageName);
                        return true;
                    }
                } catch (Throwable appError) {
                    Log.w(TAG, "Application-context launch failed for " + packageName + ": " + appError.getMessage());
                }
            }
        }

        // Priority 2: Direct PackageManager official launch intent
        Context bestCtx = (sourceContext != null) ? sourceContext : appContext;
        if (bestCtx != null) {
            try {
                PackageManager pm = bestCtx.getPackageManager();
                if (pm != null) {
                    Intent pmIntent = pm.getLaunchIntentForPackage(packageName);
                    if (pmIntent != null) {
                        pmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        bestCtx.startActivity(pmIntent);
                        Log.i(TAG, "PM direct launch started for " + packageName);
                        return true;
                    }

                    // Priority 3: Resolve explicit main launcher activity via queryIntentActivities
                    Intent mainQuery = new Intent(Intent.ACTION_MAIN);
                    mainQuery.addCategory(Intent.CATEGORY_LAUNCHER);
                    mainQuery.setPackage(packageName);
                    java.util.List<android.content.pm.ResolveInfo> matches = pm.queryIntentActivities(mainQuery, 0);
                    if (matches != null && !matches.isEmpty()) {
                        android.content.pm.ActivityInfo ai = matches.get(0).activityInfo;
                        if (ai != null) {
                            Intent explicitIntent = new Intent(Intent.ACTION_MAIN);
                            explicitIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                            explicitIntent.setComponent(new android.content.ComponentName(ai.packageName, ai.name));
                            explicitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            bestCtx.startActivity(explicitIntent);
                            Log.i(TAG, "Explicit component launch started for " + packageName + " -> " + ai.name);
                            return true;
                        }
                    }
                }
            } catch (Throwable pmError) {
                Log.w(TAG, "PM direct/query launch failed for " + packageName + ": " + pmError.getMessage());
            }
        }

        // Priority 4: Package-scoped framework launch intent
        try {
            Intent rawFallback = new Intent(Intent.ACTION_MAIN);
            rawFallback.addCategory(Intent.CATEGORY_LAUNCHER);
            rawFallback.setPackage(packageName);
            rawFallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            if (appContext != null) {
                appContext.startActivity(rawFallback);
                Log.i(TAG, "Package-scoped framework launch started for " + packageName);
                return true;
            }
        } catch (Throwable rawError) {
            Log.w(TAG, "Package-scoped framework launch failed for " + packageName + ": " + rawError.getMessage());
        }

        // Priority 5: Privileged Shizuku / Shell fallback (cmd activity start / monkey)
        try {
            String amCmd = "cmd activity start-activity -S -W " + packageName;
            String monkeyCmd = "monkey -p " + packageName + " -c android.intent.category.LAUNCHER 1";
            if (com.gamebooster.app.shizuku.ShizukuExecutor.hasShizukuPermission()) {
                String out = com.gamebooster.app.shizuku.ShizukuExecutor.executeShizukuCommand(amCmd);
                if (out != null && !out.contains("Error") && !out.contains("does not exist")) {
                    Log.i(TAG, "Shizuku privileged am start succeeded for " + packageName);
                    return true;
                }
                out = com.gamebooster.app.shizuku.ShizukuExecutor.executeShizukuCommand(monkeyCmd);
                if (out != null && !out.contains("No activities found")) {
                    Log.i(TAG, "Shizuku privileged monkey launch started for " + packageName);
                    return true;
                }
            } else if (com.gamebooster.app.engine.ShellExecutor.isRootSuAvailable()) {
                com.gamebooster.app.engine.ShellExecutor.CommandResult res =
                        com.gamebooster.app.engine.ShellExecutor.executeSuCommand(amCmd);
                if (res != null && res.isSuccess()) {
                    Log.i(TAG, "Root privileged am start succeeded for " + packageName);
                    return true;
                }
                res = com.gamebooster.app.engine.ShellExecutor.executeSuCommand(monkeyCmd);
                if (res != null && res.isSuccess() && !res.stdout.contains("No activities found")) {
                    Log.i(TAG, "Root privileged monkey launch started for " + packageName);
                    return true;
                }
            }
        } catch (Throwable privilegedError) {
            Log.w(TAG, "Privileged launch fallback failed for " + packageName + ": " + privilegedError.getMessage());
        }

        return false;
    }

    /** Reports a framework launch failure without executing shell fallbacks. */
    private static void reportLaunchFailure(Context appContext, String packageName,
                                            String gameTitle, OnGameLaunchListener listener) {
        Toast.makeText(appContext,
                "Cannot open " + gameTitle + ". Open it once from the phone launcher, then retry.",
                Toast.LENGTH_LONG).show();
        if (listener != null) {
            listener.onLaunchFailed(packageName, "No launchable activity was accepted by the device");
        }
    }

    /** Legacy hook retained for callers; it only records a display preference. */
    public static void preparePreLaunchConfigInjection(Context context, String pkg, int targetFps) {
        if (pkg == null || pkg.trim().isEmpty()) return;
        if (context != null) {
            GameProfilePreferences.setTargetHz(context, pkg, targetFps);
        }
    }
}
