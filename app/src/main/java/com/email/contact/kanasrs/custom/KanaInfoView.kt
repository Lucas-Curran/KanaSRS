package com.email.contact.kanasrs.custom

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.view.updateMargins
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.activity.setOnSingleClickListener
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.util.KanaConverter
import com.email.contact.kanasrs.util.Utilities
import com.email.contact.kanasrs.util.Utilities.Companion.getLevelColor
import com.email.contact.kanasrs.util.Utilities.Companion.levelToTitle
import com.email.contact.kanasrs.util.Utilities.Companion.mediaPlayer
import com.email.contact.kanasrs.util.Utilities.Companion.setNextAnim
import com.email.contact.kanasrs.util.Utilities.Companion.setPrevAnim
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.NumberFormat


@SuppressLint("ClickableViewAccessibility")
class KanaInfoView(val context: Context, val kana: Kana, private val showReviewTime: Boolean, private val writingInfo: Boolean) {

    private lateinit var animationIn: Animation
    private lateinit var animationOut: Animation
    private lateinit var prevAnimIn: Animation
    private lateinit var prevAnimOut: Animation
    private var extraStatsImage: ImageView
    private var dialog: BottomSheetDialog
    private var topTextView: TextView

    init {

        val view = LayoutInflater.from(context).inflate(R.layout.kana_info_dialog, null)

        extraStatsImage = view.findViewById(R.id.extraStatsImageView)

        dialog = BottomSheetDialog(context, R.style.BottomDialogTheme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        dialog.dismissWithAnimation = true

        topTextView = view.findViewById(R.id.topTextView)

        Handler(Looper.getMainLooper()).post {

            if (showReviewTime) {
                if (kana.reviewTime != null) {
                    if (kana.reviewTime!! > System.currentTimeMillis()) {
                        val timeUntilReview = kana.reviewTime!! - System.currentTimeMillis()
                        object : CountDownTimer(timeUntilReview, 1000) {
                            override fun onTick(millisLeft: Long) {
                                topTextView.text = "Review -> ${Utilities.formatTime(millisLeft)}"
                            }

                            override fun onFinish() {
                                topTextView.text = "Review -> now"
                            }
                        }.start()
                    } else {
                        topTextView.text = "Review -> now"
                    }
                } else {
                    if (!kana.hasLearned) {
                        topTextView.text = "Need to learn"
                    } else {
                        topTextView.text = "Already mastered!"
                    }
                }
            } else {
                if (writingInfo) {
                    setTextToWritingLevel()
                } else {
                    setTextToLevel()
                }
            }

            val kanaConverter = KanaConverter(false)

            view.findViewById<TextView>(R.id.kanaEnglishTextView).text = kana.letter?.let {
                kanaConverter.hiraganaToRomaji(
                    it
                )
            }
            view.findViewById<TextView>(R.id.kanaTextView).text = kana.letter

            val webStroke = view.findViewById<WebView>(R.id.strokeWebView)
            webStroke.settings.javaScriptEnabled = true
            webStroke.webViewClient = WebViewClient()
            webStroke.loadUrl(kana.gif)

            val mnemonicText = view.findViewById<TextView>(R.id.mnemonicTextView)
            if (kana.customMnemonic != null) {
                mnemonicText.text = kana.customMnemonic
            } else {
                mnemonicText.text = kana.mnemonic
            }

            view.findViewById<ImageView>(R.id.addMnemonicImageView).setOnClickListener {
                //Dialog to replace current mnemonic with edittext etc...
                //Make sure to add button to reset to default
                val mnemonicView =
                    LayoutInflater.from(context).inflate(R.layout.mnemonic_dialog, null)
                val dialog = BottomSheetDialog(context, R.style.BottomDialogTheme)

                dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

                val currentText = mnemonicView.findViewById<TextView>(R.id.currentMnemonicTextView)
                val editText = mnemonicView.findViewById<EditText>(R.id.newMnemonicEditText)

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

                mnemonicView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                    dialog.dismiss()
                }
                mnemonicView.findViewById<Button>(R.id.confirmMnemonicButton).setOnClickListener {
                    when {
                        editText.text.length > 200 -> {
                            Toast.makeText(
                                context,
                                "Please keep the mnemonic under 200 characters.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        editText.text.isBlank() -> {
                            Toast.makeText(
                                context,
                                "Please make sure you type something.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            kana.customMnemonic = editText.text.toString()
                            mnemonicText.text = kana.customMnemonic
                            KanaSRSDatabase.getInstance(context).kanaDao().updateKana(kana)
                            dialog.dismiss()
                        }
                    }
                }
                mnemonicView.findViewById<TextView>(R.id.defaultResetTextView).setOnClickListener {
                    kana.customMnemonic = null
                    mnemonicText.text = kana.mnemonic
                    KanaSRSDatabase.getInstance(context).kanaDao().updateKana(kana)
                    dialog.dismiss()
                }

                dialog.setContentView(mnemonicView)
                dialog.show()
            }

            extraStatsImage.setOnClickListener {
                showExtraStats()
            }

            val tabLayout = view.findViewById<TabLayout>(R.id.itemTabLayout)
            val viewAnimator = view.findViewById<ViewAnimator>(R.id.viewAnimator)

            val relativeLayout = RelativeLayout(context)

            setupDrawing(kana, kanaConverter, relativeLayout)

            viewAnimator.addView(relativeLayout)

            animationIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
            animationOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_left)
            prevAnimIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_left)
            prevAnimOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)

            val letterTab = tabLayout.newTab()
            val gifTab = tabLayout.newTab()
            val writeTab = tabLayout.newTab()

            tabLayout.addTab(letterTab)
            tabLayout.addTab(gifTab)
            tabLayout.addTab(writeTab)

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val tabPosition = tab?.position!!
                    viewAnimator.displayedChild = tabPosition
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }

            })

            val nextButton = view.findViewById<ImageButton>(R.id.nextItemButton)
            val previousButton = view.findViewById<ImageButton>(R.id.previousItemButton)

            view.findViewById<ImageView>(R.id.kanaAudioImageView).setOnClickListener {
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer = MediaPlayer.create(
                        context,
                        context.resources.getIdentifier(kana.letter?.let { it1 ->
                            kanaConverter.hiraganaToRomaji(
                                it1
                            )
                        }, "raw", context.packageName)
                    )
                    mediaPlayer.start()
                }
            }

            nextButton.setOnSingleClickListener {

                if (tabLayout.selectedTabPosition == 2) {
                    dialog.dismiss()
                    return@setOnSingleClickListener
                }

                setNextAnim(viewAnimator, animationIn, animationOut)
                tabLayout.getTabAt(tabLayout.selectedTabPosition + 1)?.select()
                if (tabLayout.selectedTabPosition == 2) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        nextButton.setImageResource(R.drawable.ic_checkmark)
                        nextButton.setBackgroundColor(
                            context.resources.getColor(
                                R.color.lime,
                                context.theme
                            )
                        )
                    }
                }
            }

            previousButton.setOnSingleClickListener {
                setPrevAnim(viewAnimator, prevAnimIn, prevAnimOut)
                tabLayout.getTabAt(tabLayout.selectedTabPosition - 1)?.select()
                if (tabLayout.selectedTabPosition == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    nextButton.setImageResource(R.drawable.ic_right_arrow)
                    nextButton.setBackgroundColor(
                        context.resources.getColor(
                            R.color.azure,
                            context.theme
                        )
                    )
                }
            }

            dialog.setContentView(view)
        }
    }

    private fun showExtraStats() {
        val view = LayoutInflater.from(context).inflate(R.layout.kana_stats_dialog, null)
        val dialog = BottomSheetDialog(context, R.style.BottomDialogTheme)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val modeTabs = view.findViewById<TabLayout>(R.id.modeTabs)

        for (i in 0 until modeTabs.tabCount) {
            val tab = (modeTabs.getChildAt(0) as ViewGroup).getChildAt(i)
            val p = tab.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(15, 0, 15, 0)
            tab.requestLayout()
        }

        statsHelper(view, false)

        modeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> statsHelper(view, false)
                    1 -> statsHelper(view, true)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        view.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun statsHelper(view: View, isWriting: Boolean) {

        val kanaLevel = if (isWriting) {
            kana.writingLevel!!
        } else {
            kana.level
        }

        val reviewTime = if (isWriting) {
            kana.writingReviewTime
        } else {
            kana.reviewTime
        }

        val totalAnswered = if (isWriting) {
            kana.writingTotalAnswered ?: 0
        } else {
            kana.totalAnswered ?: 0
        }

        val totalCorrect = if (isWriting) {
            kana.writingTotalCorrect ?: 0
        } else {
            kana.totalCorrect ?: 0
        }

        val streak = if (isWriting) {
            kana.writingStreak
        } else {
            kana.streak
        }

        val kanaLetter = view.findViewById<TextView>(R.id.kanaLetterTextView)
        kanaLetter.text = kana.letter
        kanaLetter.background.setTint(ContextCompat.getColor(context, getLevelColor(kanaLevel!!)))
        val progressBar = view.findViewById<ProgressBar>(R.id.accuracyProgressBar)
        val progressText = view.findViewById<TextView>(R.id.accuracyText)
        val correctText = view.findViewById<TextView>(R.id.accuracyCorrect)
        val totalText = view.findViewById<TextView>(R.id.accuracyTotal)
        val accuracyImage = view.findViewById<ImageView>(R.id.accuracyImageView)

        val progressBackgroundId = context.resources.getIdentifier("${levelToTitle(kanaLevel).lowercase()}_gradient", "drawable", context.packageName)
        progressBar.progressDrawable = ContextCompat.getDrawable(context, progressBackgroundId)

        var percent = 0.0

        progressBar.max = totalAnswered
        progressBar.progress = totalCorrect

        if (totalAnswered != 0) {
            percent = totalCorrect / totalAnswered.toDouble()
        }

        val percentageFormat = NumberFormat.getPercentInstance()
        percentageFormat.minimumFractionDigits = 2
        progressText.text = percentageFormat.format(percent)

        correctText.text = totalCorrect.toString()
        totalText.text = totalAnswered.toString()

        when {
            percent * 100 >= 70 -> {
                accuracyImage.setImageResource(R.drawable.ic_positive_face)
            }
            percent * 100 >= 40 -> {
                accuracyImage.setImageResource(R.drawable.ic_neutral_face)
            }
            percent * 100 >= 0 -> {
                accuracyImage.setImageResource(R.drawable.ic_negative_face)
            }
        }

        if (totalAnswered == 0) {
            progressBar.max = 0
            progressBar.progress = 0
            progressText.text = "---"
            correctText.text = "0"
            totalText.text = "0"
            accuracyImage.setImageResource(android.R.color.transparent)
        }

        view.findViewById<TextView>(R.id.streakTextView).text = streak.toString()

        val levelTextView = view.findViewById<TextView>(R.id.levelTextView)
        levelTextView.setTextColor(ContextCompat.getColor(context, getLevelColor(kanaLevel)))
        levelTextView.text = levelToTitle(kanaLevel)

        val nextReview = view.findViewById<TextView>(R.id.reviewTextView)
        if (reviewTime != null) {
            if (reviewTime > System.currentTimeMillis()) {
                val timeUntilReview = reviewTime - System.currentTimeMillis()
                nextReview.text = "${Utilities.formatTime(timeUntilReview)}"
            } else {
                nextReview.text = "Now"
            }
        } else {
            if (!kana.hasLearned) {
                nextReview.text = "Need to learn"
            } else {
                nextReview.setTextColor(ContextCompat.getColor(context, R.color.sensei_gold))
                nextReview.text = "Already mastered!"
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrawing(kana: Kana, kanaConverter: KanaConverter, relativeLayout: RelativeLayout) {
        relativeLayout.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)

        val traceId = if (kana.isHiragana) {
            context.resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter!!)}_trace", "drawable", context.packageName)
        } else {
            context.resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter!!)}_trace_k", "drawable", context.packageName)
        }

        val strokeId = if (kana.isHiragana) {
            context.resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter)}_stroke", "drawable", context.packageName)
        } else {
            context.resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter)}_stroke_k", "drawable", context.packageName)
        }

        val drawingView = DrawingView(context)
        drawingView.setStrokeWidth(14f)
        drawingView.id = View.generateViewId()
        drawingView.layoutParams = RelativeLayout.LayoutParams(300, 300).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
            addRule(RelativeLayout.ALIGN_PARENT_START)
        }
        drawingView.background = ContextCompat.getDrawable(context, traceId)

        drawingView.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP ->
                    v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        val strokeImageView =  ImageView(context)
        strokeImageView.setImageResource(strokeId)

        val responseImageView = ImageView(context)
        responseImageView.setImageResource(R.drawable.ic_checkmark)
        responseImageView.elevation = 10f
        responseImageView.visibility = View.INVISIBLE

        val clearButton = Button(context)
        clearButton.id = View.generateViewId()
        clearButton.text = "Clear"
        clearButton.textSize = 12f
        clearButton.setPadding(10)
        clearButton.background = ContextCompat.getDrawable(context, R.drawable.dialog_background)

        val checkButton = Button(context)
        checkButton.id = View.generateViewId()
        checkButton.text = "Check writing"
        checkButton.textSize = 12f
        checkButton.setPadding(10)
        checkButton.background = ContextCompat.getDrawable(context, R.drawable.dialog_background)

        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleSmall)
        progressBar.isIndeterminate = true
        progressBar.visibility = View.INVISIBLE
        progressBar.elevation = 10f

        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            clearButton.setTextColor(context.resources.getColor(R.color.white, context.theme))
            checkButton.setTextColor(context.resources.getColor(R.color.white, context.theme))
            clearButton.foreground = ContextCompat.getDrawable(context, outValue.resourceId)
            checkButton.foreground = ContextCompat.getDrawable(context, outValue.resourceId)
        }

        strokeImageView.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.BELOW, drawingView.id)
        }

        clearButton.layoutParams =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.RIGHT_OF, drawingView.id)
            }

        checkButton.layoutParams =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.RIGHT_OF, drawingView.id)
                addRule(RelativeLayout.ALIGN_BOTTOM, drawingView.id)
                addRule(RelativeLayout.BELOW, clearButton.id)
            }

        progressBar.layoutParams = RelativeLayout.LayoutParams(50, 50).apply {
            addRule(RelativeLayout.ALIGN_BOTTOM, checkButton.id)
            addRule(RelativeLayout.ALIGN_START, checkButton.id)
            setMargins(10, 0, 0, 10)
        }

        responseImageView.layoutParams = RelativeLayout.LayoutParams(50, 50).apply {
            addRule(RelativeLayout.ALIGN_BOTTOM, checkButton.id)
            addRule(RelativeLayout.ALIGN_START, checkButton.id)
            setMargins(10, 0, 0, 10)
        }

        clearButton.setOnClickListener {
            drawingView.clearDrawing()
        }

        checkButton.setOnClickListener {
            if (!drawingView.checkIfEmpty()) {
                progressBar.visibility = View.VISIBLE
                drawingView.disableDrawing()
                GlobalScope.launch {
                    if (drawingView.isDrawingCorrect(kana.letter, progressBar)) {
                        (context as Activity).runOnUiThread {
                            responseImageView.alpha = 1f
                            responseImageView.setImageResource(R.drawable.ic_checkmark)
                            responseImageView.imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
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
                        (context as Activity).runOnUiThread {
                            responseImageView.alpha = 1f
                            responseImageView.setImageResource(R.drawable.ic_x)
                            responseImageView.imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
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
                Toast.makeText(context, "Please write something", Toast.LENGTH_SHORT).show()
            }
        }

        relativeLayout.addView(progressBar)
        relativeLayout.addView(responseImageView)
        relativeLayout.addView(strokeImageView)
        relativeLayout.addView(checkButton)
        relativeLayout.addView(clearButton)
        relativeLayout.addView(drawingView)
    }

    fun hideExtraStats() {
        extraStatsImage.visibility = View.GONE
    }

    fun show() {
        dialog.show()
    }

    fun setTextToLevel() {
        val levelText = when (kana.level) {
            1, 2 -> "Rookie"
            3 -> "Amateur"
            4 -> "Expert"
            5 -> "Master"
            6 -> "Sensei"
            else -> ""
        }
        topTextView.text = levelText
        topTextView.setTextColor(ContextCompat.getColor(context, getLevelColor(kana.level!!)))
    }

    fun setTextToWritingLevel() {
        val levelText = when (kana.writingLevel) {
            1, 2 -> "Rookie"
            3 -> "Amateur"
            4 -> "Expert"
            5 -> "Master"
            6 -> "Sensei"
            else -> ""
        }
        topTextView.text = levelText
        topTextView.setTextColor(ContextCompat.getColor(context, getLevelColor(kana.writingLevel!!)))
    }

    fun setReviewToGone() {
        topTextView.visibility = View.GONE
    }

}