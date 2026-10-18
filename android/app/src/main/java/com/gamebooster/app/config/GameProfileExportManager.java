package com.gamebooster.app.config;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * GameProfileExportManager
 * Handles JSON backup, export, and import of Game Booster profiles (.gbcfg).
 * Encapsulates game display Hz profiles, mod configurations, and spoofing parameters.
 */
public final class GameProfileExportManager {

    private static final String TAG = "GameProfileExport";
    public static final String FILE_EXTENSION = ".gbcfg";

    private GameProfileExportManager() {}

    /**
     * Serializes all booster profiles and preferences into a portable JSON string.
     */
    public static String exportProfilesToJson(Context context) {
        if (context == null) return "{}";
        Context app = context.getApplicationContext();

        JSONObject root = new JSONObject();
        try {
            root.put("format_version", 2);
            root.put("created_timestamp", System.currentTimeMillis());
            root.put("device_model", Build.MANUFACTURER + " " + Build.MODEL);
            root.put("android_release", Build.VERSION.RELEASE);
            root.put("sdk_int", Build.VERSION.SDK_INT);

            // 1. Per-game performance profiles
            JSONObject perfObj = new JSONObject();
            SharedPreferences perfPrefs = app.getSharedPreferences("per_game_performance_profiles", Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : perfPrefs.getAll().entrySet()) {
                perfObj.put(entry.getKey(), entry.getValue());
            }
            root.put("per_game_performance_profiles", perfObj);

            // 2. Game Mod profiles (MLBB, CODM, PUBGM)
            JSONObject modObj = new JSONObject();
            SharedPreferences modPrefs = app.getSharedPreferences("game_mod_profile", Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : modPrefs.getAll().entrySet()) {
                modObj.put(entry.getKey(), entry.getValue());
            }
            root.put("game_mod_profile", modObj);

            // 3. Device Spoof profiles
            JSONObject spoofObj = new JSONObject();
            SharedPreferences spoofPrefs = app.getSharedPreferences("device_spoof_prefs", Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : spoofPrefs.getAll().entrySet()) {
                spoofObj.put(entry.getKey(), entry.getValue());
            }
            root.put("device_spoof_prefs", spoofObj);

            // 4. HUD Overlay Preferences
            JSONObject hudObj = new JSONObject();
            SharedPreferences hudPrefs = app.getSharedPreferences("gamebooster_hud_prefs", Context.MODE_PRIVATE);
            for (Map.Entry<String, ?> entry : hudPrefs.getAll().entrySet()) {
                hudObj.put(entry.getKey(), entry.getValue());
            }
            root.put("gamebooster_hud_prefs", hudObj);

            return root.toString(2);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize export profiles: " + e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * Imports and restores profiles from a JSON string.
     */
    public static boolean importProfilesFromJson(Context context, String jsonStr) {
        if (context == null || jsonStr == null || jsonStr.trim().isEmpty()) return false;
        Context app = context.getApplicationContext();

        try {
            JSONObject root = new JSONObject(jsonStr);

            // Restore per-game performance profiles
            if (root.has("per_game_performance_profiles")) {
                JSONObject perfObj = root.getJSONObject("per_game_performance_profiles");
                SharedPreferences.Editor editor = app.getSharedPreferences("per_game_performance_profiles", Context.MODE_PRIVATE).edit();
                for (java.util.Iterator<String> it = perfObj.keys(); it.hasNext(); ) {
                    String key = it.next();
                    Object val = perfObj.get(key);
                    if (val instanceof Integer) editor.putInt(key, (Integer) val);
                    else if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                    else if (val instanceof Float) editor.putFloat(key, (Float) val);
                    else if (val instanceof Long) editor.putLong(key, (Long) val);
                    else if (val instanceof String) editor.putString(key, (String) val);
                }
                editor.apply();
            }

            // Restore Game Mod profiles
            if (root.has("game_mod_profile")) {
                JSONObject modObj = root.getJSONObject("game_mod_profile");
                SharedPreferences.Editor editor = app.getSharedPreferences("game_mod_profile", Context.MODE_PRIVATE).edit();
                for (java.util.Iterator<String> it = modObj.keys(); it.hasNext(); ) {
                    String key = it.next();
                    Object val = modObj.get(key);
                    if (val instanceof Integer) editor.putInt(key, (Integer) val);
                    else if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                    else if (val instanceof Double) editor.putFloat(key, ((Double) val).floatValue());
                    else if (val instanceof Float) editor.putFloat(key, (Float) val);
                    else if (val instanceof Long) editor.putLong(key, (Long) val);
                    else if (val instanceof String) editor.putString(key, (String) val);
                }
                editor.apply();
            }

            // Restore Device Spoof profiles
            if (root.has("device_spoof_prefs")) {
                JSONObject spoofObj = root.getJSONObject("device_spoof_prefs");
                SharedPreferences.Editor editor = app.getSharedPreferences("device_spoof_prefs", Context.MODE_PRIVATE).edit();
                for (java.util.Iterator<String> it = spoofObj.keys(); it.hasNext(); ) {
                    String key = it.next();
                    Object val = spoofObj.get(key);
                    if (val instanceof Integer) editor.putInt(key, (Integer) val);
                    else if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                    else if (val instanceof Float) editor.putFloat(key, (Float) val);
                    else if (val instanceof Long) editor.putLong(key, (Long) val);
                    else if (val instanceof String) editor.putString(key, (String) val);
                }
                editor.apply();
            }

            // Restore HUD Overlay Preferences
            if (root.has("gamebooster_hud_prefs")) {
                JSONObject hudObj = root.getJSONObject("gamebooster_hud_prefs");
                SharedPreferences.Editor editor = app.getSharedPreferences("gamebooster_hud_prefs", Context.MODE_PRIVATE).edit();
                for (java.util.Iterator<String> it = hudObj.keys(); it.hasNext(); ) {
                    String key = it.next();
                    Object val = hudObj.get(key);
                    if (val instanceof Integer) editor.putInt(key, (Integer) val);
                    else if (val instanceof Boolean) editor.putBoolean(key, (Boolean) val);
                    else if (val instanceof Float) editor.putFloat(key, (Float) val);
                    else if (val instanceof Long) editor.putLong(key, (Long) val);
                    else if (val instanceof String) editor.putString(key, (String) val);
                }
                editor.apply();
            }

            Log.i(TAG, "Successfully restored game profiles from .gbcfg payload");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to import profiles: " + t.getMessage(), t);
            return false;
        }
    }

    /**
     * Saves JSON export directly into a local file.
     */
    public static boolean exportToFile(Context context, File targetFile) {
        if (targetFile == null) return false;
        String json = exportProfilesToJson(context);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to write .gbcfg file: " + t.getMessage(), t);
            return false;
        }
    }

    /**
     * Reads a .gbcfg file and imports its configuration.
     */
    public static boolean importFromFile(Context context, File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) return false;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return importProfilesFromJson(context, sb.toString());
        } catch (Throwable t) {
            Log.e(TAG, "Failed to read .gbcfg file: " + t.getMessage(), t);
            return false;
        }
    }

    /**
     * Dispatches an Android Share Intent with the generated .gbcfg file.
     */
    public static void shareConfigFile(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        try {
            File exportDir = new File(activity.getCacheDir(), "exports");
            if (!exportDir.exists()) exportDir.mkdirs();
            File exportFile = new File(exportDir, "GameBooster_Profile_" + System.currentTimeMillis() + FILE_EXTENSION);
            if (exportToFile(activity, exportFile)) {
                Uri contentUri = FileProvider.getUriForFile(
                        activity,
                        activity.getPackageName() + ".fileprovider",
                        exportFile
                );
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/octet-stream");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Game Booster Presets (.gbcfg)");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is my Game Booster performance & 185Hz profile preset!");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activity.startActivity(Intent.createChooser(shareIntent, "Share Game Booster Config"));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to share config file: " + t.getMessage(), t);
        }
    }
}
