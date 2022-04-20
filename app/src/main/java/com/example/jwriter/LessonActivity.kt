package com.example.jwriter

import android.annotation.SuppressLint
import android.app.usage.ConfigurationStats
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get

class LessonActivity : AppCompatActivity() {

    private lateinit var kanaStrokeWebView: WebView
    private lateinit var nextItemButton: ImageButton
    private lateinit var previousItemButton: ImageButton
    private lateinit var viewAnimator: ViewAnimator
    private lateinit var kanaTextView: TextView
    private lateinit var rootViewAnimator: ViewAnimator

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        rootViewAnimator = findViewById(R.id.rootViewAnimator)
        kanaStrokeWebView = findViewById(R.id.strokeWebView)
        nextItemButton = findViewById(R.id.nextItemButton)
        previousItemButton = findViewById(R.id.previousItemButton)
        viewAnimator = findViewById(R.id.viewAnimator)
        kanaTextView = findViewById(R.id.kanaTextView)
        rootViewAnimator = findViewById(R.id.rootViewAnimator)

        val animationIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        val animationOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        val prevAnimIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        val prevAnimOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)

        viewAnimator.inAnimation = animationIn
        viewAnimator.outAnimation = animationOut
        rootViewAnimator.inAnimation = animationIn
        rootViewAnimator.outAnimation = animationOut

        for (i in  1..5) {
            val newView = layoutInflater.inflate(R.layout.lesson_item, null)

            newView.findViewById<TextView>(R.id.kanaTextView).text = i.toString()

            val tempViewAnimator = newView.findViewById<ViewAnimator>(R.id.viewAnimator)
            tempViewAnimator.inAnimation = animationIn
            tempViewAnimator.outAnimation = animationOut
            val tempKanaWebStroke = newView.findViewById<WebView>(R.id.strokeWebView)

            tempKanaWebStroke.settings.javaScriptEnabled = true
            tempKanaWebStroke.webViewClient = WebViewClient()
            tempKanaWebStroke.loadUrl("https://upload.wikimedia.org/wikipedia/commons/d/d8/Hiragana_%E3%81%82_stroke_order_animation.gif")


            newView.findViewById<ImageButton>(R.id.nextItemButton).setOnClickListener {
                if (tempViewAnimator.currentView == tempKanaWebStroke) {
                    rootViewAnimator.inAnimation = animationIn
                    rootViewAnimator.outAnimation = animationOut
                    rootViewAnimator.showNext()
                } else {
                    tempViewAnimator.inAnimation = animationIn
                    tempViewAnimator.outAnimation = animationOut
                    tempViewAnimator.showNext()
                }
            }

            newView.findViewById<ImageButton>(R.id.previousItemButton).setOnClickListener {
                if (tempViewAnimator.currentView == tempKanaWebStroke) {
                    tempViewAnimator.inAnimation = prevAnimIn
                    tempViewAnimator.outAnimation = prevAnimOut
                    tempViewAnimator.showPrevious()
                } else {
                    rootViewAnimator.inAnimation = prevAnimIn
                    rootViewAnimator.outAnimation = prevAnimOut
                    rootViewAnimator.showPrevious()
                }
            }

            rootViewAnimator.addView(newView)
        }
    }
}