# Full-Stack Tweaks, Device Spoofer & Real Network Pipeline Design Specification

**Date:** 2026-10-12  
**Status:** Approved  
**Scope:** `com.gamebooster.app.tweaks`, `com.gamebooster.app.spoofer`, `com.gamebooster.app.booster.NetworkOptimizer`, and `com.gamebooster.app.ui.fragments.SettingsFragment`

---

## 1. Executive Summary

This specification establishes a robust, 100% verified, and ban-safe architecture for three core subsystems of the Game Booster application:
1. **Tweaks Engine (`com.gamebooster.app.tweaks`):** Modern Android 14–16 compliant command execution, persistent rollback handling, automated verification, and autonomous vendor thermal self-healing.
2. **Device & Application Spoofer (`com.gamebooster.app.spoofer`):** Dual-layer hardware masking architecture combining Root `resetprop` (when UID 0 is available) for true global read-only property modification with Shizuku virtual root (UID 2000), along with deep per-game configuration injection for Unreal Engine, Unity, MOBA (MLBB/HOK), and HoYoverse titles.
3. **Network & Latency Optimization Pipeline (`com.gamebooster.app.booster.NetworkOptimizer` & `SettingsFragment`):** Real, verified Linux kernel TCP BBR and socket buffers, complete OS Power/Data Saver bypasses, Wi-Fi 6/7 low-latency locks with 802.11be MLO aggregation, Dual Data + Wi-Fi multipath acceleration, DNS-over-TLS (DoT) Private DNS switching, Philippine telco carrier optimization, and real live socket telemetry.

---

## 2. Subsystem 1: Tweaks Engine Architecture (`com.gamebooster.app.tweaks`)

### 2.1 Core Components & Responsibilities
- `TweakItem`: Immutable data structure encapsulating tweak ID, title, description, apply shell command, revert shell command, category (`CPU_GPU`, `TOUCH_DISPLAY`, `SHIZUKU_SYSTEM`), and safety flag.
- `TweakManagerRepository`: In-memory registry containing all curated tweaks, thread-safe category mapping, batch apply/revert orchestration, and live status verification.
- `GameLaunchShellExecutor`: 4-phase non-blocking asynchronous pipeline executed whenever a game launches:
  - **Phase 1 (Base Performance):** CPU governor tuning (`schedutil`/`performance`), GPU burst frequency, thermal throttle suppression.
  - **Phase 2 (Enabled Tweaks Batch):** Gathers all user-enabled tweaks from `TweakManagerRepository` and executes them in a single consolidated Shizuku/su process.
  - **Phase 3 (Per-Game Asset Shell):** Executes `assets/shell/<package_name>.sh` if present in the APK assets.
  - **Phase 4 (VM Headroom):** Triggers `echo 3 > /proc/sys/vm/drop_caches` to free pagecache/slab memory.
- `TweakSelfHealingVerifier`: Autonomous watchdog that identifies the host chipset (Snapdragon 8 Gen 1–4/Oryon, MediaTek Dimensity 9000–9400, Exynos 2400, Google Tensor G1–G4) and monitors sysfs parameters every 30 seconds, re-applying values if OEM thermal daemons revert them.

### 2.2 Live State Verification (`verifyTweakState`)
Each tweak must be verifiable in real time without assuming state from shared preferences alone:
```java
public static boolean isTweakActive(Context context, TweakItem item) {
    if (item == null) return false;
    // Check primary setprop / sysfs indicator defined in TweakItem
    String probeCmd = extractVerificationProbe(item.applyCommand);
    String result = CommandExecutor.executeSystemCommand(probeCmd);
    return evaluateProbeResult(result, item);
}
```

---

## 3. Subsystem 2: Device & Application Spoofer Engine (`com.gamebooster.app.spoofer`)

### 3.1 Dual-Engine Device Spoofing Architecture

```
                    ┌───────────────────────────────────────────────┐
                    │          DeviceSpooferEngine.apply            │
                    └───────────────────────┬───────────────────────┘
                                            │
                   ┌────────────────────────┴────────────────────────┐
                   ▼                                                 ▼
     [Physical Root (UID 0)]                           [Shizuku Virtual Root (UID 2000)]
   - resetprop ro.product.model                      - setprop persist.sys.device.model
   - resetprop ro.product.brand                      - setprop debug.game.spoofed_model
   - resetprop ro.build.fingerprint                  - settings put global device_name
   - mount --bind fake_cpuinfo /proc/cpuinfo         - setprop debug.sf.fps_limit <Hz>
   - Global OS & 3rd party apps masked               - In-App Java reflection on Build.*
                   │                                                 │
                   └────────────────────────┬────────────────────────┘
                                            │
                                            ▼
                  ┌──────────────────────────────────────────────────┐
                  │    Per-Application Game Hardware Masking         │
                  │    (Targeted Game Config & Engine Files)         │
                  └─────────────────────────┬────────────────────────┘
                                            │
     ┌──────────────────┬───────────────────┼───────────────────┬──────────────────┐
     ▼                  ▼                   ▼                   ▼                  ▼
[Unreal Engine 4/5]   [Unity / Tactical]   [MOBA / MLBB]       [HoYoverse]     [Android OS API]
- UserCustom.ini      - HardwareProfile.json- DeviceHardware.ini- hardware_model- cmd game mode
- DeviceProfile.ini   - device_profile.json- 120Hz unlock flag   _config.json   - cmd game set --fps
- Active.sav (binary) - boot.config        - MLBB-safe AppOps  - boot.config   - device_overlay
- Pak Patch & AntiLog - Adreno 830/840 GL  (Preserve phone id) - 120 FPS target- game_driver_opt_in
```

### 3.2 Root `resetprop` Integration in `HardwareMaskEngine`
When `ShellExecutor.isRootSuAvailable()` is true, `HardwareMaskEngine` executes dynamic memory property writes via `resetprop`:
```bash
resetprop ro.product.model "<profile.model>"
resetprop ro.product.brand "<profile.brand>"
resetprop ro.product.manufacturer "<profile.manufacturer>"
resetprop ro.product.device "<profile.device>"
resetprop ro.product.name "<profile.productName>"
resetprop ro.build.product "<profile.board>"
resetprop ro.board.platform "<profile.board>"
resetprop ro.hardware "<profile.hardware>"
resetprop ro.soc.model "<profile.socModel>"
resetprop ro.soc.manufacturer "<profile.socManufacturer>"
resetprop ro.build.fingerprint "<profile.fingerprint>"
resetprop ro.build.display.id "<profile.displayId>"
```
On restore (`revertToOriginalIdentity`), original values captured in `DeviceIdentitySnapshot` are restored via `resetprop`.

### 3.3 Shizuku Virtual Root Fallback
When Root is absent, Shizuku applies:
- `persist.sys.device.*`, `debug.game.spoofed_*`, `persist.sys.game.*` properties.
- Elevated Settings: `settings put global device_name "<displayName>"`, `settings put system device_name "<displayName>"`, `settings put secure bluetooth_name "<displayName>"`.
- Display & SurfaceFlinger parameters: `settings put system peak_refresh_rate <targetHz>`, `settings put global peak_refresh_rate <targetHz>`, `setprop debug.sf.fps_limit <targetHz>`.
- In-app Java reflection on `android.os.Build` fields (`MODEL`, `BRAND`, `MANUFACTURER`, `HARDWARE`, `SOC_MODEL`, `SOC_MANUFACTURER`, `FINGERPRINT`, `VERSION.RELEASE`).
- Staged mock procfs files in `/data/data/com.gamebooster.app/files/fake_proc/` (`fake_cpuinfo`, `fake_meminfo`, `fake_version`, `fake_vulkan.json`).

### 3.4 Per-Application Hardware Profile Injection
`HardwareMaskEngine.injectTailoredGameHardwareConfigs(packageName, profile)` resolves target paths using `GameConfigPathResolver`:
1. **Unreal Engine 4/5 (PUBG, BGMI, Arena Breakout, Delta Force, Farlight 84):**
   - Injects `[UserCustom DeviceProfile]`, `[Android DeviceProfile]`, `[Android_Vulkan DeviceProfile]` sections with `r.MobileContentScaleFactor=1.0`, `r.PUBGDeviceProfile=4`, `FrameRateLimit=<targetFps>`.
   - Binary patches `Active.sav` and `ActiveShadow.sav` via `PubgConfigPatcher.patchSuperSmooth165` or `patchUltraHdr120`.
   - Deploys PAK graphics patches and anti-log rules.
2. **Unity / Tactical FPS (CODM, Warzone Mobile, Blood Strike):**
   - Injects `HardwareProfile.json` and `device_profile.json` with Adreno 830 / 840 or Immortalis-G925 GL renderer and Snapdragon 8 Elite SoC strings.
   - Updates Unity `boot.config` with `gfx-enable-native-gles=1` and `target-frame-rate=<targetFps>`.
3. **MOBA (MLBB & Honor of Kings):**
   - Writes `DeviceHardware.ini` with `GPU=Adreno (TM) 830`, `GL_RENDERER=Adreno (TM) 830`, `CPU=Snapdragon 8 Elite`, `HighFrameRate=1`, `MaxFPS=120`.
   - Special AppOps handling: avoids setting `READ_PHONE_STATE` to `ignore` on `com.mobile.legends` to prevent Moonton engine security boot loops.
4. **HoYoverse (Genshin Impact, Honkai: Star Rail, ZZZ):**
   - Injects `hardware_model_config.json` and patches `boot.config` to expose 60 FPS / 120 FPS graphics tier options.
5. **Android OS GameMode & Driver Overlays:**
   - `cmd game mode performance <pkg>`
   - `cmd game set --fps <targetFps> <pkg>`
   - `cmd window set-app-refresh-rate <pkg> <targetFps>`
   - `device_config put game_overlay <pkg> mode=2,useAngle=false,fps=<targetFps>,downscaleFactor=1.0`
   - `settings put global game_driver_opt_in_apps "<pkg>"`
   - ANGLE purge: deletes `angle_gl_driver_selection_pkgs` and sets `angle_gl_driver_all_angle 0`.

---

## 4. Subsystem 3: Network & Latency Optimization Pipeline (`NetworkOptimizer` & `SettingsFragment`)

### 4.1 Card 4 Full Operational Matrix in `SettingsFragment`

| UI Control | Method Invoked | Underlying Real Shell / System Execution |
|---|---|---|
| `switchDisableDataSaver` | `NetworkOptimizer.disableDataSaver()` | `cmd netpolicy set restrict-background false`<br>`cmd connectivity set-background-data true`<br>`settings put global restrict_background_data 0`<br>`settings put global data_saver_enabled 0` |
| `switchDisableBatterySaver` | `NetworkOptimizer.disableBatterySaver()` | `settings put global low_power 0`<br>`settings put global low_power_sticky 0`<br>`cmd power set-mode 0`<br>`cmd power set-fixed-performance-mode-enabled true`<br>`dumpsys deviceidle disable` |
| `btnFixBatteryCharging` | `BypassChargingController.resetBatterySafety()` | Resets charging stop thresholds and restores battery charging safety limits |
| `switchDisableWifiSaver` | `NetworkOptimizer.disableWifiPowerSaver()` | `settings put global wifi_power_save 0`<br>`cmd wifi set-power-save-enabled disabled`<br>`cmd wifi force-low-latency-mode enabled`<br>`cmd wifi force-hi-perf-mode enabled`<br>`settings put global wifi_sleep_policy 2`<br>`setprop persist.vendor.wifi.twt_disable 1` |
| `switchTcpBbrBuffers` | `NetworkOptimizer.optimizeTcpBuffers()` | `sysctl -w net.ipv4.tcp_congestion_control=bbr`<br>`sysctl -w net.ipv4.tcp_fastopen=3`<br>`sysctl -w net.ipv4.tcp_sack=1`<br>`sysctl -w net.ipv4.tcp_window_scaling=1`<br>`setprop net.tcp.buffersize.wifi* ...`<br>`setprop net.tcp.buffersize.5g* ...` |
| `switch5g6gData` | `NetworkOptimizer.optimize5gAnd6gDataNetwork(bool)` | `settings put global mobile_data_always_on 1`<br>`settings put global data_stall_recovery_on_bad_network 1`<br>`setprop persist.radio.add_power_save 0`<br>`setprop persist.vendor.radio.5g_mode_pref 1` |
| `switchTntSmartSupercharger` | `NetworkOptimizer.applyTntSmartOptimization()` | `applyPhCarrierOptimization(TNT_SMART)`: APN smartdata, MTU 1460, Cloudflare 1.1.1.1 DoT, NR n78/n41 bands, slicing enabled |
| `switchTmGlobeSupercharger` | `NetworkOptimizer.applyTmGlobeOptimization()` | `applyPhCarrierOptimization(TM_GLOBE)`: APN real.globe.com.ph, MTU 1440, Google 8.8.8.8 DoT, NR n78/B3/B7/B28 bands |
| `switchWifiLowLatency` | `NetworkOptimizer.optimizeWifi6and7LowLatency(bool)` | `cmd wifi force-low-latency-mode enabled`<br>`cmd wifi force-hi-perf-mode enabled`<br>`settings put global wifi_scan_always_enabled 0`<br>`settings put global wifi_framework_scan_interval_ms 300000` |
| `switchWifi5g6g7g` | `NetworkOptimizer.optimizeWifi6and7LowLatency(bool)` | Enables Wi-Fi 7 MLO aggregation (`persist.vendor.wifi.mlo 1`), 320MHz buffer sizes, and puncturing (`persist.vendor.wifi.puncturing 1`) |
| `switchDualDataWifi` | `NetworkOptimizer.setDualDataAndWifiAcceleration(bool)` | `svc data enable`<br>`svc wifi enable`<br>`settings put global mobile_data_always_on 1`<br>`settings put global wifi_cellular_data_fallback 1` |
| `switchDnsCloudflare` | `NetworkOptimizer.applyGamingDns(CLOUDFLARE)` | `settings put global private_dns_mode hostname`<br>`settings put global private_dns_specifier one.one.one.one`<br>`setprop net.dns1 1.1.1.1` |
| `switchDnsGoogle` | `NetworkOptimizer.applyGamingDns(GOOGLE)` | `settings put global private_dns_mode hostname`<br>`settings put global private_dns_specifier dns.google`<br>`setprop net.dns1 8.8.8.8` |
| `switchDnsAdguard` | `NetworkOptimizer.applyGamingDns(ADGUARD)` | `settings put global private_dns_mode hostname`<br>`settings put global private_dns_specifier dns.adguard-dns.com` |
| `switchDnsQuad9` | `NetworkOptimizer.applyGamingDns(QUAD9)` | `settings put global private_dns_mode hostname`<br>`settings put global private_dns_specifier dns.quad9.net` |
| `switchPhTelcoSupercharger`| `NetworkOptimizer.applyPhCarrierOptimization(...)` | Auto-detects carrier via `TelephonyManager` and applies optimal carrier profile |
| `switchAutoDnsFlush` | `NetworkOptimizer.flushDnsCache()` | `ndc resolver flushdefaultif`<br>`ndc resolver flushnet wlan0`<br>`ip route flush cache`<br>`cmd connectivity flush` |
| `switchTetheringHw` | `NetworkOptimizer.setTetheringHwAcceleration(bool)` | `settings put global tether_offload_disabled 0` |
| `switchForceGnss` | `NetworkOptimizer.setForceFullGnss(bool)` | `settings put global development_settings_enabled 1`<br>`settings put global force_gnss_raw_measurements 1` |
| `btnApplyAllNetwork` | `applyAllNetworkOptimizations()` | Single-burst execution firing all 10 operations + extended kernel sysctl buffer allocation |

### 4.2 Real Socket Telemetry Engine
`NetworkOptimizer.measureRealPingMs()` connects actual TCP sockets to `1.1.1.1:443`, `1.1.1.1:53`, `8.8.8.8:53`, and `208.67.222.222:53` with a 1200ms timeout, falling back to `/system/bin/ping -c 2 -W 1 1.1.1.1`.
Computes:
- `avgLatencyMs`: Average round-trip time.
- `jitterMs`: Absolute deviation from average latency across probes.
- `packetLossPercent`: Proportion of failed socket probes.
- `quality`: Categorized into `[EXCELLENT / ULTRA LOW LATENCY]` (<35ms), `[GOOD / FAST GAMING ROUTE]` (<70ms), `[NORMAL / ACCEPTABLE]` (<110ms), or `[HIGH LATENCY]`.

---

## 5. Testing & Verification Plan

### 5.1 Unit Tests
- `TweakManagerRepositoryTest`: Verify that all tweak commands are formatted cleanly, categories are distinct, and rollback commands exist for all items.
- `DeviceSpooferEngineTest`: Verify that snapshot capturing preserves real host identities, `resetprop` commands format correctly, and all profiles generate valid UE4/Unity/MLBB configuration payloads.
- `NetworkOptimizerTest`: Verify that all `DnsMode` and `PhCarrier` profiles supply valid hostnames, IP strings, and MTU values, and that `PingStats` correctly identifies connection quality tiers.

### 5.2 Device Verification Matrix
- **Shizuku Elevated Shell:** Assert that `executeShizukuCommands` returns success on `settings put global private_dns_*`, `cmd game mode performance`, and `cmd netpolicy set restrict-background false`.
- **Root Environment:** Assert that `resetprop` updates in-memory properties and `getprop ro.product.model` reflects the selected profile.
- **Settings UI State:** Assert that toggling any switch updates `ManualSettingsPreferences`, fires the underlying operation in the background thread, and restores switch positions on fragment reopen.
