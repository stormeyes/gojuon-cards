package com.kongkongyzt.gojuon.data

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.kongkongyzt.gojuon.R

/**
 * 一个清音平假名条目。
 *
 * @param char 假名字符,例: "あ"
 * @param romaji 罗马字(Hepburn),例: "a"、"shi"、"tsu"、"chi"、"fu"、"wo"、"n"
 * @param row 所在行,例: "あ行"、"か行"、"わ行"、"ん行"
 * @param audioRes 发音音频资源 ID(预录的 .m4a, macOS Kyoko voice 生成)
 * @param drawableRes 笔顺动画 AVD 资源 ID(KanjiVG 生成)
 */
data class Kana(
    val char: String,
    val romaji: String,
    val row: String,
    @RawRes val audioRes: Int,
    @DrawableRes val drawableRes: Int,
)

/**
 * 46 个清音平假名,按五十音表标准顺序。
 * 顺序:あ行 → か行 → さ行 → た行 → な行 → は行 → ま行 → や行 → ら行 → わ行 → ん。
 */
val GOJUON: List<Kana> = listOf(
    // あ行
    Kana("あ", "a", "あ行", R.raw.kana_a, R.drawable.stroke_a),
    Kana("い", "i", "あ行", R.raw.kana_i, R.drawable.stroke_i),
    Kana("う", "u", "あ行", R.raw.kana_u, R.drawable.stroke_u),
    Kana("え", "e", "あ行", R.raw.kana_e, R.drawable.stroke_e),
    Kana("お", "o", "あ行", R.raw.kana_o, R.drawable.stroke_o),
    // か行
    Kana("か", "ka", "か行", R.raw.kana_ka, R.drawable.stroke_ka),
    Kana("き", "ki", "か行", R.raw.kana_ki, R.drawable.stroke_ki),
    Kana("く", "ku", "か行", R.raw.kana_ku, R.drawable.stroke_ku),
    Kana("け", "ke", "か行", R.raw.kana_ke, R.drawable.stroke_ke),
    Kana("こ", "ko", "か行", R.raw.kana_ko, R.drawable.stroke_ko),
    // さ行
    Kana("さ", "sa", "さ行", R.raw.kana_sa, R.drawable.stroke_sa),
    Kana("し", "shi", "さ行", R.raw.kana_shi, R.drawable.stroke_shi),
    Kana("す", "su", "さ行", R.raw.kana_su, R.drawable.stroke_su),
    Kana("せ", "se", "さ行", R.raw.kana_se, R.drawable.stroke_se),
    Kana("そ", "so", "さ行", R.raw.kana_so, R.drawable.stroke_so),
    // た行
    Kana("た", "ta", "た行", R.raw.kana_ta, R.drawable.stroke_ta),
    Kana("ち", "chi", "た行", R.raw.kana_chi, R.drawable.stroke_chi),
    Kana("つ", "tsu", "た行", R.raw.kana_tsu, R.drawable.stroke_tsu),
    Kana("て", "te", "た行", R.raw.kana_te, R.drawable.stroke_te),
    Kana("と", "to", "た行", R.raw.kana_to, R.drawable.stroke_to),
    // な行
    Kana("な", "na", "な行", R.raw.kana_na, R.drawable.stroke_na),
    Kana("に", "ni", "な行", R.raw.kana_ni, R.drawable.stroke_ni),
    Kana("ぬ", "nu", "な行", R.raw.kana_nu, R.drawable.stroke_nu),
    Kana("ね", "ne", "な行", R.raw.kana_ne, R.drawable.stroke_ne),
    Kana("の", "no", "な行", R.raw.kana_no, R.drawable.stroke_no),
    // は行
    Kana("は", "ha", "は行", R.raw.kana_ha, R.drawable.stroke_ha),
    Kana("ひ", "hi", "は行", R.raw.kana_hi, R.drawable.stroke_hi),
    Kana("ふ", "fu", "は行", R.raw.kana_fu, R.drawable.stroke_fu),
    Kana("へ", "he", "は行", R.raw.kana_he, R.drawable.stroke_he),
    Kana("ほ", "ho", "は行", R.raw.kana_ho, R.drawable.stroke_ho),
    // ま行
    Kana("ま", "ma", "ま行", R.raw.kana_ma, R.drawable.stroke_ma),
    Kana("み", "mi", "ま行", R.raw.kana_mi, R.drawable.stroke_mi),
    Kana("む", "mu", "ま行", R.raw.kana_mu, R.drawable.stroke_mu),
    Kana("め", "me", "ま行", R.raw.kana_me, R.drawable.stroke_me),
    Kana("も", "mo", "ま行", R.raw.kana_mo, R.drawable.stroke_mo),
    // や行
    Kana("や", "ya", "や行", R.raw.kana_ya, R.drawable.stroke_ya),
    Kana("ゆ", "yu", "や行", R.raw.kana_yu, R.drawable.stroke_yu),
    Kana("よ", "yo", "や行", R.raw.kana_yo, R.drawable.stroke_yo),
    // ら行
    Kana("ら", "ra", "ら行", R.raw.kana_ra, R.drawable.stroke_ra),
    Kana("り", "ri", "ら行", R.raw.kana_ri, R.drawable.stroke_ri),
    Kana("る", "ru", "ら行", R.raw.kana_ru, R.drawable.stroke_ru),
    Kana("れ", "re", "ら行", R.raw.kana_re, R.drawable.stroke_re),
    Kana("ろ", "ro", "ら行", R.raw.kana_ro, R.drawable.stroke_ro),
    // わ行
    Kana("わ", "wa", "わ行", R.raw.kana_wa, R.drawable.stroke_wa),
    Kana("を", "wo", "わ行", R.raw.kana_wo, R.drawable.stroke_wo),
    // ん
    Kana("ん", "n", "ん行", R.raw.kana_n, R.drawable.stroke_n),
)

@Suppress("unused")
private val gojuonSizeCheck: Unit = run {
    require(GOJUON.size == 46) { "GOJUON must contain exactly 46 entries, got ${GOJUON.size}" }
}
