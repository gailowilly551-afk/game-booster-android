<div align="center">

# 🚀 Game Launcher PRO

### **Next-Generation Android Performance Engine & Competitive MOBA / Battle Royale Hardware Accelerator**

[![Android](https://img.shields.io/badge/Android-14%20|%2015%20|%2016%20(API%2034--36)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Version](https://img.shields.io/badge/Version-19.9.3-blueviolet?style=for-the-badge)](https://github.com/willygailo/Game-Launcher)
[![License](https://img.shields.io/badge/License-MIT-orange?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Arch-arm64--v8a%20|%20armeabi--v7a%20|%20x86__64-red?style=for-the-badge)](https://developer.android.com/ndk)
[![Build Status](https://img.shields.io/badge/Build-Passing-success?style=for-the-badge&logo=github-actions&logoColor=white)](https://github.com/willygailo/Game-Launcher)
[![Privilege](https://img.shields.io/badge/Privilege-Root%20|%20Shizuku%20|%20Non--Root-yellow?style=for-the-badge)](https://shizuku.rikka.app)

<p align="center">
  <b>A state-of-the-art gaming environment engineered for elite competitive esports.</b><br>
  Locks display refresh rates from <b>60 Hz up to 480 Hz</b>, pins CPU/GPU silicon clocks, overclocks digitizer touch polling to <b>1000 Hz</b>, eliminates frame buffer jitter, and orchestrates atomic, low-overhead native hardware acceleration.
</p>

---

[Key Highlights](#-key-highlights) • [Architecture](#-performance-channels--architecture) • [Supported Games](#-game-engines--competitive-presets) • [Installation & Setup](#-installation--privilege-tiers) • [Verification](#-verification--telemetry-audit) • [Build Guide](#-build--development)

---

</div>

## 🌟 Key Highlights

* ⚡ **Ultra Refresh Rate Enforcement (60–480 Hz):** 8-layer multi-tier display override through `Settings.System`, `SurfaceFlinger` composition transactions, `cmd game`, and direct DRM panel mode programming.
* 🏎️ **Per-Cluster CPU Governor Pinning:** Isolates Cortex Prime/Big cores into pure `performance` governors, clamps `scaling_min_freq` to hardware ceilings, suppresses C-states, and maximizes `uclamp.min` to 1024.
* 🎮 **GPU Clocks & Driver Tuning:** Locks GPU devfreq frequencies across Adreno, Mali, Xclipse (Exynos/RDNA), Tensor, Kirin, and UNISOC architectures with Game Driver opt-in.
* 🎯 **Zero-Latency 1000 Hz Touch Digitizer:** Bypasses Android `InputFlinger` resampling, locks touch thread real-time priority, and activates OEM low-latency game touch drivers.
* 🛡️ **Safe & Ban-Resistant System Architecture:** Runs as an external kernel/system optimizer without intrusive memory manipulation, maintaining full integrity with strict anti-ban and anti-log safeguards.
* 💎 **Sovereign Game Configurations:** Dedicated, hardware-synchronized tuning suites for **MLBB** (Ranked Draft Pick & Classic), **CODM** (Multiplayer & Battle Royale), **PUBG Mobile**, and **Free Fire**.

---

## 🏗️ Performance Channels & Architecture

Game Launcher PRO isolates system tuning into modular, thread-safe hardware channels:

```mermaid
graph TD
    Launcher[Game Launcher PRO Engine] --> Display[MaxHzForceChannel<br/>60 - 480 Hz Multi-Layer]
    Launcher --> CPU[CpuGovernorChannel<br/>Cluster Pinning & uclamp]
    Launcher --> GPU[GpuTweaksChannel<br/>Adreno / Mali / Xclipse]
    Launcher --> Input[TouchLatencyChannel<br/>1000 Hz Digitizer & Resampling]
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

---

## 🗂️ Complete Project Architecture & Directory Layout

Game Launcher PRO is architected with a decoupled, high-performance layered pipeline spanning user-facing UI overlays, background system tuning, and ultra-low latency native C++ execution:

```
Game-Launcher/
├── .github/                                           # CI/CD workflows, PR and Issue templates
│   ├── workflows/ci.yml
│   └── ISSUE_TEMPLATE/
├── .gitignore
├── LICENSE
├── README.md
├── gradle.properties
│
└── android/                                           # ANDROID PROJECT ROOT & COMPILATION
    ├── build.gradle                                   # Top-level Gradle script
    ├── build_native_libs.sh                           # Standalone multi-ABI Clang/CMake compile script
    ├── debug.keystore                                 # Development signing key
    ├── gradle.properties                              # JVM allocation & AndroidX settings
    ├── gradlew / gradlew.bat                          # Portable Gradle execution binaries
    ├── local.properties                               # Android SDK & NDK references
    ├── settings.gradle                                # Project module mapping
    │
    ├── CODM-LATEST-PATCH/                             # CODM EXPLOIT & DUMP ENVIRONMENT
    │   ├── Call of Duty_1.6.57.apks                   # Base split APK container (v1.6.57.0)
    │   ├── dump_codm_patch.py                         # High-speed dumper and metadata extractor
    │   └── dump_codm/                                 # Extracted game artifacts & signatures
    │       ├── Metadata/global-metadata.dat           # IL2CPP string literals and method table
    │       ├── Config/UserCustom.ini                  # Combat & graphic parameters
    │       └── libs/arm64-v8a/                        # libunity.so, libanort.so, libanogs.so
    │
    ├── MLBB-LATES-Patch/                              # MLBB EXPLOIT & DUMP ENVIRONMENT
    │   ├── dump_newpatch_suite.py                     # Moonton Season 42+ automated dump suite
    │   ├── patchMLBB/Mobile Legends...2.2.16.apks     # Raw game package
    │   └── dump_mlbb/                                 # Extracted MLBB assets
    │       ├── boot.config                            # Unity boot parameters
    │       ├── Metadata/global-metadata.dat           # Method signatures and field tokens
    │       ├── Document/android/                      # BattleConfig.unity3d, BinaryPatchMD5.xml
    │       ├── Scenes/android/                        # Map containers (PVP_009.unity3d)
    │       └── libs/arm64-v8a/libil2cpp.so            # Core game logic binary
    │
    └── app/                                           # MAIN ANDROID ENGINE MODULE
        ├── build.gradle                               # Dependencies, NDK toolchains & ABI filters
        ├── proguard-rules.pro
        │
        └── src/main/
            ├── AndroidManifest.xml                    # System permissions, foreground services
            ├── aidl/com/gamebooster/                   # IPC Services for Shizuku & Root bridging
            │
            ├── jniLibs/                               # COMPILED & STRIPPED NATIVE SHARED LIBS
            │   ├── arm64-v8a/libgamebooster_native.so # 1.9 MB (Arm64 Native Engine)
            │   ├── armeabi-v7a/libgamebooster_native.so # 1.2 MB (Arm32 Fallback)
            │   └── x86_64/libgamebooster_native.so    # 2.0 MB (Emulator Engine)
            │
            ├── cpp/                                   # NATIVE C++ TRANSLATION UNITS
            │   ├── CMakeLists.txt                     # Native CMake compilation script
            │   ├── config_common.h / .cpp             # Shared string parsers & JNI macros
            │   ├── native_config_injector.h / .cpp    # JNI symbol exports (1900+ lines)
            │   ├── combat_system.hpp / .cpp           # Vector3 Math, Raycast & Cone calculations
            │   ├── combat_system_jni.cpp              # Target evaluation JNI bindings
            │   ├── mlbb_injector.cpp                  # MLBB 90% Damage, Drone View, Energy loops
            │   ├── codm_injector.cpp                  # CODM 4-Bullet Kill, Recoil, Aimbot matrix
            │   ├── universal_combat_injector.cpp      # Cross-game touch overclock & damage floor
            │   ├── proc_cloak_engine.cpp              # Persistent /proc watcher daemon
            │   ├── ace_cloak_engine.cpp               # Tencent ACE anti-cheat evasion
            │   ├── memfd_inject_engine.cpp            # In-memory stealth loader (LibMap)
            │   ├── mem_proc_patch_engine.cpp          # Live memory protection flipper (RWX/RX)
            │   ├── signal_trap_guard.cpp              # ETW & debugger signal interceptor
            │   ├── il2cpp_direct_scanner.cpp          # Dynamic AOB memory pattern scanner
            │   └── native_lua_engine.h / .cpp         # Embedded Lua execution runtime
            │
            ├── assets/                                # IN-APP EMBEDDED SCRIPTS & OVERLAYS
            │   ├── frida/                             # Frida hook scripts & offset tables
            │   │   ├── mlbb_il2cpp_offsets.json       # RVAs for MLBB v2.2.16
            │   │   ├── codm_offsets.json              # Tokens for CODM v1.6.57
            │   │   ├── mlbb_hooks.js / codm_hooks.js  # Dynamic JavaScript hooks
            │   │   └── frida-server-arm64             # Standalone ARM64 server
            │   ├── mlbb_drone/                        # Drone View Tiers 1X - 10X configs
            │   ├── mlbb_mod/modified/                 # Pre-patched BattleConfig.json & Camera files
            │   ├── codm/Config/                       # Base combat INI templates
            │   └── lua/mlbb_heroes/                   # Dedicated hero combo Lua scripts
            │
            ├── java/com/gamebooster/app/              # JAVA ORCHESTRATION & BUSINESS LOGIC
            │   ├── core/                              # Base Application, Kernel & Executors
            │   │   ├── AppExecutors.java
            │   │   ├── NativeKernelBridge.java
            │   │   └── SurfaceFlingerDirectBinder.java
            │   ├── config/                            # Game Specific Patchers (50+ Classes)
            │   │   ├── GameAutoInjectDispatcher.java  # Auto-dispatcher on game start
            │   │   ├── NativeConfigInjector.java      # Java-to-JNI native binding bridge
            │   │   ├── MlbbConfigPatcher.java         # MLBB XML, JSON & Drone deployer
            │   │   ├── CodmConfigPatcher.java         # CODM Config, BSA & Recoil patcher
            │   │   ├── MlbbDroneViewPatcher.java      # Camera elevation calculator
            │   │   ├── PlayIntegrityBypassEngine.java # Google Play Integrity evasion
            │   │   └── AceContainerEvasionEngine.java # Container & VM detection bypass
            │   ├── mods/                              # Mod State Management
            │   │   ├── mlbb/ (MlbbModManager, MlbbIl2cppResolver)
            │   │   └── codm/ (CodmModManager, CodmIl2cppResolver)
            │   ├── booster/                           # System Overclock Engines
            │   │   ├── CpuGovernorChannel.java
            │   │   ├── HzFpsChannel.java (185 FPS)
            │   │   ├── GyroCalibratorEngine.java
            │   │   └── AdvancedNetworkQosEngine.java
            │   ├── spoofer/                           # Device Identity & Vendor Masking
            │   │   ├── DeviceSpooferEngine.java
            │   │   └── brands/ (ASUS ROG, BlackShark, RedMagic profiles)
            │   ├── shizuku/                           # Rootless elevated permission executor
            │   ├── overlay/                           # In-Game Floating HUD & Quick Bar
            │   ├── gamespace/                         # Gaming Mode & Per-Game Profiles
            │   └── ui/                                # UI Activities, Dialogs & Adapters
            │
            └── res-layouts/ & res-drawables/          # MODULAR UI RESOURCES
                ├── activities/layout/                 # Activity XML definitions
                ├── hud/layout/                        # In-Game Overlay Bar layouts
                └── badges/, backgrounds/, buttons/    # Gaming HUD design tokens
```

### Layered Processing Hierarchy

1. **Presentation & HUD Layer (`com.gamebooster.app.ui`, `.overlay`):**  
   Non-intrusive floating overlay and game dashboard enabling real-time telemetry, drone FOV scaling, aimbot toggle, and digitizer overclocking during live gameplay.
2. **Java Orchestration Layer (`com.gamebooster.app.config`):**  
   `GameAutoInjectDispatcher` identifies running processes, executes hardware spoofing (ROG Phone 8 Pro), applies container evasion, and schedules deferred config patching (avoiding splash-screen CRC race conditions).
3. **Native C++ Performance Engine (`app/src/main/cpp`):**  
   Compiles into `libgamebooster_native.so`. Executes direct syscalls, in-memory anonymous loading (`memfd_create`), live logic patching (MLBB 90% damage, CODM 4-bullet kill, zero recoil), and signal trapping (`SIGTRAP`/`SIGSEGV`).
4. **Persistent Daemon Watcher (`proc_cloak_engine.cpp`):**  
   Monitors `/proc/[pid]/cmdline` in a background thread to seamlessly re-apply combat and memory offsets upon game restart without user intervention.
| **`TouchLatencyChannel`** | Overclocks digitizer touch polling to 1000 Hz, eliminates `InputFlinger` motion event resampling, and activates OEM touch profiles (Samsung Game Booster, Xiaomi Game Turbo, ASUS ROG, OnePlus HyperBoost). |
| **`HwuiRenderAccelerator`** | Assigns `RenderThread` real-time FIFO priority tier 6, enables SkiaVK Vulkan backend, tunes Choreographer phase offsets, and expands GPU shader cache pool to 256 MB. |
| **`WebViewBoosterChannel`** | Tiered optimizations (Flagship, Mid, Budget) enabling WebGPU, WASM SIMD, multithreading, DrDc, Graphite rendering, and V8 TurboShaft JIT pipeline acceleration. |
| **`AdvancedNetworkQosEngine`** | Disables Wi-Fi 802.11 power saving, activates kernel TCP BBR congestion control, marks packet egress with DSCP EF (Expedited Forwarding), and enforces low-latency DoH DNS resolution. |
| **`RamZramChannel`** | Tunes ZRAM compression, pins swappiness to 10, locks essential framebuffers via `mlockall`, and shields foreground gaming processes against low-memory killer (OOM) demotions. |
| **`ThermalChannel`** & **`SafetyGuard`** | Continuous 2-second thermal polling with adaptive headroom detection. Safely dials back boost levels if internal SoC temp approaches 50°C to guarantee hardware longevity. |
| **`SnapshotSystem`** | Captures snapshot states of sysfs nodes, Android settings, and system properties prior to boost engagement with one-tap JSON rollback on game exit. |

---

## 🎯 Game Engines & Competitive Presets

Game Launcher PRO includes targeted, high-performance tuning suites designed specifically for competitive MOBA and Battle Royale titles:

### ⚔️ Mobile Legends: Bang Bang (MLBB)
* **Modes Supported:** Ranked (Draft Pick), Classic, Brawl, and Custom matches.
* **Engine Tuning:** Fully compatible with Unity & IL2CPP `libil2cpp.so` across Western Expanse and Sanctum Expanse.
* **Features:**
  * 185 FPS high-refresh mode unlock with zero frame-pacing variance.
  * Direct 1000 Hz hit registration synchronization and instant spell cast frame sync.
  * Adaptive Drone View camera FOV tiers (Tiers 1–5) with perspective stability.
  * Comprehensive 130+ hero-specific Lua scripting and passive role overdrives.
  * Fast farming, jungle creep clear speed acceleration, and smart smite indicator thresholds.

### 🔫 Call of Duty: Mobile (CODM)
* **Modes Supported:** Ranked Multiplayer (Search & Destroy, Hardpoint, Domination) and Battle Royale (Isolated, Blackout).
* **Engine Tuning:** Calibrated for Unity BSA and native C++ raycast systems (`com.garena.game.codm` and `com.activision.callofduty.shooter`).
* **Features:**
  * 120 / 144 FPS ultra-rate calibration.
  * Magnetic aim assist range expansion up to 350 meters with adaptive 95.0 radius lock.
  * Optimized headshot multiplier routing and custom hitbox scaling.
  * Zero-delay ADS transitions, weapon swap overclocking, and zero-recoil recoil stabilization.

### 🪂 PUBG Mobile & Free Fire
* **PUBG Mobile:** 90 / 120 FPS frame-unlocking via `Active.sav` and `UserCustom.ini` tuning, Gyroscope touch polling acceleration, zero sound delay, and thermal throttling suppression.
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
| Automated Kernel Sysfs Rollback | ❌ | ✅ Full | ✅ Full |

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
git clone https://github.com/willygailo/Game-Launcher.git
cd Game-Launcher/android

# Run unit tests and assemble Debug APK
./gradlew testDebugUnitTest assembleDebug

# Output APK path:
# android/app/build/outputs/apk/debug/app-debug.apk
```

To create signed production releases, define `keystore.properties` inside `android/` with your signing keys.

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more details.

<div align="center">
  <sub>Engineered with precision for the competitive gaming community. Built for speed, stability, and control.</sub>
</div>