#!/usr/bin/env python3
"""
Convert KanjiVG SVG files (in scripts/kanjivg_raw/) to Android
AnimatedVectorDrawable XML files (in app/src/main/res/drawable/).

Each output file animates strokes one-by-one using
trimPathEnd ObjectAnimators sequenced by startOffset.

Usage:
    python3 scripts/kanjivg_to_avd.py
"""

import xml.etree.ElementTree as ET
from pathlib import Path
import sys

# (codepoint hex, romaji-based filename suffix) — mirrors data/Kana.kt order
KANA_TABLE = [
    ("03042", "a"), ("03044", "i"), ("03046", "u"), ("03048", "e"), ("0304a", "o"),
    ("0304b", "ka"), ("0304d", "ki"), ("0304f", "ku"), ("03051", "ke"), ("03053", "ko"),
    ("03055", "sa"), ("03057", "shi"), ("03059", "su"), ("0305b", "se"), ("0305d", "so"),
    ("0305f", "ta"), ("03061", "chi"), ("03064", "tsu"), ("03066", "te"), ("03068", "to"),
    ("0306a", "na"), ("0306b", "ni"), ("0306c", "nu"), ("0306d", "ne"), ("0306e", "no"),
    ("0306f", "ha"), ("03072", "hi"), ("03075", "fu"), ("03078", "he"), ("0307b", "ho"),
    ("0307e", "ma"), ("0307f", "mi"), ("03080", "mu"), ("03081", "me"), ("03082", "mo"),
    ("03084", "ya"), ("03086", "yu"), ("03088", "yo"),
    ("03089", "ra"), ("0308a", "ri"), ("0308b", "ru"), ("0308c", "re"), ("0308d", "ro"),
    ("0308f", "wa"), ("03092", "wo"),
    ("03093", "n"),
]

SVG_NS = "{http://www.w3.org/2000/svg}"
STROKE_DURATION_MS = 400
STROKE_WIDTH = "3"
STROKE_COLOR = "#FF000000"  # solid black; can be themed later via tint
VIEWPORT = 109               # KanjiVG canonical viewbox

ROOT = Path(__file__).resolve().parent.parent
RAW_DIR = ROOT / "scripts" / "kanjivg_raw"
OUT_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"


def extract_paths(svg_file: Path) -> list[str]:
    """Return list of `d=` attribute strings for each stroke path, in order."""
    tree = ET.parse(svg_file)
    root = tree.getroot()
    # KanjiVG: <g id="kvg:StrokePaths_XXXXX">...<path d="..."/>...</g>
    paths: list[str] = []
    for g in root.iter(f"{SVG_NS}g"):
        gid = g.attrib.get("id", "")
        if gid.startswith("kvg:StrokePaths_"):
            for p in g.iter(f"{SVG_NS}path"):
                d = p.attrib.get("d")
                if d:
                    paths.append(d)
            break
    return paths


def build_avd_xml(paths: list[str]) -> str:
    """Build the AnimatedVectorDrawable XML string."""
    n = len(paths)
    if n == 0:
        raise ValueError("no strokes found")

    path_elements = "\n".join(
        f'        <path\n'
        f'            android:name="stroke{i + 1}"\n'
        f'            android:pathData="{d}"\n'
        f'            android:strokeColor="{STROKE_COLOR}"\n'
        f'            android:strokeWidth="{STROKE_WIDTH}"\n'
        f'            android:strokeLineCap="round"\n'
        f'            android:strokeLineJoin="round"\n'
        f'            android:trimPathStart="0"\n'
        f'            android:trimPathEnd="0"\n'
        f'            android:fillColor="#00000000"/>'
        for i, d in enumerate(paths)
    )

    target_blocks = "\n".join(
        f'    <target android:name="stroke{i + 1}">\n'
        f'        <aapt:attr name="android:animation">\n'
        f'            <objectAnimator\n'
        f'                android:propertyName="trimPathEnd"\n'
        f'                android:duration="{STROKE_DURATION_MS}"\n'
        f'                android:valueFrom="0"\n'
        f'                android:valueTo="1"\n'
        f'                android:startOffset="{i * STROKE_DURATION_MS}"/>\n'
        f'        </aapt:attr>\n'
        f'    </target>'
        for i in range(n)
    )

    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<animated-vector\n'
        '    xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt">\n'
        '    <aapt:attr name="android:drawable">\n'
        f'        <vector\n'
        f'            android:width="220dp"\n'
        f'            android:height="220dp"\n'
        f'            android:viewportWidth="{VIEWPORT}"\n'
        f'            android:viewportHeight="{VIEWPORT}">\n'
        f'{path_elements}\n'
        '        </vector>\n'
        '    </aapt:attr>\n'
        f'{target_blocks}\n'
        '</animated-vector>\n'
    )


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    successes = 0
    failures: list[tuple[str, str, str]] = []

    for cp, suffix in KANA_TABLE:
        src = RAW_DIR / f"{cp}.svg"
        dst = OUT_DIR / f"stroke_{suffix}.xml"
        if not src.exists():
            failures.append((cp, suffix, "missing raw SVG"))
            continue
        try:
            paths = extract_paths(src)
            if not paths:
                failures.append((cp, suffix, "no stroke paths"))
                continue
            xml = build_avd_xml(paths)
            dst.write_text(xml, encoding="utf-8")
            successes += 1
            print(f"OK   {cp} -> {dst.name}  ({len(paths)} strokes)")
        except Exception as e:
            failures.append((cp, suffix, repr(e)))

    print(f"\n--- {successes} success / {len(failures)} failures ---")
    for cp, suf, why in failures:
        print(f"  FAIL {cp} ({suf}): {why}")

    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
