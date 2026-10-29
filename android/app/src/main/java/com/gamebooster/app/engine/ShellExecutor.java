package com.gamebooster.app.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ShellExecutor {

    public static class CommandResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public static boolean isAndroidEnvironment() {
        String vendor = System.getProperty("java.vendor", "");
        String vmName = System.getProperty("java.vm.name", "");
        return vendor.contains("The Android Project")
                || vmName.equalsIgnoreCase("Dalvik")
                || vmName.equalsIgnoreCase("ART");
    }

    private static final String[] SU_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/data/local/tmp/su",
            "/apex/com.android.runtime/bin/su",
            "/system/bin/.ext/.su"
    };

    private static Boolean sRootAvailable = null;
    private static final Object ROOT_CHECK_LOCK = new Object();

    public static boolean isRootSuAvailable() {
        if (!isAndroidEnvironment()) {
            return false;
        }
        synchronized (ROOT_CHECK_LOCK) {
            if (sRootAvailable != null) {
                return sRootAvailable;
            }
            // 1. Check known binary paths (instant filesystem check)
            boolean suFileExists = false;
            for (String path : SU_PATHS) {
                File f = new File(path);
                if (f.exists()) {
                    suFileExists = true;
                    break;
                }
            }
            if (!suFileExists) {
                sRootAvailable = false;
                return false;
            }

            // If called on main UI thread, NEVER run blocking process execution (su/sh) — return fast false
            // and trigger deep check asynchronously in background
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                com.gamebooster.app.core.AppExecutors.getInstance().executeCommand(ShellExecutor::checkRootDeep);
                return false;
            }

            return checkRootDeep();
        }
    }

    private static boolean checkRootDeep() {
        synchronized (ROOT_CHECK_LOCK) {
            if (sRootAvailable != null) {
                return sRootAvailable;
            }
            // Verify actual root grant ('uid=0') with fast 1-second timeout.
            // Never assume root is granted just because '/system/bin/su' or 'which su' exists on disk!
            try {
                CommandResult res = executeInternal("su", "id", 1);
                if (res.isSuccess() && res.stdout != null && res.stdout.contains("uid=0")) {
                    sRootAvailable = true;
                    return true;
                }
            } catch (Throwable ignored) {}

            sRootAvailable = false;
            return false;
        }
    }

    public static void invalidateRootCache() {
        synchronized (ROOT_CHECK_LOCK) {
            sRootAvailable = null;
        }
    }

    public static boolean isPrivilegedAvailable() {
        return PrivilegeBridgeEngine.isPrivilegedActive();
    }

    public static CommandResult executeSuCommand(String command) {
        if (!isAndroidEnvironment()) {
            return new CommandResult(0, "", "");
        }
        if (isRootSuAvailable()) {
            return executeInternal("su", command);
        }
        // Fall back to Shizuku Virtual Root privileged execution
        if (PrivilegeBridgeEngine.isShizukuVirtualRootReady()) {
            String out = PrivilegeBridgeEngine.executePrivileged(command);
            if (out != null && !out.startsWith("ERROR:")) {
                return new CommandResult(0, out.equals("SUCCESS") ? "" : out, "");
            } else {
                return new CommandResult(1, "", out != null ? out : "Virtual root execution failed");
            }
        }
        return executeInternal("su", command);
    }

    public static CommandResult executeCommand(String command, boolean preferRoot) {
        if (!isAndroidEnvironment()) {
            return new CommandResult(0, "", "");
        }
        if (preferRoot && isRootSuAvailable()) {
            CommandResult suRes = executeSuCommand(command);
            if (suRes.isSuccess()) {
                return suRes;
            }
        }
        return executeCommand(command);
    }

    public static CommandResult executeCommand(String command) {
        // In desktop unit test environments, do not spawn host shell processes for Android commands
        if (!isAndroidEnvironment()) {
            return new CommandResult(0, "", "");
        }
        return executeInternal("sh", command);
    }

    private static CommandResult executeInternal(String shellBinary, String command) {
        return executeInternal(shellBinary, command, 3);
    }

    private static CommandResult executeInternal(String shellBinary, String command, int timeoutSeconds) {
        Process process = null;
        java.io.InputStream in = null;

        try {
            ProcessBuilder pb = new ProcessBuilder(shellBinary, "-c", command);
            pb.redirectErrorStream(true);
            process = pb.start();

            // Close stdin immediately so su/sh never blocks waiting for input
            try {
                process.getOutputStream().close();
            } catch (Exception ignored) {}

            in = process.getInputStream();
            StringBuilder output = new StringBuilder(512);
            byte[] buf = new byte[4096];
            long deadlineMs = System.currentTimeMillis() + Math.max(1, timeoutSeconds) * 1000L;

            while (true) {
                while (in.available() > 0) {
                    int n = in.read(buf, 0, Math.min(buf.length, in.available()));
                    if (n <= 0) break;
                    if (output.length() < 65536) {
                        output.append(new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
                if (process.waitFor(15, TimeUnit.MILLISECONDS)) {
                    // Drain any remaining bytes that arrived right as process exited
                    while (in.available() > 0) {
                        int n = in.read(buf, 0, Math.min(buf.length, in.available()));
                        if (n <= 0) break;
                        if (output.length() < 65536) {
                            output.append(new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8));
                        }
                    }
                    break;
                }
                if (System.currentTimeMillis() >= deadlineMs) {
                    process.destroyForcibly();
                    if ("su".equals(shellBinary)) {
                        sRootAvailable = false;
                    }
                    return new CommandResult(-1, output.toString().trim(), "Command timed out after " + timeoutSeconds + "s");
                }
            }

            int exitCode = process.exitValue();
            return new CommandResult(exitCode, output.toString().trim(), "");

        } catch (Exception e) {
            return new CommandResult(-1, "", e.getMessage() != null ? e.getMessage() : "Execution exception");
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {}
            try {
                if (process != null) process.destroyForcibly();
            } catch (Exception ignored) {}
        }
    }
}
