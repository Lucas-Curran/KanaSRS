package com.email.contact.kanasrs.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream


class DrawingView(var c: Context) : View(c) {

    private var mPaint: Paint? = null
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mPath: Path = Path()
    private val mBitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private val circlePaint: Paint = Paint()
    private val circlePath: Path = Path()

    private var canDraw = true

    private val retrofitAPI: KanaRetrofit

    init {

        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.isDither = true
        mPaint!!.color = Color.BLACK
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeJoin = Paint.Join.ROUND
        mPaint!!.strokeCap = Paint.Cap.ROUND
        mPaint!!.strokeWidth = 20F

        circlePaint.isAntiAlias = true
        circlePaint.color = Color.BLUE
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeJoin = Paint.Join.MITER
        circlePaint.strokeWidth = 4f

        val retrofit = Retrofit.Builder()
            .baseUrl("https://hf.space/embed/Detomo/Japanese-OCR/+/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofitAPI = retrofit.create(KanaRetrofit::class.java)

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

    fun clearDrawing() {
        onSizeChanged(width, height, width, height)
        invalidate()
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (canDraw) {
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
        return false
    }

    @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    suspend fun isDrawingCorrect(kanaLetter: String, progressBar: ProgressBar): Boolean =
        suspendCancellableCoroutine { continuation ->

            val newBitmap = Bitmap.createBitmap(
                mBitmap?.width!!,
                mBitmap?.height!!, mBitmap?.config!!
            )

            val canvas = Canvas(newBitmap)
            if (mPaint?.color == Color.BLACK) {
                canvas.drawColor(Color.WHITE)
            } else if (mPaint?.color == Color.WHITE) {
                canvas.drawColor(Color.BLACK)
            }
            canvas.drawBitmap(mBitmap!!, 0f, 0f, null)

            val baos = ByteArrayOutputStream()
            newBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val imagesBytes: ByteArray = baos.toByteArray()
            val encodedImage: String = Base64.encodeToString(imagesBytes, Base64.NO_WRAP)
            val jsonObject = JSONObject()
            val jsonArray = JSONArray()

            jsonArray.put("data:image/png;base64,placeholdertext")
            jsonObject.put("data", jsonArray)

            val jsonString = jsonObject.toString().replace("placeholdertext", encodedImage)

            val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)

            val call = retrofitAPI.writingToKana(requestBody)

            call.enqueue(object : Callback<WritingResponse> {
                override fun onResponse(
                    call: Call<WritingResponse>,
                    response: Response<WritingResponse>
                ) {
                    val data = response.body()!!.data
                    //data[0] is the first word, or letter, in a sequence of words
                    progressBar.visibility = INVISIBLE
                    println(data[0])

                    //For now, hiragana ki and katakana ki get mixed up
                    if (data[0].contains("キ") && kanaLetter == "き" || data[0].contains("き") && kanaLetter == "キ") {
                        continuation.resume(true) {
                            Log.e("MLError", it.stackTraceToString())
                        }
                        return
                    }

                    //Make sure response isn't too long so they can't get a false positive
                    if (data[0].contains(kanaLetter) && data[0].length < 5) {
                        continuation.resume(true) {
                            Log.e("MLError", it.stackTraceToString())
                        }
                    } else {
                        continuation.resume(false) {
                            Log.e("MLError", it.stackTraceToString())
                        }
                    }
                }

                override fun onFailure(call: Call<WritingResponse>, t: Throwable) {
                    progressBar.visibility = INVISIBLE
                    Toast.makeText(context, "Connection failed, check your internet", Toast.LENGTH_SHORT)
                        .show()
                    continuation.resume(false) {
                        Log.e("MLError", t.stackTraceToString())
                    }
                }

            })
        }

    fun setStrokeWidth(width: Float) {
        mPaint?.strokeWidth = width
    }

    fun disableDrawing() {
        canDraw = false
    }

    fun enableDrawing() {
        canDraw = true
    }

    fun setPaintColor(color: Int) {
        mPaint?.color = color
    }

    fun checkIfEmpty(): Boolean {
        val emptyBitmap = Bitmap.createBitmap(mBitmap!!.width, mBitmap!!.height, mBitmap!!.config)
        return mBitmap!!.sameAs(emptyBitmap)
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
    }
}