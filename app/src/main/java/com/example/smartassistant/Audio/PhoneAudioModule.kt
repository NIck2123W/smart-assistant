package com.example.smartassistant.audio

import android.content.Context
import android.media.RingtoneManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.smartassistant.core.SpeakPriority
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PhoneAudioModule(private val context: Context) : AudioModule {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    override suspend fun initialize(context: Context) = suspendCoroutine<Unit> { continuation ->
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.TRADITIONAL_CHINESE
                isInitialized = true
            } else {
                Log.e("AudioModule", "TTS 初始化失敗")
            }
            continuation.resume(Unit)
        }
    }

    override fun speak(text: String, priority: SpeakPriority) {
        if (!isInitialized) return
        val queueMode = if (priority == SpeakPriority.HIGH) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "ID_${System.currentTimeMillis()}")
    }

    override fun stopSpeaking() { tts?.stop() }

    override fun playNotificationSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, uri).play()
        } catch (e: Exception) {
            Log.e("AudioModule", "音效播放失敗: ${e.message}")
        }
    }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}