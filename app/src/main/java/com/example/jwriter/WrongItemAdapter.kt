package com.example.jwriter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text


class WrongItemAdapter(private val englishName: ArrayList<String>
, private val japaneseName: ArrayList<String>) :
    RecyclerView.Adapter<WrongItemAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val englishLetter: TextView
        val japaneseLetter: TextView
        val actualEnglishLetter: TextView
        init {
            // Define click listener for the ViewHolder's View.
            englishLetter = view.findViewById(R.id.englishLetter)
            japaneseLetter = view.findViewById(R.id.japaneseLetter)
            actualEnglishLetter = view.findViewById(R.id.actualAnswer)
            view.setOnClickListener {
                println("hi")
            }
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.wrong_item_layout, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element

        val kanaConverter = KanaConverter(false)

        viewHolder.englishLetter.text = "Said: " + englishName[position]
        viewHolder.japaneseLetter.text = japaneseName[position]
        viewHolder.actualEnglishLetter.text = "Answer: " + kanaConverter._hiraganaToRomaji(japaneseName[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = englishName.size

}
