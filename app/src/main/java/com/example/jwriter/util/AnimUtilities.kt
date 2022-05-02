package com.example.jwriter.util

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import com.takusemba.spotlight.OnTargetListener
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.RoundedRectangle

/**
 * Utilities class for animations during program
 */
class AnimUtilities {
    companion object {
        fun animateFromLeft(view: View, layout: ConstraintLayout, startDelay: Long) {
            view.x = (-layout.width).toFloat()
            view.animate().translationXBy(layout.width.toFloat() * 1.5F - view.width / 2).setStartDelay(startDelay).duration = 1000
        }
        fun animateFromRight(view: View, layout: ConstraintLayout, startDelay: Long) {
            view.x = (layout.width).toFloat() * 2
            view.animate().translationXBy(-layout.width.toFloat() * 1.5F - view.width / 2).setStartDelay(startDelay).duration = 1000
        }
        /**
         * Helper function for end screen animation
         */
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

        fun newTarget(anchor: View, height: Float, width: Float, overlay: View, onStart: (() -> Unit)? = null, onEnd: (() -> Unit)? = null): Target {
            val target = Target.Builder()
                .setAnchor(anchor)
                .setShape(RoundedRectangle(height, width, 30f))
                .setOverlay(overlay)
                .setOnTargetListener(object : OnTargetListener {
                    override fun onEnded() {
                        if (onEnd != null) {
                            onEnd()
                        }
                    }

                    override fun onStarted() {
                        if (onStart != null) {
                            onStart()
                        }
                    }

                }).build()
            return target
        }

    }
}