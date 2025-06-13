package com.example.diplom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.diplom.viewmodels.NewsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.ArrayList
import kotlin.coroutines.resume

class SplashActivity : AppCompatActivity() {

    private val PREFS_NAME = "MyAppPrefs"
    private val KEY_OFFLINE_MODE_ENABLED = "offline_mode_enabled"
    private val KEY_OFFLINE_FILE_PATH = "offline_file_path"
    private val SCHEDULE_DOWNLOAD_URL = "http://212.109.221.255/files/Raspisanie.xlsx"

    private val client = createHttpClient()
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var retryButton: Button
    private lateinit var loadingTextView: TextView
    private lateinit var newsViewModel: NewsViewModel

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.statusTextView)
        retryButton = findViewById(R.id.retryButton)
        loadingTextView = findViewById(R.id.loadingText)
        newsViewModel = (application as MyApplication).getNewsViewModel()

        retryButton.setOnClickListener {
            loadingTextView.visibility = View.VISIBLE
            startLoading()
        }

        startLoading()
    }

    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .build()
    }

    private fun startLoading() {
        progressBar.visibility = View.VISIBLE
        statusTextView.visibility = View.GONE
        retryButton.visibility = View.GONE
        loadingTextView.text = "Загрузка данных..."

        lifecycleScope.launch {
            val groupsDeferred = async { loadGroupsData() }

            if (isInternetAvailable()) {
                Log.d("SplashActivity", "Internet available. Starting news load.")
                if (newsViewModel.posts.value.isNullOrEmpty() && newsViewModel.isLoading.value == false) {
                    newsViewModel.refreshPosts()
                }
                suspendUntilNewsLoadComplete()
                Log.d("SplashActivity", "News loading procedure finished.")
            } else {
                Log.d("SplashActivity", "No internet. Skipping news load.")
            }

            val groups = groupsDeferred.await()

            if (groups.isNotEmpty()) {
                navigateToMainActivity(groups)
            } else {
                showError("Не удалось загрузить данные расписания. Проверьте подключение или настройки офлайн-режима.")
            }
        }
    }

    private suspend fun loadGroupsData(): List<String> {
        return withContext(Dispatchers.IO) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isOffline = prefs.getBoolean(KEY_OFFLINE_MODE_ENABLED, false)
            val offlineFilePath = prefs.getString(KEY_OFFLINE_FILE_PATH, null)

            if (isOffline && offlineFilePath != null && File(offlineFilePath).exists()) {
                Log.d("SplashLoad", "Offline mode is ON. Using local file: $offlineFilePath")
                return@withContext extractGroupsFromExcel(offlineFilePath)
            }

            if (isInternetAvailable()) {
                Log.d("SplashLoad", "Offline mode is OFF or file not found. Downloading from internet.")
                val downloadedFilePath = downloadExcelFile(SCHEDULE_DOWNLOAD_URL)
                if (downloadedFilePath != null) {
                    return@withContext extractGroupsFromExcel(downloadedFilePath)
                }
            }

            Log.e("SplashLoad", "Failed to load groups data. No offline file and no internet.")
            return@withContext emptyList()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        statusTextView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        loadingTextView.visibility = View.GONE
        statusTextView.text = message
    }
    // Вспомогательная suspend-функция для ожидания завершения загрузки NewsViewModel
    private suspend fun suspendUntilNewsLoadComplete() = suspendCancellableCoroutine<Unit> { continuation ->
        val observer = object : androidx.lifecycle.Observer<Boolean> {
            override fun onChanged(isLoading: Boolean) {
                Log.d("SplashActivity", "NewsViewModel isLoading changed to: $isLoading")
                if (!isLoading) { // Если загрузка завершена
                    newsViewModel.isLoading.removeObserver(this) // Удаляем наблюдателя
                    if (continuation.isActive) {
                        continuation.resume(Unit) // Возобновляем корутину
                    }
                }
            }
        }
        // Начинаем наблюдение за isLoading ViewModel
        newsViewModel.isLoading.observe(this@SplashActivity, observer)

        // Обработка случая, если загрузка уже завершилась до того, как мы начали наблюдать
        if (newsViewModel.isLoading.value == false) {
            newsViewModel.isLoading.removeObserver(observer) // Удаляем сразу
            if (continuation.isActive) {
                continuation.resume(Unit) // Возобновляем немедленно
            }
        }

        // Если корутина отменяется, удаляем наблюдателя, чтобы избежать утечек памяти
        continuation.invokeOnCancellation {
            newsViewModel.isLoading.removeObserver(observer)
        }
    }

    private fun extractGroupsFromExcel(filePath: String): List<String> {
        val groups = mutableListOf<String>()
        try {
            val file = File(filePath)
            val fis = FileInputStream(file)
            val workbook = XSSFWorkbook(fis)
            val sheet = workbook.getSheetAt(0)

            val row = sheet.getRow(23)
            for (col in 6..236) {
                val cell = row.getCell(col)
                val cellValue = cell?.stringCellValue
                if (cellValue != null) {
                    val splitGroups = cellValue.split("\n").map { it.trim().replace("Группа", "").trim() }
                    groups.addAll(splitGroups)
                }
            }
            fis.close()
        } catch (e: Exception) {
            Log.e("ExtractGroups", "Ошибка при чтении Excel файла", e)
        }
        return groups
    }

    private fun downloadExcelFile(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val inputStream: InputStream? = response.body?.byteStream()
                val file = File(getExternalFilesDir(null), "downloaded.xlsx")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                outputStream.close()
                inputStream?.close()
                file.absolutePath
            } else {
                Log.e("DownloadFile", "Ошибка загрузки файла: ${response.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("DownloadFile", "Ошибка при скачивании", e)
            null
        }
    }

    private fun navigateToMainActivity(groups: List<String>) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putStringArrayListExtra("groups_list", ArrayList(groups))
        startActivity(intent)
        finish()
    }
}