package com.example.jwriter

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ViewAnimator

class LessonActivity : AppCompatActivity() {

    private lateinit var kanaStrokeWebView: WebView
    private lateinit var nextItemButton: ImageButton
    private lateinit var previousItemButton: ImageButton
    private lateinit var viewAnimator: ViewAnimator
    private lateinit var kanaTextView: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        kanaStrokeWebView = findViewById(R.id.strokeWebView)
        nextItemButton = findViewById(R.id.nextItemButton)
        previousItemButton = findViewById(R.id.previousItemButton)
        viewAnimator = findViewById(R.id.viewAnimator)
        kanaTextView = findViewById(R.id.kanaTextView)

        kanaStrokeWebView.settings.javaScriptEnabled = true
        kanaStrokeWebView.webViewClient = WebViewClient()
        kanaStrokeWebView.loadUrl("https://upload.wikimedia.org/wikipedia/commons/d/d8/Hiragana_%E3%81%82_stroke_order_animation.gif")

        val animationIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        val animationOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        val prevAnimIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        val prevAnimOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)

        viewAnimator.inAnimation = animationIn
        viewAnimator.outAnimation = animationOut

        nextItemButton.setOnClickListener {
            if (viewAnimator.currentView == kanaStrokeWebView) {
                startActivity(Intent(this, MenuActivity::class.java))
            } else {
                viewAnimator.inAnimation = animationIn
                viewAnimator.outAnimation = animationOut
                viewAnimator.showNext()
            }
        }
        previousItemButton.setOnClickListener {
            if (viewAnimator.currentView != kanaTextView) {
                viewAnimator.inAnimation = prevAnimIn
                viewAnimator.outAnimation = prevAnimOut
                viewAnimator.showPrevious()
            }
        }

    }
}