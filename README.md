<div align="center">

# 🚀 Game Launcher PRO / Game Booster 2.0

### **Next-Generation Android Performance Engine & Competitive MOBA / Battle Royale Hardware Accelerator**

[![Android](https://img.shields.io/badge/Android-14%20|%2015%20|%2016%20(API%2034--36)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Version-2.0.0--PRO-blueviolet?style=for-the-badge)](https://github.com/Willy-kali/GAME-BOOSTER-0.2)
[![License](https://img.shields.io/badge/License-MIT-orange?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Arch-arm64--v8a%20|%20armeabi--v7a%20|%20x86__64-red?style=for-the-badge)](https://developer.android.com/ndk)
[![Build Status](https://img.shields.io/badge/Build-Passing-success?style=for-the-badge&logo=github-actions&logoColor=white)](https://github.com/Willy-kali/GAME-BOOSTER-0.2)
[![Privilege](https://img.shields.io/badge/Privilege-Root%20|%20Shizuku%20|%20Non--Root-yellow?style=for-the-badge)](https://shizuku.rikka.app)
[![GitHub](https://img.shields.io/badge/GitHub-Willy--kali-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Willy-kali)
[![Facebook](https://img.shields.io/badge/Facebook-Connect-1877F2?style=for-the-badge&logo=facebook&logoColor=white)](https://www.facebook.com/share/14tdhV1h92S/)

<p align="center">
  <b>A state-of-the-art gaming environment engineered for elite competitive esports.</b><br>
  Locks display refresh rates from <b>60 Hz up to 480 Hz</b>, pins CPU/GPU silicon clocks, overclocks digitizer touch polling to <b>1000 Hz</b>, eliminates frame buffer jitter, integrates a live floating in-game trainer overlay, and orchestrates atomic, low-overhead native hardware acceleration.
</p>

---

[Key Highlights](#-key-highlights) • [Architecture](#-performance-channels--architecture) • [Supported Games](#-game-engines--competitive-presets) • [In-Game Trainer Overlay](#-in-game-trainer-floating-overlay) • [Installation & Setup](#-installation--privilege-tiers) • [Shizuku Guide](#-step-by-step-guide-how-to-use-shizuku-with-game-space) • [Verification](#-verification--telemetry-audit) • [Build Guide](#-build--development) • [Developer & Socials](#-developer--socials)

---

</div>

## 🌟 Key Highlights

* ⚡ **Ultra Refresh Rate Enforcement (60–480 Hz):** 8-layer multi-tier display override through `Settings.System`, `SurfaceFlinger` composition transactions, `cmd game`, and direct DRM panel mode programming.
* 🏎️ **Per-Cluster CPU Governor Pinning:** Isolates Cortex Prime/Big cores into pure `performance` governors, clamps `scaling_min_freq` to hardware ceilings, suppresses C-states, and maximizes `uclamp.min` to 1024.
* 🎮 **GPU Clocks & Driver Tuning:** Locks GPU devfreq frequencies across Adreno, Mali, Xclipse (Exynos/RDNA), Tensor, Kirin, and UNISOC architectures with Game Driver opt-in.
* 🎯 **Zero-Latency 1000 Hz Touch Digitizer:** Bypasses Android `InputFlinger` resampling, locks touch thread real-time priority, and activates OEM low-latency game touch drivers.
* 📱 **Floating In-Game Trainer Overlay:** Real-time draggable cyberpunk HUD overlay featuring live toggleable switches for Damage Overdrive, Zero Recoil, Auto Aim, Speed Turbo, Drone FOV, and Anti-Ban Cloak.
* 🛡️ **Streamlined 2-Tab Cyberpunk UI:** Modernized interface featuring pure **HOME** and **SETTINGS** navigation tabs, eliminating legacy mod clutter in favor of automatic, zero-overhead background injection.
* 💎 **Dedicated Non-Overlapping Game Isolation:** Strict separation of hardware masking and native C++ memory injection suites for **Honor of Kings (HOK)**, **MLBB** (Unity IL2CPP), **CODM** (Unity IW8 + ACE), and **PUBGM** (Unreal Engine 4) with zero file or target collision.

---

## 🏗️ Performance Channels & Architecture

Game Launcher PRO isolates system tuning into modular, thread-safe hardware channels:

```mermaid
graph TD
    Launcher[Game Launcher PRO Engine] --> Display[MaxHzForceChannel<br/>60 - 480 Hz Multi-Layer]
    Launcher --> CPU[CpuGovernorChannel<br/>Cluster Pinning & uclamp]
    Launcher --> GPU[GpuTweaksChannel<br/>Adreno / Mali / Xclipse]
    Launcher --> Input[TouchLatencyChannel<br/>1000 Hz Digitizer & Resampling]
    Launcher --> Overlay[InGameTrainerOverlayService<br/>Live HUD & Combat Injector]
    Launcher --> Render[HwuiRenderAccelerator<br/>RenderThread RT FIFO 6 & SkiaVK]
    Launcher --> Net[AdvancedNetworkQosEngine<br/>TCP BBR, DSCP EF, Wi-Fi QTc]
    Launcher --> Memory[RamZramChannel<br/>ZRAM Optimization & OOM Lock]
    Launcher --> Thermal[ThermalChannel & SafetyGuard<br/>Auto-Headroom Throttling]
```

### Detailed Channel Breakdown

| Channel | Core Architecture & Engine Capability |
| :--- | :--- |
| **`HzFpsChannel`** / **`MaxHzForceChannel`** | 8-layer refresh rate lock (60, 90, 120, 144, 165, 185, 240, 300, 360, 480 Hz). Disables Variable Refresh Rate (VRR) flutter, applies per-package Game Mode API commands, and binds panel DRM refresh cycles. |
| **`CpuGovernorChannel`** | Granular per-cluster governors (`performance` on Prime/Big, `powersave` on LITTLE). Clamps `scaling_min_freq = scaling_max_freq`, suppresses C-state idle latency, and tunes CFS / EAS / WALT schedulers. |
| **`GpuTweaksChannel`** | Devfreq clock locking, extended sysfs power governors, GPU preemption control, and Vulkan / OpenGL ES Game Driver system opt-in. |
| **`TouchLatencyChannel`** | Overclocks digitizer touch polling to 1000 Hz, eliminates `InputFlinger` motion event resampling, and activates OEM touch profiles (Samsung Game Booster, Xiaomi Game Turbo, ASUS ROG, OnePlus HyperBoost). |
| **`HwuiRenderAccelerator`** | Assigns `RenderThread` real-time FIFO priority tier 6, enables SkiaVK Vulkan backend, tunes Choreographer phase offsets, and expands GPU shader cache pool to 256 MB. |
| **`AdvancedNetworkQosEngine`** | Disables Wi-Fi 802.11 power saving, activates kernel TCP BBR congestion control, marks packet egress with DSCP EF (Expedited Forwarding), and enforces low-latency DoH DNS resolution. |
| **`RamZramChannel`** | Tunes ZRAM compression, pins swappiness to 10, locks essential framebuffers via `mlockall`, and shields foreground gaming processes against low-memory killer (OOM) demotions. |
| **`ThermalChannel`** & **`SafetyGuard`** | Continuous 2-second thermal polling with adaptive headroom detection. Safely dials back boost levels if internal SoC temp approaches 50°C to guarantee hardware longevity. |

---

## 🎮 In-Game Trainer Floating Overlay

The built-in **InGameTrainerOverlayService** provides an interactive, movable floating badge on screen while running any supported game.

```
┌──────────────────────────────────────────────┐
│  ⚡ HONOR OF KINGS 2026 / MLBB / PUBGM / CODM │
├──────────────────────────────────────────────┤
│  [🦅 FOV & Camera Elevation Overdrive ▾]     │
│  [⚔️ Live Damage Overdrive (10,000 DPS)] [ON]│
│  [⚡ 2.5x Mobility & Movement Speed]    [ON]│
│  [🎯 Zero Recoil & Sway Eliminator]     [ON]│
│  [🔕 Auto Aim & Head Magnet Lock]       [ON]│
│  [🛡️ Anti-Ban Cloak & ACE Evasion]      [ON]│
│  [⚡ Re-Inject Combat Suite Now]             │
└──────────────────────────────────────────────┘
```

- **Adaptive Game Recognition:** Automatically detects foreground package (`com.levelinfinite.sgameGlobal`, `com.mobile.legends`, `com.tencent.ig`, `com.activision.callofduty.shooter`, etc.) and dynamically adjusts feature sets.
- **Dynamic Drone / FOV Control:** Single-tap cycle across MLBB Drone Tiers (1.5x–5x), HOK Ultra Drone FOV, PUBGM iPad 120° Panoramic FOV, and CODM Ultra Tactical FOV.
- **Live Memory & Prop Injector:** Real-time property flipping (`gamebooster.<game>.dmg`, `gamebooster.<game>.speed`, `gamebooster.<game>.antiban`) with zero latency.

---

## 🎯 Game Engines & Competitive Presets

Game Launcher PRO includes targeted, high-performance tuning suites designed specifically for competitive MOBA and Battle Royale titles:

### 👑 Honor of Kings (HOK)
* **Package IDs:** `com.levelinfinite.sgameGlobal`, `com.levelinfinite.sgameGlobal.gpkg`, `com.tencent.tmgp.sgame`
* **Engine Tuning:** Calibrated for Unity `libunity_cp.so`, Burst-generated math kernels (`lib_burst_generated_cp.so`), and Vulkan rendering pipelines.
* **Features:**
  * 120 / 144 / 165 / 185 FPS high-refresh mode unlock.
  * Dedicated **HokCombatOverdriveEngine** injecting 10,000 Damage multiplier & 5.0x Attack Speed.
  * Auto-Smite Objective lock for Abyssal Dragon, Dark Slayer, and Overlord.
  * Instant Spell Cooldown refresh and 2.5x hero mobility turbo.
  * Pre-warmed Vulkan pipeline cache eliminating mid-match frame drops.

### ⚔️ Mobile Legends: Bang Bang (MLBB)
* **Modes Supported:** Ranked (Draft Pick), Classic, Brawl, and Custom matches.
* **Engine Tuning:** Fully compatible with Unity & IL2CPP `libil2cpp.so` across Western Expanse and Sanctum Expanse.
* **Features:**
  * 185 FPS high-refresh mode unlock with zero frame-pacing variance (`HighFPSMode 6`, `HighFPSEnable 1`).
  * Direct 1000 Hz hit registration synchronization and instant spell cast frame sync.
  * Adaptive Drone View camera FOV tiers (Tiers 1.5X–5X) with perspective stability.
  * 130+ hero-specific Lua scripting and passive role overdrives.
  * Fast farming, jungle creep clear speed acceleration, and smart smite indicator thresholds.

### 🔫 Call of Duty: Mobile (CODM)
* **Modes Supported:** Ranked Multiplayer (Search & Destroy, Hardpoint, Domination) and Battle Royale (Isolated, Blackout).
* **Engine Tuning:** Calibrated for Unity BSA and native C++ raycast systems (`com.garena.game.codm` and `com.activision.callofduty.shooter`).
* **Features:**
  * 120 / 144 / 165 FPS ultra-rate calibration with ROG Phone 8 Pro hardware masking.
  * Magnetic aim assist range expansion up to 350 meters with adaptive 95.0 radius lock.
  * Optimized headshot multiplier routing and custom hitbox scaling.
  * Zero-delay ADS transitions, weapon swap overclocking, and zero-recoil stabilization.

### 🪂 PUBG Mobile & Free Fire
* **PUBG Mobile:** 90 / 120 / 165 FPS frame-unlocking via `Active.sav` and `UserCustom.ini` tuning, Gyroscope touch polling acceleration, zero sound delay, and thermal throttling suppression.
* **Free Fire / MAX:** 120 FPS high-rate mode, sensitivity curve linearity normalization, and instant gloo wall deployment response.

---

## 📱 Supported Refresh Rates (Hz Tiers)

Game Launcher PRO interrogates Android's `DisplayManager` and enables extended hardware gaming refresh tiers:

```text
 60 Hz ────>  90 Hz ────> 120 Hz ────> 144 Hz ────> 165 Hz
185 Hz ────> 240 Hz ────> 300 Hz ────> 360 Hz ────> 480 Hz
```

Enforcement pipeline hierarchy:
1. `Settings.System` / `Settings.Global` API (requires `WRITE_SETTINGS` or ADB/Shizuku grant).
2. `MaxHzForceChannel` 8-layer privileged shell execution.
3. Android Game Mode API: `cmd game set --fps <hz> <package_name>`.
4. Low-level panel driver programming via `/sys/class/drm/` and `SurfaceFlinger 1037`.

---

## ⚡ Installation & Privilege Tiers

Game Launcher PRO adapts dynamically to the permissions granted on your Android device:

| Feature Set | Non-Root (Standard) | Shizuku (Wireless ADB) | Root (Magisk / KernelSU / APatch) |
| :--- | :---: | :---: | :---: |
| Game Launching & Library Organization | ✅ Full | ✅ Full | ✅ Full |
| System Display API Refresh Selection | ✅ Basic | ✅ Full | ✅ Full |
| 8-Layer Privileged Hz Enforcement | ❌ | ✅ Full | ✅ Full |
| CPU Governor & Cluster Frequency Locking | ❌ | ⚠️ SchedTune Only | ✅ Full Kernel Clamping |
| GPU Clock & Devfreq Override | ❌ | ⚠️ Vendor Properties | ✅ Full Clamping |
| 1000 Hz Touch Digitizer & Resampling Bypass | ❌ | ✅ Full | ✅ Full |
| Atomic Game Config Tuning | ⚠️ SAF Storage Access | ✅ Elevated Root/Shizuku I/O | ✅ Full Root File I/O |
| Floating In-Game Trainer Overlay | ✅ Full | ✅ Full | ✅ Full |
| Automated Kernel Sysfs Rollback | ❌ | ✅ Full | ✅ Full |

---

## 📱 Step-by-Step Guide: How to Use Shizuku with Game Space

If your device is **not rooted**, you can unlock privileged hardware acceleration using **Shizuku**.

### 📋 Prerequisites
1. **Shizuku App:** Download and install [Shizuku from GitHub](https://github.com/RikkaApps/Shizuku/releases) or the [Google Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api).
2. **Wi-Fi Connection:** Connect your device to any Wi-Fi network (or turn on your phone's personal hotspot).
3. **Developer Options:** Enable Developer Options:
   * Go to `Settings` ➔ `About Phone` ➔ tap **Build Number** 7 times until you see *"You are now a developer!"*.

---

### 🚀 Step-by-Step Instructions

#### 1️⃣ Step 1: Open the Shizuku App
* Launch the **Shizuku** application on your device from the app drawer or home screen.

#### 2️⃣ Step 2: Start Shizuku
* **Enable Wireless Debugging:**
  1. Open your phone's `Settings` ➔ `Developer Options`.
  2. Scroll down to **Wireless Debugging** and toggle it **ON**.
* **Pair with Shizuku:**
  1. Inside Shizuku, tap **Pairing** under *"Start via Wireless debugging"*.
  2. Tap **Developer options**, then tap **Pair device with pairing code**. Note the **6-digit Wi-Fi code**.
  3. Enter the 6-digit code in the Shizuku notification and submit.
* **Start the Shizuku Service:**
  1. Return to Shizuku and tap **Start**.
  2. Confirm status displays **"Shizuku is running"**.

#### 3️⃣ Step 3: Authorize Game Space in Shizuku
1. In Shizuku, tap **Authorized applications**.
2. Locate **Game Space / Game Launcher PRO** (`com.gamebooster.app`).
3. Toggle the switch to **ON**.

#### 4️⃣ Step 4: Launch and Boost Games
1. Open **Game Launcher PRO**.
2. The privilege indicator in the top header will display **⚡ Shizuku Privileged Mode**.
3. Select your game, configure your desired refresh rate and tweaks, and tap **Launch & Boost**!

---

## 🔍 Verification & Telemetry Audit

Verify that your hardware boosts are actively engaged using ADB or a local root terminal:

```bash
# 1. Verify Applied Refresh Rate
adb shell settings get system peak_refresh_rate
adb shell settings get global peak_refresh_rate

# 2. Check Active CPU Governors across all policies
adb shell "for p in /sys/devices/system/cpu/cpufreq/policy*; do echo -n \$p: ; cat \$p/scaling_governor; done"

# 3. Check GPU Devfreq Frequencies (Snapdragon / Adreno example)
adb shell "cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq"
adb shell "cat /sys/class/kgsl/kgsl-3d0/devfreq/min_freq"

# 4. Audit Live Touch Polling Event Stream
adb shell getevent -l /dev/input/event* | grep -i touch

# 5. Measure Real-Time Frame Jitter & Jank Counts
adb shell dumpsys gfxinfo <package_name> framestats
```

---

## 🛠️ Build & Development

### Requirements
- **JDK:** OpenJDK 17 or Temurin 17
- **Android SDK:** `compileSdk 36`, `targetSdk 36`, `minSdk 34` (Android 14 to Android 16)
- **NDK:** `27.0.12077973` (Includes 16 KB page-size ELF segment alignment)
- **Build Tools:** Gradle 8.11.1+

### Compile Commands

```bash
# Clone the repository
git clone https://github.com/Willy-kali/GAME-BOOSTER-0.2.git
cd GAME-BOOSTER-0.2/android

# Build Debug APK directly via Gradle:
./gradlew assembleDebug

# Output APK path:
# android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 👤 Developer & Socials

Developed and maintained by **Willy-kali**. Connect, follow, or contribute:

* 🐙 **GitHub:** [https://github.com/Willy-kali](https://github.com/Willy-kali)
* 📘 **Facebook:** [Connect on Facebook](https://www.facebook.com/share/14tdhV1h92S/)

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more details.

<div align="center">
  <sub>Engineered with precision for the competitive gaming community. Built for speed, stability, and control.</sub>
</div>
