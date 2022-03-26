package com.example.jwriter

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity() {

    private lateinit var responseImage: ImageView
    private lateinit var letterTextView: TextView
    private lateinit var submitButton: Button
    private lateinit var userResponseEditText: EditText
    private lateinit var moveOnButton: Button
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var kanaConverter: KanaConverter
    private lateinit var kanaList: Array<String>
    private lateinit var layout: ConstraintLayout
    private lateinit var scoreText: TextView
    private lateinit var userScoreText: TextView
    private lateinit var restartButton: Button
    private lateinit var backToMenuButton: Button
    private lateinit var itemsWrongRecyclerView: RecyclerView
    private var indexKana = 44
    private var score = 0
    private val japaneseWrongAnswers = arrayListOf<String>()
    private val englishWrongAnswers = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        kanaList = hiraganaList
        kanaList.shuffle()

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

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        itemsWrongRecyclerView = findViewById(R.id.itemsWrongRecyclerView)
        itemsWrongRecyclerView.layoutManager = layoutManager

        for (answer in japaneseWrongAnswers) {
            englishWrongAnswers.add(kanaConverter._hiraganaToRomaji(answer))
        }

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
            moveToNext()
        }

        letterTextView.text = kanaList[indexKana]

    }

    /**
     * Moves on to next letter in list
     */
    private fun moveToNext() {

        indexKana++
        userResponseEditText.isEnabled = false

        if (moveOnButton.isVisible) {
            moveOnButton.visibility = View.INVISIBLE
            submitButton.visibility = View.VISIBLE
        }

        if (indexKana == hiraganaList.size) {
            completedSet()
            return
        }

        // Animate letter to the right, change text,
        // then set it back to the left and animate back right on screen
        // and then overshoot by a little, and recorrect
        letterTextView.animate().translationXBy((rootLayout.width).toFloat()).withEndAction {
            letterTextView.x = (-rootLayout.width).toFloat() / 2
            letterTextView.text = kanaList[indexKana]
            responseImage.animate().alpha(0F).duration = 500
            letterTextView.animate().translationXBy(rootLayout.width.toFloat() + letterTextView.width / 2).withEndAction {
                letterTextView.animate().translationXBy(-(letterTextView.width).toFloat()).duration = 500
                userResponseEditText.isEnabled = true
                userResponseEditText.requestFocus()
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(userResponseEditText, InputMethodManager.SHOW_IMPLICIT)
            }.duration = 1000
        }.duration = 1000
        userResponseEditText.setText("")
    }

    /**
     * Update the users stats on accuracy
     */
    private fun updateStats() {
        //Access SQL table and change appropiate stats
    }

    /**
     * Executes when user inputs incorrect answer
     */
    private fun incorrectAnswer() {

        englishWrongAnswers.add(userResponseEditText.text.toString())
        japaneseWrongAnswers.add(letterTextView.text.toString())

        itemsWrongRecyclerView.adapter?.notifyItemInserted(japaneseWrongAnswers.size-1)

        responseImage.visibility = View.VISIBLE
        //Set image as x, and start animation
        responseImage.alpha = 1F
        responseImage.setImageResource(R.drawable.animated_incorrect)
        (responseImage.drawable as Animatable).start()
        submitButton.visibility = View.INVISIBLE
        moveOnButton.visibility = View.VISIBLE
        userResponseEditText.isEnabled = false
        updateStats()
    }

    /**
     * Executes when user inputs correct answer
     */
    private fun correctAnswer() {
        responseImage.visibility = View.VISIBLE
        score++
        //Set image as checkmark, and start animation
        responseImage.alpha = 1F
        responseImage.setImageResource(R.drawable.animated_check)
        (responseImage.drawable as Animatable).start()
        updateStats()
        //when animation is done, move to next letter
        moveToNext()
    }

    private fun completedSet() {

        val user = JWriterDatabase.getInstance(this)?.userDao()?.getUsers()?.get(0)
        user?.totalAccuracy = user?.totalAccuracy?.plus(score)!!
        JWriterDatabase.getInstance(this)?.userDao()?.updateAccuracy(user)

        animateHelper(letterTextView, 100) {
            letterTextView.visibility = View.INVISIBLE
        }
        animateHelper(userResponseEditText, 200) {
            userResponseEditText.visibility = View.INVISIBLE
        }
        animateHelper(submitButton, 300) {
            submitButton.visibility = View.INVISIBLE
        }
        animateHelper(responseImage, 400) {
            responseImage.visibility = View.INVISIBLE

            layout.visibility = View.VISIBLE

            scoreText.x = (-layout.width).toFloat()
            scoreText.animate().translationXBy(layout.width.toFloat() * 1.5F - scoreText.width / 2).setStartDelay(200).duration = 1000
            userScoreText.text = "$score/${hiraganaList.size}"
            userScoreText.x = (layout.width).toFloat() * 2
            userScoreText.animate().translationXBy(-layout.width.toFloat() * 1.5F - userScoreText.width / 2).setStartDelay(400).duration = 1000
            itemsWrongRecyclerView.x = (-layout.width).toFloat()
            itemsWrongRecyclerView.animate().translationXBy(layout.width.toFloat() * 1.5F - restartButton.width).setStartDelay(600).duration = 1000
            restartButton.x = (layout.width).toFloat() * 2
            restartButton.animate().translationXBy(-layout.width.toFloat() * 1.5F - restartButton.width / 2).setStartDelay(800).duration = 1000
            backToMenuButton.x = (-layout.width).toFloat()
            backToMenuButton.animate().translationXBy(layout.width.toFloat() * 1.5F - backToMenuButton.width / 2).setStartDelay(1000).duration = 1000

            backToMenuButton.setOnClickListener {
                startActivity(Intent(this, MenuActivity::class.java))
            }
        }
    }

    private fun animateHelper(view: View, delay: Long, action: () -> Unit) {
        view.animate().translationYBy(-300F).withEndAction {
            view.animate().translationYBy(rootLayout.height.toFloat()).withEndAction {
                action()
            }.duration = 1000
        }.setStartDelay(delay).duration = 1000
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