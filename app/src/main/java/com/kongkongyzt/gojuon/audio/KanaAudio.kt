package com.kongkongyzt.gojuon.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 假名发音播放器(基于 MediaPlayer + raw 资源)。
 * 用法:`val audio = rememberKanaAudio(); audio.play(R.raw.kana_a)`
 *
 * - 同时只播一条:每次 [play] 会停掉上一条
 * - 离开 Composition 时自动 release()
 */
class KanaAudio(private val appContext: Context) {
    private var current: MediaPlayer? = null

    fun play(@RawRes resId: Int) {
        if (resId == 0) return  // 没有资源,静默
        // 停掉上一条
        current?.let {
            try { it.stop() } catch (_: Throwable) {}
            it.release()
        }
        current = MediaPlayer.create(appContext, resId)?.apply {
            setOnCompletionListener {
                it.release()
                if (current === it) current = null
            }
            start()
        }
    }

    fun shutdown() {
        try {
            current?.stop()
            current?.release()
        } catch (_: Throwable) {
            // best effort
        }
        current = null
    }
}

@Composable
fun rememberKanaAudio(): KanaAudio {
    val ctx = LocalContext.current.applicationContext
    val instance = remember(ctx) { KanaAudio(ctx) }
    DisposableEffect(instance) {
        onDispose { instance.shutdown() }
    }
    return instance
}
