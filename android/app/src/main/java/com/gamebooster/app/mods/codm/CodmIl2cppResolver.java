package com.gamebooster.app.mods.codm;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * CodmIl2cppResolver — Resolves target method RVAs, field offsets, tokens, and pattern signatures
 * for Call of Duty Mobile (v1.6.57 Unity IL2CPP v23 monolithic engine in libunity.so).
 * Reads deterministic offset maps and tokens from assets/frida/codm_offsets.json.
 */
public final class CodmIl2cppResolver {

    private static final String TAG = "CodmIl2cppResolver";
    private static final Map<String, Long> OFFSET_CACHE = new HashMap<>();
    private static final Map<String, Long> TOKEN_CACHE = new HashMap<>();
    private static final Map<String, Long> FIELD_CACHE = new HashMap<>();
    private static final Map<String, Long> STRING_OFFSET_CACHE = new HashMap<>();
    private static final Map<String, String> STRING_CATEGORY_CACHE = new HashMap<>();
    private static volatile boolean sLoadedFromAssets = false;

    static {
        initDefaults();
    }

    private CodmIl2cppResolver() {}

    /**
     * Loads dumped offset table, fields, tokens, and string patterns from assets/frida/codm_offsets.json.
     */
    public static synchronized void loadOffsets(Context ctx) {
        if (sLoadedFromAssets) return;

        try (InputStream is = ctx.getAssets().open("frida/codm_offsets.json")) {
            byte[] buf = new byte[is.available()];
            int read = is.read(buf);
            if (read <= 0) {
                initDefaults();
                return;
            }
            String jsonStr = new String(buf, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);

            // 1. Target method RVAs / offsets
            JSONObject offsets = json.optJSONObject("offsets");
            if (offsets != null) {
                for (Iterator<String> it = offsets.keys(); it.hasNext(); ) {
                    String key = it.next();
                    try {
                        long val = Long.decode(offsets.getString(key));
                        OFFSET_CACHE.put(key, val);
                    } catch (Exception ignored) {}
                }
            }

            // 2. Metadata Tokens
            JSONObject tokens = json.optJSONObject("tokens");
            if (tokens != null) {
                for (Iterator<String> it = tokens.keys(); it.hasNext(); ) {
                    String key = it.next();
                    try {
                        long val = Long.decode(tokens.getString(key));
                        TOKEN_CACHE.put(key, val);
                    } catch (Exception ignored) {}
                }
            }

            // 3. Field Offsets
            JSONObject fields = json.optJSONObject("field_offsets");
            if (fields != null) {
                for (Iterator<String> it = fields.keys(); it.hasNext(); ) {
                    String key = it.next();
                    try {
                        long val = Long.decode(fields.getString(key));
                        FIELD_CACHE.put(key, val);
                    } catch (Exception ignored) {}
                }
            }

            // 4. String Patterns & Signatures
            JSONObject stringPatterns = json.optJSONObject("string_patterns");
            if (stringPatterns != null) {
                for (Iterator<String> it = stringPatterns.keys(); it.hasNext(); ) {
                    String key = it.next();
                    JSONObject pat = stringPatterns.optJSONObject(key);
                    if (pat != null) {
                        String bufStr = pat.optString("buffer_offset", "");
                        if (bufStr.isEmpty()) bufStr = pat.optString("file_offset", "");
                        if (!bufStr.isEmpty()) {
                            try {
                                long val = Long.decode(bufStr);
                                STRING_OFFSET_CACHE.put(key, val);
                            } catch (Exception ignored) {}
                        }
                        String cat = pat.optString("category", "");
                        if (!cat.isEmpty()) {
                            STRING_CATEGORY_CACHE.put(key, cat);
                        }
                    }
                }
            }

            sLoadedFromAssets = true;
            Log.i(TAG, "Loaded " + OFFSET_CACHE.size() + " offsets, " +
                       TOKEN_CACHE.size() + " tokens, " +
                       FIELD_CACHE.size() + " fields, and " +
                       STRING_OFFSET_CACHE.size() + " strings for CODM v1.6.57");
        } catch (Exception e) {
            Log.w(TAG, "Failed to load codm_offsets.json from assets: " + e.getMessage());
            initDefaults();
        }
    }

    private static void initDefaults() {
        // Fallback hardcoded values for v1.6.57
        OFFSET_CACHE.put("Camera_get_main", 0x00F82410L);
        OFFSET_CACHE.put("Transform_get_position", 0x00ED8920L);
        OFFSET_CACHE.put("Transform_set_position", 0x00ED8A30L);
        OFFSET_CACHE.put("WeaponController_RecoilModifier", 0x01E4A9C0L);
        OFFSET_CACHE.put("WeaponController_SpreadModifier", 0x01E4AD50L);
        OFFSET_CACHE.put("PlayerController_SpeedMultiplier", 0x01D382B0L);

        // Core Combat & Engine Tokens
        TOKEN_CACHE.put("RecoilScaleWeaponShake", 0x06012c6bL);
        TOKEN_CACHE.put("ShotSpread", 0x06014791L);
        TOKEN_CACHE.put("RandomShotSpread", 0x060148bfL);
        TOKEN_CACHE.put("CalcShotSpreadSize", 0x06014a9fL);
        TOKEN_CACHE.put("UseAimAssist", 0x0601480bL);
        TOKEN_CACHE.put("AimAssistDis", 0x06014815L);
        TOKEN_CACHE.put("RecoilUpBase", 0x06014851L);
        TOKEN_CACHE.put("RecoilUpMax", 0x06014853L);
        TOKEN_CACHE.put("RecoilLateralModifier", 0x06014855L);
        TOKEN_CACHE.put("EnableAimAssistanceForSniper", 0x0601d7c7L);
        TOKEN_CACHE.put("AimAssistanceSpeed", 0x0601d7cdL);
        TOKEN_CACHE.put("RecoilFactor", 0x060202b1L);
        TOKEN_CACHE.put("IsOpenAimAssist", 0x060203a5L);
        TOKEN_CACHE.put("WeaponController", 0x0600519L);
        TOKEN_CACHE.put("PlayerController", 0x0600b96fL);
        TOKEN_CACHE.put("FieldOfView", 0x06007d49L);
        TOKEN_CACHE.put("Camera", 0x06002d24L);

        // Field Struct Offsets
        FIELD_CACHE.put("GAS_MainFireFunnelTask_RecoilUpBase", 0x28L);
        FIELD_CACHE.put("GAS_MainFireFunnelTask_RecoilUpModifier", 0x2CL);
        FIELD_CACHE.put("GAS_MeleeAttackTask_RecoilUpMax", 0x30L);
        FIELD_CACHE.put("GAS_MeleeAttackTask_RecoilLateralBase", 0x34L);
        FIELD_CACHE.put("GAS_MeleeAttackTask_RecoilLateralModifier", 0x38L);
        FIELD_CACHE.put("GAS_MeleeAttackTask_Damage", 0x54L);
        FIELD_CACHE.put("RootMotion_Dynamics_AimAssistRot", 0x68L);

        // Verified String Pattern Buffer Offsets & Categories
        STRING_OFFSET_CACHE.put("ShotSpread", 0x2a06a7L);
        STRING_CATEGORY_CACHE.put("ShotSpread", "Spread");
        STRING_OFFSET_CACHE.put("RandomShotSpread", 0x2b7667L);
        STRING_CATEGORY_CACHE.put("RandomShotSpread", "Spread");
        STRING_OFFSET_CACHE.put("CalcShotSpreadSize", 0x2b768eL);
        STRING_CATEGORY_CACHE.put("CalcShotSpreadSize", "Spread");
        STRING_OFFSET_CACHE.put("ShotSpreadAngle", 0x252a99L);
        STRING_CATEGORY_CACHE.put("ShotSpreadAngle", "Spread");

        STRING_OFFSET_CACHE.put("RecoilScaleWeaponShake", 0x24c147L);
        STRING_CATEGORY_CACHE.put("RecoilScaleWeaponShake", "Recoil");
        STRING_OFFSET_CACHE.put("RecoilUpBase", 0x2519f6L);
        STRING_CATEGORY_CACHE.put("RecoilUpBase", "Recoil");
        STRING_OFFSET_CACHE.put("RecoilUpMax", 0x251a14L);
        STRING_CATEGORY_CACHE.put("RecoilUpMax", "Recoil");
        STRING_OFFSET_CACHE.put("RecoilLateralBase", 0x251a20L);
        STRING_CATEGORY_CACHE.put("RecoilLateralBase", "Recoil");
        STRING_OFFSET_CACHE.put("RecoilLateralModifier", 0x251a32L);
        STRING_CATEGORY_CACHE.put("RecoilLateralModifier", "Recoil");
        STRING_OFFSET_CACHE.put("RecoilFactor", 0x303030L);
        STRING_CATEGORY_CACHE.put("RecoilFactor", "Recoil");

        STRING_OFFSET_CACHE.put("UseAimAssist", 0x251786L);
        STRING_CATEGORY_CACHE.put("UseAimAssist", "AimAssist");
        STRING_OFFSET_CACHE.put("AimAssistDis", 0x2517caL);
        STRING_CATEGORY_CACHE.put("AimAssistDis", "AimAssist");
        STRING_OFFSET_CACHE.put("AimAssistRot", 0x2b91d6L);
        STRING_CATEGORY_CACHE.put("AimAssistRot", "AimAssist");
        STRING_OFFSET_CACHE.put("IsOpenAimAssist", 0x244c50L);
        STRING_CATEGORY_CACHE.put("IsOpenAimAssist", "AimAssist");
        STRING_OFFSET_CACHE.put("AimAssistanceSpeed", 0x24487dL);
        STRING_CATEGORY_CACHE.put("AimAssistanceSpeed", "AimAssist");
        STRING_OFFSET_CACHE.put("OverrideAimAssistanceSpeed", 0x254cb4L);
        STRING_CATEGORY_CACHE.put("OverrideAimAssistanceSpeed", "AimAssist");
        STRING_OFFSET_CACHE.put("EnableAimAssistanceForSniper", 0x383824L);
        STRING_CATEGORY_CACHE.put("EnableAimAssistanceForSniper", "AimAssist");
        STRING_OFFSET_CACHE.put("AimAssistanceColliderRadius", 0x4e8991L);
        STRING_CATEGORY_CACHE.put("AimAssistanceColliderRadius", "AimAssist");

        STRING_OFFSET_CACHE.put("DamageAddition", 0x38686bL);
        STRING_CATEGORY_CACHE.put("DamageAddition", "CombatDamage");
        STRING_OFFSET_CACHE.put("DamageAmount", 0x884889L);
        STRING_CATEGORY_CACHE.put("DamageAmount", "CombatDamage");
        STRING_OFFSET_CACHE.put("A2DBuffEffect_CauseDamage", 0x16eed1L);
        STRING_CATEGORY_CACHE.put("A2DBuffEffect_CauseDamage", "CombatDamage");
        STRING_OFFSET_CACHE.put("A2DBuffEffect_DamageIncrease", 0x16ef4fL);
        STRING_CATEGORY_CACHE.put("A2DBuffEffect_DamageIncrease", "CombatDamage");
        STRING_OFFSET_CACHE.put("Headshot", 0x2f5677L);
        STRING_CATEGORY_CACHE.put("Headshot", "CombatDamage");
        STRING_OFFSET_CACHE.put("BulletTrack", 0x24dfd2L);
        STRING_CATEGORY_CACHE.put("BulletTrack", "CombatDamage");
        STRING_OFFSET_CACHE.put("BasicBulletTrackThickness", 0x24dfd2L);
        STRING_CATEGORY_CACHE.put("BasicBulletTrackThickness", "CombatDamage");

        STRING_OFFSET_CACHE.put("CatPlayerController", 0x17865fL);
        STRING_CATEGORY_CACHE.put("CatPlayerController", "PlayerController");
        STRING_OFFSET_CACHE.put("PlayerController", 0x2659c3L);
        STRING_CATEGORY_CACHE.put("PlayerController", "PlayerController");
        STRING_OFFSET_CACHE.put("IsEnemy", 0x3863ebL);
        STRING_CATEGORY_CACHE.put("IsEnemy", "PlayerController");
        STRING_OFFSET_CACHE.put("bIsEnemy", 0x32827bL);
        STRING_CATEGORY_CACHE.put("bIsEnemy", "PlayerController");
        STRING_OFFSET_CACHE.put("GetTargetEnemy", 0x28a136L);
        STRING_CATEGORY_CACHE.put("GetTargetEnemy", "PlayerController");

        STRING_OFFSET_CACHE.put("WeaponController", 0x35a56dL);
        STRING_CATEGORY_CACHE.put("WeaponController", "WeaponController");
        STRING_OFFSET_CACHE.put("GetSelectWeaponControllerType", 0x19c3eaL);
        STRING_CATEGORY_CACHE.put("GetSelectWeaponControllerType", "WeaponController");
        STRING_OFFSET_CACHE.put("mSelectWeaponController", 0x19c6deL);
        STRING_CATEGORY_CACHE.put("mSelectWeaponController", "WeaponController");

        STRING_OFFSET_CACHE.put("TacticalRadarMySelfSprite", 0x1778bcL);
        STRING_CATEGORY_CACHE.put("TacticalRadarMySelfSprite", "Radar");
        STRING_OFFSET_CACHE.put("CreateRadarController", 0x178c76L);
        STRING_CATEGORY_CACHE.put("CreateRadarController", "Radar");

        STRING_OFFSET_CACHE.put("Camera", 0x2c4efL);
        STRING_CATEGORY_CACHE.put("Camera", "Camera");
        STRING_OFFSET_CACHE.put("FieldOfView", 0x52e9ccL);
        STRING_CATEGORY_CACHE.put("FieldOfView", "Camera");

        STRING_OFFSET_CACHE.put("GetRecoilFactorInGame", 0x3bd30bL);
        STRING_CATEGORY_CACHE.put("GetRecoilFactorInGame", "Recoil");
        STRING_OFFSET_CACHE.put("GetRecoilUpBase", 0x4660e8L);
        STRING_CATEGORY_CACHE.put("GetRecoilUpBase", "Recoil");
        STRING_OFFSET_CACHE.put("MinShotSpreadSize", 0x253c1cL);
        STRING_CATEGORY_CACHE.put("MinShotSpreadSize", "Spread");
        STRING_OFFSET_CACHE.put("MaxShotSpreadSize", 0x253c2eL);
        STRING_CATEGORY_CACHE.put("MaxShotSpreadSize", "Spread");
        STRING_OFFSET_CACHE.put("BlurMinSpread", 0x893f30L);
        STRING_CATEGORY_CACHE.put("BlurMinSpread", "Spread");
        STRING_OFFSET_CACHE.put("BlurSpread", 0x893f54L);
        STRING_CATEGORY_CACHE.put("BlurSpread", "Spread");
        STRING_OFFSET_CACHE.put("MpIsOpenAimAssistSet", 0x80a638L);
        STRING_CATEGORY_CACHE.put("MpIsOpenAimAssistSet", "AimAssist");
        STRING_OFFSET_CACHE.put("BrIsOpenAimAssistSet", 0x80aa9aL);
        STRING_CATEGORY_CACHE.put("BrIsOpenAimAssistSet", "AimAssist");
        STRING_OFFSET_CACHE.put("m_DamageAmount", 0x884889L);
        STRING_CATEGORY_CACHE.put("m_DamageAmount", "CombatDamage");
        STRING_OFFSET_CACHE.put("BRCatGamePlayerController", 0x1787c1L);
        STRING_CATEGORY_CACHE.put("BRCatGamePlayerController", "PlayerController");
    }

    public static long getOffset(String key) {
        Long val = OFFSET_CACHE.get(key);
        return val != null ? val : 0L;
    }

    public static long getToken(String key) {
        Long val = TOKEN_CACHE.get(key);
        return val != null ? val : 0L;
    }

    public static long getFieldOffset(String key) {
        Long val = FIELD_CACHE.get(key);
        return val != null ? val : 0L;
    }

    public static long getStringOffset(String key) {
        Long val = STRING_OFFSET_CACHE.get(key);
        return val != null ? val : 0L;
    }

    public static String getStringCategory(String key) {
        String cat = STRING_CATEGORY_CACHE.get(key);
        return cat != null ? cat : "";
    }
}
