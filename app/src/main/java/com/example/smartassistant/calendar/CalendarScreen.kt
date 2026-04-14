package com.example.smartassistant.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 將你原本的 CourseEvent 留在這裡
data class CourseEvent(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String
)

enum class CalendarAction { SYNC, CREATE }

@Composable
fun CalendarRealScreen(
    onRegisterCallback: (((String, List<CourseEvent>) -> Unit)) -> Unit,
    onSyncClick: () -> Unit,
    onCreateClick: (String, String, String, String) -> Unit
) {
    var syncStatus by remember { mutableStateOf("尚未同步") }
    var courseList by remember { mutableStateOf<List<CourseEvent>>(emptyList()) }

    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onRegisterCallback { status, list ->
            syncStatus = status
            courseList = list
        }
    }

    val nextCourse = courseList.firstOrNull()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "智慧行程助理", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "同步狀態：$syncStatus")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onSyncClick) {
            Text("同步 Google Calendar")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "新增事件", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("事件名稱") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = location, onValueChange = { location = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("地點") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = startTime, onValueChange = { startTime = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("開始時間（格式：21:00）") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = endTime, onValueChange = { endTime = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("結束時間（格式：22:00）") }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (title.isNotBlank() && location.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                    onCreateClick(title, location, startTime, endTime)
                }
            }
        ) {
            Text("新增到 Google Calendar")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "下一堂課", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (nextCourse != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(nextCourse.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("時間：${nextCourse.startTime} - ${nextCourse.endTime}")
                    Text("地點：${nextCourse.location}")
                }
            }
        } else {
            Text("目前沒有課程資料")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "今日課程", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (courseList.isEmpty()) {
            Text("尚未取得今日課程")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                courseList.forEach { course ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(course.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("時間：${course.startTime} - ${course.endTime}")
                            Text("地點：${course.location}")
                        }
                    }
                }
            }
        }
        // 為了不被 BottomBar 擋住，底部留一點白
        Spacer(modifier = Modifier.height(80.dp))
    }

}