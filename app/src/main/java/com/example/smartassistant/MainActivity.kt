package com.example.smartassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.smartassistant.Audio.AudioModule
import com.example.smartassistant.Audio.PhoneAudioModule
import com.example.smartassistant.camera.CameraModule
import com.example.smartassistant.camera.PhoneCameraModule
import com.example.smartassistant.ui.theme.SmartassistantTheme
import com.example.smartassistant.vision.DetectionModule
import com.example.smartassistant.vision.MediaPipeDetectionModule
import kotlinx.coroutines.launch

// 🌟 繼承 ComponentActivity 才能使用最新的 Compose UI
class MainActivity : ComponentActivity() {

    private lateinit var cameraModule: CameraModule
    private lateinit var audioModule: AudioModule
    private lateinit var detectionModule: DetectionModule
    private var lastSpokenTime = 0L

    // 用來裝相機畫面的容器
    private var previewView: PreviewView? = null

    // 權限請求發射器
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startSystem()
        } else {
            Toast.makeText(this, "需要相機權限才能運作", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化我們分層寫好的模組
        cameraModule = PhoneCameraModule(this)
        audioModule = PhoneAudioModule(this)

        // 🧠 核心 AI 邏輯：過濾掉人(person)，只報信心度大於 0.5 的物品，且每 5 秒最多講一次
        detectionModule = MediaPipeDetectionModule { results ->
            val otherObjects = results.filter { it.label != "person" }
            val target = otherObjects.maxByOrNull { it.confidence } ?: results.maxByOrNull { it.confidence }

            target?.let {
                val now = System.currentTimeMillis()
                if (it.confidence > 0.5f && now - lastSpokenTime > 5000) {
                    audioModule.speak("看到 ${it.label}")
                    lastSpokenTime = now
                }
            }
        }

        // 📱 這裡就是你的 UI！我們直接使用 Compose 來畫出相機預覽畫面
        setContent {
            SmartassistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // AndroidView 用來把傳統的相機視窗塞進 Compose 畫面裡
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView = it }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 檢查權限，有權限就啟動系統，沒權限就跳出詢問視窗
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startSystem()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 🚀 啟動所有模組
    private fun startSystem() {
        lifecycleScope.launch {
            audioModule.initialize(this@MainActivity)
            detectionModule.initialize(this@MainActivity)
            cameraModule.initialize(this@MainActivity)

            // 把 AI 辨識模組掛載到相機上
            cameraModule.setImageAnalyzer(detectionModule)

            // 確保畫面已經準備好，再把相機畫面投射上去
            previewView?.post {
                previewView?.let { cameraModule.startPreview(it.surfaceProvider) }
            }
        }
    }

    // 🧹 關閉 App 時釋放資源，避免記憶體外洩
    override fun onDestroy() {
        super.onDestroy()
        cameraModule.release()
        audioModule.release()
        detectionModule.release()
    }
}