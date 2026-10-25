package com.gamebooster.app.config;

import android.util.Log;
import com.gamebooster.app.shizuku.ShizukuExecutor;
import java.io.File;

/**
 * CodmAceBypassPatcher — Dedicated safe patcher for Tencent Anti-Cheat Expert & CrashSight.
 * Operates on sandboxed preferences and configuration files without breaking network sockets
 * or triggering CODM resource auto-update loops.
 */
public final class CodmAceBypassPatcher {

    private static final String TAG = "CodmAceBypassPatcher";

    private CodmAceBypassPatcher() {}

    /**
     * Applies safe in-storage ACE and CrashSight suppression patches.
     * Guaranteed: Never applies chmod 000 to anogs/tprt sockets, never sets chattr +i
     * on session token XMLs, and repairs any existing permission locks so CODM opens smoothly.
     */
    public static void applyBypass(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;

        Log.i(TAG, "⚡ Applying Safe ACE & Telemetry suppression for " + packageName);

        try {
            // 1. First UNLOCK and HEAL any previous stale locks that broke network connection
            restoreNetworkAndUnlockDirectories(packageName);

            // 2. Safely neutralize CrashSight crash reporter logs without breaking sockets
            String[] crashSightDirs = {
                "/data/user/0/" + packageName + "/files/crashsight",
                "/data/data/" + packageName + "/files/crashsight",
                "/storage/emulated/0/Android/data/" + packageName + "/files/crashsight"
            };

            for (String dir : crashSightDirs) {
                // Keep directory accessible (775) but clear stored crash dumps and prevent log accumulation
                exec("rm -rf \"" + dir + "/*\" 2>/dev/null; "
                   + "mkdir -p \"" + dir + "\" 2>/dev/null; "
                   + "chmod 775 \"" + dir + "\" 2>/dev/null; "
                   + "touch \"" + dir + "/.nomedia\" 2>/dev/null;");
            }

            // 3. Ensure anogs and tprt network directories have full read/write permissions
            String[] networkDirs = {
                "/data/user/0/" + packageName + "/files/anogs",
                "/data/user/0/" + packageName + "/files/tprt",
                "/data/data/" + packageName + "/files/anogs",
                "/data/data/" + packageName + "/files/tprt",
                "/storage/emulated/0/Android/data/" + packageName + "/files/anogs",
                "/storage/emulated/0/Android/data/" + packageName + "/files/tprt"
            };

            for (String dir : networkDirs) {
                exec("mkdir -p \"" + dir + "\" 2>/dev/null; "
                   + "chattr -R -i \"" + dir + "\" 2>/dev/null; "
                   + "chmod 775 \"" + dir + "\" 2>/dev/null;");
            }

            Log.i(TAG, "✅ Safe ACE suppression completed (network intact) for " + packageName);
        } catch (Throwable t) {
            Log.w(TAG, "ACE bypass patch warning: " + t.getMessage());
        }
    }

    /**
     * Unlocks all directories and files, restores game UID ownership and allows CODM
     * to perform normal network handshakes and CDN asset verification.
     */
    public static void restoreNetworkAndUnlockDirectories(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        String cleanPkg = packageName.trim();

        String unlockCmd = "APP_UID=$(stat -c %u \"/data/user/0/" + cleanPkg + "\" 2>/dev/null || stat -c %u \"/data/data/" + cleanPkg + "\" 2>/dev/null || echo \"10656\"); "
                         + "for base in \"/data/user/0/" + cleanPkg + "\" \"/data/data/" + cleanPkg + "\" \"/storage/emulated/0/Android/data/" + cleanPkg + "\"; do "
                         + "  if [ -d \"$base\" ]; then "
                         + "    chattr -R -i \"$base/files\" \"$base/shared_prefs\" 2>/dev/null; "
                         + "    chmod -R 775 \"$base/files\" 2>/dev/null; "
                         + "    chmod -R 770 \"$base/shared_prefs\" 2>/dev/null; "
                         + "    chown -R \"$APP_UID:$APP_UID\" \"$base/files\" \"$base/shared_prefs\" 2>/dev/null; "
                         + "  fi; "
                         + "done; "
                         // Specifically ensure anogs and tprt socket directories are NOT 000
                         + "for d in \"/data/user/0/" + cleanPkg + "/files/anogs\" \"/data/data/" + cleanPkg + "/files/anogs\" "
                         + "          \"/data/user/0/" + cleanPkg + "/files/tprt\" \"/data/data/" + cleanPkg + "/files/tprt\"; do "
                         + "  mkdir -p \"$d\" 2>/dev/null; "
                         + "  chmod 775 \"$d\" 2>/dev/null; "
                         + "  chown \"$APP_UID:$APP_UID\" \"$d\" 2>/dev/null; "
                         + "done;";
        exec(unlockCmd);
    }

    private static void exec(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return;
        try {
            if (ShizukuExecutor.hasShizukuPermission()) {
                ShizukuExecutor.executeShizukuCommand(cmd);
            } else {
                com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd);
            }
        } catch (Throwable t) {
            Log.w(TAG, "exec failed: " + cmd + " → " + t.getMessage());
        }
    }
}
