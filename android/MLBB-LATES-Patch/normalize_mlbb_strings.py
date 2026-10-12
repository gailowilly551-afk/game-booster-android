#!/usr/bin/env python3
"""
normalize_mlbb_strings.py — Normalizes MLBB extracted strings table
Converts category arrays into both a category grouping and a flat 'by_identifier' dictionary
so that tools and resolvers can perform direct O(1) lookups by string/class name.
"""

import json
from pathlib import Path

PATCH_DIR = Path(__file__).resolve().parent
JSON_PATH = PATCH_DIR / "dump_mlbb" / "mlbb_extracted_strings.json"

def normalize():
    print(f"[*] Reading: {JSON_PATH}")
    with open(JSON_PATH, "r", encoding="utf-8") as f:
        raw_data = json.load(f)

    # If raw_data already has "by_category", extract from that
    categories_dict = raw_data.get("by_category", raw_data)
    
    normalized_categories = {}
    by_identifier = {}

    for category, items in categories_dict.items():
        if category in ("by_category", "by_identifier"):
            continue
        if not isinstance(items, list):
            continue

        normalized_categories[category] = []
        for item in items:
            name = item.get("exact_name")
            buf_off = item.get("buffer_offset")
            file_off = item.get("file_offset")

            entry = {
                "identifier": name,
                "exact_name": name,
                "category": category,
                "buffer_offset": buf_off,
                "file_offset": file_off
            }
            normalized_categories[category].append(entry)
            by_identifier[name] = entry

    output = {
        "metadata": {
            "version": "2.2.16.12322",
            "realversion": "2.2.16.1232.1",
            "source_file": "global-csharp-metadata.dat",
            "total_strings": len(by_identifier),
            "total_categories": len(normalized_categories)
        },
        "by_identifier": by_identifier,
        "by_category": normalized_categories
    }

    with open(JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2)

    print(f"[✓] Normalized {len(by_identifier)} strings across {len(normalized_categories)} categories successfully!")

if __name__ == "__main__":
    normalize()
