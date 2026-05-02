package com.kongkongyzt.gojuon.data

import androidx.annotation.DrawableRes

/**
 * 一个清音平假名条目。
 *
 * @param char 假名字符,例: "あ"
 * @param romaji 罗马字(Hepburn),例: "a"、"shi"、"tsu"、"chi"、"fu"、"wo"、"n"
 * @param row 所在行,例: "あ行"、"か行"、"わ行"、"ん行"
 * @param drawableRes 笔顺动画 AVD 资源 ID(在 stroke 数据生成后由 Phase 7 任务填入有效值;
 *                    占位期间用 0,UI 在 0 时把"笔顺"按钮置灰)
 */
data class Kana(
    val char: String,
    val romaji: String,
    val row: String,
    @DrawableRes val drawableRes: Int = 0,
)

/**
 * 46 个清音平假名,按五十音表标准顺序。
 * 顺序:あ行 → か行 → さ行 → た行 → な行 → は行 → ま行 → や行 → ら行 → わ行 → ん。
 */
val GOJUON: List<Kana> = listOf(
    // あ行
    Kana("あ", "a", "あ行"),
    Kana("い", "i", "あ行"),
    Kana("う", "u", "あ行"),
    Kana("え", "e", "あ行"),
    Kana("お", "o", "あ行"),
    // か行
    Kana("か", "ka", "か行"),
    Kana("き", "ki", "か行"),
    Kana("く", "ku", "か行"),
    Kana("け", "ke", "か行"),
    Kana("こ", "ko", "か行"),
    // さ行
    Kana("さ", "sa", "さ行"),
    Kana("し", "shi", "さ行"),
    Kana("す", "su", "さ行"),
    Kana("せ", "se", "さ行"),
    Kana("そ", "so", "さ行"),
    // た行
    Kana("た", "ta", "た行"),
    Kana("ち", "chi", "た行"),
    Kana("つ", "tsu", "た行"),
    Kana("て", "te", "た行"),
    Kana("と", "to", "た行"),
    // な行
    Kana("な", "na", "な行"),
    Kana("に", "ni", "な行"),
    Kana("ぬ", "nu", "な行"),
    Kana("ね", "ne", "な行"),
    Kana("の", "no", "な行"),
    // は行
    Kana("は", "ha", "は行"),
    Kana("ひ", "hi", "は行"),
    Kana("ふ", "fu", "は行"),
    Kana("へ", "he", "は行"),
    Kana("ほ", "ho", "は行"),
    // ま行
    Kana("ま", "ma", "ま行"),
    Kana("み", "mi", "ま行"),
    Kana("む", "mu", "ま行"),
    Kana("め", "me", "ま行"),
    Kana("も", "mo", "ま行"),
    // や行
    Kana("や", "ya", "や行"),
    Kana("ゆ", "yu", "や行"),
    Kana("よ", "yo", "や行"),
    // ら行
    Kana("ら", "ra", "ら行"),
    Kana("り", "ri", "ら行"),
    Kana("る", "ru", "ら行"),
    Kana("れ", "re", "ら行"),
    Kana("ろ", "ro", "ら行"),
    // わ行
    Kana("わ", "wa", "わ行"),
    Kana("を", "wo", "わ行"),
    // ん
    Kana("ん", "n", "ん行"),
)

@Suppress("unused")
private val gojuonSizeCheck: Unit = run {
    require(GOJUON.size == 46) { "GOJUON must contain exactly 46 entries, got ${GOJUON.size}" }
}
