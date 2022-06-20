package com.email.contact.kanasrs.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.email.contact.kanasrs.adapter.KanaGridAdapter
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.database.Kana

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
        val numeralTextView = findViewById<TextView>(R.id.levelNumeralTextView)

        when (intent.getStringExtra("level")) {
            "rookie" -> {
                dataList = (getKanaAtLevel(1) + getKanaAtLevel(2)).toMutableList()
                numeralTextView.text = "Learning"
                titleTextView.text = "ROOKIE"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.rookie_pink))
            }
            "amateur" -> {
                dataList = getKanaAtLevel(3)
                numeralTextView.text = "Understanding"
                titleTextView.text = "AMATEUR"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.amateur_purple))
            }
            "expert" -> {
                dataList = getKanaAtLevel(4)
                numeralTextView.text = "Intermediate"
                titleTextView.text = "EXPERT"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.expert_blue))
            }
            "master" -> {
                dataList = getKanaAtLevel(5)
                numeralTextView.text = "Proficient"
                titleTextView.text = "MASTER"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.master_blue))
            }
            "sensei" -> {
                dataList = getKanaAtLevel(6)
                numeralTextView.text = "Mastery"
                titleTextView.text = "SENSEI"
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.sensei_gold))
            }
        }

        val closeActivityImage = findViewById<ImageView>(R.id.closeActivityImage)

        closeActivityImage.translationY = 300f
        closeActivityImage.animate().translationYBy(-300f).setDuration(1000L).start()
        closeActivityImage.setOnClickListener {
            finish()
        }

        kanaGridAdapter.setDataList(dataList)

    }

    private fun getKanaAtLevel(level: Int): MutableList<Kana> {
        val list = mutableListOf<Kana>()
        for (kana in KanaSRSDatabase.getInstance(this).kanaDao().getKana()) {
            if (kana.level == level) {
                list.add(kana)
            }
        }
        return list
    }
}