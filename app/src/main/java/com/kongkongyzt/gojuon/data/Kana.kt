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
 * @param drawableRes 笔顺动画 AVD 资源 ID(在 stroke 数据生成后由 Phase 7 任务填入有效值;
 *                    占位期间用 0,UI 在 0 时把"笔顺"按钮置灰)
 */
data class Kana(
    val char: String,
    val romaji: String,
    val row: String,
    @RawRes val audioRes: Int,
    @DrawableRes val drawableRes: Int = 0,
)

/**
 * 46 个清音平假名,按五十音表标准顺序。
 * 顺序:あ行 → か行 → さ行 → た行 → な行 → は行 → ま行 → や行 → ら行 → わ行 → ん。
 */
val GOJUON: List<Kana> = listOf(
    // あ行
    Kana("あ", "a", "あ行", R.raw.kana_a),
    Kana("い", "i", "あ行", R.raw.kana_i),
    Kana("う", "u", "あ行", R.raw.kana_u),
    Kana("え", "e", "あ行", R.raw.kana_e),
    Kana("お", "o", "あ行", R.raw.kana_o),
    // か行
    Kana("か", "ka", "か行", R.raw.kana_ka),
    Kana("き", "ki", "か行", R.raw.kana_ki),
    Kana("く", "ku", "か行", R.raw.kana_ku),
    Kana("け", "ke", "か行", R.raw.kana_ke),
    Kana("こ", "ko", "か行", R.raw.kana_ko),
    // さ行
    Kana("さ", "sa", "さ行", R.raw.kana_sa),
    Kana("し", "shi", "さ行", R.raw.kana_shi),
    Kana("す", "su", "さ行", R.raw.kana_su),
    Kana("せ", "se", "さ行", R.raw.kana_se),
    Kana("そ", "so", "さ行", R.raw.kana_so),
    // た行
    Kana("た", "ta", "た行", R.raw.kana_ta),
    Kana("ち", "chi", "た行", R.raw.kana_chi),
    Kana("つ", "tsu", "た行", R.raw.kana_tsu),
    Kana("て", "te", "た行", R.raw.kana_te),
    Kana("と", "to", "た行", R.raw.kana_to),
    // な行
    Kana("な", "na", "な行", R.raw.kana_na),
    Kana("に", "ni", "な行", R.raw.kana_ni),
    Kana("ぬ", "nu", "な行", R.raw.kana_nu),
    Kana("ね", "ne", "な行", R.raw.kana_ne),
    Kana("の", "no", "な行", R.raw.kana_no),
    // は行
    Kana("は", "ha", "は行", R.raw.kana_ha),
    Kana("ひ", "hi", "は行", R.raw.kana_hi),
    Kana("ふ", "fu", "は行", R.raw.kana_fu),
    Kana("へ", "he", "は行", R.raw.kana_he),
    Kana("ほ", "ho", "は行", R.raw.kana_ho),
    // ま行
    Kana("ま", "ma", "ま行", R.raw.kana_ma),
    Kana("み", "mi", "ま行", R.raw.kana_mi),
    Kana("む", "mu", "ま行", R.raw.kana_mu),
    Kana("め", "me", "ま行", R.raw.kana_me),
    Kana("も", "mo", "ま行", R.raw.kana_mo),
    // や行
    Kana("や", "ya", "や行", R.raw.kana_ya),
    Kana("ゆ", "yu", "や行", R.raw.kana_yu),
    Kana("よ", "yo", "や行", R.raw.kana_yo),
    // ら行
    Kana("ら", "ra", "ら行", R.raw.kana_ra),
    Kana("り", "ri", "ら行", R.raw.kana_ri),
    Kana("る", "ru", "ら行", R.raw.kana_ru),
    Kana("れ", "re", "ら行", R.raw.kana_re),
    Kana("ろ", "ro", "ら行", R.raw.kana_ro),
    // わ行
    Kana("わ", "wa", "わ行", R.raw.kana_wa),
    Kana("を", "wo", "わ行", R.raw.kana_wo),
    // ん
    Kana("ん", "n", "ん行", R.raw.kana_n),
)

@Suppress("unused")
private val gojuonSizeCheck: Unit = run {
    require(GOJUON.size == 46) { "GOJUON must contain exactly 46 entries, got ${GOJUON.size}" }
}
