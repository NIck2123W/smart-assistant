package com.example.smartassistant.calendar

// 定義行程的資料結構
data class EventModel(
    val id: String = java.util.UUID.randomUUID().toString(), // 唯一識別碼
    val title: String,                                       // 行程標題
    val location: String = "",                               // 地點
    val description: String = "",                            // 備註
    val startTimeMillis: Long,                               // 開始時間 (毫秒)
    val remindBeforeMinutes: Int = 10,                       // 提前幾分鐘提醒 (預設10)
    val isSyncedWithGoogle: Boolean = false                  // 是否已成功同步到 Google Calendar
)