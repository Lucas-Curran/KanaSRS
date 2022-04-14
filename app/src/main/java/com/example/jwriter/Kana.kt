package com.example.jwriter

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Kana(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "letter") val letter: String?,
    @ColumnInfo(name = "next_practice") var reviewTime: Long?,
    @ColumnInfo(name = "level") var level: Int?,
    @ColumnInfo(name = "isHiragana") val isHiragana: Boolean,
    @ColumnInfo(name = "hasLearned") var hasLearned: Boolean
)