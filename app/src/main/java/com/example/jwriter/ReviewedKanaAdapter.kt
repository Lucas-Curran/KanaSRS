package com.example.jwriter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.example.jwriter.database.Kana


class ReviewedKanaAdapter(private val kanaList: List<Kana>, private val mContext: Context, private val correct: Boolean) :
    RecyclerView.Adapter<ReviewedKanaAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val japaneseLetter: TextView = view.findViewById(R.id.japaneseLetter)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.post_review_card, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element

        var color = 0

        if (correct) {
            when (kanaList[position].level) {
                1, 2 -> {
                    color = R.color.rookie_pink
                }
                3 -> {
                    color = R.color.amateur_purple
                }
                4 -> {
                    color = R.color.expert_blue
                }
                5 -> {
                    color = R.color.master_blue
                }
                6 -> {
                    color = R.color.sensei_gold
                }
            }
        } else {
            color = R.color.wrong_answer
        }

        viewHolder.japaneseLetter.text = kanaList[position].letter
        viewHolder.japaneseLetter.backgroundTintList = AppCompatResources.getColorStateList(mContext, color)
        viewHolder.japaneseLetter.setOnClickListener {
            KanaInfoView(mContext, kanaList[position], false).show()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = kanaList.size

}
