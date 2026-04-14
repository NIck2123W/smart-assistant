package com.example.smartassistant.power

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

class PowerSaverManager(private val context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // 檢查系統是否開啟省電模式
    private fun isSystemPowerSaveMode(): Boolean = powerManager.isPowerSaveMode

    // 檢查電量是否過低 (低於 15%)
    private fun isBatteryLow(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return (level / scale.toFloat()) < 0.15f
    }

    // 綜合判斷：是否應該進入「低效能模式」以節省電力
    fun shouldReducePerformance(): Boolean {
        return isSystemPowerSaveMode() || isBatteryLow()
    }
}