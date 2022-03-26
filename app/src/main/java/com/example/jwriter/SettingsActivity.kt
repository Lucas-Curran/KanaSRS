package com.example.jwriter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/*
Could possibly include:
    - Light mode/dark mode
    - Blocking certain letters
    - Having timer visible/not in timed mode
    - Switching between katakana/hiragana
    - Creating custom study sets
 */

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }
}