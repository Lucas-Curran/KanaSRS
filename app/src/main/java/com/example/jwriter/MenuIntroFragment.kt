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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager.widget.ViewPager
import com.example.jwriter.util.AnimUtilities
import com.google.android.material.button.MaterialButton
import com.skydoves.progressview.ProgressView
import com.takusemba.spotlight.OnTargetListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.RoundedRectangle
import java.lang.IllegalStateException

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

            targets.add(AnimUtilities.newTarget(summaryButton, summaryButton.height.toFloat(), summaryButton.width.toFloat(), first))
            targets.add(AnimUtilities.newTarget(nextReviewTextView, nextReviewTextView.height.toFloat(), nextReviewTextView.width.toFloat(), first))
            targets.add(AnimUtilities.newTarget(progressBarSpace, hiraganaProgressBar.height.toFloat() * 2.6f, hiraganaProgressBar.width.toFloat(), first))
            targets.add(AnimUtilities.newTarget(showMoreArrow, showMoreArrow.height.toFloat(), showMoreArrow.width.toFloat(), first, {
                showMoreArrow.isEnabled = true
                first.setOnTouchListener { view, motionEvent ->
                    false
                }
            }))

           levelsLayoutTarget = AnimUtilities.newTarget(levelsLayout.findViewById(R.id.expertLinearLayout), levelsLayout.height.toFloat(), levelsLayout.width.toFloat(), first, {

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
                AnimUtilities.slideView(
                    summaryLayout,
                    summaryLayout.height,
                    summaryLayout.height + 800
                ) {}
                AnimUtilities.slideView(
                    summaryButton,
                    summaryButton.height,
                    summaryButton.height + 800
                ) {
                    nextSpotlight()
                    showMore = false
                    moving = false
                }
                showMoreArrow.setImageResource(android.R.drawable.arrow_up_float)
            } else if (!showMore && !moving) {
                moving = true
                AnimUtilities.slideView(
                    summaryLayout,
                    summaryLayout.height,
                    summaryLayout.height - 800
                ) {}
                AnimUtilities.slideView(
                    summaryButton,
                    summaryButton.height,
                    summaryButton.height - 800
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