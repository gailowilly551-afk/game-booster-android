#!/usr/bin/env python3
"""
test_mlbb_dump_integrity.py — Integrity & Contract Test for MLBB APKS Extraction & C# Dump
"""

import os
import sys
import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DUMP_DIR = REPO_ROOT / "dump_mlbb"
STRINGS_JSON = DUMP_DIR / "mlbb_extracted_strings.json"
DUMP_CS = DUMP_DIR / "dump.cs"
COMBAT_CS = DUMP_DIR / "mlbb_combat_dump.cs"
LITERALS_JSON = DUMP_DIR / "stringliteral.json"

REQUIRED_TOKENS = [
    "CountHurtValue",
    "CalcVal",
    "m_BasePhyAtt",
    "m_BaseMagAtt",
    "m_AttSpeed",
    "m_MoveSpeed",
    "m_PhyBaseShield",
    "m_MagBaseShield",
    "EmblemV4Data",
    "Freeze142_AntiCheatReport",
    "ResCheckStartHandler"
]

def test_mlbb_dump():
    assert DUMP_DIR.exists(), f"Dump directory not found: {DUMP_DIR}"
    assert STRINGS_JSON.exists(), f"Extracted strings json not found: {STRINGS_JSON}"
    assert DUMP_CS.exists(), f"C# dump file not found: {DUMP_CS}"
    assert COMBAT_CS.exists(), f"C# combat dump file not found: {COMBAT_CS}"
    assert LITERALS_JSON.exists(), f"String literals json not found: {LITERALS_JSON}"

    with open(STRINGS_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)

    meta = data.get("metadata", {})
    assert str(meta.get("versionCode")) == "211812322", f"Invalid versionCode: {meta.get('versionCode')}"
    assert str(meta.get("uid")) == "10677", f"Invalid uid: {meta.get('uid')}"
    assert meta.get("package") == "com.mobile.legends", f"Invalid package: {meta.get('package')}"
    assert meta.get("directory_1") == "/data/user/0/com.mobile.legends", f"Invalid dir1: {meta.get('directory_1')}"
    assert meta.get("directory_2") == "/storage/emulated/0/Android/data/com.mobile.legends", f"Invalid dir2: {meta.get('directory_2')}"

    by_id = data.get("by_identifier", {})
    for token in REQUIRED_TOKENS:
        assert token in by_id, f"Missing required token in by_identifier: {token}"
        entry = by_id[token]
        assert entry.get("exact_name") == token, f"Token mismatch: {entry}"
        assert entry.get("file_offset") is not None, f"Token offset is None: {token}"
        assert entry.get("source") != "derived_symbol", f"Token {token} is a fallback derived_symbol!"

    # Verify zero fallbacks across entire dictionary
    derived = [k for k, v in by_id.items() if v.get("source") == "derived_symbol"]
    assert len(derived) == 0, f"Found {len(derived)} fallback derived symbols: {derived}"

    by_cat = data.get("by_category", {})
    assert len(by_cat) >= 8, f"Too few categories: {len(by_cat)}"
    assert "PhysicalDamage" in by_cat
    assert "AntiCheat" in by_cat
    assert "ResCheck" in by_cat

    combat_text = COMBAT_CS.read_text(encoding="utf-8")
    assert "CountHurtValue" in combat_text, "CountHurtValue missing from MLBB C# dump"
    assert "CalcVal" in combat_text, "CalcVal missing from MLBB C# dump"

    with open(LITERALS_JSON, "r", encoding="utf-8") as f:
        literals = json.load(f)
    assert len(literals) >= 50000, f"Expected >= 50000 string literals, got {len(literals)}"

    print("✅ test_mlbb_dump_integrity PASSED (0 Fallbacks, 100% Genuine Dump)!")

if __name__ == "__main__":
    try:
        test_mlbb_dump()
    except AssertionError as e:
        print(f"❌ Assertion Failed: {e}", file=sys.stderr)
        sys.exit(1)
