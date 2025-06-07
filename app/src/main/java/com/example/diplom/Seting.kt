package com.example.diplom

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class Seting : Fragment() {

    private val PREFS_NAME = "MyAppPrefs"
    private val KEY_IS_LOGGED_IN = "isLoggedIn"
    private val KEY_LOGIN = "login"
    private val KEY_PASSWORD = "password"

    private lateinit var switchNotifications: Switch
    private lateinit var switchReminders: Switch
    private lateinit var switchOffline: Switch
    private lateinit var editMinutes: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_seting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

        switchNotifications.setOnCheckedChangeListener { _, _ -> saveAutoSettings() }
        switchReminders.setOnCheckedChangeListener { _, _ -> saveAutoSettings() }
        switchOffline.setOnCheckedChangeListener { _, _ -> saveAutoSettings() }
        editMinutes.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                saveAutoSettings()
            }
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
        } else {
            showLoggedOutUI()
        }

        btnLogin.setOnClickListener {
            val loginText = textLogin.text.toString()
            val passwordText = textPassword.text.toString()

            if (loginText.isBlank() || passwordText.isBlank()) {
                Toast.makeText(requireContext(), "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val queue = Volley.newRequestQueue(requireContext())
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
                            .apply()

                        showLoggedInUI()

                        val name = response.optString("name", "Не указано")
                        val groupName = response.optString("group_name", "Не указано")
                        tvFullName.text = name
                        tvGroup.text = groupName
                        fetchSettings(loginText, passwordText)
                        Toast.makeText(requireContext(), "Вход выполнен", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    error.printStackTrace()
                    Toast.makeText(requireContext(), "Ошибка подключения: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )

            queue.add(request)
        }

        btnLogout.setOnClickListener {
            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_LOGIN)
                .remove(KEY_PASSWORD)
                .apply()

            showLoggedOutUI()
            tvFullName.text = ""
            tvGroup.text = ""
            Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserData(view: View) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val login = prefs.getString(KEY_LOGIN, null)
        val password = prefs.getString(KEY_PASSWORD, null)

        if (login.isNullOrBlank() || password.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Не удалось загрузить данные авторизации", Toast.LENGTH_SHORT).show()
            return
        }

        val queue = Volley.newRequestQueue(requireContext())
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
                    view.findViewById<TextView>(R.id.tvFullName)?.text = name
                    view.findViewById<TextView>(R.id.tvGroup)?.text = groupName
                } else {
                    Toast.makeText(requireContext(), response.optString("error", "Ошибка загрузки данных"), Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Ошибка загрузки данных: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        queue.add(request)
    }

    private fun saveSettings(login: String, password: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "http://212.109.221.255/db.php"

        val reminderTime = try {
            val minutesInput = editMinutes.text.toString()
            if (minutesInput.isBlank()) {
                0
            } else {
                minutesInput.toInt()
            }
        } catch (e: NumberFormatException) {
            0
        }

        val params = JSONObject().apply {
            put("action", "save_settings")
            put("login", login)
            put("password", password)
            put("notifications", switchNotifications.isChecked)
            put("reminders", switchReminders.isChecked)
            put("offline_mode", switchOffline.isChecked)
            put("reminder_time", reminderTime)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            params,
            { response ->
                if (!response.optBoolean("success", false)) {
                    Toast.makeText(requireContext(), "Ошибка при сохранении", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Ошибка сохранения: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        queue.add(request)
    }

    private fun fetchSettings(login: String, password: String) {
        val queue = Volley.newRequestQueue(requireContext())
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
                    switchNotifications.isChecked = response.optBoolean("notifications", true)
                    switchReminders.isChecked = response.optBoolean("reminders", false)
                    switchOffline.isChecked = response.optBoolean("offline_mode", false)

                    val reminderTime = response.optInt("reminder_time", 480)
                    editMinutes.setText(reminderTime.toString())
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Ошибка загрузки настроек: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        queue.add(request)
    }
}
