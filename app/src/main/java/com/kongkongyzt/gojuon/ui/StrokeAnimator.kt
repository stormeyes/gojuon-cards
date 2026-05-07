package com.kongkongyzt.gojuon.ui

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

private class AvdRef {
    var view: ImageView? = null
    var avd: AnimatedVectorDrawableCompat? = null
}

/**
 * 渲染笔顺 AVD,**始终显示**(默认是完整字形),playToken 增量时触发"擦掉再画"动画。
 *
 * 设计要点:
 * - AVD 内部 path `trimPathEnd` 默认 = 1(完整字形),所以未播动画时也是清晰的字
 * - playToken 从 0 → 1 → 2 → ...,每次变都触发 stop()+start() 重播
 * - playToken == 0 时不播动画,只静态显示
 * - 切到新卡片(drawableRes 变)→ 重新加载 drawable,新卡片回到静态完整字形
 * - 主题色变化(strokeColor 变,亮↔深)→ 重载并重新 setTint
 *
 * AVD 内部 strokeColor 写死黑色,运行时用 [setTint] 染成主题 onSurface,
 * 保证亮色模式黑、深色模式白。
 */
@Composable
fun StrokeAnimator(
    @DrawableRes drawableRes: Int,
    playToken: Int,
    modifier: Modifier = Modifier,
    strokeColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (drawableRes == 0) return
    val ref = remember { AvdRef() }
    val tintArgb = strokeColor.toArgb()

    AndroidView(
        factory = { ctx -> ImageView(ctx).also { ref.view = it } },
        modifier = modifier,
    )

    // 加载/重载 drawable:首次组合 + drawableRes 或 tint 变化时
    LaunchedEffect(drawableRes, tintArgb) {
        val iv = ref.view ?: return@LaunchedEffect
        val avd = AnimatedVectorDrawableCompat.create(iv.context, drawableRes)
            ?: return@LaunchedEffect
        avd.setTint(tintArgb)
        iv.setImageDrawable(avd)
        ref.avd = avd
    }

    // 播放动画:仅当 playToken 增量到 > 0 时
    LaunchedEffect(playToken) {
        if (playToken == 0) return@LaunchedEffect
        ref.avd?.let {
            it.stop()   // 先停,确保从头开始(start() 对正在跑的 AVD 是 no-op)
            it.start()
        }
    }
}
