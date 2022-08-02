package com.email.contact.kanasrs.activity

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.email.contact.kanasrs.*
import com.email.contact.kanasrs.adapter.ReviewedKanaAdapter
import com.email.contact.kanasrs.custom.KanaInfoView
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.util.KanaConverter
import com.email.contact.kanasrs.util.Utilities
import com.email.contact.kanasrs.util.Utilities.Companion.colorizeText
import com.email.contact.kanasrs.util.Utilities.Companion.reviewApp
import com.email.contact.kanasrs.util.Utilities.Companion.showKeyboard
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import java.util.*
import kotlin.random.Random


/**
 *
 * Activity view that holds kana review sessions
 *
 * @author Lucas Curran
 */
class ReviewActivity : AppCompatActivity() {

    private lateinit var responseImage: LottieAnimationView
    private lateinit var letterTextView: TextView
    private lateinit var submitButton: Button
    private lateinit var userResponseEditText: EditText
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var kanaConverter: KanaConverter
    private lateinit var kanaList: MutableList<Kana>
    private lateinit var newLevelLayout: LinearLayout
    private lateinit var arrowIndicator: ImageView
    private lateinit var newLevelTextView: TextView
    private lateinit var reviewProgressBar: ProgressBar
    private lateinit var endReviewLayout: ConstraintLayout

    private lateinit var numberCorrectTextView: TextView
    private lateinit var incorrectTextView: TextView
    private lateinit var correctImageView: ImageView
    private lateinit var incorrectImageView: ImageView

    private lateinit var correctTransition: TransitionDrawable
    private lateinit var incorrectTransition: TransitionDrawable

    //Number correct out of total answered
    private var score = 0
    private var incorrect = 0
    private var totalAnswered = 0

    //Japanese wrong answers is
    private val correctReviewAnswers = arrayListOf<Kana>()
    private val incorrectReviewAnswers = arrayListOf<Kana>()

    private var quiz = false
    private var review = false
    private var transitioning = false
    private var reviewOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        //need to clone the list or it will mess up its order when shuffling
        if (intent.getBooleanExtra("quiz", false)) {
            val parcelableList = intent.getParcelableArrayExtra("kana")
            kanaList = (parcelableList?.asList() as List<Kana>).shuffled().toMutableList()
            intent.removeExtra("quiz")
            intent.removeExtra("kana")
            quiz = true
        } else if (intent.getBooleanExtra("review", false)){
            val parcelableList = intent.getParcelableArrayListExtra<Kana>("kana")
            kanaList = (parcelableList?.toList() as List<Kana>).shuffled().toMutableList()
            intent.removeExtra("review")
            intent.removeExtra("kana")
            review = true
        }

        kanaConverter = KanaConverter(false)

        responseImage = findViewById(R.id.responseImageView)
        letterTextView = findViewById(R.id.letterToGuess)
        userResponseEditText = findViewById(R.id.responseEditText)
        rootLayout = findViewById(R.id.rootLayout)
        newLevelLayout = findViewById(R.id.newLevelLayout)
        arrowIndicator = findViewById(R.id.arrowIndicator)
        newLevelTextView = findViewById(R.id.newLevelTextView)
        reviewProgressBar = findViewById(R.id.reviewProgressBar)
        numberCorrectTextView = findViewById(R.id.numberCorrectTextView)
        incorrectTextView = findViewById(R.id.numberWrongTextView)
        correctImageView = findViewById(R.id.correctImageView)
        incorrectImageView = findViewById(R.id.incorrectImageView)
        endReviewLayout = findViewById(R.id.finishGameLayout)

        correctTransition = TransitionDrawable(arrayOf(
            ContextCompat.getDrawable(this, R.drawable.review_box),
            ContextCompat.getDrawable(this, R.drawable.input_background_correct))
        )
        incorrectTransition = TransitionDrawable(arrayOf(
            ContextCompat.getDrawable(this, R.drawable.review_box),
            ContextCompat.getDrawable(this, R.drawable.input_background_incorrect))
        )

        submitButton = findViewById(R.id.submitButton)
        submitButton.setOnSingleClickListener {
            if (!transitioning) {
                if (userResponseEditText.text.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Please enter something in the input field",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnSingleClickListener
                }

                //If guess is correct, show positive response and move on to next letter
                //else, show negative response, show correct answer, and make "next" button visible

                if (userResponseEditText.text.toString()
                        .lowercase() == kanaConverter.hiraganaToRomaji(letterTextView.text.toString())
                ) {
                    correctAnswer()
                } else {
                    incorrectAnswer()
                }
            }
        }

        userResponseEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if ((keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) || (i == EditorInfo.IME_ACTION_DONE)) {
                if (!transitioning) {
                    submitButton.performClick()
                }
            }
            true
        }

        reviewProgressBar.max = kanaList.size * 100

        switchFont()

        letterTextView.text = kanaList[0].letter

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                userResponseEditText.showKeyboard()
            }
        }, 500)

    }

    /**
     * Moves on to next letter in list
     */
    private fun moveToNext(incorrect: Boolean) {
        transitioning = true
        submitButton.isEnabled = false
        userResponseEditText.keyListener = null

        if (incorrect) {
            incorrectTransition.reverseTransition(300)
        }

        if (kanaList.size == 0 && review) {
            completedSet()
            return
        } else if (kanaList.size == 0 && quiz) {

            learnQuizKana()
            submitButton.isEnabled = false
            userResponseEditText.isEnabled = false
            val view = layoutInflater.inflate(R.layout.end_quiz_dialog, null)
            val dialog = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialog.setCancelable(false)

            val moreLessonsButton = view.findViewById<MaterialButton>(R.id.moreLessonsButton)
            val endLessonButton = view.findViewById<MaterialButton>(R.id.endLessonButton)

            reviewApp(this, this)

            if (KanaSRSDatabase.getInstance(this).userDao().getUser().lessonsNumber!! > 0) {
                moreLessonsButton.setOnClickListener {
                    startActivity(Intent(this, LessonActivity::class.java))
                    finish()
                }
            } else {
                moreLessonsButton.visibility = View.GONE
                view.findViewById<TextView>(R.id.descriptionText).text = "No more lessons are available for now. Good work!"
                endLessonButton.text = "Back to menu"
            }

            endLessonButton.setOnClickListener {
                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }

            dialog.show()

            return
        }

        // Animate letter to the right, change text,
        // then set it back to the left and animate back right on screen
        // and then overshoot by a little, and recorrect
        letterTextView.animate().translationXBy((rootLayout.width).toFloat()).withEndAction {
            letterTextView.x = (-rootLayout.width).toFloat() / 2
            switchFont()
            letterTextView.text = kanaList[0].letter

            letterTextView.animate().translationXBy(rootLayout.width.toFloat() + letterTextView.width / 2).withEndAction {
                if (!incorrect) {
                    responseImage.reverseAnimationSpeed()
                    responseImage.playAnimation()
                }
                letterTextView.animate().translationXBy(-(letterTextView.width).toFloat()).duration = 500
                correctTransition.reverseTransition(300)
                userResponseEditText.setText("")
                newLevelLayout.animate().alpha(0F).duration = 300
                //TODO: Set digits again programmatically
                userResponseEditText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                userResponseEditText.isEnabled = true
                submitButton.isEnabled = true
                //Make cursor invisible and back to visible because of UI glitch
                userResponseEditText.isCursorVisible = false
                userResponseEditText.isCursorVisible = true
                userResponseEditText.showKeyboard()
                transitioning = false
            }.duration = 850
        }.duration = 850
    }

    /**
     * Executes when user inputs incorrect answer
     */
    private fun incorrectAnswer() {

        totalAnswered++

        if (review) {
            val kana = kanaList[0]
            showWrongDialog(kana)

            if (!incorrectReviewAnswers.contains(kana)) {

                incorrect++
                kana.streak = 0
                if (kana.totalAnswered == null) {
                    kana.totalAnswered = 1
                } else {
                    kana.totalAnswered = kana.totalAnswered?.plus(1)
                }
                KanaSRSDatabase.getInstance(this).kanaDao().updateKana(kana)

                val levelText: String = when (kana.level?.minus(1)) {
                    0, 1, 2 -> "Rookie"
                    3 -> "Amateur"
                    4 -> "Expert"
                    5 -> "Master"
                    6 -> "Sensei"
                    else -> "Error"
                }

                arrowIndicator.setImageResource(R.drawable.ic_down_arrow)
                newLevelTextView.text = levelText

                newLevelLayout.backgroundTintList = AppCompatResources.getColorStateList(this, R.color.wrong_answer)
                newLevelLayout.visibility = View.VISIBLE
                newLevelLayout.alpha = 1F

                calculateNextReviewTime(kana, correct = false)

                incorrectReviewAnswers.add(kana)
            }

            if (kanaList.size > 1) {
                val newKanaPosition = Random.nextInt(1, kanaList.size)
                kanaList.remove(kana)
                kanaList.add(newKanaPosition, kana)
            }
        }

        if (quiz) {
            val kana = kanaList[0]
            showWrongDialog(kana)
            if (kanaList.size > 1) {
                val newKanaPosition = Random.nextInt(1, kanaList.size)
                kanaList.remove(kana)
                kanaList.add(newKanaPosition, kana)
            }
        }

        incorrectTextView.text = incorrect.toString()

        userResponseEditText.background = incorrectTransition
        incorrectTransition.startTransition(300)

        responseImage.visibility = View.VISIBLE

        //Set image as x, and start animation
        if (responseImage.speed < 0) {
            responseImage.reverseAnimationSpeed()
        }
        responseImage.alpha = 1F
        responseImage.setAnimation(R.raw.wrong)
        responseImage.playAnimation()
        userResponseEditText.isEnabled = false
    }

    /**
     * Executes when user inputs correct answer
     */
    private fun correctAnswer() {

        totalAnswered++

        if (review) {
            val kana = kanaList[0]

            //If the answer wasn't previously answered incorrectly, show the new level.
            // If it was, the new level was shown previously, therefore just give a nice job :)
            if (!incorrectReviewAnswers.contains(kana)) {

                calculateNextReviewTime(kana = kana, correct = true)

                kana.streak += 1

                if (kana.totalCorrect == null) {
                    kana.totalCorrect = 1
                    if (kana.totalAnswered == null) {
                        kana.totalAnswered = 1
                    } else {
                        kana.totalAnswered = kana.totalAnswered?.plus(1)
                    }
                } else {
                    kana.totalCorrect = kana.totalCorrect?.plus(1)
                    kana.totalAnswered = kana.totalAnswered?.plus(1)
                }

                KanaSRSDatabase.getInstance(this).kanaDao().updateKana(kana)

                var levelText = ""
                var color = 0

                when (kana.level) {
                    1, 2 ->  {
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

                newLevelLayout.backgroundTintList = AppCompatResources.getColorStateList(this, color)
                newLevelLayout.visibility = View.VISIBLE
                newLevelLayout.alpha = 1F

                score++

                correctReviewAnswers.add(kana)

            } else {
                arrowIndicator.setImageResource(R.drawable.ic_checkmark)
                newLevelTextView.text = "Corrected!"
                newLevelLayout.backgroundTintList = AppCompatResources.getColorStateList(this, android.R.color.darker_gray)
                newLevelLayout.visibility = View.VISIBLE
                newLevelLayout.alpha = 1F
            }

            kanaList.remove(kana)
        }

        if (quiz) {
            val kana = kanaList[0]
            score++
            kanaList.remove(kana)
            correctReviewAnswers.add(kana)
        }

        userResponseEditText.background = correctTransition
        correctTransition.startTransition(300)

        responseImage.visibility = View.VISIBLE

        numberCorrectTextView.text = score.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val animation = ObjectAnimator.ofInt(reviewProgressBar, "progress", reviewProgressBar.progress, reviewProgressBar.progress+100)
            animation.duration = 1000
            animation.interpolator = DecelerateInterpolator()
            animation.start()
        } else {
            reviewProgressBar.progress = score
        }

        //Set image as checkmark, and start animation
        if (responseImage.speed < 0) {
            responseImage.reverseAnimationSpeed()
        }
        responseImage.setAnimation(R.raw.correct)
        responseImage.playAnimation()
        //when animation is done, move to next letter
        moveToNext(incorrect = false)
    }

    /**
     * Function called when the user finishes the set of kana
     */
    private fun completedSet() {

        reviewOver = true
        transitioning = true
        submitButton.isEnabled = false
        userResponseEditText.isEnabled = false

        // Animate views off the screen and then animate the end screen onto screen

        Utilities.animateUp(reviewProgressBar, 100)
        Utilities.animateUp(correctImageView, 100)
        Utilities.animateUp(incorrectImageView, 100)
        Utilities.animateUp(numberCorrectTextView, 100)
        Utilities.animateUp(incorrectTextView, 100)

        //Animate all the on screen views to the bottom
        Utilities.animateToLeft(responseImage, 100)
        Utilities.animateUp(newLevelLayout, 200)
        Utilities.animateUp(letterTextView, 300)
        Utilities.animateToLeft(userResponseEditText, 400)
        //After last view is off screen, begin animating end screen results to screen
        Utilities.animateToRight(submitButton, 400) {
            transitioning = false
            endReviewLayout.visibility = View.VISIBLE
            val finishButton = endReviewLayout.findViewById<MaterialButton>(R.id.finishButton)
            finishButton.setOnClickListener {
                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }
            val linearLayout = endReviewLayout.findViewById<LinearLayout>(R.id.rootLinearLayout)

            endReviewLayout.findViewById<TextView>(R.id.endCorrectTextView).text = correctReviewAnswers.size.toString()
            endReviewLayout.findViewById<TextView>(R.id.endIncorrectTextView).text = incorrectReviewAnswers.size.toString()

            val correctRecyclerView = endReviewLayout.findViewById<RecyclerView>(R.id.correctRecyclerView)
            val incorrectRecyclerView = endReviewLayout.findViewById<RecyclerView>(R.id.incorrectRecyclerView)

            correctRecyclerView.layoutManager = LinearLayoutManager(this)
            incorrectRecyclerView.layoutManager = LinearLayoutManager(this)
            correctRecyclerView.adapter = ReviewedKanaAdapter(correctReviewAnswers, this, correct = true, isWriting = false)
            incorrectRecyclerView.adapter = ReviewedKanaAdapter(incorrectReviewAnswers, this, correct = false,isWriting = false)
            correctRecyclerView.layoutAnimation.animation.startOffset = 1500L
            incorrectRecyclerView.layoutAnimation.animation.startOffset = 1500L

            Utilities.animateFromTop(linearLayout, rootLayout, 200)
            Utilities.animateFromTop(endReviewLayout.findViewById(R.id.layoutDivider), rootLayout, 300)
            Utilities.animateFromBottom(finishButton, rootLayout, 400)
        }
    }

    private fun switchFont() {
        if (getSharedPreferences(
                getString(R.string.pref_key),
                Context.MODE_PRIVATE
            ).getBoolean("randomizedFonts", false)
        ) {
            if (review) {
                val randomizer = Random.nextInt(1, 18)
                val font = ResourcesCompat.getFont(
                    this,
                    resources.getIdentifier("font$randomizer", "font", packageName)
                )
                letterTextView.typeface = font
            }
        }
    }

    private fun showWrongDialog(kana: Kana) {
        val view = LayoutInflater.from(this).inflate(R.layout.wrong_answer_dialog, null)
        val dialog = BottomSheetDialog(this, R.style.BottomDialogTheme)

        val romaji = kanaConverter.hiraganaToRomaji(letterTextView.text.toString())
        val colorizedText =
            romaji?.let { romaji.colorizeText(it, ContextCompat.getColor(applicationContext, R.color.lime)) }

        val correctString = SpannableStringBuilder()
            .append("The correct answer is: ")
            .bold { append(colorizedText) }

        view.findViewById<TextView>(R.id.correctAnswerTextView).text = correctString

        val mnemonicText = view.findViewById<TextView>(R.id.mnemonicTextView)

        if (kana.customMnemonic != null) {
            mnemonicText.text = kana.customMnemonic
        } else {
            mnemonicText.text = kana.mnemonic
            if (mnemonicText.text.contains("(${kanaConverter.hiraganaToRomaji(kana.letter!!)})")) {
                mnemonicText.text = mnemonicText.text.colorizeText(
                    "(${kanaConverter.hiraganaToRomaji(kana.letter)})",
                    ContextCompat.getColor(this, R.color.pink)
                )
            }
            if (mnemonicText.text.contains("'${kanaConverter.hiraganaToRomaji(kana.letter)}'")) {
                mnemonicText.text = mnemonicText.text.colorizeText(
                    "'${kanaConverter.hiraganaToRomaji(kana.letter)}'",
                    ContextCompat.getColor(this, R.color.pink)
                )
            }
            if (mnemonicText.text.contains("'${kanaConverter.hiraganaToRomaji(kana.letter)?.uppercase()}'")) {
                mnemonicText.text = mnemonicText.text.colorizeText(
                    "'${kanaConverter.hiraganaToRomaji(kana.letter)?.uppercase()}'",
                    ContextCompat.getColor(this, R.color.pink)
                )
            }
        }

        view.findViewById<TextView>(R.id.moreInfoTextView).setOnClickListener {
            val kanaInfo = KanaInfoView(this, kana, true, writingInfo = false)
            kanaInfo.setReviewToGone()
            kanaInfo.show()
        }
        view.findViewById<TextView>(R.id.moveOnButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            moveToNext(incorrect = true)
            userResponseEditText.setText("")
            responseImage.reverseAnimationSpeed()
            responseImage.playAnimation()
            newLevelLayout.animate().alpha(0F).duration = 300
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun learnKana(kana: Kana) {
        kana.hasLearned = true
        kana.level = 1
        kana.writingLevel = 1
        val oneMinute = 1000 * 60
        val now = System.currentTimeMillis()
        val nextPracticeDate = now + oneMinute * kana.level!!
        kana.reviewTime = nextPracticeDate
        kana.writingReviewTime = nextPracticeDate
        KanaSRSDatabase.getInstance(this).kanaDao().updateKana(kana)
    }

    private fun calculateNextReviewTime(kana: Kana, correct: Boolean) {
        if (correct) {
            if (kana.level!! < 6) {
                kana.level = kana.level?.plus(1)
            }
        } else {
            if (kana.level!! > 1) {
                kana.level = kana.level?.minus(1)
            }
        }
        val now = System.currentTimeMillis()
        val nextPracticeDate = now + levelToTime(kana.level!!)
        if (kana.level == 6) {
            kana.reviewTime = null
        } else {
            kana.reviewTime = nextPracticeDate
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

    private fun learnQuizKana() {
        for (kana in correctReviewAnswers) {
            learnKana(kana)
        }

        val user = KanaSRSDatabase.getInstance(this).userDao().getUser()
        // Subtract remaining lessons for the day by the quiz size, and
        // then if the lesson refresh time isn't set, make it one day from now
        user.lessonsNumber = user.lessonsNumber?.minus(5)
        if (user.lessonRefreshTime == null) {
            val oneDay = 24 * 1000L * 60 * 60
            //val oneMinute = 1000 * 60
            user.lessonRefreshTime = System.currentTimeMillis() + oneDay
        }
        KanaSRSDatabase.getInstance(this).userDao().updateUser(user)
    }

    override fun onBackPressed() {
        if (!transitioning) {

            if (reviewOver) {
                startActivity(Intent(this, MenuActivity::class.java))
                return
            }

            val view = layoutInflater.inflate(R.layout.leave_review_dialog, null)
            val dialog = AlertDialog.Builder(this, R.style.DialogTheme).create()

            if (quiz) {
                view.findViewById<TextView>(R.id.titleTextView).text = "End Quiz"
                view.findViewById<TextView>(R.id.quizExplainTextView).visibility = View.VISIBLE
            }

            view.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
                dialog.dismiss()
            }

            view.findViewById<TextView>(R.id.endReviewTextView).setOnClickListener {
                // If it's a quiz, just send user back to menu without learning the kana
                // If it's review, then complete what they have done, or if they have not answered yet, send them to menu
                if (quiz) {
                    startActivity(Intent(this, MenuActivity::class.java))
                } else if (review && !reviewOver) {
                    if (totalAnswered == 0) {
                        startActivity(Intent(this, MenuActivity::class.java))
                    } else {
                        completedSet()
                    }
                }
                dialog.dismiss()
            }

            dialog.setView(view)
            dialog.show()
        }
    }

}