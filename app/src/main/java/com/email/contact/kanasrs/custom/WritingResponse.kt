package com.email.contact.kanasrs.custom

import com.google.gson.annotations.SerializedName

data class WritingResponse(
    @SerializedName("data")
    val data: List<String>,
    @SerializedName("durations")
    val durations: List<Float>,
    @SerializedName("average_durations")
    val averageDurations: List<Float>
)
