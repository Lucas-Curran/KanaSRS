package com.email.contact.kanasrs.slides

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.email.contact.kanasrs.R
import com.github.appintro.SlidePolicy
import com.google.android.material.tabs.TabLayout
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator


class ExplainSlide : Fragment(), SlidePolicy {

    private lateinit var englishTitle: TextView
    private lateinit var japaneseTitle: TextView
    private lateinit var japaneseFooter: TextView
    private lateinit var explainViewPager: ViewPager2
    private lateinit var dotsIndicator: SpringDotsIndicator

    private var seenHiragana = false
    private var seenKatakana = false
    private var seenKanji = false

    override val isPolicyRespected: Boolean
        get() = seenHiragana && seenKatakana && seenKanji

    override fun onUserIllegallyRequestedNextPage() {
        val toastMessage = "Please go through " +
        when {
            !seenHiragana and !seenKatakana and !seenKanji -> "hiragana, katakana, and kanji"
            !seenHiragana and !seenKatakana -> "hiragana and katakana"
            !seenKatakana and !seenKanji -> "katakana and kanji"
            !seenHiragana and !seenKanji -> "hiragana and kanji"
            !seenHiragana -> "hiragana"
            !seenKatakana -> "katakana"
            !seenKanji -> "kanji"
            else -> ""
        } + " before moving on!"
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_explain_slide, container, false)

        englishTitle = view.findViewById(R.id.englishTitle)
        japaneseTitle = view.findViewById(R.id.japaneseTitle)
        japaneseFooter = view.findViewById(R.id.footerJapaneseText)
        explainViewPager = view.findViewById(R.id.viewPagerText)
        dotsIndicator = view.findViewById(R.id.dotsIndicator)

        val tabLayout = view.findViewById<TabLayout>(R.id.explainTabsLayout)

        for (i in 0 until tabLayout.tabCount) {
            val tab = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
            val p = tab.layoutParams as MarginLayoutParams
            p.setMargins(15, 0, 15, 0)
            tab.requestLayout()
        }

        explainViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == explainViewPager.adapter?.itemCount?.minus(1) ?: Log.e("IntroError", "adapter item count null")) {
                    when (englishTitle.text) {
                        "Hiragana" -> seenHiragana = true
                        "Katakana" -> seenKatakana = true
                        "Kanji" -> seenKanji = true
                    }
                }
            }
        })

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    when (tab.position) {
                        0 -> showHiragana()
                        1 -> showKatakana()
                        2 -> showKanji()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        showHiragana()

        return view
    }

    private fun showHiragana() {
        val newFragments = mutableListOf<ExplainTextFragment>()
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.hiragana_intro_1)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.hiragana_intro_2)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.hiragana_intro_3)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.hiragana_intro_4)))
        explainViewPager.adapter = KanaPageAdapter(requireActivity(), newFragments)
        dotsIndicator.attachTo(explainViewPager)

        englishTitle.text = "Hiragana"
        japaneseTitle.text = "ひらがな"
        japaneseFooter.text = "あ　い　う　え　お"
    }

    private fun showKatakana() {
        val newFragments = mutableListOf<ExplainTextFragment>()
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.katakana_intro_1)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.katakana_intro_2)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.katakana_intro_3)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.katakana_intro_4)))
        explainViewPager.adapter = KanaPageAdapter(requireActivity(), newFragments)
        dotsIndicator.attachTo(explainViewPager)

        englishTitle.text = "Katakana"
        japaneseTitle.text = "カタカナ"
        japaneseFooter.text = "ア　イ　ウ　エ　オ"

    }

    private fun showKanji() {
        val newFragments = mutableListOf<ExplainTextFragment>()
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.kanji_intro_1)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.kanji_intro_2)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.kanji_intro_3)))
        newFragments.add(ExplainTextFragment.newInstance(getString(R.string.kanji_intro_4)))
        explainViewPager.adapter = KanaPageAdapter(requireActivity(), newFragments)
        dotsIndicator.attachTo(explainViewPager)

        englishTitle.text = "Kanji"
        japaneseTitle.text = "漢字"
        japaneseFooter.text = "日　月　山　本　天"
    }

    companion object {
        fun newInstance() : ExplainSlide {
            return ExplainSlide()
        }
    }
}