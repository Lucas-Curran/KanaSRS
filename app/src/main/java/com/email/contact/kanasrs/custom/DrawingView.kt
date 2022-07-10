package com.email.contact.kanasrs.custom

import android.content.Context
import android.graphics.*
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream


class DrawingView(var c: Context) : View(c) {

    private var mPaint: Paint? = null
    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mPath: Path = Path()
    private val mBitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private val circlePaint: Paint = Paint()
    private val circlePath: Path = Path()

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
        GlobalScope.launch {
            println(isDrawingCorrect("あ"))
        }
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

    @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    suspend fun isDrawingCorrect(kanaLetter: String): Boolean =
        suspendCancellableCoroutine { continuation ->

            val newBitmap = Bitmap.createBitmap(
                mBitmap?.width!!,
                mBitmap?.height!!, mBitmap?.config!!
            )

            val canvas = Canvas(newBitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(mBitmap!!, 0f, 0f, null)

            val baos = ByteArrayOutputStream()
            newBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val imagesBytes: ByteArray = baos.toByteArray()
            val encodedImage: String = Base64.encodeToString(imagesBytes, Base64.NO_WRAP)
            val jsonObject = JSONObject()
            val jsonArray = JSONArray()

            jsonArray.put("data:image/png;base64,sumthinfake")
            jsonObject.put("data", jsonArray)

            val jsonString = jsonObject.toString().replace("sumthinfake", encodedImage)

            val requestBody = RequestBody.create(MediaType.parse("application/json"), jsonString)

            val call = retrofitAPI.writingToKana(requestBody)

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.body()?.string()?.contains(kanaLetter) == true) {
                        continuation.resume(true) {
                            Log.e("MLError", it.stackTraceToString())
                        }
                    } else {
                        continuation.resume(false) {
                            Log.e("MLError", it.stackTraceToString())
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(context, "Kana could not be converted...", Toast.LENGTH_SHORT)
                        .show()
                    continuation.resume(false) {
                        Log.e("MLError", t.stackTraceToString())
                    }
                }

            })
        }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
    }
}