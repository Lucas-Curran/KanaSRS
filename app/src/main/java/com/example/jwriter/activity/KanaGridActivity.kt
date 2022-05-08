package com.example.jwriter.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.jwriter.KanaGridAdapter
import com.example.jwriter.R
import com.example.jwriter.database.JWriterDatabase
import com.example.jwriter.database.Kana

class KanaGridActivity : AppCompatActivity() {

    private lateinit var kanaGridAdapter: KanaGridAdapter
    private var dataList = mutableListOf<Kana>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kana_grid)

        val recyclerView = findViewById<RecyclerView>(R.id.kanaRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(applicationContext, 2)
        kanaGridAdapter = KanaGridAdapter(applicationContext)
        recyclerView.adapter = kanaGridAdapter

        val titleTextView = findViewById<TextView>(R.id.levelTitleTextView)

        when (intent.getStringExtra("level")) {
            "rookie" -> {
                dataList = (getKanaAtLevel(1) + getKanaAtLevel(2)).toMutableList()
                titleTextView.text = "ROOKIE"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.rookie_pink))
            }
            "amateur" -> {
                dataList = getKanaAtLevel(3)
                titleTextView.text = "AMATEUR"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.amateur_purple))
            }
            "expert" -> {
                dataList = getKanaAtLevel(4)
                titleTextView.text = "EXPERT"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.expert_blue))
            }
            "master" -> {
                dataList = getKanaAtLevel(5)
                titleTextView.text = "MASTER"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.master_blue))
            }
            "sensei" -> {
                dataList = getKanaAtLevel(6)
                titleTextView.text = "SENSEI"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.sensei_gold))
            }
        }

        kanaGridAdapter.setDataList(dataList)

    }

    private fun getKanaAtLevel(level: Int): MutableList<Kana> {
        val list = mutableListOf<Kana>()
        for (kana in JWriterDatabase.getInstance(this).kanaDao().getKana()) {
            if (kana.level == level) {
                list.add(kana)
            }
        }
        return list
    }
}