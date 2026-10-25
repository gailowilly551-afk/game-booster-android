package com.gamebooster.app.mods;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * GameModProfile — per-game mod configuration model.
 * Holds all toggle states and multiplier values for one game.
 */
public class GameModProfile {

    public enum Game { MLBB, CODM, PUBGM }

    // ── MLBB fields ───────────────────────────────────────────────────────────
    public boolean mlbbDamageEnabled    = false;
    public float   mlbbDamageMult       = 3.0f;
    public boolean mlbbAttackSpeedEnabled = false;
    public float   mlbbAttackSpeedMult  = 2.0f;
    public boolean mlbbDefenseEnabled   = false;
    public float   mlbbDefenseMult      = 2.0f;
    public boolean mlbbNoCooldown       = false;
    public boolean mlbbCooldownReductionEnabled = false;
    public int     mlbbCooldownReductionPct     = 80; // 0% to 100%
    public boolean mlbbManaEnabled      = false;
    public boolean mlbbInfiniteMana     = true;
    public float   mlbbManaMult         = 5.0f;
    public boolean mlbbEnergyEnabled    = false;
    public boolean mlbbInfiniteEnergy   = true;
    public float   mlbbEnergyMult       = 5.0f;
    public boolean mlbbMapHack          = false;
    public boolean mlbbNoSpread         = false;
    public boolean mlbbDroneViewEnabled = true;
    public int     mlbbDroneTier        = 20;
    public boolean mlbbFpsUnlock        = false;
    public int     mlbbTargetFps        = 165;
    public boolean mlbbAntiBan         = true;

    // ── CODM fields ───────────────────────────────────────────────────────────
    public boolean codmAimbot          = false;
    public boolean codmAllScopeLock    = true;
    public boolean codmAutoHeadshot    = false;
    public boolean codmDamageEnabled   = false;
    public float   codmDamageMult      = 2.0f;
    public boolean codmSpeedEnabled    = false;
    public float   codmSpeedMult       = 1.5f;
    public boolean codmAttackEnabled   = false;
    public float   codmAttackMult      = 2.0f;
    public boolean codmDefenseEnabled  = false;
    public float   codmDefenseMult     = 0.8f;
    public boolean codmHealthEnabled   = false;
    public float   codmHealthMult      = 3.0f;
    public boolean codmCriticalEnabled = false;
    public float   codmCriticalMult    = 3.0f;
    public boolean codmNoRecoil        = false;
    public boolean codmNoSpread        = false;
    public boolean codmFpsUnlock       = false;
    public int     codmTargetFps       = 165;
    public boolean codmAntiBan         = true;

    // ── PUBGM fields ──────────────────────────────────────────────────────────
    public boolean pubgmAimbot                 = false;
    public boolean pubgmAllScopeLock           = true;
    public boolean pubgmNoRecoil               = false;
    public boolean pubgmNoSpread               = false;
    public boolean pubgmMagicBullet            = false;
    public boolean pubgmUltraDroneView         = false;
    public int     pubgmDroneFov               = 130;
    public boolean pubgmDamageLockMax          = false;
    public boolean pubgmFpsUnlock              = false;
    public int     pubgmTargetFps              = 185;
    public boolean pubgmAntiBan                = true;
    public boolean pubgmMidnightHuntersFix     = true;
    public boolean pubgmVehicleOverride        = true;
    public boolean pubgmS32RankedSweep         = true;
    public boolean pubgmCombatOverdrive        = false;
    public boolean pubgmSovereignOverdriveBypass = false;


    private static final String PREFS_NAME = "game_mod_profile";

    public static GameModProfile load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        GameModProfile p = new GameModProfile();

        p.mlbbDamageEnabled     = sp.getBoolean("mlbb_dmg_en", false);
        p.mlbbDamageMult        = sp.getFloat("mlbb_dmg_mult", 3.0f);
        p.mlbbAttackSpeedEnabled= sp.getBoolean("mlbb_aspd_en", false);
        p.mlbbAttackSpeedMult   = sp.getFloat("mlbb_aspd_mult", 2.0f);
        p.mlbbDefenseEnabled    = sp.getBoolean("mlbb_def_en", false);
        p.mlbbDefenseMult       = sp.getFloat("mlbb_def_mult", 2.0f);
        p.mlbbNoCooldown        = sp.getBoolean("mlbb_nocool", false);
        p.mlbbCooldownReductionEnabled = sp.getBoolean("mlbb_cdr_en", false);
        p.mlbbCooldownReductionPct     = sp.getInt("mlbb_cdr_pct", 80);
        p.mlbbManaEnabled       = sp.getBoolean("mlbb_mana_en", false);
        p.mlbbInfiniteMana      = sp.getBoolean("mlbb_inf_mana", true);
        p.mlbbManaMult          = sp.getFloat("mlbb_mana_mult", 5.0f);
        p.mlbbEnergyEnabled     = sp.getBoolean("mlbb_energy_en", false);
        p.mlbbInfiniteEnergy    = sp.getBoolean("mlbb_inf_energy", true);
        p.mlbbEnergyMult        = sp.getFloat("mlbb_energy_mult", 5.0f);
        p.mlbbMapHack           = sp.getBoolean("mlbb_map", false);
        p.mlbbNoSpread          = sp.getBoolean("mlbb_spread", false);
        p.mlbbDroneViewEnabled  = sp.getBoolean("mlbb_drone_en", true);
        p.mlbbDroneTier         = sp.getInt("mlbb_drone_tier", 20);

        // Cross-sync from mlbb_drone_prefs if present
        try {
            android.content.SharedPreferences dsp = ctx.getSharedPreferences("mlbb_drone_prefs", Context.MODE_PRIVATE);
            if (dsp != null && dsp.contains("drone_enabled")) {
                p.mlbbDroneViewEnabled = dsp.getBoolean("drone_enabled", p.mlbbDroneViewEnabled);
            }
            if (dsp != null && dsp.contains("drone_tier")) {
                p.mlbbDroneTier = dsp.getInt("drone_tier", p.mlbbDroneTier);
            }
        } catch (Throwable ignored) {}

        // Cross-sync from game_cheat_prefs (God Mode & Combat Overdrive Master Switch)
        try {
            android.content.SharedPreferences cp = ctx.getSharedPreferences("game_cheat_prefs", Context.MODE_PRIVATE);
            if (cp != null) {
                // MLBB God Mode sync
                boolean mlbbGodMode = cp.getBoolean("god_mode_com.mobile.legends", true);
                if (mlbbGodMode) {
                    p.mlbbDamageEnabled = true;
                    p.mlbbDamageMult = Math.max(p.mlbbDamageMult, 10000.0f);
                    p.mlbbAttackSpeedEnabled = true;
                    p.mlbbAttackSpeedMult = Math.max(p.mlbbAttackSpeedMult, 10.0f);
                    p.mlbbDefenseEnabled = true;
                    p.mlbbDefenseMult = Math.max(p.mlbbDefenseMult, 5.0f);
                    p.mlbbNoCooldown = true;
                    p.mlbbCooldownReductionEnabled = true;
                    p.mlbbCooldownReductionPct = 100;
                    p.mlbbManaEnabled = true;
                    p.mlbbInfiniteMana = true;
                    p.mlbbEnergyEnabled = true;
                    p.mlbbInfiniteEnergy = true;
                    p.mlbbMapHack = true;
                    p.mlbbNoSpread = true;
                    p.mlbbDroneViewEnabled = true;
                }

                // CODM God Mode sync
                boolean codmGodMode = cp.getBoolean("god_mode_com.garena.game.codm", true)
                                   || cp.getBoolean("god_mode_com.activision.callofduty.shooter", true);
                if (codmGodMode) {
                    p.codmDamageEnabled = true;
                    p.codmDamageMult = Math.max(p.codmDamageMult, 10000.0f);
                    p.codmSpeedEnabled = true;
                    p.codmSpeedMult = Math.max(p.codmSpeedMult, 2.5f);
                    p.codmAttackEnabled = true;
                    p.codmAttackMult = Math.max(p.codmAttackMult, 10.0f);
                    p.codmDefenseEnabled = true;
                    p.codmHealthEnabled = true;
                    p.codmCriticalEnabled = true;
                    p.codmCriticalMult = Math.max(p.codmCriticalMult, 10.0f);
                    p.codmNoRecoil = true;
                    p.codmNoSpread = true;
                    p.codmAimbot = true;
                    p.codmAllScopeLock = true;
                    p.codmAutoHeadshot = true;
                }

                // PUBGM God Mode sync
                boolean pubgmGodMode = cp.getBoolean("god_mode_com.tencent.ig", true)
                                    || cp.getBoolean("god_mode_com.pubg.imobile", true);
                if (pubgmGodMode) {
                    p.pubgmDamageLockMax = true;
                    p.pubgmNoRecoil = true;
                    p.pubgmNoSpread = true;
                    p.pubgmMagicBullet = true;
                    p.pubgmAimbot = true;
                    p.pubgmAllScopeLock = true;
                    p.pubgmUltraDroneView = true;
                    p.pubgmCombatOverdrive = true;
                }
            }
        } catch (Throwable ignored) {}

        p.mlbbFpsUnlock         = sp.getBoolean("mlbb_fps_en", false);
        p.mlbbTargetFps         = sp.getInt("mlbb_fps", 165);
        p.mlbbAntiBan           = sp.getBoolean("mlbb_antiban", true);

        p.codmAimbot            = sp.getBoolean("codm_aim", false);
        p.codmAllScopeLock      = sp.getBoolean("codm_allscope", true);
        p.codmAutoHeadshot      = sp.getBoolean("codm_headshot", false);
        p.codmDamageEnabled     = sp.getBoolean("codm_dmg_en", false);
        p.codmDamageMult        = sp.getFloat("codm_dmg_mult", 2.0f);
        p.codmSpeedEnabled      = sp.getBoolean("codm_speed_en", false);
        p.codmSpeedMult         = sp.getFloat("codm_speed_mult", 1.5f);
        p.codmAttackEnabled     = sp.getBoolean("codm_attack_en", false);
        p.codmAttackMult        = sp.getFloat("codm_attack_mult", 2.0f);
        p.codmDefenseEnabled    = sp.getBoolean("codm_def_en", false);
        p.codmDefenseMult       = sp.getFloat("codm_def_mult", 0.8f);
        p.codmHealthEnabled     = sp.getBoolean("codm_hp_en", false);
        p.codmHealthMult        = sp.getFloat("codm_hp_mult", 3.0f);
        p.codmCriticalEnabled   = sp.getBoolean("codm_crit_en", false);
        p.codmCriticalMult      = sp.getFloat("codm_crit_mult", 3.0f);
        p.codmNoRecoil          = sp.getBoolean("codm_recoil", false);
        p.codmNoSpread          = sp.getBoolean("codm_spread", false);
        p.codmFpsUnlock         = sp.getBoolean("codm_fps_en", false);
        p.codmTargetFps         = sp.getInt("codm_fps", 165);
        p.codmAntiBan           = sp.getBoolean("codm_antiban", true);

        // ── PUBGM preferences ──
        p.pubgmAimbot                 = sp.getBoolean("pubgm_aim", false);
        p.pubgmAllScopeLock           = sp.getBoolean("pubgm_allscope", true);
        p.pubgmNoRecoil               = sp.getBoolean("pubgm_recoil", false);
        p.pubgmNoSpread               = sp.getBoolean("pubgm_spread", false);
        p.pubgmMagicBullet            = sp.getBoolean("pubgm_magic_bullet", false);
        p.pubgmUltraDroneView         = sp.getBoolean("pubgm_drone_en", false);
        p.pubgmDroneFov               = sp.getInt("pubgm_drone_fov", 130);
        p.pubgmDamageLockMax          = sp.getBoolean("pubgm_dmg_lock", false);
        p.pubgmFpsUnlock              = sp.getBoolean("pubgm_fps_en", false);
        p.pubgmTargetFps              = sp.getInt("pubgm_fps", 185);
        p.pubgmAntiBan                = sp.getBoolean("pubgm_antiban", true);
        p.pubgmMidnightHuntersFix     = sp.getBoolean("pubgm_midnight_fix", true);
        p.pubgmVehicleOverride        = sp.getBoolean("pubgm_vehicle_override", true);
        p.pubgmS32RankedSweep         = sp.getBoolean("pubgm_s32_sweep", true);
        p.pubgmCombatOverdrive        = sp.getBoolean("pubgm_combat_overdrive", false);
        p.pubgmSovereignOverdriveBypass = sp.getBoolean("pubgm_sovereign_bypass", false);

        return p;
    }

    public void save(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("mlbb_dmg_en",       mlbbDamageEnabled)
            .putFloat("mlbb_dmg_mult",       mlbbDamageMult)
            .putBoolean("mlbb_aspd_en",      mlbbAttackSpeedEnabled)
            .putFloat("mlbb_aspd_mult",      mlbbAttackSpeedMult)
            .putBoolean("mlbb_def_en",       mlbbDefenseEnabled)
            .putFloat("mlbb_def_mult",       mlbbDefenseMult)
            .putBoolean("mlbb_nocool",       mlbbNoCooldown)
            .putBoolean("mlbb_cdr_en",       mlbbCooldownReductionEnabled)
            .putInt("mlbb_cdr_pct",          mlbbCooldownReductionPct)
            .putBoolean("mlbb_mana_en",      mlbbManaEnabled)
            .putBoolean("mlbb_inf_mana",     mlbbInfiniteMana)
            .putFloat("mlbb_mana_mult",      mlbbManaMult)
            .putBoolean("mlbb_energy_en",    mlbbEnergyEnabled)
            .putBoolean("mlbb_inf_energy",   mlbbInfiniteEnergy)
            .putFloat("mlbb_energy_mult",    mlbbEnergyMult)
            .putBoolean("mlbb_map",          mlbbMapHack)
            .putBoolean("mlbb_spread",       mlbbNoSpread)
            .putBoolean("mlbb_drone_en",     mlbbDroneViewEnabled)
            .putInt("mlbb_drone_tier",       mlbbDroneTier)
            .putBoolean("mlbb_fps_en",       mlbbFpsUnlock)
            .putInt("mlbb_fps",              mlbbTargetFps)
            .putBoolean("mlbb_antiban",      mlbbAntiBan)
            .apply();

        try {
            ctx.getSharedPreferences("mlbb_drone_prefs", Context.MODE_PRIVATE).edit()
                .putBoolean("drone_enabled", mlbbDroneViewEnabled)
                .putInt("drone_tier", mlbbDroneTier)
                .apply();
        } catch (Throwable ignored) {}

        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("codm_aim",          codmAimbot)
            .putBoolean("codm_allscope",     codmAllScopeLock)
            .putBoolean("codm_headshot",     codmAutoHeadshot)
            .putBoolean("codm_dmg_en",       codmDamageEnabled)
            .putFloat("codm_dmg_mult",       codmDamageMult)
            .putBoolean("codm_speed_en",     codmSpeedEnabled)
            .putFloat("codm_speed_mult",     codmSpeedMult)
            .putBoolean("codm_attack_en",    codmAttackEnabled)
            .putFloat("codm_attack_mult",    codmAttackMult)
            .putBoolean("codm_def_en",       codmDefenseEnabled)
            .putFloat("codm_def_mult",       codmDefenseMult)
            .putBoolean("codm_hp_en",        codmHealthEnabled)
            .putFloat("codm_hp_mult",        codmHealthMult)
            .putBoolean("codm_crit_en",      codmCriticalEnabled)
            .putFloat("codm_crit_mult",      codmCriticalMult)
            .putBoolean("codm_recoil",       codmNoRecoil)
            .putBoolean("codm_spread",       codmNoSpread)
            .putBoolean("codm_fps_en",       codmFpsUnlock)
            .putInt("codm_fps",              codmTargetFps)
            .putBoolean("codm_antiban",      codmAntiBan)
            .putBoolean("pubgm_aim",              pubgmAimbot)
            .putBoolean("pubgm_allscope",         pubgmAllScopeLock)
            .putBoolean("pubgm_recoil",           pubgmNoRecoil)
            .putBoolean("pubgm_spread",           pubgmNoSpread)
            .putBoolean("pubgm_magic_bullet",     pubgmMagicBullet)
            .putBoolean("pubgm_drone_en",         pubgmUltraDroneView)
            .putInt("pubgm_drone_fov",            pubgmDroneFov)
            .putBoolean("pubgm_dmg_lock",         pubgmDamageLockMax)
            .putBoolean("pubgm_fps_en",           pubgmFpsUnlock)
            .putInt("pubgm_fps",                  pubgmTargetFps)
            .putBoolean("pubgm_antiban",          pubgmAntiBan)
            .putBoolean("pubgm_midnight_fix",     pubgmMidnightHuntersFix)
            .putBoolean("pubgm_vehicle_override", pubgmVehicleOverride)
            .putBoolean("pubgm_s32_sweep",        pubgmS32RankedSweep)
            .putBoolean("pubgm_combat_overdrive", pubgmCombatOverdrive)
            .putBoolean("pubgm_sovereign_bypass", pubgmSovereignOverdriveBypass)
            .apply();
    }

}
