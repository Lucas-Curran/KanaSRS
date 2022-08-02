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
import android.view.*
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
import com.email.contact.kanasrs.util.Utilities.Companion.disable
import com.email.contact.kanasrs.util.Utilities.Companion.formatTime
import com.email.contact.kanasrs.util.Utilities.Companion.reviewApp
import com.google.android.material.tabs.TabLayout

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
    private lateinit var currentWritingReviews: TextView
    private lateinit var nextWritingReviewText: TextView
    private lateinit var remainingLessonsTextView: TextView
    private lateinit var menuScrollView: ScrollView
    private lateinit var githubImageView: ImageView
    private lateinit var contactImageView: ImageView
    private lateinit var writingReviewButton: Button
    private lateinit var reviewAnimation: LottieAnimationView
    private lateinit var lessonAnimation: LottieAnimationView

    private var showMore = true
    private var moving = false
    private var numItemsToReview: Int = 0
    private var numItemsToWrite: Int = 0
    private var kanaToReview = ArrayList<Kana>()
    private var kanaToWrite = ArrayList<Kana>()
    private var levelsArray = ArrayList<View>()
    private var levelsItemArray = IntArray(5)

    private var writingLevelsArray = IntArray(5)

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

    private lateinit var nextReviewTime: TextView
    private lateinit var numReviewTextView: TextView
    private lateinit var numWriteTextView: TextView

    private var kanaReviewQueue = mutableListOf<Kana>()
    private var kanaWritingQueue = mutableListOf<Kana>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        val sharedPref =
            getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)

        if (sharedPref.getBoolean("needsToCompleteIntro", true)) {
            startActivity(Intent(this, KanaIntroActivity::class.java))
        } else {

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
                    }
                }
                if (kana.writingReviewTime != null) {
                    if (kana.writingReviewTime!! < System.currentTimeMillis()) {
                        numItemsToWrite++
                        kanaToWrite.add(kana)
                    } else {
                        kanaWritingQueue.add(kana)
                    }
                }
            }

            kanaReviewQueue.sortBy { it.reviewTime }
            kanaWritingQueue.sortBy { it.writingReviewTime }

            setContentView(R.layout.activity_menu)

            Utilities.setAlarm(this)

            numReviewTextView = findViewById(R.id.numItemsTextView)
            numReviewTextView.text = numItemsToReview.toString()
            numWriteTextView = findViewById(R.id.numWritingTextView)
            numWriteTextView.text = numItemsToWrite.toString()

            nextReviewTime = findViewById(R.id.nextReviewText)
            nextWritingReviewText = findViewById(R.id.nextReviewWritingText)
            writingReviewButton = findViewById(R.id.writingReviewButton)
            currentWritingReviews = findViewById(R.id.currentWritingReviewsText)

            if (kanaReviewQueue.isNotEmpty()) {
                startReviewTimer(kanaReviewQueue[0])
            } else {
                nextReviewTime.text = "Next review: \nnone".colorizeText("none", ContextCompat.getColor(this@MenuActivity, R.color.azure))
            }

            if (kanaWritingQueue.isNotEmpty()) {
                startWritingTimer(kanaWritingQueue[0])
            } else {
                nextWritingReviewText.text = "Next review: \nnone".colorizeText("none", ContextCompat.getColor(this@MenuActivity, R.color.writing_green))
            }


            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            currentReviewsTextView = findViewById(R.id.currentReviewsTextView)
            remainingLessonsTextView = findViewById(R.id.remainingLessonsTextView)

            checkLessonTimer(user)
            currentReviewsTextView.text = "Current: $numItemsToReview".colorizeText(
                numItemsToReview.toString(),
                ContextCompat.getColor(this, R.color.azure)
            )

            currentWritingReviews.text = "Current: $numItemsToWrite".colorizeText(
                numItemsToWrite.toString(),
                ContextCompat.getColor(this, R.color.writing_green)
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
                if (kana.level != null && kana.writingLevel != null) {
                    when (kana.level) {
                        1, 2 -> levelsItemArray[ROOKIE] += 1
                        3 -> levelsItemArray[AMATEUR] += 1
                        4 -> levelsItemArray[EXPERT] += 1
                        5 -> levelsItemArray[MASTER] += 1
                        6 -> levelsItemArray[SENSEI] += 1
                    }
                    when (kana.writingLevel) {
                        1, 2 -> writingLevelsArray[ROOKIE] += 1
                        3 -> writingLevelsArray[AMATEUR] += 1
                        4 -> writingLevelsArray[EXPERT] += 1
                        5 -> writingLevelsArray[MASTER] += 1
                        6 -> writingLevelsArray[SENSEI] += 1
                    }
                }
            }

            setLevelsFunction(false)

            val tabLayout = levelsLayout.findViewById<TabLayout>(R.id.levelsTabs)

            for (i in 0 until tabLayout.tabCount) {
                val tab = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
                val p = tab.layoutParams as ViewGroup.MarginLayoutParams
                p.setMargins(15, 0, 15, 0)
                tab.requestLayout()
            }

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> setLevelsFunction(isWriting = false)
                        1 -> setLevelsFunction(isWriting = true)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }

            })

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
                object :
                    CountDownTimer(user.lessonRefreshTime!! - System.currentTimeMillis(), 1000) {
                    override fun onTick(millisLeft: Long) {
                        refreshTimeText.text = "Lessons refresh in ${formatTime(millisLeft)}"
                    }

                    override fun onFinish() {
                        val sharedPref =
                            getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)
                        val lessonNumber = sharedPref.getInt("kanasrsLessonNumber", 10)
                        if (db.kanaDao().getUnlearnedKana().size in 1..9) {
                            user.lessonsNumber = db.kanaDao().getUnlearnedKana().size
                        } else {
                            user.lessonsNumber = lessonNumber
                        }
                        user.lessonRefreshTime = null
                        refreshTimeText.visibility = View.INVISIBLE
                        remainingLessonsTextView.text =
                            "Daily remaining lessons: $lessonNumber".colorizeText(
                                user.lessonsNumber.toString(),
                                ContextCompat.getColor(this@MenuActivity, R.color.pink)
                            )
                        db.userDao().updateUser(user)

                        if (!lessonButton.isEnabled) {
                            lessonButton.isEnabled = true
                            lessonButton.isClickable = true
                            lessonAnimation.setAnimation(R.raw.bulb)
                            lessonAnimation.playAnimation()
                        }

                    }

                }.start()
            }

            reviewButton = findViewById(R.id.reviewButton)
            reviewAnimation = findViewById(R.id.reviewAnimation)
            lessonAnimation = findViewById(R.id.lessonAnimation)

            if (numItemsToReview == 0) {
                reviewButton.disable()
                reviewAnimation.setAnimation(R.raw.quiz_disabled)
                numReviewTextView.background =
                    ContextCompat.getDrawable(this, R.drawable.no_review_items_background)
            } else {
                reviewAnimation.playAnimation()
            }

            if (user.lessonsNumber == 0) {
                lessonAnimation.setAnimation(R.raw.bulb_disabled)
                lessonButton.disable()
            } else {
                lessonAnimation.playAnimation()
            }

            if (numItemsToWrite == 0) {
                writingReviewButton.disable()
                numWriteTextView.background =
                    ContextCompat.getDrawable(this, R.drawable.no_review_items_background)
            } else {
                findViewById<LottieAnimationView>(R.id.writingAnimation).playAnimation()
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

            val sharedPref =
                getSharedPreferences(getString(R.string.pref_key), Context.MODE_PRIVATE)

            if (sharedPref.getBoolean("kanasrsWritingEnabled", true)) {
                writingReviewButton.setOnClickListener {
                    if (numItemsToWrite > 0) {

                        val intent = Intent(this, WritingActivity::class.java)
                        val kanaChoiceView = layoutInflater.inflate(R.layout.pick_kana_layout, null)

                        val dialog = AlertDialog.Builder(this, R.style.DialogTheme).setView(kanaChoiceView).create()
                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                        val hiraganaKana = ArrayList(kanaToWrite.filter { it.isHiragana })
                        val katakanaKana = ArrayList(kanaToWrite.filter { !it.isHiragana })

                        val hiraganaButton = kanaChoiceView.findViewById<Button>(R.id.hiraganaButton)
                        val katakanaButton = kanaChoiceView.findViewById<Button>(R.id.katakanaButton)

                        if (hiraganaKana.isEmpty()) {
                            hiraganaButton.disable()
                        }

                        if (katakanaKana.isEmpty()) {
                            katakanaButton.disable()
                        }

                        hiraganaButton.setOnClickListener {
                            intent.putExtra("kanaWriting", hiraganaKana)
                            startActivity(intent)
                        }

                        katakanaButton.setOnClickListener {
                            intent.putExtra("kanaWriting", katakanaKana)
                            startActivity(intent)
                        }

                        kanaChoiceView.findViewById<TextView>(R.id.hiraganaNumberText).text = hiraganaKana.size.toString()
                        kanaChoiceView.findViewById<TextView>(R.id.katakanaNumberText).text = katakanaKana.size.toString()

                        dialog.show()

                    }
                }
            } else {
                findViewById<RelativeLayout>(R.id.writingRelativeLayout).visibility = View.GONE
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
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.report -> showReportDialog()
            R.id.faq -> startActivity(Intent(this, FaqActivity::class.java))
            R.id.refresh -> refreshActivity()
            R.id.tutorial -> startActivity(Intent(this, KanaIntroActivity::class.java))
            R.id.review -> redirectToPlayStore()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setLevelsFunction(isWriting: Boolean) {
        for ((index, level) in levelsArray.withIndex()) {
            val name = level.context.resources.getResourceEntryName(level.id)
            if (isWriting) {
                level.findViewById<TextView>(
                    resources.getIdentifier(
                        "${name}NumKanaTextView",
                        "id",
                        packageName
                    )
                ).text = writingLevelsArray[index].toString()
                level.setOnClickListener {
                    val intent = Intent(this, KanaGridActivity::class.java)
                    intent.putExtra("writingLevel", name)
                    startActivity(intent)
                }
            } else {
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
        }
    }

    private fun startReviewTimer(kana: Kana) {
        object : CountDownTimer(kana.reviewTime!! - System.currentTimeMillis(), 1000) {
            override fun onTick(millisLeft: Long) {
                val time = formatTime(millisLeft)
                nextReviewTime.text =
                    "Next review:\n\t\t$time".colorizeText(time, ContextCompat.getColor(this@MenuActivity, R.color.azure))
            }

            override fun onFinish() {
                kanaReviewQueue.removeFirstOrNull()
                numItemsToReview++
                if (numItemsToReview == 1) {
                    numReviewTextView.background = ContextCompat.getDrawable(this@MenuActivity, R.drawable.items_review_background)
                    reviewButton.isEnabled = true
                    reviewButton.isClickable = true
                    reviewAnimation.setAnimation(R.raw.quiz)
                    reviewAnimation.playAnimation()
                }
                currentReviewsTextView.text = "Current: $numItemsToReview".colorizeText(
                    numItemsToReview.toString(),
                    ContextCompat.getColor(this@MenuActivity, R.color.azure)
                )
                numReviewTextView.text = numItemsToReview.toString()
                kanaToReview.add(kana)
                if (kanaReviewQueue.isEmpty()) {
                    nextReviewTime.text = "Next review:\nnone".colorizeText("none", ContextCompat.getColor(this@MenuActivity, R.color.azure))
                } else {
                    startReviewTimer(kanaReviewQueue[0])
                }
            }
        }.start()
    }

    private fun startWritingTimer(kana: Kana) {
        object : CountDownTimer(kana.writingReviewTime!! - System.currentTimeMillis(), 1000) {
            override fun onTick(millisLeft: Long) {
                val time = formatTime(millisLeft)
                nextWritingReviewText.text =
                    "Next review:\n\t\t$time".colorizeText(time, ContextCompat.getColor(this@MenuActivity, R.color.writing_green))
            }

            override fun onFinish() {
                kanaWritingQueue.removeFirstOrNull()
                numItemsToWrite++
                if (numItemsToWrite == 1) {
                    numWriteTextView.background = ContextCompat.getDrawable(this@MenuActivity, R.drawable.items_review_background)
                    writingReviewButton.isEnabled = true
                    writingReviewButton.isClickable = true
                    findViewById<LottieAnimationView>(R.id.writingAnimation).playAnimation()
                }
                currentWritingReviews.text = "Current: $numItemsToWrite".colorizeText(
                    numItemsToWrite.toString(),
                    ContextCompat.getColor(this@MenuActivity, R.color.writing_green)
                )
                numWriteTextView.text = numItemsToWrite.toString()
                kanaToWrite.add(kana)
                if (kanaWritingQueue.isEmpty()) {
                    nextWritingReviewText.text = "Next review:\nnone".colorizeText("none", ContextCompat.getColor(this@MenuActivity, R.color.writing_green))
                } else {
                    startWritingTimer(kanaWritingQueue[0])
                }
            }
        }.start()
    }

    private fun redirectToPlayStore() {
        val uri: Uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun refreshActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
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
        val lessonNumber = sharedPref.getInt("kanasrsLessonNumber", 10)
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