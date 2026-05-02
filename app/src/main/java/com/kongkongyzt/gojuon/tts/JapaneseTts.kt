package com.kongkongyzt.gojuon.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kongkongyzt.gojuon.R
import java.util.Locale

/**
 * 日语 TTS 封装。生命周期跟 Composition 绑定(离开 Composition 时 shutdown)。
 * 用法: `val tts = rememberJapaneseTts(); tts.speak("あ")`
 *
 * 内部初始化是异步(TextToSpeech 回调式)。在 ready=false 期间,speak() 静默丢弃。
 * 当系统未装日语数据时,第一次 speak 会 Toast 提示一次。
 */
class JapaneseTts(private val appContext: Context) {
    private val tts: TextToSpeech
    @Volatile private var ready: Boolean = false
    @Volatile private var japaneseAvailable: Boolean = false
    @Volatile private var initFailed: Boolean = false
    private var hasShownUnavailableToast: Boolean = false

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                initFailed = true
            } else {
                val result = tts.setLanguage(Locale.JAPANESE)
                japaneseAvailable = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
            ready = true
        }
    }

    fun speak(text: String) {
        if (!ready) return  // 初始化未完成时静默丢弃(用户 0.5s 内再点就行)
        if (initFailed) {
            Toast.makeText(appContext, R.string.tts_init_failed, Toast.LENGTH_SHORT).show()
            return
        }
        if (!japaneseAvailable) {
            if (!hasShownUnavailableToast) {
                Toast.makeText(appContext, R.string.tts_unavailable, Toast.LENGTH_LONG).show()
                hasShownUnavailableToast = true
            }
            return
        }
        tts.stop()  // 中断上一次,避免连击堆积
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kana_${text.hashCode()}")
    }

    fun shutdown() {
        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Throwable) {
            // best effort
        }
    }
}

@Composable
fun rememberJapaneseTts(): JapaneseTts {
    val ctx = LocalContext.current.applicationContext
    val instance = remember(ctx) { JapaneseTts(ctx) }
    DisposableEffect(instance) {
        onDispose { instance.shutdown() }
    }
    return instance
}
