package com.email.contact.kanasrs.activity

import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.slides.*
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroPageTransformerType

class KanaIntroActivity : AppIntro2() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(IntroSlide.newInstance())
        addSlide(TransitionSlide.newInstance())
        addSlide(ExplainSlide.newInstance())
        addSlide(ExitSlide.newInstance())

        setIndicatorColor(
            selectedIndicatorColor = ContextCompat.getColor(this, R.color.pink),
            unselectedIndicatorColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        )

        setSwipeLock(true)
        setTransformer(AppIntroPageTransformerType.Depth)

        isWizardMode = true
        setImmersiveMode()

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        val sharedPref = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("needsToCompleteIntro", false)
            apply()
        }
        finish()
    }
}