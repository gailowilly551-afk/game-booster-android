package com.gamebooster.app.shizuku;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages the Shizuku 13.5 `rish` (Root/Shizuku Shell) binary and `rish_shizuku.dex`.
 * Provides standalone, elevated ADB/Root shell command execution across Android 13, 14, 15, and 16.
 */
@android.annotation.SuppressLint("ObsoleteSdkInt")
public class RishManager {

    private static final String TAG = "RishManager";
    private static final String DIR_NAME = "shizuku";
    private static final String RISH_BIN_NAME = "rish";
    private static final String RISH_DEX_NAME = "rish_shizuku.dex";
    public static final long EXPECTED_DEX_SIZE = 59672L;

    private static volatile boolean isInitialized = false;
    private static volatile boolean isRishBrokenOnThisDevice = false;
    private static File rishBinFile = null;
    private static File rishDexFile = null;

    public static boolean isRishAvailable() {
        if (isRishBrokenOnThisDevice) return false;
        return (isInitialized && rishBinFile != null && rishBinFile.exists() && rishDexFile != null && rishDexFile.exists() && rishDexFile.length() > 0) ||
                (rishBinFile != null && rishBinFile.exists() && rishDexFile != null && rishDexFile.exists() && rishDexFile.length() > 0);
    }

    /**
     * Tests if the rish shell channel is actively communicating with Shizuku daemon.
     */
    public static boolean testRishLiveness(Context context) {
        if (!isAvailable(context)) return false;
        try {
            String out = executeRishCommand(context, "echo ping");
            return out != null && "ping".equals(out.trim());
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Initializes and extracts official stable rish and rish_shizuku.dex to internal app storage.
     */
    @android.annotation.SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    public static synchronized boolean initialize(Context context) {
        if (context == null) return false;
        Context appContext = context.getApplicationContext();

        try {
            File shizukuDir = new File(appContext.getFilesDir(), DIR_NAME);
            if (!shizukuDir.exists()) {
                shizukuDir.mkdirs();
            }
            try {
                appContext.getFilesDir().setExecutable(true, false);
                appContext.getFilesDir().setReadable(true, false);
                shizukuDir.setExecutable(true, false);
                shizukuDir.setReadable(true, false);
            } catch (Throwable ignored) {}

            rishBinFile = new File(shizukuDir, RISH_BIN_NAME);
            rishDexFile = new File(shizukuDir, RISH_DEX_NAME);

            // Re-extract rish_shizuku.dex if missing, empty, or size mismatch from stable asset
            if (!rishDexFile.exists() || rishDexFile.length() != EXPECTED_DEX_SIZE) {
                if (rishDexFile.exists()) {
                    try {
                        rishDexFile.setWritable(true, false);
                        boolean deleted = rishDexFile.delete();
                        if (!deleted) {
                            Log.w(TAG, "Old rishDexFile delete failed, attempting direct overwrite");
                        }
                    } catch (Throwable ignored) {}
                }
                copyAssetToFile(appContext, DIR_NAME + "/" + RISH_DEX_NAME, rishDexFile);
            }

            // Android 14+ (API 34+) requires dex loaded by app_process to be strictly read-only and world-readable
            rishDexFile.setReadable(true, false);
            if (Build.VERSION.SDK_INT >= 34) {
                rishDexFile.setReadOnly();
            }

            // Generate/write custom rish script with target package pre-configured
            String rishScript = generateRishScript(appContext.getPackageName(), rishDexFile.getAbsolutePath());
            try (FileOutputStream fos = new FileOutputStream(rishBinFile)) {
                fos.write(rishScript.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }

            // Grant executable permissions
            rishBinFile.setExecutable(true, false);
            rishBinFile.setReadable(true, false);

            isInitialized = true;
            isRishBrokenOnThisDevice = false;
            Log.i(TAG, "Stable RishManager successfully initialized at: " + rishBinFile.getAbsolutePath() + " (dexSize=" + rishDexFile.length() + ")");
            return true;

        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize RishManager", t);
            return false;
        }
    }

    /**
     * Executes a command using Shizuku stable rish shell.
     */
    public static String executeRishCommand(Context context, String command) {
        if (command == null || command.trim().isEmpty()) {
            return "SUCCESS";
        }

        if (isRishBrokenOnThisDevice) {
            return "ERROR: rish binary not available";
        }

        if (!isInitialized || rishBinFile == null || !rishBinFile.exists() || rishDexFile == null || !rishDexFile.exists()) {
            initialize(context);
        }

        if (rishBinFile == null || !rishBinFile.exists()) {
            return "ERROR: rish binary not available";
        }

        Process process = null;
        BufferedReader stdoutReader = null;
        BufferedReader stderrReader = null;

        try {
            String pkgName = context != null ? context.getPackageName() : "com.gamebooster.app";

            ProcessBuilder pb = new ProcessBuilder("sh", rishBinFile.getAbsolutePath(), "-c", command);
            pb.environment().put("RISH_APPLICATION_ID", pkgName);
            pb.environment().put("PATH", "/system/bin:/system/xbin:/vendor/bin");
            pb.directory(rishBinFile.getParentFile());

            process = pb.start();

            stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder stdout = new StringBuilder();
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                stdout.append(line).append("\n");
            }

            stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder stderr = new StringBuilder();
            while ((line = stderrReader.readLine()) != null) {
                stderr.append(line).append("\n");
            }

            // Timeout 5000ms to allow complex shell pipelines under high CPU load
            boolean finished = process.waitFor(5000, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {}
                Log.w(TAG, "rish command timed out after 5000ms: " + command);
                return "ERROR: Command timed out after 5000ms";
            }

            int exitCode = process.exitValue();
            String stdoutStr = stdout.toString().trim();
            String stderrStr = stderr.toString().trim();

            // Detect fatal ART image incompatibility
            if (exitCode != 0 && (stderrStr.contains("boot.art") || stderrStr.contains("Image header invalid"))) {
                Log.w(TAG, "Fatal ART image incompatibility detected: " + stderrStr);
                isRishBrokenOnThisDevice = true;
                return "ERROR: rish not compatible with this device ART image";
            }

            if (exitCode == 0) {
                return stdoutStr.isEmpty() ? "SUCCESS" : stdoutStr;
            } else {
                if (!stderrStr.isEmpty()) {
                    return "ERROR: " + stderrStr;
                }
                return stdoutStr.isEmpty() ? "ERROR (exit " + exitCode + ")" : stdoutStr;
            }

        } catch (Throwable t) {
            Log.w(TAG, "executeRishCommand failed: " + t.getMessage());
            return "ERROR: " + t.getMessage();
        } finally {
            try {
                if (stdoutReader != null) stdoutReader.close();
                if (stderrReader != null) stderrReader.close();
                if (process != null) process.destroy();
            } catch (Throwable ignored) {}
        }
    }

    public static boolean isAvailable(Context context) {
        if (isRishBrokenOnThisDevice) return false;
        if (!isInitialized || rishBinFile == null || !rishBinFile.exists() || rishDexFile == null || !rishDexFile.exists()) {
            initialize(context);
        }
        return rishBinFile != null && rishBinFile.exists() && rishDexFile != null && rishDexFile.exists() && rishDexFile.length() > 0;
    }

    private static String generateRishScript(String pkgName, String dexPath) {
        return "#!/system/bin/sh\n" +
                "DEX=\"" + dexPath + "\"\n" +
                "if [ ! -f \"$DEX\" ]; then\n" +
                "  echo \"Cannot find $DEX\"\n" +
                "  exit 1\n" +
                "fi\n" +
                "if [ $(getprop ro.build.version.sdk 2>/dev/null || echo 0) -ge 34 ]; then\n" +
                "  if [ -w \"$DEX\" ]; then\n" +
                "    chmod 400 \"$DEX\" 2>/dev/null\n" +
                "  fi\n" +
                "fi\n" +
                "export RISH_APPLICATION_ID=\"" + pkgName + "\"\n" +
                "exec /system/bin/app_process -Djava.class.path=\"$DEX\" /system/bin --nice-name=rish rikka.shizuku.shell.ShizukuShellLoader \"$@\"\n";
    }

    private static void copyAssetToFile(Context context, String assetPath, File destFile) throws Exception {
        try (InputStream in = context.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }
}
