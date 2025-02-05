package com.example.diplom

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import kotlinx.coroutines.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.ConnectionSpec
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFSheet
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Raspisanie : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_raspisanie, container, false)
    }


    @SuppressLint("MissingInflatedId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkPermissions()

        val keywordInput = view.findViewById<AutoCompleteTextView>(R.id.keywordInput)
        val searchWeek = view.findViewById<Button>(R.id.searchWeek)
        val searchDay = view.findViewById<Button>(R.id.searchDay)
        val searchTomorow = view.findViewById<Button>(R.id.searchTomorow)
        val resultTextView = view.findViewById<TextView>(R.id.resultTextView)
        val statusTextView = view.findViewById<TextView>(R.id.statusTextView)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val groups = requireActivity().intent.getStringArrayListExtra("groups_list")

        // Настройка AutoCompleteTextView с полученными данными
        if (groups != null && groups.isNotEmpty()) {
            setupAutoCompleteTextView(groups)
        }

        searchDay.setOnClickListener {
            if (resultTextView.text != " "){
                resultTextView.text = " "
            }
            statusTextView.visibility=View.VISIBLE
            progressBar.visibility=View.VISIBLE
            statusTextView.text="Идёт поиск, подождите..."
            val keyword = keywordInput.text.toString()
            if (keyword.isNotEmpty()){
                CoroutineScope(Dispatchers.Main).launch {
                    val filePath = withContext(Dispatchers.IO){
                        downloadExcelFile("http://212.109.221.255/files/Raspisanie.xlsx")
                    }
                    if (filePath != null) {
                        val result = withContext(Dispatchers.IO) {
                            searchInExcelDay(filePath, keyword)
                        }
                        statusTextView.visibility=View.GONE
                        progressBar.visibility=View.GONE
                        resultTextView.text = result
                    } else{
                        resultTextView.text = "Ошибка при загрузке файла"
                        statusTextView.visibility=View.GONE
                        progressBar.visibility=View.GONE
                    }
                }
            } else{
                resultTextView.text = "Введите ключевое слово"
                statusTextView.visibility=View.GONE
                progressBar.visibility=View.GONE
            }
        }

        searchTomorow.setOnClickListener {
            if (resultTextView.text != " "){
                resultTextView.text = " "
            }
            statusTextView.visibility=View.VISIBLE
            progressBar.visibility=View.VISIBLE
            statusTextView.text="Идёт поиск, подождите..."
            val keyword = keywordInput.text.toString()
            if (keyword.isNotEmpty()){
                CoroutineScope(Dispatchers.Main).launch {
                    val filePath = withContext(Dispatchers.IO){
                        downloadExcelFile("http://212.109.221.255/files/Raspisanie.xlsx")
                    }
                    if (filePath != null) {
                        val result = withContext(Dispatchers.IO) {
                            searchInExcelTomorow(filePath, keyword)
                        }
                        statusTextView.visibility=View.GONE
                        progressBar.visibility=View.GONE
                        resultTextView.text = result
                    } else{
                        resultTextView.text = "Ошибка при загрузке файла"
                        statusTextView.visibility=View.GONE
                        progressBar.visibility=View.GONE
                    }
                }
            } else{
                resultTextView.text = "Введите ключевое слово"
                statusTextView.visibility=View.GONE
                progressBar.visibility=View.GONE
            }
        }
        searchWeek.setOnClickListener {
            if (resultTextView.text != " "){
                resultTextView.text = " "
            }
            statusTextView.visibility=View.VISIBLE
            progressBar.visibility=View.VISIBLE
            statusTextView.text="Идёт поиск, подождите..."
            val keyword = keywordInput.text.toString()
            if (keyword.isNotEmpty()) {
                // Запуск асинхронного процесса загрузки и обработки Excel файла
                CoroutineScope(Dispatchers.Main).launch {
                    val filePath = withContext(Dispatchers.IO) {
                        downloadExcelFile("http://212.109.221.255/files/Raspisanie.xlsx")
                    }
                    if (filePath != null) {
                        val result = withContext(Dispatchers.IO) {
                            searchInExcel(filePath, keyword)
                        }
                        statusTextView.visibility = View.GONE
                        progressBar.visibility=View.GONE
                        resultTextView.text = result
                    } else{
                        resultTextView.text = "Ошибка при загрузке файла"
                        statusTextView.visibility = View.GONE
                        progressBar.visibility=View.GONE
                    }
                }
            } else {
                resultTextView.text = "Введите ключевое слово!"
                statusTextView.visibility = View.GONE
                progressBar.visibility=View.GONE
            }
        }
    }

    private fun downloadExcelFile(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val inputStream: InputStream? = response.body?.byteStream()
                val file = File(requireActivity().getExternalFilesDir(null), "downloaded.xlsx")
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

    private fun searchInExcelTomorow(filePath: String, keyword: String): String {
        return try {
            Log.d("ExcelSearch", "Чтение файла...")
            val fileInputStream = FileInputStream(filePath)
            val workbook = XSSFWorkbook(fileInputStream)
            val sheet = workbook.getSheetAt(0)

            val result = StringBuilder()
            var found = false
            var keywordRowIndex = -1
            var keywordColumnIndex = -1
            var totalRowCount = 0

            // Находим ключевое слово
            for (row in sheet) {
                for (cell in row) {
                    if (cell.toString().contains(keyword, ignoreCase = true)) {
                        keywordRowIndex = row.rowNum
                        keywordColumnIndex = cell.columnIndex
                        found = true
                        break
                    }
                }
                if (found) break
            }

            if (found) {
                // Получаем завтрашнюю дату
                val tomorrowDate = LocalDate.now().plusDays(1)
                val dayOfWeek = tomorrowDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM", Locale("ru"))
                val formattedDate = tomorrowDate.format(dateFormatter)

                // Создаем строку для сравнения в формате "ВТОРНИК 12.11"
                val nextDay = "$dayOfWeek $formattedDate"
                    .replace("\\s+".toRegex(), " ") // Убираем лишние пробелы
                    .uppercase()

                Log.d("ExcelSearch", "Завтрашний день: $nextDay")

                var isNextDay = false // Флаг для отслеживания нужного дня
                var currentDay = "" // Для отслеживания текущего дня

                // Обрабатываем объединённые ячейки
                val mergedRegions = sheet.mergedRegions

                for (i in (keywordRowIndex + 2) until sheet.physicalNumberOfRows) {
                    val row = sheet.getRow(i) ?: continue

                    // Получаем ячейки
                    val dayCell = getMergedCellValue(sheet, mergedRegions, i, 6) // Столбец G
                    val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7) // Столбец H

                    // Проверяем, является ли строка новой датой
                    if (dayCell != null && dayCell.toString().isNotEmpty()) {
                        // Приводим день из Excel к такому же формату, как и завтрашняя дата
                        val dayFromExcel = dayCell.toString()
                            .replace("\\s+".toRegex(), " ") // Убираем лишние пробелы
                            .uppercase()
                            .trim()

                        // Проверяем, совпадает ли день в Excel с завтрашним днём
                        if (dayFromExcel == nextDay) {
                            isNextDay = true
                        } else {
                            isNextDay = false
                        }
                    }

                    if(isNextDay){
                        val disciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex)
                        val typeCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                        val teacherCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 2)
                        val audienceCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 3)

                        if (timeCell.isNotBlank() && disciplineCell.isNotBlank()) {
                            // Если день изменился, добавляем заголовок дня
                            if (dayCell.isNotBlank() && dayCell != currentDay) {
                                result.append("\n=== $dayCell ===\n")
                                currentDay = dayCell
                            }

                            // Форматируем строку
                            if (disciplineCell == typeCell){
                                result.append("$disciplineCell\n")
                            }
                            else{
                                result.append("$timeCell | $disciplineCell | $typeCell | ${teacherCell.ifBlank { "—" }} | ${audienceCell.ifBlank { "—" }}\n")
                            }

                            if (totalRowCount % 7 == 0) {
                                result.append("\n")
                            }
                        }
                    }
                }
            }

            workbook.close()
            Log.d("ExcelSearch", "Поиск завершён.")
            if (result.isEmpty()) "Ничего не найдено" else result.toString()
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла", e)
            "Ошибка при чтении файла: ${e.message}"
        }
    }

    private fun searchInExcelDay(filePath: String, keyword: String): String {
        return try {
            Log.d("ExcelSearch", "Чтение файла...")
            val fileInputStream = FileInputStream(filePath)
            val workbook = XSSFWorkbook(fileInputStream)
            val sheet = workbook.getSheetAt(0)

            val result = StringBuilder()
            var found = false
            var keywordRowIndex = -1
            var keywordColumnIndex = -1
            var totalRowCount = 0

            // Находим ключевое слово
            for (row in sheet) {
                for (cell in row) {
                    if (cell.toString().contains(keyword, ignoreCase = true)) {
                        keywordRowIndex = row.rowNum
                        keywordColumnIndex = cell.columnIndex
                        found = true
                        break
                    }
                }
                if (found) break
            }

            if (found) {
                // Получаем текущую дату
                val currentDate = LocalDate.now()
                val dayOfWeek = currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM", Locale("ru"))
                val formattedDate = currentDate.format(dateFormatter)

                // Создаем строку для сравнения в формате "ПОНЕДЕЛЬНИК 11.11"
                val nowDay = "$dayOfWeek $formattedDate"
                    .replace("\\s+".toRegex(), " ") // Убираем лишние пробелы
                    .uppercase()

                Log.d("ExcelSearch", "Текущий день: $nowDay")

                var isCurrentDay = false // Флаг для отслеживания нужного дня
                var currentDay = "" // Для отслеживания текущего дня

                // Обрабатываем объединённые ячейки
                val mergedRegions = sheet.mergedRegions

                for (i in (keywordRowIndex + 2) until sheet.physicalNumberOfRows) {
                    val row = sheet.getRow(i) ?: continue

                    // Получаем ячейки
                    val dayCell = getMergedCellValue(sheet, mergedRegions, i, 6) // Столбец G
                    val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7) // Столбец H

                    if (dayCell != null && dayCell.toString().isNotEmpty()) {
                        // Приводим день из Excel к такому же формату, как и текущая дата
                        val dayFromExcel = dayCell.toString()
                            .replace("\\s+".toRegex(), " ") // Убираем лишние пробелы
                            .uppercase()
                            .trim()

                        // Проверяем, совпадает ли день в Excel с сегодняшним днем
                        if (dayFromExcel == nowDay) {
                            isCurrentDay = true
                        } else {
                            isCurrentDay = false
                        }
                    }

                    if (isCurrentDay){
                        val disciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex)
                        val typeCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                        val teacherCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 2)
                        val audienceCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 3)

                        if (timeCell.isNotBlank() && disciplineCell.isNotBlank()) {
                            // Если день изменился, добавляем заголовок дня
                            if (dayCell.isNotBlank() && dayCell != currentDay) {
                                result.append("\n=== $dayCell ===\n")
                                currentDay = dayCell
                            }

                            if (disciplineCell== typeCell){
                                result.append("$disciplineCell\n")
                            }
                            else{
                                result.append("$timeCell | $disciplineCell | $typeCell | ${teacherCell.ifBlank { "—" }} | ${audienceCell.ifBlank { "—" }}\n")
                            }

                            if (totalRowCount % 7 == 0) {
                                result.append("\n")
                            }
                        }
                    }
                }
            }

            workbook.close()
            Log.d("ExcelSearch", "Поиск завершён.")
            if (result.isEmpty()) "Ничего не найдено" else result.toString()
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла", e)
            "Ошибка при чтении файла: ${e.message}"
        }
    }

    private fun searchInExcel(filePath: String, keyword: String): String {
        return try {
            Log.d("ExcelSearch", "Чтение файла...")
            val fileInputStream = FileInputStream(filePath)
            val workbook = XSSFWorkbook(fileInputStream)
            val sheet = workbook.getSheetAt(0)

            val result = StringBuilder()
            var found = false
            var keywordRowIndex = -1
            var keywordColumnIndex = -1
            var totalRowCount = 0

            // Находим ключевое слово
            for (row in sheet) {
                for (cell in row) {
                    if (cell.toString().contains(keyword, ignoreCase = true)) {
                        keywordRowIndex = row.rowNum
                        keywordColumnIndex = cell.columnIndex
                        found = true
                        break
                    }
                }
                if (found) break
            }

            if (found) {
                var currentDay = "" // Для отслеживания текущего дня

                // Обрабатываем объединённые ячейки
                val mergedRegions = sheet.mergedRegions

                for (i in (keywordRowIndex + 2) until sheet.physicalNumberOfRows) {
                    val row = sheet.getRow(i) ?: continue

                    // Получаем ячейки
                    val dayCell = getMergedCellValue(sheet, mergedRegions, i, 6) // Столбец G
                    val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7) // Столбец H
                    val disciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex)
                    val typeCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                    val teacherCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 2)
                    val audienceCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 3)

                    if (timeCell.isNotBlank() && disciplineCell.isNotBlank()) {
                        // Если день изменился, добавляем заголовок дня
                        if (dayCell.isNotBlank() && dayCell != currentDay) {
                            result.append("\n=== $dayCell ===\n")
                            currentDay = dayCell
                        }

                        if (disciplineCell== typeCell){
                            result.append("$disciplineCell\n")
                        }
                        else{
                            result.append("$timeCell | $disciplineCell | $typeCell | ${teacherCell.ifBlank { "—" }} | ${audienceCell.ifBlank { "—" }}\n")
                        }
                        if (totalRowCount % 7 == 0) {
                            result.append("\n")
                        }
                    }
                }
            }

            workbook.close()
            Log.d("ExcelSearch", "Поиск завершён.")
            if (result.isEmpty()) "Ничего не найдено" else result.toString()
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла", e)
            "Ошибка при чтении файла: ${e.message}"
        }
    }

    // Функция для получения значения из объединённых ячеек
    private fun getMergedCellValue(sheet: XSSFSheet, mergedRegions: List<CellRangeAddress>, row: Int, col: Int): String {
        for (range in mergedRegions) {
            if (range.isInRange(row, col)) {
                val mergedCell = sheet.getRow(range.firstRow).getCell(range.firstColumn)
                return mergedCell?.toString() ?: ""
            }
        }
        val cell = sheet.getRow(row)?.getCell(col)
        return cell?.toString() ?: ""
    }
    private val client = createHttpClient()

    // Создание OkHttpClient для работы с HTTP, включая разрешение на cleartext трафик
    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Логгирование запросов для отладки
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.COMPATIBLE_TLS)) // Разрешение на HTTP (cleartext)
            .build()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + requireActivity().packageName)
                startActivity(intent)
            }
        }
    }

    private fun setupAutoCompleteTextView(groups: List<String>) {
        // Инициализируем адаптер с полным списком групп
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, groups)
        val keywordInput = view?.findViewById<AutoCompleteTextView>(R.id.keywordInput)
        keywordInput?.setAdapter(adapter)

        // Слушатель для изменения текста
        keywordInput?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                val filteredGroups = filterGroups(query, groups)
                val filteredAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, filteredGroups)
                keywordInput?.setAdapter(filteredAdapter)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterGroups(query: String, groups: List<String>): List<String> {
        return groups.filter { it.lowercase().contains(query.lowercase()) }
    }
}