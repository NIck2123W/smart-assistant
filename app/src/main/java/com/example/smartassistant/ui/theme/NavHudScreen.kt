package com.example.smartassistant.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartassistant.location.NavigationViewModel
import com.google.maps.android.compose.*

@Composable
fun NavHudScreen(viewModel: NavigationViewModel) {
    // 外層 Box 不設定 background(Color.Black)，這樣才能看到相機畫面
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { viewModel.nextStep() } // 點擊螢幕任何地方切換下一步
    ) {
        // 1. 角落輔助地圖
        GoogleMap(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(180.dp)
                .padding(16.dp),
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        )

        // 2. 左上角退出按鈕
        IconButton(
            onClick = { viewModel.stopNavigation() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .statusBarsPadding() // 避免被劉海擋住
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "停止導航",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // 3. 中央大指令 (HUD)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 根據指令自動切換箭頭方向
            val arrowIcon = when (viewModel.currentInstruction.maneuver) {
                "turn-left" -> Icons.AutoMirrored.Filled.ArrowBack
                "turn-right" -> Icons.AutoMirrored.Filled.ArrowForward
                else -> Icons.Default.ArrowUpward
            }

            Icon(
                imageVector = arrowIcon,
                contentDescription = null,
                modifier = Modifier.size(150.dp),
                tint = Color(0xFF00E5FF).copy(alpha = 0.8f) // 科技青色 + 透明感
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = viewModel.currentInstruction.distance,
                fontSize = 60.sp,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = viewModel.currentInstruction.text,
                fontSize = 28.sp,
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // 4. 底部狀態列
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(100.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "導航至:", color = Color.Gray, fontSize = 12.sp)
                    Text(text = viewModel.destinationName, color = Color.White, fontSize = 18.sp)
                }
                Text(text = "點擊畫面下一步 ▶", color = Color(0xFF00E5FF), fontSize = 14.sp)
            }
        }
    }
}