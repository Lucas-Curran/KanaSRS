package com.example.jwriter.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "total_accuracy") var totalAccuracy: Int,
    @ColumnInfo(name = "lessonRefreshTime") var lessonRefreshTime: Long?,
    @ColumnInfo(name = "lessonsNumber") var lessonsNumber: Int?
)
