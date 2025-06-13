package com.example.diplom

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class Seting : Fragment() {

    private val PREFS_NAME = "MyAppPrefs"
    private val KEY_IS_LOGGED_IN = "isLoggedIn"
    private val KEY_LOGIN = "login"
    private val KEY_PASSWORD = "password"
    private val KEY_GROUP = "group"

    // Ключи для офлайн-режима
    private val KEY_OFFLINE_MODE_ENABLED = "offline_mode_enabled"
    private val KEY_OFFLINE_FILE_PATH = "offline_file_path"
    private val OFFLINE_SCHEDULE_FILENAME = "schedule.xlsx" // Имя файла, который будем сохранять
    private val SCHEDULE_DOWNLOAD_URL = "http://212.109.221.255/files/Raspisanie.xlsx" // URL файла для скачивания
    private var downloadID: Long = -1L

    private lateinit var switchNotifications: Switch
    private lateinit var switchReminders: Switch
    private lateinit var switchOffline: Switch
    private lateinit var editMinutes: EditText
    private lateinit var appContext: Context

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadID) {
                val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(statusIndex)) {
                        Toast.makeText(appContext, "Файл для офлайн-режима успешно скачан", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(appContext, "Ошибка скачивания файла", Toast.LENGTH_SHORT).show()
                        deleteOfflineFile()
                    }
                }
                cursor.close()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = requireContext().applicationContext
        appContext.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        appContext.unregisterReceiver(onDownloadComplete)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_seting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appContext = requireContext().applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val textLogin = view.findViewById<AutoCompleteTextView>(R.id.textLogin)
        val textPassword = view.findViewById<AutoCompleteTextView>(R.id.textPassword)
        val contentContainer = view.findViewById<ConstraintLayout>(R.id.contentContainer)
        val tvFullName = view.findViewById<TextView>(R.id.tvFullName)
        val tvGroup = view.findViewById<TextView>(R.id.tvGroup)

        switchNotifications = view.findViewById(R.id.switchNotifications)
        switchReminders = view.findViewById(R.id.switchReminders)
        switchOffline = view.findViewById(R.id.switchOfflineMode)
        editMinutes = view.findViewById(R.id.editMinutes)

        fun saveAutoSettings() {
            val login = prefs.getString(KEY_LOGIN, null)
            val password = prefs.getString(KEY_PASSWORD, null)
            if (login != null && password != null) {
                saveSettings(login, password)
            }
        }

        switchOffline.setOnCheckedChangeListener { _, isChecked ->
            handleOfflineSwitch(isChecked)
            saveAutoSettings()
        }

        switchNotifications.setOnCheckedChangeListener { _, _ -> saveAutoSettings() }
        switchReminders.setOnCheckedChangeListener { _, isChecked ->
            saveAutoSettings()
            if (!isChecked) {
                WorkManager.getInstance(appContext).cancelAllWorkByTag("reminder")
            }
            scheduleReminderScheduler()
        }
        editMinutes.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { saveAutoSettings() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fun showLoggedInUI() {
            btnLogin.visibility = View.GONE
            textLogin.visibility = View.GONE
            textPassword.visibility = View.GONE
            contentContainer.visibility = View.VISIBLE
            btnLogout.visibility = View.VISIBLE
        }

        fun showLoggedOutUI() {
            btnLogin.visibility = View.VISIBLE
            textLogin.visibility = View.VISIBLE
            textPassword.visibility = View.VISIBLE
            contentContainer.visibility = View.GONE
            btnLogout.visibility = View.GONE
        }

        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val login = prefs.getString(KEY_LOGIN, null)
        val password = prefs.getString(KEY_PASSWORD, null)

        if (isLoggedIn && login != null && password != null) {
            showLoggedInUI()
            fetchUserData(view)
            fetchSettings(login, password)
            // И здесь вызываем планировщик, если пользователь уже залогинен
            // (fetchSettings сам вызовет его в конце, но можно и здесь для надежности,
            // хотя fetchSettings уже покроет этот случай)
        } else {
            showLoggedOutUI()
        }

        btnLogin.setOnClickListener {
            val loginText = textLogin.text.toString()
            val passwordText = textPassword.text.toString()

            if (loginText.isBlank() || passwordText.isBlank()) {
                Toast.makeText(appContext, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val queue = Volley.newRequestQueue(appContext)
            val url = "http://212.109.221.255/db.php"

            val params = JSONObject().apply {
                put("login", loginText)
                put("password", passwordText)
            }

            val request = JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                { response ->
                    if (response.optBoolean("success", false)) {
                        prefs.edit()
                            .putBoolean(KEY_IS_LOGGED_IN, true)
                            .putString(KEY_LOGIN, loginText)
                            .putString(KEY_PASSWORD, passwordText)
                            .putString(KEY_GROUP, response.optString("group_name", ""))
                            .apply()

                        if (isAdded) {
                            showLoggedInUI()
                            val name = response.optString("name", "Не указано")
                            val groupName = response.optString("group_name", "Не указано")
                            tvFullName.text = name
                            tvGroup.text = groupName
                            fetchSettings(loginText, passwordText)
                            Toast.makeText(appContext, "Вход выполнен", Toast.LENGTH_SHORT).show()

                            // --- Вызов scheduleReminderScheduler() после успешного входа ---
                            scheduleReminderScheduler() // Теперь эта функция будет найдена
                        }
                    } else {
                        if (isAdded) {
                            Toast.makeText(appContext, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                { error ->
                    error.printStackTrace()
                    if (isAdded) {
                        Toast.makeText(appContext, "Ошибка подключения: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            )

            queue.add(request)
        }

        btnLogout.setOnClickListener {
            WorkManager.getInstance(appContext).cancelAllWorkByTag("reminder")
            // Отменяем также периодический WorkManager для ReminderScheduler
            WorkManager.getInstance(appContext).cancelUniqueWork("ReminderSchedulerPeriodic")

            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_LOGIN)
                .remove(KEY_PASSWORD)
                .remove(KEY_GROUP)
                .apply()

            if (isAdded) {
                showLoggedOutUI()
                tvFullName.text = ""
                tvGroup.text = ""
                Toast.makeText(appContext, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleOfflineSwitch(isEnabled: Boolean) {
        if (isEnabled) {
            downloadOfflineFile()
        } else {
            deleteOfflineFile()
        }
    }

    private fun downloadOfflineFile() {
        val file = File(appContext.getExternalFilesDir(null), OFFLINE_SCHEDULE_FILENAME)
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(Uri.parse(SCHEDULE_DOWNLOAD_URL))
            .setTitle("Скачивание расписания")
            .setDescription("Загрузка данных для офлайн-режима")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(appContext, null, OFFLINE_SCHEDULE_FILENAME)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadID = downloadManager.enqueue(request)

        val filePath = File(appContext.getExternalFilesDir(null), OFFLINE_SCHEDULE_FILENAME).absolutePath
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_OFFLINE_FILE_PATH, filePath)
            .putBoolean(KEY_OFFLINE_MODE_ENABLED, true)
            .apply()

        Log.d("OfflineMode", "Запущено скачивание файла. Путь: $filePath")
    }

    private fun deleteOfflineFile() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val filePath = prefs.getString(KEY_OFFLINE_FILE_PATH, null)

        if (filePath != null) {
            val file = File(filePath)
            if (file.exists() && file.delete()) {
                Toast.makeText(appContext, "Офлайн-данные удалены", Toast.LENGTH_SHORT).show()
                Log.d("OfflineMode", "Офлайн-файл удален: $filePath")
            }
        }
        prefs.edit()
            .remove(KEY_OFFLINE_FILE_PATH)
            .putBoolean(KEY_OFFLINE_MODE_ENABLED, false)
            .apply()
    }

    // --- Перемещаем scheduleReminderScheduler() сюда, чтобы она была доступна из всего класса ---
    private fun scheduleReminderScheduler() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remindersEnabled = prefs.getBoolean("reminders", false) // Проверьте, включены ли напоминания в настройках
        Log.d("SetingFragment", "scheduleReminderScheduler() called. Reminders Enabled: $remindersEnabled")

        if (remindersEnabled) {
            // Отменяем любую предыдущую периодическую работу, чтобы избежать дублирования
            WorkManager.getInstance(appContext).cancelUniqueWork("ReminderSchedulerPeriodic")

            // Создаем PeriodicWorkRequest для запуска ReminderSchedulerWorker
            // Например, запускать раз в 24 часа. Adjust time according to your needs.
            // Минимальный интервал для PeriodicWorkRequest - 15 минут.
            val workRequest = PeriodicWorkRequestBuilder<ReminderSchedulerWorker>(
                24, TimeUnit.HOURS // Запускать раз в 24 часа
            )
                // Добавьте ограничения, если нужно (например, только при зарядке, или при наличии сети)
                // .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag("ReminderScheduler") // Тег для удобного управления
                .build()

            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                "ReminderSchedulerPeriodic", // Уникальное имя для периодической работы
                ExistingPeriodicWorkPolicy.UPDATE, // Обновляем существующую работу, если она уже есть
                workRequest
            )
            Log.d("SetingFragment", "ReminderSchedulerWorker scheduled.")
        } else {
            // Если напоминания выключены, отмените все задачи ReminderScheduler
            WorkManager.getInstance(appContext).cancelUniqueWork("ReminderSchedulerPeriodic")
            WorkManager.getInstance(appContext).cancelAllWorkByTag("reminder") // Отменяем также все конкретные уведомления
            Log.d("SetingFragment", "ReminderSchedulerWorker cancelled.")
        }
    }

    private fun fetchUserData(view: View) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val login = prefs.getString(KEY_LOGIN, null)
        val password = prefs.getString(KEY_PASSWORD, null)

        if (login.isNullOrBlank() || password.isNullOrBlank()) {
            if (isAdded) {
                Toast.makeText(appContext, "Не удалось загрузить данные авторизации", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val queue = Volley.newRequestQueue(appContext)
        val url = "http://212.109.221.255/db.php"

        val params = JSONObject().apply {
            put("action", "get_user_data")
            put("login", login)
            put("password", password)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                if (response.optBoolean("success", false)) {
                    val name = response.optString("name", "Не указано")
                    val groupName = response.optString("group_name", "Не указано")
                    if (isAdded) {
                        view.findViewById<TextView>(R.id.tvFullName)?.text = name
                        view.findViewById<TextView>(R.id.tvGroup)?.text = groupName
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(appContext, response.optString("error", "Ошибка загрузки данных"), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { error ->
                error.printStackTrace()
                if (isAdded) {
                    Toast.makeText(appContext, "Ошибка загрузки данных: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        )

        queue.add(request)
    }

    private fun saveSettings(login: String, password: String) {
        // Захватываем значения перед отправкой запроса
        val notificationsEnabled = switchNotifications.isChecked
        scheduleReminderScheduler()
        val remindersEnabled = switchReminders.isChecked
        val offlineModeEnabled = switchOffline.isChecked
        val reminderTime = try {
            editMinutes.text.toString().toInt()
        } catch (e: NumberFormatException) {
            0
        }

        val queue = Volley.newRequestQueue(appContext)
        val url = "http://212.109.221.255/db.php"

        val params = JSONObject().apply {
            put("action", "save_settings")
            put("login", login)
            put("password", password)
            put("notifications", notificationsEnabled)
            put("reminders", remindersEnabled)
            put("offline_mode", offlineModeEnabled)
            put("reminder_time", reminderTime)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                if (response.optBoolean("success", false)) {
                    // Используем appContext для SharedPreferences
                    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putBoolean("notifications", notificationsEnabled)
                        .putBoolean("reminders", remindersEnabled)
                        .putBoolean("offline_mode", offlineModeEnabled)
                        .putInt("reminder_time", reminderTime)
                        .apply()
                    scheduleReminderScheduler()
                } else {
                    // Показываем Toast только если фрагмент прикреплён
                    if (isAdded) {
                        Toast.makeText(appContext, "Ошибка при сохранении", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { error ->
                error.printStackTrace()
                if (isAdded) {
                    Toast.makeText(appContext, "Ошибка сохранения: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        )

        queue.add(request)
    }

    private fun fetchSettings(login: String, password: String) {
        val queue = Volley.newRequestQueue(appContext)
        val url = "http://212.109.221.255/db.php"

        val params = JSONObject().apply {
            put("action", "get_settings")
            put("login", login)
            put("password", password)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                if (response.optBoolean("success", false)) {
                    val notifications = response.optBoolean("notifications", true)
                    scheduleReminderScheduler()
                    val reminders = response.optBoolean("reminders", false)
                    scheduleReminderScheduler()
                    val offlineMode = response.optBoolean("offline_mode", false)
                    val reminderTime = response.optInt("reminder_time", 60) // Значение по умолчанию 480 минут
                    // Обновляем UI только если фрагмент прикреплён
                    if (isAdded) {
                        switchNotifications.isChecked = notifications
                        scheduleReminderScheduler()
                        switchReminders.isChecked = reminders
                        switchOffline.isChecked = offlineMode
                        editMinutes.setText(reminderTime.toString())
                    }
                    // Сохраняем в SharedPreferences всегда
                    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putBoolean("notifications", notifications)
                        .putBoolean("reminders", reminders)
                        .putBoolean("offline_mode", offlineMode)
                        .putInt("reminder_time", reminderTime)
                        .apply()
                    scheduleReminderScheduler()

                    // --- Вызов scheduleReminderScheduler() после загрузки настроек ---
                    // Он будет вызван после установки switchReminders.isChecked
                    // Использование post дважды, чтобы убедиться, что все UI-обновления завершены
                    // и prefs успели обновиться.
                    if (isAdded) { // Важно убедиться, что фрагмент прикреплен
                        view?.post { // Используем view.post или requireView().post

                        }
                    }

                }
            },
            { error ->
                error.printStackTrace()
                if (isAdded) {
                    Toast.makeText(appContext, "Ошибка загрузки настроек: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        )

        queue.add(request)
    }
}