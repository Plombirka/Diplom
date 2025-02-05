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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class SplashActivity : AppCompatActivity() {

    private val client = createHttpClient()
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var retryButton: Button
    private lateinit var loadingTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Инициализация элементов интерфейса
        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.statusTextView)
        retryButton = findViewById(R.id.retryButton)

        // Обработка нажатия кнопки "Повторить"
        retryButton.setOnClickListener {
            retryLoading()
        }

        // Начать загрузку данных
        retryLoading()
    }

    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.COMPATIBLE_TLS))
            .build()
    }

    private fun retryLoading() {
        if (isInternetAvailable()) {
            // Если интернет доступен, начать загрузку данных
            progressBar.visibility = View.VISIBLE
            statusTextView.visibility = View.GONE
            retryButton.visibility = View.GONE

            lifecycleScope.launch {
                val groups = loadData()
                if (groups.isNotEmpty()) {
                    navigateToMainActivity(groups)
                } else {
                    loadingTextView.visibility = View.GONE
                    showError("Ошибка загрузки данных")
                }
            }
        } else {
            loadingTextView.visibility = View.GONE
            showError("Нет подключения к интернету")
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        statusTextView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        statusTextView.text = message
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun loadData(): List<String> {
        return withContext(Dispatchers.IO) {
            val filePath = downloadExcelFile("http://212.109.221.255/files/Raspisanie.xlsx")

            if (filePath != null) {
                extractGroupsFromExcel(filePath)
            } else {
                emptyList()
            }
        }
    }

    private fun extractGroupsFromExcel(filePath: String): List<String> {
        val groups = mutableListOf<String>()
        try {
            val file = File(filePath)
            val fis = FileInputStream(file)
            val workbook = XSSFWorkbook(fis)
            val sheet = workbook.getSheetAt(0)

            // Чтение данных с R24C7 до R24C237
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
