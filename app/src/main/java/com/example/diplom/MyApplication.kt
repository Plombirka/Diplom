// src/main/java/com/example/diplom/MyApplication.kt
package com.example.diplom

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.diplom.viewmodels.NewsViewModel

// 1. MyApplication должен реализовывать ViewModelStoreOwner
class MyApplication : Application(), ViewModelStoreOwner {

    // 2. Создаем ViewModelStore
    private lateinit var appViewModelStore: ViewModelStore

    // 3. Создаем переменную для NewsViewModel
    private lateinit var newsViewModel: NewsViewModel

    override fun onCreate() {
        super.onCreate()
        // 4. Инициализируем ViewModelStore
        appViewModelStore = ViewModelStore()

        // 5. Создаем кастомную фабрику для NewsViewModel
        // Эта фабрика знает, как передать 'application' в конструктор NewsViewModel
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return NewsViewModel(this@MyApplication) as T // Передаем ссылку на Application
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        // 6. Инициализируем NewsViewModel, используя кастомную фабрику и наш ViewModelStore
        // Это создаст NewsViewModel один раз для всего жизненного цикла приложения.
        newsViewModel = ViewModelProvider(this, factory).get(NewsViewModel::class.java)
    }

    // 7. Предоставляем метод для получения NewsViewModel из MyApplication
    fun getNewsViewModel(): NewsViewModel {
        return newsViewModel
    }

    // 8. Переопределяем метод getViewModelStore() из ViewModelStoreOwner
    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}