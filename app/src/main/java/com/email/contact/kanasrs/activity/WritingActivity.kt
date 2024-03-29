package com.email.contact.kanasrs.activity

import android.R.attr
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.text.bold
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.adapter.ReviewedKanaAdapter
import com.email.contact.kanasrs.custom.DrawingView
import com.email.contact.kanasrs.custom.KanaInfoView
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.util.KanaConverter
import com.email.contact.kanasrs.util.Utilities
import com.email.contact.kanasrs.util.Utilities.Companion.animateToLeft
import com.email.contact.kanasrs.util.Utilities.Companion.animateToRight
import com.email.contact.kanasrs.util.Utilities.Companion.animateUp
import com.email.contact.kanasrs.util.Utilities.Companion.colorizeText
import com.email.contact.kanasrs.util.Utilities.Companion.disable
import com.email.contact.kanasrs.util.Utilities.Companion.dpToPx
import com.email.contact.kanasrs.util.Utilities.Companion.enable
import com.email.contact.kanasrs.util.Utilities.Companion.pxToDp
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    private lateinit var dontKnowTextView: TextView
    private lateinit var newLevelLayout: LinearLayout
    private val imagesAnimatedList = mutableListOf(false, false, false)
    private lateinit var kanaConverter: KanaConverter
    private var wrongCounter = 0

    private var reviewOver = false
    private var transitioning = false

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
        dontKnowTextView = findViewById(R.id.dontKnowTextView)

        findViewById<TextView>(R.id.kanaTypeTextView).text =
            if (intent.getBooleanExtra("hiraganaWriting", true)) {
                "Hiragana"
            } else {
                "Katakana"
            }

        intent.removeExtra("isHiragana")

        writingProgress.max = kanaList.size * 100

        writingLayout = findViewById(R.id.writingRelativeLayout)
        drawingView = DrawingView(this)
        drawingView.setPaintColor(Color.WHITE)

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
                Toast.makeText(
                    this,
                    "Error: please check your internet connection",
                    Toast.LENGTH_SHORT
                ).show()
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
                            correctAnswer(kana)
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
                                    showIncorrectDialog(kana, false)
                                    wrongImageThree.playAnimation()
                                    imagesAnimatedList[2] = true
                                }
                            }
                        }
                    }
                }
            }
        }

        dontKnowTextView.setOnClickListener {
            wrongImageOne.playAnimation()
            imagesAnimatedList[0] = true
            wrongImageTwo.playAnimation()
            imagesAnimatedList[1] = true
            wrongImageThree.playAnimation()
            imagesAnimatedList[2] = true
            showIncorrectDialog(kanaList[0], true)
            drawingView.clearDrawing()
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
        //val oneMinute = 1000 * 60L
        val millisecondsInHours = 1000L * 60 * 60
        val millisecondsInDays = millisecondsInHours * 24
        return when(level) {
            1 -> (millisecondsInHours * 8) // Level 1 is 8 hours after review
            2 -> (millisecondsInDays * 1) // Level 2 is 1 day after review
            3 -> (millisecondsInDays * 3) // Level 3 is 3 days after review
            4 -> (millisecondsInDays * 7) // Level 4 is 7 days (1 week) after review
            5 -> (millisecondsInDays * 14) // Level 5 is 14 day (2 weeks) after review
            6 -> (millisecondsInDays * 30) // Level 6 is 30 days (1 month) after review
            else -> 0
        }
        //For debugging
//        return when (level) {
//            1 -> oneMinute * 1
//            2 -> oneMinute * 2
//            3 -> oneMinute * 3
//            4 -> oneMinute * 4
//            5 -> oneMinute * 5
//            6 -> oneMinute * 6
//            else -> 0
//        }
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

        transitioning = true
        wrongCounter = 0
        drawingView.clearDrawing()
        drawingView.disableDrawing()

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
                drawingView.enableDrawing()
                transitioning = false
            }.duration = 500
        }.duration = 500

    }

    private fun endSession() {
        transitioning = true

        dontKnowTextView.animate().alpha(0f).duration = 200
        animateUp(writingProgress, 100)
        animateUp(findViewById(R.id.correctImageView), 100)
        animateUp(findViewById(R.id.incorrectImageView), 100)
        animateUp(correctTextView, 100)
        animateUp(incorrectTextView, 100)
        animateUp(newLevelLayout, 100)
        animateUp(findViewById(R.id.wrongAnswersLayout), 100)
        animateToLeft(writingLayout, 100)
        animateToRight(submitWriting, 100) {

            finishLayout.visibility = View.VISIBLE

            val finishButton = finishLayout.findViewById<MaterialButton>(R.id.finishButton)
            finishButton.background =
                ContextCompat.getDrawable(this, R.drawable.extra_button_selector)
            finishButton.setPadding(0, dpToPx(40), 0, dpToPx(40))
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
            correctRecyclerView.adapter =
                ReviewedKanaAdapter(correctReviewAnswers, this, correct = true, isWriting = true)
            incorrectRecyclerView.adapter =
                ReviewedKanaAdapter(incorrectReviewAnswers, this, correct = false, isWriting = true)
            correctRecyclerView.layoutAnimation.animation.startOffset = 1500L
            incorrectRecyclerView.layoutAnimation.animation.startOffset = 1500L

            val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)

            Utilities.animateFromTop(linearLayout, rootLayout, 200)
            Utilities.animateFromTop(finishLayout.findViewById(R.id.layoutDivider), rootLayout, 300)
            Utilities.animateFromBottom(finishButton, rootLayout, 400)

            reviewOver = true
            transitioning = false

        }
    }

    private fun incorrectAnswer(kana: Kana) {
        if (!incorrectReviewAnswers.contains(kana)) {

            incorrectReviewAnswers.add(kana)
            calculateNextReviewTime(kana, correct = false)

            kana.writingStreak = 0
            if (kana.writingTotalAnswered == null) {
                kana.writingTotalAnswered = 1
            } else {
                kana.writingTotalAnswered = kana.writingTotalAnswered?.plus(1)
            }
            KanaSRSDatabase.getInstance(this).kanaDao().updateKana(kana)


            val levelText: String = when (kana.writingLevel) {
                1, 2 -> "Rookie"
                3 -> "Amateur"
                4 -> "Expert"
                5 -> "Master"
                6 -> "Sensei"
                else -> "Error"
            }

            arrowIndicator.setImageResource(R.drawable.ic_down_arrow)
            newLevelTextView.text = levelText

            newLevelLayout.backgroundTintList =
                AppCompatResources.getColorStateList(this@WritingActivity, R.color.wrong_answer)
            newLevelLayout.visibility = View.VISIBLE
            newLevelLayout.alpha = 1F
        }

        if (kanaList.size > 1) {
            val newKanaPosition = Random.nextInt(1, kanaList.size)
            kanaList.remove(kana)
            kanaList.add(newKanaPosition, kana)
        }
    }

    private fun correctAnswer(kana: Kana) {
        correctAnimation.playAnimation()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val animation = ObjectAnimator.ofInt(
                writingProgress,
                "progress",
                writingProgress.progress,
                writingProgress.progress + 100
            )
            animation.duration = 1000
            animation.interpolator = DecelerateInterpolator()
            animation.start()
        } else {
            writingProgress.progress = correctReviewAnswers.size
        }

        kanaList.remove(kana)

        if (!incorrectReviewAnswers.contains(kana)) {

            calculateNextReviewTime(kana, correct = true)
            correctReviewAnswers.add(kana)

            kana.writingStreak += 1

            if (kana.writingTotalCorrect == null) {
                kana.writingTotalCorrect = 1
                if (kana.writingTotalAnswered == null) {
                    kana.writingTotalAnswered = 1
                } else {
                    kana.writingTotalAnswered = kana.writingTotalAnswered?.plus(1)
                }
            } else {
                kana.writingTotalCorrect = kana.writingTotalCorrect?.plus(1)
                kana.writingTotalAnswered = kana.writingTotalAnswered?.plus(1)
            }

            KanaSRSDatabase.getInstance(this).kanaDao().updateKana(kana)

            var levelText = ""
            var color = 0

            when (kana.writingLevel) {
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
            newLevelLayout.backgroundTintList = AppCompatResources.getColorStateList(
                this@WritingActivity,
                android.R.color.darker_gray
            )
            newLevelLayout.visibility = View.VISIBLE
            newLevelLayout.alpha = 1F
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showIncorrectDialog(kana: Kana, forced: Boolean) {

        val view = LayoutInflater.from(this).inflate(R.layout.wrong_writing_dialog, null)
        val dialog = BottomSheetDialog(this, R.style.BottomDialogTheme)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setContentView(view)
        dialog.show()

        val romaji = kanaConverter.hiraganaToRomaji(letterToDraw.text.toString())
        val colorizedText =
            romaji?.let {
                romaji.colorizeText(
                    it,
                    ContextCompat.getColor(applicationContext, R.color.lime)
                )
            }

        val correctString = SpannableStringBuilder()
            .append("Here is how to write ")
            .bold { append(colorizedText) }
            .append(":")

        view.findViewById<TextView>(R.id.correctAnswerTextView).text = correctString

        val wrongImages = drawingView.getWrongImages().toMutableList()
        val roundedImages = mutableListOf<RoundedBitmapDrawable>()

        for (bitmap in wrongImages) {
            val roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                resources, bitmap
            )
            val roundPx = bitmap.width.toFloat() * 0.1f
            roundedBitmapDrawable.cornerRadius = roundPx
            roundedImages.add(roundedBitmapDrawable)
        }


        if (!forced) {
            view.findViewById<ImageView>(R.id.wrongImageOne).setImageDrawable(roundedImages[0])
            view.findViewById<ImageView>(R.id.wrongImageTwo).setImageDrawable(roundedImages[1])
            view.findViewById<ImageView>(R.id.wrongImageThree).setImageDrawable(roundedImages[2])
        } else {
            view.findViewById<LinearLayout>(R.id.wrongWritingsLayout).visibility = View.INVISIBLE
        }

        drawingView.clearWrongImages()

        val correctWebStroke = view.findViewById<WebView>(R.id.correctWritingWebView)
        correctWebStroke.settings.javaScriptEnabled = true
        correctWebStroke.webViewClient = WebViewClient()
        correctWebStroke.loadUrl(kana.gif)
        correctWebStroke.settings.loadWithOverviewMode = true
        correctWebStroke.settings.useWideViewPort = true

        val strokeId = if (kana.isHiragana) {
            resources.getIdentifier(
                "${kanaConverter.hiraganaToRomaji(kana.letter!!)}_stroke",
                "drawable",
                packageName
            )
        } else {
            resources.getIdentifier(
                "${kanaConverter.hiraganaToRomaji(kana.letter!!)}_stroke_k",
                "drawable",
                packageName
            )
        }

        view.findViewById<ImageView>(R.id.correctStrokeImage).setImageResource(strokeId)

        view.findViewById<TextView>(R.id.overrideButton).setOnClickListener {
            correctAnswer(kana)
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.practiceButton).setOnClickListener {
            val kanaInfo = KanaInfoView(this, kana, showReviewTime = true, writingInfo = true)
            kanaInfo.setReviewToGone()
            kanaInfo.show()
        }

        view.findViewById<TextView>(R.id.moveOnButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            incorrectAnswer(kana)
            nextKana()
        }
    }

    override fun onBackPressed() {
        if (!transitioning) {

            if (reviewOver) {
                startActivity(Intent(this, MenuActivity::class.java))
                return
            }

            val view = layoutInflater.inflate(R.layout.leave_review_dialog, null)
            val dialog = AlertDialog.Builder(this, R.style.DialogTheme).create()

            view.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
                dialog.dismiss()
            }

            view.findViewById<TextView>(R.id.endReviewTextView).setOnClickListener {
                if (correctReviewAnswers.isEmpty() && incorrectReviewAnswers.isEmpty()) {
                    startActivity(Intent(this, MenuActivity::class.java))
                } else {
                    endSession()
                }
                dialog.dismiss()
            }

            dialog.setView(view)
            dialog.show()
        }
    }
}