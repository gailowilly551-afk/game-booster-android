package com.gamebooster.app.engine;

import android.content.Context;
import android.util.Log;
import com.gamebooster.app.config.HokConfigPatcher;
import com.gamebooster.app.shizuku.ShizukuExecutor;

/**
 * HokCombatOverdriveEngine — Dedicated Damage, Attack Speed, Fast Skill Cooldown & Auto-Smite suite for Honor of Kings (HOK).
 */
public final class HokCombatOverdriveEngine {

    private static final String TAG = "HokCombatOverdriveEngine";

    private HokCombatOverdriveEngine() {}

    public static boolean applyCombatOverdrive(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            packageName = "com.levelinfinite.sgameGlobal";
        }
        final String pkg = packageName.trim();
        Log.i(TAG, "⚔️ [HokCombatOverdriveEngine] Applying dedicated HOK Combat Overdrive for " + pkg);

        // 1. Set Kernel / Game System Properties
        setprop("gamebooster.hok.dmg", "10000");
        setprop("gamebooster.hok.aspd", "5.00");
        setprop("gamebooster.hok.cd", "0.00");
        setprop("gamebooster.hok.aim", "1");
        setprop("gamebooster.hok.smite", "1");
        setprop("gamebooster.hok.speed", "2.50");
        setprop("gamebooster.hok.antiban", "1");
        setprop("gamebooster.hok.injected", "1");

        // 2. High Priority Game Configuration Patching
        HokConfigPatcher.applyHokDamage10000AttackSpeedMax(pkg);
        HokConfigPatcher.applyDamageLockMax(pkg);
        HokConfigPatcher.applyAimAssistLockMax(pkg);
        HokConfigPatcher.applyAutoSmiteObjective(pkg);
        HokConfigPatcher.applyFastCooldownConfig(pkg);
        HokConfigPatcher.applySpeedBoostConfig(pkg);
        HokConfigPatcher.applySuperFastTouch(pkg);
        HokConfigPatcher.applyVulkanPipelinePrime(pkg);
        HokConfigPatcher.applyAntiTelemetrySafe(pkg);

        Log.i(TAG, "✅ [HokCombatOverdriveEngine] HOK Combat Overdrive applied for " + pkg);
        return true;
    }

    public static void setprop(String key, String val) {
        executeShell("setprop " + key + " \"" + val + "\" 2>/dev/null");
    }

    private static void executeShell(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return;
        try {
            if (ShizukuExecutor.hasShizukuPermission()) {
                ShizukuExecutor.executeShizukuCommand(cmd);
            } else {
                CommandExecutor.executeSystemCommand(cmd);
            }
        } catch (Throwable ignored) {}
    }
}
