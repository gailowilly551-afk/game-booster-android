package com.gamebooster.app.saf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import com.gamebooster.app.shizuku.ShizukuPermissionEnforcer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * SafStorageManager — Storage Access Framework (SAF) Direct Document Engine.
 *
 * Implements MT Manager / ZArchiver direct DocumentProvider access across Android 11, 12, 13, 14, 15, and 16:
 * - Grants permanent persistable URI access to /Android/data, /Android/data/<pkg>, or /Android/data/<pkg>/files.
 * - Accurately normalizes tree document roots and resolves relative subpaths to prevent folder loss or duplication.
 * - Overwrites existing files in-place using "wt" (write-truncate) to avoid "(1)" duplicate files.
 * - Detects MIME types properly so XML, JSON, and binaries are not altered with unwanted extensions by DocumentsUI.
 * - Supports Global Android/data access as well as per-game folder targets for all major game families.
 */
@android.annotation.SuppressLint("ObsoleteSdkInt")
public final class SafStorageManager {

    private static final String TAG = "SafStorageManager";
    private static final String PREF_NAME = "game_booster_saf_prefs";
    private static final String KEY_SAF_URI_PREFIX = "saf_tree_uri_";
    private static final String KEY_GLOBAL_DATA_URI = "saf_global_data_uri";
    private static final String KEY_GLOBAL_STORAGE_URI = "saf_global_storage_uri";

    /** Request code for legacy Activity Result SAF tree picker fallback. */
    public static final int REQUEST_CODE_SAF_TREE = 8842;

    public static final String PKG_MLBB = "com.mobile.legends";
    public static final String PKG_PUBGM = "com.tencent.ig";
    public static final String PKG_CODM = "com.activision.callofduty.shooter";
    public static final String PKG_FREEFIRE = "com.dts.freefireth";
    public static final String PKG_GENSHIN = "com.miHoYo.GenshinImpact";
    public static final String PKG_HOK = "com.levelinfinite.sgameGlobal";
    public static final String PKG_WILDRIFT = "com.riotgames.league.wildrift";
    public static final String PKG_ROBLOX = "com.roblox.client";
    public static final String PKG_FARLIGHT = "com.miraclegames.farlight84";
    public static final String PKG_BLOODSTRIKE = "com.netease.bloodstrike";
    public static final String PKG_STANDOFF2 = "com.axlebolt.standoff2";

    public static final String[] FAMILY_MLBB = {
        "com.mobile.legends", "com.mobilelegends.mi", "com.vng.mlbbvn",
        "com.mobile.legends.vng", "com.mobilelegends.hw", "com.mobilelegends.na",
        "com.mobile.legends.kr", "com.mobile.legends.jp", "com.mobile.legends.moonton"
    };

    public static final String[] FAMILY_PUBGM = {
        "com.tencent.ig", "com.pubg.imobile", "com.pubg.krmobile",
        "com.vng.pubgmobile", "com.tencent.iglite", "com.pubg.newstate",
        "com.krafton.bgmi", "com.tencent.tmgp.pubgm"
    };

    public static final String[] FAMILY_CODM = {
        "com.activision.callofduty.shooter", "com.garena.game.codm",
        "com.vng.codmvn", "com.tencent.tmgp.kr.codm", "com.tencent.tmgp.cod",
        "com.activision.callofduty.warzone"
    };

    public static final String[] FAMILY_FREEFIRE = {
        "com.dts.freefireth", "com.dts.freefiremax"
    };

    public static final String[] FAMILY_GENSHIN = {
        "com.miHoYo.GenshinImpact", "com.cognosphere.GenshinImpact",
        "com.HoYoverse.hkrpgoversea", "com.HoYoverse.nap", "com.miHoYo.bh3oversea",
        "com.kurogame.wutheringwaves.global"
    };

    public static final String[] FAMILY_HOK = {
        "com.levelinfinite.sgameGlobal", "com.levelinfinite.sgameGlobal.gpkg",
        "com.tencent.tmgp.sgame", "com.garena.game.kgtw", "com.garena.game.kgvn", "com.garena.game.kgid"
    };

    public static final String[] FAMILY_WILDRIFT = {
        "com.riotgames.league.wildrift", "com.riotgames.league.wildrifttw",
        "com.riotgames.league.wildriftvn", "com.riotgames.league.teamfighttactics"
    };

    public static final String[] FAMILY_ROBLOX = {
        "com.roblox.client"
    };

    public static final String[] FAMILY_FARLIGHT = {
        "com.miraclegames.farlight84"
    };

    public static final String[] FAMILY_BLOODSTRIKE = {
        "com.netease.bloodstrike"
    };

    public static final String[] FAMILY_STANDOFF2 = {
        "com.axlebolt.standoff2"
    };

    public static final class TargetAppInfo {
        public final String id;
        public final String displayName;
        public final String defaultPackage;
        public final String installedPackage;
        public final boolean isInstalled;
        public final boolean hasSaf;

        public TargetAppInfo(String id, String displayName, String defaultPackage, String installedPackage, boolean isInstalled, boolean hasSaf) {
            this.id = id;
            this.displayName = displayName;
            this.defaultPackage = defaultPackage;
            this.installedPackage = installedPackage;
            this.isInstalled = isInstalled;
            this.hasSaf = hasSaf;
        }

        public String getEffectivePackage() {
            return (installedPackage != null && !installedPackage.isEmpty()) ? installedPackage : defaultPackage;
        }
    }

    private SafStorageManager() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String[] getFamilyForPackage(String packageName) {
        if (packageName == null) return null;
        String lower = packageName.toLowerCase(Locale.US);

        for (String p : FAMILY_MLBB) if (p.equalsIgnoreCase(packageName)) return FAMILY_MLBB;
        for (String p : FAMILY_PUBGM) if (p.equalsIgnoreCase(packageName)) return FAMILY_PUBGM;
        for (String p : FAMILY_CODM) if (p.equalsIgnoreCase(packageName)) return FAMILY_CODM;
        for (String p : FAMILY_FREEFIRE) if (p.equalsIgnoreCase(packageName)) return FAMILY_FREEFIRE;
        for (String p : FAMILY_GENSHIN) if (p.equalsIgnoreCase(packageName)) return FAMILY_GENSHIN;
        for (String p : FAMILY_HOK) if (p.equalsIgnoreCase(packageName)) return FAMILY_HOK;
        for (String p : FAMILY_WILDRIFT) if (p.equalsIgnoreCase(packageName)) return FAMILY_WILDRIFT;
        for (String p : FAMILY_ROBLOX) if (p.equalsIgnoreCase(packageName)) return FAMILY_ROBLOX;
        for (String p : FAMILY_FARLIGHT) if (p.equalsIgnoreCase(packageName)) return FAMILY_FARLIGHT;
        for (String p : FAMILY_BLOODSTRIKE) if (p.equalsIgnoreCase(packageName)) return FAMILY_BLOODSTRIKE;
        for (String p : FAMILY_STANDOFF2) if (p.equalsIgnoreCase(packageName)) return FAMILY_STANDOFF2;

        if (lower.contains("mobile.legends") || lower.contains("mobilelegends")) return FAMILY_MLBB;
        if (lower.contains("pubg") || lower.contains("tencent.ig")) return FAMILY_PUBGM;
        if (lower.contains("callofduty") || lower.contains("codm") || lower.contains("warzone")) return FAMILY_CODM;
        if (lower.contains("freefire") || lower.contains("dts.freefire")) return FAMILY_FREEFIRE;
        if (lower.contains("genshin") || lower.contains("hoyoverse")) return FAMILY_GENSHIN;
        if (lower.contains("sgame") || lower.contains("arenaofvalor")) return FAMILY_HOK;
        if (lower.contains("wildrift")) return FAMILY_WILDRIFT;
        if (lower.contains("roblox")) return FAMILY_ROBLOX;
        if (lower.contains("farlight")) return FAMILY_FARLIGHT;
        if (lower.contains("bloodstrike")) return FAMILY_BLOODSTRIKE;
        if (lower.contains("standoff2")) return FAMILY_STANDOFF2;
        return null;
    }

    public static String findInstalledPackage(Context context, String[] family) {
        if (context == null || family == null) return null;
        android.content.pm.PackageManager pm = context.getPackageManager();
        for (String pkg : family) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }

    public static List<TargetAppInfo> getTargetApps(Context context) {
        List<TargetAppInfo> list = new ArrayList<>();
        if (context == null) return list;

        boolean hasGlobal = hasGlobalDataSaf(context);
        list.add(new TargetAppInfo("all", "🌍 All Games (Global Android/data)", "", "", hasGlobal, hasGlobal));

        String[][] allFamilies = {
            FAMILY_MLBB, FAMILY_PUBGM, FAMILY_CODM, FAMILY_FREEFIRE,
            FAMILY_HOK, FAMILY_GENSHIN, FAMILY_WILDRIFT, FAMILY_FARLIGHT,
            FAMILY_BLOODSTRIKE, FAMILY_ROBLOX, FAMILY_STANDOFF2
        };
        String[] defaultPkgs = {
            PKG_MLBB, PKG_PUBGM, PKG_CODM, PKG_FREEFIRE,
            PKG_HOK, PKG_GENSHIN, PKG_WILDRIFT, PKG_FARLIGHT,
            PKG_BLOODSTRIKE, PKG_ROBLOX, PKG_STANDOFF2
        };
        String[] ids = {
            "mlbb", "pubgm", "codm", "freefire",
            "hok", "genshin", "wildrift", "farlight",
            "bloodstrike", "roblox", "standoff2"
        };

        for (int i = 0; i < allFamilies.length; i++) {
            String[] family = allFamilies[i];
            String defaultPkg = defaultPkgs[i];
            String id = ids[i];
            String installed = findInstalledPackage(context, family);
            boolean isInstalled = installed != null;
            String effectivePkg = isInstalled ? installed : defaultPkg;
            boolean hasSaf = hasSafPermission(context, effectivePkg);

            if (i < 3 || isInstalled || hasSaf) {
                String name = getGameDisplayName(effectivePkg);
                list.add(new TargetAppInfo(id, name, defaultPkg, installed, isInstalled, hasSaf));
            }
        }

        return list;
    }

    public static String getGameDisplayName(String packageName) {
        if (packageName == null) return "Target Application";
        String[] family = getFamilyForPackage(packageName);
        if (family == FAMILY_MLBB) return "Mobile Legends (MLBB)";
        if (family == FAMILY_PUBGM) return "PUBG Mobile / BGMI";
        if (family == FAMILY_CODM) return "Call of Duty: Mobile (CODM)";
        if (family == FAMILY_FREEFIRE) return "Free Fire / MAX";
        if (family == FAMILY_GENSHIN) return "Genshin Impact";
        if (family == FAMILY_HOK) return "Honor of Kings / AOV";
        if (family == FAMILY_WILDRIFT) return "LoL: Wild Rift";
        if (family == FAMILY_ROBLOX) return "Roblox";
        if (family == FAMILY_FARLIGHT) return "Farlight 84";
        if (family == FAMILY_BLOODSTRIKE) return "Blood Strike";
        if (family == FAMILY_STANDOFF2) return "Standoff 2";
        if ("all".equalsIgnoreCase(packageName) || "global".equalsIgnoreCase(packageName)) {
            return "All Games (Global Android/data)";
        }
        return packageName;
    }

    public static String safeUriDecode(String s) {
        if (s == null) return "";
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Throwable t) {
            return s;
        }
    }

    public static String extractPackageFromTreeUri(Uri treeUri) {
        if (treeUri == null) return null;
        String raw = null;
        try {
            raw = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Throwable ignored) {}
        if (raw == null || raw.isEmpty()) {
            raw = treeUri.toString();
        }
        String decoded = safeUriDecode(raw);

        if (decoded.endsWith("Android/data") || decoded.endsWith("Android/data/") || decoded.equals("primary:Android/data")) {
            return "all";
        }

        for (String pkg : FAMILY_MLBB) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_PUBGM) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_CODM) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_FREEFIRE) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_GENSHIN) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_HOK) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_WILDRIFT) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_ROBLOX) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_FARLIGHT) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_BLOODSTRIKE) if (decoded.contains(pkg)) return pkg;
        for (String pkg : FAMILY_STANDOFF2) if (decoded.contains(pkg)) return pkg;

        int idx = decoded.indexOf("Android/data/");
        if (idx >= 0) {
            String remainder = decoded.substring(idx + "Android/data/".length());
            int slash = remainder.indexOf('/');
            String extracted = (slash >= 0) ? remainder.substring(0, slash) : remainder;
            extracted = extracted.trim();
            if (extracted.contains(".")) {
                return extracted;
            }
        }
        return null;
    }

    public static boolean hasGlobalDataSaf(Context context) {
        if (context == null) return false;
        Uri globalUri = getGlobalDataUri(context);
        if (globalUri == null) return false;
        try {
            List<UriPermission> persisted = context.getContentResolver().getPersistedUriPermissions();
            for (UriPermission p : persisted) {
                if (p.getUri().equals(globalUri) && p.isWritePermission() && p.isReadPermission()) {
                    return true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "hasGlobalDataSaf error checking permissions: " + t.getMessage());
        }
        return false;
    }

    public static Uri getGlobalDataUri(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = getPrefs(context);
        String uriStr = prefs.getString(KEY_GLOBAL_DATA_URI, null);
        if (uriStr != null) return Uri.parse(uriStr);
        String storageStr = prefs.getString(KEY_GLOBAL_STORAGE_URI, null);
        if (storageStr != null) return Uri.parse(storageStr);
        return null;
    }

    public static boolean hasSafPermission(Context context, String packageName) {
        if (context == null) return false;
        Uri uri = getSavedTreeUri(context, packageName);
        if (uri == null) return false;

        try {
            List<UriPermission> persisted = context.getContentResolver().getPersistedUriPermissions();
            for (UriPermission p : persisted) {
                if (p.getUri().equals(uri) && p.isWritePermission() && p.isReadPermission()) {
                    return true;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "hasSafPermission error checking permissions: " + t.getMessage());
        }
        return false;
    }

    public static Uri getSavedTreeUri(Context context, String packageName) {
        if (context == null) return null;
        SharedPreferences prefs = getPrefs(context);

        if (packageName != null && !packageName.isEmpty() && !"all".equalsIgnoreCase(packageName)) {
            String uriStr = prefs.getString(KEY_SAF_URI_PREFIX + packageName, null);
            if (uriStr != null) {
                return Uri.parse(uriStr);
            }
            String[] family = getFamilyForPackage(packageName);
            if (family != null) {
                for (String alias : family) {
                    String aliasUri = prefs.getString(KEY_SAF_URI_PREFIX + alias, null);
                    if (aliasUri != null) {
                        return Uri.parse(aliasUri);
                    }
                }
            }
        }

        String globalDataUri = prefs.getString(KEY_GLOBAL_DATA_URI, null);
        if (globalDataUri != null) {
            return Uri.parse(globalDataUri);
        }

        String globalStorageUri = prefs.getString(KEY_GLOBAL_STORAGE_URI, null);
        if (globalStorageUri != null) {
            return Uri.parse(globalStorageUri);
        }

        return null;
    }

    public static boolean saveAndPersistUri(Context context, String packageName, Uri treeUri) {
        return saveAndPersistUriSmart(context, packageName, treeUri) != null;
    }

    public static String saveAndPersistUriSmart(Context context, String expectedPackage, Uri treeUri) {
        if (context == null || treeUri == null) return null;
        try {
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);

            String detectedPkg = extractPackageFromTreeUri(treeUri);
            String targetPkg = (detectedPkg != null && !detectedPkg.isEmpty()) ? detectedPkg : expectedPackage;

            String docId = "";
            try {
                docId = DocumentsContract.getTreeDocumentId(treeUri);
            } catch (Throwable ignored) {}
            if (docId == null) docId = "";
            String decodedDocId = safeUriDecode(docId);

            boolean isGlobalData = "all".equalsIgnoreCase(targetPkg)
                    || decodedDocId.endsWith("Android/data")
                    || decodedDocId.endsWith("Android/data/")
                    || decodedDocId.equals("primary:Android/data");

            boolean isStorageRoot = decodedDocId.equals("primary:")
                    || decodedDocId.equals("primary")
                    || decodedDocId.isEmpty();

            SharedPreferences.Editor editor = getPrefs(context).edit();

            if (isGlobalData) {
                editor.putString(KEY_GLOBAL_DATA_URI, treeUri.toString());
                editor.apply();
                Log.i(TAG, "Successfully persisted GLOBAL Android/data SAF tree URI: " + treeUri);
                return "all";
            }

            if (isStorageRoot) {
                editor.putString(KEY_GLOBAL_STORAGE_URI, treeUri.toString());
                editor.putString(KEY_GLOBAL_DATA_URI, treeUri.toString());
                editor.apply();
                Log.i(TAG, "Successfully persisted STORAGE ROOT SAF tree URI: " + treeUri);
                return "all";
            }

            if (targetPkg == null || targetPkg.trim().isEmpty()) {
                Log.w(TAG, "Cannot determine target package for SAF tree: " + treeUri);
                return null;
            }

            editor.putString(KEY_SAF_URI_PREFIX + targetPkg, treeUri.toString());

            if (expectedPackage != null && !expectedPackage.isEmpty() && !expectedPackage.equals(targetPkg)) {
                editor.putString(KEY_SAF_URI_PREFIX + expectedPackage, treeUri.toString());
            }

            String[] family = getFamilyForPackage(targetPkg);
            if (family != null) {
                for (String alias : family) {
                    editor.putString(KEY_SAF_URI_PREFIX + alias, treeUri.toString());
                }
            }

            editor.apply();
            Log.i(TAG, "Successfully persisted SAF tree URI for " + targetPkg + ": " + treeUri);
            return targetPkg;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to persist SAF tree URI", t);
            return null;
        }
    }

    public static Intent createOpenDocumentTreeIntent(String packageName) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        String pkg = (packageName != null) ? packageName.trim() : "";
        if (!pkg.isEmpty() && !"all".equalsIgnoreCase(pkg)) {
            String docPath = "primary:Android/data/" + pkg;
            Uri initialUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docPath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
            }
        } else {
            String docPath = "primary:Android/data";
            Uri initialUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docPath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
            }
        }
        return intent;
    }

    public static Intent createOpenDocumentTreeIntent(Context context, String targetGameOrPkg) {
        String resolvedPkg = targetGameOrPkg;
        if (context != null && targetGameOrPkg != null) {
            if ("all".equalsIgnoreCase(targetGameOrPkg) || "global".equalsIgnoreCase(targetGameOrPkg)) {
                resolvedPkg = "all";
            } else if ("mlbb".equalsIgnoreCase(targetGameOrPkg)) {
                String found = findInstalledPackage(context, FAMILY_MLBB);
                resolvedPkg = (found != null) ? found : PKG_MLBB;
            } else if ("pubgm".equalsIgnoreCase(targetGameOrPkg) || "pubg".equalsIgnoreCase(targetGameOrPkg)) {
                String found = findInstalledPackage(context, FAMILY_PUBGM);
                resolvedPkg = (found != null) ? found : PKG_PUBGM;
            } else if ("codm".equalsIgnoreCase(targetGameOrPkg) || "cod".equalsIgnoreCase(targetGameOrPkg)) {
                String found = findInstalledPackage(context, FAMILY_CODM);
                resolvedPkg = (found != null) ? found : PKG_CODM;
            } else if ("freefire".equalsIgnoreCase(targetGameOrPkg) || "ff".equalsIgnoreCase(targetGameOrPkg)) {
                String found = findInstalledPackage(context, FAMILY_FREEFIRE);
                resolvedPkg = (found != null) ? found : PKG_FREEFIRE;
            } else {
                String[] family = getFamilyForPackage(targetGameOrPkg);
                if (family != null) {
                    String found = findInstalledPackage(context, family);
                    if (found != null) resolvedPkg = found;
                }
            }
        }
        return createOpenDocumentTreeIntent(resolvedPkg);
    }

    public static Intent createOpenDocumentTreeIntentForFiles(String packageName) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        String pkg = (packageName != null) ? packageName.trim() : "";
        if (!pkg.isEmpty() && !"all".equalsIgnoreCase(pkg)) {
            String docPath = "primary:Android/data/" + pkg + "/files";
            Uri initialUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docPath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
            }
        }
        return intent;
    }

    public static String normalizeDocumentIdToPath(String rawDocId) {
        if (rawDocId == null) return "";
        String decoded = safeUriDecode(rawDocId).trim();
        int colon = decoded.lastIndexOf(':');
        if (colon >= 0) {
            decoded = decoded.substring(colon + 1);
        }
        int treeIdx = decoded.indexOf("/tree/");
        if (treeIdx >= 0) {
            decoded = decoded.substring(treeIdx + "/tree/".length());
            int innerColon = decoded.lastIndexOf(':');
            if (innerColon >= 0) decoded = decoded.substring(innerColon + 1);
        }
        while (decoded.startsWith("/")) decoded = decoded.substring(1);
        while (decoded.endsWith("/")) decoded = decoded.substring(0, decoded.length() - 1);
        return decoded;
    }

    public static String normalizeToCanonicalStoragePath(String packageName, String path) {
        if (path == null) return "";
        String clean = path.trim().replace('\\', '/');
        while (clean.startsWith("/")) clean = clean.substring(1);

        if (clean.startsWith("storage/emulated/0/")) {
            clean = clean.substring("storage/emulated/0/".length());
        } else if (clean.startsWith("sdcard/")) {
            clean = clean.substring("sdcard/".length());
        }

        if (clean.startsWith("data/user/0/")) {
            clean = "Android/data/" + clean.substring("data/user/0/".length());
        } else if (clean.startsWith("data/data/")) {
            clean = "Android/data/" + clean.substring("data/data/".length());
        }

        String pkg = (packageName != null) ? packageName.trim() : "";

        if (clean.startsWith("Android/data/")) {
            String sub = clean.substring("Android/data/".length());
            if (!pkg.isEmpty() && !sub.startsWith(pkg + "/") && !sub.equals(pkg)) {
                int slash = sub.indexOf('/');
                String firstPart = slash >= 0 ? sub.substring(0, slash) : sub;
                if (!firstPart.contains(".")) {
                    clean = "Android/data/" + pkg + "/" + sub;
                }
            }
            return clean;
        }

        if (!pkg.isEmpty() && (clean.startsWith(pkg + "/") || clean.equals(pkg))) {
            return "Android/data/" + clean;
        }

        int firstSlash = clean.indexOf('/');
        String firstPart = firstSlash >= 0 ? clean.substring(0, firstSlash) : clean;
        if (firstPart.contains(".") && firstPart.length() >= 5) {
            return "Android/data/" + clean;
        }

        if (!pkg.isEmpty()) {
            return "Android/data/" + pkg + "/" + clean;
        }

        return "Android/data/" + clean;
    }

    public static String resolveRelativeNavPath(String treePath, String canonicalPath, String rootDocName, String packageName) {
        if (canonicalPath == null || canonicalPath.isEmpty()) return "";

        String tp = (treePath != null) ? treePath.trim() : "";
        while (tp.startsWith("/")) tp = tp.substring(1);
        while (tp.endsWith("/")) tp = tp.substring(0, tp.length() - 1);

        String cp = canonicalPath.trim();
        while (cp.startsWith("/")) cp = cp.substring(1);
        while (cp.endsWith("/")) cp = cp.substring(0, cp.length() - 1);

        if (!tp.isEmpty()) {
            if (cp.equalsIgnoreCase(tp)) {
                return "";
            }
            if (cp.toLowerCase(Locale.US).startsWith(tp.toLowerCase(Locale.US) + "/")) {
                String sub = cp.substring(tp.length() + 1);
                while (sub.startsWith("/")) sub = sub.substring(1);
                return sub;
            }
        } else {
            return cp;
        }

        String rootName = (rootDocName != null) ? rootDocName.trim() : "";
        String pkg = (packageName != null) ? packageName.trim() : "";

        if ("primary".equalsIgnoreCase(rootName) || rootName.isEmpty()) {
            return cp;
        }

        if ("files".equalsIgnoreCase(rootName)) {
            int filesIdx = cp.toLowerCase(Locale.US).indexOf("/files/");
            if (filesIdx >= 0) {
                return cp.substring(filesIdx + "/files/".length());
            }
            if (cp.toLowerCase(Locale.US).endsWith("/files")) {
                return "";
            }
        } else if (!pkg.isEmpty() && rootName.equalsIgnoreCase(pkg)) {
            int pkgIdx = cp.toLowerCase(Locale.US).indexOf("/" + pkg.toLowerCase(Locale.US) + "/");
            if (pkgIdx >= 0) {
                return cp.substring(pkgIdx + ("/" + pkg + "/").length());
            }
            if (cp.toLowerCase(Locale.US).endsWith("/" + pkg.toLowerCase(Locale.US))) {
                return "";
            }
        } else if ("data".equalsIgnoreCase(rootName)) {
            if (cp.toLowerCase(Locale.US).startsWith("android/data/")) {
                return cp.substring("android/data/".length());
            }
        }

        if (cp.toLowerCase(Locale.US).startsWith("android/data/")) {
            String afterData = cp.substring("android/data/".length());
            if (!pkg.isEmpty() && afterData.startsWith(pkg + "/")) {
                return afterData.substring(pkg.length() + 1);
            }
            return afterData;
        }

        return cp;
    }

    public static String getMimeTypeForName(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase(Locale.US);
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "text/xml";
        if (lower.endsWith(".ini") || lower.endsWith(".txt") || lower.endsWith(".cfg") || lower.endsWith(".conf")) return "text/plain";
        if (lower.endsWith(".unity3d") || lower.endsWith(".bytes") || lower.endsWith(".bin") || lower.endsWith(".dat") || lower.endsWith(".so")) return "application/octet-stream";
        return "application/octet-stream";
    }

    public static DocumentFile navigateToSubpath(Context context, String packageName, String relativePath, boolean createDirsIfMissing) {
        if (context == null) return null;
        Uri treeUri = getSavedTreeUri(context, packageName);
        if (treeUri == null) {
            Log.w(TAG, "SAF: No saved tree URI found for package: " + packageName);
            return null;
        }

        DocumentFile rootDoc = DocumentFile.fromTreeUri(context, treeUri);
        if (rootDoc == null || !rootDoc.canWrite()) {
            Log.w(TAG, "SAF: Tree rootDoc is not writable or null for " + packageName + " (uri: " + treeUri + ")");
            return null;
        }

        if (relativePath == null || relativePath.trim().isEmpty()) {
            return rootDoc;
        }

        String treeDocId = "";
        try {
            treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Throwable ignored) {}
        if (treeDocId == null || treeDocId.isEmpty()) {
            treeDocId = safeUriDecode(treeUri.toString());
        }

        String treePath = normalizeDocumentIdToPath(treeDocId);
        String canonicalPath = normalizeToCanonicalStoragePath(packageName, relativePath);
        String navPath = resolveRelativeNavPath(treePath, canonicalPath, rootDoc.getName(), packageName);

        if (navPath.isEmpty()) {
            return rootDoc;
        }

        String[] parts = navPath.split("/+");
        DocumentFile current = rootDoc;

        for (String part : parts) {
            String cleanPart = part.trim();
            if (cleanPart.isEmpty() || cleanPart.equals(".")) continue;

            DocumentFile next = current.findFile(cleanPart);
            if (next == null) {
                if (createDirsIfMissing) {
                    next = current.createDirectory(cleanPart);
                    if (next == null) {
                        Log.w(TAG, "SAF: Failed to create directory: " + cleanPart + " under " + current.getName());
                        return null;
                    }
                } else {
                    return null;
                }
            }
            current = next;
        }
        return current;
    }

    public static boolean writeFileBytes(Context context, String packageName, String relativeFilePath, byte[] data) {
        if (context == null || data == null || relativeFilePath == null) return false;
        try {
            int lastSlash = relativeFilePath.lastIndexOf('/');
            String dirPart = lastSlash >= 0 ? relativeFilePath.substring(0, lastSlash) : "";
            String fileName = lastSlash >= 0 ? relativeFilePath.substring(lastSlash + 1) : relativeFilePath;

            DocumentFile targetDir = navigateToSubpath(context, packageName, dirPart, true);
            if (targetDir == null || !targetDir.isDirectory()) {
                Log.w(TAG, "SAF: Target directory resolution failed for " + relativeFilePath);
                return false;
            }

            DocumentFile targetFile = targetDir.findFile(fileName);
            if (targetFile == null || !targetFile.exists()) {
                String mimeType = getMimeTypeForName(fileName);
                targetFile = targetDir.createFile(mimeType, fileName);
                if (targetFile == null) {
                    Log.e(TAG, "SAF: Could not create file " + fileName + " in " + targetDir.getName());
                    return false;
                }
            }

            try (OutputStream os = context.getContentResolver().openOutputStream(targetFile.getUri(), "wt")) {
                if (os == null) return false;
                os.write(data);
                os.flush();
            } catch (Throwable writeErr) {
                Log.w(TAG, "SAF: In-place write failed (" + writeErr.getMessage() + "), retrying with delete & recreate");
                try {
                    targetFile.delete();
                } catch (Throwable ignored) {}
                targetFile = targetDir.createFile(getMimeTypeForName(fileName), fileName);
                if (targetFile == null) return false;
                try (OutputStream os = context.getContentResolver().openOutputStream(targetFile.getUri(), "wt")) {
                    if (os == null) return false;
                    os.write(data);
                    os.flush();
                }
            }

            Log.i(TAG, "SAF: Successfully wrote " + data.length + " bytes to " + relativeFilePath);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "SAF: Exception writing to " + relativeFilePath, t);
            return false;
        }
    }

    public static boolean writeTextFile(Context context, String packageName, String relativeFilePath, String text) {
        if (text == null) text = "";
        return writeFileBytes(context, packageName, relativeFilePath, text.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] readFileBytes(Context context, String packageName, String relativeFilePath) {
        if (context == null || relativeFilePath == null) return new byte[0];
        try {
            DocumentFile targetFile = navigateToSubpath(context, packageName, relativeFilePath, false);
            if (targetFile == null || !targetFile.exists() || !targetFile.isFile()) {
                return new byte[0];
            }
            try (InputStream is = context.getContentResolver().openInputStream(targetFile.getUri())) {
                if (is == null) return new byte[0];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                return baos.toByteArray();
            }
        } catch (Throwable t) {
            Log.w(TAG, "SAF: Exception reading " + relativeFilePath + ": " + t.getMessage());
            return new byte[0];
        }
    }

    public static String readTextFile(Context context, String packageName, String relativeFilePath) {
        byte[] bytes = readFileBytes(context, packageName, relativeFilePath);
        return bytes.length > 0 ? new String(bytes, StandardCharsets.UTF_8) : "";
    }

    public static boolean fileExists(Context context, String packageName, String relativePath) {
        if (context == null || relativePath == null) return false;
        DocumentFile doc = navigateToSubpath(context, packageName, relativePath, false);
        return doc != null && doc.exists();
    }

    public static boolean deleteFile(Context context, String packageName, String relativeFilePath) {
        if (context == null || relativeFilePath == null) return false;
        try {
            DocumentFile file = navigateToSubpath(context, packageName, relativeFilePath, false);
            return file != null && file.exists() && file.delete();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean createDirectory(Context context, String packageName, String relativeDirPath) {
        if (context == null || relativeDirPath == null) return false;
        try {
            DocumentFile dir = navigateToSubpath(context, packageName, relativeDirPath, true);
            return dir != null && dir.isDirectory();
        } catch (Throwable t) {
            return false;
        }
    }

    public static List<String> listDirectory(Context context, String packageName, String relativeDirPath) {
        List<String> list = new ArrayList<>();
        if (context == null) return list;
        DocumentFile doc = navigateToSubpath(context, packageName, relativeDirPath, false);
        if (doc != null && doc.isDirectory()) {
            DocumentFile[] files = doc.listFiles();
            for (DocumentFile f : files) {
                if (f.getName() != null) {
                    list.add(f.getName());
                }
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ─── ⚡ ONE-TAP ALL FILES INTERNAL STORAGE ACCESS SUITE ───────────────────────
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Checks if full external/internal storage management permission (MANAGE_EXTERNAL_STORAGE)
     * or legacy read/write permissions are granted.
     */
    public static boolean isAllFilesAccessGranted(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int read = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE);
            int write = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Creates a 1-tap Intent that routes directly to the Game Booster "Allow access to manage all files" switch.
     */
    public static Intent createAllFilesAccessIntent(Context context) {
        if (context == null) return new Intent(Settings.ACTION_SETTINGS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            } catch (Throwable ignored) {
                Intent fallback = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return fallback;
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }
    }

    /**
     * Attempts to grant MANAGE_EXTERNAL_STORAGE and storage permissions instantly via Shizuku or Root.
     * Returns true if granted successfully.
     */
    public static boolean grantAllFilesPermissionWithShizuku(Context context) {
        if (context == null) return false;
        try {
            String pkg = context.getPackageName();
            String[] cmds = new String[] {
                "cmd appops set " + pkg + " MANAGE_EXTERNAL_STORAGE allow",
                "appops set " + pkg + " MANAGE_EXTERNAL_STORAGE allow",
                "pm grant " + pkg + " android.permission.MANAGE_EXTERNAL_STORAGE",
                "pm grant " + pkg + " android.permission.READ_EXTERNAL_STORAGE",
                "pm grant " + pkg + " android.permission.WRITE_EXTERNAL_STORAGE",
                "chmod 777 /storage/emulated/0/Android/data 2>/dev/null",
                "chmod 777 /storage/emulated/0/Android/obb 2>/dev/null"
            };
            for (String cmd : cmds) {
                try {
                    ShizukuExecutor.executeShizukuCommand(cmd);
                } catch (Throwable ignored) {}
            }
            // Trigger comprehensive permission enforcement
            ShizukuPermissionEnforcer.enforceAllPermissions(context, true);
            return isAllFilesAccessGranted(context);
        } catch (Throwable t) {
            Log.w(TAG, "grantAllFilesPermissionWithShizuku failed: " + t.getMessage());
            return false;
        }
    }

    /**
     * One-Tap Storage Access Orchestrator:
     * 1. Checks if already granted.
     * 2. Checks if Shizuku / Root is available -> auto-grants silently with zero taps.
     * 3. Otherwise, opens the exact system Settings page directly with 1 tap.
     */
    public static void grantAllStorageAccessOneTap(Activity activity, Runnable onGrantedCallback) {
        if (activity == null) return;
        Context ctx = activity.getApplicationContext();

        if (isAllFilesAccessGranted(ctx)) {
            Toast.makeText(activity, "✅ All Files Internal Storage Permission Already Granted!", Toast.LENGTH_SHORT).show();
            if (onGrantedCallback != null) onGrantedCallback.run();
            return;
        }

        // Try elevated instant grant first via Shizuku / Root
        AppExecutors.getInstance().executeCommand(() -> {
            boolean granted = grantAllFilesPermissionWithShizuku(ctx);
            AppExecutors.getInstance().postToMainThread(() -> {
                if (granted) {
                    Toast.makeText(activity, "⚡ Storage Access Granted Instantly via Elevated Service!", Toast.LENGTH_LONG).show();
                    if (onGrantedCallback != null) onGrantedCallback.run();
                } else {
                    // Launch 1-tap system toggle intent
                    try {
                        Intent intent = createAllFilesAccessIntent(ctx);
                        activity.startActivity(intent);
                        Toast.makeText(activity, "👉 Turn ON 'Allow access to manage all files' for Game Booster", Toast.LENGTH_LONG).show();
                    } catch (Throwable t) {
                        Toast.makeText(activity, "Error opening settings: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    /**
     * Creates an Open Document Tree intent specifically targeting Internal Storage Root.
     */
    public static Intent createOpenDocumentTreeIntentForStorageRoot() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        Uri initialUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }
        return intent;
    }
}
