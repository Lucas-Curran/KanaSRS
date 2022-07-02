package com.email.contact.kanasrs.slides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.email.contact.kanasrs.R

class FirstIntroSlide : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_first_intro_slide, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.slideDescText).text = HtmlCompat.fromHtml(getString(R.string.slide1_description), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    companion object {
        fun newInstance() : FirstIntroSlide {
            return FirstIntroSlide()
        }
    }
}