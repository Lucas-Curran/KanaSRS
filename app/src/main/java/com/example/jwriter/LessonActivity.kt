package com.example.jwriter

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

class LessonActivity : AppCompatActivity() {

    private lateinit var kanaStrokeWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        kanaStrokeWebView = findViewById(R.id.strokeWebView)

        kanaStrokeWebView.settings.javaScriptEnabled = true
        kanaStrokeWebView.webViewClient = WebViewClient()
        kanaStrokeWebView.loadUrl("https://upload.wikimedia.org/wikipedia/commons/d/d8/Hiragana_%E3%81%82_stroke_order_animation.gif")
    }
}