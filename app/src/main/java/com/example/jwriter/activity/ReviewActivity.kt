package com.example.jwriter.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jwriter.*
import com.example.jwriter.database.JWriterDatabase
import com.example.jwriter.database.Kana
import com.example.jwriter.util.Utilities
import com.example.jwriter.util.Utilities.Companion.colorizeText
import com.example.jwriter.util.KanaConverter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.random.Random

/**
 *
 * Activity view that holds kana review sessions
 *
 * @author Lucas Curran
 */
class ReviewActivity : AppCompatActivity() {

    private lateinit var responseImage: ImageView
    private lateinit var letterTextView: TextView
    private lateinit var submitButton: Button
    private lateinit var userResponseEditText: EditText
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var kanaConverter: KanaConverter
    private lateinit var kanaList: MutableList<Kana>
    private lateinit var layout: ConstraintLayout
    private lateinit var scoreText: TextView
    private lateinit var userScoreText: TextView
    private lateinit var restartButton: Button
    private lateinit var backToMenuButton: Button
    private lateinit var itemsWrongRecyclerView: RecyclerView
    private lateinit var emptyRecyclerViewText: TextView
    private lateinit var newLevelLayout: LinearLayout
    private lateinit var arrowIndicator: ImageView
    private lateinit var newLevelTextView: TextView
    private lateinit var reviewProgressBar: ProgressBar
    private lateinit var numberCorrectTextView: TextView
    private lateinit var incorrectImageView: ImageView
    private lateinit var incorrectTextView: TextView

    private lateinit var correctTransition: TransitionDrawable
    private lateinit var incorrectTransition: TransitionDrawable

    //Number correct out of total answered
    private var score = 0
    private var totalAnswered = 0

    //Japanese wrong answers is
    private val japaneseWrongAnswers = arrayListOf<String>()
    private val englishWrongAnswers = arrayListOf<String>()
    private val correctQuizAnswers = arrayListOf<Kana>()
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
        layout = findViewById(R.id.finishGameLayout)
        scoreText = findViewById(R.id.scoreTextView)
        userScoreText = findViewById(R.id.userScoreTextView)
        restartButton = findViewById(R.id.restartButton)
        backToMenuButton = findViewById(R.id.backToMenuButton)
        emptyRecyclerViewText = findViewById(R.id.empty_view)
        newLevelLayout = findViewById(R.id.newLevelLayout)
        arrowIndicator = findViewById(R.id.arrowIndicator)
        newLevelTextView = findViewById(R.id.newLevelTextView)
        reviewProgressBar = findViewById(R.id.reviewProgressBar)
        numberCorrectTextView = findViewById(R.id.numberCorrectTextView)
        incorrectTextView = findViewById(R.id.numberWrongTextView)

        correctTransition = TransitionDrawable(arrayOf(
            ContextCompat.getDrawable(this, R.drawable.square_outline),
            ContextCompat.getDrawable(this, R.drawable.input_background_correct))
        )
        incorrectTransition = TransitionDrawable(arrayOf(
            ContextCompat.getDrawable(this, R.drawable.square_outline),
            ContextCompat.getDrawable(this, R.drawable.input_background_incorrect))
        )

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        itemsWrongRecyclerView = findViewById(R.id.itemsWrongRecyclerView)
        itemsWrongRecyclerView.layoutManager = layoutManager

        itemsWrongRecyclerView.adapter = WrongItemAdapter(englishWrongAnswers, japaneseWrongAnswers)

        submitButton = findViewById(R.id.submitButton)
        submitButton.setOnClickListener {

            if (userResponseEditText.text.isEmpty()) {
                Toast.makeText(this, "Please enter something in the input field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //If guess is correct, show positive response and move on to next letter
            //else, show negative response, show correct answer, and make "next" button visible

            if (userResponseEditText.text.toString().lowercase() == kanaConverter._hiraganaToRomaji(letterTextView.text.toString())) {
                correctAnswer()
            } else {
                incorrectAnswer()
            }
        }

        userResponseEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if ((keyEvent != null && (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) || (i == EditorInfo.IME_ACTION_DONE)) {
                submitButton.performClick()
            }
            true
        }

        reviewProgressBar.max = kanaList.size

        letterTextView.text = kanaList[0].letter

        userResponseEditText.requestFocus()

        val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(userResponseEditText, InputMethodManager.SHOW_IMPLICIT)
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

        if (kanaList.size == 0) {
            completedSet()
            return
        }

        // Animate letter to the right, change text,
        // then set it back to the left and animate back right on screen
        // and then overshoot by a little, and recorrect
        letterTextView.animate().translationXBy((rootLayout.width).toFloat()).withEndAction {
            letterTextView.x = (-rootLayout.width).toFloat() / 2
            letterTextView.text = kanaList[0].letter
            letterTextView.animate().translationXBy(rootLayout.width.toFloat() + letterTextView.width / 2).withEndAction {
                letterTextView.animate().translationXBy(-(letterTextView.width).toFloat()).duration = 500
                correctTransition.reverseTransition(300)
                userResponseEditText.setText("")
                responseImage.animate().alpha(0F).duration = 300
                newLevelLayout.animate().alpha(0F).duration = 300
                userResponseEditText.inputType = InputType.TYPE_CLASS_TEXT
                userResponseEditText.isEnabled = true
                submitButton.isEnabled = true
                //Make cursor invisible and back to visible because of UI glitch
                userResponseEditText.isCursorVisible = false
                userResponseEditText.isCursorVisible = true
                userResponseEditText.requestFocus()
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(userResponseEditText, InputMethodManager.SHOW_IMPLICIT)
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

                incorrectReviewAnswers.add(kana)
            }

            if (kanaList.size > 1) {
                val newKanaPosition = Random.nextInt(1, kanaList.size)
                kanaList.remove(kana)
                kanaList.add(newKanaPosition, kana)
            } else if (kanaList.size == 2) {
                kanaList.remove(kana)
                kanaList.add(1, kana)
            }
        }

        if (quiz) {
            val kana = kanaList[0]
            showWrongDialog(kana)
            if (kanaList.size > 1) {
                val newKanaPosition = Random.nextInt(1, kanaList.size)
                kanaList.remove(kana)
                kanaList.add(newKanaPosition, kana)
            } else if (kanaList.size == 2) {
                kanaList.remove(kana)
                kanaList.add(1, kana)
            }
        }

        incorrectTextView.text = (totalAnswered - score).toString()

        userResponseEditText.background = incorrectTransition
        incorrectTransition.startTransition(300)

        englishWrongAnswers.add(userResponseEditText.text.toString())
        japaneseWrongAnswers.add(letterTextView.text.toString())

        itemsWrongRecyclerView.adapter?.notifyItemInserted(japaneseWrongAnswers.size-1)

        responseImage.visibility = View.VISIBLE

        //Set image as x, and start animation
        responseImage.alpha = 1F
        responseImage.setImageResource(R.drawable.animated_incorrect)
        (responseImage.drawable as Animatable).start()
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

                incorrectReviewAnswers.remove(kana)

            } else {
                calculateNextReviewTime(kana = kana, correct = false)
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
            kanaList.remove(kana)
            correctQuizAnswers.add(kana)
        }

        userResponseEditText.background = correctTransition
        correctTransition.startTransition(300)

        responseImage.visibility = View.VISIBLE
        score++

        numberCorrectTextView.text = score.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            reviewProgressBar.setProgress(score, true)
        } else {
            reviewProgressBar.progress = score
        }

        //Set image as checkmark, and start animation
        responseImage.alpha = 1F
        responseImage.setImageResource(R.drawable.animated_check)
        (responseImage.drawable as Animatable).start()
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

        if (quiz) {
            for (kana in correctQuizAnswers) {
                learnKana(kana)
            }

            val user = JWriterDatabase.getInstance(this).userDao().getUser()
            // Subtract remaining lessons for the day by the quiz size, and
            // then if the lesson refresh time isn't set, make it one day from now
            user.lessonsNumber = user.lessonsNumber?.minus(correctQuizAnswers.size)
            if (user.lessonRefreshTime == null) {
                val oneDay = 24 * 1000L * 60 * 60
                //val oneMinute = 1000 * 60
                user.lessonRefreshTime = System.currentTimeMillis() + oneDay
            }
            JWriterDatabase.getInstance(this).userDao().updateUser(user)
        }

        // If no wrong answers,
        // make wrong answer recycler invisible,
        // show perfect score label
        // and then reconfigure constraints on buttons
        if (japaneseWrongAnswers.isEmpty()) {
            itemsWrongRecyclerView.visibility = View.GONE
            emptyRecyclerViewText.visibility = View.VISIBLE
            val restartParams = restartButton.layoutParams as ConstraintLayout.LayoutParams
            restartParams.topToBottom = emptyRecyclerViewText.id
            restartButton.requestLayout()
        }

        //Animate all the on screen views to the bottom
        Utilities.animateEnd(letterTextView, rootLayout, 300) {
            letterTextView.visibility = View.INVISIBLE
        }
        Utilities.animateEnd(userResponseEditText, rootLayout, 400) {
            userResponseEditText.visibility = View.INVISIBLE
        }
        Utilities.animateEnd(submitButton, rootLayout, 500) {
            submitButton.visibility = View.INVISIBLE
            layout.visibility = View.VISIBLE
            userScoreText.text = "$score/$totalAnswered"

            Utilities.animateFromLeft(scoreText, layout, startDelay = 200)
            Utilities.animateFromRight(userScoreText, layout, startDelay = 400)
            if (itemsWrongRecyclerView.isVisible) Utilities.animateFromLeft(
                itemsWrongRecyclerView,
                layout,
                startDelay = 600
            )
            else Utilities.animateFromLeft(emptyRecyclerViewText, layout, startDelay = 600)
            Utilities.animateFromRight(restartButton, layout, startDelay = 800)
            Utilities.animateFromLeft(backToMenuButton, layout, startDelay = 1000)

            backToMenuButton.setOnClickListener {
                startActivity(Intent(this, MenuActivity::class.java))
            }
            transitioning = false
        }
        Utilities.animateEnd(newLevelLayout, rootLayout, 200) {
            newLevelLayout.visibility = View.INVISIBLE
        }

        //After last view is off screen, begin animating end screen results to screen
        Utilities.animateEnd(responseImage, rootLayout, 100) {
            responseImage.visibility = View.INVISIBLE
        }
    }

    private fun showWrongDialog(kana: Kana) {
        val view = LayoutInflater.from(this).inflate(R.layout.wrong_answer_dialog, null)
        val dialog = BottomSheetDialog(this)

        val romaji = kanaConverter._hiraganaToRomaji(letterTextView.text.toString())
        val colorizedText = romaji.colorizeText(romaji, ContextCompat.getColor(applicationContext, R.color.lime))

        val correctString = SpannableStringBuilder()
            .append("The correct answer is: ")
            .bold { append(colorizedText) }

        view.findViewById<TextView>(R.id.correctAnswerTextView).text = correctString

        view.findViewById<TextView>(R.id.moreInfoTextView).setOnClickListener {
            val kanaInfo = KanaInfoView(this, kana)
            kanaInfo.setReviewToGone()
            kanaInfo.show()
        }
        view.findViewById<TextView>(R.id.moveOnButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            moveToNext(incorrect = true)
            userResponseEditText.setText("")
            responseImage.animate().alpha(0F).duration = 300
            newLevelLayout.animate().alpha(0F).duration = 300
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun learnKana(kana: Kana) {
        kana.hasLearned = true
        kana.level = 1
        val oneMinute = 1000 * 60
        val now = System.currentTimeMillis()
        val nextPracticeDate = now + oneMinute * kana.level!!
        kana.reviewTime = nextPracticeDate
        JWriterDatabase.getInstance(this).kanaDao().updateKana(kana)
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
        JWriterDatabase.getInstance(this).kanaDao().updateKana(kana)
    }

    private fun levelToTime(level: Int): Long {
        //For debugging
        val oneMinute = 1000 * 60L
        val millisecondsInHours = 1000L * 60 * 60
        val millisecondsInDays = millisecondsInHours * 24
//        return when(level) {
//            1 -> (millisecondsInHours * 73) // Level 1 is 8 hours after review
//            2 -> (millisecondsInDays * 1) // Level 2 is 1 day after review
//            3 -> (millisecondsInDays * 3) // Level 3 is 2 days after review
//            4 -> (millisecondsInDays * 7) // Level 4 is 7 days (1 week) after review
//            5 -> (millisecondsInDays * 14) // Level 5 is 14 day (2 weeks) after review
//            6 -> (millisecondsInDays * 30) // Level 6 is 30 days (1 month) after review
//            else -> 0
//        }
        //For debugging
        return when(level) {
            1 -> oneMinute * 1
            2 -> oneMinute * 2
            3 -> oneMinute * 3
            4 -> oneMinute * 4
            5 -> oneMinute * 5
            6 -> oneMinute * 6
            else -> 0
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

            if (quiz) {
                view.findViewById<TextView>(R.id.titleTextView).text = "End Quiz"
                view.findViewById<TextView>(R.id.quizExplainTextView).visibility = View.VISIBLE
            }

            view.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
                dialog.dismiss()
            }

            view.findViewById<TextView>(R.id.endReviewTextView).setOnClickListener {
                //If user gets kana incorrect, it will be added to incorrect review answers list
                //If user later gets it correct, it will be removed from the list, and the review time for incorrect kana is then calculated.
                //If user gets it incorrect, but backs out of the session before getting it correct,
                //the review time for all kana in the list will be calculated, so they cannot cheat the system
                for (kana in incorrectReviewAnswers) {
                    calculateNextReviewTime(kana, correct = false)
                }

                // If it's a quiz, just send user back to menu without learning the kana
                // If it's review, then complete what they have done
                if (quiz) {
                    startActivity(Intent(this, MenuActivity::class.java))
                } else if (review && !reviewOver) {
                    completedSet()
                }
                dialog.dismiss()
            }

            dialog.setView(view)
            dialog.show()
        }
    }

    //size of 45 (0-45) since there are 46 characters
    companion object KanaList {
        val hiraganaList = arrayOf(
            "あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ"
            , "さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と"
            , "な", "に", "ぬ", "ね", "の", "は", "ひ", "ふ", "へ", "ほ"
            , "ま", "み", "む", "め", "も", "や", "ゆ", "よ", "ら", "り"
            , "る", "れ", "ろ", "わ", "を", "ん")
        val katakanaList = arrayOf(
            "ア", "イ", "ウ", "エ", "オ", "カ", "キ", "ク", "ケ", "コ"
            , "サ", "シ", "ス", "セ", "ソ", "タ", "チ", "ツ", "テ", "ト"
            , "ナ", "ニ", "ヌ", "ネ", "ノ", "ハ", "ヒ", "フ", "ヘ", "ホ"
            , "マ", "ミ", "ム", "メ", "モ", "ヤ", "ユ", "ヨ", "ラ", "リ"
            , "ル", "レ", "ロ", "ワ", "ヲ", "ン")
    }

}