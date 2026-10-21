package com.gamebooster.app.spoofer;

import android.util.Log;
import com.gamebooster.app.spoofer.brands.AsusRogProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SpoofProfileRegistry — Locked to single flagship profile: ASUS ROG Phone 9 Pro.
 *
 * All other brand modules (Samsung, Apple, Xiaomi, Nubia, etc.) are intentionally removed.
 * Every lookup method — getById(), getAll(), getDefault() — returns the ROG 9 Pro profile.
 */
public class SpoofProfileRegistry {

    private static final String TAG = "SpoofProfileRegistry";

    /** Single active profile ID — the only entry point for any spoof request. */
    public static final String ROG9_PRO_ID = "asus_rog9_pro";

    /** Flat map: profile ID → SpoofProfile (single entry) */
    private static final Map<String, SpoofProfile> ALL_BY_ID = new LinkedHashMap<>();

    /** Brand map: brand label → list of SpoofProfile (single brand) */
    private static final Map<String, List<SpoofProfile>> ALL_BY_BRAND = new LinkedHashMap<>();

    static {
        // Only the ROG 9 Pro profile is registered. All other brands are intentionally excluded.
        registerBrand(Collections.singletonList(AsusRogProfiles.getRog9ProProfile()));
    }

    private static void registerBrand(List<SpoofProfile> profiles) {
        if (profiles == null) return;
        for (SpoofProfile p : profiles) {
            if (p == null || p.id == null) continue;
            if (ALL_BY_ID.containsKey(p.id)) {
                Log.w(TAG, "Duplicate profile ID detected and ignored: " + p.id + " (" + p.displayName + ")");
                continue;
            }
            ALL_BY_ID.put(p.id, p);
            ALL_BY_BRAND
                .computeIfAbsent(p.brandLabel != null ? p.brandLabel : "Generic", k -> new ArrayList<>())
                .add(p);
        }
    }

    /**
     * Get a profile by its unique ID.
     * Always returns the ROG 9 Pro profile — any unrecognized ID falls back to it.
     */
    public static SpoofProfile getById(String id) {
        if (id != null && ALL_BY_ID.containsKey(id.trim())) {
            return ALL_BY_ID.get(id.trim());
        }
        // Fallback: always return ROG 9 Pro regardless of requested ID
        return ALL_BY_ID.get(ROG9_PRO_ID);
    }

    /**
     * Get all profiles for a given brand label.
     * Always returns the single ROG 9 Pro list regardless of requested brand.
     */
    public static List<SpoofProfile> getByBrand(String brandLabel) {
        return Collections.unmodifiableList(new java.util.ArrayList<>(ALL_BY_ID.values()));
    }

    /**
     * Get all profiles as a flat ID-keyed map.
     */
    public static Map<String, SpoofProfile> getAllProfiles() {
        return Collections.unmodifiableMap(ALL_BY_ID);
    }

    /**
     * Get list of all registered brand names — returns single "ASUS ROG" entry.
     */
    public static List<String> getBrandNames() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(ALL_BY_BRAND.keySet()));
    }

    /**
     * Get the full brand→profiles map.
     */
    public static Map<String, List<SpoofProfile>> getAllByBrand() {
        return Collections.unmodifiableMap(ALL_BY_BRAND);
    }

    /**
     * Get total number of registered profiles — always 1 (ROG 9 Pro).
     */
    public static int getTotalCount() {
        return ALL_BY_ID.size();
    }

    /**
     * Get first profile in a brand — always returns ROG 9 Pro.
     */
    public static SpoofProfile getFirstByBrand(String brandLabel) {
        return ALL_BY_ID.get(ROG9_PRO_ID);
    }

    /**
     * Get default flagship profile — locked to ASUS ROG Phone 9 Pro (Snapdragon 8 Elite / 185Hz).
     */
    public static SpoofProfile getDefaultProfile() {
        return ALL_BY_ID.get(ROG9_PRO_ID);
    }
}
