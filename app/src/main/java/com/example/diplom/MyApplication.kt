// src/main/java/com/example/diplom/MyApplication.kt
package com.example.diplom

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.work.*
import com.example.diplom.viewmodels.NewsViewModel
import java.time.LocalDateTime
import java.time.Duration

class MyApplication : Application(), ViewModelStoreOwner {

    private lateinit var appViewModelStore: ViewModelStore
    private lateinit var newsViewModel: NewsViewModel

    override fun onCreate() {
        super.onCreate()
        // Инициализация ViewModelStore
        appViewModelStore = ViewModelStore()

        // Создание кастомной фабрики для NewsViewModel
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return NewsViewModel(this@MyApplication) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        // Инициализация NewsViewModel
        newsViewModel = ViewModelProvider(this, factory).get(NewsViewModel::class.java)

        // Планирование ежедневного Worker для напоминаний
        scheduleDailyWorker()
    }

    // Метод для получения NewsViewModel
    fun getNewsViewModel(): NewsViewModel {
        return newsViewModel
    }

    // Реализация ViewModelStoreOwner
    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    // Планирование ежедневного Worker
    private fun scheduleDailyWorker() {
        val dailyWorkRequest = PeriodicWorkRequestBuilder<ReminderSchedulerWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.DAYS
        )
            .setInitialDelay(calculateInitialDelay(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reminder_scheduler",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }

    // Расчёт задержки до следующего запуска (00:01 следующего дня)
    private fun calculateInitialDelay(): Long {
        val now = LocalDateTime.now()
        val nextRun = now.withHour(0).withMinute(1).withSecond(0).plusDays(1)
        return Duration.between(now, nextRun).toMillis()
    }
}