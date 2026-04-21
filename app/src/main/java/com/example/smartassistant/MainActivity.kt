package com.example.smartassistant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.smartassistant.Audio.AudioModule
import com.example.smartassistant.Audio.PhoneAudioModule
import com.example.smartassistant.calendar.CalendarRealScreen
import com.example.smartassistant.calendar.CourseEvent
import com.example.smartassistant.camera.CameraModule
import com.example.smartassistant.camera.PhoneCameraModule
import com.example.smartassistant.location.LocationService
import com.example.smartassistant.location.NavigationViewModel
import com.example.smartassistant.power.PowerSaverManager
import com.example.smartassistant.ui.theme.NavHudScreen
import com.example.smartassistant.ui.theme.SmartassistantTheme
import com.example.smartassistant.vision.DetectionModule
import com.example.smartassistant.vision.MediaPipeDetectionModule
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // 模組宣告
    private lateinit var cameraModule: CameraModule
    private lateinit var audioModule: AudioModule
    private lateinit var detectionModule: DetectionModule
    private lateinit var powerSaverManager: PowerSaverManager
    private val navViewModel: NavigationViewModel by viewModels()

    // Google Login 宣告
    private lateinit var gso: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient

    private var lastSpokenTime = 0L
    private var previewView: PreviewView? = null

    // 權限請求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startSystem()
        } else {
            Toast.makeText(this, "權限不足，部分功能無法運作", Toast.LENGTH_LONG).show()
        }
    }

    // Google 授權與登入回呼
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private val startAuthorizationIntent = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化基礎模組
        powerSaverManager = PowerSaverManager(this)
        cameraModule = PhoneCameraModule(this)
        audioModule = PhoneAudioModule(this)

        // 2. 初始化 EfficientDet 辨識模組 (帶入語音回饋邏輯)
        detectionModule = MediaPipeDetectionModule { results ->
            if (powerSaverManager.shouldReducePerformance()) return@MediaPipeDetectionModule

            // 找出信心度最高的物品
            val target = results.maxByOrNull { it.confidence }
            target?.let {
                val now = System.currentTimeMillis()
                // 信心度 > 50% 且距離上次說話超過 5 秒才發聲
                if (it.confidence > 0.5f && now - lastSpokenTime > 5000) {
                    audioModule.speak("前方有 ${it.label}")
                    lastSpokenTime = now
                }
            }
        }

        // 3. 設定 Google 登入 (行事曆用)
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            SmartassistantTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                val isNavigating = navViewModel.isNavigating

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Text("👁️") }, label = { Text("導航/辨識") },
                                selected = selectedTab == 0, onClick = { selectedTab = 0 }
                            )
                            NavigationBarItem(
                                icon = { Text("📅") }, label = { Text("行事曆") },
                                selected = selectedTab == 1, onClick = { selectedTab = 1 }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                        // 分頁 0: 視覺導航 + 物件辨識
                        if (selectedTab == 0) {
                            // 底層：相機畫面
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        this@MainActivity.previewView = this
                                        startSystem() // 啟動相機與辨識
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // 上層：根據是否正在導航切換 UI
                            if (isNavigating) {
                                NavHudScreen(navViewModel) // 顯示導航箭頭與小地圖
                            } else {
                                AssistantInputView() // 顯示目的地搜尋框
                            }
                        }

                        // 分頁 1: Google 行事曆
                        if (selectedTab == 1) {
                            CalendarRealScreen(
                                onRegisterCallback = { /* 處理回呼 */ },
                                onSyncClick = { requestCalendarAuth() },
                                onCreateClick = { _, _, _, _ -> requestCalendarAuth() }
                            )
                        }
                    }
                }
            }
        }

        // 要求權限
        val permissions = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    @Composable
    fun AssistantInputView() {
        var address by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        val locationService = remember { LocationService(this) }

        Column(modifier = Modifier.padding(16.dp).padding(top = 32.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        label = { Text("請輸入目的地") }, modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val target = locationService.getCoordsFromAddress(address)
                                val current = locationService.getCurrentLocation()
                                if (target != null && current != null) {
                                    navViewModel.startNavigation(current, target, address, getGoogleMapsApiKey())
                                } else {
                                    Toast.makeText(this@MainActivity, "找不到位置", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("開啟視覺導航") }
                }
            }
        }
    }

    private fun requestCalendarAuth() {
        val intent = googleSignInClient.signInIntent
        signInLauncher.launch(intent)

        val requestedScopes = listOf(com.google.android.gms.common.api.Scope(Scopes.EMAIL), com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar"))
        val authRequest = AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build()
        Identity.getAuthorizationClient(this).authorize(authRequest).addOnSuccessListener { result ->
            if (result.hasResolution()) {
                startAuthorizationIntent.launch(IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build())
            }
        }
    }

    private fun getGoogleMapsApiKey(): String {
        val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return ai.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    }

    private fun startSystem() {
        lifecycleScope.launch {
            audioModule.initialize(this@MainActivity)
            detectionModule.initialize(this@MainActivity)
            cameraModule.initialize(this@MainActivity)
            cameraModule.setImageAnalyzer(detectionModule)
            previewView?.let { cameraModule.startPreview(it.surfaceProvider) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraModule.release()
        audioModule.release()
        detectionModule.release()
    }
}