package com.example.jwriter

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "total_accuracy") var totalAccuracy: Int,
) {
    constructor(totalAccuracy: Int) : this(0, totalAccuracy)
}
