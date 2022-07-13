package com.email.contact.kanasrs.activity

import android.animation.Animator
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.custom.DrawingView
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.util.KanaConverter
import com.email.contact.kanasrs.util.Utilities.Companion.disable
import com.email.contact.kanasrs.util.Utilities.Companion.enable
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class WritingActivity : AppCompatActivity() {

    private lateinit var kanaList: MutableList<Kana>
    private lateinit var drawingView: DrawingView
    private lateinit var letterToDraw: TextView
    private lateinit var wrongImageOne: LottieAnimationView
    private lateinit var wrongImageTwo: LottieAnimationView
    private lateinit var wrongImageThree: LottieAnimationView
    private lateinit var correctAnimation: LottieAnimationView
    private lateinit var wrongImages: List<LottieAnimationView>
    private lateinit var submitWriting: Button
    private lateinit var loadResultBar: ProgressBar
    private val imagesAnimatedList = mutableListOf(false, false, false)
    private lateinit var kanaConverter: KanaConverter
    private var wrongCounter = 0

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing)

        val parcelableList = intent.getParcelableArrayListExtra<Kana>("kanaWriting")
        println(parcelableList)
        kanaList = (parcelableList?.toList() as List<Kana>).shuffled().toMutableList()
        intent.removeExtra("kanaWriting")

        letterToDraw = findViewById(R.id.letterToDraw)
        kanaConverter = KanaConverter(false)
        correctAnimation = findViewById(R.id.correctAnimation)
        loadResultBar = findViewById(R.id.loadingResultBar)

        val relativeLayout = findViewById<RelativeLayout>(R.id.writingRelativeLayout)
        drawingView = DrawingView(this)
        //drawingView.setPaintColor(Color.WHITE)

        val clearButton = findViewById<Button>(R.id.clearButton)

        clearButton.setOnClickListener {
            drawingView.clearDrawing()
        }

        wrongImageOne = findViewById(R.id.firstWrong)
        wrongImageTwo = findViewById(R.id.secondWrong)
        wrongImageThree = findViewById(R.id.thirdWrong)
        wrongImages = listOf(wrongImageOne, wrongImageTwo, wrongImageThree)
        wrongImages.forEach { lottieAnimationView ->
            lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                }

                override fun onAnimationEnd(p0: Animator?) {
                    // If animation is in reverse, i.e. disappearing, then switch it back
                    if (lottieAnimationView.speed < 0) {
                        lottieAnimationView.reverseAnimationSpeed()
                    }
                    if (imagesAnimatedList[2]) {
                        nextKana()
                    }
                }

                override fun onAnimationCancel(p0: Animator?) {
                }

                override fun onAnimationRepeat(p0: Animator?) {
                }
            })
        }

        correctAnimation.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {
            }

            override fun onAnimationEnd(p0: Animator?) {
                correctAnimation.reverseAnimationSpeed()
                if (correctAnimation.speed < 0) {
                    correctAnimation.playAnimation()
                }
            }

            override fun onAnimationCancel(p0: Animator?) {
            }

            override fun onAnimationRepeat(p0: Animator?) {
            }

        })

        submitWriting = findViewById<MaterialButton>(R.id.submitWritingButton)
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)

        submitWriting.setOnClickListener {

            if (!isInternetAvailable(this)) {
                Toast.makeText(this, "Error: please check your internet connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (drawingView.checkIfEmpty()) {
                Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show()
            } else {
                loadResultBar.visibility = View.VISIBLE
                submitWriting.disable()
                GlobalScope.launch {
                    if (drawingView.isDrawingCorrect(kanaList[0].letter!!, loadResultBar)) {
                        runOnUiThread {
                            correctAnimation.playAnimation()
                            calculateNextReviewTime(kanaList[0], correct = true)
                            nextKana()
                        }
                    } else {
                        wrongCounter++
                        drawingView.clearDrawing()
                        runOnUiThread {
                            submitWriting.enable()
                            submitWriting.startAnimation(shakeAnimation)
                            when (wrongCounter) {
                                1 -> {
                                    wrongImageOne.playAnimation()
                                    imagesAnimatedList[0] = true
                                }
                                2 -> {
                                    wrongImageTwo.playAnimation()
                                    imagesAnimatedList[1] = true
                                }
                                3 -> {
                                    calculateNextReviewTime(kanaList[0], correct = false)
                                    wrongImageThree.playAnimation()
                                    imagesAnimatedList[2] = true
                                }
                            }
                        }
                    }
                }
            }
        }

        relativeLayout.addView(drawingView)

        letterToDraw.text = kanaConverter.hiraganaToRomaji(kanaList[0].letter!!)

    }

    private fun calculateNextReviewTime(kana: Kana, correct: Boolean) {
        if (correct) {
            if (kana.writingLevel!! < 6) {
                kana.writingLevel = kana.writingLevel?.plus(1)
            }
        } else {
            if (kana.writingLevel!! > 1) {
                kana.writingLevel = kana.writingLevel?.minus(1)
            }
        }
        val now = System.currentTimeMillis()
        val nextPracticeDate = now + levelToTime(kana.writingLevel!!)
        if (kana.writingLevel == 6) {
            kana.writingReviewTime = null
        } else {
            kana.writingReviewTime = nextPracticeDate
        }
        KanaSRSDatabase.getInstance(this).kanaDao().updateKana(kana)
    }

    private fun levelToTime(level: Int): Long {
        //For debugging
        val oneMinute = 1000 * 60L
        val millisecondsInHours = 1000L * 60 * 60
        val millisecondsInDays = millisecondsInHours * 24
//        return when(level) {
//            1 -> (millisecondsInHours * 8) // Level 1 is 8 hours after review
//            2 -> (millisecondsInDays * 1) // Level 2 is 1 day after review
//            3 -> (millisecondsInDays * 3) // Level 3 is 3 days after review
//            4 -> (millisecondsInDays * 7) // Level 4 is 7 days (1 week) after review
//            5 -> (millisecondsInDays * 14) // Level 5 is 14 day (2 weeks) after review
//            6 -> (millisecondsInDays * 30) // Level 6 is 30 days (1 month) after review
//            else -> 0
//        }
        //For debugging
        return when (level) {
            1 -> oneMinute * 1
            2 -> oneMinute * 2
            3 -> oneMinute * 3
            4 -> oneMinute * 4
            5 -> oneMinute * 5
            6 -> oneMinute * 6
            else -> 0
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }
        return result
    }

    private fun nextKana() {
        wrongCounter = 0
        kanaList.removeAt(0)
        drawingView.clearDrawing()

        //Reverse each animation, play it, making them all disappear, only if they've already been animated
        wrongImages.forEachIndexed { index, lottieAnimationView ->
            if (imagesAnimatedList[index]) {
                imagesAnimatedList[index] = false
                lottieAnimationView.reverseAnimationSpeed()
                lottieAnimationView.playAnimation()
            }
        }

        if (kanaList.isEmpty()) {
            //TODO: End review session
            return
        }

        letterToDraw.animate().alpha(0f).withEndAction {
            letterToDraw.text = kanaConverter.hiraganaToRomaji(kanaList[0].letter!!)
            letterToDraw.animate().alpha(1f).setStartDelay(750).withEndAction {
                submitWriting.enable()
            }.duration = 500
        }.duration = 500

    }
}