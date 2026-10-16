package com.gamebooster.app.mods.mlbb;

import android.content.Context;
import android.util.Log;

import com.gamebooster.app.engine.Il2cppDirectScanner;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * MlbbIl2cppResolver — Resolves target method RVAs, struct offsets, tokens, and pattern signatures
 * in libil2cpp.so. Loads deterministic offsets from assets/frida/mlbb_il2cpp_offsets.json
 * and bridges dynamic in-memory signature scanning via Il2cppDirectScanner as a fallback.
 */
public final class MlbbIl2cppResolver {

    private static final String TAG = "MlbbIl2cppResolver";
    private static final Map<String, Long> METHOD_CACHE = new HashMap<>();
    private static final Map<String, Long> FIELD_CACHE = new HashMap<>();
    private static final Map<String, Long> TOKEN_CACHE = new HashMap<>();
    private static final Map<String, Long> STRING_OFFSET_CACHE = new HashMap<>();
    private static final Map<String, String> STRING_CATEGORY_CACHE = new HashMap<>();
    private static final Map<String, byte[]> SIGNATURE_PATTERNS = new HashMap<>();
    private static final Map<String, String> SIGNATURE_MASKS = new HashMap<>();
    private static volatile boolean sLoadedFromAssets = false;

    static {
        initDefaults();
    }

    private MlbbIl2cppResolver() {}

    /**
     * Loads dumped offset table, fields, tokens, and signatures from assets/frida/mlbb_il2cpp_offsets.json.
     */
    public static synchronized void loadOffsets(Context ctx) {
        if (sLoadedFromAssets) return;

        try (InputStream is = ctx.getAssets().open("frida/mlbb_il2cpp_offsets.json")) {
            byte[] buf = new byte[is.available()];
            int read = is.read(buf);
            if (read <= 0) {
                initDefaults();
                return;
            }
            String jsonStr = new String(buf, "UTF-8");
            JSONObject json = new JSONObject(jsonStr);

            // 1. Methods
            JSONObject methods = json.optJSONObject("methods");
            if (methods != null) {
                for (Iterator<String> it = methods.keys(); it.hasNext(); ) {
                    String key = it.next();
                    try {
                        long offset = Long.decode(methods.getString(key));
                        METHOD_CACHE.put(key, offset);
                    } catch (Exception ignored) {}
                }
            }

            // 2. Fields
            JSONObject fields = json.optJSONObject("fields");
            if (fields != null) {
                for (Iterator<String> it = fields.keys(); it.hasNext(); ) {
                    String key = it.next();
                    try {
                        long offset = Long.decode(fields.getString(key));
                        FIELD_CACHE.put(key, offset);
                    } catch (Exception ignored) {}
                }
            }

            // 3. Tokens
            JSONObject tokens = json.optJSONObject("tokens");
            if (tokens != null) {
                for (Iterator<String> it = tokens.keys(); it.hasNext(); ) {
                    String key = it.next();
                    try {
                        long token = Long.decode(tokens.getString(key));
                        TOKEN_CACHE.put(key, token);
                    } catch (Exception ignored) {}
                }
            }

            // 4. String Patterns
            JSONObject stringPatterns = json.optJSONObject("string_patterns");
            if (stringPatterns != null) {
                for (Iterator<String> it = stringPatterns.keys(); it.hasNext(); ) {
                    String key = it.next();
                    JSONObject patObj = stringPatterns.optJSONObject(key);
                    if (patObj != null) {
                        String fileOff = patObj.optString("file_offset", "");
                        String cat = patObj.optString("category", "");
                        if (!fileOff.isEmpty()) {
                            try {
                                STRING_OFFSET_CACHE.put(key, Long.decode(fileOff));
                            } catch (Exception ignored) {}
                        }
                        if (!cat.isEmpty()) {
                            STRING_CATEGORY_CACHE.put(key, cat);
                        }
                    }
                }
            }

            // 5. Signatures
            JSONObject signatures = json.optJSONObject("signatures");
            if (signatures != null) {
                for (Iterator<String> it = signatures.keys(); it.hasNext(); ) {
                    String key = it.next();
                    JSONObject sigObj = signatures.optJSONObject(key);
                    if (sigObj != null) {
                        String hexPat = sigObj.optString("pattern", "");
                        String mask = sigObj.optString("mask", "");
                        if (!hexPat.isEmpty() && !mask.isEmpty()) {
                            byte[] patternBytes = hexStringToBytes(hexPat);
                            if (patternBytes != null) {
                                SIGNATURE_PATTERNS.put(key, patternBytes);
                                SIGNATURE_MASKS.put(key, mask);
                            }
                        }
                    }
                }
            }

            sLoadedFromAssets = true;
            Log.i(TAG, "Loaded " + METHOD_CACHE.size() + " methods, " + FIELD_CACHE.size()
                    + " fields, " + STRING_OFFSET_CACHE.size() + " string patterns, and "
                    + SIGNATURE_PATTERNS.size() + " signatures.");
        } catch (Throwable t) {
            Log.w(TAG, "Offset manifest load warning (using defaults): " + t.getMessage());
            initDefaults();
        }
    }

    private static void initDefaults() {
        // Fallback default RVAs for libil2cpp.so (v2.2.16.x)
        METHOD_CACHE.put("HeroDamageCalc_CalculateDamage", 0x0182C4D0L);
        METHOD_CACHE.put("HeroAttackSpeed_GetMultiplier",   0x01831E20L);
        METHOD_CACHE.put("HeroDefense_GetArmor",           0x01859E40L);
        METHOD_CACHE.put("SkillManager_GetCooldownTime",   0x0184A100L);
        METHOD_CACHE.put("FogOfWarManager_IsVisible",       0x019056B0L);
        METHOD_CACHE.put("CameraManager_SetHeight",         0x019128A0L);

        // Verified dump method symbols
        METHOD_CACHE.put("ISHOW_Init_SkillCoolDownCalc",   0x0184A100L);
        METHOD_CACHE.put("ISHOW_AddSkillCoolDownCalc",    0x0184A180L);
        METHOD_CACHE.put("Sync_Init_SkillCoolDownCalc",    0x0184A220L);
        METHOD_CACHE.put("FogOfWar_ShouldForceVisible",    0x019056E0L);
        METHOD_CACHE.put("FogOfWar_IsEntityHiddenByNewFOW", 0x01905740L);
        METHOD_CACHE.put("FogOfWar_IsBlockedByTerrain",    0x01905680L);
        METHOD_CACHE.put("FogOfWar_IsAttackExposed",       0x01905700L);
        METHOD_CACHE.put("m_iFieldOfView",                 0x019128A0L);
        METHOD_CACHE.put("fieldOfView",                    0x019128E0L);
        METHOD_CACHE.put("Freeze142_AntiCheatReport",      0x018D2100L);
        METHOD_CACHE.put("ResCheckStartHandler",           0x018C1500L);
        METHOD_CACHE.put("CountHurtValue",                 0x0182C4D0L);
        METHOD_CACHE.put("CalcVal",                        0x0182C520L);
        METHOD_CACHE.put("CheckCalcVal",                   0x0182C580L);
        METHOD_CACHE.put("GetMoveSpeed",                   0x0187B2C0L);
        METHOD_CACHE.put("MovementComponent_GetSpeed",     0x0187B2C0L);
        METHOD_CACHE.put("GetEmblemLv",                    0x01865200L);
        METHOD_CACHE.put("GetEmblemListWithRec",           0x01865280L);

        FIELD_CACHE.put("Hero_CurrentHp", 0x48L);
        FIELD_CACHE.put("Hero_MaxHp", 0x4CL);
        FIELD_CACHE.put("Hero_AttackPower", 0x54L);
        FIELD_CACHE.put("Hero_AttackSpeed", 0x5CL);
        FIELD_CACHE.put("Hero_MovementSpeed", 0x68L);
        FIELD_CACHE.put("m_BasePhyAtt", 0x54L);
        FIELD_CACHE.put("m_BaseMagAtt", 0x58L);
        FIELD_CACHE.put("m_LevelPhyAtt", 0x5CL);
        FIELD_CACHE.put("m_LevelMagAtt", 0x60L);
        FIELD_CACHE.put("m_AttSpeed", 0x64L);
        FIELD_CACHE.put("m_AttSpeed_show", 0x68L);
        FIELD_CACHE.put("m_AtkSpeedPer", 0x6CL);
        FIELD_CACHE.put("m_BaseSpeed", 0x70L);
        FIELD_CACHE.put("m_MoveSpeed", 0x74L);
        FIELD_CACHE.put("m_PhyBaseShield", 0x78L);
        FIELD_CACHE.put("m_MagBaseShield", 0x7CL);
        FIELD_CACHE.put("physicalDef", 0x88L);
        FIELD_CACHE.put("magicDef", 0x8CL);

        // Core class tokens
        TOKEN_CACHE.put("Camera",                 0x02000059L);
        TOKEN_CACHE.put("World",                  0x02000002L);
        TOKEN_CACHE.put("MolaBigFileDownloader",  0x02000003L);
        TOKEN_CACHE.put("FrameThreadDriver",      0x02000020L);
        TOKEN_CACHE.put("FogOfWar",               0x02000512L);
        TOKEN_CACHE.put("FOWRender",              0x02000514L);
        TOKEN_CACHE.put("FOWRevealer",            0x02000518L);
        TOKEN_CACHE.put("VisionSpriteFindEnemyHero", 0x02000520L);
        TOKEN_CACHE.put("AntiCheatReporter",      0x02000530L);
        TOKEN_CACHE.put("ResCheck",               0x02000540L);
        TOKEN_CACHE.put("MolaDownloadProvider",   0x02000550L);
        TOKEN_CACHE.put("FieldOfView",            0x02000560L);
        TOKEN_CACHE.put("LogicHurtCalc",          0x02000570L);
        TOKEN_CACHE.put("CData_Hero_Element",     0x02000575L);
        TOKEN_CACHE.put("EmblemV4Data",           0x02000580L);
        TOKEN_CACHE.put("EmblemDataManager",      0x02000585L);
        TOKEN_CACHE.put("BattleItem",             0x02000590L);
        TOKEN_CACHE.put("MovementComponent",      0x02000595L);

        // Verified dump string patterns
        STRING_OFFSET_CACHE.put("RefFogOfWarMesh", 0x30624bL);
        STRING_CATEGORY_CACHE.put("RefFogOfWarMesh", "FogOfWar");
        STRING_OFFSET_CACHE.put("RefFogOfWarCommandBuffer", 0x30625bL);
        STRING_CATEGORY_CACHE.put("RefFogOfWarCommandBuffer", "FogOfWar");
        STRING_OFFSET_CACHE.put("Freeze142_AntiCheatReport", 0x3a62d3L);
        STRING_CATEGORY_CACHE.put("Freeze142_AntiCheatReport", "AntiCheat");
        STRING_OFFSET_CACHE.put("ResCheckStartHandler", 0x3035ccL);
        STRING_CATEGORY_CACHE.put("ResCheckStartHandler", "ResCheck");
        STRING_OFFSET_CACHE.put("CountHurtValue", 0x6569b3L);
        STRING_CATEGORY_CACHE.put("CountHurtValue", "PhysicalDamage");
        STRING_OFFSET_CACHE.put("CalcVal", 0x09e4a5L);
        STRING_CATEGORY_CACHE.put("CalcVal", "PhysicalDamage");
        STRING_OFFSET_CACHE.put("LogicHurtCalc", 0x656989L);
        STRING_CATEGORY_CACHE.put("LogicHurtCalc", "PhysicalDamage");
        STRING_OFFSET_CACHE.put("m_HurtData", 0x656a13L);
        STRING_CATEGORY_CACHE.put("m_HurtData", "PhysicalDamage");
        STRING_OFFSET_CACHE.put("m_BasePhyAtt", 0x0c6a8dL);
        STRING_CATEGORY_CACHE.put("m_BasePhyAtt", "PhysicalDamage");
        STRING_OFFSET_CACHE.put("m_BaseMagAtt", 0x0c6a99L);
        STRING_CATEGORY_CACHE.put("m_BaseMagAtt", "MagicDamage");
        STRING_OFFSET_CACHE.put("m_AttSpeed", 0x0c6aa5L);
        STRING_CATEGORY_CACHE.put("m_AttSpeed", "AttackSpeed");
        STRING_OFFSET_CACHE.put("m_MoveSpeed", 0x0c6a82L);
        STRING_CATEGORY_CACHE.put("m_MoveSpeed", "MovementSpeed");
        STRING_OFFSET_CACHE.put("m_PhyBaseShield", 0x0c6abeL);
        STRING_CATEGORY_CACHE.put("m_PhyBaseShield", "PhysicalDefense");
        STRING_OFFSET_CACHE.put("m_MagBaseShield", 0x0c6acdL);
        STRING_CATEGORY_CACHE.put("m_MagBaseShield", "MagicDefense");
        STRING_OFFSET_CACHE.put("EmblemV4Data", 0x0b7a4fL);
        STRING_CATEGORY_CACHE.put("EmblemV4Data", "Emblem");
    }

    public static long getOffset(String key) {
        Long val = METHOD_CACHE.get(key);
        return val != null ? val : 0L;
    }

    public static long getMethodOffset(String key) {
        return getOffset(key);
    }

    public static long getFieldOffset(String key) {
        Long val = FIELD_CACHE.get(key);
        return val != null ? val : 0L;
    }

    public static long getToken(String key) {
        Long val = TOKEN_CACHE.get(key);
        return val != null ? val : 0L;
    }

    public static long getStringOffset(String key) {
        Long val = STRING_OFFSET_CACHE.get(key);
        return val != null ? val : 0L;
    }

    public static String getStringCategory(String key) {
        String val = STRING_CATEGORY_CACHE.get(key);
        return val != null ? val : "";
    }

    /**
     * Attempts dynamic in-memory pattern scan via Il2cppDirectScanner if static RVA is unresolved.
     */
    public static long resolveDynamicOffset(String key) {
        long staticOffset = getOffset(key);
        if (staticOffset != 0L) {
            return staticOffset;
        }

        if (!Il2cppDirectScanner.isAvailable()) {
            return 0L;
        }

        byte[] pat = SIGNATURE_PATTERNS.get(key);
        String mask = SIGNATURE_MASKS.get(key);
        if (pat == null || mask == null) {
            return 0L;
        }

        long matchedAddr = Il2cppDirectScanner.scanPattern(pat, mask);
        if (matchedAddr != 0L) {
            long base = Il2cppDirectScanner.getIl2cppBase();
            long rva = matchedAddr - base;
            METHOD_CACHE.put(key, rva);
            Log.i(TAG, "Dynamically resolved RVA for " + key + " -> 0x" + Long.toHexString(rva));
            return rva;
        }

        return 0L;
    }

    private static byte[] hexStringToBytes(String s) {
        if (s == null || s.length() % 2 != 0) return null;
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}

