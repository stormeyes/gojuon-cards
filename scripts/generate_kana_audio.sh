#!/usr/bin/env bash
# Generate 46 hiragana audio files using macOS `say -v Kyoko` (native Japanese voice).
# Output: app/src/main/res/raw/kana_<romaji>.m4a (AAC, ~5 KB each, ~230 KB total)
#
# Run from project root:
#   bash scripts/generate_kana_audio.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUT_DIR="$ROOT/app/src/main/res/raw"
TMP_DIR="$(mktemp -d)"
trap "rm -rf $TMP_DIR" EXIT

mkdir -p "$OUT_DIR"

# (kana, romaji) pairs in 五十音表 order — matches data/Kana.kt
KANA_PAIRS=(
  "あ:a"  "い:i"  "う:u"  "え:e"  "お:o"
  "か:ka" "き:ki" "く:ku" "け:ke" "こ:ko"
  "さ:sa" "し:shi" "す:su" "せ:se" "そ:so"
  "た:ta" "ち:chi" "つ:tsu" "て:te" "と:to"
  "な:na" "に:ni" "ぬ:nu" "ね:ne" "の:no"
  "は:ha" "ひ:hi" "ふ:fu" "へ:he" "ほ:ho"
  "ま:ma" "み:mi" "む:mu" "め:me" "も:mo"
  "や:ya" "ゆ:yu" "よ:yo"
  "ら:ra" "り:ri" "る:ru" "れ:re" "ろ:ro"
  "わ:wa" "を:wo"
  "ん:n"
)

count=0
for pair in "${KANA_PAIRS[@]}"; do
  kana="${pair%%:*}"
  romaji="${pair##*:}"
  aiff="$TMP_DIR/kana_${romaji}.aiff"
  m4a="$OUT_DIR/kana_${romaji}.m4a"
  # 末尾塞 400ms 静默 — 单个假名本身音节短,Kyoko 念完会立刻截断,
  # 加 silence 给一个"听完整"的尾韵
  say -v Kyoko -o "$aiff" "$kana[[slnc 400]]"
  afconvert -f m4af -d aac "$aiff" "$m4a" >/dev/null
  count=$((count + 1))
  printf "  [%2d/46] %s -> kana_%s.m4a\n" "$count" "$kana" "$romaji"
done

total_size=$(du -sh "$OUT_DIR" | cut -f1)
echo ""
echo "Done: $count files in $OUT_DIR (total $total_size)"
