package com.email.contact.kanasrs.slides

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.email.contact.kanasrs.R

class ExitSlide : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_final_slide, container, false)
    }

    companion object {
        fun newInstance() : ExitSlide {
            return ExitSlide()
        }
    }
}