package com.email.contact.kanasrs.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.media.MediaPlayer
import android.os.*
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.view.updateMargins
import com.email.contact.kanasrs.custom.DrawingView
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.util.KanaConverter
import com.email.contact.kanasrs.util.Utilities.Companion.colorizeText
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class LessonActivity : AppCompatActivity() {

    private lateinit var rootViewAnimator: ViewAnimator
    var animationTime = 0L
    private lateinit var kanaList: List<Kana>
    private lateinit var loadingBar: ProgressBar
    private lateinit var lessonTabLayout: TabLayout

    private lateinit var animationIn: Animation
    private lateinit var  animationOut: Animation
    private lateinit var  prevAnimIn: Animation
    private lateinit var  prevAnimOut: Animation

    private lateinit var rootView: RelativeLayout

    private var mPlayer = MediaPlayer()

    private val FIRST_KANA = 0
    private val KANA_LETTER_SCREEN = 0
    private val KANA_GIF_SCREEN = 1
    private val KANA_DRAW_SCREEN = 2

    private lateinit var buttonList: ArrayList<ImageButton>

    private var resetQuiz = false

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        rootViewAnimator = findViewById(R.id.rootViewAnimator)
        loadingBar = findViewById(R.id.lessonProgressBar)
        lessonTabLayout = findViewById(R.id.lessonTabLayout)

        animationIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        animationOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        prevAnimIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        prevAnimOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)

        rootView = findViewById(R.id.rootView)

        buttonList = ArrayList()

        lessonTabLayout.addOnTabSelectedListener( object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val tabPosition = tab?.position!!
                if (rootViewAnimator.displayedChild < tabPosition) { setNextAnim(rootViewAnimator) } else { setPrevAnim(rootViewAnimator) }
                rootViewAnimator.displayedChild = tabPosition
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })


        animationTime = animationIn.duration

        setNextAnim(rootViewAnimator)

        kanaList = KanaSRSDatabase.getInstance(this).kanaDao().getUnlearnedKana()
        val kanaConverter = KanaConverter(false)

        //Lesson number should generally be 10, 5, or 0.
        //However when the user reaches the end of the unlearned kana list, the number will become something else
        //So if the lesson number is >= 5, then just make the sublist 5 kana
        //Otherwise, for example the lessons to do is 3, since we check in the menu activity if the unlearned kana is > 5 or not,
        //then we can just make the sublist size equal to the number of lessons left
        val subList = if (KanaSRSDatabase.getInstance(this).userDao().getUser().lessonsNumber!! >= 5) {
            val tempSub = kanaList.subList(0, 5)
            checkExceptions(tempSub)
        } else {
            val tempSub = kanaList.subList(0, KanaSRSDatabase.getInstance(this).userDao().getUser().lessonsNumber!!)
            checkExceptions(tempSub)
        }

        Handler(Looper.getMainLooper()).post {

            for ((index, kana) in subList.withIndex()) {

                val newTab = lessonTabLayout.newTab()
                //Make touch do nothing so user can't skip through lesson
                newTab.view.setOnTouchListener { _, _ ->
                    true
                }
                lessonTabLayout.addTab(newTab)

                val newView = layoutInflater.inflate(R.layout.lesson_item, null)

                newView.findViewById<TextView>(R.id.kanaTextView).text = kana.letter
                newView.findViewById<TextView>(R.id.englishTextView).text =
                    kana.letter?.let { kanaConverter.hiraganaToRomaji(it) }

                newView.findViewById<ImageView>(R.id.kanaAudioImageView).setOnClickListener {
                    kana.letter?.let { it1 -> kanaConverter.hiraganaToRomaji(it1)?.let { it1 -> playAudio(it1) } }
                }

                val mnemonicText = newView.findViewById<TextView>(R.id.mnemonicTextView)

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

                newView.findViewById<ImageView>(R.id.addMnemonicImageView).setOnClickListener {
                    //Dialog to replace current mnemonic with edittext etc...
                    //Make sure to add button to reset to default
                    val view = layoutInflater.inflate(R.layout.mnemonic_dialog, null)
                    val dialog = BottomSheetDialog(this, R.style.BottomDialogTheme)

                    dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

                    val currentText = view.findViewById<TextView>(R.id.currentMnemonicTextView)
                    val editText = view.findViewById<EditText>(R.id.newMnemonicEditText)

                    editText.setOnTouchListener { v, event ->
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        when (event.action and MotionEvent.ACTION_MASK) {
                            MotionEvent.ACTION_UP ->
                                v.parent.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }

                    if (kana.customMnemonic != null) {
                        currentText.text = kana.customMnemonic
                    } else {
                        currentText.text = kana.mnemonic
                    }
                    currentText.movementMethod = ScrollingMovementMethod()
                    currentText.setOnTouchListener { v, event ->
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        when (event.action and MotionEvent.ACTION_MASK) {
                            MotionEvent.ACTION_UP ->
                                v.parent.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }

                    view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                        dialog.dismiss()
                    }
                    view.findViewById<Button>(R.id.confirmMnemonicButton).setOnClickListener {
                        when {
                            editText.text.length > 200 -> {
                                Toast.makeText(this, "Please keep the mnemonic under 200 characters.", Toast.LENGTH_SHORT).show()
                            }
                            editText.text.isBlank() -> {
                                Toast.makeText(this, "Please make sure you type something.", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                kana.customMnemonic = editText.text.toString()
                                mnemonicText.text = kana.customMnemonic
                                KanaSRSDatabase.getInstance(this).kanaDao().updateKana(kana)
                                dialog.dismiss()
                            }
                        }
                    }
                    view.findViewById<TextView>(R.id.defaultResetTextView).setOnClickListener {
                        kana.customMnemonic = null
                        mnemonicText.text = kana.mnemonic
                        KanaSRSDatabase.getInstance(this).kanaDao().updateKana(kana)
                        dialog.dismiss()
                    }

                    dialog.setContentView(view)
                    dialog.show()
                }

                val itemTabLayout = newView.findViewById<TabLayout>(R.id.itemTabLayout)

                val constraintLayout = newView.findViewById<ConstraintLayout>(R.id.itemConstraintLayout)
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(newView.findViewById<RelativeLayout>(R.id.topRelativeLayout).id, ConstraintSet.BOTTOM, itemTabLayout.id, ConstraintSet.TOP)
                constraintSet.applyTo(constraintLayout)

                //Add two tabs for letter and for gif
                val letterTab = itemTabLayout.newTab()
                val gifTab = itemTabLayout.newTab()
                val writeTab = itemTabLayout.newTab()

                itemTabLayout.addTab(letterTab)
                itemTabLayout.addTab(gifTab)
                itemTabLayout.addTab(writeTab)

                val tempViewAnimator = newView.findViewById<ViewAnimator>(R.id.viewAnimator)
                val nextButton = newView.findViewById<ImageButton>(R.id.nextItemButton)
                val previousButton = newView.findViewById<ImageButton>(R.id.previousItemButton)

                buttonList.add(nextButton)
                buttonList.add(previousButton)

                val relativeLayout = RelativeLayout(this)

                val kanaDrawingInfo = newView.findViewById<ConstraintLayout>(R.id.kanaDrawingInfo)

                setupDrawing(kanaDrawingInfo, kana, kanaConverter, relativeLayout)

                tempViewAnimator.addView(relativeLayout)

                setNextAnim(tempViewAnimator)

                val tempKanaWebStroke = newView.findViewById<WebView>(R.id.strokeWebView)
                tempKanaWebStroke.settings.javaScriptEnabled = true
                tempKanaWebStroke.webViewClient = WebViewClient()
                tempKanaWebStroke.loadUrl(kana.gif)

                itemTabLayout.addOnTabSelectedListener( object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        val tabPosition = tab?.position!!
                        if (tempViewAnimator.displayedChild < tabPosition) { setNextAnim(tempViewAnimator) } else { setPrevAnim(tempViewAnimator) }
                        //If lesson tab is current tab + 1, i.e. going to the next tab, then set animation of current tab to null so no UI glitch
                        //Or if quiz is resetting, set anim null to avoid UI glitch
                        if (lessonTabLayout.selectedTabPosition == newTab.position+1 || resetQuiz) {
                            setAnimNull(tempViewAnimator)
                            resetQuiz = false
                        }
                        tempViewAnimator.displayedChild = tabPosition
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }

                })

                nextButton.setOnSingleClickListener {
                    //If on the last item and gif screen, execute code to finish lesson
                    if (rootViewAnimator.displayedChild == subList.lastIndex && tempViewAnimator.displayedChild == KANA_DRAW_SCREEN) {
                        val view = layoutInflater.inflate(R.layout.lesson_completed_dialog, null)
                        val builder = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()
                        builder.setCancelable(false)
                        //If restart button is clicked, set tab to first item
                        //Setting reset quiz to true will remove animation from final item flipping back to letter, i.e. fix UI glitch
                        view.findViewById<MaterialButton>(R.id.restartLessonButton).setOnClickListener {
                            lessonTabLayout.getTabAt(0)?.select()
                            rootViewAnimator.postDelayed({
                                resetQuiz = true
                                itemTabLayout.selectTab(letterTab)
                            }, rootViewAnimator.inAnimation.duration)
                            //Set the right button back to blue and having an arrow
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_right_arrow)
                                nextButton.setBackgroundColor(resources.getColor(R.color.azure, theme))
                            }
                            builder.dismiss()
                        }
                        //If begin button is clicked, go to review intent, but send quiz as true and send learned kana sublist
                        view.findViewById<MaterialButton>(R.id.beginQuizButton).setOnClickListener {
                            val intent = Intent(this, ReviewActivity::class.java)
                            intent.putExtra("quiz", true)
                            intent.putExtra("kana", subList.toTypedArray())

                            // Check if lesson list ends with ん　or ン, this indicates the end of hiragana/katakana
                            if (subList.last().letter == "ん") {
                                intent.putExtra("finishHiragana", true)
                            }
                            if (subList.last().letter == "ン") {
                                intent.putExtra("finishKatakana", true)
                            }

                            startActivity(intent)
                            finish()
                        }
                        builder.show()
                        startConfetti(rootView)

                    } else if (tempViewAnimator.displayedChild == KANA_DRAW_SCREEN) {
                        //If on drawing view, show next kana and switch current kana back to letter tab
                        disableButtons()
                        lessonTabLayout.getTabAt(newTab.position+1)?.select()
                        rootViewAnimator.postDelayed({
                            newView.findViewById<ConstraintLayout>(R.id.infoLayout).visibility = View.VISIBLE
                            kanaDrawingInfo.visibility = View.INVISIBLE
                            itemTabLayout.selectTab(letterTab)
                            enableButtons()
                        }, rootViewAnimator.inAnimation.duration)
                    } else {
                        //In case of the else, it switches from letter tab to gif tab or draw tab
                            //With new draw tab, if the next tab is draw, switch icon
                        //However if on the last item, set the right arrow icon to a checkmark and set the background to lime,
                            // to indicate end of lesson

                        if (tempViewAnimator.displayedChild == KANA_GIF_SCREEN) {
                            newView.findViewById<ConstraintLayout>(R.id.infoLayout).visibility = View.INVISIBLE
                            kanaDrawingInfo.visibility = View.VISIBLE
                        }

                        if (rootViewAnimator.displayedChild == subList.lastIndex && (itemTabLayout.selectedTabPosition+1 == KANA_DRAW_SCREEN)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_checkmark)
                                nextButton.setBackgroundColor(resources.getColor(R.color.lime, theme))
                            }
                        }
                        itemTabLayout.getTabAt(itemTabLayout.selectedTabPosition+1)?.select()
                    }
                }

                previousButton.setOnSingleClickListener {
                    //If on the first kana and also on the letter tab, then don't allow for previous button to do anything
                    if (rootViewAnimator.displayedChild == FIRST_KANA && tempViewAnimator.displayedChild == KANA_LETTER_SCREEN) {
                        return@setOnSingleClickListener
                    }
                    //If on the gif screen, then go back to the letter tab. Else, it means they are on letter screen, so go to the previous kana
                    if (tempViewAnimator.displayedChild != KANA_LETTER_SCREEN) {

                        if (tempViewAnimator.displayedChild == KANA_DRAW_SCREEN) {
                            newView.findViewById<ConstraintLayout>(R.id.infoLayout).visibility = View.VISIBLE
                            kanaDrawingInfo.visibility = View.INVISIBLE
                        }

                        if (rootViewAnimator.displayedChild == subList.lastIndex) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_right_arrow)
                                nextButton.setBackgroundColor(resources.getColor(R.color.azure, theme))
                            }
                        }
                        itemTabLayout.getTabAt(itemTabLayout.selectedTabPosition-1)?.select()
                    } else {
                        lessonTabLayout.getTabAt(newTab.position-1)?.select()
                    }
                }

                //Add view of kana item
                rootViewAnimator.addView(newView)

                //If last index in the kana sublist, remove the loading spinner
                if (index == subList.lastIndex) {
                    lessonTabLayout.visibility = View.VISIBLE
                    loadingBar.visibility = View.INVISIBLE
                    rootViewAnimator.removeView(loadingBar)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrawing(kanaDrawingInfo: ConstraintLayout, kana: Kana, kanaConverter: KanaConverter, relativeLayout: RelativeLayout) {

        relativeLayout.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)

        val traceId = if (kana.isHiragana) {
            resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter!!)}_trace", "drawable", packageName)
        } else {
            resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter!!)}_trace_k", "drawable", packageName)
        }

        val strokeId = if (kana.isHiragana) {
            resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter)}_stroke", "drawable", packageName)
        } else {
            resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter)}_stroke_k", "drawable", packageName)
        }

        val drawingView = DrawingView(this)
        drawingView.id = View.generateViewId()
        drawingView.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            addRule(RelativeLayout.ALIGN_PARENT_START)
        }

        drawingView.background = ContextCompat.getDrawable(this, traceId)

        drawingView.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP ->
                    v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        kanaDrawingInfo.findViewById<ImageView>(R.id.strokeImageView).setImageResource(strokeId)

        kanaDrawingInfo.findViewById<Button>(R.id.kanaClearButton).setOnClickListener {
            drawingView.clearDrawing()
        }

        val responseImageView = kanaDrawingInfo.findViewById<ImageView>(R.id.kanaResponseImage)
        val progressBar = kanaDrawingInfo.findViewById<ProgressBar>(R.id.kanaProgressBar)

        kanaDrawingInfo.findViewById<Button>(R.id.checkButton).setOnClickListener {
            if (!drawingView.checkIfEmpty()) {
                progressBar.visibility = View.VISIBLE
                drawingView.disableDrawing()
                GlobalScope.launch {
                    if (drawingView.isDrawingCorrect(kana.letter, progressBar)) {
                        runOnUiThread {
                            responseImageView.alpha = 1f
                            responseImageView.setImageResource(R.drawable.ic_checkmark)
                            responseImageView.imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    this@LessonActivity,
                                    R.color.lime
                                )
                            )
                            responseImageView.visibility = View.VISIBLE
                            drawingView.clearDrawing()
                            responseImageView.animate().alpha(0f).setStartDelay(1000L)
                                .withEndAction {
                                    drawingView.enableDrawing()
                                }.duration = 300
                        }
                    } else {
                        runOnUiThread {
                            responseImageView.alpha = 1f
                            responseImageView.setImageResource(R.drawable.ic_x)
                            responseImageView.imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    this@LessonActivity,
                                    R.color.wrong_answer
                                )
                            )
                            responseImageView.visibility = View.VISIBLE
                            drawingView.clearDrawing()
                            responseImageView.animate().alpha(0f).setStartDelay(1000L)
                                .withEndAction {
                                    drawingView.enableDrawing()
                                }.duration = 300
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show()
            }
        }

        relativeLayout.addView(drawingView)
    }

    private fun checkExceptions(list: List<Kana>): List<Kana> {
        //Check if the starting letter is や、わ、ヤ、ワ since ya, yu yo only have three, and wa,wo,n is three as well
        //In other words, keep letters in correct sets
        return when (list[0].letter) {
            "や" -> {
                list.subList(0, 3)
            }
            "わ" -> {
                list.subList(0, 3)
            }
            "ヤ" -> {
                list.subList(0, 3)
            }
            "ワ" -> {
                list.subList(0, 3)
            }
            else -> {list}
        }
    }

    private fun playAudio(letter: String) {
        if (!mPlayer.isPlaying) {
            mPlayer = MediaPlayer.create(this, resources.getIdentifier(letter, "raw", packageName))
            mPlayer.start()
        }
    }

    private fun startConfetti(container: ViewGroup) {
        CommonConfetti.rainingConfetti(
            container, intArrayOf(
                Color.BLACK, Color.BLUE, Color.CYAN, Color.YELLOW, Color.RED, Color.GREEN
            )
        ).oneShot().setVelocityY(600F, 100F).animate()
    }

    private fun disableButtons() {
        for (button in buttonList) {
            button.isEnabled = false
        }
    }

    private fun enableButtons() {
        for (button in buttonList) {
            button.isEnabled = true
        }
    }

    private fun setAnimNull(viewAnimator: ViewAnimator) {
        viewAnimator.inAnimation = null
        viewAnimator.outAnimation = null
    }

    private fun setNextAnim(viewAnimator: ViewAnimator) {
        viewAnimator.inAnimation = animationIn
        viewAnimator.outAnimation = animationOut
    }

    private fun setPrevAnim(viewAnimator: ViewAnimator) {
        viewAnimator.inAnimation = prevAnimIn
        viewAnimator.outAnimation = prevAnimOut
    }
}

class OnSingleClickListener(private val block: () -> Unit) : View.OnClickListener {

    private var lastClickTime = 0L

    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < 500) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()
        block()
    }
}

fun View.setOnSingleClickListener(block: () -> Unit) {
    setOnClickListener(OnSingleClickListener(block))
}