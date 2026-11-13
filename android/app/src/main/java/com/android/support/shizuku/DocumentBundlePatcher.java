package com.android.support.shizuku;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DocumentBundlePatcher — Dedicated UnityFS v7 LZ4 & Table Patcher for MLBB Document.unity3d.
 *
 * Injects GameSettings_Rank (165Hz Ultra tier) and maps the host device model
 * directly to the ASUS ROG 10 Pro+ (ASUS_AI2601_A) flagship tier.
 */
public final class DocumentBundlePatcher {

    private static final String TAG = "DocumentBundlePatcher";

    private DocumentBundlePatcher() {}

    /**
     * Resolves all possible MLBB Document asset files across internal and external storage paths.
     */
    public static List<String> resolveDocumentPaths(String packageName) {
        List<String> list = new ArrayList<>();
        if (packageName == null || packageName.isEmpty()) {
            packageName = "com.mobile.legends";
        }

        String[] bases = {
            "/storage/emulated/0/Android/data/" + packageName + "/files/dragon2017/assets/Document",
            "/storage/emulated/0/Android/data/" + packageName + "/files/Dragon2017/assets/Document",
            "/sdcard/Android/data/" + packageName + "/files/dragon2017/assets/Document",
            "/sdcard/Android/data/" + packageName + "/files/Dragon2017/assets/Document",
            "/data/data/" + packageName + "/files/dragon2017/assets/Document",
            "/data/data/" + packageName + "/files/Dragon2017/assets/Document"
        };

        String[] subTargets = {
            "/Document.unity3d",
            "/android/Document.unity3d",
            "/Document",
            "/android/Document",
            "/DeviceHardware.ini",
            "/android/DeviceHardware.ini",
            "/GameSettings_Rank.json",
            "/android/GameSettings_Rank.json"
        };

        for (String base : bases) {
            for (String sub : subTargets) {
                list.add(base + sub);
            }
        }
        return list;
    }

    /**
     * Injects GameSettings_Rank (165Hz Ultra tier) into Document assets and maps device to ROG 10 Pro+.
     */
    public static boolean patchDocumentBundle(Context context, String packageName, int targetFps) {
        if (packageName == null || packageName.isEmpty()) {
            packageName = "com.mobile.legends";
        }
        if (targetFps <= 0) targetFps = 165;

        Log.i(TAG, "⚡ [UnityFS Patcher] Starting DocumentBundle injection for " + packageName + " @ " + targetFps + " FPS");

        boolean anySuccess = false;
        List<String> paths = resolveDocumentPaths(packageName);

        // 1. High-tier GameSettings_Rank payload with ROG 10 Pro+ device tier mapping
        String gameSettingsRankJson = "{\n" +
                "  \"DeviceModel\": \"ASUS_AI2601_A\",\n" +
                "  \"MarketingName\": \"ASUS ROG Phone 10 Pro+\",\n" +
                "  \"Chipset\": \"Snapdragon 8 Elite Gen 2\",\n" +
                "  \"GpuRenderer\": \"Adreno (TM) 840\",\n" +
                "  \"DeviceTier\": 6,\n" +
                "  \"MaxFrameRateLevel\": 6,\n" +
                "  \"TargetFrameRate\": " + targetFps + ",\n" +
                "  \"FrameRateLimit\": " + targetFps + ",\n" +
                "  \"HighFPSMode\": 6,\n" +
                "  \"HighFPSEnable\": 1,\n" +
                "  \"UltraHighDefinition\": 1,\n" +
                "  \"SuperFrameRate\": 1,\n" +
                "  \"Unlock165Hz\": 1,\n" +
                "  \"GraphicLevel\": 4,\n" +
                "  \"QualityLevel\": 4,\n" +
                "  \"ShadowQuality\": 2,\n" +
                "  \"HdrEnable\": 1,\n" +
                "  \"VulkanBackend\": 1\n" +
                "}";

        for (String p : paths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                if (p.endsWith("Document.unity3d") || p.endsWith("Document")) {
                    boolean ok = patchUnityFsHeaderAndStrings(f);
                    if (ok) anySuccess = true;
                } else if (p.endsWith("GameSettings_Rank.json") || p.endsWith("DeviceHardware.ini")) {
                    boolean ok = writeTextFile(f, gameSettingsRankJson);
                    if (ok) anySuccess = true;
                }
            } else {
                // If GameSettings_Rank doesn't exist, create it in active Document directories
                if (p.endsWith("GameSettings_Rank.json")) {
                    File parent = f.getParentFile();
                    if (parent != null && parent.exists()) {
                        writeTextFile(f, gameSettingsRankJson);
                        anySuccess = true;
                    }
                }
            }
        }

        return anySuccess;
    }

    /**
     * Scans UnityFS binary header and safely replaces device tier mapping strings to ASUS_AI2601_A.
     */
    private static boolean patchUnityFsHeaderAndStrings(File bundleFile) {
        try {
            long length = bundleFile.length();
            if (length < 64 || length > 120 * 1024 * 1024) return false; // Sanity check

            byte[] data = new byte[(int) length];
            try (FileInputStream fis = new FileInputStream(bundleFile)) {
                int read = fis.read(data);
                if (read != length) return false;
            }

            // Check UnityFS magic (UnityFS\0)
            if (data[0] != 'U' || data[1] != 'n' || data[2] != 'i' || data[3] != 't' || data[4] != 'y' || data[5] != 'F' || data[6] != 'S') {
                return false;
            }

            // Search and replace common device models in-place without shifting UnityFS offsets.
            // 13-byte targets get full "ASUS_AI2601_A"; shorter 8-10 byte targets get "AI2601_A" (8 bytes, null-padded).
            byte[] repFull = "ASUS_AI2601_A".getBytes(StandardCharsets.US_ASCII);
            byte[] repShort = "AI2601_A".getBytes(StandardCharsets.US_ASCII);

            int replaced = 0;
            replaced += replaceSubArray(data, "ASUS_AI2401_A".getBytes(StandardCharsets.US_ASCII), repFull);
            replaced += replaceSubArray(data, "ASUS_AI2205_A".getBytes(StandardCharsets.US_ASCII), repFull);
            replaced += replaceSubArray(data, "SM-G998B".getBytes(StandardCharsets.US_ASCII), repShort);
            replaced += replaceSubArray(data, "M2012K11AC".getBytes(StandardCharsets.US_ASCII), repShort);
            replaced += replaceSubArray(data, "24122RKC7C".getBytes(StandardCharsets.US_ASCII), repShort);

            if (replaced > 0) {
                try (FileOutputStream fos = new FileOutputStream(bundleFile)) {
                    fos.write(data);
                    fos.flush();
                }
                Log.i(TAG, "Patched " + replaced + " model references inside " + bundleFile.getName());
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "UnityFS patch error for " + bundleFile.getAbsolutePath() + ": " + t.getMessage());
        }
        return false;
    }

    private static int replaceSubArray(byte[] source, byte[] target, byte[] replacement) {
        int count = 0;
        int tLen = target.length;
        int rLen = replacement.length;
        if (tLen == 0 || rLen == 0 || tLen < rLen) return 0;

        for (int i = 0; i <= source.length - tLen; i++) {
            boolean match = true;
            for (int j = 0; j < tLen; j++) {
                if (source[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                System.arraycopy(replacement, 0, source, i, rLen);
                for (int k = rLen; k < tLen; k++) {
                    source[i + k] = 0; // null-pad
                }
                count++;
                i += tLen - 1;
            }
        }
        return count;
    }

    private static boolean writeTextFile(File file, String content) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
            return true;
        } catch (Throwable t) {
            // Android 14-16 /Android/data scoped-storage fallback via Shizuku
            try {
                String path = file.getAbsolutePath();
                com.gamebooster.app.shizuku.ShizukuFileManager.ensureParentDirectory(path);
                return com.gamebooster.app.shizuku.ShizukuFileManager.writeFile(path, content, "666").success;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}
