package com.email.contact.kanasrs.slides

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class KanaPageAdapter(activity: FragmentActivity, private val dataFragments: MutableList<ExplainTextFragment>): FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = dataFragments.size
    override fun createFragment(position: Int): Fragment = dataFragments[position]

    fun add(fragment: ExplainTextFragment) {
        dataFragments.add(fragment)
        notifyItemChanged(dataFragments.size-1)
    }

}