package com.email.contact.kanasrs.database

import androidx.room.*

@Dao
interface KanaDao {

    @Query("SELECT * FROM kana")
    fun getKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE hasLearned = 0")
    fun getUnlearnedKana(): List<Kana>

    @Query("SELECT SUM(totalCorrect) FROM kana")
    fun sumCorrect(): Int

    @Query("SELECT SUM(totalAnswered) FROM kana")
    fun sumAnswered(): Int

    @Query("SELECT SUM(writingTotalCorrect) FROM kana")
    fun sumWritingCorrect(): Int

    @Query("SELECT SUM(writingTotalAnswered) FROM kana")
    fun sumWritingAnswered(): Int

    @Query("SELECT * FROM kana WHERE hasLearned = 1 and isHiragana = 1")
    fun getLearnedHiragana(): List<Kana>

    @Query("SELECT * FROM kana WHERE hasLearned = 1 and isHiragana = 0")
    fun getLearnedKatakana(): List<Kana>

    @Query("SELECT * FROM kana WHERE isHiragana = 1")
    fun getHiragana(): List<Kana>

    @Query("SELECT * FROM kana WHERE isHiragana = 0")
    fun getKatakana(): List<Kana>

    @Query("SELECT * FROM kana WHERE level = 1 or level = 2")
    fun getRookieKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE level = 3")
    fun getAmateurKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE level = 4")
    fun getExpertKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE level = 5")
    fun getMasterKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE level = 6")
    fun getSenseiKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE writingLevel = 1 or writingLevel = 2")
    fun rookieWritingKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE writingLevel = 3")
    fun amateurWritingKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE writingLevel = 4")
    fun expertWritingKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE writingLevel = 5")
    fun masterWritingKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE writingLevel = 6")
    fun senseiWritingKana(): List<Kana>

    @Insert
    fun insertAll(vararg kana: Kana)

    @Update
    fun updateKana(kana: Kana)

    @Delete
    fun delete(kana: Kana)
}