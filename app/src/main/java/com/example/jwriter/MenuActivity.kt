package com.example.jwriter

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MenuActivity : AppCompatActivity() {

    private lateinit var beginButton: Button
    private lateinit var statsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var lessonButton: Button
    private lateinit var logo: ImageView
    private var numItemsToReview: Int = 0
    private var kanaToReview = ArrayList<Kana>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        for (kana in JWriterDatabase.getInstance(this).kanaDao().getKana()) {
            //Check if there is a review time, and if so, check if the current time has passed the stored review time
            // Review time is calculated during review answers
            if (kana.reviewTime != null) {
                if (kana.reviewTime!! < System.currentTimeMillis()) {
                    numItemsToReview++
                    kanaToReview.add(kana)
                }
            }
        }

        setContentView(R.layout.activity_menu)

        val numReviewTextView = findViewById<TextView>(R.id.numItemsTextView)
        numReviewTextView.text = numItemsToReview.toString()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.overflowIcon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(R.color.white, BlendModeCompat.SRC_ATOP)
        setSupportActionBar(toolbar)

        lessonButton = findViewById(R.id.lessonButton)
        lessonButton.setOnClickListener {
            startActivity(Intent(this, LessonActivity::class.java))
        }

        beginButton = findViewById(R.id.reviewButton)
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

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.report -> println("report")
            R.id.faq -> println("faq")
            R.id.refresh -> refreshActivity()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}