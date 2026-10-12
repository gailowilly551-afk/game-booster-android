import json
from pathlib import Path

def test_normalized_structure():
    path = Path("/home/willygailo/Downloads/Game-Launcher/android/MLBB-LATES-Patch/dump_mlbb/mlbb_extracted_strings.json")
    with open(path) as f:
        data = json.load(f)
    
    # Must have both category indexing and top-level or entries map
    assert "entries" in data or "by_identifier" in data, "Missing structured identifier dictionary in root"
    
    index = data.get("by_identifier", data.get("entries", {}))
    assert "RefFogOfWarMesh" in index, "RefFogOfWarMesh must exist in identifier index"
    assert "ISHOW_Init_SkillCoolDownCalc" in index, "ISHOW_Init_SkillCoolDownCalc must exist in identifier index"
    assert "m_iFieldOfView" in index, "m_iFieldOfView must exist in identifier index"
    assert "Freeze142_AntiCheatReport" in index, "Freeze142_AntiCheatReport must exist in identifier index"
    assert "ResCheckStartHandler" in index, "ResCheckStartHandler must exist in identifier index"

    item = index["RefFogOfWarMesh"]
    for req in ["identifier", "exact_name", "category", "buffer_offset", "file_offset"]:
        assert req in item, f"Missing required property {req} in item"

    print("✅ test_normalized_structure PASSED!")

if __name__ == "__main__":
    test_normalized_structure()
