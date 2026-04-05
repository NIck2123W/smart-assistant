package com.example.smartassistant.vision
import android.content.Context
import androidx.camera.core.ImageAnalysis
import com.example.smartassistant.core.DetectionResult

interface DetectionModule : ImageAnalysis.Analyzer {
    suspend fun initialize(context: Context)
    fun release()
}