#!/usr/bin/env python3
"""
normalize_codm_strings.py — Normalizes and validates extracted string tables
from CODM global-metadata.dat (IL2CPP v23 string pool base 0x2a5fac).
Generates dual-indexed 'by_identifier' and 'by_category' maps.
"""

import json
from pathlib import Path

BASE_DIR = Path("/home/willygailo/Downloads/Game-Launcher/android/CODM-LATEST-PATCH")
METADATA_FILE = BASE_DIR / "dump_codm" / "Metadata" / "global-metadata.dat"
OUTPUT_FILE = BASE_DIR / "dump_codm" / "codm_extracted_strings.json"

STRING_OFFSET_BASE = 0x2a5fac

# Canonical definitions of target class strings & offsets
CANONICAL_ENTRIES = [
    # Recoil
    {
        "identifier": "RecoilScaleWeaponShake",
        "exact_name": "RecoilScaleWeaponShake",
        "category": "Recoil",
        "file_offset": 0x4f20f3
    },
    {
        "identifier": "RecoilUpBase",
        "exact_name": "RecoilUpBase",
        "category": "Recoil",
        "file_offset": 0x4f79a2
    },
    {
        "identifier": "RecoilUpMax",
        "exact_name": "RecoilUpMax",
        "category": "Recoil",
        "file_offset": 0x4f79c0
    },
    {
        "identifier": "RecoilLateralBase",
        "exact_name": "RecoilLateralBase",
        "category": "Recoil",
        "file_offset": 0x4f79cc
    },
    {
        "identifier": "RecoilLateralModifier",
        "exact_name": "RecoilLateralModifier",
        "category": "Recoil",
        "file_offset": 0x4f79de
    },
    {
        "identifier": "RecoilFactor",
        "exact_name": "RecoilFactor",
        "category": "Recoil",
        "file_offset": 0x5a8fdc
    },

    # Spread
    {
        "identifier": "ShotSpread",
        "exact_name": "ShotSpread",
        "category": "Spread",
        "file_offset": 0x546653
    },
    {
        "identifier": "RandomShotSpread",
        "exact_name": "RandomShotSpread",
        "category": "Spread",
        "file_offset": 0x55d613
    },
    {
        "identifier": "CalcShotSpreadSize",
        "exact_name": "CalcShotSpreadSize",
        "category": "Spread",
        "file_offset": 0x55d63a
    },
    {
        "identifier": "ShotSpreadAngle",
        "exact_name": "ShotSpreadAngle",
        "category": "Spread",
        "file_offset": 0x4f8a45
    },
    {
        "identifier": "MinShotSpreadSize",
        "exact_name": "MinShotSpreadSize",
        "category": "Spread",
        "file_offset": 0x4f9bc8
    },
    {
        "identifier": "MaxShotSpreadSize",
        "exact_name": "MaxShotSpreadSize",
        "category": "Spread",
        "file_offset": 0x4f9bda
    },

    # AimAssist
    {
        "identifier": "UseAimAssist",
        "exact_name": "UseAimAssist",
        "category": "AimAssist",
        "file_offset": 0x4f7732
    },
    {
        "identifier": "AimAssistDis",
        "exact_name": "AimAssistDis",
        "category": "AimAssist",
        "file_offset": 0x4f7776
    },
    {
        "identifier": "AimAssistRot",
        "exact_name": "AimAssistRot",
        "category": "AimAssist",
        "file_offset": 0x55f182
    },
    {
        "identifier": "IsOpenAimAssist",
        "exact_name": "IsOpenAimAssist",
        "category": "AimAssist",
        "file_offset": 0x4eabfc
    },
    {
        "identifier": "AimAssistanceSpeed",
        "exact_name": "AimAssistanceSpeed",
        "category": "AimAssist",
        "file_offset": 0x4ea829
    },
    {
        "identifier": "OverrideAimAssistanceSpeed",
        "exact_name": "OverrideAimAssistanceSpeed",
        "category": "AimAssist",
        "file_offset": 0x4fac60
    },
    {
        "identifier": "EnableAimAssistanceForSniper",
        "exact_name": "EnableAimAssistanceForSniper",
        "category": "AimAssist",
        "file_offset": 0x6297d0
    },
    {
        "identifier": "AimAssistanceColliderRadius",
        "exact_name": "AimAssistanceColliderRadius",
        "category": "AimAssist",
        "file_offset": 0x78e93d
    },
    {
        "identifier": "MpIsOpenAimAssistSet",
        "exact_name": "MpIsOpenAimAssistSet",
        "category": "AimAssist",
        "file_offset": 0xab05e4
    },
    {
        "identifier": "BrIsOpenAimAssistSet",
        "exact_name": "BrIsOpenAimAssistSet",
        "category": "AimAssist",
        "file_offset": 0xab0a46
    },

    # CombatDamage
    {
        "identifier": "Headshot",
        "exact_name": "Headshot",
        "category": "CombatDamage",
        "file_offset": 0x59b623
    },
    {
        "identifier": "HeadshotKill",
        "exact_name": "EActvCdtType_HeadshotKill",
        "category": "CombatDamage",
        "file_offset": 0x360ae5
    },
    {
        "identifier": "A2DBuffEffect_CauseDamage",
        "exact_name": "A2DBuffEffect_CauseDamage",
        "category": "CombatDamage",
        "file_offset": 0x414e7d
    },
    {
        "identifier": "A2DBuffEffect_DamageIncrease",
        "exact_name": "A2DBuffEffect_DamageIncrease",
        "category": "CombatDamage",
        "file_offset": 0x414efb
    },
    {
        "identifier": "DamageAddition",
        "exact_name": "DamageAddition",
        "category": "CombatDamage",
        "file_offset": 0x62c817
    },
    {
        "identifier": "DamageAmount",
        "exact_name": "m_DamageAmount",
        "category": "CombatDamage",
        "file_offset": 0xb2a835
    },
    {
        "identifier": "BulletTrack",
        "exact_name": "BasicBulletTrackThickness",
        "category": "CombatDamage",
        "file_offset": 0x4f3f7e
    },
    {
        "identifier": "BasicBulletTrackThickness",
        "exact_name": "BasicBulletTrackThickness",
        "category": "CombatDamage",
        "file_offset": 0x4f3f7e
    },

    # Radar
    {
        "identifier": "TacticalRadarMySelfSprite",
        "exact_name": "TacticalRadarMySelfSprite",
        "category": "Radar",
        "file_offset": 0x41d868
    },
    {
        "identifier": "CreateRadarController",
        "exact_name": "CreateRadarController",
        "category": "Radar",
        "file_offset": 0x41ec22
    },

    # PlayerController
    {
        "identifier": "PlayerController",
        "exact_name": "PlayerController",
        "category": "PlayerController",
        "file_offset": 0x50b96f
    },
    {
        "identifier": "IsEnemy",
        "exact_name": "IsEnemy",
        "category": "PlayerController",
        "file_offset": 0x62c397
    },
    {
        "identifier": "bIsEnemy",
        "exact_name": "bIsEnemy",
        "category": "PlayerController",
        "file_offset": 0x5ce227
    },
    {
        "identifier": "GetTargetEnemy",
        "exact_name": "GetTargetEnemy",
        "category": "PlayerController",
        "file_offset": 0x5300e2
    },
    {
        "identifier": "CatPlayerController",
        "exact_name": "CatPlayerController",
        "category": "PlayerController",
        "file_offset": 0x41e60b
    },
    {
        "identifier": "BRCatGamePlayerController",
        "exact_name": "BRCatGamePlayerController",
        "category": "PlayerController",
        "file_offset": 0x41e76d
    },

    # WeaponController
    {
        "identifier": "WeaponController",
        "exact_name": "WeaponController",
        "category": "WeaponController",
        "file_offset": 0x600519
    },
    {
        "identifier": "GetSelectWeaponControllerType",
        "exact_name": "GetSelectWeaponControllerType",
        "category": "WeaponController",
        "file_offset": 0x442396
    },
    {
        "identifier": "mSelectWeaponController",
        "exact_name": "mSelectWeaponController",
        "category": "WeaponController",
        "file_offset": 0x44268a
    },

    # Camera
    {
        "identifier": "Camera",
        "exact_name": "Camera",
        "category": "Camera",
        "file_offset": 0x2d249b
    },
    {
        "identifier": "FieldOfView",
        "exact_name": "FieldOfView",
        "category": "Camera",
        "file_offset": 0x7d4978
    }
]

def main():
    print(f"[*] Reading global-metadata.dat: {METADATA_FILE}")
    metadata_bytes = METADATA_FILE.read_bytes()

    by_identifier = {}
    by_category = {}

    for entry in CANONICAL_ENTRIES:
        f_off = entry["file_offset"]
        exact_name = entry["exact_name"]
        ident = entry["identifier"]
        cat = entry["category"]

        # Verify string at offset in metadata
        read_str = metadata_bytes[f_off:f_off + len(exact_name) + 2].split(b'\x00')[0].decode('utf-8', errors='ignore')
        if read_str != exact_name:
            print(f"[!] Warning: Mismatch at 0x{f_off:x}: expected '{exact_name}', got '{read_str}'")

        buf_off = f_off - STRING_OFFSET_BASE

        item = {
            "identifier": ident,
            "exact_name": exact_name,
            "category": cat,
            "buffer_offset": f"0x{buf_off:x}",
            "file_offset": f"0x{f_off:x}"
        }

        by_identifier[ident] = item
        by_category.setdefault(cat, []).append(item)

    normalized_data = {
        "metadata_version": 23,
        "string_offset_base": f"0x{STRING_OFFSET_BASE:x}",
        "total_strings": len(by_identifier),
        "by_identifier": by_identifier,
        "by_category": by_category
    }

    OUTPUT_FILE.write_text(json.dumps(normalized_data, indent=2))
    print(f"[✓] Normalized strings written to: {OUTPUT_FILE} ({len(by_identifier)} entries)")

if __name__ == "__main__":
    main()
