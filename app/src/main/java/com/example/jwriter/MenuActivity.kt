package com.example.jwriter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MenuActivity : AppCompatActivity() {

    private lateinit var beginButton: Button
    private lateinit var statsButton: Button
    private lateinit var settingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        beginButton = findViewById(R.id.beginButton)
        beginButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        statsButton = findViewById(R.id.statsButton)
        statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val db = JWriterDatabase.getInstance(this)

        if (db?.userDao()?.getUsers()?.isNotEmpty() == true) {

        } else {
            val newUser = User(0)
            db?.userDao()?.insertAll(newUser)
        }

    }
}