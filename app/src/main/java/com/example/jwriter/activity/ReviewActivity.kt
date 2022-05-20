package com.example.jwriter.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jwriter.*
import com.example.jwriter.database.JWriterDatabase
import com.example.jwriter.database.Kana
import com.example.jwriter.util.AnimUtilities
import com.example.jwriter.util.AnimUtilities.Companion.colorizeText
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

            if (userResponseEditText.text.toString() == kanaConverter._hiraganaToRomaji(letterTextView.text.toString())) {
                correctAnswer()
            } else {
                incorrectAnswer()
            }
        }

        userResponseEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if ((keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (i == EditorInfo.IME_ACTION_DONE)) {
                submitButton.performClick()
                true
            } else {
                false
            }
        }

        reviewProgressBar.max = kanaList.size

        letterTextView.text = kanaList[0].letter
    }

    /**
     * Moves on to next letter in list
     */
    private fun moveToNext(incorrect: Boolean) {

        totalAnswered++
        submitButton.isEnabled = false
        userResponseEditText.isEnabled = false

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
                userResponseEditText.isEnabled = true
                submitButton.isEnabled = true
                userResponseEditText.requestFocus()
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(userResponseEditText, InputMethodManager.SHOW_IMPLICIT)
            }.duration = 850
        }.duration = 850
    }

    /**
     * Executes when user inputs incorrect answer
     */
    private fun incorrectAnswer() {

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

        this.currentFocus?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
        }

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
        AnimUtilities.animateEnd(letterTextView, rootLayout, 300) {
            letterTextView.visibility = View.INVISIBLE
        }
        AnimUtilities.animateEnd(userResponseEditText, rootLayout, 400) {
            userResponseEditText.visibility = View.INVISIBLE
        }
        AnimUtilities.animateEnd(submitButton, rootLayout, 500) {
            submitButton.visibility = View.INVISIBLE
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
        AnimUtilities.animateEnd(newLevelLayout, rootLayout, 200) {
            newLevelLayout.visibility = View.INVISIBLE
        }

        //After last view is off screen, begin animating end screen results to screen
        AnimUtilities.animateEnd(responseImage, rootLayout, 100) {
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

    override fun onBackPressed() {
        super.onBackPressed()
        //TODO: Dialog pops up asking if they are sure they want to leave mid-review
        // If user leaves, calculate next review time for all items in the wrong answers array
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