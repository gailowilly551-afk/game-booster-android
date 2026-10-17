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
 * PubgmUe4Resolver — Dynamic Symbol & Memory Offset Resolver for PUBG Mobile 4.6.0.
 * Parses assets/frida/pubgm_offsets.json to expose libUE4.so and libanogs.so offsets.
 */
public final class PubgmUe4Resolver {

    private static final String TAG = "PubgmUe4Resolver";
    private static final Map<String, Long> sUe4Offsets = new HashMap<>();
    private static final Map<String, Long> sAnticheatOffsets = new HashMap<>();
    private static volatile boolean sLoaded = false;

    private PubgmUe4Resolver() {}

    /**
     * Loads 4.6.0 offsets from assets/frida/pubgm_offsets.json.
     */
    public static synchronized void loadOffsets(Context ctx) {
        if (sLoaded || ctx == null) return;
        try {
            InputStream is = ctx.getAssets().open("frida/pubgm_offsets.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();

            String jsonStr = new String(buf, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonStr);

            if (root.has("ue4_offsets")) {
                JSONObject ue4 = root.getJSONObject("ue4_offsets");
                Iterator<String> keys = ue4.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    String hexVal = ue4.getString(k);
                    long val = parseHex(hexVal);
                    sUe4Offsets.put(k, val);
                }
            }

            if (root.has("anticheat_offsets")) {
                JSONObject ac = root.getJSONObject("anticheat_offsets");
                Iterator<String> keys = ac.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    String hexVal = ac.getString(k);
                    long val = parseHex(hexVal);
                    sAnticheatOffsets.put(k, val);
                }
            }

            sLoaded = true;
            Log.i(TAG, "✅ Loaded PUBG Mobile 4.6.0 UE4 offsets (" + sUe4Offsets.size()
                    + ") & Anti-Cheat offsets (" + sAnticheatOffsets.size() + ")");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load pubgm_offsets.json: " + t.getMessage(), t);
        }
    }

    public static long getUe4Offset(String symbol) {
        Long val = sUe4Offsets.get(symbol);
        return val != null ? val : 0L;
    }

    public static long getAnticheatOffset(String symbol) {
        Long val = sAnticheatOffsets.get(symbol);
        return val != null ? val : 0L;
    }

    private static long parseHex(String hex) {
        if (hex == null) return 0L;
        String clean = hex.trim();
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
