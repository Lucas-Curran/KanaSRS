package com.example.jwriter

import androidx.room.*

@Dao
interface UserDao {

    @Query("SELECT * FROM user")
    fun getUsers(): List<User>

    @Query ("SELECT total_accuracy FROM user")
    fun getAccuracy(): Int

    @Insert
    fun insertAll(vararg users: User)

    @Update
    fun updateAccuracy(user: User): Int

    @Delete
    fun delete(user: User)
}