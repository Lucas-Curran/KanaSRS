package com.email.contact.kanasrs.activity

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.email.contact.kanasrs.custom.KanaInfoView
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.util.Utilities
import com.email.contact.kanasrs.util.Utilities.Companion.ROOKIE1
import com.email.contact.kanasrs.util.Utilities.Companion.ROOKIE2
import com.email.contact.kanasrs.util.Utilities.Companion.getLevelColor
import com.google.android.material.tabs.TabLayout
import com.skydoves.progressview.ProgressLabelConstraints
import com.skydoves.progressview.ProgressView
import com.skydoves.progressview.progressView
import kotlin.math.cos
import kotlin.math.sin


/*
Stats: each letter's individual accuracy, worst letter, best time (for time mode)
Eventually include graphs, possibly over an interval of time
*/


class StatsActivity : AppCompatActivity() {

    private lateinit var progressLinearLayout: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var hiraganaProgressViews: ArrayList<ProgressView>
    private lateinit var katakanaProgressViews: ArrayList<ProgressView>
    private lateinit var progressScrollView: ScrollView
    private lateinit var overallView: View

    private var mostRecentReview = Long.MAX_VALUE

    private var kanaLearned = 0
    private var currentHiragana = 0
    private var currentKatakana = 0

    private var numItemsToReview = 0

    private var levelsItemArray = IntArray(5)

    private val ROOKIE = 0
    private val AMATEUR = 1
    private val EXPERT = 2
    private val MASTER = 3
    private val SENSEI = 4

    private var fading = false

    object TabConstants {
        const val OVERALL = 0
        const val HIRAGANA = 1
        const val KATAKANA = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        hiraganaProgressViews = ArrayList()
        katakanaProgressViews = ArrayList()

        overallView = layoutInflater.inflate(R.layout.stats_overall_tab, null)

        progressLinearLayout = findViewById(R.id.progressLinearLayout)
        tabLayout = findViewById(R.id.statsTabLayout)
        progressScrollView = findViewById(R.id.progressScrollView)

        for (kana in KanaSRSDatabase.getInstance(this).kanaDao().getKana()) {
            if (kana.hasLearned) {
                when (kana.level) {
                    ROOKIE1, ROOKIE2 -> levelsItemArray[ROOKIE] += 1
                    Utilities.AMATEUR -> levelsItemArray[AMATEUR] += 1
                    Utilities.EXPERT -> levelsItemArray[EXPERT] += 1
                    Utilities.MASTER -> levelsItemArray[MASTER] += 1
                    Utilities.SENSEI -> levelsItemArray[SENSEI] += 1
                }
                kanaLearned++
            }
        }

        val hiraganaFraction = overallView.findViewById<TextView>(R.id.hiraganaFraction)
        val katakanaFraction = overallView.findViewById<TextView>(R.id.katakanaFraction)
        val hiraganaLayout = overallView.findViewById<FrameLayout>(R.id.hiraganaFractionLayout)
        val katakanaLayout = overallView.findViewById<FrameLayout>(R.id.katakanaFractionLayout)

        val hiraBounce = AnimationUtils.loadAnimation(this, R.anim.bounce)
        val kataBounce = AnimationUtils.loadAnimation(this, R.anim.bounce)

        val learnedHiragana = KanaSRSDatabase.getInstance(this).kanaDao().getLearnedHiragana()
        val learnedKatakana = KanaSRSDatabase.getInstance(this).kanaDao().getLearnedKatakana()

        hiraganaFraction.text = learnedHiragana.size.toString()
        katakanaFraction.text = learnedKatakana.size.toString()

        val currentHiraganaText = overallView.findViewById<TextView>(R.id.currentHiraganaTextView)
        val currentKatakanaText = overallView.findViewById<TextView>(R.id.currentKatakanaTextView)

        if (learnedHiragana.isEmpty()) {
            currentHiraganaText.visibility = View.GONE
        } else {
            currentHiraganaText.setTextColor(ContextCompat.getColor(this, getLevelColor(learnedHiragana[currentHiragana].level!!)))
            currentHiraganaText.text = learnedHiragana[currentHiragana].letter
        }

        if (learnedKatakana.isEmpty()) {
            currentKatakanaText.visibility = View.GONE
        } else {
            currentKatakanaText.setTextColor(ContextCompat.getColor(this, getLevelColor(learnedKatakana[currentKatakana].level!!)))
            currentKatakanaText.text = learnedKatakana[currentHiragana].letter
        }

        hiraganaLayout.setOnLongClickListener {
            if (learnedHiragana.isNotEmpty()) {
                KanaInfoView(this, learnedHiragana[currentHiragana], true).show()
            }
            false
        }
        katakanaLayout.setOnLongClickListener {
            if (learnedKatakana.isNotEmpty()) {
                KanaInfoView(this, learnedKatakana[currentKatakana], true).show()
            }
            false
        }

        hiraganaLayout.setOnClickListener {
            hiraganaLayout.startAnimation(hiraBounce)
            currentHiragana++
            if (currentHiragana == learnedHiragana.size) {
                currentHiragana = 0
            }
            if (learnedHiragana.isNotEmpty()) {
                currentHiraganaText.setTextColor(ContextCompat.getColor(this, getLevelColor(learnedHiragana[currentHiragana].level!!)))
                currentHiraganaText.text = learnedHiragana[currentHiragana].letter
            }
        }
        katakanaLayout.setOnClickListener {
            katakanaLayout.startAnimation(kataBounce)
            currentKatakana++
            if (currentKatakana == learnedKatakana.size) {
                currentKatakana = 0
            }
            if (learnedKatakana.isNotEmpty()) {
                currentKatakanaText.setTextColor(ContextCompat.getColor(this, getLevelColor(learnedKatakana[currentKatakana].level!!)))
                currentKatakanaText.text = learnedKatakana[currentKatakana].letter
            }
        }

        val smileAnimation = overallView.findViewById<LottieAnimationView>(R.id.smileAnimation)
        smileAnimation.addAnimatorListener(object : Animator.AnimatorListener{
            override fun onAnimationStart(p0: Animator?) {
            }

            override fun onAnimationEnd(p0: Animator?) {
                smileAnimation.animate().alpha(0f).setDuration(1000L).withEndAction {
                    fading = false
                }.start()
                fading = true
            }

            override fun onAnimationCancel(p0: Animator?) {
            }

            override fun onAnimationRepeat(p0: Animator?) {
            }

        })

        overallView.findViewById<LottieAnimationView>(R.id.statsAnimation1).setOnClickListener {
            if (!smileAnimation.isAnimating && !fading) {
                smileAnimation.alpha = 1f
                smileAnimation.playAnimation()
            }
        }

        animateInPlace(hiraganaLayout)
        animateInPlace(katakanaLayout)


        val progressView = overallView.findViewById<ProgressView>(R.id.kanaLearnedBar)

        val totalMastered = kanaLearned
        progressView.progress = totalMastered.toFloat()

        progressView.setOnClickListener {
            Toast.makeText(this, "$totalMastered kana learned", Toast.LENGTH_SHORT).show()
        }
        progressView.setOnProgressClickListener {
            Toast.makeText(this, "$totalMastered kana learned", Toast.LENGTH_SHORT).show()
        }

        if (totalMastered == 92) {
            progressView.labelText = "All Learned!"
            progressView.labelConstraints = ProgressLabelConstraints.ALIGN_CONTAINER
            progressView.labelGravity = Gravity.CENTER
        } else {
            progressView.labelText = "$kanaLearned kana learned"
        }

        for (kana in KanaSRSDatabase.getInstance(this).kanaDao().getKana()) {
            //Check if there is a review time, and if so, check if the current time has passed the stored review time
            // Review time is calculated during review answers and initially added when learned in lessons
            if (kana.reviewTime != null) {
                if (kana.reviewTime!! < System.currentTimeMillis()) {
                    numItemsToReview++
                } else {
                    val millisecondsUntilReview = kana.reviewTime!! - System.currentTimeMillis()
                    if (millisecondsUntilReview < mostRecentReview) {
                        mostRecentReview = millisecondsUntilReview
                    }
                }
            }
        }


        val accuracyProgressBar = overallView.findViewById<ProgressView>(R.id.totalAccuracyProgress)
        accuracyProgressBar.max = KanaSRSDatabase.getInstance(this).kanaDao().sumAnswered().toFloat()
        accuracyProgressBar.progress = KanaSRSDatabase.getInstance(this).kanaDao().sumCorrect().toFloat()
        overallView.findViewById<TextView>(R.id.totalCorrectText).text = KanaSRSDatabase.getInstance(this).kanaDao().sumCorrect().toString()
        overallView.findViewById<TextView>(R.id.totalText).text = KanaSRSDatabase.getInstance(this).kanaDao().sumAnswered().toString()

        loadUserStats()
        overallTab()
    }

    /**
     * Loads the user's stats
     */
    private fun loadUserStats() {

        loadBars(KanaSRSDatabase.getInstance(this).kanaDao().getHiragana())
        loadBars(KanaSRSDatabase.getInstance(this).kanaDao().getKatakana())

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    when (tab.position) {
                        TabConstants.OVERALL -> overallTab()
                        TabConstants.HIRAGANA -> hiraganaTab()
                        TabConstants.KATAKANA -> katakanaTab()
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Handle tab reselect
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Handle tab unselect
            }
        })
    }

    /**
     * General statistics tab
     */
    private fun overallTab() {
        progressLinearLayout.removeAllViewsInLayout()
        progressLinearLayout.addView(overallView)
        val constraintLayout = findViewById<ConstraintLayout>(R.id.rootConstraintLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(
            progressScrollView.id,
            ConstraintSet.TOP,
            constraintLayout.id,
            ConstraintSet.TOP,
            0
        )
        constraintSet.applyTo(constraintLayout)
    }

    /**
     * Hiragana progress tab
     */
    private fun hiraganaTab() {
        //Remove current layout, loop through hiragana progress bars and add them to the scroll view
        progressLinearLayout.removeAllViewsInLayout()
        for (view in hiraganaProgressViews) {
            progressLinearLayout.addView(view)
        }
        progressScrollView.fullScroll(ScrollView.FOCUS_UP)

        val constraintLayout = findViewById<ConstraintLayout>(R.id.rootConstraintLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(
            progressScrollView.id,
            ConstraintSet.TOP,
            tabLayout.id,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.applyTo(constraintLayout)
    }

    /**
     * Katakana progress tab
     */
    private fun katakanaTab() {
        //Remove current layout, loop through katakana progress bars and add them to the scroll view
        progressLinearLayout.removeAllViewsInLayout()
        for (view in katakanaProgressViews) {
            progressLinearLayout.addView(view)
        }
        progressScrollView.fullScroll(ScrollView.FOCUS_UP)
        val constraintLayout = findViewById<ConstraintLayout>(R.id.rootConstraintLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(
            progressScrollView.id,
            ConstraintSet.TOP,
            tabLayout.id,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.applyTo(constraintLayout)
    }

    /**
     * Adds the data for each kana into progress view array list
     * @param barList string array of labels attached to each bar
     * @param hiragana whether the bar is hiragana labels
     */
    private fun loadBars(barList: List<Kana>) {
        for (kana in barList) {

            var color = 0
            var progress = 0f

            if (kana.level != null) {
                when (kana.level) {
                    1 -> {
                        color = ContextCompat.getColor(this, R.color.rookie_pink)
                        progress = 1f
                    }
                    2 -> {
                        color = ContextCompat.getColor(this, R.color.rookie_pink)
                        progress = 2f
                    }
                    3 -> {
                        color = ContextCompat.getColor(this, R.color.amateur_purple)
                        progress = 3f
                    }
                    4 -> {
                        color = ContextCompat.getColor(this, R.color.expert_blue)
                        progress = 4f
                    }
                    5 -> {
                        color = ContextCompat.getColor(this, R.color.master_blue)
                        progress = 5f
                    }
                    6 -> {
                        color = ContextCompat.getColor(this, R.color.sensei_gold)
                        progress = 6f
                    }
                }
            }

            val myProgressView = progressView(this) {

                setProgress(progress)
                setMin(0f)
                setMax(6f)
                setRadius(12f)
                setDuration(1200L)
                setAutoAnimate(true)
                setLabelColorInner(ContextCompat.getColor(applicationContext, R.color.white))
                setLabelColorOuter(ContextCompat.getColor(applicationContext, R.color.black))
                setLabelText(kana.letter!!)
                setProgressbarColor(color)
                setLabelSize(13f)
                setLabelSpace(10f)
                setLabelTypeface(Typeface.BOLD)
                setColorBackground(
                    ContextCompat.getColor(
                        applicationContext,
                        androidx.cardview.R.color.cardview_shadow_start_color
                    )
                )
            }

            myProgressView.setOnProgressClickListener {
                if (kana.hasLearned) {
                    KanaInfoView(this, kana, true).show()
                } else {
                    Toast.makeText(
                        this,
                        "${kana.letter} has not been learned yet, info is unavailable",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            myProgressView.setOnClickListener {
                if (kana.hasLearned) {
                    KanaInfoView(this, kana, true).show()
                } else {
                    Toast.makeText(
                        this,
                        "${kana.letter} has not been learned yet, info is unavailable",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(10, 15, 10, 15)
            myProgressView.layoutParams = params
            if (kana.isHiragana) hiraganaProgressViews.add(myProgressView) else katakanaProgressViews.add(
                myProgressView
            )
        }
    }

    private fun animateWave(view: View, startDelay: Long) {
        val downAnimation = ObjectAnimator.ofFloat(view, "translationY", 30f)
        val upAnimation = ObjectAnimator.ofFloat(view, "translationY", -30f)
        val animationSet = AnimatorSet()
        animationSet.duration = 1500
        animationSet.startDelay = startDelay
        animationSet.playSequentially(downAnimation, upAnimation)
        animationSet.childAnimations.forEach {
            val animation = it as ObjectAnimator
            animation.repeatCount = Animation.INFINITE
            animation.repeatMode = ValueAnimator.REVERSE
        }
        animationSet.start()
    }

    private fun animateInPlace(view: View) {

        val distance = 50

        val direction = Math.random() * 2 * Math.PI
        val translationX = (cos(direction) * distance).toFloat()
        val translationY = (sin(direction) * distance).toFloat()

        val verticalAnimation = ObjectAnimator.ofFloat(view, "translationY", translationY)
        val horizontalAnimation = ObjectAnimator.ofFloat(view, "translationX", translationX)

        val animationSet = AnimatorSet()
        animationSet.duration = 5000
        animationSet.playTogether(verticalAnimation, horizontalAnimation)

        animationSet.doOnEnd {
            animateInPlace(view)
        }

        animationSet.start()
    }
}