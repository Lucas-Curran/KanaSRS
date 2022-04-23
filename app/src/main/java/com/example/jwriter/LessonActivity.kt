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
                tempKanaWebStroke.loadUrl("https://upload.wikimedia.org/wikipedia/commons/d/d8/Hiragana_%E3%81%82_stroke_order_animation.gif")

                nextButton.setOnSingleClickListener {

                    if (tempViewAnimator.currentView == tempViewAnimator.getChildAt(tempViewAnimator.childCount - 1)) {
                        println(rootViewAnimator.displayedChild)
                        rootViewAnimator.inAnimation = animationIn
                        rootViewAnimator.outAnimation = animationOut
                        if (rootViewAnimator.displayedChild == subList.lastIndex) {
                            val view = layoutInflater.inflate(R.layout.lesson_completed_dialog, null)
                            val builder = AlertDialog.Builder(this).setView(view).create()
                            view.findViewById<MaterialButton>(R.id.restartLessonButton).setOnClickListener {
                                rootViewAnimator.displayedChild = 0
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
                                tempViewAnimator.displayedChild = tempViewAnimator[0].id
                            }, rootViewAnimator.inAnimation.duration)
                        }
                    } else {
                        tempViewAnimator.inAnimation = animationIn
                        tempViewAnimator.outAnimation = animationOut
                        tempViewAnimator.showNext()
                    }
                }

                previousButton.setOnSingleClickListener {

                    if (rootViewAnimator.displayedChild == 0 && tempViewAnimator.displayedChild == 0) {
                        return@setOnSingleClickListener
                    }

                    if (tempViewAnimator.currentView == tempViewAnimator.getChildAt(tempViewAnimator.childCount - 1)) {
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
                            tempViewAnimator.displayedChild = 0
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