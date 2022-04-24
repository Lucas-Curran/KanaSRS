package com.example.jwriter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.get
import com.google.android.material.button.MaterialButton
import kotlin.concurrent.thread

class LessonActivity : AppCompatActivity() {

    private lateinit var rootViewAnimator: ViewAnimator
    var animationTime = 0L
    private lateinit var kanaList: List<Kana>
    private lateinit var loadingBar: ProgressBar

    private val FIRST_KANA = 0
    private val KANA_LETTER_SCREEN = 0
    private val KANA_GIF_SCREEN = 1

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        rootViewAnimator = findViewById(R.id.rootViewAnimator)
        loadingBar = findViewById(R.id.lessonProgressBar)

        val animationIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        val animationOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        val prevAnimIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        val prevAnimOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)

        animationTime = animationIn.duration

        rootViewAnimator.inAnimation = animationIn
        rootViewAnimator.outAnimation = animationOut

        kanaList = JWriterDatabase.getInstance(this).kanaDao().getUnlearnedKana()
        val kanaConverter = KanaConverter(false)

        val subList = kanaList.subList(0, 5)

        Handler(Looper.getMainLooper()).post {

            for ((index, kana) in subList.withIndex()) {
                val newView = layoutInflater.inflate(R.layout.lesson_item, null)

                newView.findViewById<TextView>(R.id.kanaTextView).text = kana.letter
                newView.findViewById<TextView>(R.id.englishTextView).text =
                    kanaConverter._hiraganaToRomaji(kana.letter)

                val tempViewAnimator = newView.findViewById<ViewAnimator>(R.id.viewAnimator)
                val nextButton = newView.findViewById<ImageButton>(R.id.nextItemButton)
                val previousButton = newView.findViewById<ImageButton>(R.id.previousItemButton)

                tempViewAnimator.inAnimation = animationIn
                tempViewAnimator.outAnimation = animationOut

                val tempKanaWebStroke = newView.findViewById<WebView>(R.id.strokeWebView)
                tempKanaWebStroke.settings.javaScriptEnabled = true
                tempKanaWebStroke.webViewClient = WebViewClient()
                tempKanaWebStroke.loadUrl(kana.gif)

                nextButton.setOnSingleClickListener {

                    //If displayed screen is not the kana web view, show the kana web view, else continue and flip to the next letter
                    if (tempViewAnimator.displayedChild != KANA_GIF_SCREEN) {
                        tempViewAnimator.inAnimation = animationIn
                        tempViewAnimator.outAnimation = animationOut
                        tempViewAnimator.showNext()
                        return@setOnSingleClickListener
                    }

                    rootViewAnimator.inAnimation = animationIn
                    rootViewAnimator.outAnimation = animationOut
                    if (rootViewAnimator.displayedChild == subList.lastIndex) {
                        val view = layoutInflater.inflate(R.layout.lesson_completed_dialog, null)
                        val builder = AlertDialog.Builder(this).setView(view).create()
                        view.findViewById<MaterialButton>(R.id.restartLessonButton).setOnClickListener {
                            rootViewAnimator.displayedChild = FIRST_KANA
                            builder.dismiss()
                        }
                        view.findViewById<MaterialButton>(R.id.beginQuizButton).setOnClickListener {
                            startActivity(Intent(this, ReviewActivity::class.java))
                        }
                        builder.show()
                    } else {
                        rootViewAnimator.showNext()
                        rootViewAnimator.postOnAnimationDelayed({
                            tempViewAnimator.inAnimation = null
                            tempViewAnimator.outAnimation = null
                            tempViewAnimator.displayedChild = KANA_LETTER_SCREEN
                        }, rootViewAnimator.inAnimation.duration)
                    }
                }

                previousButton.setOnSingleClickListener {
                    if (rootViewAnimator.displayedChild == FIRST_KANA && tempViewAnimator.displayedChild == KANA_LETTER_SCREEN) {
                        return@setOnSingleClickListener
                    }

                    if (tempViewAnimator.displayedChild == KANA_GIF_SCREEN) {
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
                            tempViewAnimator.displayedChild = KANA_LETTER_SCREEN
                        }, rootViewAnimator.outAnimation.duration)
                    }
                }

                rootViewAnimator.addView(newView)
                if (index == subList.lastIndex) {
                    rootViewAnimator.removeView(loadingBar)
                }
            }
        }
    }

}

class OnSingleClickListener(private val block: () -> Unit) : View.OnClickListener {

    private var lastClickTime = 0L

    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < 500) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()
        block()
    }
}

fun View.setOnSingleClickListener(block: () -> Unit) {
    setOnClickListener(OnSingleClickListener(block))
}