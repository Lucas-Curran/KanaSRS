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
        kanaGridAdapter = if (intent.getStringExtra("level") != null) {
            KanaGridAdapter(applicationContext, false)
        } else {
            KanaGridAdapter(applicationContext, true)
        }
        recyclerView.adapter = kanaGridAdapter

        val titleTextView = findViewById<TextView>(R.id.levelTitleTextView)

        val db = KanaSRSDatabase.getInstance(this)

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

        when (intent.getStringExtra("writingLevel")) {
            "rookie" -> {
                dataList = db.kanaDao().rookieWritingKana().toMutableList()
                titleTextView.text = "WRITING ROOKIE"
                titleTextView.textSize = 40f
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.rookie_pink))
            }
            "amateur" -> {
                dataList = db.kanaDao().amateurWritingKana().toMutableList()
                titleTextView.text = "WRITING AMATEUR"
                titleTextView.textSize = 40f
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.amateur_purple))
            }
            "expert" -> {
                dataList = db.kanaDao().expertWritingKana().toMutableList()
                titleTextView.text = "WRITING EXPERT"
                titleTextView.textSize = 40f
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.expert_blue))
            }
            "master" -> {
                dataList = db.kanaDao().expertWritingKana().toMutableList()
                titleTextView.text = "WRITING MASTER"
                titleTextView.textSize = 40f
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.master_blue))
            }
            "sensei" -> {
                dataList = db.kanaDao().senseiWritingKana().toMutableList()
                titleTextView.text = "WRITING SENSEI"
                titleTextView.textSize = 40f
                titleTextView.setTextColor(ContextCompat.getColor(this, R.color.sensei_gold))
            }
        }

        intent.removeExtra("level")
        intent.removeExtra("writingLevel")


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