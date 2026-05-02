#!/usr/bin/env bash
# Downloads KanjiVG SVGs for the 46 清音 hiragana into scripts/kanjivg_raw/.
# License: KanjiVG is CC-BY-SA-3.0, see https://kanjivg.tagaini.net/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAW_DIR="$SCRIPT_DIR/kanjivg_raw"
mkdir -p "$RAW_DIR"

# 46 hiragana codepoints (lowercase hex, 5 digits, matching KanjiVG file naming)
CODEPOINTS=(
  03042 03044 03046 03048 0304a    # あいうえお
  0304b 0304d 0304f 03051 03053    # かきくけこ
  03055 03057 03059 0305b 0305d    # さしすせそ
  0305f 03061 03064 03066 03068    # たちつてと
  0306a 0306b 0306c 0306d 0306e    # なにぬねの
  0306f 03072 03075 03078 0307b    # はひふへほ
  0307e 0307f 03080 03081 03082    # まみむめも
  03084 03086 03088                # やゆよ
  03089 0308a 0308b 0308c 0308d    # らりるれろ
  0308f 03092                       # わを
  03093                             # ん
)

BASE_URL="https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji"

for cp in "${CODEPOINTS[@]}"; do
  out="$RAW_DIR/${cp}.svg"
  if [ -f "$out" ]; then
    echo "skip $cp (cached)"
    continue
  fi
  url="$BASE_URL/${cp}.svg"
  echo "fetch $cp ..."
  curl -fsSL --retry 3 -o "$out" "$url" || { echo "FAILED $cp"; exit 1; }
done

echo "Done: $(ls "$RAW_DIR" | wc -l | tr -d ' ') files in $RAW_DIR"
