package com.email.contact.kanasrs.slides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.email.contact.kanasrs.R

class TransitionSlide : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_transition_slide, container, false)

    companion object {
        fun newInstance() : TransitionSlide {
            return TransitionSlide()
        }
    }
}