package com.gamebooster.app.mods.pubgm;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * PubgmUe4Resolver — Dynamic Symbol & Memory Offset Resolver for PUBG Mobile 4.7.0.
 * Parses assets/frida/pubgm_offsets.json to expose verified libUE4.so JNI offsets
 * across Global (com.tencent.ig, 21740) and Flash (com.tencent.igfit, 21745), plus
 * libanogs.so / libanort.so / libCrashSight.so / libtgpa.so / libsentry-android.so / libcubehawk.so offsets.
 */
public final class PubgmUe4Resolver {

    private static final String TAG = "PubgmUe4Resolver";
    private static final Map<String, Long> sUe4Offsets = new HashMap<>();
    private static final Map<String, Long> sGlobalJniOffsets = new HashMap<>();
    private static final Map<String, Long> sFlashJniOffsets = new HashMap<>();
    private static final Map<String, Long> sAnticheatOffsets = new HashMap<>();
    private static volatile String sVersionName = "4.7.0";
    private static volatile int sVersionCode = 21740;
    private static volatile int sFlashVersionCode = 21745;
    private static volatile boolean sLoaded = false;

    private PubgmUe4Resolver() {}

    /**
     * Loads 4.7.0 offsets from assets/frida/pubgm_offsets.json.
     */
    public static synchronized void loadOffsets(Context ctx) {
        if (sLoaded || ctx == null) return;
        try {
            InputStream is = ctx.getAssets().open("frida/pubgm_offsets.json");
            byte[] buf = new byte[is.available()];
            int read = is.read(buf);
            is.close();
            if (read > 0) {
                parseJson(new String(buf, 0, read, StandardCharsets.UTF_8));
                sLoaded = true;
                Log.i(TAG, "✅ Loaded PUBG Mobile " + sVersionName + " offsets (ue4=" + sUe4Offsets.size()
                        + ", jniGlobal=" + sGlobalJniOffsets.size()
                        + ", jniFlash=" + sFlashJniOffsets.size()
                        + ", ac=" + sAnticheatOffsets.size() + ")");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load pubgm_offsets.json: " + t.getMessage(), t);
        }
    }

    /**
     * Parses a pubgm_offsets.json payload directly (also used by JVM unit tests).
     */
    public static synchronized void parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) return;
        try {
            sUe4Offsets.clear();
            sGlobalJniOffsets.clear();
            sFlashJniOffsets.clear();
            sAnticheatOffsets.clear();

            JSONObject root = new JSONObject(jsonStr);
            if (root.has("version")) {
                sVersionName = root.optString("version", "4.7.0");
            }
            if (root.has("versionCode")) {
                sVersionCode = parseIntSafe(root.optString("versionCode", "21740"), 21740);
            }
            if (root.has("flashVersionCode")) {
                sFlashVersionCode = parseIntSafe(root.optString("flashVersionCode", "21745"), 21745);
            }

            readOffsetObject(root.optJSONObject("ue4_offsets"), sUe4Offsets);

            JSONObject jni = root.optJSONObject("ue4_jni_offsets");
            if (jni != null) {
                readOffsetObject(jni.optJSONObject("global"), sGlobalJniOffsets);
                readOffsetObject(jni.optJSONObject("flash"), sFlashJniOffsets);
            }

            readOffsetObject(root.optJSONObject("anticheat_offsets"), sAnticheatOffsets);
        } catch (Throwable t) {
            Log.e(TAG, "parseJson failed: " + t.getMessage(), t);
        }
    }

    private static void readOffsetObject(JSONObject obj, Map<String, Long> dest) {
        if (obj == null) return;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            if (obj.isNull(k)) {
                dest.put(k, 0L);
                continue;
            }
            String hexVal = obj.optString(k, "");
            dest.put(k, parseHex(hexVal));
        }
    }

    public static long getUe4Offset(String symbol) {
        Long val = sUe4Offsets.get(symbol);
        return val != null ? val : 0L;
    }

    public static long getJniOffset(String symbol) {
        return getJniOffset("com.tencent.ig", symbol);
    }

    public static long getJniOffset(String packageName, String symbol) {
        boolean isFlash = packageName != null && packageName.toLowerCase().contains("igfit");
        Long val = (isFlash ? sFlashJniOffsets : sGlobalJniOffsets).get(symbol);
        if (val == null && isFlash) {
            val = sGlobalJniOffsets.get(symbol);
        }
        return val != null ? val : 0L;
    }

    public static long getAnticheatOffset(String symbol) {
        Long val = sAnticheatOffsets.get(symbol);
        return val != null ? val : 0L;
    }

    public static String getTargetVersionName() {
        return sVersionName;
    }

    public static int getTargetVersionCode(String packageName) {
        if (packageName != null && packageName.toLowerCase().contains("igfit")) {
            return sFlashVersionCode;
        }
        return sVersionCode;
    }

    private static int parseIntSafe(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parseHex(String hex) {
        if (hex == null) return 0L;
        String clean = hex.trim();
        if (clean.isEmpty() || "null".equalsIgnoreCase(clean)) return 0L;
        if (clean.startsWith("0x") || clean.startsWith("0X")) {
            clean = clean.substring(2);
        }
        try {
            return Long.parseLong(clean, 16);
        } catch (Exception e) {
            return 0L;
        }
    }
}
