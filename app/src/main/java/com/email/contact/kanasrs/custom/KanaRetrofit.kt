package com.email.contact.kanasrs.custom

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface KanaRetrofit {
    @POST("predict/")
    fun writingToKana(@Body data: RequestBody): Call<WritingResponse>
}