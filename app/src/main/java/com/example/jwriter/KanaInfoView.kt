package com.example.jwriter

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
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
import com.example.jwriter.activity.setOnSingleClickListener
import com.example.jwriter.database.JWriterDatabase
import com.example.jwriter.database.Kana
import com.example.jwriter.util.KanaConverter
import com.example.jwriter.util.Utilities
import com.example.jwriter.util.Utilities.Companion.getLevelColor
import com.example.jwriter.util.Utilities.Companion.mediaPlayer
import com.example.jwriter.util.Utilities.Companion.setNextAnim
import com.example.jwriter.util.Utilities.Companion.setPrevAnim
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout


@SuppressLint("ClickableViewAccessibility")
class KanaInfoView(val context: Context, val kana: Kana) {

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

            if (kana.reviewTime != null) {
                if (kana.reviewTime!! > System.currentTimeMillis()) {
                    val timeUntilReview = kana.reviewTime!! - System.currentTimeMillis()
                    topTextView.text = "Review -> ${Utilities.formatTime(timeUntilReview)}"
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
            mnemonicText.text = kana.mnemonic

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

                currentText.text = kana.mnemonic

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
                            kana.mnemonic = editText.text.toString()
                            mnemonicText.text = kana.mnemonic
                            JWriterDatabase.getInstance(context).kanaDao().updateKana(kana)
                            dialog.dismiss()
                        }
                    }
                }
                mnemonicView.findViewById<TextView>(R.id.defaultResetTextView).setOnClickListener {

                }

                dialog.setContentView(mnemonicView)
                dialog.show()
            }


            val tabLayout = view.findViewById<TabLayout>(R.id.itemTabLayout)
            val viewAnimator = view.findViewById<ViewAnimator>(R.id.viewAnimator)

            val relativeLayout = RelativeLayout(context)
            relativeLayout.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            relativeLayout.background = ContextCompat.getDrawable(context, R.drawable.pink_outline)

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
            clearButton.background = ContextCompat.getDrawable(context, R.drawable.pink_outline)
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