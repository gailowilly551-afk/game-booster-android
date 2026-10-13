# MLBB Season 42+ Patch Synchronization & Injection Architecture Design

**Date**: 2026-10-13  
**Status**: Approved by User  
**Target Package**: `com.mobile.legends` (v2.2.16.1232.1 / Season 42+)  
**Source Dumps**: `android/MLBB-LATES-Patch/dump_mlbb/`

---

## 1. Overview & Objectives

This design establishes complete synchronization between the Season 42+ dump artifacts in `android/MLBB-LATES-Patch/` and the runtime injection layers of `Game-Launcher`. It ensures all class names, string literals, method RVAs, and field tokens are 100% accurate, enabling deterministic injection and execution of:
- **Physical Damage & Magic Damage** boost (DPS floor, true damage scaling, `CountHurtValue`)
- **Attack Speed & Movement Speed** boost (`m_AttSpeed`, `m_MoveSpeed`, `MovementComponent_GetSpeed`)
- **Physical Defence & Magic Defence** boost (`m_PhyBaseShield`, `m_MagBaseShield`, `physicalDef`, `magicDef`)
- **All Hero Boost** across all 6 classes (Mage, Assassin, Marksman, Fighter, Tank, Support)
- **Item & Emblem Extra Boost** (`EmblemV4Data`, `GetEmblemLv`, `BattleItem`)
- **Bypass All** (Anti-Cheat reporting, resource integrity check, Fog of War, and download patcher)

---

## 2. Architecture & Subsystems

The implementation spans seven interconnected layers:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   MLBB-LATES-Patch Metadata Dump                       │
│    (global-csharp-metadata.dat, global-metadata.dat, Document XMLs)    │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ extract & normalize
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     Offset & String Manifest                           │
│  - MLBB-LATES-Patch/dump_mlbb/mlbb_extracted_strings.json              │
│  - assets/frida/mlbb_il2cpp_offsets.json                               │
└──────────────┬───────────────────┬─────────────────────┬───────────────┘
               │                   │                     │
               ▼                   ▼                     ▼
┌──────────────────────┐ ┌──────────────────┐ ┌──────────────────────────┐
│   Native C++ Layer   │ │   Frida Dynamic  │ │     Java Orchestration   │
│  mlbb_injector.cpp   │ │   mlbb_hooks.js  │ │  MlbbIl2cppResolver.java │
│  combat_system.cpp   │ │                  │ │  MlbbConfigPatcher.java  │
│                      │ │                  │ │  MlbbModManager.java     │
└──────────────┬───────┘ └─────────┬────────┘ └──────────┬───────────────┘
               │                   │                     │
               ▼                   ▼                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Assets, Lua & Shell Scripts                     │
│  - assets/mlbb_mod/modified/Document/android/BattleConfig.json         │
│  - assets/mlbb_mod/modified/Document/android/HeroStatConfig.json       │
│  - assets/mlbb_drone/tiers/ (1.5x - 10x)                               │
│  - assets/lua/mlbb_boost.lua & assets/lua/mlbb_heroes/ (20 scripts)    │
│  - assets/shell/com.mobile.legends.sh                                  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Component Specifications

### 3.1 Metadata Extraction (`MLBB-LATES-Patch`)
- **Source Files**:
  - `dump_mlbb/Metadata/global-csharp-metadata.dat` (20,090,228 bytes)
  - `dump_mlbb/Metadata/global-metadata.dat` (4,754,024 bytes)
- **Extractor Script**: `android/MLBB-LATES-Patch/extract_combat_metadata.py`
  - Scans and indexes all tokens into `by_identifier` and `by_category` maps.
  - Categories: `PhysicalDamage`, `MagicDamage`, `AttackSpeed`, `MovementSpeed`, `PhysicalDefense`, `MagicDefense`, `HeroStats`, `Emblem`, `BattleItem`, `AntiCheat`, `ResCheck`, `FogOfWar`, `Camera`.
- **Integrity Test**: `android/MLBB-LATES-Patch/test_schema_integrity.py`
  - Asserts required keys exist with valid `file_offset` and `buffer_offset`.

### 3.2 Frida & Offset Manifest (`assets/frida/`)
- **`mlbb_il2cpp_offsets.json`**:
  - Add newly verified method RVAs and field tokens to the manifest.
  - Register target split binaries: `libil2cpp.so`, `libcsharp.so`, `liblogic.so`, `libmoba.so`, `libfirst.so`, `libunity.so`.
  - Add combat floor profiles for all hero classes with verified field names.
- **`mlbb_hooks.js`**:
  - Intercept `LogicHurtCalc.CountHurtValue` to enforce damage amplification and true damage conversion.
  - Intercept `MovementComponent.GetSpeed` to multiply hero movement speed.
  - Intercept `HeroAttackSpeed.GetMultiplier` or `m_AttSpeed` references.
  - Intercept `EmblemV4Data.GetEmblemLv` to set level 60 maximum talent/attribute bonuses.
  - Intercept `Freeze142_AntiCheatReport` and `ResCheckStartHandler` to suppress detections and reporting.

### 3.3 Native C++ Injector (`app/src/main/cpp/`)
- **`mlbb_injector.cpp`**:
  - Update all key-value injections to match real MLBB C# element variables:
    `m_BasePhyAtt`, `m_BaseMagAtt`, `m_AttSpeed`, `m_MoveSpeed`, `m_PhyBaseShield`, `m_MagBaseShield`, `physicalDef`, `magicDef`.
  - Ensure memory patching routines in `nativeInjectMlbbDamage10000AttackSpeedMax`, `nativeInjectMlbbAllHeroMaxDamage2026`, and `nativeInjectMlbbMasterCombatMatrix` target valid RVAs and signature patterns.
- **`il2cpp_direct_scanner.cpp`**:
  - Add pattern scanner definitions for `CountHurtValue` and `HeroAttackSpeed` to support dynamic runtime resolution without static offsets.

### 3.4 Lua Scripts (`assets/lua/`)
- **`mlbb_boost.lua`**:
  - Update combat attributes:
    - Physical Attack: 10,000 floor + 3.5x multiplier
    - Magic Attack: 10,000 floor + 3.5x multiplier
    - Attack Speed: 10.0 cap unlock + zero windup
    - Movement Speed: 520 base + 1.45x sprint scale
    - Physical & Magic Defence: 10,000 floor + 99% damage reduction
    - Emblem & Item boost: 100% max attribute scaling
- **`assets/lua/mlbb_heroes/`** (All 20 dedicated hero combo scripts):
  - Ling, Fanny, Gusion, Chou, Hayabusa, Beatrix, Kagura, Zilong, Saber, Alucard, YSS, Lancelot, Franco, Masha, Brody, Paquito, Joy, Nolan, Arlott, Suyou, Zhuxin.
  - Calibrate hero-specific execution logic with verified stat variables.

### 3.5 Asset Configurations (`assets/mlbb_mod/` & `assets/mlbb_drone/`)
- **`BattleConfig.json` & `HeroStatConfig.json`**:
  - Synchronize JSON schemas with real MLBB property names.
  - Ensure hash consistency with `BinaryPatchMD5.xml` and `ResCheckConf.xml`.
- **Drone View Tiers (`assets/mlbb_drone/tiers/`)**:
  - Verify zoom levels (1.5X, 2X, 3X, 4X, 5X, 10X) against `m_iFieldOfView` and camera elevation tables.

### 3.6 Java Orchestration (`app/src/main/java/com/gamebooster/app/`)
- **`MlbbIl2cppResolver.java`**:
  - Add verified default RVAs, fields, and string tokens for damage, attack speed, movement speed, defense, emblem, and anti-cheat.
  - Update token caches and string offset caches.
- **`MlbbConfigPatcher.java`**:
  - Ensure `deployMlbbAssets()` safely deploys updated `BattleConfig.json`, `HeroStatConfig.json`, and bypass configs.
  - Maintain exclusion of dangerous files (`version.xml`, `GameResAlternative.unity3d`) that cause lobby black screens.
- **`MlbbModManager.java`**:
  - Synchronize property propagation and Frida hook orchestration.

### 3.7 Shell Tuning (`assets/shell/`)
- **`com.mobile.legends.sh`**:
  - Enforce Vulkan render engine (`debug.renderengine.backend vulkan`).
  - Set refresh rate clamps up to 165Hz/185Hz.
  - Overclock touch input buffer to 1000Hz with zero input lag.

---

## 4. Error Handling & Stability Rules

1. **Zero-Crash / No Black Screen Guarantee**: Never touch installed APK `.so` files on disk; rely strictly on in-memory hooks and safe asset hot-patching.
2. **Version Lock Bypass**: Do not overwrite `version.xml` or `realversion.xml` to avoid Moonton server desynchronization.
3. **Safe Fallback**: If Frida is unavailable, the C++ native injector and asset deployer must carry out all combat floors seamlessly.

---

## 5. Verification Plan

1. **Extraction Verification**: Run `extract_combat_metadata.py` and `test_schema_integrity.py` to confirm 100% schema integrity.
2. **Offset JSON Validation**: Verify `assets/frida/mlbb_il2cpp_offsets.json` passes JSON parsing and contains all required tokens.
3. **Java Compilation Check**: Ensure `MlbbIl2cppResolver.java` and `MlbbConfigPatcher.java` compile cleanly without missing symbols.
4. **Lua Syntax Validation**: Validate all 21 Lua files (`mlbb_boost.lua` and 20 hero scripts) syntax.
