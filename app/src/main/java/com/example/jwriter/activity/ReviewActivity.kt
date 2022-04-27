package com.example.jwriter.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jwriter.*
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
    private lateinit var moveOnButton: Button
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var kanaConverter: KanaConverter
    private lateinit var kanaList: MutableList<Kana>
    private lateinit var layout: ConstraintLayout
    private lateinit var scoreText: TextView
    private lateinit var userScoreText: TextView
    private lateinit var restartButton: Button
    private lateinit var backToMenuButton: Button
    private lateinit var itemsWrongRecyclerView: RecyclerView
    private lateinit var correctAnswerTextView: TextView
    private lateinit var emptyRecyclerViewText: TextView

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
        correctAnswerTextView = findViewById(R.id.correctAnswerTextView)
        emptyRecyclerViewText = findViewById(R.id.empty_view)

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

            if (userResponseEditText.text.toString() == kanaConverter._hiraganaToRomaji(letterTextView.text.toString())) {
                correctAnswer()
            } else {
                incorrectAnswer()
            }
        }

        userResponseEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (i == EditorInfo.IME_ACTION_DONE)) {
                submitButton.performClick()
            }
            false
        }

        moveOnButton = findViewById(R.id.moveOnButton)
        moveOnButton.setOnClickListener {
            correctAnswerTextView.visibility = View.INVISIBLE
            moveToNext()
        }

        letterTextView.text = kanaList[0].letter

    }

    /**
     * Moves on to next letter in list
     */
    private fun moveToNext() {

        totalAnswered++
        submitButton.isEnabled = false
        userResponseEditText.isEnabled = false

        if (moveOnButton.isVisible) {
            moveOnButton.visibility = View.INVISIBLE
            submitButton.visibility = View.VISIBLE
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
            responseImage.animate().alpha(0F).duration = 500
            letterTextView.animate().translationXBy(rootLayout.width.toFloat() + letterTextView.width / 2).withEndAction {
                letterTextView.animate().translationXBy(-(letterTextView.width).toFloat()).duration = 500
                userResponseEditText.isEnabled = true
                submitButton.isEnabled = true
                userResponseEditText.requestFocus()
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(userResponseEditText, InputMethodManager.SHOW_IMPLICIT)
            }.duration = 1000
        }.duration = 1000
        userResponseEditText.setText("")
    }

    /**
     * Executes when user inputs incorrect answer
     */
    private fun incorrectAnswer() {

        if (review) {
            val kana = kanaList[0]

            if (!incorrectReviewAnswers.contains(kana)) {
                incorrectReviewAnswers.add(kana)
                calculateNextReviewTime(kana = kana, correct = false)
            }

            if (kanaList.size > 1) {
                kanaList.remove(kana)
                val newKanaPosition = Random.nextInt(1, kanaList.size-1)
                kanaList.add(newKanaPosition, kana)
            }
        }

        if (quiz) {
            val kana = kanaList[0]
            if (kanaList.size > 1) {
                kanaList.remove(kana)
                val newKanaPosition = Random.nextInt(1, kanaList.size-1)
                kanaList.add(newKanaPosition, kana)
            }
        }

        englishWrongAnswers.add(userResponseEditText.text.toString())
        japaneseWrongAnswers.add(letterTextView.text.toString())

        itemsWrongRecyclerView.adapter?.notifyItemInserted(japaneseWrongAnswers.size-1)

        responseImage.visibility = View.VISIBLE

        val correctString = SpannableStringBuilder()
            .append("The correct answer is ")
            .bold { append(kanaConverter._hiraganaToRomaji(letterTextView.text.toString())) }

        correctAnswerTextView.text = correctString
        correctAnswerTextView.visibility = View.VISIBLE

        //Set image as x, and start animation
        responseImage.alpha = 1F
        responseImage.setImageResource(R.drawable.animated_incorrect)
        (responseImage.drawable as Animatable).start()
        submitButton.visibility = View.INVISIBLE
        moveOnButton.visibility = View.VISIBLE
        userResponseEditText.isEnabled = false
    }

    /**
     * Executes when user inputs correct answer
     */
    private fun correctAnswer() {

        if (review) {
            val kana = kanaList[0]
            calculateNextReviewTime(kana = kana, correct = true)
            kanaList.remove(kana)
        }

        if (quiz) {
            val kana = kanaList[0]
            kanaList.remove(kana)
            correctQuizAnswers.add(kana)
        }

        responseImage.visibility = View.VISIBLE
        score++
        //Set image as checkmark, and start animation
        responseImage.alpha = 1F
        responseImage.setImageResource(R.drawable.animated_check)
        (responseImage.drawable as Animatable).start()
        //when animation is done, move to next letter
        moveToNext()
    }

    /**
     * Function called when the user finishes the set of kana
     */
    private fun completedSet() {

        //Since there is only one user in the database, get index 0 of users
        val user = JWriterDatabase.getInstance(this).userDao().getUser()
        user.totalAccuracy = user.totalAccuracy.plus(score)
        JWriterDatabase.getInstance(this).userDao().updateUser(user)

        if (quiz) {
            for (kana in correctQuizAnswers) {
                learnKana(kana)
            }
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
        AnimUtilities.animateEnd(letterTextView, rootLayout, 100) {
            letterTextView.visibility = View.INVISIBLE
        }
        AnimUtilities.animateEnd(userResponseEditText, rootLayout, 200) {
            userResponseEditText.visibility = View.INVISIBLE
        }
        AnimUtilities.animateEnd(submitButton, rootLayout, 300) {
            submitButton.visibility = View.INVISIBLE
        }

        //After last view is off screen, begin animating end screen results to screen
        AnimUtilities.animateEnd(responseImage, rootLayout, 400) {
            responseImage.visibility = View.INVISIBLE
            layout.visibility = View.VISIBLE
            userScoreText.text = "$score/$totalAnswered"

            AnimUtilities.animateFromLeft(scoreText, layout, startDelay = 200)
            AnimUtilities.animateFromRight(userScoreText, layout, startDelay = 400)
            if (itemsWrongRecyclerView.isVisible) AnimUtilities.animateFromLeft(
                itemsWrongRecyclerView,
                layout,
                startDelay = 600
            )
            else AnimUtilities.animateFromLeft(emptyRecyclerViewText, layout, startDelay = 600)
            AnimUtilities.animateFromRight(restartButton, layout, startDelay = 800)
            AnimUtilities.animateFromLeft(backToMenuButton, layout, startDelay = 1000)

            backToMenuButton.setOnClickListener {
                startActivity(Intent(this, MenuActivity::class.java))
            }

        }
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
        val millisecondsInDay = 60 * 60 * 24 * 1000
        val now = System.currentTimeMillis()
        val nextPracticeDate = now + levelToTime(kana.level!!)
        kana.reviewTime = nextPracticeDate
        JWriterDatabase.getInstance(this).kanaDao().updateKana(kana)
    }

    private fun levelToTime(level: Int): Long {
        //For debugging
        val oneMinute = 1000 * 60L
        val millisecondsInHours = 1000L * 60 * 60
        val millisecondsInDays = millisecondsInHours * 24
//        return when(level) {
//            1 -> (millisecondsInHours * 8) // Level 1 is 8 hours after review
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