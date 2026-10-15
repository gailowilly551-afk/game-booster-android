#!/usr/bin/env python3
"""
verify_all_patches.py — Project-wide Comprehensive Verification Suite
Validates:
  1. Dump integrity for MLBB & CODM
  2. VersionCode & UID alignments (MLBB: 211812322 / 10677, CODM: 28028 / 10656)
  3. Dual directory bindings (/data/user/0/ and /storage/emulated/0/Android/data/)
  4. Permanent zero-revert injection mechanisms (chattr +i, chmod 444, touch -t 203001010000)
  5. C++ ioctl and JNI native bindings
  6. Frida hooks and shell deployment scripts
"""

import os
import sys
import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

def test_mlbb_dump_metadata():
    json_path = REPO_ROOT / "dump_mlbb" / "mlbb_extracted_strings.json"
    assert json_path.exists(), f"MLBB strings JSON missing: {json_path}"
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    meta = data.get("metadata", {})
    assert str(meta.get("versionCode")) == "211812322", f"MLBB versionCode mismatch: {meta.get('versionCode')}"
    assert str(meta.get("uid")) == "10677", f"MLBB uid mismatch: {meta.get('uid')}"
    assert meta.get("package") == "com.mobile.legends", f"MLBB package mismatch: {meta.get('package')}"
    assert meta.get("directory_1") == "/data/user/0/com.mobile.legends", f"MLBB dir1 mismatch: {meta.get('directory_1')}"
    assert meta.get("directory_2") == "/storage/emulated/0/Android/data/com.mobile.legends", f"MLBB dir2 mismatch: {meta.get('directory_2')}"
    print("  [✓] MLBB Dump & Metadata verified.")

def test_codm_dump_metadata():
    json_path = REPO_ROOT / "dump_codm" / "codm_extracted_strings.json"
    assert json_path.exists(), f"CODM strings JSON missing: {json_path}"
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    meta = data.get("metadata", {})
    assert str(meta.get("versionCode")) == "28028", f"CODM versionCode mismatch: {meta.get('versionCode')}"
    assert str(meta.get("uid")) == "10656", f"CODM uid mismatch: {meta.get('uid')}"
    assert meta.get("package") == "com.garena.game.codm", f"CODM package mismatch: {meta.get('package')}"
    assert meta.get("directory_1") == "/data/user/0/com.garena.game.codm", f"CODM dir1 mismatch: {meta.get('directory_1')}"
    assert meta.get("directory_2") == "/storage/emulated/0/Android/data/com.garena.game.codm", f"CODM dir2 mismatch: {meta.get('directory_2')}"
    print("  [✓] CODM Dump & Metadata verified.")

def test_java_patchers():
    mlbb_java = (REPO_ROOT / "app/src/main/java/com/gamebooster/app/config/MlbbConfigPatcher.java").read_text("utf-8")
    assert "TARGET_VERSION_CODE = 211812322" in mlbb_java
    assert "TARGET_UID = 10677" in mlbb_java
    assert "DIR_USER_0 = \"/data/user/0/com.mobile.legends\"" in mlbb_java
    assert "DIR_ANDROID_DATA = \"/storage/emulated/0/Android/data/com.mobile.legends\"" in mlbb_java
    assert "enforcePermanentZeroRevert" in mlbb_java

    codm_java = (REPO_ROOT / "app/src/main/java/com/gamebooster/app/config/CodmConfigPatcher.java").read_text("utf-8")
    assert "TARGET_VERSION_CODE = 28028" in codm_java
    assert "TARGET_UID = 10656" in codm_java
    assert "TARGET_PACKAGE = \"com.garena.game.codm\"" in codm_java
    assert "DIR_USER_0 = \"/data/user/0/com.garena.game.codm\"" in codm_java
    assert "DIR_ANDROID_DATA = \"/storage/emulated/0/Android/data/com.garena.game.codm\"" in codm_java
    assert "enforcePermanentZeroRevert" in codm_java
    print("  [✓] Java Config Patchers verified.")

def test_native_cpp_layer():
    common_cpp = (REPO_ROOT / "app/src/main/cpp/config_common.cpp").read_text("utf-8")
    assert "FS_IMMUTABLE_FL" in common_cpp
    assert "FS_IOC_SETFLAGS" in common_cpp
    assert "1893456000L" in common_cpp

    bypass_cpp = (REPO_ROOT / "app/src/main/cpp/native_security_bypass.cpp").read_text("utf-8")
    assert "Java_com_gamebooster_app_config_NativeConfigInjector_nativeSetImmutableLock" in bypass_cpp
    assert "Java_com_gamebooster_app_config_NativeConfigInjector_nativeEnforceGameOwnership" in bypass_cpp
    print("  [✓] Native C++ Layer verified.")

def test_shell_scripts():
    install_sh = (REPO_ROOT / "app/src/main/assets/shell/install_game_mod_core.sh").read_text("utf-8")
    assert "UID_VAL=10677" in install_sh
    assert "VER_CODE=211812322" in install_sh
    assert "UID_VAL=10656" in install_sh
    assert "VER_CODE=28028" in install_sh
    assert "/data/user/0/" in install_sh
    assert "/storage/emulated/0/Android/data/" in install_sh
    assert "chattr +i" in install_sh
    assert "chmod 444" in install_sh
    assert "touch -t 203001010000" in install_sh

    mlbb_sh = (REPO_ROOT / "app/src/main/assets/shell/com.mobile.legends.sh").read_text("utf-8")
    assert "MLBB_UID=10677" in mlbb_sh
    assert "/data/user/0/com.mobile.legends/files" in mlbb_sh
    assert "/storage/emulated/0/Android/data/com.mobile.legends/files" in mlbb_sh
    assert "chattr +i" in mlbb_sh

    codm_sh = (REPO_ROOT / "app/src/main/assets/shell/com.garena.game.codm.sh").read_text("utf-8")
    assert "CODM_UID=10656" in codm_sh
    assert "/data/user/0/com.garena.game.codm/files" in codm_sh
    assert "/storage/emulated/0/Android/data/com.garena.game.codm/files" in codm_sh
    assert "chattr +i" in codm_sh
    print("  [✓] Shell Deployment Scripts verified.")

def main():
    print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    print(" RUNNING MLBB & CODM PATCH VERIFICATION SUITE")
    print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    try:
        test_mlbb_dump_metadata()
        test_codm_dump_metadata()
        test_java_patchers()
        test_native_cpp_layer()
        test_shell_scripts()
        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        print(" ALL VERIFICATION CHECKS PASSED (100% GREEN)")
        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    except AssertionError as e:
        print(f"❌ Verification Failed: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
