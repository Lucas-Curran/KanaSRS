package com.email.contact.kanasrs.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.email.contact.kanasrs.custom.KanaInfoView
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.database.Kana


class KanaGridAdapter(var context: Context, val writing: Boolean): RecyclerView.Adapter<KanaGridAdapter.ViewHolder>()  {

    var dataList = emptyList<Kana>()
    private lateinit var parent: ViewGroup
    private lateinit var animationIn: Animation
    private lateinit var animationOut: Animation
    private lateinit var prevAnimIn: Animation
    private lateinit var prevAnimOut: Animation

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

        val level = if (writing) {
            data.writingLevel!!
        } else {
            data.level!!
        }

        when (level) {
            1, 2 -> drawable.color = AppCompatResources.getColorStateList(context,
                R.color.rookie_pink
            )
            3 -> drawable.color = AppCompatResources.getColorStateList(context,
                R.color.amateur_purple
            )
            4 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.expert_blue)
            5 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.master_blue)
            6 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.sensei_gold)
        }
        holder.itemView.setOnClickListener {
            KanaInfoView(parent.context, data, true).show()
        }
    }

    override fun getItemCount() = dataList.size

}