package com.email.contact.kanasrs.activity

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.utils.LottieValueAnimator
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.util.Utilities


class FaqActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_faq)

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(
                ContextCompat.getColor(this, R.color.night_gradient_5),
                ContextCompat.getColor(this, R.color.night_gradient_4),
                ContextCompat.getColor(this, R.color.night_gradient_3),
                ContextCompat.getColor(this, R.color.night_gradient_2),
                ContextCompat.getColor(this, R.color.night_gradient_1)
            )
        )

        val faqAnimation = findViewById<LottieAnimationView>(R.id.faqAnimation)
        val catAnimation = LottieAnimationView(this)
        val dogAnimation = LottieAnimationView(this)

        faqAnimation.setOnSingleClickListener {
            if (!faqAnimation.isAnimating) {
                faqAnimation.playAnimation()
            }
        }

        val linearLayout = findViewById<LinearLayout>(R.id.faqLinearLayout)
        linearLayout.background = gradientDrawable

        Handler(Looper.getMainLooper()).post {

            val contents = this.assets.open("faq.txt").bufferedReader().use { it.readText() }
            val questions = contents.split("\n")

            for ((index, question) in questions.withIndex()) {

                val list = question.split("* ")
                val faqItem = layoutInflater.inflate(R.layout.faq_item, null)
                val questionTextView = faqItem.findViewById<TextView>(R.id.questionTextView)
                val relativeLayout = faqItem.findViewById<RelativeLayout>(R.id.faqRelativeLayout)
                val answerTextView = faqItem.findViewById<TextView>(R.id.answerTextView)
                val divider = faqItem.findViewById<View>(R.id.faqDivider)

                var open = false
                var moving = false

//                if (index == 4) {
//                    catAnimation.setAnimation(R.raw.cat)
//                    catAnimation.repeatCount = LottieValueAnimator.INFINITE
//                    val layoutParams = LinearLayout.LayoutParams(120, 120)
//                    layoutParams.setMargins(0, 20, 50, 0)
//                    catAnimation.layoutParams = layoutParams
//                    catAnimation.scaleX = 3f
//                    catAnimation.scaleY = 3f
//                    linearLayout.addView(catAnimation)
//                }
//
//                if (index == 9) {
//                    dogAnimation.setAnimation(R.raw.dog)
//                    dogAnimation.repeatCount = LottieValueAnimator.INFINITE
//                    val layoutParams = LinearLayout.LayoutParams(120, 120)
//                    layoutParams.setMargins(200, 20, 0, 0)
//                    dogAnimation.layoutParams = layoutParams
//                    dogAnimation.scaleX = 3f
//                    dogAnimation.scaleY = 3f
//                    linearLayout.addView(dogAnimation)
//                }

                //First index is kanji number, second index is question, third index is answer

                faqItem.findViewById<TextView>(R.id.numberTextView).text = list[0]
                questionTextView.text = list[1]
                answerTextView.text = list[2]

                faqItem.setOnSingleClickListener {

                    val shiftSpace =
                        answerTextView.measuredHeight + answerTextView.marginBottom + divider.measuredHeight + divider.marginTop + divider.marginBottom

                    if (!moving) {
                        moving = true
                        faqItem.isSelected = !faqItem.isSelected
                        if (!open) {
                            Utilities.slideView(
                                relativeLayout,
                                relativeLayout.measuredHeight,
                                relativeLayout.measuredHeight + shiftSpace
                            ) {
                            }
                            Utilities.slideView(
                                questionTextView,
                                questionTextView.measuredHeight,
                                questionTextView.measuredHeight + shiftSpace
                            ) {
                                open = true
                                moving = false
                            }
                        } else {
                            Utilities.slideView(
                                relativeLayout,
                                relativeLayout.measuredHeight,
                                relativeLayout.measuredHeight - shiftSpace
                            ) {
                            }
                            Utilities.slideView(
                                questionTextView,
                                questionTextView.measuredHeight,
                                questionTextView.measuredHeight - shiftSpace
                            ) {
                                open = false
                                moving = false
                            }
                        }
                    }
                }
                linearLayout.addView(faqItem)
                if (index == questions.lastIndex) {

                    faqAnimation.visibility = View.VISIBLE
                    faqAnimation.playAnimation()
                    catAnimation.playAnimation()
                    dogAnimation.playAnimation()
                    linearLayout.removeView(findViewById<ProgressBar>(R.id.faqProgressBar))

                    val fireAnimation = LottieAnimationView(this)
                    fireAnimation.setAnimation(R.raw.campfire)
                    fireAnimation.repeatCount = LottieValueAnimator.INFINITE
                    val layoutParams = LinearLayout.LayoutParams(240, 240)
                    fireAnimation.layoutParams = layoutParams
                    fireAnimation.scaleX = 2f
                    fireAnimation.scaleY = 2f
                    linearLayout.addView(fireAnimation)
                    fireAnimation.playAnimation()

                    val closeActivityImage = findViewById<ImageView>(R.id.closeActivityImage)

                    closeActivityImage.visibility = View.VISIBLE
                    closeActivityImage.translationX = -300f
                    closeActivityImage.animate().translationXBy(300f).setDuration(1000L).start()
                    closeActivityImage.setOnClickListener {
                        finish()
                    }

                }
            }
        }
    }
}