// ReminderSchedulerWorker.kt
package com.example.diplom


import android.content.Context
import androidx.work.*
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.io.FileOutputStream
import okhttp3.Request
import okhttp3.OkHttpClient
import org.apache.poi.ss.util.CellRangeAddress
import java.util.Locale
import android.util.Log

class ReminderSchedulerWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val group = prefs.getString("group", null)
        val remindersEnabled = prefs.getBoolean("reminders", false)
        val reminderMinutes = prefs.getInt("reminder_time", 0)
        Log.d("WorkerDebug", "Starting ReminderSchedulerWorker. Group: $group, Reminders Enabled: $remindersEnabled")

        if (!remindersEnabled || group == null) {
            return Result.success()
        }

        // Скачиваем или используем кэшированный Excel-файл
        val filePath = downloadExcelFile() ?: return Result.failure()
        val dates = (0..6).map { LocalDate.now().plusDays(it.toLong()) }

        dates.forEach { date ->
            val scheduleItems = getScheduleForGroupAndDate(filePath, group, date)
            scheduleItems.forEachIndexed { index, item ->
                if (item is ScheduleListItem.PairItem) {
                    val fullTimeString = item.time // Получаем полную строку, например "23:40-00:00"

                    // Находим индекс первого дефиса. Если его нет, берем всю строку.
                    val endIndex = fullTimeString.indexOf('-')
                    val startTimeRaw = if (endIndex != -1) {
                        fullTimeString.substring(0, endIndex)
                    } else {
                        fullTimeString
                    }
                    val startTimeStr = startTimeRaw.replace('.', ':')
                    val startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
                    val classDateTime = LocalDateTime.of(date, startTime)
                    val reminderDateTime = classDateTime.minusMinutes(reminderMinutes.toLong())
                    val now = LocalDateTime.now()

                    if (reminderDateTime.isAfter(now)) {
                        val delay = Duration.between(now, reminderDateTime).toMillis()
                        val workRequest = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
                            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .addTag("reminder")
                            .setInputData(workDataOf(
                                "discipline" to item.discipline,
                                "time" to item.time,
                                "type" to item.type,
                                "teacher" to item.teacher,
                                "audience" to item.audience
                            ))
                            .build()
                        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                            "reminder_${date}_${index}",
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                    }
                }
            }
        }
        return Result.success()
    }

    private fun downloadExcelFile(): String? {
        val request = Request.Builder().url("http://212.109.221.255/files/Raspisanie.xlsx").build()
        val response = OkHttpClient().newCall(request).execute()
        return if (response.isSuccessful) {
            val file = File(applicationContext.getExternalFilesDir(null), "downloaded.xlsx")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } else null
    }

    private fun getScheduleForGroupAndDate(filePath: String, group: String, date: LocalDate): List<ScheduleListItem> {
        val scheduleItems = mutableListOf<ScheduleListItem>()
        FileInputStream(filePath).use { fis ->
            XSSFWorkbook(fis).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                var keywordRowIndex = -1
                var keywordColumnIndex = -1
                for (row in sheet) {
                    for (cell in row) {
                        if (cell.toString().trim().contains(group, ignoreCase = true)) {
                            keywordRowIndex = row.rowNum
                            keywordColumnIndex = cell.columnIndex
                            break
                        }
                    }
                    if (keywordRowIndex != -1) break
                }
                if (keywordRowIndex != -1) {
                    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru"))
                    val formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM"))
                    val targetDayString = "$dayOfWeek $formattedDate".replace("\\s+".toRegex(), " ").uppercase()
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
                                }
                            } else if (isTargetDay) {
                                break
                            }
                        }
                        if (isTargetDay) {
                            val timeCell = getMergedCellValue(sheet, mergedRegions, i, 7)
                            val disciplineCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex)
                            val typeCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 1)
                            val teacherCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 2)
                            val audienceCell = getMergedCellValue(sheet, mergedRegions, i, keywordColumnIndex + 3)
                            if (timeCell.isNotBlank() && disciplineCell.isNotBlank()) {
                                scheduleItems.add(ScheduleListItem.PairItem(
                                    timeCell, disciplineCell, typeCell,
                                    teacherCell.ifBlank { "—" }, audienceCell.ifBlank { "—" }
                                ))
                            }
                        }
                    }
                }
            }
        }
        return scheduleItems
    }

    private fun getMergedCellValue(sheet: XSSFSheet, mergedRegions: List<CellRangeAddress>, row: Int, col: Int): String {
        for (range in mergedRegions) {
            if (range.isInRange(row, col)) {
                val firstRow = sheet.getRow(range.firstRow) ?: return ""
                val mergedCell = firstRow.getCell(range.firstColumn)
                return mergedCell?.toString()?.trim() ?: ""
            }
        }
        val currentRow = sheet.getRow(row) ?: return ""
        val cell = currentRow.getCell(col)
        return cell?.toString()?.trim() ?: ""

    }
}