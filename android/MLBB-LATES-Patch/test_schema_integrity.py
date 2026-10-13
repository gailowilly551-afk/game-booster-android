import json
from pathlib import Path

def test_normalized_structure():
    path = Path("/home/willygailo/Downloads/Game-Launcher/android/MLBB-LATES-Patch/dump_mlbb/mlbb_extracted_strings.json")
    with open(path) as f:
        data = json.load(f)
    
    # Must have both category indexing and top-level or entries map
    assert "entries" in data or "by_identifier" in data, "Missing structured identifier dictionary in root"
    
    index = data.get("by_identifier", data.get("entries", {}))
    # Combat and bypass token assertions
    required_tokens = [
        "RefFogOfWarMesh", "ISHOW_Init_SkillCoolDownCalc", "m_iFieldOfView",
        "Freeze142_AntiCheatReport", "ResCheckStartHandler", "CountHurtValue",
        "m_BasePhyAtt", "m_BaseMagAtt", "m_AttSpeed", "m_MoveSpeed",
        "m_PhyBaseShield", "m_MagBaseShield", "EmblemV4Data", "GetEmblemLv"
    ]
    for token in required_tokens:
        assert token in index, f"{token} must exist in identifier index"

    item = index["RefFogOfWarMesh"]
    for req in ["identifier", "exact_name", "category", "buffer_offset", "file_offset"]:
        assert req in item, f"Missing required property {req} in item"

    print("✅ test_normalized_structure PASSED! All 14 required tokens and schema verified.")

if __name__ == "__main__":
    test_normalized_structure()

