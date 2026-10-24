package com.gamebooster.app.config;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.gamebooster.app.booster.HzFpsChannel;
import com.gamebooster.app.engine.CommandExecutor;
import com.gamebooster.app.games.GameFamily;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GameFpsCoordinator — the single authoritative writer for per-game FPS.
 *
 * All launcher entry points (launch dispatch, pre-launch dialog, mod UI, settings
 * preset) funnel through this class. Work is serialized on one daemon thread and
 * guarded by a per-package generation token, so an older apply can never land
 * after a newer one. Family config patches use the canonical package's resolver,
 * never a hardcoded path or a global 185 default.
 */
public final class GameFpsCoordinator {

    private static final String TAG = "GameFpsCoordinator";
    private static final int MIN_FPS = 60;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GameFpsCoordinator");
        t.setDaemon(true);
        return t;
    });

    private static final ConcurrentHashMap<String, AtomicLong> GENERATIONS =
            new ConcurrentHashMap<>();

    private GameFpsCoordinator() {}

    /** Async apply using the package's stored preference. */
    public static void applyForPackage(Context context, String packageName) {
        applyForPackage(context, packageName, 0, "stored-preference");
    }

    /**
     * Async apply using an explicit request (0 = use stored preference).
     * The request is clamped to a physical display mode; it is not persisted.
     */
    public static void applyForPackage(Context context, String packageName,
                                       int requestedHz, String source) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return;

        final Context appContext = context.getApplicationContext();
        final String pkg = packageName.trim();
        final String key = pkg.toLowerCase(java.util.Locale.ROOT);
        final AtomicLong generation = GENERATIONS.computeIfAbsent(key, k -> new AtomicLong());
        final long token = generation.incrementAndGet();

        EXECUTOR.execute(() -> {
            if (generation.get() != token) {
                Log.i(TAG, "Skipping stale FPS apply for " + pkg + " (" + source + ")");
                return;
            }
            try {
                if (!GameProfilePreferences.isFpsUnlockEnabled(appContext, pkg)) {
                    Log.i(TAG, "FPS unlock OFF for " + pkg + " — no display/config write");
                    setRuntimeFpsProp(pkg, MIN_FPS);
                    return;
                }

                int targetHz = requestedHz > 0
                        ? GameProfilePreferences.resolveTargetHz(appContext, pkg, requestedHz)
                        : GameProfilePreferences.getTargetHz(appContext, pkg);
                if (targetHz < MIN_FPS) targetHz = MIN_FPS;

                if (generation.get() != token) {
                    Log.i(TAG, "Skipping superseded FPS apply for " + pkg + " (" + source + ")");
                    return;
                }

                // One display write for both global panel and this package's Game Mode.
                HzFpsChannel.setRefreshRatePerPackage(appContext, pkg, targetHz);
                applyConfigOnlyInternal(appContext, pkg, targetHz);
                setRuntimeFpsProp(pkg, targetHz);

                Log.i(TAG, "Applied " + targetHz + "Hz for " + pkg
                        + " [" + GameFamily.fromPackage(pkg) + "] via " + source);
            } catch (Throwable t) {
                Log.w(TAG, "FPS apply failed for " + pkg + " (" + source + "): " + t.getMessage());
            }
        });
    }

    /** Re-applies the stored target after a short delay (OEM display managers revert). */
    public static void applyForPackageDeferred(Context context, String packageName, long delayMs) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return;
        final Context appContext = context.getApplicationContext();
        final String pkg = packageName.trim();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> applyForPackage(appContext, pkg, 0, "deferred"), Math.max(0, delayMs));
    }

    /** Global display only — used when no game is active. */
    public static void applyGlobalDisplay(Context context, int targetHz) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();
        final int hz = GameProfilePreferences.resolveTargetHz(
                appContext, GameFamily.MLBB.primaryPackage, targetHz);
        EXECUTOR.execute(() -> {
            try {
                HzFpsChannel.setRefreshRate(appContext, hz);
                Log.i(TAG, "Applied global display " + hz + "Hz");
            } catch (Throwable t) {
                Log.w(TAG, "Global display apply failed: " + t.getMessage());
            }
        });
    }

    /** Writes the family's game config files synchronously (used by the facade). */
    public static void applyConfigOnly(Context context, String packageName, int targetHz) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return;
        applyConfigOnlyInternal(context.getApplicationContext(), packageName.trim(),
                Math.max(MIN_FPS, targetHz));
    }

    /** Runtime FPS used for {@code gamebooster.<family>.fps} props (60 when disabled). */
    public static int resolveRuntimeFps(Context context, String packageName) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return MIN_FPS;
        if (!GameProfilePreferences.isFpsUnlockEnabled(context, packageName)) return MIN_FPS;
        return Math.max(MIN_FPS, GameProfilePreferences.getTargetHz(context, packageName));
    }

    private static void applyConfigOnlyInternal(Context context, String pkg, int targetHz) {
        switch (GameFamily.fromPackage(pkg)) {
            case MLBB:
                MlbbConfigPatcher.patchCompetitive(pkg, targetHz);
                MlbbConfigPatcher.applyMlbbPrefsIntAndBootConfig(pkg, targetHz);
                break;
            case CODM:
                CodmConfigPatcher.patchCompetitive(pkg, targetHz);
                break;
            case PUBGM:
                PubgConfigPatcher.patchCompetitive(pkg, targetHz);
                break;
            case OTHER:
            default:
                // Other titles keep their existing dispatcher-owned config path.
                break;
        }
    }

    private static void setRuntimeFpsProp(String pkg, int fps) {
        String key;
        switch (GameFamily.fromPackage(pkg)) {
            case MLBB:
                key = "gamebooster.mlbb.fps";
                break;
            case CODM:
                key = "gamebooster.codm.fps";
                break;
            case PUBGM:
                key = "gamebooster.pubgm.fps";
                break;
            default:
                return;
        }
        try {
            CommandExecutor.executeSystemCommand("setprop " + key + " " + fps);
        } catch (Throwable ignored) {}
    }
}
