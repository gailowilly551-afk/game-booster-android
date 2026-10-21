package com.gamebooster.app.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import com.gamebooster.app.engine.MemoryManipulationBridge;

import java.util.List;

/**
 * Android Foreground Service managing the ESP Overlay layer.
 * Runs non-touchable pass-through full-screen canvas on top of games.
 */
public class EspOverlayService extends Service {

    private static final String TAG = "EspOverlayService";
    public static final String PREF_NAME = "gamebooster_esp_prefs";
    public static final String KEY_ENABLED = "esp_enabled";
    public static final String KEY_SHOW_BOX = "esp_show_box";
    public static final String KEY_SHOW_NAME = "esp_show_name";
    public static final String KEY_SHOW_HEALTH = "esp_show_health";
    public static final String KEY_SHOW_DISTANCE = "esp_show_distance";
    public static final String KEY_SHOW_SNAPLINE = "esp_show_snapline";
    public static final String KEY_COLOR = "esp_color";

    private static final String CHANNEL_ID = "esp_overlay_channel";
    private static final int NOTIFICATION_ID = 2005;

    private static volatile boolean sRunning = false;
    private WindowManager windowManager;
    private EspOverlayView overlayView;
    private final Handler loopHandler = new Handler(Looper.getMainLooper());
    private boolean isLoopActive = false;

    public static boolean isRunning() {
        return sRunning;
    }

    public static void startOverlay(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, EspOverlayService.class);
        intent.setAction("ACTION_START");
        context.startForegroundService(intent);
    }

    public static void stopOverlay(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, EspOverlayService.class);
        intent.setAction("ACTION_STOP");
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;
        if ("ACTION_STOP".equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification());
        attachOverlay();
        startUpdateLoop();
        sRunning = true;
        return START_STICKY;
    }

    private void attachOverlay() {
        if (overlayView != null) return;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        int layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        int windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                windowFlags,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        overlayView = new EspOverlayView(this);
        applyPrefsToView();

        try {
            windowManager.addView(overlayView, params);
            Log.i(TAG, "ESP Overlay Canvas attached successfully");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to attach ESP overlay: " + t.getMessage(), t);
        }
    }

    private void applyPrefsToView() {
        if (overlayView == null) return;
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean box = sp.getBoolean(KEY_SHOW_BOX, true);
        boolean name = sp.getBoolean(KEY_SHOW_NAME, true);
        boolean health = sp.getBoolean(KEY_SHOW_HEALTH, true);
        boolean dist = sp.getBoolean(KEY_SHOW_DISTANCE, true);
        boolean snapline = sp.getBoolean(KEY_SHOW_SNAPLINE, false);
        int color = sp.getInt(KEY_COLOR, Color.rgb(0, 255, 200));

        overlayView.setOptions(box, name, health, dist, snapline, color);
    }

    private void startUpdateLoop() {
        if (isLoopActive) return;
        isLoopActive = true;

        Runnable tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isLoopActive || overlayView == null) return;

                List<EspEntity> entities = MemoryManipulationBridge.getTrackedEntities();
                overlayView.updateEntities(entities);

                // 60 FPS refresh tick (16ms)
                loopHandler.postDelayed(this, 16);
            }
        };
        loopHandler.post(tickRunnable);
    }

    @Override
    public void onDestroy() {
        isLoopActive = false;
        loopHandler.removeCallbacksAndMessages(null);
        if (windowManager != null && overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Throwable ignored) {}
            overlayView = null;
        }
        sRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "ESP Overlay Display",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Shows real-time entity visual overlay");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GameBooster ESP Overlay Active")
                .setContentText("Rendering visual indicators on active game")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
