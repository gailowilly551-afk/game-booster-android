#!/usr/bin/env python3
"""
test_codm_dump_integrity.py — Integrity & Contract Test for CODM APKS Extraction & C# Dump
"""

import os
import sys
import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DUMP_DIR = REPO_ROOT / "dump_codm"
STRINGS_JSON = DUMP_DIR / "codm_extracted_strings.json"
DUMP_CS = DUMP_DIR / "dump.cs"
COMBAT_CS = DUMP_DIR / "codm_combat_dump.cs"
LITERALS_JSON = DUMP_DIR / "stringliteral.json"

REQUIRED_TOKENS = [
    "RecoilScaleWeaponShake",
    "RecoilUpBase",
    "RecoilUpMax",
    "RecoilLateralBase",
    "ShotSpread",
    "AimAssistDis",
    "EnableAimAssistanceForSniper",
    "AimAssistanceSpeed",
    "WeaponController",
    "PlayerController"
]

def test_codm_dump():
    assert DUMP_DIR.exists(), f"Dump directory not found: {DUMP_DIR}"
    assert STRINGS_JSON.exists(), f"Extracted strings json not found: {STRINGS_JSON}"
    assert DUMP_CS.exists(), f"C# dump file not found: {DUMP_CS}"
    assert COMBAT_CS.exists(), f"C# combat dump file not found: {COMBAT_CS}"
    assert LITERALS_JSON.exists(), f"String literals json not found: {LITERALS_JSON}"

    with open(STRINGS_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)

    meta = data.get("metadata", {})
    assert str(meta.get("versionCode")) == "28028", f"Invalid versionCode: {meta.get('versionCode')}"
    assert str(meta.get("uid")) == "10656", f"Invalid uid: {meta.get('uid')}"
    assert meta.get("package") == "com.garena.game.codm", f"Invalid package: {meta.get('package')}"
    assert meta.get("directory_1") == "/data/user/0/com.garena.game.codm", f"Invalid dir1: {meta.get('directory_1')}"
    assert meta.get("directory_2") == "/storage/emulated/0/Android/data/com.garena.game.codm", f"Invalid dir2: {meta.get('directory_2')}"

    by_id = data.get("by_identifier", {})
    for token in REQUIRED_TOKENS:
        assert token in by_id, f"Missing required token in by_identifier: {token}"
        entry = by_id[token]
        assert entry.get("exact_name") == token, f"Token mismatch: {entry}"
        assert entry.get("file_offset") is not None, f"Token offset is None: {token}"

    by_cat = data.get("by_category", {})
    assert len(by_cat) >= 4, f"Too few categories: {len(by_cat)}"
    assert "Recoil" in by_cat
    assert "Spread" in by_cat
    assert "AimAssist" in by_cat

    combat_text = COMBAT_CS.read_text(encoding="utf-8")
    assert "class PlayerController" in combat_text, "PlayerController class missing from C# dump"
    assert "class WeaponFireComponent_Instant" in combat_text, "WeaponFireComponent_Instant class missing from C# dump"
    assert "get_ShotSpread" in combat_text, "get_ShotSpread method missing from C# dump"
    assert "RecoilUpBase" in combat_text, "RecoilUpBase missing from C# dump"
    assert "0x6014791" in combat_text, "Expected method token 0x6014791 missing from C# dump"

    with open(LITERALS_JSON, "r", encoding="utf-8") as f:
        literals = json.load(f)
    assert len(literals) >= 60000, f"Expected >= 60000 string literals, got {len(literals)}"

    print("✅ test_codm_dump_integrity PASSED!")

if __name__ == "__main__":
    try:
        test_codm_dump()
    except AssertionError as e:
        print(f"❌ Assertion Failed: {e}", file=sys.stderr)
        sys.exit(1)
