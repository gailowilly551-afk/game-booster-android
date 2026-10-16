#!/usr/bin/env python3
"""
extract_mlbb_patch.py — Automated APKS Unpacker, IL2CPP SOSplit C# Dumper (dump.cs) & Combat Metadata Extractor for MLBB
Targets:
  - APKS: Mobile Legends_Bang Bang_2.2.16.12322.version/Mobile Legends_ Bang Bang_2.2.16.12322_sign.apks
  - Version Code: 211812322
  - UID: 10677
  - Directory 1: /data/user/0/com.mobile.legends
  - Directory 2: /storage/emulated/0/Android/data/com.mobile.legends
"""

import os
import sys
import json
import struct
import zipfile
import subprocess
import tempfile
import shutil
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
WORKSPACE_ROOT = REPO_ROOT.parent

APKS_CANDIDATES = [
    WORKSPACE_ROOT / "Mobile Legends_Bang Bang_2.2.16.12322.version" / "Mobile Legends_ Bang Bang_2.2.16.12322_sign.apks",
    WORKSPACE_ROOT / "Mobile Legends_Bang Bang_2.2.16.12322.version" / "Mobile Legends_ Bang Bang_2.2.16.12322.apks",
    REPO_ROOT / "Mobile Legends_Bang Bang_2.2.16.12322.version" / "Mobile Legends_ Bang Bang_2.2.16.12322_sign.apks",
    REPO_ROOT / "Mobile Legends_Bang Bang_2.2.16.12322.version" / "Mobile Legends_ Bang Bang_2.2.16.12322.apks",
]
APKS_PATH = next((p for p in APKS_CANDIDATES if p.exists()), APKS_CANDIDATES[0])
PATCH_DIR = APKS_PATH.parent

DUMP_DIR = REPO_ROOT / "dump_mlbb"
META_DIR = DUMP_DIR / "Metadata"
LIB_DIR = DUMP_DIR / "lib" / "arm64-v8a"
DOCS_DIR = DUMP_DIR / "Document" / "android"
SCENES_DIR = DUMP_DIR / "Scenes" / "android"
VERSION_DIR = DUMP_DIR / "version" / "android"

OUTPUT_JSON = DUMP_DIR / "mlbb_extracted_strings.json"
OUTPUT_DUMP_CS = DUMP_DIR / "dump.cs"
OUTPUT_COMBAT_CS = DUMP_DIR / "mlbb_combat_dump.cs"
OUTPUT_STRING_LITERALS = DUMP_DIR / "stringliteral.json"

TARGET_DEFINITIONS = {
    "PhysicalDamage": [
        "CountHurtValue", "CalcVal", "CheckCalcVal", "LogicHurtCalc",
        "m_HurtData", "m_BeHurtData", "BeHurtData",
        "m_BasePhyAtt", "m_LevelPhyAtt", "phyAtk"
    ],
    "MagicDamage": [
        "m_BaseMagAtt", "m_LevelMagAtt", "mgcAtk", "magicAtk", "HeivySpellAtt"
    ],
    "AttackSpeed": [
        "m_AttSpeed", "m_AttSpeed_show", "m_LevelAttSpeed", "m_AtkSpeedPer",
        "CoverAttackSpeedValue"
    ],
    "MovementSpeed": [
        "m_BaseSpeed", "m_MoveSpeed", "GetMoveSpeed", "GetMoveSpeedRadio",
        "GetMoveSpeedAccelerate", "bQuickMove", "moveSpeed"
    ],
    "PhysicalDefense": [
        "m_PhyBaseShield", "m_PhyLevelShield", "iPhysicalDefense",
        "physicalDef", "phyDef", "mechArmorHp", "maxMechArmorHp"
    ],
    "MagicDefense": [
        "m_MagBaseShield", "m_MagLevelShield", "iMagicDefense",
        "magicDef", "mgcDef", "shldAll"
    ],
    "HeroStats": [
        "CData_Hero_Element", "HeroData", "HeroAttribute", "UI_HeroAttribute",
        "FRAME_HeroAttribute", "m_BaseHp", "m_BaseMp", "m_FightHpRec", "m_FightMpRec",
        "m_Crit", "m_LevelHp", "m_LevelMp", "m_HPLevelRec", "m_MPLevelRec"
    ],
    "Emblem": [
        "EmblemV4Data", "GetEmblemListWithRec", "GetEmblemLv", "SaveArnoldPlan",
        "EmblemDataManager", "m_RecommendRune", "m_RecommendEquip", "m_RecommendSkill"
    ],
    "BattleItem": [
        "BattleItem", "TryCacheBattleItemData", "CmdId_BattleItem_GetAll_CS",
        "CData_ArenaResConfig", "m_EquipData"
    ],
    "SkillCooldown": [
        "ISHOW_Init_SkillCoolDownCalc", "ISHOW_AddSkillCoolDownCalc",
        "ISHOW_RemoveSkillCoolDownCalc", "Sync_Init_SkillCoolDownCalc"
    ],
    "AntiCheat": [
        "Freeze142_AntiCheatReport", "SetAntiCheatReport", "m_bAntiCheatReport",
        "AntiCheatReporter"
    ],
    "ResCheck": [
        "ResCheckStartHandler", "ResCheckProgressHandler", "ResCheckEndHandler",
        "ResCheckListener", "OnResCheckStartHandler", "OnResCheckEndHandler",
        "RegisterResCheckListener", "UnregisterResCheckListener", "ResCheck"
    ],
    "FogOfWar": [
        "RefFogOfWarMesh", "RefFogOfWarCommandBuffer", "IsFogOfWarMode",
        "FogOfWar_IsBlockedByTerrain", "FogOfWar_ShouldForceVisible",
        "FogOfWar_IsEntityHiddenByNewFOW", "VisionSpriteFindEnemyHero"
    ],
    "Camera": [
        "m_iFieldOfView", "fieldOfView", "fieldOfViewDict"
    ],
    "MolaDownload": [
        "DecodeMolaDownloadPatch", "IsMolaDownloadPatch", "MolaDownloadProvider",
        "GetMolaDownloader"
    ]
}

COMBAT_KEYWORDS = (
    "Hurt", "Damage", "Hero", "Speed", "Defense", "Shield", "Emblem",
    "Skill", "Item", "AntiCheat", "ResCheck", "FogOfWar", "Camera", "Battle"
)


def generate_mlbb_csharp_dump(meta_dir: Path):
    csharp_path = meta_dir / "global-csharp-metadata.dat"
    first_path = meta_dir / "global-first-metadata.dat"
    global_path = meta_dir / "global-metadata.dat"

    if not csharp_path.exists():
        return

    splits = {}
    for s_idx, p in [(1, global_path), (2, first_path), (3, csharp_path)]:
        if p.exists():
            d = p.read_bytes()
            s_off, s_sz = struct.unpack_from("<2I", d, 3 * 4 + 2 * 8)
            splits[s_idx] = (d, s_off, s_sz)

    d3 = splits[3][0]

    def get_str(idx: int) -> str:
        if idx < 0:
            return ""
        s_id = (idx >> 24) & 0xFF
        if s_id not in splits:
            s_id = 3
        d, s_off, s_sz = splits[s_id]
        off = idx & 0x00FFFFFF
        if off >= s_sz:
            return ""
        end = d.find(b"\x00", s_off + off)
        if end == -1:
            end = s_off + s_sz
        return d[s_off + off:end].decode("utf-8", errors="replace")

    # Unpack string literals from global-csharp-metadata.dat
    lit_off, lit_size = struct.unpack_from("<2I", d3, 3 * 4 + 0 * 8)
    lit_data_off, lit_data_size = struct.unpack_from("<2I", d3, 3 * 4 + 1 * 8)
    num_literals = lit_size // 8
    print(f"  -> Extracting {num_literals} MLBB C# string literals...")
    literals_out = []
    lit_view = memoryview(d3)[lit_off:lit_off + lit_size]
    lit_data_view = memoryview(d3)[lit_data_off:lit_data_off + lit_data_size]
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

    # Unpack Images
    img_off, img_size = struct.unpack_from("<2I", d3, 3 * 4 + 20 * 8)
    num_images = img_size // 40
    images = []
    type_to_image = {}
    for i in range(num_images):
        vals = struct.unpack_from("<10i", d3, img_off + i * 40)
        img_name = get_str(vals[0])
        t_start = vals[2] & 0x00FFFFFF
        t_count = vals[3]
        images.append((i, img_name, t_start, t_count))
        for t in range(t_start, t_start + t_count):
            type_to_image[t] = img_name

    # Unpack Types, Methods, Fields, Parameters, Properties
    typedef_off, typedef_size = struct.unpack_from("<2I", d3, 3 * 4 + 19 * 8)
    method_off, method_size = struct.unpack_from("<2I", d3, 3 * 4 + 5 * 8)
    field_off, field_size = struct.unpack_from("<2I", d3, 3 * 4 + 11 * 8)
    param_off, param_size = struct.unpack_from("<2I", d3, 3 * 4 + 10 * 8)
    prop_off, prop_size = struct.unpack_from("<2I", d3, 3 * 4 + 4 * 8)

    num_types = typedef_size // 92
    num_methods = method_size // 32
    num_fields = field_size // 12
    num_params = param_size // 12
    num_props = prop_size // 20

    print(f"  -> Parsing {num_types} Types, {num_methods} Methods, {num_fields} Fields across {num_images} MLBB Assemblies...")

    methods_raw = list(struct.iter_unpack("<6i4H", memoryview(d3)[method_off:method_off + num_methods * 32]))
    fields_raw = list(struct.iter_unpack("<3i", memoryview(d3)[field_off:field_off + num_fields * 12]))
    params_raw = list(struct.iter_unpack("<3i", memoryview(d3)[param_off:param_off + num_params * 12]))
    props_raw = list(struct.iter_unpack("<5i", memoryview(d3)[prop_off:prop_off + num_props * 20]))

    # Pre-index methods by declaringType
    print(f"  -> Indexing {num_methods} methods by declaringType...")
    methods_by_type = {}
    for m_idx in range(num_methods):
        m_tuple = methods_raw[m_idx]
        dt = m_tuple[1] & 0x00FFFFFF
        methods_by_type.setdefault(dt, []).append((m_idx, m_tuple))

    print(f"  -> Generating C# dumps: {OUTPUT_DUMP_CS.name} and {OUTPUT_COMBAT_CS.name}...")
    with open(OUTPUT_DUMP_CS, "w", encoding="utf-8") as f_all, open(OUTPUT_COMBAT_CS, "w", encoding="utf-8") as f_combat:
        header_lines = [
            "// ============================================================================",
            "// Mobile Legends: Bang Bang (com.mobile.legends) — v2.2.16.12322 (VersionCode: 211812322)",
            f"// Moonton Unity IL2CPP SOSplit C# Dump",
            f"// Images: {num_images} | Types: {num_types} | Methods: {num_methods} | Fields: {num_fields} | Properties: {num_props} | Literals: {num_literals}",
            "// ============================================================================\n"
        ]
        f_all.write("\n".join(header_lines) + "\n")
        f_combat.write("\n".join(header_lines) + "\n")

        for img_idx, img_name, t_start, t_count in images:
            f_all.write(f"// Image {img_idx}: {img_name} - Types {t_start}..{t_start + t_count - 1} ({t_count} types)\n")
        f_all.write("\n")

        combat_classes = 0
        for t in range(num_types):
            vals = struct.unpack_from("<18i8HI", d3, typedef_off + t * 92)
            name = get_str(vals[0])
            ns = get_str(vals[1])
            full_name = f"{ns}.{name}" if ns else name
            img_name = type_to_image.get(t, "Unknown.dll")

            f_start = vals[9] & 0x00FFFFFF if vals[9] >= 0 else -1
            p_start = vals[12] & 0x00FFFFFF if vals[12] >= 0 else -1
            f_cnt = vals[18] # H[0]
            p_cnt = vals[19] # H[1]

            block_lines = [
                f"// Image: {img_name} - Namespace: {ns}",
                f"public class {name} // TypeDefIndex: {t}",
                "{"
            ]

            # Fields
            if f_start >= 0 and f_cnt > 0:
                block_lines.append("\t// Fields")
                for fi in range(f_cnt):
                    f_idx = f_start + fi
                    if f_idx < num_fields:
                        fn_idx, ft_idx, ftok = fields_raw[f_idx]
                        block_lines.append(f"\tpublic object {get_str(fn_idx)}; // Token: {hex(ftok)} | FieldIndex: {f_idx}")

            # Properties
            if p_start >= 0 and p_cnt > 0:
                block_lines.append("\n\t// Properties")
                for pi in range(p_cnt):
                    p_idx = p_start + pi
                    if p_idx < num_props:
                        pn_idx, pget, pset, pattr, ptok = props_raw[p_idx]
                        acc = []
                        if pget >= 0: acc.append("get;")
                        if pset >= 0: acc.append("set;")
                        block_lines.append(f"\tpublic object {get_str(pn_idx)} {{ {' '.join(acc)} }} // Token: {hex(ptok)}")

            # Methods
            method_names = []
            type_methods = methods_by_type.get(t, [])
            if type_methods:
                block_lines.append("\n\t// Methods")
                for m_idx, m_tuple in type_methods:
                    mn_idx, mdecl, mret, mpstart, mgen, mtok, mflags, miflags, mslot, mpcnt = m_tuple
                    mname = get_str(mn_idx)
                    method_names.append(mname)
                    p_params = []
                    if mpstart >= 0 and mpcnt > 0:
                        p_base = mpstart & 0x00FFFFFF
                        for pi in range(mpcnt):
                            if p_base + pi < num_params:
                                pn_idx, ptok_param, ptype = params_raw[p_base + pi]
                                p_params.append(f"object {get_str(pn_idx)}")
                    param_str = ", ".join(p_params)
                    slot_str = f" Slot: {mslot}" if mslot != 65535 else ""
                    block_lines.append(
                        f"\t// Token: {hex(mtok)} | MethodDefIndex: {m_idx}{slot_str}\n"
                        f"\tpublic object {mname}({param_str}) {{ }}"
                    )

            block_lines.append("}\n\n")
            block_text = "\n".join(block_lines)
            f_all.write(block_text)

            is_combat = any(kw.lower() in full_name.lower() for kw in COMBAT_KEYWORDS)
            if not is_combat and method_names:
                is_combat = any(any(kw.lower() in mn.lower() for kw in COMBAT_KEYWORDS) for mn in method_names)
            if is_combat:
                f_combat.write(block_text)
                combat_classes += 1

    print(f"[✓] Generated MLBB full C# dump: {OUTPUT_DUMP_CS} ({OUTPUT_DUMP_CS.stat().st_size / (1024*1024):.2f} MB)")
    print(f"[✓] Generated MLBB combat C# dump: {OUTPUT_COMBAT_CS} ({combat_classes} classes, {OUTPUT_COMBAT_CS.stat().st_size / (1024*1024):.2f} MB)")


def extract_all():
    print(f"[*] Opening MLBB APKS: {APKS_PATH}")
    if not APKS_PATH.exists():
        raise FileNotFoundError(f"APKS not found at {APKS_PATH}")

    META_DIR.mkdir(parents=True, exist_ok=True)
    LIB_DIR.mkdir(parents=True, exist_ok=True)
    DOCS_DIR.mkdir(parents=True, exist_ok=True)
    SCENES_DIR.mkdir(parents=True, exist_ok=True)
    VERSION_DIR.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(APKS_PATH, "r") as z_apks:
        # Extract base.apk to temp file
        temp_base = DUMP_DIR / "temp_base.apk"
        print("  -> Unpacking 7z Resource archives from base.apk...")
        with z_apks.open("base.apk") as src, open(temp_base, "wb") as dst:
            shutil.copyfileobj(src, dst)

        with zipfile.ZipFile(temp_base, "r") as z_base:
            # 1. Extract 7z archive Resources4-2.dat for real IL2CPP metadata
            if "assets/Resources4-2.dat" in z_base.namelist():
                tmp_r2 = DUMP_DIR / "Resources4-2.7z"
                with z_base.open("assets/Resources4-2.dat") as src, open(tmp_r2, "wb") as dst:
                    shutil.copyfileobj(src, dst)
                print(f"  -> Unpacking Metadata from Resources4-2.7z ({tmp_r2.stat().st_size} bytes)...")
                subprocess.run(["7z", "e", str(tmp_r2), f"-o{META_DIR}", "UnityData/Managed/Metadata/*", "-y"], capture_output=True)
                tmp_r2.unlink(missing_ok=True)
                print(f"[✓] Extracted MLBB metadata to {META_DIR}")

            # 2. Extract 7z archive Resources4-4.dat for Document and Scenes
            if "assets/Resources4-4.dat" in z_base.namelist():
                tmp_r4 = DUMP_DIR / "Resources4-4.7z"
                with z_base.open("assets/Resources4-4.dat") as src, open(tmp_r4, "wb") as dst:
                    shutil.copyfileobj(src, dst)
                print(f"  -> Unpacking Document and Scenes from Resources4-4.7z ({tmp_r4.stat().st_size} bytes)...")
                subprocess.run(["7z", "e", str(tmp_r4), f"-o{DOCS_DIR}", "Document/android/*", "-y"], capture_output=True)
                subprocess.run(["7z", "e", str(tmp_r4), f"-o{SCENES_DIR}", "Scenes/android/*", "-y"], capture_output=True)
                tmp_r4.unlink(missing_ok=True)
                print(f"[✓] Extracted Document & Scenes to {DOCS_DIR.parent}")

            # 3. Extract version manifests
            for vname in ["version.xml", "realversion.xml", "usrinfo.xml", "iplist.xml"]:
                v_path = f"assets/version/android/{vname}"
                if v_path in z_base.namelist():
                    (VERSION_DIR / vname).write_bytes(z_base.read(v_path))

            # 4. Extract boot.config
            if "assets/bin/Data/boot.config" in z_base.namelist():
                (DUMP_DIR / "boot.config").write_bytes(z_base.read("assets/bin/Data/boot.config"))

        if temp_base.exists():
            temp_base.unlink()

        # 5. Extract native libs from split_config.arm64_v8a.apk
        if "split_config.arm64_v8a.apk" in z_apks.namelist():
            print("  -> Unpacking arm64-v8a native libraries from split_config.arm64_v8a.apk...")
            temp_split = DUMP_DIR / "temp_split.apk"
            with z_apks.open("split_config.arm64_v8a.apk") as src, open(temp_split, "wb") as dst:
                shutil.copyfileobj(src, dst)

            with zipfile.ZipFile(temp_split, "r") as z_split:
                for item in z_split.namelist():
                    if item.startswith("lib/arm64-v8a/") and item.endswith(".so"):
                        fname = Path(item).name
                        (LIB_DIR / fname).write_bytes(z_split.read(item))
            print(f"[✓] Extracted arm64-v8a native libraries to {LIB_DIR}")

            if temp_split.exists():
                temp_split.unlink()

    # Generate C# dump from extracted metadata
    generate_mlbb_csharp_dump(META_DIR)

    # Read extracted metadata files for string index extraction
    csharp_meta = META_DIR / "global-csharp-metadata.dat"
    first_meta = META_DIR / "global-first-metadata.dat"
    global_meta = META_DIR / "global-metadata.dat"

    csharp_bytes = csharp_meta.read_bytes() if csharp_meta.exists() else b""
    first_bytes = first_meta.read_bytes() if first_meta.exists() else b""
    global_bytes = global_meta.read_bytes() if global_meta.exists() else b""

    by_identifier = {}
    by_category = {}

    for cat, terms in TARGET_DEFINITIONS.items():
        by_category[cat] = []
        for term in terms:
            term_bytes = term.encode("utf-8")
            csharp_idx = csharp_bytes.find(term_bytes)
            first_idx = first_bytes.find(term_bytes)
            global_idx = global_bytes.find(term_bytes)

            file_off = None
            buf_off = None
            source = "not_found"

            if csharp_idx != -1:
                file_off = hex(csharp_idx)
                buf_off = hex(csharp_idx)
                source = "global-csharp-metadata.dat"
            elif first_idx != -1:
                file_off = hex(first_idx)
                buf_off = hex(first_idx)
                source = "global-first-metadata.dat"
            elif global_idx != -1:
                file_off = hex(global_idx)
                buf_off = hex(global_idx)
                source = "global-metadata.dat"

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
            "versionCode": "211812322",
            "uid": "10677",
            "package": "com.mobile.legends",
            "directory_1": "/data/user/0/com.mobile.legends",
            "directory_2": "/storage/emulated/0/Android/data/com.mobile.legends",
            "version": "2.2.16.12322",
            "realversion": "2.2.16.1232.1",
            "source_files": [
                "global-csharp-metadata.dat",
                "global-first-metadata.dat",
                "global-metadata.dat",
                "dump.cs",
                "mlbb_combat_dump.cs",
                "stringliteral.json",
                "libil2cpp.so"
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

    # Mirror artifacts to Mobile Legends_Bang Bang_2.2.16.12322.version directory
    if PATCH_DIR.exists():
        print(f"  -> Syncing MLBB C# dump & extracted artifacts into {PATCH_DIR}...")
        for artifact in (OUTPUT_DUMP_CS, OUTPUT_COMBAT_CS, OUTPUT_STRING_LITERALS, OUTPUT_JSON):
            if artifact.exists():
                shutil.copy2(artifact, PATCH_DIR / artifact.name)
        patch_dump_dir = PATCH_DIR / "dump_mlbb"
        if patch_dump_dir.resolve() != DUMP_DIR.resolve():
            shutil.copytree(DUMP_DIR, patch_dump_dir, dirs_exist_ok=True)
        print(f"[✓] Synced MLBB C# dumps to {PATCH_DIR}")


if __name__ == "__main__":
    extract_all()
