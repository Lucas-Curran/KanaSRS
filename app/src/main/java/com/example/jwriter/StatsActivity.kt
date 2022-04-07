package com.example.jwriter

import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.skydoves.progressview.ProgressView
import com.skydoves.progressview.progressView
import kotlin.random.Random


/*
Stats: each letter's individual accuracy, worst letter, best time (for time mode)
Eventually include graphs, possibly over an interval of time
*/


class StatsActivity : AppCompatActivity() {

    private var score: Int = 0
    private lateinit var progressLinearLayout: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var hiraganaProgressViews: ArrayList<ProgressView>
    private lateinit var katakanaProgressViews: ArrayList<ProgressView>
    private lateinit var progressScrollView: ScrollView

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

        progressLinearLayout = findViewById(R.id.progressLinearLayout)
        tabLayout = findViewById(R.id.statsTabLayout)
        progressScrollView = findViewById(R.id.progressScrollView)

        loadUserStats()
    }

    /**
     * Loads the user's stats
     */
    private fun loadUserStats() {

        loadBars(ReviewActivity.hiraganaList, hiragana = true)
        loadBars(ReviewActivity.katakanaList, hiragana = false)

        score = JWriterDatabase.getInstance(this).userDao().getAccuracy()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    when(tab.position) {
                        TabConstants.OVERALL ->
                            overallTab()
                        TabConstants.HIRAGANA ->
                            hiraganaTab()
                        TabConstants.KATAKANA ->
                            katakanaTab()
                        else ->
                            println("No tab")
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
        val test = TextView(this)
        test.text = "Score: " + JWriterDatabase.getInstance(this).userDao().getAccuracy()
        progressLinearLayout.addView(test)
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
    private fun loadBars(barList: Array<String>, hiragana: Boolean) {
        for (letter in barList) {
            val myProgressView = progressView(this) {
                setSize(300, 35)
                setProgress(Random.nextDouble(0.0, 100.0).toFloat())
                setMin(0f)
                setMax(100f)
                setRadius(12f)
                setDuration(1200L)
                setAutoAnimate(true)
                setLabelColorInner(ContextCompat.getColor(applicationContext, R.color.white))
                setLabelColorOuter(ContextCompat.getColor(applicationContext, R.color.black))
                setLabelText(letter)
                setLabelSize(13f)
                setLabelSpace(10f)
                setLabelTypeface(Typeface.BOLD)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(10, 15, 10, 15)
            myProgressView.layoutParams = params
            if (hiragana) hiraganaProgressViews.add(myProgressView) else katakanaProgressViews.add(myProgressView)
        }
    }

}