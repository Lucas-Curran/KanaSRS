package com.email.contact.kanasrs.activity

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.adapter.ReviewedKanaAdapter
import com.email.contact.kanasrs.custom.DrawingView
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.util.KanaConverter
import com.email.contact.kanasrs.util.Utilities
import com.email.contact.kanasrs.util.Utilities.Companion.disable
import com.email.contact.kanasrs.util.Utilities.Companion.enable
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.random.Random


class WritingActivity : AppCompatActivity() {

    private lateinit var kanaList: MutableList<Kana>
    private lateinit var drawingView: DrawingView
    private lateinit var letterToDraw: TextView
    private lateinit var wrongImageOne: LottieAnimationView
    private lateinit var wrongImageTwo: LottieAnimationView
    private lateinit var wrongImageThree: LottieAnimationView
    private lateinit var correctAnimation: LottieAnimationView
    private lateinit var wrongImages: List<LottieAnimationView>

    private lateinit var finishLayout: ConstraintLayout

    private lateinit var incorrectTextView: TextView
    private lateinit var correctTextView: TextView

    private lateinit var writingProgress: ProgressBar

    private val correctReviewAnswers = arrayListOf<Kana>()
    private val incorrectReviewAnswers = arrayListOf<Kana>()

    private lateinit var submitWriting: Button
    private lateinit var loadResultBar: ProgressBar
    private lateinit var arrowIndicator: ImageView
    private lateinit var newLevelTextView: TextView
    private lateinit var newLevelLayout: LinearLayout
    private val imagesAnimatedList = mutableListOf(false, false, false)
    private lateinit var kanaConverter: KanaConverter
    private var wrongCounter = 0

    private lateinit var writingLayout: RelativeLayout

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
        incorrectTextView = findViewById(R.id.numberWrongTextView)
        correctTextView = findViewById(R.id.numberCorrectTextView)
        writingProgress = findViewById(R.id.reviewProgressBar)
        finishLayout = findViewById(R.id.finishSessionLayout)
        arrowIndicator = findViewById(R.id.arrowIndicator)
        newLevelTextView = findViewById(R.id.newLevelTextView)
        newLevelLayout = findViewById(R.id.newLevelLayout)

        writingProgress.max = kanaList.size * 100

        writingLayout = findViewById<RelativeLayout>(R.id.writingRelativeLayout)
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
                    val kana = kanaList[0]
                    if (drawingView.isDrawingCorrect(kana.letter!!, loadResultBar)) {
                        runOnUiThread {
                            correctAnimation.playAnimation()
                            correctReviewAnswers.add(kana)
                            calculateNextReviewTime(kana, correct = true)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                val animation = ObjectAnimator.ofInt(writingProgress, "progress", writingProgress.progress, writingProgress.progress+100)
                                animation.duration = 1000
                                animation.interpolator = DecelerateInterpolator()
                                animation.start()
                            } else {
                                writingProgress.progress = correctReviewAnswers.size
                            }
                            kanaList.removeAt(0)

                            if (!incorrectReviewAnswers.contains(kana)) {

                                var levelText = ""
                                var color = 0

                                when (kana.level) {
                                    1, 2 -> {
                                        levelText = "Rookie"
                                        color = R.color.rookie_pink
                                    }
                                    3 -> {
                                        levelText = "Amateur"
                                        color = R.color.amateur_purple
                                    }
                                    4 -> {
                                        levelText = "Expert"
                                        color = R.color.expert_blue
                                    }
                                    5 -> {
                                        levelText = "Master"
                                        color = R.color.master_blue
                                    }
                                    6 -> {
                                        levelText = "Sensei"
                                        color = R.color.sensei_gold
                                    }
                                }

                                arrowIndicator.setImageResource(R.drawable.ic_up_arrow)
                                newLevelTextView.text = levelText

                                newLevelLayout.backgroundTintList =
                                    AppCompatResources.getColorStateList(
                                        this@WritingActivity,
                                        color
                                    )
                                newLevelLayout.visibility = View.VISIBLE
                                newLevelLayout.alpha = 1F
                            } else {
                                arrowIndicator.setImageResource(R.drawable.ic_checkmark)
                                newLevelTextView.text = "Corrected!"
                                newLevelLayout.backgroundTintList = AppCompatResources.getColorStateList(this@WritingActivity, android.R.color.darker_gray)
                                newLevelLayout.visibility = View.VISIBLE
                                newLevelLayout.alpha = 1F
                            }

                            nextKana()
                        }
                    } else {
                        wrongCounter++
                        runOnUiThread {
                            submitWriting.enable()
                            submitWriting.startAnimation(shakeAnimation)
                            when (wrongCounter) {
                                1 -> {
                                    wrongImageOne.playAnimation()
                                    imagesAnimatedList[0] = true
                                    drawingView.clearDrawing()
                                }
                                2 -> {
                                    wrongImageTwo.playAnimation()
                                    imagesAnimatedList[1] = true
                                    drawingView.clearDrawing()
                                }
                                3 -> {
                                    if (!incorrectReviewAnswers.contains(kana)) {
                                        incorrectReviewAnswers.add(kana)
                                        calculateNextReviewTime(kana, correct = false)
                                        val levelText: String = when (kana.level) {
                                            1, 2 -> "Rookie"
                                            3 -> "Amateur"
                                            4 -> "Expert"
                                            5 -> "Master"
                                            6 -> "Sensei"
                                            else -> "Error"
                                        }

                                        arrowIndicator.setImageResource(R.drawable.ic_down_arrow)
                                        newLevelTextView.text = levelText

                                        newLevelLayout.backgroundTintList = AppCompatResources.getColorStateList(this@WritingActivity, R.color.wrong_answer)
                                        newLevelLayout.visibility = View.VISIBLE
                                        newLevelLayout.alpha = 1F
                                    }
                                    if (kanaList.size > 1) {
                                        val newKanaPosition = Random.nextInt(1, kanaList.size)
                                        kanaList.remove(kana)
                                        kanaList.add(newKanaPosition, kana)
                                    }
                                    wrongImageThree.playAnimation()
                                    imagesAnimatedList[2] = true
                                }
                            }
                        }
                    }
                }
            }
        }

        writingLayout.addView(drawingView)

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
        drawingView.clearDrawing()

        incorrectTextView.text = incorrectReviewAnswers.size.toString()
        correctTextView.text = correctReviewAnswers.size.toString()

        //Reverse each animation, play it, making them all disappear, only if they've already been animated
        wrongImages.forEachIndexed { index, lottieAnimationView ->
            if (imagesAnimatedList[index]) {
                imagesAnimatedList[index] = false
                lottieAnimationView.reverseAnimationSpeed()
                lottieAnimationView.playAnimation()
            }
        }

        if (kanaList.isEmpty()) {
            endSession()
            return
        }

        letterToDraw.animate().alpha(0f).withEndAction {
            letterToDraw.text = kanaConverter.hiraganaToRomaji(kanaList[0].letter!!)
            newLevelLayout.animate().alpha(0F).setStartDelay(750).duration = 500
            letterToDraw.animate().alpha(1f).setStartDelay(750).withEndAction {
                submitWriting.enable()
            }.duration = 500
        }.duration = 500

    }

    private fun endSession() {
        Utilities.animateUp(writingProgress, 100)
        Utilities.animateUp(findViewById(R.id.correctImageView), 100)
        Utilities.animateUp(findViewById(R.id.incorrectImageView), 100)
        Utilities.animateUp(correctTextView, 100)
        Utilities.animateUp(incorrectTextView, 100)
        Utilities.animateUp(findViewById(R.id.wrongAnswersLayout), 100)
        Utilities.animateToLeft(writingLayout, 100)
        Utilities.animateToRight(submitWriting, 100) {

            finishLayout.visibility = View.VISIBLE

            val finishButton = finishLayout.findViewById<MaterialButton>(R.id.finishButton)
            finishButton.setOnClickListener {
                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }
            val linearLayout = finishLayout.findViewById<LinearLayout>(R.id.rootLinearLayout)

            finishLayout.findViewById<TextView>(R.id.endCorrectTextView).text =
                correctReviewAnswers.size.toString()
            finishLayout.findViewById<TextView>(R.id.endIncorrectTextView).text =
                incorrectReviewAnswers.size.toString()

            val correctRecyclerView =
                finishLayout.findViewById<RecyclerView>(R.id.correctRecyclerView)
            val incorrectRecyclerView =
                finishLayout.findViewById<RecyclerView>(R.id.incorrectRecyclerView)

            correctRecyclerView.layoutManager = LinearLayoutManager(this)
            incorrectRecyclerView.layoutManager = LinearLayoutManager(this)
            correctRecyclerView.adapter = ReviewedKanaAdapter(correctReviewAnswers, this, true)
            incorrectRecyclerView.adapter = ReviewedKanaAdapter(incorrectReviewAnswers, this, false)
            correctRecyclerView.layoutAnimation.animation.startOffset = 1500L
            incorrectRecyclerView.layoutAnimation.animation.startOffset = 1500L

            val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)

            Utilities.animateFromTop(linearLayout, rootLayout, 200)
            Utilities.animateFromTop(finishLayout.findViewById(R.id.layoutDivider), rootLayout, 300)
            Utilities.animateFromBottom(finishButton, rootLayout, 400)

        }
    }

    private fun showIncorrectDialog() {

    }

}