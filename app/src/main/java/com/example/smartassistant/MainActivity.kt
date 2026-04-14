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

    private lateinit var cameraModule: CameraModule
    private lateinit var audioModule: AudioModule
    private lateinit var detectionModule: DetectionModule
    private lateinit var powerSaverManager: PowerSaverManager

    // 統一變數名稱為 navViewModel
    private val navViewModel: NavigationViewModel by viewModels()

    // Google 登入相關
    private lateinit var gso: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient

    private var lastSpokenTime = 0L
    private var previewView: PreviewView? = null
    private var calendarUiCallback: ((String, List<CourseEvent>) -> Unit)? = null

    // 權限請求清單
    private val requiredPermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true &&
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startSystem()
        } else {
            Toast.makeText(this, "需要相機與定位權限", Toast.LENGTH_LONG).show()
        }
    }

    // 處理 Google 登入回傳
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "登入成功", Toast.LENGTH_SHORT).show()
        }
    }

    private val startAuthorizationIntent = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Google 授權成功", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 Google 登入選項
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(Scopes.DRIVE_APPFOLDER))
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        powerSaverManager = PowerSaverManager(this)
        cameraModule = PhoneCameraModule(this)
        audioModule = PhoneAudioModule(this)

        detectionModule = MediaPipeDetectionModule { results ->
            if (powerSaverManager.shouldReducePerformance()) return@MediaPipeDetectionModule
            val target = results.maxByOrNull { it.confidence }
            target?.let {
                val now = System.currentTimeMillis()
                if (it.confidence > 0.5f && now - lastSpokenTime > 5000) {
                    audioModule.speak("前方有 ${it.label}")
                    lastSpokenTime = now
                }
            }
        }

        setContent {
            SmartassistantTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                val isNavigating = navViewModel.isNavigating

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Text("👁️") },
                                label = { Text("助理/導航") },
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            )
                            NavigationBarItem(
                                icon = { Text("📅") },
                                label = { Text("行事曆") },
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                        // 導航與助理模式 (Tab 0)
                        if (selectedTab == 0) {
                            // 相機畫面底圖
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        this@MainActivity.previewView = this
                                        startSystem()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isNavigating) {
                                NavHudScreen(navViewModel)
                            } else {
                                AssistantAndNavView()
                            }
                        }

                        // 行事曆模式 (Tab 1)
                        if (selectedTab == 1) {
                            CalendarRealScreen(
                                onRegisterCallback = { calendarUiCallback = it },
                                onSyncClick = { requestCalendarAuth() },
                                onCreateClick = { t, l, s, e -> requestCalendarAuth() }
                            )
                        }
                    }
                }
            }
        }
        requestPermissionLauncher.launch(requiredPermissions)
    }

    @Composable
    private fun AssistantAndNavView() {
        var addressInput by remember { mutableStateOf("") }
        val locationService = remember { LocationService(this@MainActivity) }
        val scope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 40.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("要去哪裡？") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val target = locationService.getCoordsFromAddress(addressInput)
                                val current = locationService.getCurrentLocation()
                                val key = getGoogleMapsApiKey()
                                if (target != null && current != null) {
                                    navViewModel.startNavigation(current, target, addressInput, key)
                                } else {
                                    Toast.makeText(this@MainActivity, "地址解析失敗", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("開始導航")
                    }
                }
            }
        }
    }

    private fun requestCalendarAuth() {
        // 先執行 Google 登入
        val intent = googleSignInClient.signInIntent
        signInLauncher.launch(intent)

        // 再執行 Identity 授權
        val requestedScopes = listOf(
            com.google.android.gms.common.api.Scope(Scopes.OPEN_ID),
            com.google.android.gms.common.api.Scope(Scopes.EMAIL),
            com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar")
        )
        val authRequest = AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build()
        Identity.getAuthorizationClient(this).authorize(authRequest)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val sender = result.pendingIntent!!.intentSender
                    startAuthorizationIntent.launch(IntentSenderRequest.Builder(sender).build())
                }
            }
    }

    private fun getGoogleMapsApiKey(): String {
        return try {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            ai.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) { "" }
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