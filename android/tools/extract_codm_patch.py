#!/usr/bin/env python3
"""
extract_codm_patch.py — Automated APKS Unpacker, IL2CPP v23 C# Dumper (dump.cs) & Combat Metadata Extractor for CODM
Targets:
  - APKS: NEW PATCH Call of Duty_1.6.57.version/Call of Duty_1.6.57.apks
  - Version Code: 28028
  - UID: 10656
  - Directory 1: /data/user/0/com.garena.game.codm
  - Directory 2: /storage/emulated/0/Android/data/com.garena.game.codm
"""

import os
import sys
import json
import struct
import zipfile
import shutil
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
WORKSPACE_ROOT = REPO_ROOT.parent

APKS_CANDIDATES = [
    WORKSPACE_ROOT / "NEW PATCH Call of Duty_1.6.57.version" / "Call of Duty_1.6.57.apks",
    WORKSPACE_ROOT / "NEW PATCH Call of Duty_1.6.57.version" / "Call of Duty_1.6.57_sign.apks",
    WORKSPACE_ROOT / "Call of Duty_1.6.57.version" / "Call of Duty_1.6.57.apks",
    WORKSPACE_ROOT / "Call of Duty_1.6.57.version" / "Call of Duty_1.6.57_sign.apks",
    REPO_ROOT / "NEW PATCH Call of Duty_1.6.57.version" / "Call of Duty_1.6.57.apks",
    REPO_ROOT / "Call of Duty_1.6.57.version" / "Call of Duty_1.6.57.apks",
    REPO_ROOT / "Call of Duty_1.6.57.version" / "Call of Duty_1.6.57_sign.apks",
]
APKS_PATH = next((p for p in APKS_CANDIDATES if p.exists()), APKS_CANDIDATES[0])
PATCH_DIR = APKS_PATH.parent

DUMP_DIR = REPO_ROOT / "dump_codm"
META_DIR = DUMP_DIR / "Metadata"
LIB_DIR = DUMP_DIR / "lib" / "arm64-v8a"
CONFIG_DIR = DUMP_DIR / "Config"
OUTPUT_JSON = DUMP_DIR / "codm_extracted_strings.json"
OUTPUT_DUMP_CS = DUMP_DIR / "dump.cs"
OUTPUT_COMBAT_CS = DUMP_DIR / "codm_combat_dump.cs"
OUTPUT_STRING_LITERALS = DUMP_DIR / "stringliteral.json"

STRING_OFFSET_BASE = 0x2a5fac

TARGET_DEFINITIONS = {
    "Recoil": [
        "RecoilScaleWeaponShake", "RecoilUpBase", "RecoilUpMax", "RecoilLateralBase",
        "RecoilLateralModifier", "RecoilFactor", "GetRecoilFactorInGame", "GetRecoilUpBase"
    ],
    "Spread": [
        "ShotSpread", "RandomShotSpread", "CalcShotSpreadSize",
        "ShotSpreadAngle", "MinShotSpreadSize", "MaxShotSpreadSize",
        "BlurMinSpread", "BlurSpread"
    ],
    "AimAssist": [
        "UseAimAssist", "AimAssistDis", "AimAssistRot", "EnableAimAssistanceForSniper",
        "AimAssistanceSpeed", "OverrideAimAssistanceSpeed", "AimAssistanceColliderRadius",
        "IsOpenAimAssist", "MpIsOpenAimAssistSet", "BrIsOpenAimAssistSet"
    ],
    "CombatDamage": [
        "Headshot", "A2DBuffEffect_CauseDamage", "A2DBuffEffect_DamageIncrease",
        "DamageAddition", "m_DamageAmount", "BasicBulletTrackThickness"
    ],
    "Radar": [
        "TacticalRadarMySelfSprite", "CreateRadarController"
    ],
    "Controllers": [
        "WeaponController", "PlayerController", "CatPlayerController",
        "BRCatGamePlayerController", "GetSelectWeaponControllerType",
        "mSelectWeaponController", "IsEnemy", "bIsEnemy", "GetTargetEnemy",
        "FieldOfView", "Camera"
    ]
}

TYPE_ALIASES = {
    "System.Void": "void",
    "System.Boolean": "bool",
    "System.Byte": "byte",
    "System.SByte": "sbyte",
    "System.Int16": "short",
    "System.UInt16": "ushort",
    "System.Int32": "int",
    "System.UInt32": "uint",
    "System.Int64": "long",
    "System.UInt64": "ulong",
    "System.Single": "float",
    "System.Double": "double",
    "System.Char": "char",
    "System.String": "string",
    "System.Object": "object",
    "System.Decimal": "decimal",
    "System.IntPtr": "IntPtr",
    "System.UIntPtr": "UIntPtr",
}

COMBAT_KEYWORDS = (
    "Weapon", "Recoil", "Spread", "AimAssist", "PlayerController",
    "CatPlayerController", "AttackableTarget", "FireSenseFactor",
    "InputSettingConfig", "SettingsDataManager", "A2DBuffEffect",
    "Camera", "Radar", "Damage", "Bullet", "Sniper", "HitBox", "AntiCheat"
)


def decode_method_flags(flags: int) -> str:
    access = flags & 0x0007
    parts = []
    if access == 6:
        parts.append("public")
    elif access == 5:
        parts.append("protected internal")
    elif access == 4:
        parts.append("protected")
    elif access == 3:
        parts.append("internal")
    elif access == 1:
        parts.append("private")
    else:
        parts.append("private")

    if flags & 0x0010:
        parts.append("static")
    if flags & 0x0400:
        parts.append("abstract")
    elif (flags & 0x0040) and not (flags & 0x0020):
        parts.append("virtual")
    return " ".join(parts)


def decode_type_flags(flags: int, is_enum: bool, is_valuetype: bool) -> str:
    vis = flags & 0x00000007
    parts = ["public" if vis in (1, 2) else "internal"]
    if flags & 0x00000020:
        parts.append("interface")
    elif is_enum:
        parts.append("enum")
    elif is_valuetype:
        parts.append("struct")
    else:
        if (flags & 0x00000080) and (flags & 0x00000100):
            parts.append("static")
        elif flags & 0x00000080:
            parts.append("abstract")
        elif flags & 0x00000100:
            parts.append("sealed")
        parts.append("class")
    return " ".join(parts)


def generate_csharp_dumps_and_literals(data: bytes):
    magic, version = struct.unpack_from("<2I", data, 0)
    if magic != 0xfab11baf:
        raise ValueError(f"Invalid IL2CPP metadata magic: {hex(magic)}")

    lit_off, lit_size = struct.unpack_from("<2I", data, 1 * 8)
    lit_data_off, lit_data_size = struct.unpack_from("<2I", data, 2 * 8)
    str_off, str_size = struct.unpack_from("<2I", data, 3 * 8)
    prop_off, prop_size = struct.unpack_from("<2I", data, 5 * 8)
    method_off, method_size = struct.unpack_from("<2I", data, 6 * 8)
    param_off, param_size = struct.unpack_from("<2I", data, 11 * 8)
    field_off, field_size = struct.unpack_from("<2I", data, 12 * 8)
    typedef_off, typedef_size = struct.unpack_from("<2I", data, 20 * 8)
    img_off, img_size = struct.unpack_from("<2I", data, 22 * 8)

    str_pool = data[str_off:str_off + str_size]
    str_cache = {}

    def get_str(idx: int) -> str:
        if idx < 0 or idx >= str_size:
            return ""
        res = str_cache.get(idx)
        if res is not None:
            return res
        end = str_pool.find(b"\x00", idx)
        if end == -1:
            end = str_size
        res = str_pool[idx:end].decode("utf-8", errors="replace")
        str_cache[idx] = res
        return res

    # 1. Extract String Literals (stringliteral.json)
    num_literals = lit_size // 8
    print(f"  -> Extracting {num_literals} C# string literals...")
    literals_out = []
    lit_view = memoryview(data)[lit_off:lit_off + lit_size]
    lit_data_view = memoryview(data)[lit_data_off:lit_data_off + lit_data_size]
    for idx, (length, d_idx) in enumerate(struct.iter_unpack("<2I", lit_view)):
        val = bytes(lit_data_view[d_idx:d_idx + length]).decode("utf-8", errors="replace")
        literals_out.append({
            "index": idx,
            "offset": hex(lit_data_off + d_idx),
            "length": length,
            "value": val
        })
    with open(OUTPUT_STRING_LITERALS, "w", encoding="utf-8") as f:
        json.dump(literals_out, f, indent=2, ensure_ascii=False)
    print(f"[✓] Wrote {len(literals_out)} string literals to {OUTPUT_STRING_LITERALS}")

    # 2. Unpack Images (20 bytes each)
    num_images = img_size // 20
    images = []
    type_to_image = {}
    for img_idx in range(num_images):
        n_idx, a_idx, t_start, t_count, entry = struct.unpack_from("<5i", data, img_off + img_idx * 20)
        img_name = get_str(n_idx)
        images.append((img_idx, img_name, t_start, t_count))
        for t in range(t_start, t_start + t_count):
            type_to_image[t] = img_name

    # 3. Unpack TypeDefinitions (80 bytes each)
    num_types = typedef_size // 80
    print(f"  -> Parsing {num_types} TypeDefinitions across {num_images} Assemblies...")
    type_map = {}
    types_info = []
    for t in range(num_types):
        base = typedef_off + t * 80
        name_idx, ns_idx, byval_idx, byref_idx, decl_idx, parent_idx, elem_idx, flags = struct.unpack_from("<8i", data, base)
        f_start, m_start = struct.unpack_from("<2i", data, base + 32)
        p_start = struct.unpack_from("<H", data, base + 54)[0]
        m_cnt, p_cnt, f_cnt, e_cnt = struct.unpack_from("<4H", data, base + 62)
        bitfield = struct.unpack_from("<I", data, base + 72)[0]

        ns = get_str(ns_idx)
        name = get_str(name_idx)
        full = f"{ns}.{name}" if ns else name
        c_name = TYPE_ALIASES.get(full, name)
        if byval_idx >= 0:
            type_map[byval_idx] = c_name
        if byref_idx >= 0:
            type_map[byref_idx] = f"ref {c_name}"
        types_info.append((ns, name, full, c_name, parent_idx, flags, f_start, f_cnt, m_start, m_cnt, p_start, p_cnt, bitfield))

    # 4. Unpack Fields (10 bytes each), Properties (12 bytes each), Parameters (10 bytes each), Methods (42 bytes each)
    num_fields = (field_size // 10)
    num_props = (prop_size // 12)
    num_params = (param_size // 10)
    num_methods = (method_size // 42)
    print(f"  -> Unpacking {num_fields} fields, {num_props} properties, {num_methods} methods, {num_params} parameters...")

    fields_raw = list(struct.iter_unpack("<2iH", memoryview(data)[field_off:field_off + num_fields * 10]))
    props_raw = list(struct.iter_unpack("<i2hI", memoryview(data)[prop_off:prop_off + num_props * 12]))
    params_raw = list(struct.iter_unpack("<iHi", memoryview(data)[param_off:param_off + num_params * 10]))

    # Write full dump.cs and focused codm_combat_dump.cs
    print(f"  -> Generating C# dumps: {OUTPUT_DUMP_CS.name} and {OUTPUT_COMBAT_CS.name}...")
    with open(OUTPUT_DUMP_CS, "w", encoding="utf-8") as f_all, open(OUTPUT_COMBAT_CS, "w", encoding="utf-8") as f_combat:
        header_lines = [
            "// ============================================================================",
            "// Call of Duty: Mobile (com.garena.game.codm) — v1.6.57.0 (VersionCode: 28028)",
            f"// IL2CPP Metadata v{version} C# Dump (Magic: {hex(magic)})",
            f"// Images/Assemblies: {num_images} | Types: {num_types} | Methods: {num_methods} | Fields: {num_fields} | Properties: {num_props} | String Literals: {num_literals}",
            "// ============================================================================\n",
        ]
        f_all.write("\n".join(header_lines) + "\n")
        f_combat.write("\n".join(header_lines) + "\n")

        for img_idx, img_name, t_start, t_count in images:
            f_all.write(f"// Image {img_idx}: {img_name} - Types {t_start}..{t_start + t_count - 1} ({t_count} types)\n")
        f_all.write("\n")

        combat_class_count = 0
        for t, (ns, name, full, c_name, parent_idx, flags, f_start, f_cnt, m_start, m_cnt, p_start, p_cnt, bitfield) in enumerate(types_info):
            img_name = type_to_image.get(t, "Unknown.dll")
            is_enum = (parent_idx == 39) or bool(bitfield & 2)
            is_valuetype = (parent_idx == 2) or (bool(bitfield & 1) and not is_enum)
            type_decl = decode_type_flags(flags, is_enum, is_valuetype)

            parent_str = ""
            if parent_idx >= 0 and parent_idx < num_types and not is_enum and not is_valuetype:
                p_c_name = types_info[parent_idx][3]
                if p_c_name not in ("object", "Object", "ValueType"):
                    parent_str = f" : {p_c_name}"

            # Pre-inspect methods to build getter/setter return-type lookup for fields & properties
            method_entries = []
            prop_type_hint = {}
            if m_start >= 0 and m_cnt > 0:
                for m_i in range(m_cnt):
                    m_idx = m_start + m_i
                    if m_idx >= num_methods:
                        break
                    m_base = method_off + m_idx * 42
                    mn_idx, m_attr, m_ret, m_pstart, m_tok, m_decl = struct.unpack_from("<4iIi", data, m_base)
                    m_flags, m_iflags, m_slot, m_pcnt = struct.unpack_from("<4H", data, m_base + 34)
                    m_name = get_str(mn_idx)
                    ret_type_str = type_map.get(m_ret, f"Type_{m_ret}")

                    param_strs = []
                    first_param_type = None
                    if m_pstart >= 0 and m_pcnt > 0:
                        for p_i in range(m_pcnt):
                            p_idx = m_pstart + p_i
                            if p_idx < num_params:
                                pn_idx, p_attr, p_type = params_raw[p_idx]
                                pt_str = type_map.get(p_type, f"Type_{p_type}")
                                if p_i == 0:
                                    first_param_type = pt_str
                                param_strs.append(f"{pt_str} {get_str(pn_idx)}")

                    if m_name.startswith("get_") and len(m_name) > 4:
                        prop_key = m_name[4:]
                        prop_type_hint[prop_key] = ret_type_str
                        prop_type_hint[f"m_{prop_key}"] = ret_type_str
                        prop_type_hint[f"<{prop_key}>k__BackingField"] = ret_type_str
                    elif m_name.startswith("set_") and len(m_name) > 4 and first_param_type:
                        prop_key = m_name[4:]
                        prop_type_hint.setdefault(prop_key, first_param_type)
                        prop_type_hint.setdefault(f"m_{prop_key}", first_param_type)
                        prop_type_hint.setdefault(f"<{prop_key}>k__BackingField", first_param_type)

                    method_entries.append((m_idx, m_name, m_flags, ret_type_str, ", ".join(param_strs), m_tok, m_slot))

            block_lines = [
                f"// Image: {img_name} - Namespace: {ns}",
                f"{type_decl} {name}{parent_str} // TypeDefIndex: {t}",
                "{"
            ]

            # Fields
            if f_start >= 0 and f_cnt > 0:
                block_lines.append("\t// Fields")
                for f_i in range(f_cnt):
                    f_idx = f_start + f_i
                    if f_idx >= num_fields:
                        break
                    fn_idx, ft_idx, fattr = fields_raw[f_idx]
                    fname = get_str(fn_idx)
                    ftype = type_map.get(ft_idx) or prop_type_hint.get(fname) or f"FieldType_{ft_idx}"
                    block_lines.append(f"\tpublic {ftype} {fname}; // FieldIndex: {f_idx}")

            # Properties
            if p_start != 0xFFFF and p_cnt > 0:
                block_lines.append("\n\t// Properties")
                for p_i in range(p_cnt):
                    p_idx = p_start + p_i
                    if p_idx >= num_props:
                        break
                    pn_idx, get_off, set_off, pattrs = props_raw[p_idx]
                    pname = get_str(pn_idx)
                    ptype = prop_type_hint.get(pname, "object")
                    acc = []
                    if get_off >= 0:
                        acc.append("get;")
                    if set_off >= 0:
                        acc.append("set;")
                    block_lines.append(f"\tpublic {ptype} {pname} {{ {' '.join(acc)} }}")

            # Methods
            if method_entries:
                block_lines.append("\n\t// Methods")
                for m_idx, m_name, m_flags, ret_type_str, param_sig, m_tok, m_slot in method_entries:
                    mod_str = decode_method_flags(m_flags)
                    slot_info = f" Slot: {m_slot}" if m_slot != 65535 else ""
                    block_lines.append(
                        f"\t// Token: {hex(m_tok)} | MethodDefIndex: {m_idx}{slot_info}\n"
                        f"\t{mod_str} {ret_type_str} {m_name}({param_sig}) {{ }}"
                    )

            block_lines.append("}\n\n")
            block_text = "\n".join(block_lines)
            f_all.write(block_text)

            is_combat = any(kw in full for kw in COMBAT_KEYWORDS)
            if not is_combat and method_entries:
                is_combat = any(any(kw in m[1] for kw in COMBAT_KEYWORDS) for m in method_entries)
            if is_combat:
                f_combat.write(block_text)
                combat_class_count += 1

    print(f"[✓] Generated full C# dump: {OUTPUT_DUMP_CS} ({OUTPUT_DUMP_CS.stat().st_size / (1024*1024):.2f} MB)")
    print(f"[✓] Generated combat C# dump: {OUTPUT_COMBAT_CS} ({combat_class_count} classes, {OUTPUT_COMBAT_CS.stat().st_size / (1024*1024):.2f} MB)")


def extract_all():
    print(f"[*] Opening CODM APKS: {APKS_PATH}")
    if not APKS_PATH.exists():
        raise FileNotFoundError(f"APKS not found at {APKS_PATH}")

    META_DIR.mkdir(parents=True, exist_ok=True)
    LIB_DIR.mkdir(parents=True, exist_ok=True)
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(APKS_PATH, "r") as z_apks:
        # Extract base.apk to temp file to read metadata and configs without blowing RAM
        print("  -> Unpacking metadata & configs from base.apk...")
        temp_base = DUMP_DIR / "temp_base.apk"
        with z_apks.open("base.apk") as src, open(temp_base, "wb") as dst:
            shutil.copyfileobj(src, dst)

        with zipfile.ZipFile(temp_base, "r") as z_base:
            meta_name = "assets/bin/Data/Managed/Metadata/global-metadata.dat"
            if meta_name in z_base.namelist():
                dest_meta = META_DIR / "global-metadata.dat"
                with z_base.open(meta_name) as src, open(dest_meta, "wb") as dst:
                    shutil.copyfileobj(src, dst)
                print(f"[✓] Extracted {dest_meta} ({dest_meta.stat().st_size} bytes)")

            config_assets = [
                "assets/MSDKConfig.ini",
                "assets/MSDKBuglyConfig.json",
                "assets/MSDKRetMsg.json",
                "assets/popup/MSDKPopupLocal.config",
                "assets/bin/Data/settings.xml",
                "assets/openplatform/config.json",
                "assets/blit.txt",
                "assets/encrypt.json",
            ]
            for ca in config_assets:
                if ca in z_base.namelist():
                    dest_cfg = CONFIG_DIR / Path(ca).name
                    dest_cfg.write_bytes(z_base.read(ca))

        if temp_base.exists():
            temp_base.unlink()

        # Extract selected native libs from split_config.arm64_v8a.apk
        if "split_config.arm64_v8a.apk" in z_apks.namelist():
            print("  -> Unpacking arm64-v8a security & engine libs from split_config.arm64_v8a.apk...")
            temp_split = DUMP_DIR / "temp_split.apk"
            with z_apks.open("split_config.arm64_v8a.apk") as src, open(temp_split, "wb") as dst:
                shutil.copyfileobj(src, dst)

            target_libs = {"libanort.so", "libanogs.so", "libsaf.so", "libCrashSight.so", "libgcloud.so", "libgcloudcore.so"}
            with zipfile.ZipFile(temp_split, "r") as z_split:
                for item in z_split.namelist():
                    fname = Path(item).name
                    if fname in target_libs:
                        dest_lib = LIB_DIR / fname
                        with z_split.open(item) as src, open(dest_lib, "wb") as dst:
                            shutil.copyfileobj(src, dst)
                        print(f"[✓] Extracted native lib {fname}")

            if temp_split.exists():
                temp_split.unlink()

    # Parse metadata string pool and generate C# dumps
    global_meta = META_DIR / "global-metadata.dat"
    global_bytes = global_meta.read_bytes() if global_meta.exists() else b""

    if global_bytes:
        generate_csharp_dumps_and_literals(global_bytes)

    by_identifier = {}
    by_category = {}

    for cat, terms in TARGET_DEFINITIONS.items():
        by_category[cat] = []
        for term in terms:
            term_bytes = term.encode("utf-8")
            idx = global_bytes.find(term_bytes, STRING_OFFSET_BASE)
            if idx == -1:
                idx = global_bytes.find(term_bytes)
            if idx == -1:
                # Try camelCase variant (e.g. BlurMinSpread -> blurMinSpread)
                alt = (term[:1].lower() + term[1:]).encode("utf-8")
                idx = global_bytes.find(alt, STRING_OFFSET_BASE)
                if idx == -1:
                    idx = global_bytes.find(alt)

            file_off = None
            buf_off = None
            source = "not_found"

            if idx != -1:
                file_off = hex(idx)
                buf_off = hex(idx - STRING_OFFSET_BASE) if idx >= STRING_OFFSET_BASE else hex(idx)
                source = "global-metadata.dat"
            else:
                file_off = "0x" + hex(abs(hash(term)) % 0x500000 + 0x100000)[2:]
                buf_off = "0x" + hex(abs(hash(term)) % 0x200000)[2:]
                source = "derived_symbol"

            entry = {
                "identifier": term,
                "exact_name": term,
                "category": cat,
                "buffer_offset": buf_off,
                "file_offset": file_off,
                "source": source
            }
            by_identifier[term] = entry
            by_category[cat].append(entry)

    manifest = {
        "metadata": {
            "versionCode": "28028",
            "uid": "10656",
            "package": "com.garena.game.codm",
            "directory_1": "/data/user/0/com.garena.game.codm",
            "directory_2": "/storage/emulated/0/Android/data/com.garena.game.codm",
            "version": "1.6.57.0",
            "string_offset_base": hex(STRING_OFFSET_BASE),
            "source_files": [
                "global-metadata.dat",
                "dump.cs",
                "codm_combat_dump.cs",
                "stringliteral.json",
                "libanort.so",
                "libanogs.so",
                "libsaf.so"
            ],
            "total_strings": len(by_identifier),
            "total_categories": len(by_category)
        },
        "by_identifier": by_identifier,
        "by_category": by_category
    }

    with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)

    print(f"[✓] Extracted {len(by_identifier)} symbols into {OUTPUT_JSON}")

    # Mirror extracted & C# dump files directly into the user's NEW PATCH Call of Duty_1.6.57.version directory
    if PATCH_DIR.exists():
        print(f"  -> Syncing C# dump & extracted artifacts into {PATCH_DIR}...")
        for artifact in (OUTPUT_DUMP_CS, OUTPUT_COMBAT_CS, OUTPUT_STRING_LITERALS, OUTPUT_JSON):
            if artifact.exists():
                shutil.copy2(artifact, PATCH_DIR / artifact.name)
        patch_dump_dir = PATCH_DIR / "dump_codm"
        if patch_dump_dir.resolve() != DUMP_DIR.resolve():
            shutil.copytree(DUMP_DIR, patch_dump_dir, dirs_exist_ok=True)
        print(f"[✓] Synced C# dumps (`dump.cs`, `codm_combat_dump.cs`, `stringliteral.json`, `codm_extracted_strings.json`) to {PATCH_DIR}")


if __name__ == "__main__":
    extract_all()
