package com.email.contact.kanasrs.database

import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Kana(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "letter") val letter: String?,
    @ColumnInfo(name = "next_practice") var reviewTime: Long?,
    @ColumnInfo(name = "level") var level: Int?,
    @ColumnInfo(name = "isHiragana") val isHiragana: Boolean,
    @ColumnInfo(name = "hasLearned") var hasLearned: Boolean,
    @ColumnInfo(name = "gif") val gif: String,
    @ColumnInfo(name = "description") var mnemonic: String,
    @ColumnInfo(name = "mnemonicDescription") var customMnemonic: String?,
    @ColumnInfo(name = "writingLevel") var writingLevel: Int?,
    @ColumnInfo(name = "totalAnswered") var totalAnswered: Int?,
    @ColumnInfo(name = "totalCorrect") var totalCorrect: Int?,
    @ColumnInfo(name = "writingNextReview") var writingReviewTime: Long?,
    @ColumnInfo(name = "streak", defaultValue = "0") var streak: Int = 0
): Parcelable
