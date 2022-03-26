package com.example.jwriter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [User::class], version = 1)
abstract class JWriterDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        private var INSTANCE: JWriterDatabase? = null

        fun getInstance(context: Context): JWriterDatabase? {
            if (INSTANCE == null) {
                synchronized(JWriterDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        JWriterDatabase::class.java, "user.db").allowMainThreadQueries()
                        .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

}