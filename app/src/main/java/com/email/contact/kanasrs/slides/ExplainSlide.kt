package com.email.contact.kanasrs.slides

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.email.contact.kanasrs.R

class ExplainSlide : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hiragana_slide, container, false)
    }

    companion object {
        fun newInstance() : ExplainSlide {
            return ExplainSlide()
        }
    }
}