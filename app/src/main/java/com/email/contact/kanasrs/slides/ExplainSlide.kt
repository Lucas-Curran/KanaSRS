package com.email.contact.kanasrs.slides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.email.contact.kanasrs.R

class ExplainSlide : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_explain_slide, container, false)

    companion object {
        fun newInstance() : ExplainSlide {
            return ExplainSlide()
        }
    }
}