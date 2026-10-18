package com.gamebooster.app.engine;

import android.content.Context;
import android.util.Log;
import com.gamebooster.app.config.GameProfileAutoConfigurator;
import com.gamebooster.app.device.DevicePerformanceCapabilities;

/**
 * HardwareDisplayHelper — Central helper for querying physical display panel
 * capabilities, supported refresh rate modes, and resolving device-adaptive target FPS
 * (up to 165Hz – 185Hz depending on hardware panel).
 */
public final class HardwareDisplayHelper {

    private static final String TAG = "HardwareDisplayHelper";

    private HardwareDisplayHelper() {}

    /**
     * Resolves the highest physical display panel refresh rate supported by the device hardware.
     * Supported standard tiers: 60Hz, 90Hz, 120Hz, 144Hz, 165Hz, 185Hz, up to 240Hz+.
     */
    public static int getDeviceMaxRefreshRate(Context context) {
        if (context == null) return 60;
        try {
            DevicePerformanceCapabilities caps = DevicePerformanceCapabilities.detect(context);
            int maxHz = caps.getMaxRefreshRate();
            if (maxHz > 0) return maxHz;
        } catch (Throwable t) {
            Log.w(TAG, "Could not detect display caps: " + t.getMessage());
        }
        return 60;
    }

    /**
     * Dynamically clamps and aligns the target game FPS to the physical display capabilities
     * of the device (e.g. 185, 165, 144, 120, 90, 60).
     */
    public static int resolveAdaptiveGameFps(Context context, int requestedFps) {
        return GameProfileAutoConfigurator.clampTargetFpsToDisplay(context, requestedFps);
    }

    /**
     * Returns true if device panel supports high refresh rate (>= 90Hz).
     */
    public static boolean isHighRefreshRateDevice(Context context) {
        return getDeviceMaxRefreshRate(context) >= 90;
    }
}
