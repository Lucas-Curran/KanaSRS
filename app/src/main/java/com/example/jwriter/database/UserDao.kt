package com.example.jwriter.database

import androidx.room.*
import com.example.jwriter.database.User

@Dao
interface UserDao {

    @Query("SELECT * FROM user")
    fun getUser(): User

    @Query ("SELECT total_accuracy FROM user")
    fun getAccuracy(): Int

    @Insert
    fun insertAll(vararg user: User)

    @Update
    fun updateUser(user: User)

    @Delete
    fun delete(user: User)
}