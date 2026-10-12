# CODM v1.6.57 IL2CPP Dump & String Verification Report

- **Target Game:** Call of Duty: Mobile (`com.garena.game.codm` / `com.activision.callofduty.shooter`)
- **Version:** `1.6.57.0` (Build 2026 update)
- **Architecture:** `arm64-v8a`
- **Engine Type:** Monolithic Unity IL2CPP runtime embedded in `libunity.so`
- **Metadata Version:** IL2CPP Metadata v23 (`0xfab11baf`)
- **Metadata Base String Pool:** `0x2a5fac` (`stringOffset`)
- **Extraction Timestamp:** 2026-10-12

---

## 1. Metadata String Pool Architecture

In Unity IL2CPP v23, strings are stored within `global-metadata.dat`.
- **String Offset Header:** `0x2a5fac`
- **Buffer Offset Formula:** `buffer_offset = file_offset - 0x2a5fac`
- **File Offset Formula:** `file_offset = 0x2a5fac + buffer_offset`

---

## 2. Verified Class Strings & Corrections

| Canonical Identifier | Previous Fuzzy Name | Verified Exact String | Category | File Offset | Buffer Offset |
|---|---|---|---|---|---|
| `ShotSpread` | `IgnoreShotSpreadTime` | `ShotSpread` | Spread | `0x546653` | `0x2a06a7` |
| `AimAssistRot` | `get_AimAssistRot` | `AimAssistRot` | AimAssist | `0x55f182` | `0x2b91d6` |
| `EnableAimAssistanceForSniper` | `get_EnableAimAssistanceForSniper` | `EnableAimAssistanceForSniper` | AimAssist | `0x6297d0` | `0x383824` |
| `A2DBuffEffect_CauseDamage` | `A2DBuffEffect_CauseDamageBy...` | `A2DBuffEffect_CauseDamage` | CombatDamage | `0x414e7d` | `0x16eed1` |
| `A2DBuffEffect_DamageIncrease` | `A2DBuffEffect_DamageIncrease_...` | `A2DBuffEffect_DamageIncrease` | CombatDamage | `0x414efb` | `0x16ef4f` |
| `DamageAddition` | `MonsterDamageAddition` | `DamageAddition` | CombatDamage | `0x62c817` | `0x38686b` |
| `IsEnemy` | `IsEnemyDead` | `IsEnemy` | PlayerController | `0x62c397` | `0x3863eb` |
| `CatPlayerController` | `get_CatPlayerController` | `CatPlayerController` | PlayerController | `0x41e60b` | `0x17865f` |
| `WeaponController` | *(missing)* | `WeaponController` | WeaponController | `0x600519` | `0x35a56d` |
| `PlayerController` | *(missing)* | `PlayerController` | PlayerController | `0x50b96f` | `0x2659c3` |
| `FieldOfView` | *(missing)* | `FieldOfView` | Camera | `0x7d4978` | `0x52e9cc` |
| `Camera` | *(missing)* | `Camera` | Camera | `0x2d249b` | `0x2c4ef` |
| `RecoilFactor` | *(missing)* | `RecoilFactor` | Recoil | `0x5a8fdc` | `0x303030` |

---

## 3. Runtime Integration Verification

All 43 string values are indexed into:
- `by_identifier`: Direct dictionary lookups for Android runtime resolvers (`CodmIl2cppResolver.java`).
- `by_category`: Categorized array mappings for mod engines and Frida combat injectors (`codm_hooks.js`).
