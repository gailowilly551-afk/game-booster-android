# Non-Rooted Device Full Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure 100% full functionality of Game-Launcher on non-rooted Android devices (Android 10 to 16) by eliminating all hardcoded `su` dependencies and routing all privileged operations through Shizuku (UID 2000), Rish, and unprivileged shell fallbacks.

**Architecture:** Replace legacy raw `su` command invocations with the centralized `CommandExecutor` and `PrivilegeBridgeEngine` virtual root pipeline. This transparently bridges ADB shell (UID 2000) capabilities to system tweaks, FPS unlocks, display refresh rate configuration, and scoped storage file operations via `ShizukuFileManager`.

**Tech Stack:** Android (Java), Shizuku AIDL (UID 2000), Rish binary, Scoped Storage bypass, Android Shell API, JUnit 4.

**Spec:** [docs/superpowers/specs/2026-10-13-mlbb-patch-sync-design.md](file:///home/willygailo/Downloads/Game-Launcher/docs/superpowers/specs/2026-10-13-mlbb-patch-sync-design.md) & [Everything.md](file:///home/willygailo/Downloads/Everything.md)

## Global Constraints
- Non-rooted Android devices (Android 10 - 16 / API 29 - 36) must never crash or hang on missing `su` binary.
- All file operations in `/sdcard/Android/data/` and `/sdcard/Android/obb/` on non-rooted devices must route through `ShizukuFileManager`.
- System settings and display refresh rates (120Hz/144Hz/165Hz) must execute via Shizuku elevated shell (UID 2000) `settings put` and `cmd game`.
- Code changes must preserve backward compatibility for rooted devices (`su` UID 0) while guaranteeing flawless execution on non-rooted devices.
- All existing JUnit unit tests must pass.
- Gradle build `./gradlew assembleDebug` must compile cleanly with 0 errors.

## Review Focus
1. Non-rooted device with no `su` binary installed: `Runtime.getRuntime().exec("su")` throws `IOException`. Must be intercepted and handled via `CommandExecutor` without exception.
2. Shizuku granted via Wireless Debugging: Command execution succeeds via UID 2000 without requiring root superuser prompts.
3. Shizuku not yet started / running in background: Engine falls back to unprivileged shell (`sh`) for standard commands without crashing.
4. Android 13-16 scoped storage lock: Game config patching uses `ShizukuFileManager` to write into `/sdcard/Android/data/`.
5. Display & FPS unlock: `enforceDisplayRefreshRate()` applies `settings put system` via Shizuku shell.

---

### Task 1: Refactor `GameModEngine.java` for Safe Non-Root Execution

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/mods/GameModEngine.java:230-260,318-345`
- Test: `android/app/src/test/java/com/gamebooster/app/engine/NonRootedEngineTest.java`

- [ ] Inspect lines 318–345 of `GameModEngine.java` containing `Runtime.getRuntime().exec(new String[]{"su", "-c", cmd})`.
- [ ] Replace `exec(String cmd)` to invoke `com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd)`.
- [ ] Replace `execOutput(String cmd)` to invoke `com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd)` and trim clean non-error output.
- [ ] Update `injectFrida()` to use `CommandExecutor.executeSystemCommand()` for process and binary discovery instead of raw Shizuku calls.
- [ ] Verify no unhandled `su` exceptions occur when running on non-rooted environments.

---

### Task 2: Refactor `MlbbModManager.java` for Non-Root Virtual Root Execution

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/mods/mlbb/MlbbModManager.java:175-195,210-225`
- Test: `android/app/src/test/java/com/gamebooster/app/mods/mlbb/MlbbIl2cppResolverTest.java`

- [ ] Inspect lines 213–223 of `MlbbModManager.java` containing fallback to `Runtime.getRuntime().exec("su")`.
- [ ] Replace `exec(String cmd)` with `com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd)`.
- [ ] Update `injectFrida()` binary lookup to use `CommandExecutor.executeSystemCommand()`.
- [ ] Ensure MLBB 4-layer injection operates smoothly through Shizuku (UID 2000) and `ShizukuFileManager`.

---

### Task 3: Refactor `CodmModManager.java` and `CodmSecurityCloak.java`

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/mods/codm/CodmModManager.java:170-190,205-220`
- Modify: `android/app/src/main/java/com/gamebooster/app/mods/codm/CodmSecurityCloak.java:55-68`
- Test: `android/app/src/test/java/com/gamebooster/app/mods/codm/CodmIl2cppResolverTest.java`

- [ ] Inspect lines 209–219 of `CodmModManager.java` containing fallback to `Runtime.getRuntime().exec("su")`.
- [ ] Replace `exec(String cmd)` with `com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd)`.
- [ ] Update `injectFrida()` binary lookup in `CodmModManager.java` to use `CommandExecutor.executeSystemCommand()`.
- [ ] In `CodmSecurityCloak.java`, replace lines 57–67 `exec(String cmd)` with `com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd)`.
- [ ] Ensure CODM 5-layer injection and security cloaking operate smoothly without root.

---

### Task 4: Refactor `CodmAceBypassPatcher.java`

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/config/CodmAceBypassPatcher.java:55-67`
- Test: `android/app/src/test/java/com/gamebooster/app/mods/codm/CodmIl2cppResolverTest.java`

- [ ] Inspect lines 56–66 of `CodmAceBypassPatcher.java` containing fallback to `Runtime.getRuntime().exec("su")`.
- [ ] Replace `exec(String cmd)` with `com.gamebooster.app.engine.CommandExecutor.executeSystemCommand(cmd)`.
- [ ] Verify ACE folder suppression runs safely through Shizuku shell on non-rooted devices.

---

### Task 5: Add Non-Rooted Privilege & Execution Unit Tests

**Files:**
- Create: `android/app/src/test/java/com/gamebooster/app/engine/NonRootedEngineTest.java`

- [ ] Create test cases for `PrivilegeBridgeEngine.unwrapSuCommand()` verifying:
  - `"su -c 'settings put system peak_refresh_rate 120'"` -> `"settings put system peak_refresh_rate 120"`.
  - `"sudo rm -rf /data/local/tmp/test"` -> `"rm -rf /data/local/tmp/test"`.
  - `"su"` -> `"sh"`.
- [ ] Create test case for `CommandExecutor.getActiveEngineMode()` verifying fallback modes (`SHIZUKU` or `SYSTEM_SETTINGS`) when root is absent.
- [ ] Create test case for `CommandExecutor.isSuccessOutput()` verifying detection of failure keywords and error strings.
- [ ] Run `./gradlew testDebugUnitTest` and ensure all tests pass.

---

### Task 6: End-to-End Build & Validation

**Files:**
- Test execution: `./gradlew testDebugUnitTest`
- Build execution: `./gradlew assembleDebug`

- [ ] Run full JUnit test suite to ensure 0 regressions across all tests.
- [ ] Build release/debug APK with `./gradlew assembleDebug`.
- [ ] Verify clean APK compilation and packaging.
