package com.email.contact.kanasrs.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.email.contact.kanasrs.R
import com.email.contact.kanasrs.activity.KanaGridActivity
import com.email.contact.kanasrs.database.KanaSRSDatabase

class DistributionBar(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint: Paint = Paint()
    private val levelColors = mutableListOf<Int>()
    private val db: KanaSRSDatabase
    private var total = 0f
    private val levelProportion = mutableListOf<Float>()
    private val rects = mutableListOf<RectF>()
    private var savedColor = 0
    private var savedIndex = -1

    init {
        paint.style = Paint.Style.FILL
        paint.textSize = 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        db = KanaSRSDatabase.getInstance(context)
        total = db.kanaDao().getLearnedHiragana().size.toFloat() + db.kanaDao().getLearnedKatakana().size.toFloat()
        val rookieKana = db.kanaDao().getRookieKana()
        val amateurKana = db.kanaDao().getAmateurKana()
        val expertKana = db.kanaDao().getExpertKana()
        val masterKana = db.kanaDao().getMasterKana()
        val senseiKana = db.kanaDao().getSenseiKana()

        if (rookieKana.isNotEmpty()) {
            levelProportion.add(rookieKana.size / total)
            levelColors.add(ContextCompat.getColor(context, R.color.rookie_pink))
        }
        if (amateurKana.isNotEmpty()) {
            levelProportion.add(amateurKana.size / total)
            levelColors.add(ContextCompat.getColor(context, R.color.amateur_purple))
        }
        if (expertKana.isNotEmpty()) {
            levelProportion.add(expertKana.size / total)
            levelColors.add(ContextCompat.getColor(context, R.color.expert_blue))
        }
        if (masterKana.isNotEmpty()) {
            levelProportion.add(masterKana.size / total)
            levelColors.add(ContextCompat.getColor(context, R.color.master_blue))
        }
        if (senseiKana.isNotEmpty()) {
            levelProportion.add(senseiKana.size / total)
            levelColors.add(ContextCompat.getColor(context, R.color.sensei_gold))
        }

    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        rects.clear()

        if (levelProportion.isNotEmpty()) {
            var rectLeft = 0f
            val marginLeft = 2f
            for ((index, proportion) in levelProportion.withIndex()) {
                paint.color = levelColors[index]
                val path = Path()
                var rectF = RectF(rectLeft, 0f, proportion * width + rectLeft, height.toFloat())
                if (levelProportion.size > 1 && index == levelProportion.size - 1) {
                    rectF = RectF(
                        rectLeft,
                        0f,
                        proportion * width + rectLeft - marginLeft * levelProportion.size,
                        height.toFloat()
                    )
                }
                when {
                    // If only size 1, make rect radius even
                    levelProportion.size == 1 -> {
                        val corners = floatArrayOf(
                            20f, 20f,   // Top left radius in px
                            20f, 20f,   // Top right radius in px
                            20f, 20f,     // Bottom right radius in px
                            20f, 20f      // Bottom left radius in px
                        )
                        path.addRoundRect(
                            rectF,
                            corners, Path.Direction.CW
                        )
                        canvas.drawPath(path, paint)

                        // If not size 1, then on index 0, top left and bottom left radius
                    }
                    index == 0 -> {
                        val corners = floatArrayOf(
                            20f, 20f,   // Top left radius in px
                            0f, 0f,   // Top right radius in px
                            0f, 0f,     // Bottom right radius in px
                            20f, 20f      // Bottom left radius in px
                        )
                        path.addRoundRect(
                            rectF,
                            corners, Path.Direction.CW
                        )
                        canvas.drawPath(path, paint)

                        // If not size = 1 or index = 0, then on the last index, which is size-1, top right and bottom right radius
                    }
                    index == levelProportion.size - 1 -> {
                        val corners = floatArrayOf(
                            0f, 0f,   // Top left radius in px
                            20f, 20f,   // Top right radius in px
                            20f, 20f,     // Bottom right radius in px
                            0f, 0f      // Bottom left radius in px
                        )
                        path.addRoundRect(
                            rectF,
                            corners, Path.Direction.CW
                        )
                        canvas.drawPath(path, paint)
                    }
                    else -> {
                        path.addRect(
                            rectF, Path.Direction.CW
                        )
                        canvas.drawPath(path, paint)
                    }
                }
                rects.add(rectF)
                paint.color = Color.WHITE
                canvas.drawText(
                    "${(proportion * total).toInt()}",
                    rectF.centerX() - 10,
                    height.toFloat() / 2 - ((paint.ascent() + paint.descent()) / 2),
                    paint
                )
                rectLeft += proportion * width + marginLeft
            }
        } else {
            paint.color = Color.WHITE
            canvas.drawText("No kana learned yet! Begin some lessons!", 0f, height.toFloat() / 2, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (KanaSRSDatabase.getInstance(context).kanaDao().getLearnedHiragana().isEmpty()) {
            return false
        }
        val touchX = event.x
        val touchY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for ((index, rect) in rects.withIndex()) {
                    if (rect.contains(touchX, touchY)) {
                        savedColor = levelColors[index]
                        savedIndex = index
                        levelColors[index] = ContextCompat.getColor(context, R.color.pink)
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                levelColors[savedIndex] = savedColor
                invalidate()
            }
            MotionEvent.ACTION_UP -> {

                // First, check if the touch up contains one of the rectangles in the list
                // Second, loop through color list and check if index is the same as the rectangle
                // This is because the colors are added in the same order as the rects are added, rookie -> sensei
                // So to identify what the rectangle does, we use the parallel array of colors
                // Then a switch is used to identify the exact color, and to go to the kana grid

                for ((index, rect) in rects.withIndex()) {
                    if (rect.contains(touchX, touchY)) {
                        levelColors[savedIndex] = savedColor
                        invalidate()
                        for ((index2, color) in levelColors.withIndex()) {
                            if (index == index2) {
                                when (color) {
                                    ContextCompat.getColor(context, R.color.rookie_pink) -> {
                                        val intent = Intent(context, KanaGridActivity::class.java)
                                        intent.putExtra("level", "rookie")
                                        context.startActivity(intent)
                                    }
                                    ContextCompat.getColor(context, R.color.amateur_purple) -> {
                                        val intent = Intent(context, KanaGridActivity::class.java)
                                        intent.putExtra("level", "amateur")
                                        context.startActivity(intent)
                                    }
                                    ContextCompat.getColor(context, R.color.expert_blue) -> {
                                        val intent = Intent(context, KanaGridActivity::class.java)
                                        intent.putExtra("level", "expert")
                                        context.startActivity(intent)
                                    }
                                    ContextCompat.getColor(context, R.color.master_blue) -> {
                                        val intent = Intent(context, KanaGridActivity::class.java)
                                        intent.putExtra("level", "master")
                                        context.startActivity(intent)
                                    }
                                    ContextCompat.getColor(context, R.color.sensei_gold) -> {
                                        val intent = Intent(context, KanaGridActivity::class.java)
                                        intent.putExtra("level", "sensei")
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true
    }
}