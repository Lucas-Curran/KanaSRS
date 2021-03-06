package com.email.contact.kanasrs.activity

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.airbnb.lottie.LottieAnimationView
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.util.Utilities
import java.text.SimpleDateFormat

class SettingsActivity : AppCompatActivity() {

    private lateinit var resetAccount: TextView
    private lateinit var setTimeTextView: TextView
    private lateinit var sentAtTextView: TextView
    private lateinit var lessonNumberRadioGroup: RadioGroup
    private lateinit var writingButton: Button
    private lateinit var randomizeButton: Button
    private lateinit var extraInfoRandomize: ImageView
    private lateinit var extraInfoWriting: ImageView

    private var sleepingCatCounter = 0
    private var catToast: Toast? = null

    private val timePickerDialogListener: TimePickerDialog.OnTimeSetListener =
        TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            val sharedPref = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE) ?: return@OnTimeSetListener
            with (sharedPref.edit()) {
                putInt("kanasrsNotificationHour", hourOfDay)
                putInt("kanasrsNotificationMinute", minute)
                apply()
            }
            Utilities.setAlarm(this)

            "Daily notifications are currently sent at ${formatTime(sharedPref.getInt("kanasrsNotificationHour", 12), sharedPref.getInt("kanasrsNotificationMinute", 0))}".also { sentAtTextView.text = it }
            Toast.makeText(this, "Notifications will now be sent at ${formatTime(hourOfDay, minute)}", Toast.LENGTH_SHORT).show()
        }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        setTimeTextView = findViewById(R.id.setTimeTextView)
        sentAtTextView = findViewById(R.id.sentAtTextView)
        lessonNumberRadioGroup = findViewById(R.id.lessonNumberRadioGroup)
        writingButton = findViewById(R.id.writingButton)
        randomizeButton = findViewById(R.id.randomizeButton)
        extraInfoRandomize = findViewById(R.id.randomizeInfoImage)
        extraInfoWriting = findViewById(R.id.writingInfoImage)


         val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(
                ContextCompat.getColor(this, R.color.settings_gradient_1),
                ContextCompat.getColor(this, R.color.settings_gradient_2),
                ContextCompat.getColor(this, R.color.settings_gradient_3),
                ContextCompat.getColor(this, R.color.settings_gradient_4),
                ContextCompat.getColor(this, R.color.settings_gradient_5)
            )
        )

        findViewById<ConstraintLayout>(R.id.settingsConstraint).background = gradientDrawable

        resetAccount = findViewById(R.id.resetAccountTextView)
        resetAccount.setOnClickListener {

            val view = layoutInflater.inflate(R.layout.reset_account_dialog, null)
            val dialog = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()

            view.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            view.findViewById<TextView>(R.id.resetAccountTextView).setOnClickListener {
                this.deleteDatabase("kanasrs.db")
                KanaSRSDatabase.destroyInstance()
                this.getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE).edit().clear().apply()
                val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.setText(resources.getText(R.string.clear_account))
                toast.show()
                startActivity(Intent(this, MenuActivity::class.java))
            }
            dialog.show()
        }

        extraInfoWriting.setOnClickListener {
            showInfoDialog(R.string.writing_info, "Writing Info")
        }

        extraInfoRandomize.setOnClickListener {
            showInfoDialog(R.string.random_font_info, "Random Font Info")
        }

        val sharedPref = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)

        if (sharedPref.getBoolean("kanasrsWritingEnabled", true))  {
            writingButton.text = "Writing\nenabled"
            writingButton.isSelected = true
        } else {
            writingButton.text = "Writing\ndisabled"
            writingButton.isSelected = false
        }

        if (sharedPref.getBoolean("randomizedFonts", false)) {
            randomizeButton.text = "Randomize fonts\nenabled"
            randomizeButton.isSelected = true
        } else {
            randomizeButton.text = "Randomize fonts\ndisabled"
            randomizeButton.isSelected = false
        }

        randomizeButton.setOnClickListener {
            randomizeButton.isSelected = !randomizeButton.isSelected
            if (randomizeButton.isSelected) {
                randomizeButton.text = "Randomize fonts\nenabled"
                with (sharedPref.edit()) {
                    putBoolean("randomizedFonts", true)
                    apply()
                }
            } else {
                randomizeButton.text = "Randomize fonts\ndisabled"
                with (sharedPref.edit()) {
                    putBoolean("randomizedFonts", false)
                    apply()
                }
            }
        }

        writingButton.setOnClickListener {
            writingButton.isSelected = !writingButton.isSelected
            if (writingButton.isSelected) {
                writingButton.text = "Writing\nenabled"
                with (sharedPref.edit()) {
                    putBoolean("kanasrsWritingEnabled", true)
                    apply()
                }
            } else {
                writingButton.text = "Writing\ndisabled"
                with (sharedPref.edit()) {
                    putBoolean("kanasrsWritingEnabled", false)
                    apply()
                }
            }
        }

        val closeActivityImage = findViewById<ImageView>(R.id.closeActivityImage)

        closeActivityImage.visibility = View.VISIBLE
        closeActivityImage.translationX = -300f
        closeActivityImage.animate().translationXBy(300f).setDuration(1000L).start()
        closeActivityImage.setOnClickListener {
            finish()
        }

        setTimeTextView.setOnClickListener {
            val timePicker = TimePickerDialog(
                this,
                timePickerDialogListener,
                sharedPref.getInt("kanasrsNotificationHour", 12),
                sharedPref.getInt("kanasrsNotificationMinute", 0),
                false
            )
            timePicker.setOnCancelListener { Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show() }
            timePicker.show()
        }

        findViewById<TextView>(R.id.sushiLinkTextView).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com/search?q=kaitenzushi"))
            startActivity(browserIntent)
        }

        findViewById<LottieAnimationView>(R.id.catSleepingAnimation).setOnClickListener {
            sleepingCatCounter++

            catToast?.cancel()

            if (sleepingCatCounter == 7) {
                catToast = Toast.makeText(this, "Hey!! I said the cat is sleeping!!", Toast.LENGTH_SHORT)
                catToast?.show()
                sleepingCatCounter = 0
                return@setOnClickListener
            }

            if (sleepingCatCounter % 3 == 0 ) {
                catToast = Toast.makeText(this, "Shhh, the cat is sleeping...", Toast.LENGTH_SHORT)
                catToast?.show()
            } else {
                catToast = Toast.makeText(this, "zzz...", Toast.LENGTH_SHORT)
                catToast?.show()
            }
        }

        lessonNumberRadioGroup.setOnCheckedChangeListener { _, id ->

            val numLesson = when(id) {
                R.id.fiveLessonButton -> 5
                R.id.tenLessonButton -> 10
                R.id.fifteenLessonButton -> 15
                else -> 10
            }
            with (sharedPref.edit()) {
                putInt("kanasrsLessonNumber", numLesson)
                apply()
            }
        }

        when(sharedPref.getInt("kanasrsLessonNumber", 10)) {
            5 -> lessonNumberRadioGroup.check(R.id.fiveLessonButton)
            10 -> lessonNumberRadioGroup.check(R.id.tenLessonButton)
            15 -> lessonNumberRadioGroup.check(R.id.fifteenLessonButton)
            else -> lessonNumberRadioGroup.check(R.id.tenLessonButton)
        }

        "Daily notifications are currently sent at ${formatTime(sharedPref.getInt("kanasrsNotificationHour", 12), sharedPref.getInt("kanasrsNotificationMinute", 0))}".also { sentAtTextView.text = it }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val sdf = SimpleDateFormat("KK:mm")
        val sdfs = SimpleDateFormat("hh:mm a")
        val date = sdf.parse("$hour:$minute")
        return sdfs.format(date)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showInfoDialog(stringResource: Int, title: String) {
        val view = layoutInflater.inflate(R.layout.extra_info_dialog, null)
        val dialog = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val infoText = view.findViewById<TextView>(R.id.infoTextView)

        infoText.text = HtmlCompat.fromHtml(getString(stringResource), HtmlCompat.FROM_HTML_MODE_LEGACY)
        view.findViewById<TextView>(R.id.titleTextView).text = title
        view.findViewById<LottieAnimationView>(R.id.closeAnimation).setOnClickListener {
            dialog.dismiss()
        }

        infoText.movementMethod = ScrollingMovementMethod()
        infoText.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP ->
                    v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        dialog.show()
    }

}