package com.email.contact.kanasrs.activity

import android.animation.Animator
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.custom.DrawingView
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.util.KanaConverter
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
    private lateinit var wrongImages: List<LottieAnimationView>
    private val imagesAnimatedList = mutableListOf(false, false, false)
    private lateinit var kanaConverter: KanaConverter
    private var wrongCounter = 0

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing)

        letterToDraw = findViewById(R.id.letterToDraw)
        kanaConverter = KanaConverter(false)

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
        wrongImages.forEachIndexed { index, lottieAnimationView ->
            lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator?) {
                }

                override fun onAnimationEnd(p0: Animator?) {
                    // If animation is in reverse, i.e. disappearing, then switch it back
                    if (lottieAnimationView.speed < 0) {
                        lottieAnimationView.reverseAnimationSpeed()
                    }
                }

                override fun onAnimationCancel(p0: Animator?) {
                }

                override fun onAnimationRepeat(p0: Animator?) {
                }
            })
        }

        val submitWriting = findViewById<MaterialButton>(R.id.submitWritingButton)
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)

        submitWriting.setOnClickListener {
            if (drawingView.checkIfEmpty()) {
                Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show()
            } else {
                GlobalScope.launch {
                    if (drawingView.isDrawingCorrect(testList[0])) {
                        nextKana()
                    } else {
                        wrongCounter++
                        drawingView.clearDrawing()
                        runOnUiThread {
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
                                    nextKana()
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

    private fun nextKana() {
        wrongCounter = 0
        testList.removeAt(0)
        drawingView.clearDrawing()
        runOnUiThread {
            letterToDraw.text = kanaConverter.hiraganaToRomaji(testList[0])
            //Reverse each animation, play it, and reverse it back, making them all disappear
            wrongImages.forEachIndexed { index, lottieAnimationView ->
                if (imagesAnimatedList[index]) {
                    lottieAnimationView.reverseAnimationSpeed()
                    lottieAnimationView.playAnimation()
                    imagesAnimatedList[index] = false
                }
            }
        }
    }
}