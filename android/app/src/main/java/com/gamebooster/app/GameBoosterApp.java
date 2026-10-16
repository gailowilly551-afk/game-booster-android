package com.gamebooster.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gamebooster.app.booster.BypassChargingController;
import com.gamebooster.app.booster.NetworkOptimizer;
import com.gamebooster.app.config.ConfigBackupManager;
import com.gamebooster.app.config.GameModAutoSyncEngine;
import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.device.HardwareDisplayController;
import com.gamebooster.app.diagnostics.CrashLog;
import com.gamebooster.app.diagnostics.DiagnosticsExporter;
import com.gamebooster.app.engine.PlayIntegrityBypassEngine;
import com.gamebooster.app.engine.ResolutionScalerEngine;
import com.gamebooster.app.games.HomeGameScanner;
import com.gamebooster.app.gamespace.AutoGameMonitorService;
import com.gamebooster.app.services.GameUpdateMonitorService;
import com.gamebooster.app.shizuku.RishManager;
import com.gamebooster.app.shizuku.ShizukuAutoConnectEngine;
import com.gamebooster.app.shizuku.ShizukuConnectionManager;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.shizuku.ShizukuLifecycleManager;
import com.gamebooster.app.shizuku.ShizukuManager;
import com.gamebooster.app.shizuku.ShizukuPermissionEnforcer;
import com.gamebooster.app.shizuku.ShizukuUserServiceConnector;
import com.gamebooster.app.spoofer.DeviceSpooferEngine;
import com.gamebooster.app.spoofer.HardwareMaskEngine;
import com.gamebooster.app.spoofer.SpoofPreferences;
import com.gamebooster.app.spoofer.SpoofProfile;
import com.gamebooster.app.terminal.TerminalFolderManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * GameBoosterApp — Central Application Class.
 *
 * Provides safe application-wide initialization, global uncaught exception handling
 * to prevent crashes and startup ANRs, background service pre-warming (Rish, Shizuku AIDL,
 * Terminal Scripts, Game Library Scanner, Display Hz & Network Tuning), and cold-start
 * in-app identity spoofing restoration.
 */
public class GameBoosterApp extends Application {

    private static final String TAG = "GameBoosterApp";
    private static GameBoosterApp sInstance;

    private final AtomicInteger mForegroundActivityCount = new AtomicInteger(0);

    public static GameBoosterApp getInstance() {
        return sInstance;
    }

    public static Context getAppContext() {
        return sInstance != null ? sInstance.getApplicationContext() : null;
    }

    public boolean isAppInForeground() {
        return mForegroundActivityCount.get() > 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // 1. Global crash protection & logging: install CrashLog early
        try {
            CrashLog.install(this);
        } catch (Throwable t) {
            Log.w(TAG, "CrashLog install error: " + t.getMessage());
        }

        // 2. Early safety initialization (App-private storage & backup paths)
        try {
            ConfigBackupManager.setAppContext(getApplicationContext());
        } catch (Throwable t) {
            Log.w(TAG, "ConfigBackupManager init error: " + t.getMessage());
        }

        // 2.1 Process-wide Shizuku Sticky Binder Listeners & Connection Lifecycle
        try {
            ShizukuLifecycleManager.getInstance(this).init();
            ShizukuManager.registerBinderListeners();
            ShizukuConnectionManager.getInstance().start();
            ShizukuAutoConnectEngine.startEarlyAutoConnect(this);
        } catch (Throwable t) {
            Log.w(TAG, "Shizuku early initialization error: " + t.getMessage());
        }

        // 2.2 Network transition listener: preserve Shizuku Binder during Wi-Fi drops & cellular handoffs
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                cm.registerDefaultNetworkCallback(new android.net.ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull android.net.Network network) {
                        ShizukuConnectionManager.getInstance().forceReconnectCheck();
                    }

                    @Override
                    public void onLost(@NonNull android.net.Network network) {
                        // Wi-Fi disconnected: verify local Binder immediately
                        ShizukuConnectionManager.getInstance().forceReconnectCheck();
                    }
                });
            }
        } catch (Throwable t) {
            Log.w(TAG, "NetworkCallback registration error: " + t.getMessage());
        }

        // 2.3 Activity Lifecycle Tracking for foreground/background state
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                mForegroundActivityCount.incrementAndGet();
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                mForegroundActivityCount.decrementAndGet();
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });

        // 3. Pre-warm background engines on dedicated worker thread (Zero Main-Thread Block)
        final Context appCtx = getApplicationContext();
        AppExecutors.getInstance().executeCommand(() -> {
            // A. Emergency Battery Safety Baseline: ensure device is NOT stuck in mocked unplugged state
            try {
                BypassChargingController.restoreNormalCharging(appCtx);
            } catch (Throwable t) {
                Log.w(TAG, "Battery charging baseline restore note: " + t.getMessage());
            }

            // B. Zero Display Distortion Safety: ensure physical screen size and density are in stock state
            try {
                ResolutionScalerEngine.forceResetResolutionSync();
            } catch (Throwable t) {
                Log.w(TAG, "Resolution baseline safety check note: " + t.getMessage());
            }

            // C. Pre-warm Terminal Scripts & Folder structure
            try {
                TerminalFolderManager.getInstance(appCtx).initTerminalFolder();
            } catch (Throwable t) {
                Log.w(TAG, "TerminalFolderManager background warm-up error: " + t.getMessage());
            }

            // D. Extract and prepare Rish executable & dex
            try {
                RishManager.initialize(appCtx);
            } catch (Throwable t) {
                Log.w(TAG, "RishManager background initialization error: " + t.getMessage());
            }

            // E. Pre-bind Shizuku AIDL UserService
            try {
                ShizukuUserServiceConnector.getInstance().bindService();
            } catch (Throwable t) {
                Log.w(TAG, "ShizukuUserServiceConnector pre-bind error: " + t.getMessage());
            }

            // F. Enforce Shizuku Elevated Storage & System Permissions if granted
            try {
                if (ShizukuExecutor.hasShizukuPermission()) {
                    ShizukuPermissionEnforcer.enforceAllPermissions(appCtx);
                }
            } catch (Throwable t) {
                Log.w(TAG, "Shizuku permissions early enforcement note: " + t.getMessage());
            }

            // G. Cold-Start Identity Restoration: apply in-app reflection if spoofing is active
            try {
                if (SpoofPreferences.isSpoofEnabled(appCtx)) {
                    String activeId = SpoofPreferences.getActiveProfileId(appCtx);
                    if (activeId != null && !activeId.isEmpty()) {
                        SpoofProfile profile = DeviceSpooferEngine.getProfileById(activeId);
                        if (profile != null) {
                            DeviceSpooferEngine.applyInAppBuildSpoof(profile);
                            Log.i(TAG, "Cold-start spoof identity restored: " + profile.displayName);
                        }
                    }
                    try {
                        DeviceSpooferEngine.applyActiveProfileToAllGames(appCtx);
                        HardwareMaskEngine.maskAllInstalledApplications(appCtx);
                    } catch (Throwable t) {
                        Log.w(TAG, "Hardware mask initialization note: " + t.getMessage());
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "Cold-start spoof restoration error: " + t.getMessage());
            }

            // H. Hardware Display Refresh Rate Baseline: prevent dynamic 60Hz downthrottling
            try {
                HardwareDisplayController.forceMaxHzNeverFallback(appCtx);
            } catch (Throwable t) {
                Log.w(TAG, "Hardware display refresh rate baseline note: " + t.getMessage());
            }

            // I. Low-Latency Network Socket Tuning & TCP Buffer Pre-warm
            try {
                NetworkOptimizer.enableRankedLowLatencySocketTuning(appCtx);
            } catch (Throwable t) {
                Log.w(TAG, "NetworkOptimizer low latency pre-warm note: " + t.getMessage());
            }

            // J. Play Integrity & Zygisk/Shamiko/TrickyStore Baseline Diagnostic
            try {
                PlayIntegrityBypassEngine.ModuleStackStatus moduleStatus = PlayIntegrityBypassEngine.checkModuleStack();
                Log.i(TAG, "Play Integrity module stack baseline: " + moduleStatus);
            } catch (Throwable t) {
                Log.w(TAG, "PlayIntegrity module stack check note: " + t.getMessage());
            }

            // K. Pre-scan installed target games for instant Home screen rendering
            try {
                HomeGameScanner.scanTargetGames(appCtx);
            } catch (Throwable t) {
                Log.w(TAG, "HomeGameScanner pre-cache error: " + t.getMessage());
            }

            // L. Start persistent game detection + auto-inject service.
            // This is the core engine that watches for game opens and fires
            // GameAutoInjectDispatcher for MLBB, PUBGM, CODM, etc. automatically.
            try {
                AutoGameMonitorService.start(appCtx);
                Log.i(TAG, "✅ AutoGameMonitorService started — game detection active");
            } catch (Throwable t) {
                Log.w(TAG, "AutoGameMonitorService start error: " + t.getMessage());
            }

            // M. Start GameUpdateMonitorService — auto-patch when Play Store updates games.
            // This is critical: without it, every game update silently breaks all patches.
            try {
                GameUpdateMonitorService.startIfNeeded(appCtx);
                Log.i(TAG, "✅ GameUpdateMonitorService started — auto-patch on game update active");
            } catch (Throwable t) {
                Log.w(TAG, "GameUpdateMonitorService start error: " + t.getMessage());
            }

            // N. Initialize /storage/emulated/0/GameLauncher/mods/ staging folders for MT-free drop-in
            try {
                GameModAutoSyncEngine.initializeStagingDirectory();
            } catch (Throwable ignored) {}

            // O. Pre-warm System Diagnostics Snapshot in memory for instant zero-wait live display
            try {
                DiagnosticsExporter.prewarm(appCtx);
            } catch (Throwable t) {
                Log.w(TAG, "Diagnostics pre-warm error: " + t.getMessage());
            }
        });

        Log.i(TAG, "Game Booster Application initialized successfully.");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            Log.i(TAG, "System memory low (level=" + level + ") — flushing non-essential memory pools");
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "System critically low on memory — onLowMemory triggered");
    }
}
