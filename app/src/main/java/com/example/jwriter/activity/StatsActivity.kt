package com.example.jwriter.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.example.jwriter.database.JWriterDatabase
import com.example.jwriter.R
import com.google.android.material.tabs.TabLayout
import com.skydoves.progressview.ProgressView
import com.skydoves.progressview.progressView
import kotlin.random.Random


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

    private var masteredHiragana = 0
    private var masteredKatakana = 0

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
        overallView.findViewById<ProgressBar>(R.id.kanaMasteryBar).progress = masteredKatakana + masteredHiragana

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

        loadBars(ReviewActivity.hiraganaList, hiragana = true)
        loadBars(ReviewActivity.katakanaList, hiragana = false)

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