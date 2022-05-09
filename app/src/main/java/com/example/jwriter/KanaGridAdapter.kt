package com.example.jwriter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.RecyclerView
import com.example.jwriter.activity.setOnSingleClickListener
import com.example.jwriter.database.Kana
import com.example.jwriter.util.AnimUtilities
import com.example.jwriter.util.KanaConverter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout


class KanaGridAdapter(var context: Context): RecyclerView.Adapter<KanaGridAdapter.ViewHolder>()  {

    var dataList = emptyList<Kana>()
    private lateinit var parent: ViewGroup
    private lateinit var animationIn: Animation
    private lateinit var animationOut: Animation
    private lateinit var prevAnimIn: Animation
    private lateinit var prevAnimOut: Animation
    private var mPlayer = MediaPlayer()


    internal fun setDataList(dataList: List<Kana>) {
        this.dataList = dataList
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var letter: TextView = itemView.findViewById(R.id.kanaCardText)
        var linearLayout: LinearLayout = itemView.findViewById(R.id.cardLinearLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        this.parent = parent
        val view = LayoutInflater.from(parent.context).inflate(R.layout.kana_card_layout, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]
        holder.letter.text = data.letter

        val drawable = (holder.linearLayout.background as GradientDrawable)
        drawable.setStroke(2, AppCompatResources.getColorStateList(context, R.color.white))
        when (data.level) {
            1, 2 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.rookie_pink)
            3 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.amateur_purple)
            4 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.expert_blue)
            5 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.master_blue)
            6 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.sensei_gold)
        }
        holder.itemView.setOnClickListener {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.kana_info_dialog, null)

            val dialog = BottomSheetDialog(parent.context)
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            if (data.reviewTime != null) {
                if (data.reviewTime!! > System.currentTimeMillis()) {
                    val timeUntilReview = data.reviewTime!! - System.currentTimeMillis()
                    view.findViewById<TextView>(R.id.nextReviewTextView).text = "Review -> ${AnimUtilities.formatTime(timeUntilReview)}"
                } else {
                    view.findViewById<TextView>(R.id.nextReviewTextView).text = "Review -> now"
                }
            }

            val kanaConverter = KanaConverter(false)

            view.findViewById<TextView>(R.id.kanaEnglishTextView).text = kanaConverter._hiraganaToRomaji(data.letter)
            view.findViewById<TextView>(R.id.kanaTextView).text = data.letter

            val webStroke = view.findViewById<WebView>(R.id.strokeWebView)
            webStroke.settings.javaScriptEnabled = true
            webStroke.webViewClient = WebViewClient()
            webStroke.loadUrl(data.gif)


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

            view.findViewById<ImageView>(R.id.kanaAudioImageView).setOnClickListener {
                playAudio(kanaConverter._hiraganaToRomaji(data.letter))
            }

            view.findViewById<ImageButton>(R.id.nextItemButton).setOnSingleClickListener {
                setNextAnim(viewAnimator)
                tabLayout.getTabAt(tabLayout.selectedTabPosition+1)?.select()
                if (tabLayout.selectedTabPosition == 2) {
                    dialog.behavior.isDraggable = false
                }
            }

            view.findViewById<ImageButton>(R.id.previousItemButton).setOnSingleClickListener {
                setPrevAnim(viewAnimator)
                tabLayout.getTabAt(tabLayout.selectedTabPosition-1)?.select()
                dialog.behavior.isDraggable = true
            }

            dialog.setContentView(view)
            dialog.show()
        }
    }

    override fun getItemCount() = dataList.size

    private fun playAudio(letter: String) {
        if (!mPlayer.isPlaying) {
            mPlayer = MediaPlayer.create(context, context.resources.getIdentifier(letter, "raw", context.packageName))
            mPlayer.start()
        }
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