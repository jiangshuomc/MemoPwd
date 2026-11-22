package com.example.memopwd

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MemoPwdApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REMINDER,
                "备忘录提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "待办事项到期提醒"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_REMINDER = "memo_reminder"
    }
}
