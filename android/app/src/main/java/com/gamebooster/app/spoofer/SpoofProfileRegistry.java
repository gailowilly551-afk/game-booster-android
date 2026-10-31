package com.gamebooster.app.spoofer;

import android.util.Log;
import com.gamebooster.app.spoofer.brands.AppleProfiles;
import com.gamebooster.app.spoofer.brands.AsusRogProfiles;
import com.gamebooster.app.spoofer.brands.BlackSharkProfiles;
import com.gamebooster.app.spoofer.brands.HuaweiProfiles;
import com.gamebooster.app.spoofer.brands.LenovoLegionProfiles;
import com.gamebooster.app.spoofer.brands.NubiaProfiles;
import com.gamebooster.app.spoofer.brands.OnePlusProfiles;
import com.gamebooster.app.spoofer.brands.OppoProfiles;
import com.gamebooster.app.spoofer.brands.RealmeProfiles;
import com.gamebooster.app.spoofer.brands.SamsungProfiles;
import com.gamebooster.app.spoofer.brands.TranssionProfiles;
import com.gamebooster.app.spoofer.brands.VivoProfiles;
import com.gamebooster.app.spoofer.brands.XiaomiProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SpoofProfileRegistry — Central registry for all spoof profiles across major OEM flagships.
 * Supports Asus ROG, RedMagic (Nubia), Samsung, Xiaomi/Redmi, Vivo/iQOO,
 * Transsion (Infinix/Tecno), Huawei/Honor, OnePlus, Oppo, Realme, BlackShark, Lenovo Legion, and Apple.
 */
public class SpoofProfileRegistry {

    private static final String TAG = "SpoofProfileRegistry";

    /** Default flagship profile ID fallback */
    public static final String ROG9_PRO_ID = "asus_rog9_pro";

    /** Flat map: profile ID -> SpoofProfile */
    private static final Map<String, SpoofProfile> ALL_BY_ID = new LinkedHashMap<>();

    /** Brand map: brand label -> list of SpoofProfile */
    private static final Map<String, List<SpoofProfile>> ALL_BY_BRAND = new LinkedHashMap<>();

    static {
        // Register all OEM flagship profiles across Snapdragon, Dimensity, Exynos, Tensor, and Apple suites
        registerBrand(com.gamebooster.app.spoofer.brands.AsusRogProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.NubiaProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.SamsungProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.XiaomiProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.VivoProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.TranssionProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.OnePlusProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.OppoProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.RealmeProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.BlackSharkProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.LenovoLegionProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.HuaweiProfiles.getProfiles());
        registerBrand(com.gamebooster.app.spoofer.brands.AppleProfiles.getProfiles());
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
                .computeIfAbsent(p.brandLabel != null ? p.brandLabel : (p.brand != null ? p.brand : "Generic"), k -> new ArrayList<>())
                .add(p);
        }
    }

    /**
     * Get a profile by its unique ID.
     */
    public static SpoofProfile getById(String id) {
        if (id != null && ALL_BY_ID.containsKey(id.trim())) {
            return ALL_BY_ID.get(id.trim());
        }
        // Fallback: ROG 9 Pro or first available
        SpoofProfile fallback = ALL_BY_ID.get(ROG9_PRO_ID);
        if (fallback != null) return fallback;
        if (!ALL_BY_ID.isEmpty()) return ALL_BY_ID.values().iterator().next();
        return null;
    }

    /**
     * Returns default flagship profile.
     */
    public static SpoofProfile getDefaultProfile() {
        return getById(ROG9_PRO_ID);
    }

    /**
     * Get all profiles for a given brand query with smart aliasing and multi-brand grouping.
     * Supports:
     * - "Infinix" / "Tecno" / "Transsion" -> matches Infinix and Tecno models
     * - "Xiaomi" / "Redmi" / "Poco" -> matches Xiaomi, Redmi, and Poco models
     * - "Vivo" / "iQOO" -> matches Vivo and iQOO models
     * - "Huawei" / "Honor" -> matches Huawei and Honor models
     * - "Samsung" -> matches Samsung Galaxy models
     * - "Asus" / "ROG" -> matches Asus ROG models
     * - "Nubia" / "RedMagic" -> matches Nubia RedMagic models
     */
    public static List<SpoofProfile> getByBrand(String brandQuery) {
        if (brandQuery == null || brandQuery.trim().isEmpty()) return Collections.emptyList();
        String query = brandQuery.trim().toLowerCase();

        if (query.equals("all") || query.equals("all brands")) {
            return Collections.unmodifiableList(new ArrayList<>(ALL_BY_ID.values()));
        }

        List<SpoofProfile> results = new ArrayList<>();
        java.util.Set<String> addedIds = new java.util.HashSet<>();

        boolean matchInfinix = query.contains("infinix") || query.contains("transsion");
        boolean matchTecno = query.contains("tecno") || query.contains("transsion");
        boolean matchXiaomi = query.contains("xiaomi") || query.contains("showme");
        boolean matchRedmi = query.contains("redmi") || query.contains("poco") || query.contains("showme");
        boolean matchVivo = query.contains("vivo") || query.contains("iqoo");
        boolean matchHuawei = query.contains("huawei") || query.contains("huwawi") || query.contains("honor");
        boolean matchSamsung = query.contains("samsung") || query.contains("galaxy");
        boolean matchRog = query.contains("rog") || query.contains("asus");
        boolean matchNubia = query.contains("nubia") || query.contains("redmagic") || query.contains("red magic");
        boolean matchRealme = query.contains("realme");
        boolean matchOnePlus = query.contains("oneplus") || query.contains("one plus") || query.contains("1+");
        boolean matchOppo = query.contains("oppo");
        boolean matchApple = query.contains("apple") || query.contains("iphone") || query.contains("ipad");
        boolean matchBlackShark = query.contains("blackshark") || query.contains("black shark");
        boolean matchLenovo = query.contains("lenovo") || query.contains("legion");

        for (SpoofProfile p : ALL_BY_ID.values()) {
            if (p == null || addedIds.contains(p.id)) continue;

            String brand = (p.brand != null ? p.brand : "").toLowerCase();
            String brandLabel = (p.brandLabel != null ? p.brandLabel : "").toLowerCase();
            String name = (p.displayName != null ? p.displayName : "").toLowerCase();

            boolean matched = false;
            if (matchInfinix && (brandLabel.contains("infinix") || brand.contains("infinix") || name.contains("infinix"))) matched = true;
            else if (matchTecno && (brandLabel.contains("tecno") || brand.contains("tecno") || name.contains("tecno"))) matched = true;
            else if (matchXiaomi && (brandLabel.contains("xiaomi") || brand.contains("xiaomi") || name.contains("xiaomi"))) matched = true;
            else if (matchRedmi && (brandLabel.contains("redmi") || brand.contains("redmi") || brandLabel.contains("poco") || name.contains("redmi") || name.contains("poco"))) matched = true;
            else if (matchVivo && (brandLabel.contains("vivo") || brand.contains("vivo") || brandLabel.contains("iqoo") || name.contains("iqoo"))) matched = true;
            else if (matchHuawei && (brandLabel.contains("huawei") || brand.contains("huawei") || brandLabel.contains("honor") || name.contains("honor"))) matched = true;
            else if (matchSamsung && (brandLabel.contains("samsung") || brand.contains("samsung") || name.contains("galaxy"))) matched = true;
            else if (matchRog && (brandLabel.contains("asus") || brand.contains("asus") || name.contains("rog"))) matched = true;
            else if (matchNubia && (brandLabel.contains("nubia") || brand.contains("nubia") || name.contains("redmagic"))) matched = true;
            else if (matchRealme && (brandLabel.contains("realme") || brand.contains("realme") || name.contains("realme"))) matched = true;
            else if (matchOnePlus && (brandLabel.contains("oneplus") || brand.contains("oneplus") || name.contains("oneplus"))) matched = true;
            else if (matchOppo && (brandLabel.contains("oppo") || brand.contains("oppo") || name.contains("oppo"))) matched = true;
            else if (matchApple && (brandLabel.contains("apple") || brand.contains("apple") || name.contains("iphone") || name.contains("ipad"))) matched = true;
            else if (matchBlackShark && (brandLabel.contains("black") || name.contains("black shark"))) matched = true;
            else if (matchLenovo && (brandLabel.contains("lenovo") || name.contains("legion"))) matched = true;
            else if (brandLabel.contains(query) || brand.contains(query) || name.contains(query)) {
                matched = true;
            }

            if (matched) {
                addedIds.add(p.id);
                results.add(p);
            }
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Get all profiles as a flat ID-keyed map.
     */
    public static Map<String, SpoofProfile> getAllProfiles() {
        return Collections.unmodifiableMap(ALL_BY_ID);
    }

    /**
     * Get list of all registered brand names.
     */
    public static List<String> getBrandNames() {
        return Collections.unmodifiableList(new ArrayList<>(ALL_BY_BRAND.keySet()));
    }

    /**
     * Get the full brand->profiles map.
     */
    public static Map<String, List<SpoofProfile>> getAllByBrand() {
        return Collections.unmodifiableMap(ALL_BY_BRAND);
    }

    /**
     * Get total number of registered profiles.
     */
    public static int getTotalCount() {
        return ALL_BY_ID.size();
    }

    /**
     * Get first profile in a brand.
     */
    public static SpoofProfile getFirstInBrand(String brandLabel) {
        List<SpoofProfile> list = getByBrand(brandLabel);
        return (list != null && !list.isEmpty()) ? list.get(0) : getById(ROG9_PRO_ID);
    }

    /**
     * Check if a profile ID is registered.
     */
    public static boolean hasId(String id) {
        return id != null && ALL_BY_ID.containsKey(id.trim());
    }
}
