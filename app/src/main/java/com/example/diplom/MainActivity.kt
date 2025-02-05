package com.example.diplom

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val Raspisanie = findViewById<Button>(R.id.button1)
        val Prepodavateli = findViewById<Button>(R.id.button2)
        val Novosti = findViewById<Button>(R.id.button3)
        val Seting = findViewById<Button>(R.id.button4)


        // Устанавливаем первый фрагмент по умолчанию
        replaceFragment(Raspisanie())

        // Обработчики кнопок для переключения фрагментов
        Raspisanie.setOnClickListener { replaceFragment(Raspisanie()) }
        Prepodavateli.setOnClickListener { replaceFragment(Prepodavateli()) }
        Novosti.setOnClickListener { replaceFragment(Novosti()) }
        Seting.setOnClickListener { replaceFragment(Seting()) }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
