package com.example.jwriter.activity

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.marginBottom
import com.example.jwriter.*
import com.example.jwriter.database.JWriterDatabase
import com.example.jwriter.database.Kana
import com.example.jwriter.notification.NotificationReceiver
import com.example.jwriter.util.AnimUtilities
import com.google.android.material.button.MaterialButton
import java.time.Duration
import java.util.*


class MenuActivity : AppCompatActivity() {

    private lateinit var reviewButton: Button
    private lateinit var statsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var lessonButton: Button
    private lateinit var showMoreLayout: RelativeLayout
    private lateinit var summaryLayout: RelativeLayout
    private lateinit var levelsLayout: LinearLayout
    private lateinit var showMoreArrow: ImageView
    private lateinit var summaryButton: Button

    private var showMore = true
    private var moving = false
    private var numItemsToReview: Int = 0
    private var mostRecentReview = Long.MAX_VALUE
    private var kanaToReview = ArrayList<Kana>()
    private var levelsArray = ArrayList<View>()
    private var levelsItemArray = IntArray(5)

    private val ROOKIE = 0
    private val AMATEUR = 1
    private val EXPERT = 2
    private val MASTER = 3
    private val SENSEI = 4

    val MotionEvent.up get() = action == MotionEvent.ACTION_UP

    private fun MotionEvent.isIn(view: View): Boolean {
        val rect = Rect(view.left, view.top, view.right, view.bottom)
        return rect.contains((view.left + x).toInt(), (view.top + y).toInt())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        if (1 == 2) {
            startActivity(Intent(this, IntroActivity::class.java))
        } else {
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

            myAlarm()

            val numReviewTextView = findViewById<TextView>(R.id.numItemsTextView)
            numReviewTextView.text = numItemsToReview.toString()

            val nextReviewTime = findViewById<TextView>(R.id.nextReviewText)
            if (mostRecentReview == Long.MAX_VALUE) {
                nextReviewTime.text = "Time to next review: none"
            } else {
                nextReviewTime.text = "Time to next review: ${formatTime(mostRecentReview)}"
            }

            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            toolbar.overflowIcon?.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    R.color.white, BlendModeCompat.SRC_ATOP
                )
            setSupportActionBar(toolbar)

            summaryButton = findViewById(R.id.summaryButton)
            showMoreLayout = findViewById(R.id.showMoreRelativeLayout)
            showMoreArrow = findViewById(R.id.arrowImageView)
            summaryLayout = findViewById(R.id.summaryRelativeLayout)
            levelsLayout = findViewById(R.id.levelsLayout)

            val rookie = levelsLayout.findViewById<LinearLayout>(R.id.rookie)
            val amateur = levelsLayout.findViewById<LinearLayout>(R.id.amateur)
            val expert = levelsLayout.findViewById<LinearLayout>(R.id.expert)
            val master = levelsLayout.findViewById<LinearLayout>(R.id.master)
            val sensei = levelsLayout.findViewById<LinearLayout>(R.id.sensei)
            levelsArray.add(rookie)
            levelsArray.add(amateur)
            levelsArray.add(expert)
            levelsArray.add(master)
            levelsArray.add(sensei)

            for (kana in JWriterDatabase.getInstance(this).kanaDao().getKana()) {
                if (kana.level != null) {
                    println(kana.level)
                    when (kana.level) {
                        1, 2 -> levelsItemArray[ROOKIE] += 1
                        3 -> levelsItemArray[AMATEUR] += 1
                        4 -> levelsItemArray[EXPERT] += 1
                        5 -> levelsItemArray[MASTER] += 1
                        6 -> levelsItemArray[SENSEI] += 1
                    }
                }
            }

            for ((index, level) in levelsArray.withIndex()) {
                val name = level.context.resources.getResourceEntryName(level.id)
                level.findViewById<TextView>(resources.getIdentifier("${name}NumKanaTextView", "id", packageName)).text = levelsItemArray[index].toString()
               level.setOnClickListener {
                    println("level")
                }
            }

            showMoreArrow.setOnClickListener {

                showMoreArrow.isSelected = !showMoreArrow.isSelected

                if (showMore && !moving) {
                    moving = true
                    showMoreArrow.isEnabled = false
                    AnimUtilities.slideView(
                        summaryLayout,
                        summaryLayout.height,
                        summaryLayout.height + levelsLayout.measuredHeight + levelsLayout.marginBottom

                    ) {}
                    AnimUtilities.slideView(
                        summaryButton,
                        summaryButton.height,
                        summaryButton.height + levelsLayout.measuredHeight + levelsLayout.marginBottom
                    ) {
                        showMore = false
                        moving = false
                        showMoreArrow.isEnabled = true
                    }
                    showMoreArrow.setImageResource(android.R.drawable.arrow_up_float)
                } else if (!showMore && !moving) {
                    moving = true
                    showMoreArrow.isEnabled = false
                    AnimUtilities.slideView(
                        summaryLayout,
                        summaryLayout.height,
                        summaryLayout.height -  levelsLayout.measuredHeight - levelsLayout.marginBottom
                    ) {}
                    AnimUtilities.slideView(
                        summaryButton,
                        summaryButton.height,
                        summaryButton.height - levelsLayout.measuredHeight - levelsLayout.marginBottom
                    ) {
                        moving = false
                        showMore = true
                        showMoreArrow.isEnabled = true
                    }
                    showMoreArrow.setImageResource(android.R.drawable.arrow_down_float)
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
            R.id.report -> showReportDialog()
            R.id.faq -> showFAQ()
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

    private fun showFAQ() {
        val view = layoutInflater.inflate(R.layout.faq_dialog, null)
        val builder = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()
        builder.show()
    }

    private fun showReportDialog() {
        val view = layoutInflater.inflate(R.layout.report_dialog, null)
        val builder = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()

        view.findViewById<MaterialButton>(R.id.reportButton).setOnClickListener {
            val text = view.findViewById<TextView>(R.id.reportTextView).text
            val email = Intent(Intent.ACTION_VIEW)
            val data = Uri.parse("mailto:?subject=JWriter email&body=$text&to=report.jwriter@gmail.com")
            email.data = data
            try {
                startActivity(Intent.createChooser(email, "Send mail..."))
                finish()
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                    this,
                    "There is no email client installed.", Toast.LENGTH_SHORT
                ).show()
            }

            builder.dismiss()
        }
        builder.show()
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

    @SuppressLint("UnspecifiedImmutableFlag")
    fun myAlarm() {
        val calendar = Calendar.getInstance()
        calendar[Calendar.HOUR_OF_DAY] = 20
        calendar[Calendar.MINUTE] = 43
        calendar[Calendar.SECOND] = 0
        if (calendar.time < Date()) calendar.add(Calendar.DAY_OF_MONTH, 1)
        val intent = Intent(applicationContext, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

}