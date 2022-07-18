package com.email.contact.kanasrs.custom

import android.annotation.SuppressLint
import android.content.Context
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
import java.text.NumberFormat


@SuppressLint("ClickableViewAccessibility")
class KanaInfoView(val context: Context, val kana: Kana, val showReviewTime: Boolean) {

    private lateinit var animationIn: Animation
    private lateinit var animationOut: Animation
    private lateinit var prevAnimIn: Animation
    private lateinit var prevAnimOut: Animation
    private var dialog: BottomSheetDialog
    private var topTextView: TextView

    init {

        val view = LayoutInflater.from(context).inflate(R.layout.kana_info_dialog, null)

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
                setTextToLevel()
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

            view.findViewById<ImageView>(R.id.extraStatsImageView).setOnClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.kana_stats_dialog, null)
                val dialog = BottomSheetDialog(context, R.style.BottomDialogTheme)
                dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

                /*
                    - Kana header
                    - Accuracy progress bar and fraction
                    - Streak
                    - Level and writing level
                    - Next review
                 */

                val kanaLetter = view.findViewById<TextView>(R.id.kanaLetterTextView)
                kanaLetter.text = kana.letter
                // TODO: Causes error when clicking stat view on lesson wrong
                kanaLetter.background.setTint(ContextCompat.getColor(context, getLevelColor(kana.level!!)))
                val progressBar = view.findViewById<ProgressBar>(R.id.accuracyProgressBar)
                val progressText = view.findViewById<TextView>(R.id.accuracyText)
                val correctText = view.findViewById<TextView>(R.id.accuracyCorrect)
                val totalText = view.findViewById<TextView>(R.id.accuracyTotal)
                val accuracyImage = view.findViewById<ImageView>(R.id.accuracyImageView)

                val progressBackgroundId = context.resources.getIdentifier("${levelToTitle(kana.level!!).lowercase()}_gradient", "drawable", context.packageName)
                progressBar.progressDrawable = ContextCompat.getDrawable(context, progressBackgroundId)

                val totalAnswered = kana.totalAnswered ?: 0
                val totalCorrect = kana.totalCorrect ?: 0
                var percent = 0.0

                progressBar.max = totalAnswered
                progressBar.progress = totalCorrect
//                    val df = DecimalFormat("##.##%")
                if (totalAnswered != 0) {
                    percent = totalCorrect / totalAnswered.toDouble()
                }
//                    val formattedPercent = df.format(percent)
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

                view.findViewById<TextView>(R.id.streakTextView).text = kana.streak.toString()

                val levelTextView = view.findViewById<TextView>(R.id.levelTextView)
                levelTextView.setTextColor(ContextCompat.getColor(context, getLevelColor(kana.level!!)))
                levelTextView.text = levelToTitle(kana.level!!)
                val writingLevelTextView = view.findViewById<TextView>(R.id.writingLevelTextView)
                writingLevelTextView.setTextColor(ContextCompat.getColor(context, getLevelColor(kana.level!!)))
                writingLevelTextView.text = levelToTitle(kana.level!!)

                val nextReview = view.findViewById<TextView>(R.id.reviewTextView)
                if (kana.reviewTime != null) {
                    if (kana.reviewTime!! > System.currentTimeMillis()) {
                        val timeUntilReview = kana.reviewTime!! - System.currentTimeMillis()
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
                view.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
                    dialog.dismiss()
                }

                dialog.setContentView(view)
                dialog.show()
            }


            val tabLayout = view.findViewById<TabLayout>(R.id.itemTabLayout)
            val viewAnimator = view.findViewById<ViewAnimator>(R.id.viewAnimator)

            val relativeLayout = RelativeLayout(context)
            relativeLayout.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )

            val traceId = if (kana.isHiragana) {
                context.resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter!!)}_trace", "drawable", context.packageName)
            } else {
                context.resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter!!)}_trace_k", "drawable", context.packageName)
            }

            relativeLayout.background = ContextCompat.getDrawable(context, traceId)

            val strokeId = if (kana.isHiragana) {
                context.resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter)}_stroke", "drawable", context.packageName)
            } else {
                context.resources.getIdentifier("${kanaConverter.hiraganaToRomaji(kana.letter)}_stroke_k", "drawable", context.packageName)
            }

            val kanaStrokeView = view.findViewById<ImageView>(R.id.kanaStrokeImageView)
            kanaStrokeView.setImageResource(strokeId)

            val drawingView = DrawingView(context)

            drawingView.setOnTouchListener { v, event ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP ->
                        v.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
            }

            val clearButton = Button(context)
            clearButton.text = "Clear"
            clearButton.textSize = 12f
            clearButton.minimumHeight = 0
            clearButton.layoutParams =
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    updateMargins(0, 100, 10, 10)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                clearButton.setTextColor(context.resources.getColor(R.color.white, context.theme))
            }
            clearButton.height = 75
            clearButton.setPadding(10)
            clearButton.background = ContextCompat.getDrawable(context, R.drawable.dialog_background)
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                clearButton.foreground = ContextCompat.getDrawable(context, outValue.resourceId)
            }

            clearButton.setOnClickListener {
                drawingView.clearDrawing()
            }

            relativeLayout.addView(clearButton)
            relativeLayout.addView(drawingView)

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
                    if (tabPosition == writeTab.position) {
                        kanaStrokeView.visibility = View.VISIBLE
                    } else {
                        kanaStrokeView.visibility = View.GONE
                    }
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

    fun setReviewToGone() {
        topTextView.visibility = View.GONE
    }

}