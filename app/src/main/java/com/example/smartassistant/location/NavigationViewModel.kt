package com.example.smartassistant.location

import android.text.Html
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

// 1. 資料模型保持不變
data class NavInstruction(val text: String, val distance: String, val maneuver: String)

class NavigationViewModel : ViewModel() {

    // 🔴 錯誤修正：刪除 private val navigationViewModel: NavigationViewModel by viewModels()
    // ViewModel 內部不需要也不可以再次宣告自己。

    var destinationName by mutableStateOf("")
    var isNavigating by mutableStateOf(false)

    // 儲存整條路線的步驟
    var allSteps = mutableStateListOf<NavInstruction>()
    var currentStepIndex by mutableStateOf(0)

    // 目前要顯示的步驟
    val currentInstruction: NavInstruction
        get() = if (allSteps.isNotEmpty() && currentStepIndex < allSteps.size)
            allSteps[currentStepIndex]
        else NavInstruction("讀取路線中...", "", "straight")

    fun startNavigation(origin: LatLng, dest: LatLng, name: String, apiKey: String) {
        destinationName = name
        isNavigating = true
        allSteps.clear()
        currentStepIndex = 0

        viewModelScope.launch {
            try {
                val originStr = "${origin.latitude},${origin.longitude}"
                val destStr = "${dest.latitude},${dest.longitude}"

                // 呼叫 Google API
                val response = RetrofitClient.apiService.getDirections(originStr, destStr, apiKey)

                val steps = response.routes.firstOrNull()?.legs?.firstOrNull()?.steps
                if (steps != null) {
                    // 把 Google 的資料轉成我們的 UI 模型
                    val parsedSteps = steps.map { step ->
                        // 處理 HTML 標籤
                        val cleanText = Html.fromHtml(step.html_instructions, Html.FROM_HTML_MODE_COMPACT).toString()
                        NavInstruction(
                            text = cleanText,
                            distance = step.distance.text,
                            // 將 maneuver 統一小寫處理，方便 UI 判斷
                            maneuver = (step.maneuver ?: "straight").lowercase()
                        )
                    }
                    allSteps.addAll(parsedSteps)
                }
            } catch (e: Exception) {
                Log.e("Navigation", "API 錯誤: ${e.message}")
                allSteps.clear()
                allSteps.add(NavInstruction("路線獲取失敗，請檢查 API Key", "", "error"))
            }
        }
    }

    // 手動切換下一步
    fun nextStep() {
        if (currentStepIndex < allSteps.size - 1) {
            currentStepIndex++
        } else {
            // 到達終點，自動停止
            stopNavigation()
        }
    }

    fun stopNavigation() {
        isNavigating = false
        allSteps.clear()
        currentStepIndex = 0
    }
}