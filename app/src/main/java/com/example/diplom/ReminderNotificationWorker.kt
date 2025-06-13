// ReminderNotificationWorker.kt
package com.example.diplom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log

class ReminderNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val discipline = inputData.getString("discipline") ?: return Result.failure()
        val time = inputData.getString("time") ?: return Result.failure()
        val type = inputData.getString("type")
        val teacher = inputData.getString("teacher")
        val audience = inputData.getString("audience")


        val channelId = "reminder_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Напоминания", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Предстоящее занятие")
            .setContentText("Пара: $discipline в $time")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Пара: $discipline\nВремя: $time\nТип: $type\nПреподаватель: $teacher\nАудитория: $audience"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        return Result.success()
    }
}