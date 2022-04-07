package com.example.jwriter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MenuActivity : AppCompatActivity() {

    private lateinit var beginButton: Button
    private lateinit var statsButton: Button
    private lateinit var settingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_menu)

        beginButton = findViewById(R.id.beginButton)
        beginButton.setOnClickListener {
            startActivity(Intent(this, ReviewActivity::class.java))
        }

        statsButton = findViewById(R.id.statsButton)
        statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}