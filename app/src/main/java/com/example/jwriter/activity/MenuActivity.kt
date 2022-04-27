package com.example.jwriter.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.jwriter.*
import java.time.Duration
import java.util.*

class MenuActivity : AppCompatActivity() {

    private lateinit var reviewButton: Button
    private lateinit var statsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var lessonButton: Button
    private lateinit var showMoreLayout: RelativeLayout
    private lateinit var summaryButton: Button
    private var showMore = true
    private var numItemsToReview: Int = 0
    private var mostRecentReview = Long.MAX_VALUE
    private var kanaToReview = ArrayList<Kana>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        for (kana in JWriterDatabase.getInstance(this).kanaDao().getKana()) {
            //Check if there is a review time, and if so, check if the current time has passed the stored review time
            // Review time is calculated during review answers and initially added when learned in lessons
            if (kana.reviewTime != null) {
                if (kana.reviewTime!! < System.currentTimeMillis()) {
                    numItemsToReview++
                    kanaToReview.add(kana)
                } else {
                    val millisecondsUntilReview = kana.reviewTime!! - System.currentTimeMillis()
                    if (millisecondsUntilReview < mostRecentReview) {
                        mostRecentReview = millisecondsUntilReview
                    }
                }
            }
        }

        setContentView(R.layout.activity_menu)

        val numReviewTextView = findViewById<TextView>(R.id.numItemsTextView)
        numReviewTextView.text = numItemsToReview.toString()

        val nextReviewTime = findViewById<TextView>(R.id.nextReviewText)
        if (mostRecentReview == Long.MAX_VALUE) {
            nextReviewTime.text = "Time to next review: none"
        } else {
            nextReviewTime.text = "Time to next review: ${formatTime(mostRecentReview)}"
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.overflowIcon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            R.color.white, BlendModeCompat.SRC_ATOP)
        setSupportActionBar(toolbar)

        summaryButton = findViewById(R.id.summary)
        showMoreLayout = findViewById(R.id.showMoreRelativeLayout)
        showMoreLayout.setOnClickListener {
            if (showMore) {
                AnimUtilities.slideView(
                    summaryButton,
                    summaryButton.height,
                    summaryButton.height + 300
                )
                showMore = false
            } else {
                AnimUtilities.slideView(
                    summaryButton,
                    summaryButton.height,
                    summaryButton.height - 300
                )
                showMore = true
            }
        }

        lessonButton = findViewById(R.id.lessonButton)
        lessonButton.setOnClickListener {
            startActivity(Intent(this, LessonActivity::class.java))
        }

        reviewButton = findViewById(R.id.reviewButton)
        reviewButton.setOnClickListener {
            if (numItemsToReview > 0) {
                val intent = Intent(this, ReviewActivity::class.java)
                intent.putExtra("review", true)
                intent.putExtra("kana", kanaToReview)
                startActivity(intent)
            }
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

    private fun formatTime(milliseconds: Long): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = Duration.ofMillis(milliseconds)
            val newSeconds = duration.seconds - duration.toMinutes() * 60
            return duration.run {
                "%02d:%02d:%02d".format(toHours(), toMinutes(), newSeconds)
            }
        } else {
            return ""
        }
    }

}