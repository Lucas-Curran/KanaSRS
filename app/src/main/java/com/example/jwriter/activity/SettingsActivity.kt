package com.example.jwriter.activity

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.jwriter.R
import com.example.jwriter.database.JWriterDatabase

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        resetAccount = findViewById(R.id.resetAccountTextView)
        resetAccount.setOnClickListener {

            val view = layoutInflater.inflate(R.layout.reset_account_dialog, null)
            val dialog = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()

            view.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            view.findViewById<TextView>(R.id.resetAccountTextView).setOnClickListener {
                this.deleteDatabase("jwriter.db")
                JWriterDatabase.destroyInstance()
                val toast = Toast(this)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.setText(resources.getText(R.string.clear_account))
                toast.show()
                startActivity(Intent(this, MenuActivity::class.java))
            }

            dialog.show()
        }
    }
}