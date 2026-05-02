package com.kongkongyzt.gojuon.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
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
    private var enhancer: LoudnessEnhancer? = null

    fun play(@RawRes resId: Int) {
        if (resId == 0) return  // 没有资源,静默
        // 停掉上一条
        current?.let {
            try { it.stop() } catch (_: Throwable) {}
            it.release()
        }
        enhancer?.release()
        enhancer = null

        val player = MediaPlayer.create(appContext, resId) ?: return
        // Kyoko 录的假名峰值约 -11 dBFS,默认音量在 Android 媒体流上偏小;
        // 用 LoudnessEnhancer 加 ~15 dB 提升(自带 compressor 防削波)。
        enhancer = runCatching {
            LoudnessEnhancer(player.audioSessionId).apply {
                setTargetGain(1500)
                enabled = true
            }
        }.getOrNull()
        player.setOnCompletionListener {
            enhancer?.release()
            enhancer = null
            it.release()
            if (current === it) current = null
        }
        current = player
        player.start()
    }

    fun shutdown() {
        try {
            enhancer?.release()
            current?.stop()
            current?.release()
        } catch (_: Throwable) {
            // best effort
        }
        enhancer = null
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
