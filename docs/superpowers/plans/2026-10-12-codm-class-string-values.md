# Fix CODM Class String Value Names & Offset Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standardize, fix, and integrate verified IL2CPP class string value names, tokens, and method offsets dumped in `android/CODM-LATEST-PATCH` into the Android project's runtime resolver, Frida hook suite, and asset configuration engines.

**Architecture:** A unified metadata pipeline where raw dumped strings from `dump_codm/Metadata/global-metadata.dat` (header `stringOffset` = `0x2a5fac`) are audited, normalized into exact class/field identifiers (correcting fuzzy matches like `IgnoreShotSpreadTime` -> `ShotSpread`, `MonsterDamageAddition` -> `DamageAddition`, `get_CatPlayerController` -> `CatPlayerController`), synchronized with `assets/frida/codm_offsets.json` under `string_patterns`, `tokens`, and `field_offsets`, parsed natively in `CodmIl2cppResolver.java`, and bound to runtime dynamic hooks in `codm_hooks.js`.

**Tech Stack:** Android Java (SDK 36, Java 17), Python 3 (Metadata & JSON ETL), Frida JS Engine, Unity IL2CPP v23 Monolithic Architecture (`libunity.so`), JUnit 4.

**Spec:** Direct requirements from user request to fix class string value names from `/home/willygailo/Downloads/Game-Launcher/android/CODM-LATEST-PATCH` and synchronize across `/home/willygailo/Downloads/Game-Launcher/android`.

## Global Constraints

- Android `compileSdk 36`, `minSdk 34`, `sourceCompatibility JavaVersion.VERSION_17`.
- All JSON schema fields must remain backward-compatible with existing `CodmIl2cppResolver.java`, `CodmModManager.java`, and `CodmConfigPatcher.java` callers.
- No regression or modification to original game APK package signatures.
- Anti-cheat and telemetry symbols (`libanort.so`, `libanogs.so`, `libCrashSight.so`) must remain properly cloaked without crashing game processes.

## Review Focus

- Inaccurate string offsets or fuzzy names in `codm_offsets.json` causing `NullPointerException` or 0 buffer offsets in `CodmIl2cppResolver.java`.
- Endianness / hex decoding failures on token definitions (`0x0601...`).
- Discrepancies between `combat_signatures` and `string_patterns` (e.g. `DamageAddition` buffer offset `0x38686b` vs old fuzzy `0x2ef518`).
- Static initialization safety: calling `CodmIl2cppResolver.getOffset()`, `getToken()`, or `getStringOffset()` before `loadOffsets(Context)` must not return 0 for known default values.
- Frida script syntax and runtime RVA bounds inside `codm_hooks.js`.

---

### Task 1: Normalize & Standardize CODM Patch Metadata Extraction Schema (`android/CODM-LATEST-PATCH`)

**Files:**
- Create: `android/CODM-LATEST-PATCH/test_schema_integrity.py`
- Create: `android/CODM-LATEST-PATCH/normalize_codm_strings.py`
- Create: `android/CODM-LATEST-PATCH/DUMP_VERIFICATION_REPORT.md`
- Modify: `android/CODM-LATEST-PATCH/dump_codm/codm_extracted_strings.json`

**Interfaces:**
- Consumes: Raw extracted string tables and `dump_codm/Metadata/global-metadata.dat`
- Produces: Normalized `codm_extracted_strings.json` mapping each class string to both `by_identifier` and `by_category` structures with exact string matches:
  - `ShotSpread` (`0x546653`, buffer `0x2a06a7`)
  - `AimAssistRot` (`0x55f182`, buffer `0x2b91d6`)
  - `EnableAimAssistanceForSniper` (`0x6297d0`, buffer `0x383824`)
  - `A2DBuffEffect_CauseDamage` (`0x414e7d`, buffer `0x16eed1`)
  - `A2DBuffEffect_DamageIncrease` (`0x414efb`, buffer `0x16ef4f`)
  - `DamageAddition` (`0x62c817`, buffer `0x38686b`)
  - `IsEnemy` (`0x62c397`, buffer `0x3863eb`)
  - `CatPlayerController` (`0x41e60b`, buffer `0x17865f`)

- [ ] **Step 1: Write the failing test / verification script**

Create `android/CODM-LATEST-PATCH/test_schema_integrity.py`:
```python
import json
from pathlib import Path

def test_normalized_structure():
    path = Path("/home/willygailo/Downloads/Game-Launcher/android/CODM-LATEST-PATCH/dump_codm/codm_extracted_strings.json")
    with open(path) as f:
        data = json.load(f)
    assert "by_identifier" in data, "Missing 'by_identifier' top-level key"
    assert "by_category" in data, "Missing 'by_category' top-level key"
    
    # Check that fuzzy keys are fixed to exact strings
    by_id = data["by_identifier"]
    assert by_id["ShotSpread"]["exact_name"] == "ShotSpread", "ShotSpread must be exact string"
    assert by_id["DamageAddition"]["exact_name"] == "DamageAddition", "DamageAddition must be exact string"
    assert by_id["CatPlayerController"]["exact_name"] == "CatPlayerController", "CatPlayerController must be exact string"
    assert by_id["IsEnemy"]["exact_name"] == "IsEnemy", "IsEnemy must be exact string"
    assert by_id["AimAssistRot"]["exact_name"] == "AimAssistRot", "AimAssistRot must be exact string"

if __name__ == "__main__":
    test_normalized_structure()
    print("✅ test_normalized_structure PASSED!")
```

- [ ] **Step 2: Run verification script to confirm current structure fails**

Run: `python3 android/CODM-LATEST-PATCH/test_schema_integrity.py`
Expected: `AssertionError: Missing 'by_identifier' top-level key`

- [ ] **Step 3: Implement `normalize_codm_strings.py` and generate normalized output**

Create `android/CODM-LATEST-PATCH/normalize_codm_strings.py` that:
- Reads `global-metadata.dat` (header `stringOffset = 0x2a5fac`).
- Resolves exact string matches in the metadata string pool.
- Structures entries into:
  - `by_identifier`: keyed by canonical identifier (e.g. `ShotSpread`, `RecoilScaleWeaponShake`, `DamageAddition`).
  - `by_category`: categorized under `AimAssist`, `Recoil`, `Spread`, `CombatDamage`, `Radar`, `PlayerController`, `WeaponController`, `Anticheat`.
- Updates `android/CODM-LATEST-PATCH/dump_codm/codm_extracted_strings.json`.

- [ ] **Step 4: Create `DUMP_VERIFICATION_REPORT.md`**

Document:
- Game version: `Call of Duty: Mobile v1.6.57.0 (arm64-v8a)`
- Metadata details: Unity IL2CPP v23, `stringLiteral` offset `0x108`, `stringOffset` `0x2a5fac`.
- Offset formula: `file_offset = 0x2a5fac + buffer_offset`.
- Complete verification table of corrected identifiers, buffer offsets, and categories.

- [ ] **Step 5: Run `test_schema_integrity.py` to confirm success**

Run: `python3 android/CODM-LATEST-PATCH/test_schema_integrity.py`
Expected: `✅ test_normalized_structure PASSED!`

- [ ] **Step 6: Commit changes**

Commit: `feat(codm): standardize metadata string extraction and verification report`

---

### Task 2: Synchronize & Enrich `codm_offsets.json` in Android Assets (`android/app/src/main/assets/frida/codm_offsets.json`)

**Files:**
- Modify: `android/app/src/main/assets/frida/codm_offsets.json`
- Test: `.superpowers/sdd/2026-10-12-codm-class-string-values/validate_codm_offsets.py`

**Interfaces:**
- Consumes: Verified offsets from `android/CODM-LATEST-PATCH/dump_codm/codm_extracted_strings.json`
- Produces: Updated `codm_offsets.json` with standardized `string_patterns` (`file_offset`, `buffer_offset`, `category`, `identifier`), verified tokens, and aligned `combat_signatures`.

- [ ] **Step 1: Write validation test script**

Create `.superpowers/sdd/2026-10-12-codm-class-string-values/validate_codm_offsets.py`:
```python
import json
from pathlib import Path

def test_codm_offsets_schema():
    path = Path("android/app/src/main/assets/frida/codm_offsets.json")
    with open(path) as f:
        data = json.load(f)
    
    assert "string_patterns" in data, "Missing string_patterns"
    patterns = data["string_patterns"]
    
    for key, item in patterns.items():
        assert "category" in item, f"Key {key} missing category"
        assert "file_offset" in item, f"Key {key} missing file_offset"
        assert "buffer_offset" in item, f"Key {key} missing buffer_offset"
        assert "identifier" in item, f"Key {key} missing identifier"
        
    # Check exact offsets
    assert patterns["ShotSpread"]["buffer_offset"] == "0x2a06a7"
    assert patterns["DamageAddition"]["buffer_offset"] == "0x38686b"
    assert patterns["AimAssistRot"]["buffer_offset"] == "0x2b91d6"
    assert patterns["IsEnemy"]["buffer_offset"] == "0x3863eb"
    assert patterns["CatPlayerController"]["buffer_offset"] == "0x17865f"

if __name__ == "__main__":
    test_codm_offsets_schema()
    print("✅ test_codm_offsets_schema PASSED!")
```

- [ ] **Step 2: Run validation script to confirm failure**

Run: `python3 .superpowers/sdd/2026-10-12-codm-class-string-values/validate_codm_offsets.py`
Expected: `AssertionError: Key RecoilScaleWeaponShake missing category`

- [ ] **Step 3: Update `codm_offsets.json`**

Update `android/app/src/main/assets/frida/codm_offsets.json`:
- Standardize all `string_patterns` entries with:
  `category`, `file_offset`, `buffer_offset`, and `identifier`.
- Update `ShotSpread` to `file_offset: "0x546653"`, `buffer_offset: "0x2a06a7"`.
- Update `AimAssistRot` to `file_offset: "0x55f182"`, `buffer_offset: "0x2b91d6"`.
- Update `EnableAimAssistanceForSniper` to `file_offset: "0x6297d0"`, `buffer_offset: "0x383824"`.
- Update `A2DBuffEffect_CauseDamage` to `file_offset: "0x414e7d"`, `buffer_offset: "0x16eed1"`.
- Update `A2DBuffEffect_DamageIncrease` to `file_offset: "0x414efb"`, `buffer_offset: "0x16ef4f"`.
- Update `DamageAddition` to `file_offset: "0x62c817"`, `buffer_offset: "0x38686b"`.
- Update `IsEnemy` to `file_offset: "0x62c397"`, `buffer_offset: "0x3863eb"`.
- Update `CatPlayerController` to `file_offset: "0x41e60b"`, `buffer_offset: "0x17865f"`.
- Add core class tokens: `WeaponController` (`0x0600519`), `PlayerController` (`0x0600b96f`), `FieldOfView` (`0x06007d49`), `Camera` (`0x06002d24`).
- Ensure `combat_signatures` buffer offsets match `string_patterns` (`DamageAddition`: `38686b`, `A2DBuffEffect_CauseDamage`: `16eed1`).

- [ ] **Step 4: Run validation test script to confirm success**

Run: `python3 .superpowers/sdd/2026-10-12-codm-class-string-values/validate_codm_offsets.py`
Expected: `✅ test_codm_offsets_schema PASSED!`

- [ ] **Step 5: Commit changes**

Commit: `feat(codm): enrich codm_offsets.json with standardized string patterns and verified offsets`

---

### Task 3: Update `CodmIl2cppResolver.java` & Add Unit Tests (`android/app/`)

**Files:**
- Create: `android/app/src/test/java/com/gamebooster/app/mods/codm/CodmIl2cppResolverTest.java`
- Modify: `android/app/src/main/java/com/gamebooster/app/mods/codm/CodmIl2cppResolver.java`

**Interfaces:**
- Method additions to `CodmIl2cppResolver`:
  - `public static long getStringOffset(String key)`
  - `public static String getStringCategory(String key)`
  - `static { initDefaults(); }` (guaranteeing pre-load availability)
  - `sLoadedFromAssets` guard (preventing redundant asset reads)

- [ ] **Step 1: Write failing unit test**

Create `android/app/src/test/java/com/gamebooster/app/mods/codm/CodmIl2cppResolverTest.java`:
```java
package com.gamebooster.app.mods.codm;

import org.junit.Test;
import static org.junit.Assert.*;

public class CodmIl2cppResolverTest {

    @Test
    public void testDefaultOffsetsAndTokensAvailableWithoutContext() {
        long camOffset = CodmIl2cppResolver.getOffset("Camera_get_main");
        assertTrue("Camera_get_main offset should be > 0", camOffset > 0);

        long recoilToken = CodmIl2cppResolver.getToken("RecoilScaleWeaponShake");
        assertTrue("RecoilScaleWeaponShake token should be > 0", recoilToken > 0);
    }

    @Test
    public void testStringOffsetAndCategoryResolution() {
        long shotSpreadBuf = CodmIl2cppResolver.getStringOffset("ShotSpread");
        assertEquals("ShotSpread buffer offset should match verified dump", 0x2a06a7L, shotSpreadBuf);

        String category = CodmIl2cppResolver.getStringCategory("ShotSpread");
        assertEquals("Spread", category);
    }

    @Test
    public void testNonExistentKeyReturnsZero() {
        assertEquals(0L, CodmIl2cppResolver.getOffset("NonExistent_Key"));
        assertEquals(0L, CodmIl2cppResolver.getToken("NonExistent_Token"));
        assertEquals(0L, CodmIl2cppResolver.getStringOffset("NonExistent_String"));
        assertEquals("", CodmIl2cppResolver.getStringCategory("NonExistent_String"));
    }
}
```

- [ ] **Step 2: Run test to confirm failure**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.mods.codm.CodmIl2cppResolverTest`
Expected: Compilation failure or assertion failure (missing `getStringOffset` / `getStringCategory` methods).

- [ ] **Step 3: Implement string resolution and static initialization in `CodmIl2cppResolver.java`**

In `CodmIl2cppResolver.java`:
1. Add `STRING_OFFSET_CACHE` and `STRING_CATEGORY_CACHE`.
2. Add static initializer block:
   ```java
   static {
       initDefaults();
   }
   ```
3. In `loadOffsets(Context ctx)`:
   - Parse `string_patterns` JSONObject:
     extract `buffer_offset` (or `file_offset`), store into `STRING_OFFSET_CACHE`.
     extract `category`, store into `STRING_CATEGORY_CACHE`.
   - Guard with volatile `sLoadedFromAssets`.
4. In `initDefaults()`:
   - Pre-populate verified `STRING_OFFSET_CACHE` and `STRING_CATEGORY_CACHE` for default strings (`ShotSpread`, `RecoilScaleWeaponShake`, `DamageAddition`, `IsEnemy`, `CatPlayerController`, `AimAssistRot`).
5. Add public accessor methods:
   - `public static long getStringOffset(String key)`
   - `public static String getStringCategory(String key)`

- [ ] **Step 4: Run unit test to verify pass**

Run: `./gradlew testDebugUnitTest --tests com.gamebooster.app.mods.codm.CodmIl2cppResolverTest`
Expected: `BUILD SUCCESSFUL` (100% tests passing).

- [ ] **Step 5: Commit changes**

Commit: `feat(codm): add string pattern resolution and unit tests to CodmIl2cppResolver`

---

### Task 4: Harmonize Frida Combat Hooks (`android/app/src/main/assets/frida/codm_hooks.js`)

**Files:**
- Modify: `android/app/src/main/assets/frida/codm_hooks.js`

**Interfaces:**
- Synchronizes `CODM_RVAS`, `FIELD_OFFSETS`, and `combat_signatures` with `codm_offsets.json`.

- [ ] **Step 1: Check existing hook definitions and parameters**

Examine `CODM_RVAS` and `FIELD_OFFSETS` in `codm_hooks.js` around lines 170-191.
Verify alignment with `codm_offsets.json` values.

- [ ] **Step 2: Update string pattern references in hooks**

Ensure combat hooking routines reference normalized string keys:
- `ShotSpread` (`0x2a06a7`)
- `DamageAddition` (`0x38686b`)
- `A2DBuffEffect_CauseDamage` (`0x16eed1`)
- `AimAssistanceColliderRadius` (`0x4e8991`)
- `BulletTrack` (`0x24dfd2`)

- [ ] **Step 3: Validate syntax using Node.js**

Run: `node -c android/app/src/main/assets/frida/codm_hooks.js`
Expected: Zero syntax errors.

- [ ] **Step 4: Commit changes**

Commit: `feat(codm): harmonize Frida combat hooks with normalized string offsets`

---

### Task 5: End-to-End Build & Test Verification

**Files:**
- Verify: Entire `android/` test suite and APK packaging.

- [ ] **Step 1: Run complete unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: All unit tests (`MlbbIl2cppResolverTest`, `CodmIl2cppResolverTest`) pass cleanly.

- [ ] **Step 2: Build release/debug APK**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL` generating `Game_Space_Debug.apk`.

- [ ] **Step 3: Verify asset packaging in APK**

Run: `unzip -l app/build/outputs/apk/debug/Game_Space_Debug.apk | grep codm_offsets.json`
Expected: `assets/frida/codm_offsets.json` is packaged and sized correctly.

- [ ] **Step 4: Commit verification chore**

Commit: `chore(codm): complete end-to-end verification of codm class string value integration`
