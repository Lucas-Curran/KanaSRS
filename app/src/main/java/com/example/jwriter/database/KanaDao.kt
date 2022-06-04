package com.example.jwriter.database

import androidx.room.*
import com.example.jwriter.database.Kana

@Dao
interface KanaDao {

    @Query("SELECT * FROM kana")
    fun getKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE hasLearned = 0")
    fun getUnlearnedKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE hasLearned = 1 and isHiragana = 1")
    fun getLearnedHiragana(): List<Kana>

    @Query("SELECT * FROM kana WHERE hasLearned = 1 and isHiragana = 0")
    fun getLearnedKatakana(): List<Kana>

    @Query("SELECT * FROM kana WHERE isHiragana = 1")
    fun getHiragana(): List<Kana>

    @Query("SELECT * FROM kana WHERE isHiragana = 0")
    fun getKatakana(): List<Kana>

    @Insert
    fun insertAll(vararg kana: Kana)

    @Update
    fun updateKana(kana: Kana)

    @Delete
    fun delete(kana: Kana)
}