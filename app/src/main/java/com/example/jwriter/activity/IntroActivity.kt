package com.example.jwriter.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.jwriter.MenuIntroFragment
import com.example.jwriter.R
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroPageTransformerType

class IntroActivity : AppIntro2() {

    private lateinit var menuIntroFragment: MenuIntroFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransformer(AppIntroPageTransformerType.Flow)

        isIndicatorEnabled = true
        isVibrate = true
        vibrateDuration = 50L

        setIndicatorColor(
            selectedIndicatorColor = R.color.white,
            unselectedIndicatorColor = android.R.color.darker_gray
        )

        menuIntroFragment = MenuIntroFragment.newInstance()

        addSlide(menuIntroFragment)

    }

    override fun onBackPressed() {
        menuIntroFragment.previousSpotlight()
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finish()
    }

    override fun onNextPressed(currentFragment: Fragment?) {
        super.onNextPressed(currentFragment)
    }
}