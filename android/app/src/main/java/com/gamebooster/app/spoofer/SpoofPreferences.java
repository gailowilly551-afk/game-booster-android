package com.gamebooster.app.spoofer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * SpoofPreferences handles persistence of the master Device Spoof toggle,
 * active global SpoofProfile selection, and per-package profile overrides.
 *
 * Per-package keys are stored as "pkg_profile_<packageName>" in the same SharedPreferences.
 * Per-package profile takes priority over the global profile when applySpoofing() is called.
 */
public class SpoofPreferences {

    private static final String PREF_NAME = "device_spoofer_prefs";
    private static final String KEY_SPOOF_ENABLED = "spoof_enabled";
    private static final String KEY_ACTIVE_PROFILE_ID = "active_profile_id";
    private static final String KEY_SPOOF_ALL_APPS = "spoof_all_apps";
    private static final String PREFIX_PKG_PROFILE = "pkg_profile_";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Global Spoof Toggle ──

    public static boolean isSpoofEnabled(Context context) {
        if (context == null) return false;
        return getPrefs(context).getBoolean(KEY_SPOOF_ENABLED, false);
    }

    public static void setSpoofEnabled(Context context, boolean enabled) {
        if (context == null) return;
        getPrefs(context).edit().putBoolean(KEY_SPOOF_ENABLED, enabled).apply();
    }

    // ── Spoof All Apps Toggle ──

    public static boolean isSpoofAllApps(Context context) {
        if (context == null) return false;
        return getPrefs(context).getBoolean(KEY_SPOOF_ALL_APPS, false);
    }

    public static void setSpoofAllApps(Context context, boolean enabled) {
        if (context == null) return;
        getPrefs(context).edit().putBoolean(KEY_SPOOF_ALL_APPS, enabled).apply();
    }

    // ── Global Active Profile ──

    public static String getActiveProfileId(Context context) {
        if (context == null) return null;
        String id = getPrefs(context).getString(KEY_ACTIVE_PROFILE_ID, null);
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return id;
    }

    public static void setActiveProfileId(Context context, String profileId) {
        if (context == null) return;
        getPrefs(context).edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply();
    }

    public static void clearActiveProfile(Context context) {
        if (context == null) return;
        getPrefs(context).edit().remove(KEY_ACTIVE_PROFILE_ID).apply();
    }

    // ── Per-Package Profile Override ──

    /**
     * Returns the spoof profile ID saved for a specific game package.
     * Returns null if no per-package override is set (falls back to global profile).
     */
    public static String getProfileIdForPackage(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return null;
        return getPrefs(context).getString(PREFIX_PKG_PROFILE + packageName, null);
    }

    /**
     * Saves a spoof profile ID for a specific game package.
     * This overrides the global profile when that game is launched.
     */
    public static void setProfileIdForPackage(Context context, String packageName, String profileId) {
        if (packageName == null || packageName.isEmpty()) return;
        getPrefs(context).edit().putString(PREFIX_PKG_PROFILE + packageName, profileId).apply();
    }

    /**
     * Removes the per-package override for a game, so it falls back to the global profile.
     */
    public static void clearProfileForPackage(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        getPrefs(context).edit().remove(PREFIX_PKG_PROFILE + packageName).apply();
    }

    // ── Dedicated Per-Game Family Spoof Profile Storage ──

    public static final String PKG_FAMILY_MLBB  = "com.mobile.legends";
    public static final String PKG_FAMILY_CODM  = "com.garena.game.codm";
    public static final String PKG_FAMILY_PUBGM = "com.tencent.ig";

    public static String getMlbbSpoofProfile(Context context) {
        return getProfileIdForPackage(context, PKG_FAMILY_MLBB);
    }

    public static void setMlbbSpoofProfile(Context context, String profileId) {
        setProfileIdForPackage(context, PKG_FAMILY_MLBB, profileId);
    }

    public static String getCodmSpoofProfile(Context context) {
        return getProfileIdForPackage(context, PKG_FAMILY_CODM);
    }

    public static void setCodmSpoofProfile(Context context, String profileId) {
        setProfileIdForPackage(context, PKG_FAMILY_CODM, profileId);
    }

    public static String getPubgmSpoofProfile(Context context) {
        return getProfileIdForPackage(context, PKG_FAMILY_PUBGM);
    }

    public static void setPubgmSpoofProfile(Context context, String profileId) {
        setProfileIdForPackage(context, PKG_FAMILY_PUBGM, profileId);
    }

    /**
     * Resolves the effective profile ID for a package:
     * per-package override → per-family override → global active profile → null.
     */
    public static String resolveProfileId(Context context, String packageName) {
        String pkgProfile = getProfileIdForPackage(context, packageName);
        if (pkgProfile != null && !pkgProfile.isEmpty()) return pkgProfile;

        // Fallback to family-level override
        com.gamebooster.app.games.GameFamily family = com.gamebooster.app.games.GameFamily.fromPackage(packageName);
        if (family == com.gamebooster.app.games.GameFamily.MLBB) {
            String familyProfile = getMlbbSpoofProfile(context);
            if (familyProfile != null && !familyProfile.isEmpty()) return familyProfile;
        } else if (family == com.gamebooster.app.games.GameFamily.CODM) {
            String familyProfile = getCodmSpoofProfile(context);
            if (familyProfile != null && !familyProfile.isEmpty()) return familyProfile;
        } else if (family == com.gamebooster.app.games.GameFamily.PUBGM) {
            String familyProfile = getPubgmSpoofProfile(context);
            if (familyProfile != null && !familyProfile.isEmpty()) return familyProfile;
        }

        return getActiveProfileId(context);
    }

    /**
     * Exposes all persisted spoof preferences as a key→value map.
     */
    public static Map<String, String> readAllPrefs(Context context) {
        Map<String, String> out = new HashMap<>();
        try {
            Map<String, ?> all = getPrefs(context).getAll();
            for (Map.Entry<String, ?> e : all.entrySet()) {
                Object v = e.getValue();
                if (v != null) out.put(e.getKey(), String.valueOf(v));
            }
        } catch (Throwable ignored) {}
        return out;
    }

    /**
     * Checks whether spoofing is enabled and configured for a specific package.
     */
    public static boolean isSpoofActiveForPackage(Context context, String packageName) {
        if (context == null || !isSpoofEnabled(context)) return false;
        String id = resolveProfileId(context, packageName);
        return id != null && !id.trim().isEmpty();
    }

    /**
     * Completely resets all spoofing preferences (master switch, active profile, per-package overrides).
     */
    public static void resetAll(Context context) {
        if (context == null) return;
        getPrefs(context).edit().clear().apply();
    }
}
