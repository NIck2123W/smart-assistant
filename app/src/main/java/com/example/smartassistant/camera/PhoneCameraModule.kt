package com.example.smartassistant.camera
import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PhoneCameraModule(private val context: Context) : CameraModule {
    private var provider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var owner: LifecycleOwner

    override suspend fun initialize(lifecycleOwner: LifecycleOwner) {
        this.owner = lifecycleOwner
        provider = suspendCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
        }
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
    }

    override fun startPreview(surfaceProvider: Preview.SurfaceProvider) {
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
        provider?.unbindAll()
        provider?.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
    }

    override fun setImageAnalyzer(analyzer: ImageAnalysis.Analyzer) {
        imageAnalysis?.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
    }

    override fun release() { provider?.unbindAll(); provider = null }
}