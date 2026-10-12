import json
from pathlib import Path

def test_normalized_structure():
    path = Path("/home/willygailo/Downloads/Game-Launcher/android/CODM-LATEST-PATCH/dump_codm/codm_extracted_strings.json")
    with open(path) as f:
        data = json.load(f)
    assert "by_identifier" in data, "Missing 'by_identifier' top-level key"
    assert "by_category" in data, "Missing 'by_category' top-level key"
    
    # Check that fuzzy keys are fixed to exact strings
    by_id = data["by_identifier"]
    assert by_id["ShotSpread"]["exact_name"] == "ShotSpread", "ShotSpread must be exact string"
    assert by_id["DamageAddition"]["exact_name"] == "DamageAddition", "DamageAddition must be exact string"
    assert by_id["CatPlayerController"]["exact_name"] == "CatPlayerController", "CatPlayerController must be exact string"
    assert by_id["IsEnemy"]["exact_name"] == "IsEnemy", "IsEnemy must be exact string"
    assert by_id["AimAssistRot"]["exact_name"] == "AimAssistRot", "AimAssistRot must be exact string"

if __name__ == "__main__":
    test_normalized_structure()
    print("✅ test_normalized_structure PASSED!")
