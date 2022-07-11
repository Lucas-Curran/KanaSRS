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
import com.email.contact.kanasrs.util.KanaConverter
import com.email.contact.kanasrs.util.Utilities.Companion.disable
import com.email.contact.kanasrs.util.Utilities.Companion.enable
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class WritingActivity : AppCompatActivity() {

    private val reviewQueue = mutableListOf<Kana>()
    private val testList = mutableListOf("あ", "い", "う", "え", "お")
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
                    if (drawingView.isDrawingCorrect(testList[0], loadResultBar)) {
                        runOnUiThread {
                            correctAnimation.playAnimation()
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

        letterToDraw.text = kanaConverter.hiraganaToRomaji(testList[0])

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
        testList.removeAt(0)
        drawingView.clearDrawing()
        letterToDraw.animate().alpha(0f).withEndAction {
            letterToDraw.text = kanaConverter.hiraganaToRomaji(testList[0])
            letterToDraw.animate().alpha(1f).setStartDelay(750).withEndAction {
                submitWriting.enable()
            }.duration = 500
        }.duration = 500
        //Reverse each animation, play it, making them all disappear, only if they've already been animated
        wrongImages.forEachIndexed { index, lottieAnimationView ->
            if (imagesAnimatedList[index]) {
                imagesAnimatedList[index] = false
                lottieAnimationView.reverseAnimationSpeed()
                lottieAnimationView.playAnimation()
            }
        }
    }
}