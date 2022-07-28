package com.email.contact.kanasrs.util

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import android.widget.ViewAnimator
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.marginBottom
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.notification.NotificationReceiver
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.google.android.play.core.review.testing.FakeReviewManager
import java.time.Duration
import java.util.*

/**
 * Utilities class for methods used across the program
 */
class Utilities {

    companion object {

        const val ROOKIE1 = 1
        const val ROOKIE2 = 2
        const val AMATEUR = 3
        const val EXPERT = 4
        const val MASTER = 5
        const val SENSEI = 6

        var mediaPlayer = MediaPlayer()

        fun animateUp(view: View, startDelay: Long) {
            view.animate().translationYBy(-1000F).withEndAction {
                view.visibility = View.GONE
            }.setStartDelay(startDelay).duration = 2000
        }

        fun animateFromTop(view: View, layout: ConstraintLayout, startDelay: Long) {
            view.y = (-layout.height).toFloat()
            view.animate().translationYBy(layout.height.toFloat()).setStartDelay(startDelay).duration = 1000
        }

        fun animateFromBottom(view: View, layout: ConstraintLayout, startDelay: Long) {
            view.y = (layout.height).toFloat()
            view.animate().translationYBy(-view.height.toFloat() - view.marginBottom).setStartDelay(startDelay).duration = 1000
        }

        fun animateFromLeft(view: View, layout: ConstraintLayout, startDelay: Long) {
            view.x = (-layout.width).toFloat()
            view.animate().translationXBy(layout.width.toFloat() * 1.5F - view.width / 2).setStartDelay(startDelay).duration = 1000
        }

        fun animateFromRight(view: View, layout: ConstraintLayout, startDelay: Long) {
            view.x = (layout.width).toFloat() * 2
            view.animate().translationXBy(-layout.width.toFloat() * 1.5F - view.width / 2).setStartDelay(startDelay).duration = 1000
        }

        fun animateToLeft(view: View, startDelay: Long, endAction: (() -> Unit)? = null) {
            view.animate().translationXBy(-1000F).withEndAction {
                view.visibility = View.GONE
                if (endAction != null) {
                    endAction()
                }
            }.setStartDelay(startDelay).duration = 2000
        }

        fun animateToRight(view: View, startDelay: Long, endAction: (() -> Unit)? = null) {
            view.animate().translationXBy(1000F).withEndAction {
                view.visibility = View.GONE
                if (endAction != null) {
                    endAction()
                }
            }.setStartDelay(startDelay).duration = 2000
        }

         fun animateEnd(view: View, rootLayout: ConstraintLayout, delay: Long, action: () -> Unit) {
            view.animate().translationYBy(-300F).withEndAction {
                view.animate().translationYBy(rootLayout.height.toFloat()).withEndAction {
                    action()
                }.duration = 1000
            }.setStartDelay(delay).duration = 1000
        }

        @SuppressLint("Recycle")
        fun slideView(view: View, currentHeight: Int, newHeight: Int, onEnd: () -> Unit) {
            val slideAnimator = ValueAnimator.ofInt(currentHeight, newHeight).setDuration(500)
            slideAnimator.addUpdateListener {
                val value = it.animatedValue as Int
                view.layoutParams.height = value
                view.requestLayout()
            }
            val animationSet = AnimatorSet()
            animationSet.interpolator = AccelerateDecelerateInterpolator()
            animationSet.play(slideAnimator)
            animationSet.doOnEnd {
                onEnd()
            }
            animationSet.start()
        }

        fun setNextAnim(viewAnimator: ViewAnimator, animationIn: Animation, animationOut: Animation) {
            viewAnimator.inAnimation = animationIn
            viewAnimator.outAnimation = animationOut
        }

        fun setPrevAnim(viewAnimator: ViewAnimator, prevAnimIn: Animation, prevAnimOut: Animation) {
            viewAnimator.inAnimation = prevAnimIn
            viewAnimator.outAnimation = prevAnimOut
        }

        fun formatTime(milliseconds: Long): String {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = Duration.ofMillis(milliseconds)
                val days = duration.toHours().toInt() / 24
                val timeFormat = "%02d:%02d:%02d".format(
                    (duration.seconds / 3600) % 24,
                    (duration.seconds % 3600) / 60,
                    (duration.seconds % 60)
                )
                var returnTime = ""
                if (days > 0) {
                    if (days == 1) {
                        returnTime = "$days Day and $timeFormat"
                    } else {
                        returnTime = "$days Days and $timeFormat"
                    }
                } else {
                    returnTime = timeFormat
                }
                return returnTime
            } else {
                //TODO: Properly error handle lower level API
                return "error"
            }
        }


        fun CharSequence.colorizeText(
            textPartToColorize: CharSequence,
            @ColorInt color: Int
        ): CharSequence = SpannableString(this).apply {
            val startIndexOfText = indexOf(textPartToColorize.toString())
            setSpan(ForegroundColorSpan(color), startIndexOfText, startIndexOfText.plus(textPartToColorize.length), 0)
        }

        fun View.disable() {
            isClickable = false
            isEnabled = false
        }

        fun View.enable() {
            isClickable = true
            isEnabled = true
        }

        @SuppressLint("ClickableViewAccessibility")
        fun ScrollView.disableScroll() {
            setOnTouchListener { _, _ -> true }
        }

        @SuppressLint("ClickableViewAccessibility")
        fun ScrollView.enableScroll() {
            setOnTouchListener { _, _ -> false }
        }

        fun EditText.showKeyboard() {
            post {
                requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        fun getLevelColor(level: Int): Int {
            return when(level) {
                1, 2 ->  {
                    R.color.rookie_pink
                }
                3 -> {
                    R.color.amateur_purple
                }
                4 -> {
                    R.color.expert_blue
                }
                5 -> {
                    R.color.master_blue
                }
                6 -> {
                    R.color.sensei_gold
                }
                else -> 0
            }
        }

        fun levelToTitle(level: Int): String {
            return when(level) {
                1, 2 ->  {
                    "Rookie"
                }
                3 -> {
                    "Amateur"
                }
                4 -> {
                    "Expert"
                }
                5 -> {
                    "Master"
                }
                6 -> {
                    "Sensei"
                }
                else -> ""
            }
        }

        fun Context.dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

        fun Context.pxToDp(px: Int): Int {
            return (px / resources.displayMetrics.density).toInt()
        }

        fun reviewApp(context: Context, fromActivity: Activity) {
            val manager = FakeReviewManager(context)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    manager.launchReviewFlow(fromActivity, reviewInfo!!)
                } else {
                    Toast.makeText(context, "There was an issue handling the request, check your internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun setAlarm(context: Context) {

            val sharedPref = context.getSharedPreferences(context.getString(R.string.pref_key), Context.MODE_PRIVATE)

            val calendar = Calendar.getInstance()
            calendar[Calendar.HOUR_OF_DAY] = sharedPref.getInt("kanasrsNotificationHour", 12)
            calendar[Calendar.MINUTE] = sharedPref.getInt("kanasrsNotificationMinute", 0)
            calendar[Calendar.SECOND] = 0
            if (calendar.time < Date()) calendar.add(Calendar.DAY_OF_MONTH, 1)
            val intent = Intent(context.applicationContext, NotificationReceiver::class.java)
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getBroadcast(
                    context.applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    context.applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            val alarmManager = context.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }

    }
}