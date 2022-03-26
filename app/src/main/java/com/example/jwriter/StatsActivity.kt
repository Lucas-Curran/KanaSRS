package com.example.jwriter

import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate


/*
Stats: each letter's individual accuracy, worst letter, best time (for time mode)
Eventually include graphs, possibly over an interval of time
*/


class StatsActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener, OnChartValueSelectedListener {

    private lateinit var accuracyTextView: TextView
    private lateinit var accuracyBarChart: BarChart
    private var score: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        accuracyTextView = findViewById(R.id.accuracyTextView)
        accuracyBarChart = findViewById(R.id.accuracyBarChart)

        loadUserStats()
    }

    /**
     * Loads the user's stats
     */
    private fun loadUserStats() {
        score = JWriterDatabase.getInstance(this)?.userDao()?.getAccuracy()!!
        accuracyTextView.text = "Total Accuracy: $score"
        accuracyBarChart.setMaxVisibleValueCount(40)
        accuracyBarChart.setDrawBarShadow(false)
        accuracyBarChart.setDrawGridBackground(false)
        accuracyBarChart.setDrawValueAboveBar(true)
        accuracyBarChart.isHighlightFullBarEnabled = true
        // change the position of the y-labels
        // change the position of the y-labels
        val leftAxis: YAxis = accuracyBarChart.axisLeft
        leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)
        accuracyBarChart.axisRight.isEnabled = false

        // chart.setDrawXLabels(false);
        // chart.setDrawYLabels(false);

        // setting data;

        // chart.setDrawXLabels(false);
        // chart.setDrawYLabels(false);

        // setting data;
        val legend = accuracyBarChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.formSize = 8f
        legend.formToTextSpace = 4f
        legend.xEntrySpace = 6f

        val values = ArrayList<BarEntry>()
        values.add(BarEntry(0F, floatArrayOf(score.toFloat(), 5F)))

        val set1: BarDataSet

        if (accuracyBarChart.data != null &&
            accuracyBarChart.data.dataSetCount > 0
        ) {
            set1 = accuracyBarChart.getData().getDataSetByIndex(0) as BarDataSet
            set1.values = values
            accuracyBarChart.getData().notifyDataChanged()
            accuracyBarChart.notifyDataSetChanged()
        } else {
            set1 = BarDataSet(values, "Statistics Vienna 2014")
            set1.setDrawIcons(false)
            set1.colors = getColors()
            set1.stackLabels = arrayOf("Births", "Divorces", "Marriages")
            val dataSets: ArrayList<IBarDataSet> = ArrayList()
            dataSets.add(set1)
            val data = BarData(dataSets)
            //data.setValueFormatter(MyValueFormatter())
            data.setValueTextColor(Color.WHITE)
            accuracyBarChart.data = data
        }

        accuracyBarChart.setFitBars(true)
        accuracyBarChart.invalidate()

    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        //val values = ArrayList<BarEntry>()
        //values.add(BarEntry(0F, floatArrayOf(score.toFloat(), 5F)))

    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
    }

    override fun onNothingSelected() {

    }

    private fun getColors(): MutableList<Int> {

        // have as many colors as stack-values per entry
        val colors = mutableListOf(3)
        for (color in ColorTemplate.MATERIAL_COLORS) {
            colors.add(color)
        }
        return colors
    }

}