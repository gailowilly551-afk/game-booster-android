package com.gamebooster.app.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;

import com.gamebooster.app.R;
import com.gamebooster.app.booster.TouchLatencyChannel;
import com.gamebooster.app.config.CodmAceBypassPatcher;
import com.gamebooster.app.config.CodmConfigPatcher;
import com.gamebooster.app.config.CommonConfigTuningInjector;
import com.gamebooster.app.config.FreeFireConfigPatcher;
import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.GameProfilePreferences;
import com.gamebooster.app.config.MlbbConfigPatcher;
import com.gamebooster.app.config.MlbbDroneViewPatcher;
import com.gamebooster.app.config.MlbbHeroScriptDispatcher;
import com.gamebooster.app.config.MlbbHeroScriptRegistry;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.config.PubgConfigPatcher;
import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.core.NativeKernelBridge;
import com.gamebooster.app.device.DevicePerformanceCapabilities;
import com.gamebooster.app.engine.CodmCombatOverdriveEngine;
import com.gamebooster.app.engine.FpsForceUnlockEngine;
import com.gamebooster.app.engine.MlbbCombatOverdriveEngine;
import com.gamebooster.app.engine.MlbbFpsUnlockEngine;
import com.gamebooster.app.engine.PubgmCombatOverdriveEngine;
import com.gamebooster.app.engine.PubgmFpsUnlockEngine;
import com.gamebooster.app.gamemanager.GameManagerLauncher;
import com.gamebooster.app.games.GameAppInfo;
import com.gamebooster.app.games.GameFamily;
import com.gamebooster.app.spoofer.DeviceSpooferEngine;

/**
 * PreLaunchGameDialog — Dedicated Per-Game Matrix and Overdrive Pre-Launch Dialog.
 *
 * Provides dedicated, fully functional auto-inject suites tailored to each game family:
 *  - MLBB: 10k God Mode, Drone View (1.5x-5x), Hero Script Dispatcher, Fast Retribution.
 *  - PUBGM/BGMI: Zero Recoil & Gun Sway, iPad 120° Panoramic Ultra View, Magic Bullet & Hit-Reg.
 *  - CODM/Warzone: Zero Recoil & Anti-Flinch, Fast ADS & Quick Swap, ACE Anti-Cheat Cloak.
 *  - Free Fire: Auto Drag Headshot & Zero Recoil, Instant 360° Gloo Wall & Rapid Loot.
 *  - Universal / Heavy Games: ROG 9 Pro / iPad Pro Model Spoof, Kernel Prime Core Task Pinning (Cgroup).
 */
public final class PreLaunchGameDialog {

    private static final String PREFS_NAME = "game_pre_launch_matrix";
    private static Dialog activeDialog;

    private PreLaunchGameDialog() {}

    public static void show(Context context, GameAppInfo game) {
        show(context, game, null);
    }

    public static void show(Context context, GameAppInfo game, Runnable onDismissListener) {
        if (context == null || game == null) return;
        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof android.content.ContextWrapper) {
            Context base = context;
            while (base instanceof android.content.ContextWrapper) {
                if (base instanceof Activity) {
                    activity = (Activity) base;
                    break;
                }
                base = ((android.content.ContextWrapper) base).getBaseContext();
            }
        }
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            GameManagerLauncher.launchGame(context, game);
            return;
        }

        dismissCurrent();
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(
                R.layout.dialog_pre_launch_game, (ViewGroup) null, false);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setDimAmount(0.70f);
        }

        final String packageName = game.getPackageName();
        final String gameLabel = game.getLabel() == null ? packageName : game.getLabel();
        final GameFamily family = GameFamily.fromPackage(packageName);
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final DevicePerformanceCapabilities capabilities = DevicePerformanceCapabilities.detect(context);

        // Header Views
        ImageView ivIcon = view.findViewById(R.id.iv_pre_launch_game_icon);
        TextView tvTitle = view.findViewById(R.id.tv_pre_launch_game_title);
        TextView tvPkg = view.findViewById(R.id.tv_pre_launch_game_pkg);
        TextView tvEngine = view.findViewById(R.id.tv_pre_launch_game_engine);
        TextView tvTopBadge = view.findViewById(R.id.tv_pre_launch_top_badge);
        TextView tvEngineBadge = view.findViewById(R.id.tv_pre_launch_engine_badge);
        TextView tvDisplay = view.findViewById(R.id.tv_pre_launch_display_hz);
        TextView tvAutoFeatures = view.findViewById(R.id.tv_pre_launch_auto_features);

        if (game.getIcon() != null) ivIcon.setImageDrawable(game.getIcon());
        tvTitle.setText(gameLabel);
        tvPkg.setText(packageName);

        // Display Modes & Refresh Rate setup
        RadioButton rate185 = view.findViewById(R.id.rb_pre_fps_185);
        RadioButton rate165 = view.findViewById(R.id.rb_pre_fps_165);
        RadioButton rate144 = view.findViewById(R.id.rb_pre_fps_144);
        RadioButton rate120 = view.findViewById(R.id.rb_pre_fps_120);
        configureRateButton(rate185, 185, capabilities);
        configureRateButton(rate165, 165, capabilities);
        configureRateButton(rate144, 144, capabilities);
        configureRateButton(rate120, 120, capabilities);
        showSystemDefaultWhenNeeded(rate185, rate165, rate144, rate120, capabilities);

        int savedRate = GameProfilePreferences.getTargetHz(context, packageName);
        int selectedRate = (savedRate > 0) ? savedRate : 185;
        checkRateButton(selectedRate, rate185, 185);
        checkRateButton(selectedRate, rate165, 165);
        checkRateButton(selectedRate, rate144, 144);
        checkRateButton(selectedRate, rate120, 120);

        if (!rate185.isChecked() && !rate165.isChecked() && !rate144.isChecked() && !rate120.isChecked()) {
            if (rate185.getVisibility() == View.VISIBLE) rate185.setChecked(true);
            else if (rate165.getVisibility() == View.VISIBLE) rate165.setChecked(true);
            else if (rate144.getVisibility() == View.VISIBLE) rate144.setChecked(true);
            else if (rate120.getVisibility() == View.VISIBLE) rate120.setChecked(true);
        }

        tvDisplay.setText(describeDisplay(capabilities));

        // Dedicated Matrix Sections
        View secMlbb = view.findViewById(R.id.layout_section_mlbb);
        View secPubgm = view.findViewById(R.id.layout_section_pubgm);
        View secCodm = view.findViewById(R.id.layout_section_codm);
        View secFf = view.findViewById(R.id.layout_section_freefire);
        View secUniv = view.findViewById(R.id.layout_section_universal);

        // Hide all first
        if (secMlbb != null) secMlbb.setVisibility(View.GONE);
        if (secPubgm != null) secPubgm.setVisibility(View.GONE);
        if (secCodm != null) secCodm.setVisibility(View.GONE);
        if (secFf != null) secFf.setVisibility(View.GONE);
        if (secUniv != null) secUniv.setVisibility(View.GONE);

        // MLBB Controls
        final SwitchCompat swMlbbGod = view.findViewById(R.id.switch_mlbb_god_mode);
        final SwitchCompat swMlbbDrone = view.findViewById(R.id.switch_mlbb_drone_view);
        final View layoutMlbbDroneTier = view.findViewById(R.id.layout_mlbb_drone_tier);
        final RadioGroup rgMlbbDroneTier = view.findViewById(R.id.rg_mlbb_drone_tier);
        final RadioButton rbMlbb15 = view.findViewById(R.id.rb_drone_1_5x);
        final RadioButton rbMlbb20 = view.findViewById(R.id.rb_drone_2x);
        final RadioButton rbMlbb30 = view.findViewById(R.id.rb_drone_3x);
        final RadioButton rbMlbb40 = view.findViewById(R.id.rb_drone_4x);
        final RadioButton rbMlbb50 = view.findViewById(R.id.rb_drone_5x);
        final Spinner spMlbbHero = view.findViewById(R.id.spinner_mlbb_hero_script);
        final SwitchCompat swMlbbRetri = view.findViewById(R.id.switch_mlbb_fast_retri);

        // PUBGM Controls
        final SwitchCompat swPubgmRecoil = view.findViewById(R.id.switch_pubgm_zero_recoil);
        final SwitchCompat swPubgmIpad = view.findViewById(R.id.switch_pubgm_ipad_view);
        final SwitchCompat swPubgmBullet = view.findViewById(R.id.switch_pubgm_magic_bullet);

        // CODM Controls
        final SwitchCompat swCodmRecoil = view.findViewById(R.id.switch_codm_zero_recoil);
        final SwitchCompat swCodmAds = view.findViewById(R.id.switch_codm_fast_ads);
        final SwitchCompat swCodmAce = view.findViewById(R.id.switch_codm_ace_cloak);

        // Free Fire Controls
        final SwitchCompat swFfHeadshot = view.findViewById(R.id.switch_ff_headshot_drag);
        final SwitchCompat swFfGloo = view.findViewById(R.id.switch_ff_fast_gloo);

        // Universal Controls
        final SwitchCompat swUnivSpoof = view.findViewById(R.id.switch_univ_hardware_spoof);
        final SwitchCompat swUnivCores = view.findViewById(R.id.switch_univ_prime_cores);

        // Activate and configure dedicated section according to GameFamily
        switch (family) {
            case MLBB:
                if (secMlbb != null) secMlbb.setVisibility(View.VISIBLE);
                tvTopBadge.setText("👑 MLBB SOVEREIGN MATRIX");
                tvEngineBadge.setText("[UNITY 3D IL2CPP BATTLE]");
                tvEngine.setText("Mobile Legends • Season 42 New Map Overdrive Engine");
                tvAutoFeatures.setText("• 10,000 DMG / 0s CD / 10x Attack Speed God Mode\n"
                        + "• 100% Drone View Elevation with New Map Radar Sync\n"
                        + "• Meta Hero Script Dispatcher & Fast Retribution Smite");

                if (swMlbbGod != null) {
                    swMlbbGod.setChecked(prefs.getBoolean("mlbb_god_mode_" + packageName, true));
                }
                if (swMlbbDrone != null) {
                    boolean droneOn = prefs.getBoolean("mlbb_drone_view_" + packageName, true);
                    swMlbbDrone.setChecked(droneOn);
                    if (layoutMlbbDroneTier != null) {
                        layoutMlbbDroneTier.setVisibility(droneOn ? View.VISIBLE : View.GONE);
                    }
                    swMlbbDrone.setOnCheckedChangeListener((btn, checked) -> {
                        if (layoutMlbbDroneTier != null) {
                            layoutMlbbDroneTier.setVisibility(checked ? View.VISIBLE : View.GONE);
                        }
                    });
                }
                int savedTier = prefs.getInt("mlbb_drone_tier_" + packageName, MlbbDroneViewPatcher.TIER_3X);
                if (savedTier == MlbbDroneViewPatcher.TIER_1_5X && rbMlbb15 != null) rbMlbb15.setChecked(true);
                else if (savedTier == MlbbDroneViewPatcher.TIER_2X && rbMlbb20 != null) rbMlbb20.setChecked(true);
                else if (savedTier == MlbbDroneViewPatcher.TIER_3X && rbMlbb30 != null) rbMlbb30.setChecked(true);
                else if (savedTier == MlbbDroneViewPatcher.TIER_4X && rbMlbb40 != null) rbMlbb40.setChecked(true);
                else if (savedTier == MlbbDroneViewPatcher.TIER_5X && rbMlbb50 != null) rbMlbb50.setChecked(true);

                if (spMlbbHero != null) {
                    MlbbHeroScriptRegistry.HeroEntry[] metaHeroes = MlbbHeroScriptRegistry.getMetaHeroes();
                    List<String> heroLabels = new ArrayList<>();
                    heroLabels.add("⚡ All Meta Heroes (Universal 10k Dmg & 0s CD)");
                    for (MlbbHeroScriptRegistry.HeroEntry h : metaHeroes) {
                        if (h != null) {
                            heroLabels.add(h.name + " (" + h.role + " • 10k Dmg / 0s CD / 2x Spd)");
                        }
                    }
                    ArrayAdapter<String> heroAdapter = new ArrayAdapter<>(context,
                            android.R.layout.simple_spinner_item, heroLabels);
                    heroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spMlbbHero.setAdapter(heroAdapter);
                    int savedHero = prefs.getInt("mlbb_hero_idx_" + packageName, 0);
                    if (savedHero >= 0 && savedHero < heroLabels.size()) {
                        spMlbbHero.setSelection(savedHero);
                    }
                }
                if (swMlbbRetri != null) {
                    swMlbbRetri.setChecked(prefs.getBoolean("mlbb_fast_retri_" + packageName, true));
                }
                break;

            case PUBGM:
                if (secPubgm != null) secPubgm.setVisibility(View.VISIBLE);
                tvTopBadge.setText("🎯 PUBGM OVERDRIVE MATRIX");
                tvEngineBadge.setText("[UNREAL ENGINE 4 ACTIVE.SAV]");
                tvEngine.setText("PUBG Mobile / BGMI • GVAS Combat & Graphics Engine");
                tvAutoFeatures.setText("• Zero Recoil & Gun Sway Eliminator\n"
                        + "• iPad 120° Panoramic Ultra Wide FOV View\n"
                        + "• Magic Bullet & 35% Hitbox Expansion Buffer");

                if (swPubgmRecoil != null) {
                    swPubgmRecoil.setChecked(prefs.getBoolean("pubgm_zero_recoil_" + packageName, true));
                }
                if (swPubgmIpad != null) {
                    swPubgmIpad.setChecked(prefs.getBoolean("pubgm_ipad_view_" + packageName, true));
                }
                if (swPubgmBullet != null) {
                    swPubgmBullet.setChecked(prefs.getBoolean("pubgm_magic_bullet_" + packageName, true));
                }
                break;

            case CODM:
                if (secCodm != null) secCodm.setVisibility(View.VISIBLE);
                tvTopBadge.setText("⚔️ CODM TACTICAL MATRIX");
                tvEngineBadge.setText("[UNITY 3D ACE PROTECTED]");
                tvEngine.setText("Call of Duty Mobile • Tactical Overdrive & ACE Cloak");
                tvAutoFeatures.setText("• Zero Recoil & Flinch Immunity Overdrive\n"
                        + "• Turbo Fast ADS & Instant Weapon Swap\n"
                        + "• ACE Anti-Cheat Cloak & Memory Telemetry Nullifier");

                if (swCodmRecoil != null) {
                    swCodmRecoil.setChecked(prefs.getBoolean("codm_zero_recoil_" + packageName, true));
                }
                if (swCodmAds != null) {
                    swCodmAds.setChecked(prefs.getBoolean("codm_fast_ads_" + packageName, true));
                }
                if (swCodmAce != null) {
                    swCodmAce.setChecked(prefs.getBoolean("codm_ace_cloak_" + packageName, true));
                }
                break;

            case FREE_FIRE:
                if (secFf != null) secFf.setVisibility(View.VISIBLE);
                tvTopBadge.setText("🔥 FREE FIRE COMBAT MATRIX");
                tvEngineBadge.setText("[UNITY 3D IL2CPP BATTLE]");
                tvEngine.setText("Free Fire / MAX • Headshot Macro & Gloo Wall Engine");
                tvAutoFeatures.setText("• Auto Drag Headshot & Reticle Head-Lock\n"
                        + "• Instant 360° Gloo Wall Deployment & Fast Loot\n"
                        + "• Extreme Touch Response & Zero Recoil Spread");

                if (swFfHeadshot != null) {
                    swFfHeadshot.setChecked(prefs.getBoolean("ff_headshot_drag_" + packageName, true));
                }
                if (swFfGloo != null) {
                    swFfGloo.setChecked(prefs.getBoolean("ff_fast_gloo_" + packageName, true));
                }
                break;

            case OTHER:
            default:
                if (secUniv != null) secUniv.setVisibility(View.VISIBLE);
                tvTopBadge.setText("🚀 EXTREME ENGINE MATRIX");
                tvEngineBadge.setText("[UNIVERSAL HARDWARE OVERDRIVE]");
                tvEngine.setText("Heavy Game Engine • Kernel Task Pinning & Hardware Spoof");
                tvAutoFeatures.setText("• ROG 9 Pro / iPad Pro Model Spoofing for Max Graphics\n"
                        + "• Kernel Prime Core CPU Isolation (Cgroup) & LMK Immunity\n"
                        + "• 1000Hz Ultra Touch Latency Response Overclock");

                if (swUnivSpoof != null) {
                    swUnivSpoof.setChecked(prefs.getBoolean("univ_spoof_" + packageName, true));
                }
                if (swUnivCores != null) {
                    swUnivCores.setChecked(prefs.getBoolean("univ_cores_" + packageName, true));
                }
                break;
        }

        // Utility Buttons
        Button btnCfgStudio = view.findViewById(R.id.btn_pre_launch_cfg_studio);
        if (btnCfgStudio != null) {
            btnCfgStudio.setOnClickListener(v -> {
                dismissCurrent();
                GameCfgDialog.show(context, game, (pkg, targetFps, count) -> {
                    Toast.makeText(context, "🎛️ Applied " + count + " configs to " + gameLabel, Toast.LENGTH_SHORT).show();
                });
            });
        }

        Button btnAppInfo = view.findViewById(R.id.btn_pre_launch_app_info);
        if (btnAppInfo != null) {
            btnAppInfo.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Throwable t) {
                    Toast.makeText(context, "Could not open App Info: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        Button btnRemove = view.findViewById(R.id.btn_pre_launch_remove_game);
        if (btnRemove != null) {
            btnRemove.setOnClickListener(v -> confirmRemove(context, packageName, gameLabel));
        }

        Button btnCancel = view.findViewById(R.id.btn_pre_launch_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismissCurrent());
        }

        // AUTO-INJECT & LAUNCH Execution
        Button btnStart = view.findViewById(R.id.btn_pre_launch_start);
        btnStart.setOnClickListener(v -> {
            int requestedRate = selectedRate(rate185, rate165, rate144, rate120);
            if (requestedRate <= 0) requestedRate = 185;
            final int finalRequestedRate = requestedRate;
            GameProfilePreferences.setTargetHz(context, packageName, finalRequestedRate);

            SharedPreferences.Editor editor = prefs.edit();

            // Save and execute based on game family
            switch (family) {
                case MLBB: {
                    boolean godOn = swMlbbGod != null && swMlbbGod.isChecked();
                    boolean droneOn = swMlbbDrone != null && swMlbbDrone.isChecked();
                    int droneTier = MlbbDroneViewPatcher.TIER_3X;
                    if (rbMlbb15 != null && rbMlbb15.isChecked()) droneTier = MlbbDroneViewPatcher.TIER_1_5X;
                    else if (rbMlbb20 != null && rbMlbb20.isChecked()) droneTier = MlbbDroneViewPatcher.TIER_2X;
                    else if (rbMlbb30 != null && rbMlbb30.isChecked()) droneTier = MlbbDroneViewPatcher.TIER_3X;
                    else if (rbMlbb40 != null && rbMlbb40.isChecked()) droneTier = MlbbDroneViewPatcher.TIER_4X;
                    else if (rbMlbb50 != null && rbMlbb50.isChecked()) droneTier = MlbbDroneViewPatcher.TIER_5X;

                    int heroIdx = spMlbbHero != null ? spMlbbHero.getSelectedItemPosition() : 0;
                    boolean retriOn = swMlbbRetri != null && swMlbbRetri.isChecked();

                    editor.putBoolean("mlbb_god_mode_" + packageName, godOn);
                    editor.putBoolean("mlbb_drone_view_" + packageName, droneOn);
                    editor.putInt("mlbb_drone_tier_" + packageName, droneTier);
                    editor.putInt("mlbb_hero_idx_" + packageName, heroIdx);
                    editor.putBoolean("mlbb_fast_retri_" + packageName, retriOn);

                    final int finalDroneTier = droneTier;
                    AppExecutors.getInstance().executeCommand(() -> {
                        if (finalRequestedRate >= 165) {
                            MlbbFpsUnlockEngine.unlock165Fps(context, packageName);
                            MlbbConfigPatcher.patchUltraExtreme165(packageName);
                            MlbbConfigPatcher.apply165FpsGraphicsUnlock(packageName);
                        } else {
                            MlbbConfigPatcher.patch(packageName, finalRequestedRate);
                        }

                        if (godOn) {
                            MlbbCombatOverdriveEngine.applyCombatOverdrive(context, packageName);
                        }
                        if (droneOn) {
                            MlbbDroneViewPatcher.applyNewMapUpdate(context, packageName, finalDroneTier);
                        } else {
                            MlbbDroneViewPatcher.restoreStockCamera(context, packageName);
                        }
                        if (heroIdx <= 0) {
                            MlbbHeroScriptDispatcher.dispatchAllHeroes(context, packageName);
                        } else {
                            MlbbHeroScriptRegistry.HeroEntry[] meta = MlbbHeroScriptRegistry.getMetaHeroes();
                            int mIdx = heroIdx - 1;
                            if (mIdx >= 0 && mIdx < meta.length && meta[mIdx] != null) {
                                MlbbHeroScriptDispatcher.dispatch(context, packageName, meta[mIdx].id);
                            }
                        }
                        if (retriOn) {
                            com.gamebooster.app.config.MlbbConfigPatcher.applyFastRetributionObjectiveSteal(packageName);
                        }
                    });
                    break;
                }

                case PUBGM: {
                    boolean recoilOn = swPubgmRecoil != null && swPubgmRecoil.isChecked();
                    boolean ipadOn = swPubgmIpad != null && swPubgmIpad.isChecked();
                    boolean bulletOn = swPubgmBullet != null && swPubgmBullet.isChecked();

                    editor.putBoolean("pubgm_zero_recoil_" + packageName, recoilOn);
                    editor.putBoolean("pubgm_ipad_view_" + packageName, ipadOn);
                    editor.putBoolean("pubgm_magic_bullet_" + packageName, bulletOn);

                    AppExecutors.getInstance().executeCommand(() -> {
                        PubgmCombatOverdriveEngine.applyCombatOverdrive(context, packageName);
                        if (finalRequestedRate >= 165) {
                            PubgmFpsUnlockEngine.unlock165Fps(context, packageName);
                            PubgConfigPatcher.patchSuperSmooth165(packageName);
                            PubgConfigPatcher.apply165FpsHdrUnlock(packageName);
                        } else {
                            PubgConfigPatcher.patch(packageName, finalRequestedRate);
                        }

                        if (recoilOn) {
                            PubgConfigPatcher.applyPubgmZeroRecoilNoSway(packageName);
                            PubgConfigPatcher.applyRecoilControlConfig(packageName);
                        }
                        if (ipadOn) {
                            PubgConfigPatcher.applyPubgUltraDroneViewMaxFov(packageName);
                        }
                        if (bulletOn) {
                            PubgConfigPatcher.applyPubgmMagicBulletInstantHit(packageName);
                            PubgConfigPatcher.applyTrackingBulletConfig(packageName);
                        }
                    });
                    break;
                }

                case CODM: {
                    boolean recoilOn = swCodmRecoil != null && swCodmRecoil.isChecked();
                    boolean adsOn = swCodmAds != null && swCodmAds.isChecked();
                    boolean aceOn = swCodmAce != null && swCodmAce.isChecked();

                    editor.putBoolean("codm_zero_recoil_" + packageName, recoilOn);
                    editor.putBoolean("codm_fast_ads_" + packageName, adsOn);
                    editor.putBoolean("codm_ace_cloak_" + packageName, aceOn);

                    AppExecutors.getInstance().executeCommand(() -> {
                        CodmCombatOverdriveEngine.applyCombatOverdrive(context, packageName);
                        if (finalRequestedRate >= 185) {
                            CodmConfigPatcher.patchUltraExtreme185(packageName);
                        } else if (finalRequestedRate >= 165) {
                            CodmConfigPatcher.patchUltraExtreme165(packageName);
                        } else if (finalRequestedRate >= 144) {
                            CodmConfigPatcher.patchUltraExtreme144(packageName);
                        } else {
                            CodmConfigPatcher.patch(packageName, finalRequestedRate);
                        }
                        CodmConfigPatcher.apply165FpsGraphicsUnlock(packageName);

                        if (recoilOn) {
                            CodmConfigPatcher.applyNoRecoilNoSpread(packageName);
                            CodmConfigPatcher.applyRecoilControlConfig(packageName);
                        }
                        if (adsOn) {
                            CodmConfigPatcher.applySpeedBoostConfig(packageName);
                            CodmConfigPatcher.applySuperFastTouch(packageName);
                        }
                        if (aceOn) {
                            CodmAceBypassPatcher.applyBypass(packageName);
                            CodmConfigPatcher.applyFastLoadShaderBypass(packageName);
                        }
                    });
                    break;
                }

                case FREE_FIRE: {
                    boolean headshotOn = swFfHeadshot != null && swFfHeadshot.isChecked();
                    boolean glooOn = swFfGloo != null && swFfGloo.isChecked();

                    editor.putBoolean("ff_headshot_drag_" + packageName, headshotOn);
                    editor.putBoolean("ff_fast_gloo_" + packageName, glooOn);

                    AppExecutors.getInstance().executeCommand(() -> {
                        FreeFireConfigPatcher.applyFreeFireMasterSuite(packageName);
                        FreeFireConfigPatcher.patch(packageName, finalRequestedRate);

                        if (headshotOn) {
                            FreeFireConfigPatcher.applyAutoDragHeadshot(packageName);
                            FreeFireConfigPatcher.applyFreeFireAutoHeadshot(packageName);
                            FreeFireConfigPatcher.applyAimAssistLockMax(packageName);
                        }
                        if (glooOn) {
                            FreeFireConfigPatcher.applyInstant360GlooWall(packageName);
                            FreeFireConfigPatcher.applyFreeFireFastGlooWall(packageName);
                            FreeFireConfigPatcher.applyFastLootAndWeaponSwap(packageName);
                        }
                    });
                    break;
                }

                case OTHER:
                default: {
                    boolean spoofOn = swUnivSpoof != null && swUnivSpoof.isChecked();
                    boolean coresOn = swUnivCores != null && swUnivCores.isChecked();

                    editor.putBoolean("univ_spoof_" + packageName, spoofOn);
                    editor.putBoolean("univ_cores_" + packageName, coresOn);

                    AppExecutors.getInstance().executeCommand(() -> {
                        if (spoofOn) {
                            DeviceSpooferEngine.applyWorkingSpoofForGame(context, packageName, finalRequestedRate);
                        }
                        if (coresOn) {
                            NativeKernelBridge.isolateProcessToPrimeCores(context, packageName);
                        }
                        CommonConfigTuningInjector.applySuperFastTouch(packageName);
                    });
                    break;
                }
            }

            editor.apply();

            // Core Global Overclocks: Display Hz & Touch Latency
            AppExecutors.getInstance().executeCommand(() -> {
                FpsForceUnlockEngine.applyFpsForceUnlock(context, packageName, finalRequestedRate);
                TouchLatencyChannel.enableUltraTouchResponse();
            });

            dismissCurrent();
            GameManagerLauncher.launchGame(context, game);
        });

        dialog.setCanceledOnTouchOutside(true);
        if (onDismissListener != null) {
            dialog.setOnDismissListener(d -> onDismissListener.run());
        }
        activeDialog = dialog;
        dialog.show();
    }

    private static void configureRateButton(RadioButton button, int rate,
                                            DevicePerformanceCapabilities capabilities) {
        if (button == null) return;
        boolean available = capabilities.supportsRefreshRate(rate) || rate == 185 || rate == 165 || rate == 144 || rate == 120;
        button.setVisibility(available ? View.VISIBLE : View.GONE);
        if (rate == 185) {
            button.setText("Use 185Hz display mode (Ultra Extreme Overdrive)");
        } else if (rate == 165) {
            button.setText("Use 165Hz display mode (Super Smooth Extreme)");
        } else if (rate == 144) {
            button.setText("Use 144Hz display mode (Max Physical Refresh Rate)");
        } else if (rate == 120) {
            button.setText("Use 120Hz display mode (Esports Pro Standard)");
        } else {
            button.setText("Use " + rate + "Hz display mode");
        }
    }

    private static void checkRateButton(int selectedRate, RadioButton button, int buttonRate) {
        if (button != null && button.getVisibility() == View.VISIBLE && selectedRate == buttonRate) {
            button.setChecked(true);
        }
    }

    private static int selectedRate(RadioButton rate185, RadioButton rate165,
                                    RadioButton rate144, RadioButton rate120) {
        if (rate185 != null && rate185.isChecked()) return 185;
        if (rate165 != null && rate165.isChecked()) return 165;
        if (rate144 != null && rate144.isChecked()) return 144;
        if (rate120 != null && rate120.isChecked()) {
            Object requestedRate = rate120.getTag();
            return requestedRate instanceof Integer ? (Integer) requestedRate : 120;
        }
        return 0;
    }

    private static void showSystemDefaultWhenNeeded(RadioButton rate185, RadioButton rate165,
                                                    RadioButton rate144, RadioButton rate120,
                                                    DevicePerformanceCapabilities capabilities) {
        boolean hasDedicatedOption = (rate185 != null && rate185.getVisibility() == View.VISIBLE)
                || (rate165 != null && rate165.getVisibility() == View.VISIBLE)
                || (rate144 != null && rate144.getVisibility() == View.VISIBLE)
                || (rate120 != null && rate120.getVisibility() == View.VISIBLE);
        if (hasDedicatedOption || rate120 == null || capabilities.getMaxRefreshRate() <= 0) return;

        rate120.setVisibility(View.VISIBLE);
        rate120.setTag(Integer.valueOf(0));
        rate120.setText("Use system default (" + capabilities.getMaxRefreshRate() + "Hz maximum)");
        rate120.setChecked(true);
    }

    private static void confirmRemove(Context context, String packageName, String label) {
        dismissCurrent();
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Remove game")
                .setMessage("Remove " + label + " from the Home Launcher list?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    com.gamebooster.app.games.GameLauncherHelper.removeGameFromHome(context, packageName);
                    Toast.makeText(context, "Removed " + label + " from Home", Toast.LENGTH_SHORT).show();
                    if (context instanceof com.gamebooster.app.ui.activities.MainActivity) {
                        ((com.gamebooster.app.ui.activities.MainActivity) context).reloadHomeGames();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String describeDisplay(DevicePerformanceCapabilities capabilities) {
        String base = capabilities.getSupportedRefreshRates().isEmpty()
                ? "Physical modes: 60Hz, 120Hz, 144Hz"
                : "Available display modes: " + capabilities.getSupportedRefreshRates();
        return base + " • Overclock: 165Hz, 185Hz Unlocked";
    }

    public static void dismissCurrent() {
        if (activeDialog == null) return;
        try {
            if (activeDialog.isShowing()) activeDialog.dismiss();
        } catch (Throwable ignored) {
        }
        activeDialog = null;
    }
}
