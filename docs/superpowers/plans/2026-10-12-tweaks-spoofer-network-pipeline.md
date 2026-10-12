# Full-Stack Tweaks, Device Spoofer & Real Network Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement, harden, and verify the full operational pipelines for Tweaks (`com.gamebooster.app.tweaks`), Device & App Hardware Spoofing (`com.gamebooster.app.spoofer`), and Network Optimization (`NetworkOptimizer` + `SettingsFragment`) on Android 14–16.

**Architecture:** A dual-engine execution architecture leveraging physical Root `resetprop` (when UID 0 is available) and Shizuku Virtual Root (UID 2000) for system-wide and per-game spoofing, paired with Linux kernel TCP BBR sysctl net tuning, complete OS power/saver bypasses, Wi-Fi 6/7 MLO low-latency locks, and DoT Private DNS switching.

**Tech Stack:** Android Java (SDK 36, minSdk 34, Java 17), Linux Kernel sysctl/procfs, Shizuku AIDL / Elevated Shell, Magisk/KernelSU resetprop, JUnit 4, Roboelectric/Mock testing.

**Spec:** [docs/superpowers/specs/2026-10-12-tweaks-spoofer-network-design.md](file:///home/willygailo/Downloads/Game-Launcher/docs/superpowers/specs/2026-10-12-tweaks-spoofer-network-design.md)

---

## Global Constraints

- Android `compileSdk 36`, `minSdk 34`, `sourceCompatibility JavaVersion.VERSION_17`.
- All commands must be non-blocking and execute asynchronously via `AppExecutors.getInstance().executeCommand(...)`.
- Backward compatibility with existing `ManualSettingsPreferences`, `SpoofPreferences`, and `GameManagerRepository`.
- Zero crash policy: all elevated shell commands must contain error suppression (`2>/dev/null` or try/catch blocks).
- Anti-cheat safety: preserve Play Integrity and avoid setting `READ_PHONE_STATE` to `ignore` on Moonton games (`com.mobile.legends`).

## Review Focus

1. **Root `resetprop` execution failure when running on non-root Shizuku:** Gracefully falls back to `persist.sys.device.*` without logging errors or failing the apply.
2. **Moonton / MLBB AppOps security loop:** Setting `READ_PHONE_STATE` to `ignore` on `com.mobile.legends` causes engine crash loops; ensure AppOps shield skips phone state on MLBB.
3. **Private DNS invalid host input:** Passing an empty or malformed hostname to `settings put global private_dns_specifier` leaves the device offline; ensure enum strictly controls hosts.
4. **Network Telemetry timeout in offline mode:** Probing dead IPs hangs UI thread; ensure multi-target socket probes run in background with strict 1200ms timeout.
5. **Concurrent Tweak & Launch shell race condition:** `GameLaunchShellExecutor` batching tweaks while UI applies a tweak must not lock the Shizuku process channel.

---

### Task 1: Tweaks Engine Live Verification & Command Hardening (`com.gamebooster.app.tweaks`)

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/tweaks/TweakItem.java`
- Modify: `android/app/src/main/java/com/gamebooster/app/tweaks/TweakManagerRepository.java`
- Create: `android/app/src/test/java/com/gamebooster/app/tweaks/TweakManagerRepositoryTest.java`

**Interfaces:**
- Consumes: `TweakCategory`, `CommandExecutor.executeSystemCommand`
- Produces: `TweakManagerRepository.isTweakActive(Context, TweakItem)`, `TweakItem.getVerificationProbe()`

- [ ] **Step 1: Write failing unit test for TweakManagerRepository**

Create `android/app/src/test/java/com/gamebooster/app/tweaks/TweakManagerRepositoryTest.java`:
```java
package com.gamebooster.app.tweaks;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class TweakManagerRepositoryTest {

    @Test
    public void testAllTweaksHaveValidCommandsAndRollback() {
        List<TweakItem> all = TweakManagerRepository.getAllTweaks();
        assertNotNull(all);
        assertFalse(all.isEmpty());

        for (TweakItem item : all) {
            assertNotNull("Tweak ID must not be null", item.getId());
            assertFalse("Tweak ID must not be empty", item.getId().trim().isEmpty());
            assertNotNull("Apply command must not be null: " + item.getId(), item.getApplyCommand());
            assertFalse("Apply command must not be empty: " + item.getId(), item.getApplyCommand().trim().isEmpty());
            assertNotNull("Revert command must not be null: " + item.getId(), item.getRevertCommand());
            assertNotNull("Category must not be null: " + item.getId(), item.getCategory());
        }
    }

    @Test
    public void testCategoryMappings() {
        for (TweakCategory cat : TweakCategory.values()) {
            List<TweakItem> items = TweakManagerRepository.getTweaksByCategory(cat);
            assertNotNull(items);
            if (cat != TweakCategory.ALL) {
                for (TweakItem item : items) {
                    assertEquals(cat, item.getCategory());
                }
            }
        }
    }
}
```

- [ ] **Step 2: Run test to verify baseline behavior**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.tweaks.TweakManagerRepositoryTest`  
Expected: PASS or initial baseline check.

- [ ] **Step 3: Implement verification probe extraction in `TweakItem` and `isTweakActive` in `TweakManagerRepository`**

Update `TweakItem.java` to support probe query:
```java
public String getVerificationProbe() {
    if (applyCommand == null) return "";
    String[] parts = applyCommand.split(";");
    for (String p : parts) {
        String trimmed = p.trim();
        if (trimmed.startsWith("setprop ")) {
            String[] tokens = trimmed.split("\\s+");
            if (tokens.length >= 2) return "getprop " + tokens[1];
        } else if (trimmed.startsWith("settings put ")) {
            String[] tokens = trimmed.split("\\s+");
            if (tokens.length >= 4) return "settings get " + tokens[2] + " " + tokens[3];
        }
    }
    return "";
}
```
Add `isTweakActive` in `TweakManagerRepository.java`.

- [ ] **Step 4: Re-run unit tests to verify passes**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.tweaks.TweakManagerRepositoryTest`  
Expected: PASS.

- [ ] **Step 5: Commit changes**

```bash
git add android/app/src/main/java/com/gamebooster/app/tweaks/TweakItem.java \
        android/app/src/main/java/com/gamebooster/app/tweaks/TweakManagerRepository.java \
        android/app/src/test/java/com/gamebooster/app/tweaks/TweakManagerRepositoryTest.java
git commit -m "feat(tweaks): add live tweak verification probes and repository test suite"
```

---

### Task 2: Root `resetprop` System Spoofing & Revert Integration (`com.gamebooster.app.spoofer`)

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/spoofer/HardwareMaskEngine.java`
- Modify: `android/app/src/main/java/com/gamebooster/app/spoofer/DeviceSpooferEngine.java`
- Create: `android/app/src/test/java/com/gamebooster/app/spoofer/DeviceSpooferEngineTest.java`

**Interfaces:**
- Consumes: `SpoofProfile`, `DeviceIdentitySnapshot`, `ShellExecutor.isRootSuAvailable()`
- Produces: `HardwareMaskEngine.buildResetpropCommands(SpoofProfile)`, `HardwareMaskEngine.buildResetpropRestoreCommands(DeviceIdentitySnapshot)`

- [ ] **Step 1: Write failing unit test for resetprop command builder**

Create `android/app/src/test/java/com/gamebooster/app/spoofer/DeviceSpooferEngineTest.java`:
```java
package com.gamebooster.app.spoofer;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class DeviceSpooferEngineTest {

    @Test
    public void testResetpropCommandGeneration() {
        SpoofProfile profile = SpoofProfileRegistry.getById("samsung_s25_ultra");
        assertNotNull(profile);

        List<String> commands = HardwareMaskEngine.buildResetpropCommands(profile);
        assertNotNull(commands);
        assertFalse(commands.isEmpty());

        boolean hasModel = false;
        boolean hasBrand = false;
        boolean hasFingerprint = false;
        for (String cmd : commands) {
            if (cmd.contains("resetprop ro.product.model \"" + profile.model + "\"")) hasModel = true;
            if (cmd.contains("resetprop ro.product.brand \"" + profile.brand + "\"")) hasBrand = true;
            if (cmd.contains("resetprop ro.build.fingerprint \"" + profile.fingerprint + "\"")) hasFingerprint = true;
        }
        assertTrue("Must include ro.product.model resetprop", hasModel);
        assertTrue("Must include ro.product.brand resetprop", hasBrand);
        assertTrue("Must include ro.build.fingerprint resetprop", hasFingerprint);
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.spoofer.DeviceSpooferEngineTest`  
Expected: Compilation failure or missing `buildResetpropCommands` method.

- [ ] **Step 3: Implement `buildResetpropCommands` and root integration in `HardwareMaskEngine`**

In `HardwareMaskEngine.java`:
- Add `public static List<String> buildResetpropCommands(SpoofProfile profile)`
- Add `public static List<String> buildResetpropRestoreCommands(DeviceSpooferEngine.DeviceIdentitySnapshot snap)`
- In `applyFullHardwareMask`: If `ShellExecutor.isRootSuAvailable()`, execute the resetprop batch and `/proc/cpuinfo` mount bind.
- In `restoreReflectionMask`: If `ShellExecutor.isRootSuAvailable()`, restore original properties using `buildResetpropRestoreCommands(snap)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.spoofer.DeviceSpooferEngineTest`  
Expected: PASS.

- [ ] **Step 5: Commit changes**

```bash
git add android/app/src/main/java/com/gamebooster/app/spoofer/HardwareMaskEngine.java \
        android/app/src/main/java/com/gamebooster/app/spoofer/DeviceSpooferEngine.java \
        android/app/src/test/java/com/gamebooster/app/spoofer/DeviceSpooferEngineTest.java
git commit -m "feat(spoofer): integrate root resetprop system property spoofing and rollback"
```

---

### Task 3: Per-Application Game Hardware & Engine Config Injection Verification (`com.gamebooster.app.spoofer`)

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/spoofer/HardwareMaskEngine.java`
- Create: `android/app/src/test/java/com/gamebooster/app/spoofer/GameConfigInjectionTest.java`

**Interfaces:**
- Consumes: `SpoofProfile`, `GameConfigPathResolver`
- Produces: `HardwareMaskEngine.injectTailoredGameHardwareConfigs(packageName, profile)`, `applyAppOpsShieldForPackage(batch, pkg)`

- [ ] **Step 1: Write unit test for game config profile formats and AppOps MLBB safety**

Create `android/app/src/test/java/com/gamebooster/app/spoofer/GameConfigInjectionTest.java`:
```java
package com.gamebooster.app.spoofer;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class GameConfigInjectionTest {

    @Test
    public void testMlbbAppOpsPreservesPhoneState() {
        List<String> mlbbCmds = new ArrayList<>();
        HardwareMaskEngine.applyAppOpsShieldForPackage(mlbbCmds, "com.mobile.legends");

        for (String cmd : mlbbCmds) {
            assertFalse("Must NOT ignore READ_PHONE_STATE on MLBB", cmd.contains("READ_PHONE_STATE ignore"));
            assertFalse("Must NOT ignore READ_DEVICE_IDENTIFIERS on MLBB", cmd.contains("READ_DEVICE_IDENTIFIERS ignore"));
        }

        List<String> codmCmds = new ArrayList<>();
        HardwareMaskEngine.applyAppOpsShieldForPackage(codmCmds, "com.activision.callofduty.shooter");
        boolean hasReadDevId = false;
        for (String cmd : codmCmds) {
            if (cmd.contains("READ_DEVICE_IDENTIFIERS ignore")) hasReadDevId = true;
        }
        assertTrue("Non-MLBB game must shield device identifiers", hasReadDevId);
    }

    @Test
    public void testUe4ProfileKeysFormat() {
        SpoofProfile profile = SpoofProfileRegistry.getById("asus_rog9_pro");
        assertNotNull(profile);
        String[] keys = profile.generateUe4DeviceProfileKeys(185);
        assertNotNull(keys);
        assertTrue(keys.length > 0);

        boolean hasFps = false;
        for (String k : keys) {
            if (k.contains("FrameRateLimit=185")) hasFps = true;
        }
        assertTrue("Must contain FrameRateLimit key", hasFps);
    }
}
```

- [ ] **Step 2: Run test to confirm behavior**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.spoofer.GameConfigInjectionTest`  
Expected: PASS or identifies any AppOps leakage.

- [ ] **Step 3: Harden `injectTailoredGameHardwareConfigs` in `HardwareMaskEngine`**

Ensure that:
1. `UserCustom.ini`, `DeviceProfile.ini`, and `EnjoyCJZC.ini` have verified base64 fallback writes via Shizuku.
2. `HardwareProfile.json` for CODM correctly generates GPU renderer and SoC strings.
3. `DeviceHardware.ini` for MLBB writes valid FPS and GPU strings.
4. `hardware_model_config.json` for Genshin Impact correctly formats JSON.

- [ ] **Step 4: Run test to verify passes**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.spoofer.GameConfigInjectionTest`  
Expected: PASS.

- [ ] **Step 5: Commit changes**

```bash
git add android/app/src/main/java/com/gamebooster/app/spoofer/HardwareMaskEngine.java \
        android/app/src/test/java/com/gamebooster/app/spoofer/GameConfigInjectionTest.java
git commit -m "feat(spoofer): harden per-game engine hardware profile injection and AppOps safety"
```

---

### Task 4: Network Optimizer Kernel TCP, Savers Bypass & Carrier Tuning (`com.gamebooster.app.booster`)

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/booster/NetworkOptimizer.java`
- Create: `android/app/src/test/java/com/gamebooster/app/booster/NetworkOptimizerTest.java`

**Interfaces:**
- Consumes: `Context`, `CommandExecutor`
- Produces: `NetworkOptimizer.disableDataSaver()`, `disableBatterySaver()`, `disableWifiPowerSaver()`, `optimizeTcpBuffers()`, `applyGamingDns(Context, DnsMode)`, `applyPhCarrierOptimization(Context, PhCarrier)`

- [ ] **Step 1: Write failing unit test for NetworkOptimizer enums and logic**

Create `android/app/src/test/java/com/gamebooster/app/booster/NetworkOptimizerTest.java`:
```java
package com.gamebooster.app.booster;

import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkOptimizerTest {

    @Test
    public void testDnsModesAreValid() {
        for (NetworkOptimizer.DnsMode mode : NetworkOptimizer.DnsMode.values()) {
            assertNotNull("DnsMode primary IP must not be null", mode.primary);
            assertNotNull("DnsMode private DNS host must not be null", mode.privateDnsHost);
            assertFalse("Private DNS host must not be empty", mode.privateDnsHost.trim().isEmpty());
        }
    }

    @Test
    public void testPhCarriersAreValid() {
        for (NetworkOptimizer.PhCarrier carrier : NetworkOptimizer.PhCarrier.values()) {
            assertNotNull(carrier.title);
            assertNotNull(carrier.defaultApn);
            assertNotNull(carrier.dnsPrimary);
            assertNotNull(carrier.privateDnsHost);
            assertTrue("MTU must be standard (1400-1500)", carrier.mtu >= 1400 && carrier.mtu <= 1500);
        }
    }

    @Test
    public void testPingStatsQualityClassification() {
        NetworkOptimizer.PingStats excellent = new NetworkOptimizer.PingStats(25, 2, 0, true);
        assertEquals("[EXCELLENT / ULTRA LOW LATENCY]", excellent.quality);

        NetworkOptimizer.PingStats good = new NetworkOptimizer.PingStats(50, 5, 0, true);
        assertEquals("[GOOD / FAST GAMING ROUTE]", good.quality);

        NetworkOptimizer.PingStats timeout = new NetworkOptimizer.PingStats(-1, 0, 100, false);
        assertEquals("[OFFLINE / TIMEOUT]", timeout.quality);
    }
}
```

- [ ] **Step 2: Run test to confirm behavior**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.booster.NetworkOptimizerTest`  
Expected: PASS.

- [ ] **Step 3: Harden `NetworkOptimizer.java` execution paths**

- Ensure `optimizeTcpBuffers()` includes both `sysctl -w` and `/proc/sys/net/ipv4/tcp_congestion_control` fallbacks.
- Ensure `disableDataSaver()`, `disableBatterySaver()`, and `disableWifiPowerSaver()` execute all corresponding system commands cleanly.
- Ensure `applyGamingDns` properly sets `private_dns_mode` and `private_dns_specifier` via `CommandExecutor.setSystemSetting` and fallback shell `settings put global`.

- [ ] **Step 4: Run test to verify passes**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.booster.NetworkOptimizerTest`  
Expected: PASS.

- [ ] **Step 5: Commit changes**

```bash
git add android/app/src/main/java/com/gamebooster/app/booster/NetworkOptimizer.java \
        android/app/src/test/java/com/gamebooster/app/booster/NetworkOptimizerTest.java
git commit -m "feat(network): verify network kernel buffers, savers bypass, and carrier DNS configs"
```

---

### Task 5: SettingsFragment Network UI Wiring & Synchronization (`com.gamebooster.app.ui.fragments`)

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/ui/fragments/SettingsFragment.java`
- Create: `android/app/src/test/java/com/gamebooster/app/ui/SettingsNetworkWiringTest.java`

**Interfaces:**
- Consumes: `NetworkOptimizer`, `ManualSettingsPreferences`, `AppExecutors`
- Produces: Fully functional Card 4 listeners, UI state persistence, live telemetry background refresh

- [ ] **Step 1: Write test for ManualSettingsPreferences network keys**

Create `android/app/src/test/java/com/gamebooster/app/ui/SettingsNetworkWiringTest.java`:
Verify that all getters and setters in `ManualSettingsPreferences` for network toggles exist and store boolean flags accurately.

- [ ] **Step 2: Run test to confirm baseline behavior**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.ui.SettingsNetworkWiringTest`  
Expected: PASS.

- [ ] **Step 3: Verify and audit `SettingsFragment.java` Card 4 listeners**

- Audit all 18 switches in `SettingsFragment`:
  - `switchDisableDataSaver`, `switchDisableBatterySaver`, `switchDisableWifiSaver`, `switchTcpBbrBuffers`, `switch5g6gData`, `switchTntSmartSupercharger`, `switchTmGlobeSupercharger`, `switchWifiLowLatency`, `switchWifi5g6g7g`, `switchDualDataWifi`, `switchDnsCloudflare`, `switchDnsGoogle`, `switchDnsAdguard`, `switchDnsQuad9`, `switchPhTelcoSupercharger`, `switchAutoDnsFlush`, `switchTetheringHw`, `switchForceGnss`.
- Ensure each switch listener:
  1. Checks `checkShizukuOrRevert(buttonView, ...)` when turned on.
  2. Updates `ManualSettingsPreferences`.
  3. Executes underlying `NetworkOptimizer` method via `AppExecutors.getInstance().executeCommand(...)`.
  4. Posts UI toast and telemetry update on main thread.
- Ensure `applyAllNetworkOptimizations()` executes all 10 operations in background without blocking the UI thread.

- [ ] **Step 4: Run test to verify passes**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.ui.SettingsNetworkWiringTest`  
Expected: PASS.

- [ ] **Step 5: Commit changes**

```bash
git add android/app/src/main/java/com/gamebooster/app/ui/fragments/SettingsFragment.java \
        android/app/src/test/java/com/gamebooster/app/ui/SettingsNetworkWiringTest.java
git commit -m "feat(ui): ensure all network settings in SettingsFragment are 100% functional and synchronized"
```

---

### Task 6: Full End-to-End Build & Test Suite Verification

**Files:**
- Execute project-wide test suite across all modules.

- [ ] **Step 1: Run complete test suite**

Run: `./gradlew testDebugUnitTest`  
Expected: All tests pass with 0 failures and 0 compilation errors.

- [ ] **Step 2: Check git diff and workspace status**

Run: `git status`  
Expected: Clean working tree with all commits recorded.

- [ ] **Step 3: Final commit and completion summary**
