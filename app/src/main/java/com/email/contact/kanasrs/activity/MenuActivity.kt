package com.email.contact.kanasrs.activity

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.postOnAnimationDelayed
import com.airbnb.lottie.LottieAnimationView
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.database.KanaSRSDatabase
import com.email.contact.kanasrs.database.Kana
import com.email.contact.kanasrs.database.User
import com.email.contact.kanasrs.util.Utilities
import com.email.contact.kanasrs.util.Utilities.Companion.colorizeText
import com.email.contact.kanasrs.util.Utilities.Companion.createShowcase
import com.email.contact.kanasrs.util.Utilities.Companion.createShowcaseRectangle
import com.email.contact.kanasrs.util.Utilities.Companion.disable
import com.email.contact.kanasrs.util.Utilities.Companion.disableScroll
import com.email.contact.kanasrs.util.Utilities.Companion.enableScroll
import com.email.contact.kanasrs.util.Utilities.Companion.formatTime
import me.toptas.fancyshowcase.FancyShowCaseQueue
import me.toptas.fancyshowcase.FancyShowCaseView
import me.toptas.fancyshowcase.listener.OnCompleteListener


class MenuActivity : AppCompatActivity() {

    private lateinit var db: KanaSRSDatabase

    private lateinit var reviewButton: Button
    private lateinit var statsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var lessonButton: Button
    private lateinit var summaryLayout: RelativeLayout
    private lateinit var levelsLayout: LinearLayout
    private lateinit var showMoreArrow: ImageView
    private lateinit var summaryButton: Button
    private lateinit var currentReviewsTextView: TextView
    private lateinit var remainingLessonsTextView: TextView
    private lateinit var menuScrollView: ScrollView
    private lateinit var githubImageView: ImageView
    private lateinit var contactImageView: ImageView
    private lateinit var writingReviewButton: Button

    private lateinit var showcaseQueue: FancyShowCaseQueue

    private var showMore = true
    private var moving = false
    private var numItemsToReview: Int = 0
    private var numHiraganaMastered: Float = 0f
    private var numKatakanaMastered: Float = 0f
    private var kanaToReview = ArrayList<Kana>()
    private var levelsArray = ArrayList<View>()
    private var levelsItemArray = IntArray(5)

    private val ROOKIE = 0
    private val AMATEUR = 1
    private val EXPERT = 2
    private val MASTER = 3
    private val SENSEI = 4

    val MotionEvent.up get() = action == MotionEvent.ACTION_UP

    private fun MotionEvent.isIn(view: View): Boolean {
        val rect = Rect(view.left, view.top, view.right, view.bottom)
        return rect.contains((view.left + x).toInt(), (view.top + y).toInt())
    }

    private val refreshString = "The refresh button is useful for staying up-to-date on all new reviews and lessons in your queue!"
    private val summaryString = "Here is the summary!"
    private val lessonString = "To begin learning new kana, click here"

    private lateinit var refreshShowcase: FancyShowCaseView
    private lateinit var summaryShowcase: FancyShowCaseView
    private lateinit var lessonShowcase: FancyShowCaseView

    private lateinit var nextReviewTime: TextView
    private lateinit var numReviewTextView: TextView

    private var kanaReviewQueue = mutableListOf<Kana>()

    private val showcaseArray = mutableListOf<FancyShowCaseView>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        db = KanaSRSDatabase.getInstance(this)

        val user = db.userDao().getUser()
        for (kana in db.kanaDao().getKana()) {
            //Check if there is a review time, and if so, check if the current time has passed the stored review time
            // Review time is calculated during review answers and initially added when learned in lessons
            if (kana.reviewTime != null) {
                if (kana.reviewTime!! < System.currentTimeMillis()) {
                    numItemsToReview++
                    kanaToReview.add(kana)
                } else {
                    kanaReviewQueue.add(kana)
//                    val millisecondsUntilReview = kana.reviewTime!! - System.currentTimeMillis()
//                    if (millisecondsUntilReview < mostRecentReview) {
//                        mostRecentReview = millisecondsUntilReview
//                        nextKanaToReview = kana.letter!!
//                    }
                }
            }
            if (kana.level == Utilities.SENSEI && kana.isHiragana) {
                numHiraganaMastered++
            } else if (kana.level == Utilities.SENSEI && !kana.isHiragana) {
                numKatakanaMastered++
            }
        }

        kanaReviewQueue.sortBy { it.reviewTime }

        setContentView(R.layout.activity_menu)

        Utilities.setAlarm(this)

        numReviewTextView = findViewById(R.id.numItemsTextView)
        numReviewTextView.text = numItemsToReview.toString()

        nextReviewTime = findViewById(R.id.nextReviewText)
        if (kanaReviewQueue.isNotEmpty()) {
            startReviewTimer(kanaReviewQueue[0])
        } else {
            nextReviewTime.text = "Next review: none"
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        currentReviewsTextView = findViewById(R.id.currentReviewsTextView)
        remainingLessonsTextView = findViewById(R.id.remainingLessonsTextView)

        checkLessonTimer(user)
        currentReviewsTextView.text = "Current Reviews: $numItemsToReview".colorizeText(
            numItemsToReview.toString(),
            ContextCompat.getColor(this, R.color.azure)
        )

        summaryButton = findViewById(R.id.summaryButton)
        showMoreArrow = findViewById(R.id.arrowImageView)
        summaryLayout = findViewById(R.id.summaryRelativeLayout)
        levelsLayout = findViewById(R.id.levelsLayout)

        val rookie = levelsLayout.findViewById<LinearLayout>(R.id.rookie)
        val amateur = levelsLayout.findViewById<LinearLayout>(R.id.amateur)
        val expert = levelsLayout.findViewById<LinearLayout>(R.id.expert)
        val master = levelsLayout.findViewById<LinearLayout>(R.id.master)
        val sensei = levelsLayout.findViewById<LinearLayout>(R.id.sensei)
        levelsArray.add(rookie)
        levelsArray.add(amateur)
        levelsArray.add(expert)
        levelsArray.add(master)
        levelsArray.add(sensei)

        for (kana in db.kanaDao().getKana()) {
            if (kana.level != null) {
                when (kana.level) {
                    1, 2 -> levelsItemArray[ROOKIE] += 1
                    3 -> levelsItemArray[AMATEUR] += 1
                    4 -> levelsItemArray[EXPERT] += 1
                    5 -> levelsItemArray[MASTER] += 1
                    6 -> levelsItemArray[SENSEI] += 1
                }
            }
        }

        for ((index, level) in levelsArray.withIndex()) {
            val name = level.context.resources.getResourceEntryName(level.id)
            level.findViewById<TextView>(
                resources.getIdentifier(
                    "${name}NumKanaTextView",
                    "id",
                    packageName
                )
            ).text = levelsItemArray[index].toString()
            level.setOnClickListener {
                val intent = Intent(this, KanaGridActivity::class.java)
                intent.putExtra("level", name)
                startActivity(intent)
            }
        }

        showMoreArrow.setOnClickListener {

            showMoreArrow.isSelected = !showMoreArrow.isSelected

            if (showMore && !moving) {
                moving = true
                showMoreArrow.isEnabled = false
                Utilities.slideView(
                    summaryLayout,
                    summaryLayout.height,
                    summaryLayout.height + levelsLayout.measuredHeight + levelsLayout.marginBottom

                ) {}
                Utilities.slideView(
                    summaryButton,
                    summaryButton.height,
                    summaryButton.height + levelsLayout.measuredHeight + levelsLayout.marginBottom
                ) {
                    showMore = false
                    moving = false
                    showMoreArrow.isEnabled = true
                }
                showMoreArrow.setImageResource(R.drawable.ic_up_arrow_wide)
            } else if (!showMore && !moving) {
                moving = true
                showMoreArrow.isEnabled = false
                Utilities.slideView(
                    summaryLayout,
                    summaryLayout.height,
                    summaryLayout.height - levelsLayout.measuredHeight - levelsLayout.marginBottom
                ) {}
                Utilities.slideView(
                    summaryButton,
                    summaryButton.height,
                    summaryButton.height - levelsLayout.measuredHeight - levelsLayout.marginBottom
                ) {
                    moving = false
                    showMore = true
                    showMoreArrow.isEnabled = true
                }
                showMoreArrow.setImageResource(R.drawable.ic_down_arrow_wide)
            }
        }

        lessonButton = findViewById(R.id.lessonButton)
        lessonButton.setOnClickListener {
            if (user.lessonsNumber!! > 0) {
                startActivity(Intent(this, LessonActivity::class.java))
            } else {
                Toast.makeText(
                    this,
                    "You can do more lessons in ${formatTime(user.lessonRefreshTime!! - System.currentTimeMillis())}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val refreshTimeText = findViewById<TextView>(R.id.lessonRefreshTime)
        if (user.lessonRefreshTime != null) {
            refreshTimeText.visibility = View.VISIBLE
            object : CountDownTimer(user.lessonRefreshTime!! - System.currentTimeMillis(), 1000) {
                override fun onTick(millisLeft: Long) {
                    refreshTimeText.text = "Lessons refresh in ${formatTime(millisLeft)}"
                }

                override fun onFinish() {
                    val sharedPref = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)
                    val lessonNumber = sharedPref.getInt("jwriterLessonNumber", 10)
                    if (db.kanaDao().getUnlearnedKana().size in 1..9) {
                        user.lessonsNumber = db.kanaDao().getUnlearnedKana().size
                    } else {
                        user.lessonsNumber = lessonNumber
                    }
                    user.lessonRefreshTime = null
                    refreshTimeText.visibility = View.INVISIBLE
                    remainingLessonsTextView.text = "Daily remaining lessons: $lessonNumber".colorizeText(user.lessonsNumber.toString(), ContextCompat.getColor(this@MenuActivity, R.color.pink))
                    db.userDao().updateUser(user)
                }

            }.start()
//            refreshTimeText.text =
//                "Lessons refresh in ${formatTime(user.lessonRefreshTime!! - System.currentTimeMillis())}"
        }

        reviewButton = findViewById(R.id.reviewButton)

        if (numItemsToReview == 0) {
            reviewButton.disable()
            numReviewTextView.background =
                ContextCompat.getDrawable(this, R.drawable.no_review_items_background)
        }

        if (user.lessonsNumber == 0) {
            lessonButton.disable()
        }

        reviewButton.setOnClickListener {
            if (numItemsToReview > 0) {
                val intent = Intent(this, ReviewActivity::class.java)
                intent.putExtra("review", true)
                intent.putExtra("kana", kanaToReview)
                startActivity(intent)
            }
        }

        statsButton = findViewById(R.id.statsButton)
        statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        contactImageView = findViewById(R.id.emailContactImageView)
        contactImageView.setOnClickListener {
            showReportDialog()
        }

        githubImageView = findViewById(R.id.githubContactImageView)
        githubImageView.setOnClickListener {
            val githubIntent = Intent(
                "android.intent.action.VIEW",
                Uri.parse("https://github.com/Lucas-Curran/KanaSRS")
            )
            startActivity(githubIntent)
        }

        writingReviewButton = findViewById(R.id.writingReviewButton)
        writingReviewButton.setOnClickListener {

        }

        if (intent.getBooleanExtra("openKanaNotification", false) && numItemsToReview > 0) {
            intent.removeExtra("openKanaNotification")
            val view = layoutInflater.inflate(R.layout.notification_review_dialog, null)
            val dialog = AlertDialog.Builder(this).setView(view).create()

            view.findViewById<TextView>(R.id.beginReviewTextView).setOnClickListener {
                val intent = Intent(this, ReviewActivity::class.java)
                intent.putExtra("review", true)
                intent.putExtra("kana", kanaToReview)
                startActivity(intent)
            }
            view.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        menuScrollView = findViewById(R.id.menuScrollView)
        showcaseQueue = FancyShowCaseQueue()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        Handler(Looper.getMainLooper()).postDelayed(
            {
                showcaseArray.add(createShowcase(
                    findViewById(R.id.refresh),
                    refreshString,
                    false,
                    this
                ))
                showcaseArray.add(createShowcaseRectangle(
                    summaryLayout,
                    summaryString,
                    false,
                    this
                ))
                showcaseArray.add(createShowcaseRectangle(
                    lessonButton,
                    lessonString,
                    true,
                    this
                ))

                for (showcase in showcaseArray) {
                    showcaseQueue.add(showcase)
                }

                //showcaseQueue.show()
                if (showcaseQueue.current != null) {
                    menuScrollView.disableScroll()
                }
                showcaseQueue.completeListener = object : OnCompleteListener {
                    override fun onComplete() {
                        menuScrollView.enableScroll()
                    }
                }

            }, 50)
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.report -> showReportDialog()
            R.id.faq -> showFAQ()
            R.id.refresh -> refreshActivity()
            R.id.tutorial -> {
                for (showcase in showcaseArray) {
                    showcaseQueue.add(showcase)
                }
                showcaseQueue.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startReviewTimer(kana: Kana) {
        object : CountDownTimer(kana.reviewTime!! - System.currentTimeMillis(), 1000) {
            override fun onTick(millisLeft: Long) {
                nextReviewTime.text =
                    "Next review: \n\t\t${formatTime(millisLeft)}"
            }

            override fun onFinish() {
                kanaReviewQueue.removeFirstOrNull()
                numItemsToReview++
                currentReviewsTextView.text = "Current Reviews: $numItemsToReview".colorizeText(
                    numItemsToReview.toString(),
                    ContextCompat.getColor(this@MenuActivity, R.color.azure)
                )
                numReviewTextView.text = numItemsToReview.toString()
                kanaToReview.add(kana)
                if (kanaReviewQueue.isEmpty()) {
                    nextReviewTime.text = "Next review: none"
                } else {
                    startReviewTimer(kanaReviewQueue[0])
                }
            }
        }.start()
    }

    private fun refreshActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun showFAQ() {
        startActivity(Intent(this, FaqActivity::class.java))
    }

    private fun showReportDialog() {
        val view = layoutInflater.inflate(R.layout.report_dialog, null)
        val builder = AlertDialog.Builder(this, R.style.DialogTheme).setView(view).create()
        builder.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val contactButton = view.findViewById<LottieAnimationView>(R.id.reportButton)

        contactButton.setOnClickListener {

            contactButton.playAnimation()

            contactButton.postOnAnimationDelayed(contactButton.duration / 2) {
                val text = view.findViewById<TextView>(R.id.reportTextView).text
                val email = Intent(Intent.ACTION_VIEW)
                val data =
                    Uri.parse("mailto:?subject=KanaSRS email&body=$text&to=contact.kanasrs@gmail.com")
                email.data = data
                try {
                    startActivity(Intent.createChooser(email, "Send mail..."))
                    finish()
                } catch (ex: ActivityNotFoundException) {
                    Toast.makeText(
                        this,
                        "There is no email client installed.", Toast.LENGTH_SHORT
                    ).show()
                }

                builder.dismiss()
            }
        }
        builder.show()

    }

    private fun checkLessonTimer(user: User) {
        val sharedPref = getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)
        val lessonNumber = sharedPref.getInt("jwriterLessonNumber", 10)
        if (user.lessonRefreshTime != null) {
            //If current clock time is greater than time set to refresh, reset lesson number and make refresh time null
            if (System.currentTimeMillis() > user.lessonRefreshTime!!) {
                if (db.kanaDao().getUnlearnedKana().size in 1..9) {
                    user.lessonsNumber = db.kanaDao().getUnlearnedKana().size
                } else {
                    user.lessonsNumber = lessonNumber
                }
                user.lessonRefreshTime = null
                db.userDao().updateUser(user)
            }
        } else {
            //If the current remaining kana number is between 1-9, make lesson number the unlearned kana size
            if (db.kanaDao().getUnlearnedKana().size in 1 until lessonNumber) {
                user.lessonsNumber = db.kanaDao().getUnlearnedKana().size
            } else {
                user.lessonsNumber = lessonNumber
            }
            db.userDao().updateUser(user)
        }
        remainingLessonsTextView.text = "Daily remaining lessons: ${user.lessonsNumber}".colorizeText(user.lessonsNumber.toString(), ContextCompat.getColor(this, R.color.pink))
    }

    override fun onRestart() {
        super.onRestart()
        refreshActivity()
    }

}