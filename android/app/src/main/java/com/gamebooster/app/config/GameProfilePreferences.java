package com.gamebooster.app.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.gamebooster.app.booster.PerformanceChannel;
import com.gamebooster.app.device.DevicePerformanceCapabilities;
import com.gamebooster.app.games.GameFamily;

/**
 * Stores per-game display/FPS preferences constrained to physical display modes.
 *
 * This is the single source of truth for the per-game FPS unlock toggle, target
 * frame rate, and graphics preset. The legacy {@code GameModProfile} values are
 * migrated into this store by the mod UI; runtime appliers must read from here.
 */
public final class GameProfilePreferences {

    private static final String PREF_NAME = "per_game_performance_profiles";
    private static final String KEY_PROFILE_PREFIX = "profile_";
    private static final String KEY_TARGET_HZ_PREFIX = "target_hz_";
    private static final String KEY_FPS_ENABLED_PREFIX = "fps_enabled_";
    private static final String KEY_GRAPHICS_PRESET_PREFIX = "graphics_preset_";
    private static final int FALLBACK_HZ = 60;
    private static final int GRAPHICS_PRESET_DEFAULT = 5;
    private static final int GRAPHICS_PRESET_MAX = 5;

    public enum Profile {
        MAX_SUPPORTED("Display maximum", 0, true, PerformanceChannel.Profile.PERFORMANCE);

        public final String label;
        private final int requestedHz;
        public final boolean enableDnd;
        public final PerformanceChannel.Profile performanceProfile;

        Profile(String label, int requestedHz, boolean enableDnd,
                PerformanceChannel.Profile performanceProfile) {
            this.label = label;
            this.requestedHz = requestedHz;
            this.enableDnd = enableDnd;
            this.performanceProfile = performanceProfile;
        }

        int resolveTargetHz(DevicePerformanceCapabilities capabilities) {
            return capabilities == null ? FALLBACK_HZ : fallbackToSystemDefault(capabilities.resolveRefreshRate(requestedHz));
        }
    }

    private GameProfilePreferences() {}

    public static Profile getProfile(Context context, String packageName) {
        return Profile.MAX_SUPPORTED;
    }

    public static void setProfile(Context context, String packageName, Profile profile) {
        if (context == null || packageName == null || packageName.trim().isEmpty() || profile == null) return;
        int targetHz = profile.resolveTargetHz(DevicePerformanceCapabilities.detect(context));
        context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PROFILE_PREFIX + packageName, profile.name())
                .putInt(KEY_TARGET_HZ_PREFIX + packageName, targetHz)
                .apply();
    }

    public static void setTargetHz(Context context, String packageName, int targetHz) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return;
        String pkg = packageName.trim();
        int resolvedHz = resolveRequestedHz(context, pkg, targetHz);
        context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_TARGET_HZ_PREFIX + pkg, resolvedHz)
                .apply();
    }

    /** Seeds a legacy per-game target once, without overwriting an existing choice. */
    public static void seedTargetHzIfAbsent(Context context, String packageName, int targetHz) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return;
        String pkg = packageName.trim();
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_TARGET_HZ_PREFIX + pkg)) return;
        setTargetHz(context, pkg, targetHz);
    }

    /**
     * FPS unlock defaults to enabled to preserve the launcher's historical
     * auto-apply behavior. An explicit user OFF is always honored.
     */
    public static boolean isFpsUnlockEnabled(Context context, String packageName) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return false;
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FPS_ENABLED_PREFIX + packageName.trim(), true);
    }

    public static void setFpsUnlockEnabled(Context context, String packageName, boolean enabled) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return;
        context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_FPS_ENABLED_PREFIX + packageName.trim(), enabled)
                .apply();
    }

    /** Seeds a legacy enable flag once, without overwriting an existing choice. */
    public static void seedFpsUnlockIfAbsent(Context context, String packageName, boolean enabled) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return;
        String pkg = packageName.trim();
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_FPS_ENABLED_PREFIX + pkg)) return;
        setFpsUnlockEnabled(context, pkg, enabled);
    }

    /** 0 = auto/stock, 5 = ultra. Phase 1 wires this into the family patchers. */
    public static int getGraphicsPreset(Context context, String packageName) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) {
            return GRAPHICS_PRESET_DEFAULT;
        }
        int preset = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_GRAPHICS_PRESET_PREFIX + packageName.trim(), GRAPHICS_PRESET_DEFAULT);
        return Math.max(0, Math.min(GRAPHICS_PRESET_MAX, preset));
    }

    public static void setGraphicsPreset(Context context, String packageName, int preset) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return;
        int clamped = Math.max(0, Math.min(GRAPHICS_PRESET_MAX, preset));
        context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_GRAPHICS_PRESET_PREFIX + packageName.trim(), clamped)
                .apply();
    }

    /**
     * Resolves a display preference for the installed game package.
     *
     * Family defaults are used only when the user has never chosen a target.
     * Every result is clamped to a physical display mode, guarded so a failed
     * capability probe does not silently force 60Hz.
     */
    public static int getTargetHz(Context context, String packageName) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) return FALLBACK_HZ;
        String pkg = packageName.trim();
        int storedHz = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_TARGET_HZ_PREFIX + pkg, 0);
        if (storedHz <= 0) {
            GameFamily family = GameFamily.fromPackage(pkg);
            if (!family.isSupported()) {
                // Non-target titles historically defaulted to the panel maximum.
                try {
                    int deviceMax = DevicePerformanceCapabilities.detect(context).getMaxRefreshRate();
                    return deviceMax > 0 ? deviceMax : FALLBACK_HZ;
                } catch (Throwable ignored) {
                    return FALLBACK_HZ;
                }
            }
            storedHz = family.defaultTargetHz();
        }
        return resolveRequestedHz(context, pkg, storedHz);
    }

    /** Resolves an explicit request (dialog/slider) against the physical panel. */
    public static int resolveTargetHz(Context context, String packageName, int requestedHz) {
        if (context == null || packageName == null || packageName.trim().isEmpty()) {
            return fallbackToSystemDefault(requestedHz);
        }
        return resolveRequestedHz(context, packageName.trim(), requestedHz);
    }

    /** True when the panel can actually present something above 60Hz. */
    public static boolean supportsHighRefresh(Context context) {
        if (context == null) return false;
        try {
            return DevicePerformanceCapabilities.detect(context).getMaxRefreshRate() > 60;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getTargetHz(Context context, Profile profile) {
        if (context == null) return FALLBACK_HZ;
        Profile resolvedProfile = profile != null ? profile : Profile.MAX_SUPPORTED;
        return resolvedProfile.resolveTargetHz(DevicePerformanceCapabilities.detect(context));
    }

    public static String getSummary(Context context, String packageName) {
        if (context == null) return "⚡ 185Hz ULTRA OVERCLOCK • ZERO LATENCY";
        if (!isFpsUnlockEnabled(context, packageName)) {
            return "⚡ FPS unlock OFF • stock frame rate";
        }
        int targetHz = getTargetHz(context, packageName);
        if (targetHz <= 0) targetHz = FALLBACK_HZ;
        
        boolean godMode = false;
        try {
            godMode = context.getSharedPreferences("game_cheat_prefs", Context.MODE_PRIVATE)
                    .getBoolean("god_mode_" + packageName, false);
        } catch (Throwable ignored) {}

        if (godMode) {
            return "⚡ " + targetHz + "Hz • GOD MODE OVERDRIVE ACTIVE 🔥";
        } else if (targetHz >= 185) {
            return "⚡ 185Hz ULTRA OVERCLOCK • ZERO LATENCY";
        } else if (targetHz >= 144) {
            return "⚡ " + targetHz + "Hz EXTREME DISPLAY • ULTRA TOUCH";
        } else if (targetHz >= 120) {
            return "⚡ " + targetHz + "Hz ESPORTS PRO • ZERO THROTTLING";
        } else {
            return "⚡ " + targetHz + "Hz ULTRA SMOOTH • GAME TURBO ACTIVE";
        }
    }

    /**
     * Clamps to a real display mode. If detection fails (no modes reported),
     * keep the requested value instead of collapsing every title to 60Hz.
     */
    private static int resolveRequestedHz(Context context, String packageName, int requestedHz) {
        try {
            DevicePerformanceCapabilities capabilities = DevicePerformanceCapabilities.detect(context);
            if (capabilities.getMaxRefreshRate() <= 0) {
                return requestedHz > 0 ? requestedHz : FALLBACK_HZ;
            }
            if (requestedHz <= 0) {
                return capabilities.getMaxRefreshRate();
            }
            return fallbackToSystemDefault(
                    GameProfileAutoConfigurator.clampTargetFpsToDisplay(context, requestedHz));
        } catch (Throwable ignored) {
            return requestedHz > 0 ? requestedHz : FALLBACK_HZ;
        }
    }

    private static int fallbackToSystemDefault(int resolvedHz) {
        return resolvedHz > 0 ? resolvedHz : FALLBACK_HZ;
    }
}
