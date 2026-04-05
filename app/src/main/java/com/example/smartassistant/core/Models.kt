package com.example.smartassistant.core

import android.graphics.RectF

enum class SpeakPriority { LOW, NORMAL, HIGH }

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)