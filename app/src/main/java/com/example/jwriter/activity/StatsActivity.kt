package com.example.jwriter.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.jwriter.KanaInfoView
import com.example.jwriter.database.JWriterDatabase
import com.example.jwriter.R
import com.example.jwriter.database.Kana
import com.example.jwriter.util.Utilities.Companion.formatTime
import com.google.android.material.tabs.TabLayout
import com.skydoves.progressview.ProgressLabelConstraints
import com.skydoves.progressview.ProgressView
import com.skydoves.progressview.progressView


/*
Stats: each letter's individual accuracy, worst letter, best time (for time mode)
Eventually include graphs, possibly over an interval of time
*/


class StatsActivity : AppCompatActivity() {

    private lateinit var progressLinearLayout: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var hiraganaProgressViews: ArrayList<ProgressView>
    private lateinit var katakanaProgressViews: ArrayList<ProgressView>
    private lateinit var progressScrollView: ScrollView
    private lateinit var overallView: View

    private var mostRecentReview = Long.MAX_VALUE

    private var masteredHiragana = 0
    private var masteredKatakana = 0

    private var numItemsToReview = 0

    private var levelsItemArray = IntArray(5)

    private val ROOKIE = 0
    private val AMATEUR = 1
    private val EXPERT = 2
    private val MASTER = 3
    private val SENSEI = 4

    object TabConstants {
         const val OVERALL = 0
         const val HIRAGANA = 1
         const val KATAKANA = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        hiraganaProgressViews = ArrayList()
        katakanaProgressViews = ArrayList()

        overallView = layoutInflater.inflate(R.layout.stats_overall_tab, null)


        progressLinearLayout = findViewById(R.id.progressLinearLayout)
        tabLayout = findViewById(R.id.statsTabLayout)
        progressScrollView = findViewById(R.id.progressScrollView)

        val rookie = overallView.findViewById<LinearLayout>(R.id.rookie)
        val amateur = overallView.findViewById<LinearLayout>(R.id.amateur)
        val expert = overallView.findViewById<LinearLayout>(R.id.expert)
        val master = overallView.findViewById<LinearLayout>(R.id.master)
        val sensei = overallView.findViewById<LinearLayout>(R.id.sensei)
        val levelsArray = arrayOf(rookie, amateur, expert, master, sensei)

        for (kana in JWriterDatabase.getInstance(this).kanaDao().getKana()) {
            if (kana.level != null) {
                when (kana.level) {
                    1, 2 -> levelsItemArray[ROOKIE] += 1
                    3 -> levelsItemArray[AMATEUR] += 1
                    4 -> levelsItemArray[EXPERT] += 1
                    5 -> levelsItemArray[MASTER] += 1
                    6 ->  {
                        levelsItemArray[SENSEI] += 1
                        if (kana.isHiragana) {
                            masteredHiragana++
                        } else {
                            masteredKatakana++
                        }
                    }
                }
            }
        }

        overallView.findViewById<TextView>(R.id.hiraganaFraction).text = masteredHiragana.toString()
        overallView.findViewById<TextView>(R.id.katakanaFraction).text = masteredKatakana.toString()

        val progressView = overallView.findViewById<ProgressView>(R.id.kanaMasteryBar)

        val totalMastered = masteredHiragana + masteredKatakana
        progressView.progress = totalMastered.toFloat()

        progressView.setOnClickListener {
            Toast.makeText(this, "$totalMastered kana mastered", Toast.LENGTH_SHORT).show()
        }
        progressView.setOnProgressClickListener {
            Toast.makeText(this, "$totalMastered kana mastered", Toast.LENGTH_SHORT).show()
        }

        if (totalMastered == 92) {
            progressView.labelText = "All Mastered!"
            progressView.labelConstraints = ProgressLabelConstraints.ALIGN_CONTAINER
            progressView.labelGravity = Gravity.CENTER
        } else {
            progressView.labelText = "%.2f".format((totalMastered.toFloat() / 92) * 100) + "%"
        }

        for (kana in JWriterDatabase.getInstance(this).kanaDao().getKana()) {
            //Check if there is a review time, and if so, check if the current time has passed the stored review time
            // Review time is calculated during review answers and initially added when learned in lessons
            if (kana.reviewTime != null) {
                if (kana.reviewTime!! < System.currentTimeMillis()) {
                    numItemsToReview++
                } else {
                    val millisecondsUntilReview = kana.reviewTime!! - System.currentTimeMillis()
                    if (millisecondsUntilReview < mostRecentReview) {
                        mostRecentReview = millisecondsUntilReview
                    }
                }
            }
        }

        val nextReviewTime = overallView.findViewById<TextView>(R.id.nextReviewTextView)

        if (mostRecentReview == Long.MAX_VALUE) {
            nextReviewTime.text = "none"
        } else {
            nextReviewTime.text = "${formatTime(mostRecentReview)}"
        }

        overallView.findViewById<TextView>(R.id.reviewNumberTextView).text = "$numItemsToReview"

        for ((index, level) in levelsArray.withIndex()) {
            val name = level.resources.getResourceEntryName(level.id)
            overallView.findViewById<TextView>(resources.getIdentifier("${name}NumKanaTextView", "id", packageName)).text = levelsItemArray[index].toString()
            level.setOnClickListener {
                val intent = Intent(this, KanaGridActivity::class.java)
                intent.putExtra("level", name)
                startActivity(intent)
            }
        }

        loadUserStats()
        overallTab()
    }

    /**
     * Loads the user's stats
     */
    private fun loadUserStats() {

        loadBars(JWriterDatabase.getInstance(this).kanaDao().getHiragana())
        loadBars(JWriterDatabase.getInstance(this).kanaDao().getKatakana())

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    when(tab.position) {
                        TabConstants.OVERALL -> overallTab()
                        TabConstants.HIRAGANA -> hiraganaTab()
                        TabConstants.KATAKANA -> katakanaTab()
                    }
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Handle tab reselect
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Handle tab unselect
            }
        })
    }

    /**
     * General statistics tab
     */
    private fun overallTab() {
        progressLinearLayout.removeAllViewsInLayout()
        progressLinearLayout.addView(overallView)
    }

    /**
     * Hiragana progress tab
     */
    private fun hiraganaTab() {
        //Remove current layout, loop through hiragana progress bars and add them to the scroll view
        progressLinearLayout.removeAllViewsInLayout()
        for (view in hiraganaProgressViews) {
            progressLinearLayout.addView(view)
        }
        progressScrollView.fullScroll(ScrollView.FOCUS_UP)
    }

    /**
     * Katakana progress tab
     */
    private fun katakanaTab() {
        //Remove current layout, loop through katakana progress bars and add them to the scroll view
        progressLinearLayout.removeAllViewsInLayout()
        for (view in katakanaProgressViews) {
            progressLinearLayout.addView(view)
        }
        progressScrollView.fullScroll(ScrollView.FOCUS_UP)
    }

    /**
     * Adds the data for each kana into progress view array list
     * @param barList string array of labels attached to each bar
     * @param hiragana whether the bar is hiragana labels
     */
    private fun loadBars(barList: List<Kana>) {
        for (kana in barList) {

            var color: Int = 0
            var progress: Float = 0f

            if (kana.level != null) {
                when (kana.level) {
                    1 -> {
                        color = ContextCompat.getColor(this, R.color.rookie_pink)
                        progress = 1f
                    }
                    2 -> {
                        color = ContextCompat.getColor(this, R.color.rookie_pink)
                        progress = 2f
                    }
                    3 -> {
                        color = ContextCompat.getColor(this, R.color.amateur_purple)
                        progress = 3f
                    }
                    4 -> {
                        color = ContextCompat.getColor(this, R.color.expert_blue)
                        progress = 4f
                    }
                    5 -> {
                        color = ContextCompat.getColor(this, R.color.master_blue)
                        progress = 5f
                    }
                    6 -> {
                        color = ContextCompat.getColor(this, R.color.sensei_gold)
                        progress = 6f
                    }
                }
            }

            val myProgressView = progressView(this) {

                setProgress(progress)
                setMin(0f)
                setMax(6f)
                setRadius(12f)
                setDuration(1200L)
                setAutoAnimate(true)
                setLabelColorInner(ContextCompat.getColor(applicationContext, R.color.white))
                setLabelColorOuter(ContextCompat.getColor(applicationContext, R.color.black))
                setLabelText(kana.letter!!)
                setProgressbarColor(color)
                setLabelSize(13f)
                setLabelSpace(10f)
                setLabelTypeface(Typeface.BOLD)
            }

            myProgressView.setOnProgressClickListener {
                KanaInfoView(this, kana).show()
            }

            myProgressView.setOnClickListener {
                KanaInfoView(this, kana).show()
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(10, 15, 10, 15)
            myProgressView.layoutParams = params
            if (kana.isHiragana) hiraganaProgressViews.add(myProgressView) else katakanaProgressViews.add(myProgressView)
        }
    }

}