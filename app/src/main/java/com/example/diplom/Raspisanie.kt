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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class Raspisanie : Fragment() {

    private lateinit var keywordInput: AutoCompleteTextView
    private lateinit var searchWeek: Button
    private lateinit var searchDay: Button
    private lateinit var searchTomorow: Button
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewSchedule: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter

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

        keywordInput = view.findViewById(R.id.keywordInput)
        searchWeek = view.findViewById(R.id.searchWeek)
        searchDay = view.findViewById(R.id.searchDay)
        searchTomorow = view.findViewById(R.id.searchTomorow)
        statusTextView = view.findViewById(R.id.statusTextView)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerViewSchedule = view.findViewById(R.id.recyclerViewSchedule)

        // Настройка RecyclerView
        scheduleAdapter = ScheduleAdapter()
        recyclerViewSchedule.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = scheduleAdapter
        }

        val groups = requireActivity().intent.getStringArrayListExtra("groups_list")

        if (groups != null && groups.isNotEmpty()) {
            setupAutoCompleteTextView(groups)
        }

        searchDay.setOnClickListener {
            scheduleAdapter.submitList(emptyList()) // Очищаем RecyclerView
            statusTextView.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            statusTextView.text = "Идёт поиск, подождите..."
            val keyword = keywordInput.text.toString().trim()
            if (keyword.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val filePath = withContext(Dispatchers.IO) {
                        downloadExcelFile("http://212.109.221.255/files/Raspisanie.xlsx")
                    }
                    if (filePath != null) {
                        val scheduleItems = withContext(Dispatchers.IO) {
                            searchInExcelDay(filePath, keyword)
                        }
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        if (scheduleItems.isEmpty()) {
                            scheduleAdapter.submitList(listOf(ScheduleListItem.EmptyState))
                        } else {
                            scheduleAdapter.submitList(scheduleItems)
                        }
                    } else {
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        scheduleAdapter.submitList(listOf(ScheduleListItem.SpecialNoteItem("Ошибка при загрузке файла расписания.")))
                    }
                }
            } else {
                statusTextView.visibility = View.GONE
                progressBar.visibility = View.GONE
                scheduleAdapter.submitList(listOf(ScheduleListItem.SpecialNoteItem("Введите ключевое слово для поиска.")))
            }
        }

        searchTomorow.setOnClickListener {
            scheduleAdapter.submitList(emptyList())
            statusTextView.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            statusTextView.text = "Идёт поиск, подождите..."
            val keyword = keywordInput.text.toString().trim()
            if (keyword.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val filePath = withContext(Dispatchers.IO) {
                        downloadExcelFile("http://212.109.221.255/files/Raspisanie.xlsx")
                    }
                    if (filePath != null) {
                        val scheduleItems = withContext(Dispatchers.IO) {
                            searchInExcelTomorow(filePath, keyword)
                        }
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        if (scheduleItems.isEmpty()) {
                            scheduleAdapter.submitList(listOf(ScheduleListItem.EmptyState))
                        } else {
                            scheduleAdapter.submitList(scheduleItems)
                        }
                    } else {
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        scheduleAdapter.submitList(listOf(ScheduleListItem.SpecialNoteItem("Ошибка при загрузке файла расписания.")))
                    }
                }
            } else {
                statusTextView.visibility = View.GONE
                progressBar.visibility = View.GONE
                scheduleAdapter.submitList(listOf(ScheduleListItem.SpecialNoteItem("Введите ключевое слово для поиска.")))
            }
        }
        searchWeek.setOnClickListener {
            scheduleAdapter.submitList(emptyList())
            statusTextView.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            statusTextView.text = "Идёт поиск, подождите..."
            val keyword = keywordInput.text.toString().trim()
            if (keyword.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val filePath = withContext(Dispatchers.IO) {
                        downloadExcelFile("http://212.109.221.255/files/Raspisanie.xlsx")
                    }
                    if (filePath != null) {
                        val scheduleItems = withContext(Dispatchers.IO) {
                            searchInExcel(filePath, keyword)
                        }
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        if (scheduleItems.isEmpty()) {
                            scheduleAdapter.submitList(listOf(ScheduleListItem.EmptyState))
                        } else {
                            scheduleAdapter.submitList(scheduleItems)
                        }
                    } else {
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        scheduleAdapter.submitList(listOf(ScheduleListItem.SpecialNoteItem("Ошибка при загрузке файла расписания.")))
                    }
                }
            } else {
                statusTextView.visibility = View.GONE
                progressBar.visibility = View.GONE
                scheduleAdapter.submitList(listOf(ScheduleListItem.SpecialNoteItem("Введите ключевое слово для поиска.")))
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
                Log.e("DownloadFile", "Ошибка загрузки файла: ${response.code} ${response.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("DownloadFile", "Ошибка при скачивании", e)
            null
        }
    }

    private fun searchInExcelTomorow(filePath: String, keyword: String): List<ScheduleListItem> {
        val scheduleItems = mutableListOf<ScheduleListItem>()
        try {
            Log.d("ExcelSearch", "Чтение файла для завтрашнего дня (ключ: $keyword)...")
            val fileInputStream = FileInputStream(filePath)
            XSSFWorkbook(fileInputStream).use { workbook -> // Используем use для автоматического закрытия
                val sheet = workbook.getSheetAt(0)
                var found = false
                var keywordRowIndex = -1
                var keywordColumnIndex = -1

                for (row in sheet) {
                    for (cell in row) {
                        if (cell.toString().trim().contains(keyword, ignoreCase = true)) {
                            keywordRowIndex = row.rowNum
                            keywordColumnIndex = cell.columnIndex
                            found = true
                            Log.d("ExcelSearch", "Ключевое слово '$keyword' найдено в строке $keywordRowIndex, столбце $keywordColumnIndex")
                            break
                        }
                    }
                    if (found) break
                }

                if (found) {
                    val tomorrowDate = LocalDate.now().plusDays(1)
                    val dayOfWeek = tomorrowDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
                    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM", Locale("ru"))
                    val formattedDate = tomorrowDate.format(dateFormatter)
                    val targetDayString = "$dayOfWeek $formattedDate".replace("\\s+".toRegex(), " ").uppercase()
                    Log.d("ExcelSearch", "Искомый день (завтра): $targetDayString")

                    var isTargetDay = false
                    var dayHeaderAdded = false

                    val mergedRegions = sheet.mergedRegions
                    for (i in (keywordRowIndex + 2) until sheet.physicalNumberOfRows) {
                        val row = sheet.getRow(i) ?: continue
                        val dayCellRaw = getMergedCellValue(sheet, mergedRegions, i, 6) // Столбец G (дни недели)

                        if (dayCellRaw.isNotBlank()) {
                            val dayFromExcel = dayCellRaw.replace("\\s+".toRegex(), " ").uppercase().trim()
                            if (dayFromExcel == targetDayString) {
                                isTargetDay = true
                                if (!dayHeaderAdded) {
                                    scheduleItems.add(ScheduleListItem.DayHeader(dayCellRaw.trim()))
                                    dayHeaderAdded = true
                                    Log.d("ExcelSearch", "Добавлен заголовок дня: $dayCellRaw")
                                }
                            } else {
                                // Если мы были на целевом дне и день сменился, прекращаем поиск для этого дня
                                if (isTargetDay) {
                                    Log.d("ExcelSearch", "День сменился с $targetDayString на $dayFromExcel, завершаем поиск на завтра.")
                                    break
                                }
                                isTargetDay = false // Сбрасываем флаг, если это не целевой день
                            }
                        }

                        if (isTargetDay) {
                            val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7) // Столбец H (время)
                            val disciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex)
                            val typeCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                            val teacherCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 2)
                            val audienceCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 3)

                            if (timeCell.isNotBlank() && disciplineCell.isNotBlank()) {
                                Log.d("ExcelSearch", "Найдена запись: $timeCell, $disciplineCell, $typeCell, $teacherCell, $audienceCell")
                                if (disciplineCell.equals(typeCell, ignoreCase = true)) {
                                    scheduleItems.add(ScheduleListItem.SpecialNoteItem(disciplineCell))
                                } else {
                                    scheduleItems.add(ScheduleListItem.PairItem(
                                        timeCell,
                                        disciplineCell,
                                        typeCell,
                                        teacherCell.ifBlank { "—" },
                                        audienceCell.ifBlank { "—" }
                                    ))
                                }
                            }
                        }
                    }
                } else {
                    Log.d("ExcelSearch", "Ключевое слово '$keyword' не найдено.")
                }
            } // workbook.close() вызывается автоматически
            Log.d("ExcelSearch", "Поиск на завтра завершён. Найдено элементов: ${scheduleItems.size}")
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла (завтра)", e)
            scheduleItems.clear() // Очищаем, если были добавлены частичные данные до ошибки
            scheduleItems.add(ScheduleListItem.SpecialNoteItem("Ошибка при чтении файла расписания: ${e.message}"))
        }
        return scheduleItems
    }

    private fun searchInExcelDay(filePath: String, keyword: String): List<ScheduleListItem> {
        val scheduleItems = mutableListOf<ScheduleListItem>()
        try {
            Log.d("ExcelSearch", "Чтение файла на текущий день (ключ: $keyword)...")
            val fileInputStream = FileInputStream(filePath)
            XSSFWorkbook(fileInputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                var found = false
                var keywordRowIndex = -1
                var keywordColumnIndex = -1

                for (row in sheet) {
                    for (cell in row) {
                        if (cell.toString().trim().contains(keyword, ignoreCase = true)) {
                            keywordRowIndex = row.rowNum
                            keywordColumnIndex = cell.columnIndex
                            found = true
                            Log.d("ExcelSearch", "Ключевое слово '$keyword' найдено в строке $keywordRowIndex, столбце $keywordColumnIndex")
                            break
                        }
                    }
                    if (found) break
                }

                if (found) {
                    val currentDate = LocalDate.now()
                    val dayOfWeek = currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
                    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM", Locale("ru"))
                    val formattedDate = currentDate.format(dateFormatter)
                    val targetDayString = "$dayOfWeek $formattedDate".replace("\\s+".toRegex(), " ").uppercase()
                    Log.d("ExcelSearch", "Искомый день (сегодня): $targetDayString")

                    var isTargetDay = false
                    var dayHeaderAdded = false

                    val mergedRegions = sheet.mergedRegions
                    for (i in (keywordRowIndex + 2) until sheet.physicalNumberOfRows) {
                        val row = sheet.getRow(i) ?: continue
                        val dayCellRaw = getMergedCellValue(sheet, mergedRegions, i, 6)

                        if (dayCellRaw.isNotBlank()) {
                            val dayFromExcel = dayCellRaw.replace("\\s+".toRegex(), " ").uppercase().trim()
                            if (dayFromExcel == targetDayString) {
                                isTargetDay = true
                                if (!dayHeaderAdded) {
                                    scheduleItems.add(ScheduleListItem.DayHeader(dayCellRaw.trim()))
                                    dayHeaderAdded = true
                                    Log.d("ExcelSearch", "Добавлен заголовок дня: $dayCellRaw")
                                }
                            } else {
                                if (isTargetDay) {
                                    Log.d("ExcelSearch", "День сменился с $targetDayString на $dayFromExcel, завершаем поиск на сегодня.")
                                    break
                                }
                                isTargetDay = false
                            }
                        }

                        if (isTargetDay) {
                            val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7)
                            val disciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex)
                            val typeCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                            val teacherCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 2)
                            val audienceCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 3)

                            if (timeCell.isNotBlank() && disciplineCell.isNotBlank()) {
                                Log.d("ExcelSearch", "Найдена запись: $timeCell, $disciplineCell, $typeCell, $teacherCell, $audienceCell")
                                if (disciplineCell.equals(typeCell, ignoreCase = true)) {
                                    scheduleItems.add(ScheduleListItem.SpecialNoteItem(disciplineCell))
                                } else {
                                    scheduleItems.add(ScheduleListItem.PairItem(
                                        timeCell,
                                        disciplineCell,
                                        typeCell,
                                        teacherCell.ifBlank { "—" },
                                        audienceCell.ifBlank { "—" }
                                    ))
                                }
                            }
                        }
                    }
                } else {
                    Log.d("ExcelSearch", "Ключевое слово '$keyword' не найдено.")
                }
            }
            Log.d("ExcelSearch", "Поиск на сегодня завершён. Найдено элементов: ${scheduleItems.size}")
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла (сегодня)", e)
            scheduleItems.clear()
            scheduleItems.add(ScheduleListItem.SpecialNoteItem("Ошибка при чтении файла расписания: ${e.message}"))
        }
        return scheduleItems
    }

    private fun searchInExcel(filePath: String, keyword: String): List<ScheduleListItem> {
        val scheduleItems = mutableListOf<ScheduleListItem>()
        try {
            Log.d("ExcelSearch", "Чтение файла (вся неделя, ключ: $keyword)...")
            val fileInputStream = FileInputStream(filePath)
            XSSFWorkbook(fileInputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                var found = false
                var keywordRowIndex = -1
                var keywordColumnIndex = -1

                for (row in sheet) {
                    for (cell in row) {
                        if (cell.toString().trim().contains(keyword, ignoreCase = true)) {
                            keywordRowIndex = row.rowNum
                            keywordColumnIndex = cell.columnIndex
                            found = true
                            Log.d("ExcelSearch", "Ключевое слово '$keyword' найдено в строке $keywordRowIndex, столбце $keywordColumnIndex")
                            break
                        }
                    }
                    if (found) break
                }

                if (found) {
                    var currentDayHeader = ""
                    val mergedRegions = sheet.mergedRegions

                    for (i in (keywordRowIndex + 2) until sheet.physicalNumberOfRows) {
                        val row = sheet.getRow(i) ?: continue
                        val dayCellRaw = getMergedCellValue(sheet, mergedRegions, i, 6)
                        val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7)
                        val disciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex)
                        val typeCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                        val teacherCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 2)
                        val audienceCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 3)

                        if (dayCellRaw.isNotBlank() && dayCellRaw != currentDayHeader) {
                            scheduleItems.add(ScheduleListItem.DayHeader(dayCellRaw.trim()))
                            currentDayHeader = dayCellRaw
                            Log.d("ExcelSearch", "Добавлен заголовок дня: $dayCellRaw")
                        }

                        if (timeCell.isNotBlank() && disciplineCell.isNotBlank()) {
                            Log.d("ExcelSearch", "Найдена запись: $timeCell, $disciplineCell, $typeCell, $teacherCell, $audienceCell")
                            if (disciplineCell.equals(typeCell, ignoreCase = true)) {
                                scheduleItems.add(ScheduleListItem.SpecialNoteItem(disciplineCell))
                            } else {
                                scheduleItems.add(ScheduleListItem.PairItem(
                                    timeCell,
                                    disciplineCell,
                                    typeCell,
                                    teacherCell.ifBlank { "—" },
                                    audienceCell.ifBlank { "—" }
                                ))
                            }
                        }
                    }
                } else {
                    Log.d("ExcelSearch", "Ключевое слово '$keyword' не найдено.")
                }
            }
            Log.d("ExcelSearch", "Поиск по неделе завершён. Найдено элементов: ${scheduleItems.size}")
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла (неделя)", e)
            scheduleItems.clear()
            scheduleItems.add(ScheduleListItem.SpecialNoteItem("Ошибка при чтении файла расписания: ${e.message}"))
        }
        return scheduleItems
    }

    private fun getMergedCellValue(sheet: XSSFSheet, mergedRegions: List<CellRangeAddress>, row: Int, col: Int): String {
        for (range in mergedRegions) {
            if (range.isInRange(row, col)) {
                val firstRow = sheet.getRow(range.firstRow)
                if (firstRow != null) {
                    val mergedCell = firstRow.getCell(range.firstColumn)
                    return mergedCell?.toString()?.trim() ?: ""
                }
                return "" // Если первая строка региона не найдена
            }
        }
        val currentRow = sheet.getRow(row)
        if (currentRow != null) {
            val cell = currentRow.getCell(col)
            return cell?.toString()?.trim() ?: ""
        }
        return "" // Если строка не найдена
    }

    private val client: OkHttpClient by lazy { createHttpClient() } // ленивая инициализация

    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS)) // Добавлен MODERN_TLS
            .build()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + requireActivity().packageName)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("Permissions", "Не удалось запросить ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", e)
                    // Можно показать диалог пользователю с инструкцией как дать разрешение вручную
                }
            }
        }
        // Для версий < R разрешения WRITE_EXTERNAL_STORAGE/READ_EXTERNAL_STORAGE
        // должны запрашиваться через `requestPermissions()` и обрабатываться в `onRequestPermissionsResult()`.
        // Текущий код не содержит этой логики, но для полноты картины это важно.
    }

    private fun setupAutoCompleteTextView(groups: List<String>) {
        // Создаем ArrayAdapter один раз
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, groups.toMutableList())
        keywordInput.setAdapter(adapter)
        keywordInput.threshold = 1 // Показывать подсказки после ввода 1 символа

        keywordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}