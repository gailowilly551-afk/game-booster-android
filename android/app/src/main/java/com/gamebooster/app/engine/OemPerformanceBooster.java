package com.gamebooster.app.engine;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * OemPerformanceBooster
 * Dynamic OEM-aware optimization engine for Android 12 to 16.
 * Disables vendor throttling daemons, enables game-mode boosts,
 * unlocks maximum display refresh rate, and injects manufacturer-specific
 * performance parameters across Transsion (Infinix/Tecno), Xiaomi/Redmi,
 * Vivo/iQOO, Samsung, Huawei/Honor, and Oppo/Realme/OnePlus.
 */
public final class OemPerformanceBooster {

    private static final String TAG = "OemPerformanceBooster";

    public enum OemType {
        TRANSSION,  // Infinix, Tecno, Itel (HiOS / XOS)
        XIAOMI,     // Xiaomi, Redmi, POCO (HyperOS / MIUI)
        VIVO,       // Vivo, iQOO (OriginOS / FuntouchOS)
        SAMSUNG,    // Samsung (One UI)
        HUAWEI,     // Huawei, Honor (EMUI / MagicOS)
        OPPO,       // Oppo, Realme, OnePlus (ColorOS / OxygenOS / Realme UI)
        GENERIC
    }

    private OemPerformanceBooster() {}

    /**
     * Identify host device OEM based on Build properties.
     */
    public static OemType detectHostOem() {
        String mfg = (Build.MANUFACTURER != null ? Build.MANUFACTURER : "").toLowerCase();
        String brand = (Build.BRAND != null ? Build.BRAND : "").toLowerCase();

        if (mfg.contains("infinix") || mfg.contains("tecno") || mfg.contains("transsion") || mfg.contains("itel") ||
            brand.contains("infinix") || brand.contains("tecno") || brand.contains("itel")) {
            return OemType.TRANSSION;
        }
        if (mfg.contains("xiaomi") || mfg.contains("redmi") || brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")) {
            return OemType.XIAOMI;
        }
        if (mfg.contains("vivo") || mfg.contains("iqoo") || brand.contains("vivo") || brand.contains("iqoo")) {
            return OemType.VIVO;
        }
        if (mfg.contains("samsung") || brand.contains("samsung")) {
            return OemType.SAMSUNG;
        }
        if (mfg.contains("huawei") || mfg.contains("honor") || brand.contains("huawei") || brand.contains("honor")) {
            return OemType.HUAWEI;
        }
        if (mfg.contains("oppo") || mfg.contains("realme") || mfg.contains("oneplus") ||
            brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus")) {
            return OemType.OPPO;
        }
        return OemType.GENERIC;
    }

    /**
     * Apply OEM-specific performance tweaks and vendor game-mode configurations.
     */
    public static void applyOemOptimizations(Context context, String packageName, int targetFps) {
        OemType oem = detectHostOem();
        Log.i(TAG, "Applying OEM performance boosters for: " + oem.name() + " (Game: " + packageName + ", Target: " + targetFps + " FPS)");

        List<String> cmds = new ArrayList<>();

        // Universal Android 12 - 16 SurfaceFlinger / Refresh Rate unlocks
        cmds.add("settings put system peak_refresh_rate " + targetFps + ".0");
        cmds.add("settings put system min_refresh_rate " + targetFps + ".0");
        cmds.add("settings put secure user_refresh_rate " + targetFps);
        cmds.add("setprop debug.sf.showfps 0");
        cmds.add("setprop debug.hwui.fps_divisor 1");

        switch (oem) {
            case TRANSSION: // Tecno / Infinix / HiOS / XOS
                cmds.add("setprop persist.sys.tran.gamemode 1");
                cmds.add("setprop debug.transsion.highfps 1");
                cmds.add("setprop tran_game_mode 1");
                cmds.add("settings put system tran_game_mode 1");
                cmds.add("settings put global transsion_game_space 1");
                cmds.add("settings put system intelligent_refresh_rate 0");
                break;

            case XIAOMI: // HyperOS / MIUI
                cmds.add("settings put secure miui_power_mode 3");
                cmds.add("setprop debug.miui.fps.switch 1");
                cmds.add("setprop persist.vendor.power.dfps.level " + targetFps);
                cmds.add("settings put system power_mode extreme");
                cmds.add("cmd appops set com.xiaomi.joyose RUN_IN_BACKGROUND ignore 2>/dev/null || true");
                break;

            case VIVO: // OriginOS / FuntouchOS
                cmds.add("settings put secure vivo_game_mode 1");
                cmds.add("settings put system vivo_game_engine 1");
                cmds.add("setprop persist.vivo.game.boost 1");
                cmds.add("setprop debug.vivo.high_refresh_rate 1");
                break;

            case SAMSUNG: // One UI
                cmds.add("settings put secure game_home_enable 1");
                cmds.add("settings put system refresh_rate_mode 2");
                cmds.add("setprop debug.samsung.game_fps " + targetFps);
                cmds.add("cmd appops set com.samsung.android.game.gos RUN_IN_BACKGROUND ignore 2>/dev/null || true");
                break;

            case HUAWEI: // EMUI / MagicOS
                cmds.add("settings put secure game_mode 1");
                cmds.add("settings put system hw_gamemode_status 1");
                cmds.add("setprop persist.sys.huawei.game 1");
                break;

            case OPPO: // ColorOS / OxygenOS / Realme UI
                cmds.add("settings put secure game_space_support 1");
                cmds.add("settings put system oppo_game_mode 1");
                cmds.add("setprop persist.sys.oppo.gamemode 1");
                cmds.add("setprop persist.sys.oplus.highfps 1");
                break;

            case GENERIC:
            default:
                cmds.add("settings put global game_driver_all_apps 1");
                break;
        }

        new Thread(() -> {
            try {
                PrivilegeBridgeEngine.executePrivilegedBatch(cmds);
                Log.d(TAG, "OEM Optimizations batch finished");
            } catch (Throwable t) {
                Log.w(TAG, "Failed executing OEM batch: " + t.getMessage());
            }
        }).start();
    }
}
