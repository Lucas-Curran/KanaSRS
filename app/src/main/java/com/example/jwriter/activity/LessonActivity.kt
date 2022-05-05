package com.example.jwriter.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaPlayer
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import com.example.jwriter.R
import com.example.jwriter.database.JWriterDatabase
import com.example.jwriter.database.Kana
import com.example.jwriter.util.KanaConverter
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

private var mPaint: Paint? = null

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

    private lateinit var rootView: RelativeLayout

    private var mPlayer = MediaPlayer()

    private val FIRST_KANA = 0
    private val KANA_LETTER_SCREEN = 0
    private val KANA_GIF_SCREEN = 1
    private val KANA_DRAW_SCREEN = 2

    private lateinit var buttonList: ArrayList<ImageButton>

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

        rootView = findViewById(R.id.rootView)

        buttonList = ArrayList()

        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.isDither = true
        mPaint!!.color = Color.WHITE
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeJoin = Paint.Join.ROUND
        mPaint!!.strokeCap = Paint.Cap.ROUND
        mPaint!!.strokeWidth = 12F

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

                newView.findViewById<ImageView>(R.id.kanaAudioImageView).setOnClickListener {
                    playAudio(kanaConverter._hiraganaToRomaji(kana.letter))
                }

                val itemTabLayout = newView.findViewById<TabLayout>(R.id.itemTabLayout)

                val constraintLayout = newView.findViewById<ConstraintLayout>(R.id.itemConstraintLayout)
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(newView.findViewById<RelativeLayout>(R.id.topRelativeLayout).id, ConstraintSet.BOTTOM, itemTabLayout.id, ConstraintSet.TOP)
                constraintSet.applyTo(constraintLayout)

                //Add two tabs for letter and for gif
                val letterTab = itemTabLayout.newTab()
                val gifTab = itemTabLayout.newTab()
                val writeTab = itemTabLayout.newTab()

                itemTabLayout.addTab(letterTab)
                itemTabLayout.addTab(gifTab)
                itemTabLayout.addTab(writeTab)

                val tempViewAnimator = newView.findViewById<ViewAnimator>(R.id.viewAnimator)
                val nextButton = newView.findViewById<ImageButton>(R.id.nextItemButton)
                val previousButton = newView.findViewById<ImageButton>(R.id.previousItemButton)

                buttonList.add(nextButton)
                buttonList.add(previousButton)

                val drawingView = DrawingView(this)
                drawingView.background = ContextCompat.getDrawable(this, R.drawable.pink_outline)

                tempViewAnimator.addView(drawingView)

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
                    //If on the last item and gif screen, execute code to finish lesson
                    if (rootViewAnimator.displayedChild == subList.lastIndex && tempViewAnimator.displayedChild == KANA_DRAW_SCREEN) {
                        val view = layoutInflater.inflate(R.layout.lesson_completed_dialog, null)
                        val builder = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()
                        builder.setCancelable(false)
                        //If restart button is clicked, set tab to first item
                        //Setting reset quiz to true will remove animation from final item flipping back to letter, i.e. fix UI glitch
                        view.findViewById<MaterialButton>(R.id.restartLessonButton).setOnClickListener {
                            lessonTabLayout.getTabAt(0)?.select()
                            rootViewAnimator.postDelayed({
                                resetQuiz = true
                                itemTabLayout.selectTab(letterTab)
                            }, rootViewAnimator.inAnimation.duration)
                            //Set the right button back to blue and having an arrow
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_right_arrow)
                                nextButton.setBackgroundColor(resources.getColor(R.color.azure, theme))
                            }
                            builder.dismiss()
                        }
                        //If begin button is clicked, go to review intent, but send quiz as true and send learned kana sublist
                        view.findViewById<MaterialButton>(R.id.beginQuizButton).setOnClickListener {
                            val intent = Intent(this, ReviewActivity::class.java)
                            intent.putExtra("quiz", true)
                            intent.putExtra("kana", subList.toTypedArray())
                            startActivity(intent)
                        }
                        builder.show()
                        startConfetti(rootView)

                    } else if (tempViewAnimator.displayedChild == KANA_DRAW_SCREEN) {
                        //If on gif screen, show next kana and switch current kana back to letter tab
                        disableButtons()
                        lessonTabLayout.getTabAt(newTab.position+1)?.select()
                        rootViewAnimator.postDelayed({
                            itemTabLayout.selectTab(letterTab)
                            enableButtons()
                        }, rootViewAnimator.inAnimation.duration)
                    } else {
                        //In case of the else, it switches from letter tab to gif tab or draw tab
                            //With new draw tab, if the next tab is draw, switch icon
                        //However if on the last item, set the right arrow icon to a checkmark and set the background to lime,
                            // to indicate end of lesson
                        if (rootViewAnimator.displayedChild == subList.lastIndex && (itemTabLayout.selectedTabPosition+1 == KANA_DRAW_SCREEN)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_checkmark)
                                nextButton.setBackgroundColor(resources.getColor(R.color.lime, theme))
                            }
                        }
                        itemTabLayout.getTabAt(itemTabLayout.selectedTabPosition+1)?.select()
                    }
                }

                previousButton.setOnSingleClickListener {
                    //If on the first kana and also on the letter tab, then don't allow for previous button to do anything
                    if (rootViewAnimator.displayedChild == FIRST_KANA && tempViewAnimator.displayedChild == KANA_LETTER_SCREEN) {
                        return@setOnSingleClickListener
                    }
                    //If on the gif screen, then go back to the letter tab. Else, it means they are on letter screen, so go to the previous kana
                    if (tempViewAnimator.displayedChild != KANA_LETTER_SCREEN) {
                        if (rootViewAnimator.displayedChild == subList.lastIndex) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                nextButton.setImageResource(R.drawable.ic_right_arrow)
                                nextButton.setBackgroundColor(resources.getColor(R.color.azure, theme))
                            }
                        }
                        itemTabLayout.getTabAt(itemTabLayout.selectedTabPosition-1)?.select()
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

    private fun playAudio(letter: String) {
        if (!mPlayer.isPlaying) {
            mPlayer = MediaPlayer.create(this, resources.getIdentifier(letter, "raw", packageName))
            mPlayer.start()
        }
    }

    private fun startConfetti(container: ViewGroup) {
        CommonConfetti.rainingConfetti(
            container, intArrayOf(
                Color.BLACK, Color.BLUE, Color.CYAN, Color.YELLOW, Color.RED, Color.GREEN
            )
        ).oneShot().setVelocityY(600F, 100F).animate()
    }

    private fun disableButtons() {
        for (button in buttonList) {
            button.isEnabled = false
        }
    }

    private fun enableButtons() {
        for (button in buttonList) {
            button.isEnabled = true
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

    class DrawingView(var c: Context) : View(c) {

        private var mBitmap: Bitmap? = null
        private var mCanvas: Canvas? = null
        private val mPath: Path = Path()
        private val mBitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
        private val circlePaint: Paint = Paint()
        private val circlePath: Path = Path()

        init {
            circlePaint.isAntiAlias = true
            circlePaint.color = Color.BLUE
            circlePaint.style = Paint.Style.STROKE
            circlePaint.strokeJoin = Paint.Join.MITER
            circlePaint.strokeWidth = 4f
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(mBitmap!!)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawBitmap(mBitmap!!, 0F, 0F, mBitmapPaint)
            canvas.drawPath(mPath, mPaint!!)
            canvas.drawPath(circlePath, circlePaint)
        }

        private var mX = 0f
        private var mY = 0f
        private fun touchStart(x: Float, y: Float) {
            mPath.reset()
            mPath.moveTo(x, y)
            mX = x
            mY = y
        }

        private fun touchMove(x: Float, y: Float) {
            val dx = Math.abs(x - mX)
            val dy = Math.abs(y - mY)
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                mX = x
                mY = y
                circlePath.reset()
                circlePath.addCircle(mX, mY, 30F, Path.Direction.CW)
            }
        }

        private fun touchUp() {
            mPath.lineTo(mX, mY)
            circlePath.reset()
            // commit the path to our offscreen
            mCanvas?.drawPath(mPath, mPaint!!)
            // kill this so we don't double draw
            mPath.reset()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStart(x, y)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    touchMove(x, y)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    touchUp()
                    invalidate()
                }
            }
            return true
        }

        companion object {
            private const val TOUCH_TOLERANCE = 4f
        }
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