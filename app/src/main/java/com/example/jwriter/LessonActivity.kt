package com.example.jwriter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.get
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import kotlin.concurrent.thread

class LessonActivity : AppCompatActivity() {

    private lateinit var rootViewAnimator: ViewAnimator
    var animationTime = 0L
    private lateinit var kanaList: List<Kana>
    private lateinit var loadingBar: ProgressBar
    private lateinit var lessonTabLayout: TabLayout

    private lateinit var animationIn: Animation
    private lateinit var  animationOut: Animation
    private lateinit var  prevAnimIn: Animation
    private lateinit var  prevAnimOut: Animation

    private val FIRST_KANA = 0
    private val KANA_LETTER_SCREEN = 0
    private val KANA_GIF_SCREEN = 1

    private var resetQuiz = false

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        rootViewAnimator = findViewById(R.id.rootViewAnimator)
        loadingBar = findViewById(R.id.lessonProgressBar)
        lessonTabLayout = findViewById(R.id.lessonTabLayout)

        animationIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        animationOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        prevAnimIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        prevAnimOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)

        lessonTabLayout.addOnTabSelectedListener( object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val tabPosition = tab?.position!!
                if (rootViewAnimator.displayedChild < tabPosition) { setNextAnim(rootViewAnimator) } else { setPrevAnim(rootViewAnimator) }
                rootViewAnimator.displayedChild = tabPosition
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })


        animationTime = animationIn.duration

        setNextAnim(rootViewAnimator)

        kanaList = JWriterDatabase.getInstance(this).kanaDao().getUnlearnedKana()
        val kanaConverter = KanaConverter(false)

        val subList = kanaList.subList(0, 5)

        Handler(Looper.getMainLooper()).post {

            for ((index, kana) in subList.withIndex()) {

                val newTab = lessonTabLayout.newTab()
                //Make touch do nothing so user can't skip through lesson
                newTab.view.setOnTouchListener { view, motionEvent ->
                    true
                }
                lessonTabLayout.addTab(newTab)

                val newView = layoutInflater.inflate(R.layout.lesson_item, null)

                newView.findViewById<TextView>(R.id.kanaTextView).text = kana.letter
                newView.findViewById<TextView>(R.id.englishTextView).text =
                    kanaConverter._hiraganaToRomaji(kana.letter)

                val itemTabLayout = newView.findViewById<TabLayout>(R.id.itemTabLayout)

                //Add two tabs for letter and for gif
                val letterTab = itemTabLayout.newTab()
                val gifTab = itemTabLayout.newTab()

                itemTabLayout.addTab(letterTab)
                itemTabLayout.addTab(gifTab)

                val tempViewAnimator = newView.findViewById<ViewAnimator>(R.id.viewAnimator)
                val nextButton = newView.findViewById<ImageButton>(R.id.nextItemButton)
                val previousButton = newView.findViewById<ImageButton>(R.id.previousItemButton)

                setNextAnim(tempViewAnimator)

                val tempKanaWebStroke = newView.findViewById<WebView>(R.id.strokeWebView)
                tempKanaWebStroke.settings.javaScriptEnabled = true
                tempKanaWebStroke.webViewClient = WebViewClient()
                tempKanaWebStroke.loadUrl(kana.gif)

                itemTabLayout.addOnTabSelectedListener( object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        val tabPosition = tab?.position!!
                        if (tempViewAnimator.displayedChild < tabPosition) { setNextAnim(tempViewAnimator) } else { setPrevAnim(tempViewAnimator) }
                        //If lesson tab is current tab + 1, i.e. going to the next tab, then set animation of current tab to null so no UI glitch
                        //Or if quiz is resetting, set anim null to avoid UI glitch
                        if (lessonTabLayout.selectedTabPosition == newTab.position+1 || resetQuiz) {
                            setAnimNull(tempViewAnimator)
                            resetQuiz = false
                        }
                        tempViewAnimator.displayedChild = tabPosition
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }

                })

                nextButton.setOnSingleClickListener {

                    setNextAnim(rootViewAnimator)
                    if (rootViewAnimator.displayedChild == subList.lastIndex && tempViewAnimator.displayedChild == KANA_GIF_SCREEN) {
                        val view = layoutInflater.inflate(R.layout.lesson_completed_dialog, null)
                        val builder = AlertDialog.Builder(this).setView(view).create()
                        view.findViewById<MaterialButton>(R.id.restartLessonButton).setOnClickListener {
                            lessonTabLayout.getTabAt(0)?.select()
                            rootViewAnimator.postDelayed({
                                resetQuiz = true
                                itemTabLayout.selectTab(letterTab)
                            }, rootViewAnimator.inAnimation.duration)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_right_arrow)
                                nextButton.setBackgroundColor(resources.getColor(R.color.azure, theme))
                            }
                            builder.dismiss()
                        }
                        view.findViewById<MaterialButton>(R.id.beginQuizButton).setOnClickListener {
                            val intent = Intent(this, ReviewActivity::class.java)
                            intent.putExtra("quiz", true)
                            intent.putExtra("kana", subList.toTypedArray())
                            startActivity(intent)
                        }
                        builder.show()
                    } else if (tempViewAnimator.displayedChild == KANA_GIF_SCREEN) {
                        lessonTabLayout.getTabAt(newTab.position+1)?.select()
                        rootViewAnimator.postDelayed({
                            itemTabLayout.selectTab(letterTab)
                        }, rootViewAnimator.inAnimation.duration)
                    } else {
                        if (rootViewAnimator.displayedChild == subList.lastIndex) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_checkmark)
                                nextButton.setBackgroundColor(resources.getColor(R.color.lime, theme))
                            }
                        }
                        itemTabLayout.selectTab(gifTab)
                    }
                }

                previousButton.setOnSingleClickListener {
                    //If on the first kana and also on the letter tab, then don't allow for previous button to do anything
                    if (rootViewAnimator.displayedChild == FIRST_KANA && tempViewAnimator.displayedChild == KANA_LETTER_SCREEN) {
                        return@setOnSingleClickListener
                    }
                    //If on the gif screen, then go back to the letter tab. Else, it means they are on letter screen, so go to the previous kana
                    if (tempViewAnimator.displayedChild == KANA_GIF_SCREEN) {
                        if (rootViewAnimator.displayedChild == subList.lastIndex) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_right_arrow)
                                nextButton.setBackgroundColor(resources.getColor(R.color.azure, theme))
                            }
                        }
                        itemTabLayout.selectTab(letterTab)
                    } else {
                        lessonTabLayout.getTabAt(newTab.position-1)?.select()
                    }
                }

                //Add view of kana item
                rootViewAnimator.addView(newView)

                //If last index in the kana sublist, remove the loading spinner
                if (index == subList.lastIndex) {
                    lessonTabLayout.visibility = View.VISIBLE
                    loadingBar.visibility = View.INVISIBLE
                    rootViewAnimator.removeView(loadingBar)
                }
            }
        }
    }

    private fun setAnimNull(viewAnimator: ViewAnimator) {
        viewAnimator.inAnimation = null
        viewAnimator.outAnimation = null
    }

    private fun setNextAnim(viewAnimator: ViewAnimator) {
        viewAnimator.inAnimation = animationIn
        viewAnimator.outAnimation = animationOut
    }

    private fun setPrevAnim(viewAnimator: ViewAnimator) {
        viewAnimator.inAnimation = prevAnimIn
        viewAnimator.outAnimation = prevAnimOut
    }

}

class OnSingleClickListener(private val block: () -> Unit) : View.OnClickListener {

    private var lastClickTime = 0L

    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < 500) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()
        block()
    }
}

fun View.setOnSingleClickListener(block: () -> Unit) {
    setOnClickListener(OnSingleClickListener(block))
}