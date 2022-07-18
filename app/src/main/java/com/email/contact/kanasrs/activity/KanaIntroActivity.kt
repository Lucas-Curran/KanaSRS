package com.email.contact.kanasrs.activity

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.slides.FinalIntroSlide
import com.email.contact.kanasrs.slides.FirstIntroSlide
import com.email.contact.kanasrs.slides.SecondIntroSlide
import com.email.contact.kanasrs.slides.ThirdIntroSlide
import com.email.contact.kanasrs.util.Utilities.Companion.colorizeText
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroCustomLayoutFragment
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType

class KanaIntroActivity : AppIntro2() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(FirstIntroSlide.newInstance())
        addSlide(SecondIntroSlide.newInstance())
        addSlide(ThirdIntroSlide.newInstance())
        addSlide(FinalIntroSlide.newInstance())

        setIndicatorColor(
            selectedIndicatorColor = ContextCompat.getColor(this, R.color.pink),
            unselectedIndicatorColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        )

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
        // Decide what to do when the user clicks on "Done"
        finish()
    }
}