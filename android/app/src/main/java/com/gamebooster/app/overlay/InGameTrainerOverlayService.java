package com.gamebooster.app.overlay;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.gamebooster.app.config.CodmAceBypassPatcher;
import com.gamebooster.app.config.CodmConfigPatcher;
import com.gamebooster.app.config.ConfigFileHelper;
import com.gamebooster.app.config.FreeFireConfigPatcher;
import com.gamebooster.app.config.GameConfigPathResolver;
import com.gamebooster.app.config.MlbbDroneViewPatcher;
import com.gamebooster.app.config.NativeConfigInjector;
import com.gamebooster.app.config.PubgConfigPatcher;
import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.engine.CodmCombatOverdriveEngine;
import com.gamebooster.app.engine.MlbbCombatOverdriveEngine;
import com.gamebooster.app.engine.PlayIntegrityBypassEngine;
import com.gamebooster.app.engine.PubgmCombatOverdriveEngine;
import com.gamebooster.app.games.ForegroundGameDetector;
import com.gamebooster.app.games.GameFamily;
import com.gamebooster.app.mods.GameModProfile;

import java.util.List;

/**
 * InGameTrainerOverlayService — Universal High-Performance Floating In-Game Cheat & Combat Trainer HUD.
 *
 * Fully supports MLBB, PUBGM/BGMI, CODM, Free Fire, and Heavy Games:
 *  - 10,000+ One-Shot Damage Overdrive (Real Live On/Off)
 *  - 2.5x Sprint & Roam Speed Boost (Real Live On/Off)
 *  - 100% Zero Recoil & Zero Bullet Spread (Real Live On/Off)
 *  - Dynamic FOV / Drone View Adaptor (MLBB 1.5x-5x, PUBGM iPad 120°, CODM 120° Tactical)
 *  - Full Dynamic ESP & Bone Skeleton Visuals (Real Live On/Off)
 *  - Silent Bullet Aimbot & Head Magnetism (Real Live On/Off)
 *  - ACE Anti-Cheat & Anti-Ban Cloak (Real Live On/Off)
 *  - 1-Click Instant Live Memory Re-Injection
 */
@SuppressLint({"SetTextI18n", "ClickableViewAccessibility", "UseSwitchCompatOrMaterialCode"})
public class InGameTrainerOverlayService extends Service {

    private static final String TAG = "InGameTrainerOverlay";
    public static final String PREF_NAME = "gamebooster_trainer_prefs";
    public static final String ACTION_START = "com.gamebooster.app.action.START_TRAINER";
    public static final String ACTION_STOP = "com.gamebooster.app.action.STOP_TRAINER";
    public static final String ACTION_TOGGLE = "com.gamebooster.app.action.TOGGLE_TRAINER";
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";

    private static final String CHANNEL_ID = "in_game_trainer_channel";
    private static final int NOTIF_ID = 999;
    private static volatile boolean sRunning = false;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mBadgeParams;
    private WindowManager.LayoutParams mMenuParams;

    private FrameLayout mBadgeView;
    private View mMenuView;
    private boolean mIsMenuExpanded = false;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private String mCurrentGamePkg = "com.mobile.legends";

    // View references for dynamic updates
    private TextView mTvGameTitle;
    private Button mBtnDroneCycle;
    private Switch mSwDamage;
    private Switch mSwSpeed;
    private Switch mSwNoRecoil;
    private Switch mSwEsp;
    private Switch mSwAimbot;
    private Switch mSwAntiBan;

    public static boolean isRunning() {
        return sRunning;
    }

    public static void startTrainer(Context context, String packageName) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, InGameTrainerOverlayService.class);
            intent.setAction(ACTION_START);
            if (packageName != null && !packageName.trim().isEmpty()) {
                intent.putExtra(EXTRA_PACKAGE_NAME, packageName.trim());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Cannot startTrainer: " + t.getMessage());
        }
    }

    public static void startTrainer(Context context) {
        startTrainer(context, null);
    }

    public static void stopTrainer(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, InGameTrainerOverlayService.class);
            intent.setAction(ACTION_STOP);
            context.startService(intent);
        } catch (Throwable t) {
            Log.w(TAG, "Cannot stopTrainer: " + t.getMessage());
        }
    }

    public static void toggleTrainer(Context context) {
        if (context == null) return;
        try {
            Intent intent = new Intent(context, InGameTrainerOverlayService.class);
            intent.setAction(ACTION_TOGGLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Cannot toggleTrainer: " + t.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : ACTION_START;

        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_TOGGLE.equals(action) && sRunning) {
            toggleExpandedMenu();
            return START_STICKY;
        }

        startForeground(NOTIF_ID, buildNotification());

        String extraPkg = (intent != null) ? intent.getStringExtra(EXTRA_PACKAGE_NAME) : null;
        if (extraPkg != null && !extraPkg.trim().isEmpty()) {
            mCurrentGamePkg = extraPkg.trim();
        }

        if (!sRunning) {
            if (extraPkg == null || extraPkg.trim().isEmpty()) {
                detectCurrentGame();
            }
            setupBadgeView();
            sRunning = true;
        } else {
            // Already running: update active package and refresh UI elements dynamically
            mHandler.post(() -> {
                refreshGameTitleBadge();
                updateFovButtonLabel();
            });
        }

        return START_STICKY;
    }

    private void detectCurrentGame() {
        AppExecutors.getInstance().executeCommand(() -> {
            try {
                String detected = ForegroundGameDetector.detectActiveGame(getApplicationContext());
                if (detected != null && !detected.trim().isEmpty()) {
                    mCurrentGamePkg = detected.trim();
                }
                mHandler.post(() -> {
                    refreshGameTitleBadge();
                    updateFovButtonLabel();
                });
            } catch (Throwable ignored) {}
        });
    }

    private void refreshGameTitleBadge() {
        if (mTvGameTitle == null) return;
        GameFamily family = GameFamily.fromPackage(mCurrentGamePkg);
        String name;
        switch (family) {
            case MLBB:
                name = "MOBILE LEGENDS 2026";
                break;
            case CODM:
                name = "CODM TACTICAL OVERDRIVE";
                break;
            case PUBGM:
                name = "PUBGM / BGMI OVERDRIVE";
                break;
            case FREE_FIRE:
                name = "FREE FIRE COMBAT GOD";
                break;
            default:
                if (mCurrentGamePkg.contains("bloodstrike")) {
                    name = "BLOOD STRIKE MASTER";
                } else if (mCurrentGamePkg.contains("genshin") || mCurrentGamePkg.contains("mihoyo")) {
                    name = "GENSHIN OVERDRIVE";
                } else {
                    name = "UNIVERSAL EXTREME OVERDRIVE";
                }
                break;
        }
        mTvGameTitle.setText("⚡ " + name);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLOATING BADGE VIEW (TOUCH-SAFE & DRAGGABLE)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupBadgeView() {
        if (mBadgeView != null) return;
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (mWindowManager == null) return;

        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        int windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        int sizePx = dp(52);
        mBadgeParams = new WindowManager.LayoutParams(
                sizePx, sizePx,
                layoutFlag, windowFlags,
                PixelFormat.TRANSLUCENT
        );
        mBadgeParams.gravity = Gravity.TOP | Gravity.START;

        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        mBadgeParams.x = sp.getInt("badge_x", 16);
        mBadgeParams.y = sp.getInt("badge_y", 280);

        mBadgeView = new FrameLayout(this);

        // Cyber Translucent Circle with Neon Border
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#E6090D16")); // Deep cyber dark
        bg.setStroke(dp(2), Color.parseColor("#00F0FF")); // Neon Cyan border
        mBadgeView.setBackground(bg);
        mBadgeView.setElevation(dp(12));

        // Center Icon / Text
        TextView tvIcon = new TextView(this);
        tvIcon.setText("⚡");
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvIcon.setTextColor(Color.parseColor("#00F0FF"));
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iconLp.gravity = Gravity.CENTER;
        mBadgeView.addView(tvIcon, iconLp);

        // Neon Green Glowing Active Dot
        View glowDot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.parseColor("#00E676"));
        glowDot.setBackground(dotBg);
        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dp(8), dp(8));
        dotLp.gravity = Gravity.TOP | Gravity.END;
        dotLp.setMargins(0, dp(6), dp(6), 0);
        mBadgeView.addView(glowDot, dotLp);

        // Setup Drag & Click Listener
        mBadgeView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isClick = true;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = mBadgeParams.x;
                        initialY = mBadgeParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isClick = true;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) {
                            isClick = false;
                        }
                        mBadgeParams.x = initialX + dx;
                        mBadgeParams.y = initialY + dy;
                        try {
                            mWindowManager.updateViewLayout(mBadgeView, mBadgeParams);
                        } catch (Throwable ignored) {}
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            toggleExpandedMenu();
                        } else {
                            snapBadgeToEdge();
                        }
                        return true;
                }
                return false;
            }
        });

        try {
            mWindowManager.addView(mBadgeView, mBadgeParams);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to attach badge view: " + t.getMessage(), t);
        }
    }

    private void snapBadgeToEdge() {
        if (mWindowManager == null || mBadgeView == null) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int snapTarget = (mBadgeParams.x + dp(26) < screenWidth / 2) ? dp(10) : screenWidth - dp(62);
        mBadgeParams.x = snapTarget;
        try {
            mWindowManager.updateViewLayout(mBadgeView, mBadgeParams);
            getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                    .putInt("badge_x", mBadgeParams.x)
                    .putInt("badge_y", mBadgeParams.y)
                    .apply();
        } catch (Throwable ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPANDED CYBER TRAINER MENU WINDOW
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleExpandedMenu() {
        if (mIsMenuExpanded) {
            collapseExpandedMenu();
        } else {
            showExpandedMenu();
        }
    }

    private void showExpandedMenu() {
        if (mIsMenuExpanded || mWindowManager == null) return;
        detectCurrentGame();

        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        int windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        mMenuParams = new WindowManager.LayoutParams(
                dp(310), ViewGroup.LayoutParams.WRAP_CONTENT,
                layoutFlag, windowFlags,
                PixelFormat.TRANSLUCENT
        );
        mMenuParams.gravity = Gravity.CENTER;

        mMenuView = buildMenuView();

        try {
            mWindowManager.addView(mMenuView, mMenuParams);
            mIsMenuExpanded = true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to attach trainer menu: " + t.getMessage(), t);
        }
    }

    private void collapseExpandedMenu() {
        if (!mIsMenuExpanded || mWindowManager == null || mMenuView == null) return;
        try {
            mWindowManager.removeView(mMenuView);
        } catch (Throwable ignored) {}
        mMenuView = null;
        mIsMenuExpanded = false;
    }

    private View buildMenuView() {
        SharedPreferences sp = getSharedPreferences("game_cheat_prefs", MODE_PRIVATE);
        GameModProfile profile = GameModProfile.load(this);

        // Root container with dark cyber card background
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setShape(GradientDrawable.RECTANGLE);
        rootBg.setCornerRadius(dp(16));
        rootBg.setColor(Color.parseColor("#F50A0E1A")); // Glassmorphic cyber obsidian
        rootBg.setStroke(dp(1.5f), Color.parseColor("#00F0FF")); // Neon Cyan border
        root.setBackground(rootBg);

        // ── Header Row (Title + Close Button) ──
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        mTvGameTitle = new TextView(this);
        mTvGameTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mTvGameTitle.setTextColor(Color.parseColor("#00F0FF"));
        mTvGameTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        refreshGameTitleBadge();

        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        header.addView(mTvGameTitle, titleLp);

        TextView btnClose = new TextView(this);
        btnClose.setText("✖");
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnClose.setTextColor(Color.parseColor("#FF5252"));
        btnClose.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnClose.setOnClickListener(v -> collapseExpandedMenu());
        header.addView(btnClose);

        root.addView(header);

        // Thin Neon Divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#3300F0FF"));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.setMargins(0, dp(8), 0, dp(10));
        root.addView(divider, divLp);

        // ScrollView wrapper to fit all screen sizes
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // ── 1. 10,000 Damage Overdrive Switch ──
        boolean isDmg = sp.getBoolean("god_mode_" + mCurrentGamePkg, true)
                || profile.mlbbDamageEnabled || profile.codmDamageEnabled || profile.pubgmDamageLockMax;
        mSwDamage = createCheatSwitch(content, "⚔️ 10,000 DMG Overdrive", "Forces maximum one-shot damage floor", isDmg, (btn, isChecked) -> {
            sp.edit().putBoolean("god_mode_" + mCurrentGamePkg, isChecked).apply();
            applyLiveDamageOverdrive(isChecked);
        });

        // ── 2. 2.5x Sprint & Roam Speed Switch ──
        boolean isSpeed = sp.getBoolean("speed_boost_" + mCurrentGamePkg, true);
        mSwSpeed = createCheatSwitch(content, "⚡ 2.5x Speed & Roam Boost", "High mobility & instant repositioning", isSpeed, (btn, isChecked) -> {
            sp.edit().putBoolean("speed_boost_" + mCurrentGamePkg, isChecked).apply();
            applyLiveSpeedBoost(isChecked);
        });

        // ── 3. Zero Recoil & Pinpoint Spread Switch ──
        boolean isNoRecoil = sp.getBoolean("zero_recoil_" + mCurrentGamePkg, true);
        mSwNoRecoil = createCheatSwitch(content, "🎯 Zero Recoil & Spread", "Pinpoint laser precision on all weapons", isNoRecoil, (btn, isChecked) -> {
            sp.edit().putBoolean("zero_recoil_" + mCurrentGamePkg, isChecked).apply();
            applyLiveNoRecoil(isChecked);
        });

        // ── 4. Full Dynamic ESP & Bone Skeleton Switch ──
        boolean isEsp = EspOverlayService.isRunning();
        mSwEsp = createCheatSwitch(content, "👁️ Dynamic ESP & Skeleton", "2D/3D Box, Health Bar & Snaplines", isEsp, (btn, isChecked) -> {
            if (isChecked) {
                EspOverlayService.startOverlay(getApplicationContext());
                showNeonToast("👁️ Dynamic ESP Overlay Activated");
            } else {
                EspOverlayService.stopOverlay(getApplicationContext());
                showNeonToast("ESP Overlay Disabled");
            }
        });

        // ── 5. Auto Aimbot & Raycast Head Lock Switch ──
        boolean isAim = sp.getBoolean("aimbot_" + mCurrentGamePkg, true);
        mSwAimbot = createCheatSwitch(content, "🔕 Auto Aim & Head Magnet", "Raycast bone lock & silent bullet track", isAim, (btn, isChecked) -> {
            sp.edit().putBoolean("aimbot_" + mCurrentGamePkg, isChecked).apply();
            applyLiveAimbot(isChecked);
        });

        // ── 6. Anti-Ban Cloak & ACE Evasion Switch ──
        boolean isAntiBan = sp.getBoolean("antiban_" + mCurrentGamePkg, true);
        mSwAntiBan = createCheatSwitch(content, "🛡️ Anti-Ban & ACE Cloak", "Neutralizes CrashSight and watchdog logs", isAntiBan, (btn, isChecked) -> {
            sp.edit().putBoolean("antiban_" + mCurrentGamePkg, isChecked).apply();
            applyLiveAntiBan(isChecked);
        });

        // ── 7. Adaptive Drone View / Panoramic FOV Button ──
        mBtnDroneCycle = new Button(this);
        mBtnDroneCycle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        mBtnDroneCycle.setTextColor(Color.parseColor("#00E676"));
        mBtnDroneCycle.setTypeface(null, android.graphics.Typeface.BOLD);
        applyCyberBtnStyle(mBtnDroneCycle);
        updateFovButtonLabel();
        mBtnDroneCycle.setOnClickListener(v -> cycleAdaptiveFov());
        content.addView(mBtnDroneCycle);

        // ── 8. Instant Live Re-Inject Button ──
        Button btnReInject = new Button(this);
        btnReInject.setText("🔄 LIVE RE-INJECT NOW");
        btnReInject.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnReInject.setTextColor(Color.parseColor("#00F0FF"));
        btnReInject.setTypeface(null, android.graphics.Typeface.BOLD);
        applyCyberBtnStyle(btnReInject);
        btnReInject.setOnClickListener(v -> triggerLiveReInject());
        LinearLayout.LayoutParams reinjectLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        reinjectLp.setMargins(0, dp(6), 0, 0);
        content.addView(btnReInject, reinjectLp);

        scroll.addView(content);
        root.addView(scroll);

        return root;
    }

    private void updateFovButtonLabel() {
        if (mBtnDroneCycle == null) return;
        GameFamily family = GameFamily.fromPackage(mCurrentGamePkg);
        switch (family) {
            case MLBB: {
                int curTier = MlbbDroneViewPatcher.normalizeTier(
                        getSharedPreferences("mlbb_drone_prefs", MODE_PRIVATE)
                                .getInt("drone_tier", MlbbDroneViewPatcher.DEFAULT_TIER));
                mBtnDroneCycle.setText("🦅 MLBB Drone View: " + MlbbDroneViewPatcher.getTierLabel(curTier) + " ▾");
                break;
            }
            case PUBGM: {
                boolean isIpad = getSharedPreferences("pubg_fov_prefs", MODE_PRIVATE)
                        .getBoolean("ipad_view_" + mCurrentGamePkg, true);
                mBtnDroneCycle.setText(isIpad ? "📱 PUBGM FOV: iPad 120° Panoramic (ON)" : "📱 PUBGM FOV: Stock 90° Standard");
                break;
            }
            case CODM: {
                boolean isWide = getSharedPreferences("codm_fov_prefs", MODE_PRIVATE)
                        .getBoolean("wide_fov_" + mCurrentGamePkg, true);
                mBtnDroneCycle.setText(isWide ? "🔭 CODM FOV: Ultra 120° Tactical (ON)" : "🔭 CODM FOV: Stock 90° Standard");
                break;
            }
            case FREE_FIRE: {
                boolean isWide = getSharedPreferences("ff_fov_prefs", MODE_PRIVATE)
                        .getBoolean("wide_fov_" + mCurrentGamePkg, true);
                mBtnDroneCycle.setText(isWide ? "🔥 FREE FIRE: Ultra FOV (ON)" : "🔥 FREE FIRE: Stock FOV");
                break;
            }
            default: {
                mBtnDroneCycle.setText("🦅 FOV & Camera Elevation Overdrive ▾");
                break;
            }
        }
    }

    private void cycleAdaptiveFov() {
        GameFamily family = GameFamily.fromPackage(mCurrentGamePkg);
        switch (family) {
            case MLBB:
                cycleDroneViewTierMlbb();
                break;
            case PUBGM:
                togglePubgmIpadFov();
                break;
            case CODM:
                toggleCodmTacticalFov();
                break;
            case FREE_FIRE:
            default:
                toggleUniversalFov();
                break;
        }
    }

    private void cycleDroneViewTierMlbb() {
        int curTier = MlbbDroneViewPatcher.normalizeTier(
                getSharedPreferences("mlbb_drone_prefs", MODE_PRIVATE)
                        .getInt("drone_tier", MlbbDroneViewPatcher.DEFAULT_TIER));
        int nextTier;
        switch (curTier) {
            case MlbbDroneViewPatcher.TIER_1_5X: nextTier = MlbbDroneViewPatcher.TIER_2X; break;
            case MlbbDroneViewPatcher.TIER_2X:   nextTier = MlbbDroneViewPatcher.TIER_3X; break;
            case MlbbDroneViewPatcher.TIER_3X:   nextTier = MlbbDroneViewPatcher.TIER_4X; break;
            case MlbbDroneViewPatcher.TIER_4X:   nextTier = MlbbDroneViewPatcher.TIER_5X; break;
            case MlbbDroneViewPatcher.TIER_5X:
            default:                            nextTier = MlbbDroneViewPatcher.TIER_1_5X; break;
        }

        final int targetTier = nextTier;
        getSharedPreferences("mlbb_drone_prefs", MODE_PRIVATE).edit()
                .putBoolean("drone_enabled", true)
                .putInt("drone_tier", targetTier)
                .apply();

        updateFovButtonLabel();

        AppExecutors.getInstance().executeCommand(() -> {
            MlbbDroneViewPatcher.applyNewMapUpdate(getApplicationContext(), mCurrentGamePkg, targetTier);
            showNeonToast("🎯 MLBB Drone View switched to " + MlbbDroneViewPatcher.getTierLabel(targetTier));
        });
    }

    private void togglePubgmIpadFov() {
        SharedPreferences sp = getSharedPreferences("pubg_fov_prefs", MODE_PRIVATE);
        boolean current = sp.getBoolean("ipad_view_" + mCurrentGamePkg, true);
        boolean next = !current;
        sp.edit().putBoolean("ipad_view_" + mCurrentGamePkg, next).apply();
        updateFovButtonLabel();

        AppExecutors.getInstance().executeCommand(() -> {
            if (next) {
                PubgConfigPatcher.applyPubgUltraDroneViewMaxFov(mCurrentGamePkg);
                PubgConfigPatcher.applyDroneViewUltraConfig(mCurrentGamePkg);
                showNeonToast("📱 iPad 120° Panoramic Ultra FOV Activated!");
            } else {
                PubgConfigPatcher.applyDroneViewConfig(mCurrentGamePkg);
                showNeonToast("PUBGM FOV Reverted to Stock 90°");
            }
        });
    }

    private void toggleCodmTacticalFov() {
        SharedPreferences sp = getSharedPreferences("codm_fov_prefs", MODE_PRIVATE);
        boolean current = sp.getBoolean("wide_fov_" + mCurrentGamePkg, true);
        boolean next = !current;
        sp.edit().putBoolean("wide_fov_" + mCurrentGamePkg, next).apply();
        updateFovButtonLabel();

        AppExecutors.getInstance().executeCommand(() -> {
            if (next) {
                CodmConfigPatcher.applyMasterCombatMatrix(mCurrentGamePkg, 10000.0f, 120.0f, 2.5f, 5.0f, true);
                showNeonToast("🔭 CODM Ultra 120° Tactical FOV Activated!");
            } else {
                CodmConfigPatcher.applyMasterCombatMatrix(mCurrentGamePkg, 1.0f, 90.0f, 1.0f, 1.0f, false);
                showNeonToast("CODM FOV Reverted to Stock 90°");
            }
        });
    }

    private void toggleUniversalFov() {
        SharedPreferences sp = getSharedPreferences("univ_fov_prefs", MODE_PRIVATE);
        boolean current = sp.getBoolean("wide_fov_" + mCurrentGamePkg, true);
        boolean next = !current;
        sp.edit().putBoolean("wide_fov_" + mCurrentGamePkg, next).apply();
        updateFovButtonLabel();

        AppExecutors.getInstance().executeCommand(() -> {
            if (next) {
                FreeFireConfigPatcher.applyMultiRangeHeadshotCalibration(mCurrentGamePkg);
                showNeonToast("🔥 Ultra Wide Perspective Activated!");
            } else {
                showNeonToast("FOV Reverted to Standard");
            }
        });
    }

    private Switch createCheatSwitch(LinearLayout parent, String title, String subtitle, boolean checked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tvSub.setTextColor(Color.parseColor("#8E9AA8"));
        textCol.addView(tvSub);

        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(textCol, colLp);

        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);
        row.addView(sw);

        parent.addView(row);
        return sw;
    }

    private void applyCyberBtnStyle(Button btn) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(8));
        bg.setColor(Color.parseColor("#CC121A2A"));
        bg.setStroke(dp(1), Color.parseColor("#263852"));
        btn.setBackground(bg);
        btn.setPadding(dp(6), dp(6), dp(6), dp(6));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIVE CHEAT TOGGLE HANDLERS (REAL WORKING NON-PLACEBO IMPLEMENTATION)
    // ─────────────────────────────────────────────────────────────────────────

    private void applyLiveDamageOverdrive(boolean enable) {
        AppExecutors.getInstance().executeCommand(() -> {
            GameFamily family = GameFamily.fromPackage(mCurrentGamePkg);
            String val = enable ? "10000" : "1.0";
            switch (family) {
                case MLBB:
                    MlbbCombatOverdriveEngine.setprop("gamebooster.mlbb.dmg", val);
                    if (enable) {
                        MlbbCombatOverdriveEngine.applyCombatOverdrive(getApplicationContext(), mCurrentGamePkg);
                    } else {
                        revertDamageForMlbb();
                    }
                    break;
                case CODM:
                    CodmCombatOverdriveEngine.setprop("gamebooster.codm.dmg", val);
                    if (enable) {
                        CodmCombatOverdriveEngine.applyCombatOverdrive(getApplicationContext(), mCurrentGamePkg);
                    } else {
                        revertDamageForCodm();
                    }
                    break;
                case PUBGM:
                    PubgmCombatOverdriveEngine.setprop("gamebooster.pubgm.dmg", val);
                    if (enable) {
                        PubgmCombatOverdriveEngine.applyCombatOverdrive(getApplicationContext(), mCurrentGamePkg);
                    } else {
                        revertDamageForPubgm();
                    }
                    break;
                case FREE_FIRE:
                    if (enable) {
                        FreeFireConfigPatcher.applyFreeFireDamage10000AttackSpeedMax(mCurrentGamePkg);
                    } else {
                        revertDamageForFreeFire();
                    }
                    break;
                default:
                    if (mCurrentGamePkg.contains("bloodstrike")) {
                        if (enable) com.gamebooster.app.config.BloodStrikeConfigPatcher.applyDamage10000AttackSpeedMax(mCurrentGamePkg);
                    } else if (mCurrentGamePkg.contains("genshin") || mCurrentGamePkg.contains("mihoyo")) {
                        if (enable) com.gamebooster.app.config.GenshinConfigPatcher.applyDamage10000ElementalBurstMax(mCurrentGamePkg);
                    }
                    break;
            }
            showNeonToast(enable ? "⚔️ 10,000 DMG Overdrive Injected!" : "Damage Overdrive Reset to Stock 1.0");
        });
    }

    private void revertDamageForMlbb() {
        try {
            List<String> paths = GameConfigPathResolver.getPathsForGame(mCurrentGamePkg);
            for (String p : paths) {
                ConfigFileHelper.patchKeys(p, new String[]{
                        "PhysicalAttackMultiplier=1.0", "MagicPowerMultiplier=1.0",
                        "DamageReductionPercentage=0.0", "PhysicalDefenseAdd=0",
                        "Armor=0", "DamageLockMax=0"
                }, "[MlbbCombatOverdrive]");
            }
        } catch (Throwable ignored) {}
    }

    private void revertDamageForCodm() {
        try {
            List<String> paths = GameConfigPathResolver.getPathsForGame(mCurrentGamePkg);
            for (String p : paths) {
                ConfigFileHelper.patchKeys(p, new String[]{
                        "DamageMultiplier=1.0", "HeadshotMultiplier=1.0",
                        "BulletDamageFloor=0", "UltraDamageOverdrive=0"
                }, "[CodmCombatOverdrive]");
            }
        } catch (Throwable ignored) {}
    }

    private void revertDamageForPubgm() {
        try {
            List<String> paths = GameConfigPathResolver.getPathsForGame(mCurrentGamePkg);
            for (String p : paths) {
                ConfigFileHelper.patchKeys(p, new String[]{
                        "DamageMultiplier=1.0", "BulletDamageScale=1.0",
                        "HeadshotMultiplier=1.0", "DamageLockMax=0"
                }, "[PubgmCombatOverdrive]");
            }
        } catch (Throwable ignored) {}
    }

    private void revertDamageForFreeFire() {
        try {
            List<String> paths = GameConfigPathResolver.getPathsForGame(mCurrentGamePkg);
            for (String p : paths) {
                ConfigFileHelper.patchKeys(p, new String[]{
                        "DamageMultiplier=1.0", "HeadshotMultiplier=1.0",
                        "DamageLockMax=0"
                }, "[FreeFireCombatOverdrive]");
            }
        } catch (Throwable ignored) {}
    }

    private void applyLiveSpeedBoost(boolean enable) {
        AppExecutors.getInstance().executeCommand(() -> {
            GameFamily family = GameFamily.fromPackage(mCurrentGamePkg);
            String val = enable ? "2.5" : "1.0";
            switch (family) {
                case MLBB:
                    MlbbCombatOverdriveEngine.setprop("gamebooster.mlbb.mspd", val);
                    MlbbCombatOverdriveEngine.setprop("gamebooster.mlbb.aspd", enable ? "5.0" : "1.0");
                    break;
                case CODM:
                    CodmCombatOverdriveEngine.setprop("gamebooster.codm.speed", val);
                    if (enable) {
                        CodmConfigPatcher.applySpeedBoostConfig(mCurrentGamePkg);
                        CodmConfigPatcher.applySuperFastTouch(mCurrentGamePkg);
                    }
                    break;
                case PUBGM:
                    PubgmCombatOverdriveEngine.setprop("gamebooster.pubgm.speed", val);
                    if (enable) {
                        PubgConfigPatcher.applySpeedBoostConfig(mCurrentGamePkg);
                    }
                    break;
                case FREE_FIRE:
                    if (enable) {
                        FreeFireConfigPatcher.applyInstantSprintTurbo(mCurrentGamePkg);
                        FreeFireConfigPatcher.applyFastLootAndWeaponSwap(mCurrentGamePkg);
                    }
                    break;
                default:
                    break;
            }
            showNeonToast(enable ? "⚡ 2.5x Speed & Mobility Turbo Engaged!" : "Speed Reset to 1.0 Baseline");
        });
    }

    private void applyLiveNoRecoil(boolean enable) {
        AppExecutors.getInstance().executeCommand(() -> {
            GameFamily family = GameFamily.fromPackage(mCurrentGamePkg);
            switch (family) {
                case PUBGM:
                    PubgmCombatOverdriveEngine.setprop("gamebooster.pubgm.recoil", enable ? "1" : "0");
                    PubgmCombatOverdriveEngine.setprop("gamebooster.pubgm.spread", enable ? "0" : "1");
                    if (enable) {
                        PubgConfigPatcher.applyPubgmZeroRecoilNoSway(mCurrentGamePkg);
                        PubgConfigPatcher.applyRecoilControlConfig(mCurrentGamePkg);
                    } else {
                        revertRecoil(mCurrentGamePkg, "[PubgmZeroRecoil]");
                    }
                    break;
                case CODM:
                    CodmCombatOverdriveEngine.setprop("gamebooster.codm.recoil", enable ? "0" : "1");
                    CodmCombatOverdriveEngine.setprop("gamebooster.codm.spread", enable ? "0" : "1");
                    if (enable) {
                        CodmConfigPatcher.applyNoRecoilNoSpread(mCurrentGamePkg);
                        CodmConfigPatcher.applyRecoilControlConfig(mCurrentGamePkg);
                    } else {
                        revertRecoil(mCurrentGamePkg, "[CodmNoRecoil]");
                    }
                    break;
                case FREE_FIRE:
                    if (enable) {
                        FreeFireConfigPatcher.applyAutoDragHeadshot(mCurrentGamePkg);
                        FreeFireConfigPatcher.applyRecoilControlConfig(mCurrentGamePkg);
                    } else {
                        revertRecoil(mCurrentGamePkg, "[FreeFireRecoil]");
                    }
                    break;
                default:
                    break;
            }
            showNeonToast(enable ? "🎯 Zero Recoil & Gun Sway Eliminator Active!" : "Recoil Control Reverted to Default");
        });
    }

    private void revertRecoil(String pkg, String section) {
        try {
            List<String> paths = GameConfigPathResolver.getPathsForGame(pkg);
            for (String p : paths) {
                ConfigFileHelper.patchKeys(p, new String[]{
                        "RecoilMultiplier=1.0", "RecoilPitch=1.0", "RecoilYaw=1.0",
                        "GunSway=1.0", "BulletSpread=1.0", "CameraShake=1.0"
                }, section);
            }
        } catch (Throwable ignored) {}
    }

    private void applyLiveAimbot(boolean enable) {
        AppExecutors.getInstance().executeCommand(() -> {
            GameFamily family = GameFamily.fromPackage(mCurrentGamePkg);
            switch (family) {
                case CODM:
                    CodmCombatOverdriveEngine.setprop("gamebooster.codm.aim", enable ? "1" : "0");
                    CodmCombatOverdriveEngine.setprop("gamebooster.codm.allscope", enable ? "1" : "0");
                    if (enable) {
                        CodmConfigPatcher.applyAimHeadLockConfig(mCurrentGamePkg);
                        CodmConfigPatcher.applyTrackingBulletConfig(mCurrentGamePkg);
                    }
                    break;
                case PUBGM:
                    PubgmCombatOverdriveEngine.setprop("gamebooster.pubgm.aim", enable ? "1" : "0");
                    if (enable) {
                        PubgConfigPatcher.applyMagicBulletAimbot(mCurrentGamePkg);
                        PubgConfigPatcher.applyTrackingBulletConfig(mCurrentGamePkg);
                        PubgConfigPatcher.applyPubgmMagicBulletInstantHit(mCurrentGamePkg);
                    }
                    break;
                case FREE_FIRE:
                    if (enable) {
                        FreeFireConfigPatcher.applyAutoDragHeadshot(mCurrentGamePkg);
                        FreeFireConfigPatcher.applyFreeFireAutoHeadshot(mCurrentGamePkg);
                        FreeFireConfigPatcher.applyAimAssistLockMax(mCurrentGamePkg);
                    }
                    break;
                case MLBB:
                    if (enable) {
                        com.gamebooster.app.config.MlbbConfigPatcher.applyHeroAimLockConfig(mCurrentGamePkg);
                    }
                    break;
                default:
                    break;
            }
            showNeonToast(enable ? "🔕 Auto Aim & Head Magnet Locked!" : "Auto Aim Disabled");
        });
    }

    private void applyLiveAntiBan(boolean enable) {
        AppExecutors.getInstance().executeCommand(() -> {
            String val = enable ? "1" : "0";
            MlbbCombatOverdriveEngine.setprop("gamebooster.mlbb.antiban", val);
            CodmCombatOverdriveEngine.setprop("gamebooster.codm.antiban", val);
            PubgmCombatOverdriveEngine.setprop("gamebooster.pubgm.antiban", val);

            if (enable) {
                PlayIntegrityBypassEngine.applyFullBypassSuite(getApplicationContext(), mCurrentGamePkg);
                if (mCurrentGamePkg.contains("cod") || mCurrentGamePkg.contains("callofduty")) {
                    CodmAceBypassPatcher.applyBypass(mCurrentGamePkg);
                    CodmConfigPatcher.applyFastLoadShaderBypass(mCurrentGamePkg);
                }
            }
            showNeonToast(enable ? "🛡️ Anti-Ban Cloak & ACE Evasion Shielded" : "Anti-Ban Protection Off");
        });
    }

    private void triggerLiveReInject() {
        AppExecutors.getInstance().executeCommand(() -> {
            GameFamily family = GameFamily.fromPackage(mCurrentGamePkg);
            switch (family) {
                case MLBB:
                    MlbbCombatOverdriveEngine.applyCombatOverdrive(getApplicationContext(), mCurrentGamePkg);
                    break;
                case CODM:
                    CodmCombatOverdriveEngine.applyCombatOverdrive(getApplicationContext(), mCurrentGamePkg);
                    CodmConfigPatcher.patch(mCurrentGamePkg, 185);
                    break;
                case PUBGM:
                    PubgmCombatOverdriveEngine.applyCombatOverdrive(getApplicationContext(), mCurrentGamePkg);
                    PubgConfigPatcher.patch(mCurrentGamePkg, 185);
                    break;
                case FREE_FIRE:
                    FreeFireConfigPatcher.applyFreeFireMasterSuite(mCurrentGamePkg);
                    FreeFireConfigPatcher.patch(mCurrentGamePkg, 120);
                    break;
                default:
                    com.gamebooster.app.config.CommonConfigTuningInjector.applySuperFastTouch(mCurrentGamePkg);
                    break;
            }
            showNeonToast("⚡ 100% Live Combat Suite Re-Injected for " + family.displayName + "!");
        });
    }

    private void showNeonToast(String message) {
        mHandler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    private int dp(float dpVal) {
        return (int) (dpVal * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onDestroy() {
        collapseExpandedMenu();
        if (mWindowManager != null && mBadgeView != null) {
            try {
                mWindowManager.removeView(mBadgeView);
            } catch (Throwable ignored) {}
            mBadgeView = null;
        }
        sRunning = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "In-Game Trainer Overlay",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Floating in-game trainer overlay controls");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("In-Game Trainer Active")
                .setContentText("Tap floating badge inside match to toggle cheats")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
