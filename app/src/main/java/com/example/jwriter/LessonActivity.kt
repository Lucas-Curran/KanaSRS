package com.example.jwriter

import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.get

class LessonActivity : AppCompatActivity() {

    private lateinit var rootViewAnimator: ViewAnimator

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        rootViewAnimator = findViewById(R.id.rootViewAnimator)

        val animationIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        val animationOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        val prevAnimIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        val prevAnimOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)

        rootViewAnimator.inAnimation = animationIn
        rootViewAnimator.outAnimation = animationOut

        for (i in 1..5) {
            val newView = layoutInflater.inflate(R.layout.lesson_item, null)

            newView.findViewById<TextView>(R.id.kanaTextView).text = i.toString()

            val tempViewAnimator = newView.findViewById<ViewAnimator>(R.id.viewAnimator)
            val nextButton = newView.findViewById<ImageButton>(R.id.nextItemButton)
            val previousButton =  newView.findViewById<ImageButton>(R.id.previousItemButton)

            tempViewAnimator.inAnimation = animationIn
            tempViewAnimator.outAnimation = animationOut

            val tempKanaWebStroke = newView.findViewById<WebView>(R.id.strokeWebView)

            tempKanaWebStroke.settings.javaScriptEnabled = true
            tempKanaWebStroke.webViewClient = WebViewClient()
            tempKanaWebStroke.loadUrl("https://upload.wikimedia.org/wikipedia/commons/d/d8/Hiragana_%E3%81%82_stroke_order_animation.gif")

            nextButton.setOnClickListener {
                //nextButton.isEnabled = false
                //previousButton.isEnabled = false
                if (tempViewAnimator.currentView == tempViewAnimator.getChildAt(tempViewAnimator.childCount-1)) {
                    rootViewAnimator.inAnimation = animationIn
                    rootViewAnimator.outAnimation = animationOut
                    rootViewAnimator.showNext()
                } else {
                    tempViewAnimator.inAnimation = animationIn
                    tempViewAnimator.outAnimation = animationOut
                    tempViewAnimator.showNext()
                }
            }

           previousButton.setOnClickListener {
               //nextButton.isEnabled = false
               //previousButton.isEnabled = false
                if (tempViewAnimator.currentView == tempViewAnimator.getChildAt(tempViewAnimator.childCount)) {
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