package com.example.smartassistant.vision
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.example.smartassistant.core.DetectionResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector

class MediaPipeDetectionModule(private val onResults: (List<DetectionResult>) -> Unit) : DetectionModule {
    private var detector: ObjectDetector? = null

    override suspend fun initialize(context: Context) {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("efficientdet_lite0.tflite").build())
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                val detections = result.detections().map {
                    DetectionResult(it.categories()[0].categoryName(), it.categories()[0].score(), it.boundingBox())
                }
                onResults(detections)
            }.build()
        detector = ObjectDetector.createFromOptions(context, options)
    }

    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        detector?.detectAsync(BitmapImageBuilder(rotated).build(), SystemClock.uptimeMillis())
        imageProxy.close()
    }

    override fun release() { detector?.close(); detector = null }
}