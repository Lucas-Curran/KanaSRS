package com.example.jwriter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.RecyclerView
import com.example.jwriter.activity.setOnSingleClickListener
import com.example.jwriter.database.Kana
import com.example.jwriter.util.AnimUtilities
import com.example.jwriter.util.AnimUtilities.Companion.setNextAnim
import com.example.jwriter.util.AnimUtilities.Companion.setPrevAnim
import com.example.jwriter.util.KanaConverter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout


class KanaGridAdapter(var context: Context): RecyclerView.Adapter<KanaGridAdapter.ViewHolder>()  {

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
        when (data.level) {
            1, 2 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.rookie_pink)
            3 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.amateur_purple)
            4 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.expert_blue)
            5 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.master_blue)
            6 -> drawable.color = AppCompatResources.getColorStateList(context, R.color.sensei_gold)
        }
        holder.itemView.setOnClickListener {
            KanaInfoView(parent.context, data).show()
        }
    }

    override fun getItemCount() = dataList.size

}