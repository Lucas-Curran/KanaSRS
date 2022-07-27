package com.email.contact.kanasrs.slides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.email.contact.kanasrs.R

private const val TEXT_PARAM = "Explanation Text"

class ExplainTextFragment : Fragment() {

    private var textParam: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            textParam = it.getString(TEXT_PARAM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.intro_text_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.explainText).text = textParam
    }

    companion object {
        @JvmStatic
        fun newInstance(textExplanation: String) =
            ExplainTextFragment().apply {
                arguments = Bundle().apply {
                    putString(TEXT_PARAM, textExplanation)
                }
            }
    }
}