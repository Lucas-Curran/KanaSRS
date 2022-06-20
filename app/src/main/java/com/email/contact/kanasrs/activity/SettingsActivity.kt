package com.email.contact.kanasrs.activity

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.util.Utilities
import java.text.SimpleDateFormat


/*
Could possibly include:
    - Light mode/dark mode
    - Blocking certain letters
    - Having timer visible/not in timed mode
    - Switching between katakana/hiragana
    - Creating custom study sets
 */

class SettingsActivity : AppCompatActivity() {

    private lateinit var resetAccount: TextView
    private lateinit var setTimeTextView: TextView
    private lateinit var sentAtTextView: TextView
    private lateinit var lessonNumberRadioGroup: RadioGroup

    private val timePickerDialogListener: TimePickerDialog.OnTimeSetListener =
        TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            val sharedPref = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE) ?: return@OnTimeSetListener
            with (sharedPref.edit()) {
                putInt("jwriterNotificationHour", hourOfDay)
                putInt("jwriterNotificationMinute", minute)
                apply()
            }
            Utilities.setAlarm(this)

            "Daily notifications are currently sent at ${formatTime(sharedPref.getInt("jwriterNotificationHour", 12), sharedPref.getInt("jwriterNotificationMinute", 0))}".also { sentAtTextView.text = it }
            Toast.makeText(this, "Notifications will now be sent at ${formatTime(hourOfDay, minute)}", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        setTimeTextView = findViewById(R.id.setTimeTextView)
        sentAtTextView = findViewById(R.id.sentAtTextView)
        lessonNumberRadioGroup = findViewById(R.id.lessonNumberRadioGroup)

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
                val toast = Toast(this)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.setText(resources.getText(R.string.clear_account))
                toast.show()
                startActivity(Intent(this, MenuActivity::class.java))
            }
            dialog.show()
        }

        val sharedPref = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)

        setTimeTextView.setOnClickListener {
            val timePicker = TimePickerDialog(
                this,
                timePickerDialogListener,
                sharedPref.getInt("jwriterNotificationHour", 12),
                sharedPref.getInt("jwriterNotificationMinute", 0),
                false
            )
            timePicker.setOnCancelListener { Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show() }
            timePicker.show()
        }

        lessonNumberRadioGroup.setOnCheckedChangeListener { _, id ->

            val numLesson = when(id) {
                R.id.fiveLessonButton -> 5
                R.id.tenLessonButton -> 10
                R.id.fifteenLessonButton -> 15
                else -> 10
            }
            with (sharedPref.edit()) {
                putInt("jwriterLessonNumber", numLesson)
                apply()
            }
        }

        when(sharedPref.getInt("jwriterLessonNumber", 10)) {
            5 -> lessonNumberRadioGroup.check(R.id.fiveLessonButton)
            10 -> lessonNumberRadioGroup.check(R.id.tenLessonButton)
            15 -> lessonNumberRadioGroup.check(R.id.fifteenLessonButton)
            else -> lessonNumberRadioGroup.check(R.id.tenLessonButton)
        }

        "Daily notifications are currently sent at ${formatTime(sharedPref.getInt("jwriterNotificationHour", 12), sharedPref.getInt("jwriterNotificationMinute", 0))}".also { sentAtTextView.text = it }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val sdf = SimpleDateFormat("KK:mm")
        val sdfs = SimpleDateFormat("hh:mm a")
        val date = sdf.parse("$hour:$minute")
        return sdfs.format(date)
    }

}