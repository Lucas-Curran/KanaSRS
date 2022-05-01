package com.example.jwriter.database

import androidx.room.*
import com.example.jwriter.database.Kana

@Dao
interface KanaDao {

    @Query("SELECT * FROM kana")
    fun getKana(): List<Kana>

    @Query("SELECT * FROM kana WHERE hasLearned = 0")
    fun getUnlearnedKana(): List<Kana>

    @Insert
    fun insertAll(vararg kana: Kana)

    @Update
    fun updateKana(kana: Kana)

    @Delete
    fun delete(kana: Kana)
}