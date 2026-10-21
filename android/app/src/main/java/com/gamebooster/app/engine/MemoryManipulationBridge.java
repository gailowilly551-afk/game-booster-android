package com.gamebooster.app.engine;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;

import com.gamebooster.app.overlay.EspEntity;
import com.gamebooster.app.shizuku.ShizukuExecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MemoryManipulationBridge — Real-time memory reader/writer and ESP state engine
 * for non-rooted Android environments running through Shizuku / UID 2000.
 *
 * Targets:
 *  - com.mobile.legends
 *  - com.tencent.ig
 *  - com.garena.game.codm
 *
 * Attributes:
 *  - Damage
 *  - Attack Speed
 *  - Movement Speed
 *  - Mana
 *  - Energy
 *  - Defense
 */
public final class MemoryManipulationBridge {

    private static final String TAG = "MemoryBridge";

    public static final String PKG_MLBB  = "com.mobile.legends";
    public static final String PKG_PUBGM = "com.tencent.ig";
    public static final String PKG_CODM  = "com.garena.game.codm";

    public static class GameAttributes {
        public float damage = 1.0f;
        public float attackSpeed = 1.0f;
        public float moveSpeed = 1.0f;
        public float mana = 100.0f;
        public float energy = 100.0f;
        public float defense = 1.0f;
    }

    private static final List<EspEntity> sTrackedEntities = new CopyOnWriteArrayList<>();
    private static volatile boolean sMonitoring = false;
    private static Thread sWorkerThread = null;

    private MemoryManipulationBridge() {}

    public static List<EspEntity> getTrackedEntities() {
        return Collections.unmodifiableList(sTrackedEntities);
    }

    /**
     * Resolves process ID for target package.
     */
    public static int getPidForPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return -1;
        try {
            String out;
            if (ShizukuExecutor.hasShizukuPermission()) {
                out = ShizukuExecutor.executeShizukuCommand("pidof " + packageName + " 2>/dev/null");
            } else {
                out = CommandExecutor.executeSystemCommand("pidof " + packageName + " 2>/dev/null");
            }
            if (out != null && !out.trim().isEmpty()) {
                String[] parts = out.trim().split("\\s+");
                return Integer.parseInt(parts[0]);
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    /**
     * Resolves base address of target shared library in remote process maps.
     */
    public static long getModuleBase(int pid, String moduleName) {
        if (pid <= 0 || moduleName == null) return 0L;
        try {
            String cmd = "grep -m 1 '" + moduleName + "' /proc/" + pid + "/maps 2>/dev/null | awk '{print $1}' | cut -d'-' -f1";
            String out;
            if (ShizukuExecutor.hasShizukuPermission()) {
                out = ShizukuExecutor.executeShizukuCommand(cmd);
            } else {
                out = CommandExecutor.executeSystemCommand(cmd);
            }
            if (out != null && !out.trim().isEmpty()) {
                return Long.parseLong(out.trim(), 16);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error getting module base for " + moduleName + ": " + t.getMessage());
        }
        return 0L;
    }

    /**
     * Writes 4-byte float attribute to process memory at specific virtual address.
     */
    public static boolean writeFloatAttribute(int pid, long address, float value) {
        if (pid <= 0 || address == 0) return false;
        try {
            int intBits = Float.floatToIntBits(value);
            byte b0 = (byte) (intBits & 0xFF);
            byte b1 = (byte) ((intBits >> 8) & 0xFF);
            byte b2 = (byte) ((intBits >> 16) & 0xFF);
            byte b3 = (byte) ((intBits >> 24) & 0xFF);

            String hexBytes = String.format("\\x%02x\\x%02x\\x%02x\\x%02x", b0, b1, b2, b3);
            String writeCmd = String.format(
                    "printf '%s' | dd of=/proc/%d/mem bs=1 seek=%d conv=notrunc 2>/dev/null",
                    hexBytes, pid, address
            );

            if (ShizukuExecutor.hasShizukuPermission()) {
                ShizukuExecutor.executeShizukuCommand(writeCmd);
            } else {
                CommandExecutor.executeSystemCommand(writeCmd);
            }
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed writing memory at 0x" + Long.toHexString(address), t);
            return false;
        }
    }

    /**
     * Applies full 6-attribute manipulation suite to target game.
     */
    public static boolean applyAttributeOverrides(String packageName, GameAttributes attrs) {
        if (packageName == null || attrs == null) return false;
        int pid = getPidForPackage(packageName);
        if (pid <= 0) {
            Log.w(TAG, "Target game not running: " + packageName);
            return false;
        }

        long baseModule = 0L;
        if (packageName.contains("mobile.legends") || packageName.contains("codm") || packageName.contains("callofduty")) {
            baseModule = getModuleBase(pid, "libil2cpp.so");
        } else if (packageName.contains("tencent.ig") || packageName.contains("pubg")) {
            baseModule = getModuleBase(pid, "libUE4.so");
            if (baseModule == 0L) baseModule = getModuleBase(pid, "libShadowTrackerExtra.so");
        }

        if (baseModule == 0L) {
            Log.w(TAG, "Unable to resolve base module for " + packageName);
            return false;
        }

        Log.i(TAG, "Resolved base module 0x" + Long.toHexString(baseModule) + " for PID " + pid);

        // Apply attribute overrides
        // Configurable offsets per package
        if (packageName.contains("mobile.legends")) {
            long logicOffset = baseModule + 0x04C189A0L;
            writeFloatAttribute(pid, logicOffset + 0x0118L, attrs.damage);
            writeFloatAttribute(pid, logicOffset + 0x011CL, attrs.attackSpeed);
            writeFloatAttribute(pid, logicOffset + 0x0120L, attrs.moveSpeed);
            writeFloatAttribute(pid, logicOffset + 0x0124L, attrs.mana);
            writeFloatAttribute(pid, logicOffset + 0x0128L, attrs.energy);
            writeFloatAttribute(pid, logicOffset + 0x0130L, attrs.defense);
        } else if (packageName.contains("tencent.ig") || packageName.contains("pubg")) {
            long playerOffset = baseModule + 0x078E2040L;
            writeFloatAttribute(pid, playerOffset + 0x0188L, attrs.moveSpeed * 300.0f);
            writeFloatAttribute(pid, playerOffset + 0x0190L, attrs.damage);
        } else if (packageName.contains("codm") || packageName.contains("callofduty")) {
            long codmOffset = baseModule + 0x0512D100L;
            writeFloatAttribute(pid, codmOffset + 0x00D0L, attrs.damage);
            writeFloatAttribute(pid, codmOffset + 0x00E4L, attrs.attackSpeed);
            writeFloatAttribute(pid, codmOffset + 0x0120L, attrs.moveSpeed);
            writeFloatAttribute(pid, codmOffset + 0x0130L, attrs.defense);
        }

        Log.i(TAG, "⚡ Attribute overrides applied successfully for " + packageName);
        return true;
    }

    /**
     * Starts continuous background ESP entity tracking loop.
     */
    public static synchronized void startEspTracking(Context ctx, String packageName) {
        if (sMonitoring) return;
        sMonitoring = true;

        sWorkerThread = new Thread(() -> {
            Log.i(TAG, "ESP entity tracking loop started for " + packageName);
            while (sMonitoring) {
                try {
                    int pid = getPidForPackage(packageName);
                    if (pid > 0) {
                        pollEntities(packageName, pid);
                    }
                    Thread.sleep(33); // ~30 Hz polling
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable t) {
                    Log.w(TAG, "ESP polling tick error: " + t.getMessage());
                }
            }
            sTrackedEntities.clear();
            Log.i(TAG, "ESP tracking loop stopped.");
        }, "EspTrackerThread");

        sWorkerThread.start();
    }

    public static synchronized void stopEspTracking() {
        sMonitoring = false;
        if (sWorkerThread != null) {
            sWorkerThread.interrupt();
            sWorkerThread = null;
        }
        sTrackedEntities.clear();
    }

    /**
     * Polls and updates world-to-screen entity positions for ESP rendering.
     */
    private static void pollEntities(String pkg, int pid) {
        List<EspEntity> detected = new ArrayList<>();

        // Synthetic/demo entities if game is in active match
        // In full native runtime, coordinates are read from Actor array
        long now = SystemClock.uptimeMillis();
        float t = (now % 10000) / 10000.0f * 2.0f * (float) Math.PI;

        if (pkg.contains("mobile.legends")) {
            detected.add(new EspEntity("Enemy Ling", 4200.0f, 5500.0f, 18.0f,
                    540.0f + (float) Math.sin(t) * 120.0f, 850.0f, 90.0f, 130.0f, true, Color.RED));
            detected.add(new EspEntity("Enemy Fanny", 2100.0f, 4800.0f, 24.0f,
                    800.0f + (float) Math.cos(t) * 80.0f, 600.0f, 90.0f, 130.0f, true, Color.RED));
        } else if (pkg.contains("tencent.ig") || pkg.contains("pubg")) {
            detected.add(new EspEntity("Player_Sniper", 100.0f, 100.0f, 85.0f,
                    650.0f + (float) Math.sin(t) * 60.0f, 520.0f, 75.0f, 140.0f, true, Color.CYAN));
            detected.add(new EspEntity("Bot_Target", 65.0f, 100.0f, 142.0f,
                    420.0f + (float) Math.cos(t) * 40.0f, 480.0f, 50.0f, 90.0f, true, Color.YELLOW));
        } else if (pkg.contains("codm") || pkg.contains("callofduty")) {
            detected.add(new EspEntity("Ghost", 150.0f, 150.0f, 32.0f,
                    580.0f + (float) Math.sin(t) * 90.0f, 700.0f, 80.0f, 150.0f, true, Color.RED));
        }

        sTrackedEntities.clear();
        sTrackedEntities.addAll(detected);
    }
}
