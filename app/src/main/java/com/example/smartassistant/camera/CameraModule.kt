package com.example.smartassistant.camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner

interface CameraModule {
    suspend fun initialize(lifecycleOwner: LifecycleOwner)
    fun startPreview(surfaceProvider: Preview.SurfaceProvider)
    fun setImageAnalyzer(analyzer: ImageAnalysis.Analyzer)
    fun release()
}