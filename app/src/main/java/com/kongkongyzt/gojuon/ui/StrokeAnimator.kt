package com.kongkongyzt.gojuon.ui

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

private class ImageViewRef { var view: ImageView? = null }

/**
 * 渲染并播放一次 AVD 笔顺动画。
 *
 * 调用方约定:此 Composable **只在需要播放时才组合进来**。
 * - `playToken == 0` → 调用方应该渲染静态 `Text(kana.char)`,**不**调用此 Composable。
 * - `playToken > 0` → 渲染此 Composable;每次 `playToken` 增量都会重置并重播一次。
 *   父组件因其它原因(例如开关重组)的 recomposition **不会**误触发重播 ——
 *   播放由 `LaunchedEffect(playToken, drawableRes)` 驱动,只在 key 变化时执行。
 */
@Composable
fun StrokeAnimator(
    @DrawableRes drawableRes: Int,
    playToken: Int,
    modifier: Modifier = Modifier,
) {
    if (drawableRes == 0 || playToken == 0) return
    val ref = remember { ImageViewRef() }

    AndroidView(
        factory = { ctx -> ImageView(ctx).also { ref.view = it } },
        modifier = modifier,
    )

    LaunchedEffect(playToken, drawableRes) {
        val iv = ref.view ?: return@LaunchedEffect
        val avd = AnimatedVectorDrawableCompat.create(iv.context, drawableRes)
            ?: return@LaunchedEffect
        iv.setImageDrawable(avd)
        avd.start()
    }
}
