package com.example.jwriter

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
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
import com.example.jwriter.database.Kana
import com.example.jwriter.util.Utilities
import com.example.jwriter.util.Utilities.Companion.mediaPlayer
import com.example.jwriter.util.Utilities.Companion.setNextAnim
import com.example.jwriter.util.Utilities.Companion.setPrevAnim
import com.example.jwriter.util.KanaConverter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout

class KanaInfoView(context: Context, kana: Kana) {

    private var animationIn: Animation
    private var animationOut: Animation
    private var prevAnimIn: Animation
    private var prevAnimOut: Animation
    private var dialog: BottomSheetDialog
    private var nextReviewText: TextView
    private var mContext: Context

    init {

        mContext = context

        val view = LayoutInflater.from(context).inflate(R.layout.kana_info_dialog, null)

        dialog = BottomSheetDialog(context)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        nextReviewText = view.findViewById(R.id.nextReviewTextView)

        if (kana.reviewTime != null) {
            if (kana.reviewTime!! > System.currentTimeMillis()) {
                val timeUntilReview = kana.reviewTime!! - System.currentTimeMillis()
                nextReviewText.text = "Review -> ${Utilities.formatTime(timeUntilReview)}"
            } else {
                nextReviewText.text = "Review -> now"
            }
        } else {
            if (!kana.hasLearned) {
                nextReviewText.text = "Need to learn"
            } else {
                nextReviewText.text = "Already mastered!"
            }
        }

        val kanaConverter = KanaConverter(false)

        view.findViewById<TextView>(R.id.kanaEnglishTextView).text = kanaConverter._hiraganaToRomaji(kana.letter)
        view.findViewById<TextView>(R.id.kanaTextView).text = kana.letter

        val webStroke = view.findViewById<WebView>(R.id.strokeWebView)
        webStroke.settings.javaScriptEnabled = true
        webStroke.webViewClient = WebViewClient()
        webStroke.loadUrl(kana.gif)


        val tabLayout = view.findViewById<TabLayout>(R.id.itemTabLayout)
        val viewAnimator = view.findViewById<ViewAnimator>(R.id.viewAnimator)

        val relativeLayout = RelativeLayout(context)
        relativeLayout.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        relativeLayout.background = ContextCompat.getDrawable(context, R.drawable.pink_outline)

        val drawingView = DrawingView(context)
        val clearButton = Button(context)
        clearButton.text = "Clear"
        clearButton.textSize = 12f
        clearButton.minimumHeight = 0
        clearButton.layoutParams =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
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

        tabLayout.addOnTabSelectedListener( object : TabLayout.OnTabSelectedListener {
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
                mediaPlayer = MediaPlayer.create(mContext, mContext.resources.getIdentifier(kanaConverter._hiraganaToRomaji(kana.letter), "raw", mContext.packageName))
                mediaPlayer.start()
            }
        }

        nextButton.setOnSingleClickListener {

            if (tabLayout.selectedTabPosition == 2) {
                dialog.dismiss()
                return@setOnSingleClickListener
            }

            setNextAnim(viewAnimator, animationIn, animationOut)
            tabLayout.getTabAt(tabLayout.selectedTabPosition+1)?.select()
            if (tabLayout.selectedTabPosition == 2) {
                dialog.behavior.isDraggable = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    nextButton.setImageResource(R.drawable.ic_checkmark)
                    nextButton.setBackgroundColor(context.resources.getColor(R.color.lime, context.theme))
                }
            }
        }

        previousButton.setOnSingleClickListener {
            setPrevAnim(viewAnimator, prevAnimIn, prevAnimOut)
            tabLayout.getTabAt(tabLayout.selectedTabPosition-1)?.select()
            dialog.behavior.isDraggable = true
            if (tabLayout.selectedTabPosition == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nextButton.setImageResource(R.drawable.ic_right_arrow)
                nextButton.setBackgroundColor(context.resources.getColor(R.color.azure, context.theme))
            }
        }

        dialog.setContentView(view)
    }

    fun show() {
        dialog.show()
    }

    fun setReviewToGone() {
        nextReviewText.visibility = View.GONE
    }

}