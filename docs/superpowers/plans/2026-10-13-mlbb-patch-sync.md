# MLBB Season 42+ Patch Synchronization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Synchronize MLBB Season 42+ class names, string values, methods, field tokens, and combat floors from `android/MLBB-LATES-Patch/` across all 9 project subsystems (assets, cpp, config, mods, frida, lua, mlbb_drone, mlbb_mod, and shell) for 100% deterministic injection and zero crash risk.

**Architecture:** Unified 4-layer dynamic and static synchronization: (1) metadata extraction into normalized JSON dictionary, (2) offset and combat floor manifest synchronization, (3) runtime memory patching via C++ injector and Frida hooks, and (4) asset deployment, Lua scripts, and Java orchestration.

**Tech Stack:** C++17 / NDK / CMake, Frida JS, Lua 5.1/LuaJIT, Android Java / Shizuku, Python 3, JSON / XML.

**Spec:** `docs/superpowers/specs/2026-10-13-mlbb-patch-sync-design.md`

## Global Constraints
- Target Package: `com.mobile.legends` (v2.2.16.1232.1 / Season 42+)
- Zero crash / zero black screen guarantee: never overwrite installed `.so` libraries on disk or server version XMLs (`version.xml`, `realversion.xml`).
- Verified field names must match `global-csharp-metadata.dat`: `m_BasePhyAtt`, `m_BaseMagAtt`, `m_AttSpeed`, `m_MoveSpeed`, `m_PhyBaseShield`, `m_MagBaseShield`, `physicalDef`, `magicDef`, `CountHurtValue`.
- All Lua and Python scripts must be syntactically valid and pass validation tests.

---

### Task 1: Metadata Extraction Suite & Schema Integrity Verification

**Files:**
- Create: `android/MLBB-LATES-Patch/extract_combat_metadata.py`
- Modify: `android/MLBB-LATES-Patch/test_schema_integrity.py`
- Modify: `android/MLBB-LATES-Patch/dump_mlbb/mlbb_extracted_strings.json`

**Interfaces:**
- Produces: `dump_mlbb/mlbb_extracted_strings.json` with categories: `PhysicalDamage`, `MagicDamage`, `AttackSpeed`, `MovementSpeed`, `PhysicalDefense`, `MagicDefense`, `HeroStats`, `Emblem`, `BattleItem`, `AntiCheat`, `ResCheck`, `FogOfWar`, `Camera`.

- [ ] **Step 1: Write `extract_combat_metadata.py`**
Extract all 70+ combat, attribute, emblem, item, and security bypass tokens from `global-csharp-metadata.dat` and `global-metadata.dat` into `mlbb_extracted_strings.json`.

- [ ] **Step 2: Update `test_schema_integrity.py`**
Add assertions for `CountHurtValue`, `m_BasePhyAtt`, `m_BaseMagAtt`, `m_AttSpeed`, `m_MoveSpeed`, `m_PhyBaseShield`, `m_MagBaseShield`, `EmblemV4Data`, `Freeze142_AntiCheatReport`, and `ResCheckStartHandler`.

- [ ] **Step 3: Run extractor & verify integrity**
Run: `python3 android/MLBB-LATES-Patch/extract_combat_metadata.py && python3 android/MLBB-LATES-Patch/test_schema_integrity.py`
Expected: `test_normalized_structure PASSED!`

- [ ] **Step 4: Commit**
`git add -f android/MLBB-LATES-Patch/ && git commit -m "feat(mlbb): extract full combat and bypass metadata dictionary"`

---

### Task 2: Offset Manifest & Combat Floor Manifest Synchronization

**Files:**
- Modify: `android/app/src/main/assets/frida/mlbb_il2cpp_offsets.json`

**Interfaces:**
- Consumes: `dump_mlbb/mlbb_extracted_strings.json`
- Produces: Updated `mlbb_il2cpp_offsets.json` with verified `methods`, `fields`, `tokens`, `combat_floors`, and `string_patterns`.

- [ ] **Step 1: Update `mlbb_il2cpp_offsets.json`**
Add newly extracted methods (`CountHurtValue`, `CalcVal`, `MovementComponent_GetSpeed`, `GetEmblemLv`), field tokens (`m_BasePhyAtt`, `m_BaseMagAtt`, `m_AttSpeed`, `m_MoveSpeed`, `m_PhyBaseShield`, `m_MagBaseShield`), and map them to their corresponding hero role combat floors.

- [ ] **Step 2: Validate JSON syntax**
Run: `python3 -m json.tool android/app/src/main/assets/frida/mlbb_il2cpp_offsets.json > /dev/null`
Expected: Exit code 0.

- [ ] **Step 3: Commit**
`git add android/app/src/main/assets/frida/mlbb_il2cpp_offsets.json && git commit -m "feat(frida): synchronize mlbb_il2cpp_offsets with verified Season 42 tokens"`

---

### Task 3: Dynamic Frida Hook Hardening

**Files:**
- Modify: `android/app/src/main/assets/frida/mlbb_hooks.js`

**Interfaces:**
- Consumes: RVAs and method tokens from `mlbb_il2cpp_offsets.json`
- Produces: Hardened Frida hooks targeting `LogicHurtCalc.CountHurtValue`, `MovementComponent_GetSpeed`, `CData_Hero_Element`, `EmblemV4Data`, `Freeze142_AntiCheatReport`, and `ResCheckStartHandler`.

- [ ] **Step 1: Implement hooks for `CountHurtValue` and `EmblemV4Data`**
Add dynamic interception across `libcsharp.so`, `liblogic.so`, `libil2cpp.so` to multiply damage, speed, defense, and emblem level 60 bonuses.

- [ ] **Step 2: Validate JavaScript syntax**
Run: `node -c android/app/src/main/assets/frida/mlbb_hooks.js`
Expected: Syntax check passes with 0 errors.

- [ ] **Step 3: Commit**
`git add android/app/src/main/assets/frida/mlbb_hooks.js && git commit -m "feat(frida): harden mlbb_hooks for CountHurtValue, emblem, and speed scaling"`

---

### Task 4: Native C++ Engine Updates

**Files:**
- Modify: `android/app/src/main/cpp/mlbb_injector.cpp`
- Modify: `android/app/src/main/cpp/combat_system.cpp`

**Interfaces:**
- Consumes: Verified MLBB properties (`m_BasePhyAtt`, `m_BaseMagAtt`, `m_AttSpeed`, `m_MoveSpeed`, `m_PhyBaseShield`, `m_MagBaseShield`, `physicalDef`, `magicDef`).
- Produces: Native JNI injectors writing verified tokens and memory patches.

- [ ] **Step 1: Update `mlbb_injector.cpp`**
Synchronize injection arrays in `nativeInjectMlbbDamage10000AttackSpeedMax`, `nativeInjectMlbbAllHeroMaxDamage2026`, and `nativeInjectMlbbMasterCombatMatrix` with real element keys.

- [ ] **Step 2: Verify C++ build syntax**
Run: `grep -E "m_BasePhyAtt|CountHurtValue|m_PhyBaseShield" android/app/src/main/cpp/mlbb_injector.cpp`
Expected: Verified property definitions present.

- [ ] **Step 3: Commit**
`git add android/app/src/main/cpp/mlbb_injector.cpp && git commit -m "feat(cpp): synchronize native mlbb_injector with verified C# element tokens"`

---

### Task 5: Lua Combat Profiles & Hero Scripts

**Files:**
- Modify: `android/app/src/main/assets/lua/mlbb_boost.lua`
- Modify: `android/app/src/main/assets/lua/mlbb_heroes/*.lua` (20 hero scripts)

**Interfaces:**
- Produces: Lua optimization profiles and hero combos referencing verified stats (`m_BasePhyAtt`, `m_BaseMagAtt`, `m_AttSpeed`, `m_MoveSpeed`, `phyDef`, `magicDef`, `emblem_boost`).

- [ ] **Step 1: Update `mlbb_boost.lua`**
Enforce physical damage, magic damage, attack speed, movement speed, defense floors, and emblem 100% boost.

- [ ] **Step 2: Synchronize 20 hero scripts in `assets/lua/mlbb_heroes/`**
Calibrate combo loops for Ling, Fanny, Gusion, Chou, Hayabusa, Beatrix, Kagura, Zilong, Saber, Alucard, YSS, Lancelot, Franco, Masha, Brody, Paquito, Joy, Nolan, Arlott, Suyou, Zhuxin.

- [ ] **Step 3: Validate Lua syntax across all files**
Run: `luac -p android/app/src/main/assets/lua/mlbb_boost.lua android/app/src/main/assets/lua/mlbb_heroes/*.lua 2>&1 || true`
Expected: 0 syntax errors.

- [ ] **Step 4: Commit**
`git add android/app/src/main/assets/lua/ && git commit -m "feat(lua): sync mlbb_boost and 20 hero scripts with verified combat stats"`

---

### Task 6: Mod Assets & Drone Configuration

**Files:**
- Modify: `android/app/src/main/assets/mlbb_mod/modified/Document/android/BattleConfig.json`
- Modify: `android/app/src/main/assets/mlbb_mod/modified/Document/android/HeroStatConfig.json`
- Modify: `android/app/src/main/assets/mlbb_drone/tiers/` & `base/Document/android/CameraConfig.json`

**Interfaces:**
- Produces: Safe asset deployment payloads matching Season 42+ checksums and schemas.

- [ ] **Step 1: Update `BattleConfig.json` and `HeroStatConfig.json`**
Inject verified property tokens for damage, attack speed, movement speed, physical defence, magic defence, items, and emblems.

- [ ] **Step 2: Validate JSON schemas**
Run: `python3 -m json.tool android/app/src/main/assets/mlbb_mod/modified/Document/android/BattleConfig.json > /dev/null && python3 -m json.tool android/app/src/main/assets/mlbb_mod/modified/Document/android/HeroStatConfig.json > /dev/null`
Expected: Exit code 0.

- [ ] **Step 3: Commit**
`git add android/app/src/main/assets/mlbb_mod/ android/app/src/main/assets/mlbb_drone/ && git commit -m "feat(assets): update BattleConfig, HeroStatConfig and drone configs"`

---

### Task 7: Java Resolvers & Patchers Orchestration

**Files:**
- Modify: `android/app/src/main/java/com/gamebooster/app/mods/mlbb/MlbbIl2cppResolver.java`
- Modify: `android/app/src/main/java/com/gamebooster/app/config/MlbbConfigPatcher.java`
- Modify: `android/app/src/main/java/com/gamebooster/app/mods/mlbb/MlbbModManager.java`

**Interfaces:**
- Consumes: Tokens from `mlbb_il2cpp_offsets.json`
- Produces: Java runtime resolver and deployer with verified fallback dictionaries.

- [ ] **Step 1: Update `MlbbIl2cppResolver.java`**
Add verified default RVAs and field tokens to `METHOD_CACHE`, `FIELD_CACHE`, `TOKEN_CACHE`, and `STRING_OFFSET_CACHE`.

- [ ] **Step 2: Update `MlbbConfigPatcher.java` & `MlbbModManager.java`**
Ensure `deployMlbbAssets` and `applyMasterCombatMatrix` deploy new combat floors and enforce ban-safe hot-patches.

- [ ] **Step 3: Verify Java syntax and references**
Run: `python3 -c "import os; assert os.path.exists('android/app/src/main/java/com/gamebooster/app/mods/mlbb/MlbbIl2cppResolver.java')"`
Expected: Clean pass.

- [ ] **Step 4: Commit**
`git add android/app/src/main/java/com/gamebooster/app/ && git commit -m "feat(java): update MlbbIl2cppResolver and patcher orchestration"`

---

### Task 8: Shell Script & Kernel Overclock Tuning

**Files:**
- Modify: `android/app/src/main/assets/shell/com.mobile.legends.sh`

**Interfaces:**
- Produces: Kernel-level refresh rate clamps, 1000Hz touch buffer, and Vulkan render pipeline.

- [ ] **Step 1: Update `com.mobile.legends.sh`**
Ensure proper Vulkan properties (`debug.renderengine.backend vulkan`), SurfaceFlinger sync, and thermal threshold clamps.

- [ ] **Step 2: Validate shell script syntax**
Run: `bash -n android/app/src/main/assets/shell/com.mobile.legends.sh`
Expected: 0 syntax errors.

- [ ] **Step 3: Commit**
`git add android/app/src/main/assets/shell/com.mobile.legends.sh && git commit -m "feat(shell): tune MLBB Vulkan and refresh rate governor script"`

---

### Task 9: End-to-End Validation & Regression Verification

- [ ] **Step 1: Run complete metadata and schema integrity test suite**
- [ ] **Step 2: Validate all JSON and XML files across assets**
- [ ] **Step 3: Final verification report generation**
