package com.example.jwriter

import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
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
    var animationTime = 0L
    private lateinit var kanaList: List<Kana>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        rootViewAnimator = findViewById(R.id.rootViewAnimator)

        val animationIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        val animationOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        val prevAnimIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        val prevAnimOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)

        animationTime = animationIn.duration

        rootViewAnimator.inAnimation = animationIn
        rootViewAnimator.outAnimation = animationOut

        kanaList = JWriterDatabase.getInstance(this).kanaDao().getUnlearnedKana()
        val kanaConverter = KanaConverter(false)

        for (kana in kanaList.subList(0, 5)) {
            val newView = layoutInflater.inflate(R.layout.lesson_item, null)

            newView.findViewById<TextView>(R.id.kanaTextView).text = kana.letter
            newView.findViewById<TextView>(R.id.englishTextView).text = kanaConverter._hiraganaToRomaji(kana.letter)

            val tempViewAnimator = newView.findViewById<ViewAnimator>(R.id.viewAnimator)
            val nextButton = newView.findViewById<ImageButton>(R.id.nextItemButton)
            val previousButton =  newView.findViewById<ImageButton>(R.id.previousItemButton)

            tempViewAnimator.inAnimation = animationIn
            tempViewAnimator.outAnimation = animationOut

            val tempKanaWebStroke = newView.findViewById<WebView>(R.id.strokeWebView)

            tempKanaWebStroke.settings.javaScriptEnabled = true
            tempKanaWebStroke.webViewClient = WebViewClient()
            tempKanaWebStroke.loadUrl("https://upload.wikimedia.org/wikipedia/commons/d/d8/Hiragana_%E3%81%82_stroke_order_animation.gif")

            nextButton.setOnSingleClickListener {
                if (tempViewAnimator.currentView == tempViewAnimator.getChildAt(tempViewAnimator.childCount-1)) {
                    rootViewAnimator.inAnimation = animationIn
                    rootViewAnimator.outAnimation = animationOut
                    rootViewAnimator.showNext()
                    rootViewAnimator.postOnAnimationDelayed({
                        tempViewAnimator.inAnimation = null
                        tempViewAnimator.outAnimation = null
                        tempViewAnimator.displayedChild = tempViewAnimator[0].id
                    }, rootViewAnimator.inAnimation.duration)
                } else {
                    tempViewAnimator.inAnimation = animationIn
                    tempViewAnimator.outAnimation = animationOut
                    tempViewAnimator.showNext()
                }
            }

           previousButton.setOnSingleClickListener {
                if (tempViewAnimator.currentView == tempViewAnimator.getChildAt(tempViewAnimator.childCount-1)) {
                    tempViewAnimator.inAnimation = prevAnimIn
                    tempViewAnimator.outAnimation = prevAnimOut
                    tempViewAnimator.showPrevious()
                } else {
                    rootViewAnimator.inAnimation = prevAnimIn
                    rootViewAnimator.outAnimation = prevAnimOut
                    rootViewAnimator.showPrevious()
                    rootViewAnimator.postOnAnimationDelayed({
                        tempViewAnimator.inAnimation = null
                        tempViewAnimator.outAnimation = null
                        tempViewAnimator.displayedChild = tempViewAnimator[0].id
                    }, rootViewAnimator.outAnimation.duration)
                }
            }
            rootViewAnimator.addView(newView)
        }
    }

}

class OnSingleClickListener(private val block: () -> Unit) : View.OnClickListener {

    private var lastClickTime = 0L

    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < 600) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()

        block()
    }
}

fun View.setOnSingleClickListener(block: () -> Unit) {
    setOnClickListener(OnSingleClickListener(block))
}