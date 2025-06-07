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

class Prepodavateli : Fragment() {

    private lateinit var keywordInput: AutoCompleteTextView
    private lateinit var searchWeek: Button
    private lateinit var searchDay: Button
    private lateinit var searchTomorow: Button
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewSchedule: RecyclerView // Объявление RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapterPrepod // Объявление адаптера

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_prepodavateli, container, false)
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
        recyclerViewSchedule = view.findViewById(R.id.recyclerViewSchedule) // Инициализация RecyclerView

        // Настройка RecyclerView
        scheduleAdapter = ScheduleAdapterPrepod()
        recyclerViewSchedule.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = scheduleAdapter
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
                            scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.EmptyState))
                        } else {
                            scheduleAdapter.submitList(scheduleItems)
                        }
                    } else {
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.SpecialNoteItem("Ошибка при загрузке файла расписания.")))
                    }
                }
            } else {
                statusTextView.visibility = View.GONE
                progressBar.visibility = View.GONE
                scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.SpecialNoteItem("Введите ФИО преподавателя для поиска.")))
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
                            scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.EmptyState))
                        } else {
                            scheduleAdapter.submitList(scheduleItems)
                        }
                    } else {
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.SpecialNoteItem("Ошибка при загрузке файла расписания.")))
                    }
                }
            } else {
                statusTextView.visibility = View.GONE
                progressBar.visibility = View.GONE
                scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.SpecialNoteItem("Введите ФИО преподавателя для поиска.")))
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
                            scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.EmptyState))
                        } else {
                            scheduleAdapter.submitList(scheduleItems)
                        }
                    } else {
                        statusTextView.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.SpecialNoteItem("Ошибка при загрузке файла расписания.")))
                    }
                }
            } else {
                statusTextView.visibility = View.GONE
                progressBar.visibility = View.GONE
                scheduleAdapter.submitList(listOf(ScheduleListItemPrepod.SpecialNoteItem("Введите ФИО преподавателя для поиска.")))
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

    // --- Методы поиска расписания (изменены для возврата List<ScheduleListItem>) ---

    private fun searchInExcelTomorow(filePath: String, keyword: String): List<ScheduleListItemPrepod> {
        val scheduleItems = mutableListOf<ScheduleListItemPrepod>()
        try {
            Log.d("ExcelSearch", "Чтение файла для завтрашнего дня (ключ: $keyword)...")
            val fileInputStream = FileInputStream(filePath)
            XSSFWorkbook(fileInputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                var found = false
                var index = false
                var keywordRowIndex = -1
                var keywordColumnIndex = -1

                val headerRowIndex = 23
                val headerRow = sheet.getRow(headerRowIndex)
                if (headerRow != null) {
                    for (row in sheet) {
                        for (cell in row) {
                            if (cell.toString().trim().contains(keyword, ignoreCase = true)) {
                                keywordRowIndex = row.rowNum
                                keywordColumnIndex = cell.columnIndex
                                found = true
                                Log.d(
                                    "ExcelSearch",
                                    "Преподаватель '$keyword' найден в столбце $keywordColumnIndex"
                                )
                                break
                            }
                        }
                        if (found) break
                    }
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
                    // Начинаем поиск пар ниже строки заголовков (например, с 24 строки)
                    for (i in (headerRowIndex + 2) until sheet.physicalNumberOfRows) { // +2, чтобы начать после заголовков дней
                        val row = sheet.getRow(i) ?: continue
                        for (cell in row) {
                            if (cell.toString().contains(keyword, ignoreCase = true)) {
                                keywordColumnIndex = cell.columnIndex
                                index = false
                                break
                            } else {
                                index = true
                            }
                        }
                        if (index){
                            index = false
                            continue
                        }
                        val dayCellRaw = getMergedCellValue(sheet, mergedRegions, i, 6) // Столбец G (дни недели)

                        if (dayCellRaw.isNotBlank()) {
                            val dayFromExcel = dayCellRaw.replace("\\s+".toRegex(), " ").uppercase().trim()
                            if (dayFromExcel == targetDayString) {
                                isTargetDay = true
                                if (!dayHeaderAdded) {
                                    scheduleItems.add(ScheduleListItemPrepod.DayHeader(dayCellRaw.trim()))
                                    dayHeaderAdded = true
                                    Log.d("ExcelSearch", "Добавлен заголовок дня: $dayCellRaw")
                                }
                            } else {
                                if (isTargetDay) { // Если мы были на целевом дне и день сменился, прекращаем поиск
                                    Log.d("ExcelSearch", "День сменился с $targetDayString на $dayFromExcel, завершаем поиск на завтра.")
                                    break
                                }
                                isTargetDay = false
                            }
                        }

                        if (isTargetDay) {
                            val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7) // Столбец H (время)
                            val disciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex)
                            // В вашем первом коде, typeCell (Пр. занятие/Лекция) был частью disciplineCell
                            // teacherCell был в другом столбце (keywordColumnIndex + 1)
                            // audienceCell был в keywordColumnIndex + 2
                            // Но в вашем коде для групп, typeCell, teacherCell, audienceCell
                            // были в соседних столбцах от группы.
                            // Теперь, так как keywordColumnIndex - это столбец преподавателя,
                            // аудитория, тип занятия и дисциплина будут находиться в других смещениях
                            // относительно этого столбца.
                            // **ВНИМАНИЕ:** Эти смещения (keywordColumnIndex + X) могут быть неверными
                            // и требуют точной проверки структуры вашей Excel-таблицы.
                            val actualDisciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex) // Дисциплина (и тип занятия)
                            val actualTeacherCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1) // ФИО преподавателя (если в этом столбце)
                            val actualAudienceCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 2) // Аудитория
                            val actualGroupCell = getMergedCellValue(sheet, mergedRegions, 23, keywordColumnIndex) // Строка 23, столбец найденного преподавателя для группы

                            // **Важно:** Логика извлечения данных из ячеек для преподавателя
                            // отличается от логики для группы.
                            // В Excel для преподавателей, дисциплина, аудитория, тип занятия и группа
                            // могут быть расположены иначе относительно столбца преподавателя.
                            // Вам нужно точно знать, какие столбцы соответствуют этим данным
                            // относительно столбца, в котором находится ФИО преподавателя.

                            // Пример адаптации:
                            // Предположим, что:
                            // - Время: столбец H (индекс 7)
                            // - ФИО преподавателя: keyowrd (найдено в keywordColumnIndex)
                            // - Дисциплина и Тип занятия: в столбце слева от преподавателя (keywordColumnIndex - 1)
                            // - Аудитория: в столбце справа от преподавателя (keywordColumnIndex + 1)
                            // - Группа: в строке 23 (индекс 22) и том же столбце, что и преподаватель

                            val lessonDisciplineAndType = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex - 2) // Если дисциплина и тип занятия находятся в столбце преподавателя
                            val actualAudience = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1) // Если аудитория находится в столбце справа
                            val groupName = getMergedCellValue(sheet, mergedRegions, 23, keywordColumnIndex).replace("Группа", "").trim()


                            // Разделяем дисциплину и тип занятия
                            val parts = lessonDisciplineAndType.split("\n").map { it.trim() }
                            val discipline = parts.getOrElse(0) { "" }
                            val type = parts.getOrElse(1) { "" } // Например, "Пр. занятие" или "Лекция"

                            // Проверяем, что найденная дисциплина относится к текущему преподавателю (ключевому слову)
                            // Если преподаватель найден в заголовке столбца, то все пары в этом столбце
                            // относятся к этому преподавателю.
                            if (timeCell.isNotBlank() && discipline.isNotBlank()) {
                                Log.d("ExcelSearch", "Найдена запись: Время: $timeCell, Дисциплина: $discipline, Тип: $type, Преподаватель (ключ): $keyword, Аудитория: $actualAudience, Группа: $groupName")

                                // Проверяем случай "день самостоятельной подготовки"
                                if (discipline.equals(type, ignoreCase = true) && discipline.isNotBlank()) {
                                    scheduleItems.add(ScheduleListItemPrepod.SpecialNoteItem(discipline))
                                } else {
                                    scheduleItems.add(ScheduleListItemPrepod.PairItem(
                                        time = timeCell,
                                        discipline = discipline,
                                        type = type,
                                        teacher = keyword, // Используем искомое ФИО преподавателя
                                        audience = actualAudience.ifBlank { "—" },
                                        group = groupName.ifBlank { "—" }
                                    ))
                                }
                            }
                        }
                    }
                } else {
                    Log.d("ExcelSearch", "Преподаватель '$keyword' не найден в заголовках.")
                }
            }
            Log.d("ExcelSearch", "Поиск на завтра завершён. Найдено элементов: ${scheduleItems.size}")
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла (завтра)", e)
            scheduleItems.clear()
            scheduleItems.add(ScheduleListItemPrepod.SpecialNoteItem("Ошибка при чтении файла расписания: ${e.message}"))
        }
        return scheduleItems
    }

    private fun searchInExcelDay(filePath: String, keyword: String): List<ScheduleListItemPrepod> {
        val scheduleItems = mutableListOf<ScheduleListItemPrepod>()
        try {
            Log.d("ExcelSearch", "Чтение файла на текущий день (ключ: $keyword)...")
            val fileInputStream = FileInputStream(filePath)
            XSSFWorkbook(fileInputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                var found = false
                var index = false
                var keywordRowIndex = -1
                var keywordColumnIndex = -1

                val headerRowIndex = 23
                val headerRow = sheet.getRow(headerRowIndex)
                if (headerRow != null) {
                    for (row in sheet) {
                        for (cell in row) {
                            if (cell.toString().trim().contains(keyword, ignoreCase = true)) {
                                keywordRowIndex = row.rowNum
                                keywordColumnIndex = cell.columnIndex
                                found = true
                                Log.d(
                                    "ExcelSearch",
                                    "Преподаватель '$keyword' найден в столбце $keywordColumnIndex"
                                )
                                break
                            }
                        }
                        if (found) break
                    }
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
                    for (i in (headerRowIndex + 2) until sheet.physicalNumberOfRows) {
                        val row = sheet.getRow(i) ?: continue
                        for (cell in row) {
                            if (cell.toString().contains(keyword, ignoreCase = true)) {
                                keywordColumnIndex = cell.columnIndex
                                index = false
                                break
                            } else {
                                index = true
                            }
                        }
                        if (index){
                            index = false
                            continue
                        }
                        val dayCellRaw = getMergedCellValue(sheet, mergedRegions, i, 6)

                        if (dayCellRaw.isNotBlank()) {
                            val dayFromExcel = dayCellRaw.replace("\\s+".toRegex(), " ").uppercase().trim()
                            if (dayFromExcel == targetDayString) {
                                isTargetDay = true
                                if (!dayHeaderAdded) {
                                    scheduleItems.add(ScheduleListItemPrepod.DayHeader(dayCellRaw.trim()))
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
                            val lessonDisciplineAndType = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex - 2)
                            val actualAudience = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                            val groupName = getMergedCellValue(sheet, mergedRegions, 23, keywordColumnIndex).replace("Группа", "").trim()

                            val parts = lessonDisciplineAndType.split("\n").map { it.trim() }
                            val discipline = parts.getOrElse(0) { "" }
                            val type = parts.getOrElse(1) { "" }

                            if (timeCell.isNotBlank() && discipline.isNotBlank()) {
                                Log.d("ExcelSearch", "Найдена запись: Время: $timeCell, Дисциплина: $discipline, Тип: $type, Преподаватель (ключ): $keyword, Аудитория: $actualAudience, Группа: $groupName")

                                if (discipline.equals(type, ignoreCase = true) && discipline.isNotBlank()) {
                                    scheduleItems.add(ScheduleListItemPrepod.SpecialNoteItem(discipline))
                                } else {
                                    scheduleItems.add(ScheduleListItemPrepod.PairItem(
                                        time = timeCell,
                                        discipline = discipline,
                                        type = type,
                                        teacher = keyword,
                                        audience = actualAudience.ifBlank { "—" },
                                        group = groupName.ifBlank { "—" }
                                    ))
                                }
                            }
                        }
                    }
                } else {
                    Log.d("ExcelSearch", "Преподаватель '$keyword' не найден в заголовках.")
                }
            }
            Log.d("ExcelSearch", "Поиск на сегодня завершён. Найдено элементов: ${scheduleItems.size}")
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла (сегодня)", e)
            scheduleItems.clear()
            scheduleItems.add(ScheduleListItemPrepod.SpecialNoteItem("Ошибка при чтении файла расписания: ${e.message}"))
        }
        return scheduleItems
    }

    private fun searchInExcel(filePath: String, keyword: String): List<ScheduleListItemPrepod> {
        val scheduleItems = mutableListOf<ScheduleListItemPrepod>()
        try {
            Log.d("ExcelSearch", "Чтение файла (вся неделя, ключ: $keyword)...")
            val fileInputStream = FileInputStream(filePath)
            XSSFWorkbook(fileInputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                var found = false
                var index = false
                var keywordRowIndex = -1
                var keywordColumnIndex = -1

                val headerRowIndex = 23
                val headerRow = sheet.getRow(headerRowIndex)
                if (headerRow != null) {
                    for (row in sheet) {
                        for (cell in row) {
                            if (cell.toString().trim().contains(keyword, ignoreCase = true)) {
                                keywordRowIndex = row.rowNum
                                keywordColumnIndex = cell.columnIndex
                                found = true
                                Log.d(
                                    "ExcelSearch",
                                    "Преподаватель '$keyword' найден в столбце $keywordColumnIndex"
                                )
                                break
                            }
                        }
                        if (found) break
                    }
                }

                if (found) {
                    var currentDayHeader = ""
                    val mergedRegions = sheet.mergedRegions


                    for (i in (headerRowIndex + 2) until sheet.physicalNumberOfRows) {
                        val row = sheet.getRow(i) ?: continue
                        for (cell in row) {
                            if (cell.toString().contains(keyword, ignoreCase = true)) {
                                keywordColumnIndex = cell.columnIndex
                                index = false
                                break
                            } else {
                                index = true
                            }
                        }
                        if (index){
                            index = false
                            continue
                        }
                        val dayCellRaw = getMergedCellValue(sheet, mergedRegions, i, 6) // Столбец G (дни недели)

                        if (dayCellRaw.isNotBlank() && dayCellRaw != currentDayHeader) {
                            scheduleItems.add(ScheduleListItemPrepod.DayHeader(dayCellRaw.trim()))
                            currentDayHeader = dayCellRaw
                            Log.d("ExcelSearch", "Добавлен заголовок дня: $dayCellRaw")
                        }

                        val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7) // Столбец H (время)
                        val lessonDisciplineAndType = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex-2)
                        val actualAudience = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                        val groupName = getMergedCellValue(sheet, mergedRegions, 23, keywordColumnIndex).replace("Группа", "").trim()

                        val parts = lessonDisciplineAndType.split("\n").map { it.trim() }
                        val discipline = parts.getOrElse(0) { "" }
                        val type = parts.getOrElse(1) { "" }

                        if (timeCell.isNotBlank() && discipline.isNotBlank()) {
                            Log.d("ExcelSearch", "Найдена запись: Время: $timeCell, Дисциплина: $discipline, Тип: $type, Преподаватель (ключ): $keyword, Аудитория: $actualAudience, Группа: $groupName")
                            if (discipline.equals(type, ignoreCase = true) && discipline.isNotBlank()) {
                                scheduleItems.add(ScheduleListItemPrepod.SpecialNoteItem(discipline))
                            } else {
                                scheduleItems.add(ScheduleListItemPrepod.PairItem(
                                    time = timeCell,
                                    discipline = discipline,
                                    type = type,
                                    teacher = keyword,
                                    audience = actualAudience.ifBlank { "—" },
                                    group = groupName.ifBlank { "—" }
                                ))
                            }
                        }
                    }
                } else {
                    Log.d("ExcelSearch", "Преподаватель '$keyword' не найден в заголовках.")
                }
            }
            Log.d("ExcelSearch", "Поиск по неделе завершён. Найдено элементов: ${scheduleItems.size}")
        } catch (e: Exception) {
            Log.e("ExcelSearch", "Ошибка при чтении файла (неделя)", e)
            scheduleItems.clear()
            scheduleItems.add(ScheduleListItemPrepod.SpecialNoteItem("Ошибка при чтении файла расписания: ${e.message}"))
        }
        return scheduleItems
    }

    // --- Остальные вспомогательные методы (без изменений) ---

    private fun getMergedCellValue(sheet: XSSFSheet, mergedRegions: List<CellRangeAddress>, row: Int, col: Int): String {
        for (range in mergedRegions) {
            if (range.isInRange(row, col)) {
                val firstRow = sheet.getRow(range.firstRow)
                if (firstRow != null) {
                    val mergedCell = firstRow.getCell(range.firstColumn)
                    return mergedCell?.toString()?.trim() ?: ""
                }
                return ""
            }
        }
        val currentRow = sheet.getRow(row)
        if (currentRow != null) {
            val cell = currentRow.getCell(col)
            return cell?.toString()?.trim() ?: ""
        }
        return ""
    }

    private val client: OkHttpClient by lazy { createHttpClient() }

    private fun createHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
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
                }
            }
        }
    }

    private fun setupAutoCompleteTextView(suggestions: List<String>) {
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions.toMutableList())
        keywordInput.setAdapter(adapter)
        keywordInput.threshold = 1

        keywordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}