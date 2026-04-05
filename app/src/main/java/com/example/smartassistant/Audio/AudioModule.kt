package com.example.smartassistant.Audio

import android.content.Context
import com.example.smartassistant.core.SpeakPriority

interface AudioModule {
    suspend fun initialize(context: Context)
    fun speak(text: String, priority: SpeakPriority = SpeakPriority.NORMAL)
    fun stopSpeaking()
    fun playNotificationSound()
    fun release()
}