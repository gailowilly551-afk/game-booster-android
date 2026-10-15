#!/usr/bin/env python3
"""
test_codm_dump_integrity.py — Integrity & Contract Test for CODM APKS Extraction
"""

import os
import sys
import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DUMP_DIR = REPO_ROOT / "dump_codm"
STRINGS_JSON = DUMP_DIR / "codm_extracted_strings.json"

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

    print("✅ test_codm_dump_integrity PASSED!")

if __name__ == "__main__":
    try:
        test_codm_dump()
    except AssertionError as e:
        print(f"❌ Assertion Failed: {e}", file=sys.stderr)
        sys.exit(1)
