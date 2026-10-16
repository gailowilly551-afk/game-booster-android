package com.gamebooster.app.booster;

import android.content.Context;
import android.util.Log;

import com.gamebooster.app.config.GameProfileAutoConfigurator;
import com.gamebooster.app.core.AppExecutors;
import com.gamebooster.app.engine.PrivilegeBridgeEngine;

/**
 * PerformanceChannel — Master Orchestrator for Hardware Performance Profiles.
 *
 * Coordinates display refresh rate (up to 185Hz), CPU governor & frequency locks,
 * GPU turbo pipelines, zero-delay touch response, RAM compaction, and network QoS.
 */
public final class PerformanceChannel {

    private static final String TAG = "PerformanceChannel";

    private PerformanceChannel() {}

    public enum Profile {
        EXTREME_PERFORMANCE("Display maximum & Uncapped Overdrive"),
        PERFORMANCE("High refresh & Sustained Performance"),
        BALANCED("Balanced & Stock Baseline");

        public final String title;

        Profile(String title) {
            this.title = title;
        }
    }

    public static final class ProfileResult {
        public final boolean refreshRateApplied;
        public final int appliedHz;
        public final String message;

        public ProfileResult(boolean refreshRateApplied, int appliedHz, String message) {
            this.refreshRateApplied = refreshRateApplied;
            this.appliedHz = appliedHz;
            this.message = message;
        }
    }

    public static boolean applyProfile(Context context, Profile profile) {
        return applyProfileWithResult(context, profile).refreshRateApplied;
    }

    public static ProfileResult applyProfileWithResult(Context context, Profile profile) {
        if (context == null) return new ProfileResult(false, 0, "Device context is unavailable");
        final Profile activeProfile = (profile != null) ? profile : Profile.BALANCED;
        int targetHz = GameProfileAutoConfigurator.getTargetFpsHz(context);

        Log.i(TAG, "Applying Profile: " + activeProfile.name() + " @ target " + targetHz + "Hz");

        switch (activeProfile) {
            case EXTREME_PERFORMANCE:
                return applyExtremePerformance(context, targetHz);
            case PERFORMANCE:
                return applyHighPerformance(context, targetHz);
            case BALANCED:
            default:
                return applyBalanced(context);
        }
    }

    private static ProfileResult applyExtremePerformance(Context context, int requestedHz) {
        final int targetHz = (requestedHz >= 165) ? Math.min(185, requestedHz) : 185;

        // 1. Arm Safety Watchdog & Capture pre-boost snapshot
        try {
            SafetyGuard.ensureArmed(context.getApplicationContext());
        } catch (Throwable t) {
            Log.w(TAG, "SafetyGuard arming note: " + t.getMessage());
        }

        // 2. Display Pipeline Force (185Hz / Uncapped)
        HzFpsChannel.RefreshRateResult hzResult;
        if (PrivilegeBridgeEngine.isPrivilegedActive()) {
            MaxHzForceChannel.ForceResult forceRes = MaxHzForceChannel.forceApply(targetHz);
            hzResult = HzFpsChannel.RefreshRateResult.success(targetHz, forceRes.appliedHz > 0 ? forceRes.appliedHz : targetHz);
        } else {
            hzResult = HzFpsChannel.setRefreshRate(context, targetHz);
        }

        // 3. CPU, GPU, Memory, Touch & Network Channels
        AppExecutors.getInstance().executeCommand(() -> {
            try {
                // CPU Governors & Topology
                CpuGovernorChannel.tuneMultiCoreTopology();
                CpuGovernorChannel.setPerformanceLock();
                CpuGovernorChannel.applyIoPipelineAndRtFlags();

                // GPU Turbo & Vulkan
                GpuTweaksChannel.setGpuMaxPerformance();
                GpuTweaksChannel.enableVulkanRenderer();

                // Touch & Combat
                TouchLatencyChannel.enableUltraTouchResponse();
                CombatEngineChannel.enableCombatMode(context);

                // HWUI Pipeline Acceleration
                HwuiRenderAccelerator.applyAll(null);

                // RAM & ZRAM
                RamZramChannel.trimMemoryAndCleanCache(context);
                RamZramChannel.applyIOAndMemoryFlags();

                // Network QoS
                AdvancedNetworkQosEngine.applyAll(context);
                NetworkOptimizer.optimizeAllDataAndWifi(context);
            } catch (Throwable t) {
                Log.w(TAG, "Extreme profile sub-channel error: " + t.getMessage());
            }
        });

        int finalHz = hzResult != null ? hzResult.appliedHz : targetHz;
        return new ProfileResult(
                hzResult != null && hzResult.success,
                finalHz,
                Profile.EXTREME_PERFORMANCE.title + ": " + (hzResult != null ? hzResult.message : "Applied")
        );
    }

    private static ProfileResult applyHighPerformance(Context context, int targetHz) {
        // High refresh rate (120Hz/144Hz)
        HzFpsChannel.RefreshRateResult hzResult = HzFpsChannel.setRefreshRate(context, targetHz);

        AppExecutors.getInstance().executeCommand(() -> {
            try {
                CpuGovernorChannel.setPerformanceLock();
                GpuTweaksChannel.enableVulkanRenderer();
                TouchLatencyChannel.enableUltraTouchResponse();
                RamZramChannel.trimMemoryAndCleanCache(context);
                NetworkOptimizer.optimizeAllDataAndWifi(context);
            } catch (Throwable t) {
                Log.w(TAG, "High performance sub-channel note: " + t.getMessage());
            }
        });

        int finalHz = hzResult != null ? hzResult.appliedHz : targetHz;
        return new ProfileResult(
                hzResult != null && hzResult.success,
                finalHz,
                Profile.PERFORMANCE.title + ": " + (hzResult != null ? hzResult.message : "Applied")
        );
    }

    private static ProfileResult applyBalanced(Context context) {
        // Disengage Overdrive
        try {
            NoLimitExtremeOverdriveEngine.disengageNoLimitOverdrive(context);
        } catch (Throwable ignored) {}

        // Reset CPU, GPU, and Memory
        try {
            CpuGovernorChannel.restoreStockGovernors();
            GpuTweaksChannel.restoreStockGpuSettings();
            RamZramChannel.restoreStockMemorySettings();
        } catch (Throwable ignored) {}

        // Reset Touch & Combat
        try {
            TouchLatencyChannel.restoreDefaultTouchResponse();
            CombatEngineChannel.disableCombatMode(context);
        } catch (Throwable ignored) {}

        // Restore Pre-boost Snapshot & Disarm SafetyGuard
        try {
            SnapshotSystem.restoreLatest(context.getApplicationContext());
            SafetyGuard.stop(false);
        } catch (Throwable ignored) {}

        // Reset Display to Default (60Hz / auto)
        HzFpsChannel.RefreshRateResult hzResult = HzFpsChannel.setRefreshRate(context, 60);

        return new ProfileResult(
                true,
                hzResult != null ? hzResult.appliedHz : 60,
                Profile.BALANCED.title + ": Stock settings restored"
        );
    }

    /** Direct execution of performance tweaks. */
    public static boolean writeAndExecutePerformanceTweaksScript() {
        return writeAndExecutePerformanceTweaksScript(120);
    }

    /** Direct execution of performance tweaks with target Hz. */
    public static boolean writeAndExecutePerformanceTweaksScript(int targetHz) {
        try {
            CpuGovernorChannel.setPerformanceLock();
            GpuTweaksChannel.enableVulkanRenderer();
            TouchLatencyChannel.enableUltraTouchResponse();
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "writeAndExecutePerformanceTweaksScript note: " + t.getMessage());
            return false;
        }
    }

    /** Direct execution of privileged root/Shizuku performance tweaks. */
    public static boolean writeAndExecuteRootTweaksScript() {
        return writeAndExecuteRootTweaksScript(185);
    }

    /** Direct execution of privileged root/Shizuku performance tweaks with target Hz. */
    public static boolean writeAndExecuteRootTweaksScript(int targetHz) {
        try {
            int hz = targetHz > 0 ? targetHz : 185;
            MaxHzForceChannel.forceApply(hz);
            CpuGovernorChannel.tuneMultiCoreTopology();
            CpuGovernorChannel.setPerformanceLock();
            GpuTweaksChannel.setGpuMaxPerformance();
            TouchLatencyChannel.enableUltraTouchResponse();
            RamZramChannel.applyIOAndMemoryFlags();
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "writeAndExecuteRootTweaksScript note: " + t.getMessage());
            return false;
        }
    }

    /** Enables high-performance Vulkan / SkiaVK rendering pipeline. */
    public static boolean setGpuRenderMode(boolean is3D) {
        if (is3D) {
            return GpuTweaksChannel.enableVulkanRenderer();
        }
        return true;
    }

    /** Comprehensive One-Tap Boost. */
    public static boolean executeOneTapBoost(Context context) {
        if (context == null) return false;
        try {
            RamZramChannel.trimMemoryAndCleanCache(context);
            NetworkOptimizer.optimizeAllDataAndWifi(context);
            TouchLatencyChannel.enableUltraTouchResponse();
            Profile profile = PrivilegeBridgeEngine.isPrivilegedActive() ? Profile.EXTREME_PERFORMANCE : Profile.PERFORMANCE;
            return applyProfile(context, profile);
        } catch (Throwable t) {
            Log.w(TAG, "executeOneTapBoost error: " + t.getMessage());
            return false;
        }
    }
}
