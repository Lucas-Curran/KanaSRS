package com.example.jwriter

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.core.view.marginBottom
import com.example.jwriter.util.Utilities
import com.google.android.material.button.MaterialButton
import com.skydoves.progressview.ProgressView
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target

class MenuIntroFragment : Fragment() {

    private lateinit var menuView: View
    private var container: ViewGroup? = null
    private var showMore = true
    private var moving = false
    private lateinit var levelsLayoutTarget: Target
    private lateinit var buttons: ArrayList<View>
    private lateinit var spotLight: Spotlight
    private var currentSpotlight = 0
    private var targets = ArrayList<Target>()

    private val entireSummaryButtonSpotlight = 0
    private val nextReviewTimeSpotlight = 1
    private val progressBarsSpotlight = 2
    private val showMoreArrowSpotlight = 3
    private val levelsSpotlight = 4

    override fun onCreateView(inflater: LayoutInflater,
                          container: ViewGroup?,
                          savedInstanceState: Bundle?
    ): View {
        this.container = container

        menuView = inflater.inflate(R.layout.fragment_menu_intro, container, false)
        return inflater.inflate(R.layout.fragment_menu_intro, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val first = layoutInflater.inflate(R.layout.intro_spotlight, FrameLayout(requireContext()))
        val summaryButton = view.findViewById<MaterialButton>(R.id.summaryButton)
        val scrollView = view.findViewById<ScrollView>(R.id.menuScrollView)
        val nextReviewTextView = view.findViewById<TextView>(R.id.nextReviewText)
        val progressBarSpace = view.findViewById<Space>(R.id.belowHiraganaSpace)
        val hiraganaProgressBar = view.findViewById<ProgressView>(R.id.hiraganaProgressView)

        //Set scroll view touch listener so user can't scroll
        scrollView.setOnTouchListener { view, motionEvent ->
            true
        }

        first.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                if (currentSpotlight != showMoreArrowSpotlight && currentSpotlight != levelsSpotlight) {
                    nextSpotlight()
                }
            }
            true
        }

        val learnButton = view.findViewById<MaterialButton>(R.id.lessonButton)
        val reviewButton = view.findViewById<MaterialButton>(R.id.reviewButton)
        val statsButton = view.findViewById<MaterialButton>(R.id.statsButton)
        val settingsButton = view.findViewById<MaterialButton>(R.id.settingsButton)
        val showMoreArrow = view.findViewById<ImageView>(R.id.arrowImageView)
        val summaryLayout = view.findViewById<RelativeLayout>(R.id.summaryRelativeLayout)
        val levelsLayout = view.findViewById<LinearLayout>(R.id.levelsLayout)

        buttons = arrayListOf(learnButton, reviewButton, statsButton, settingsButton, showMoreArrow)
        disableButtons()

        view.post {

            targets.add(Utilities.newTarget(summaryButton, summaryButton.height.toFloat(), summaryButton.width.toFloat(), first))
            targets.add(Utilities.newTarget(nextReviewTextView, nextReviewTextView.height.toFloat(), nextReviewTextView.width.toFloat(), first))
            targets.add(Utilities.newTarget(progressBarSpace, hiraganaProgressBar.height.toFloat() * 2.6f, hiraganaProgressBar.width.toFloat(), first))
            targets.add(Utilities.newTarget(showMoreArrow, showMoreArrow.height.toFloat(), showMoreArrow.width.toFloat(), first, {
                showMoreArrow.isEnabled = true
                first.setOnTouchListener { view, motionEvent ->
                    false
                }
            }))

           levelsLayoutTarget = Utilities.newTarget(levelsLayout.findViewById(R.id.expert), levelsLayout.measuredHeight.toFloat(), levelsLayout.measuredWidth.toFloat(), first, {

            }, {
                showMoreArrow.isEnabled = false
                first.setOnTouchListener { view, motionEvent ->
                    if (motionEvent.action == MotionEvent.ACTION_UP) {
                        if (currentSpotlight != showMoreArrowSpotlight && currentSpotlight != levelsSpotlight) {
                            nextSpotlight()
                        }
                    }
                    true
                }
            })

            targets.add(levelsLayoutTarget)

            activity?.let {
                    spotLight = Spotlight.Builder(it)
                        .setTargets(targets)
                        .setBackgroundColorRes(R.color.spotlight_background)
                        .setDuration(1000L)
                        .setAnimation(DecelerateInterpolator(2f))
                        .build()
                    spotLight.start()
            }
        }

        showMoreArrow.setOnClickListener {
            if (showMore && !moving) {
                moving = true
                Utilities.slideView(
                    summaryLayout,
                    summaryLayout.height,
                    summaryLayout.height + levelsLayout.measuredHeight + levelsLayout.marginBottom
                ) {}
                Utilities.slideView(
                    summaryButton,
                    summaryButton.height,
                    summaryLayout.height + levelsLayout.measuredHeight + levelsLayout.marginBottom
                ) {
                    nextSpotlight()
                    showMore = false
                    moving = false
                }
                showMoreArrow.setImageResource(android.R.drawable.arrow_up_float)
            } else if (!showMore && !moving) {
                moving = true
                Utilities.slideView(
                    summaryLayout,
                    summaryLayout.height,
                    summaryLayout.height - levelsLayout.measuredHeight - levelsLayout.marginBottom
                ) {}
                Utilities.slideView(
                    summaryButton,
                    summaryButton.height,
                    summaryLayout.height - levelsLayout.measuredHeight - levelsLayout.marginBottom
                ) {
                    nextSpotlight()
                    moving = false
                    showMore = true
                }
                showMoreArrow.setImageResource(android.R.drawable.arrow_down_float)
            }
        }
    }



    private fun disableButtons() {
        for (button in buttons) {
            button.isEnabled = false
        }
    }

    fun nextSpotlight() {
        currentSpotlight++
        spotLight.next()
    }

    fun previousSpotlight() {
        if (currentSpotlight != entireSummaryButtonSpotlight) {
            currentSpotlight--
            spotLight.previous()
        }
    }

    companion object {
        fun newInstance() : MenuIntroFragment {
            return MenuIntroFragment()
        }
    }
}