package com.example.jwriter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors


@Database(entities = [User::class], version = 1)
abstract class JWriterDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: JWriterDatabase? = null

        fun getInstance(context: Context): JWriterDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): JWriterDatabase {
            return Room.databaseBuilder(context, JWriterDatabase::class.java, "jwriter.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        //pre-populate data
                        Executors.newSingleThreadExecutor().execute {
                            instance?.let {
                                val newUser = User(0)
                                it.userDao().insertAll(newUser)
                            }
                        }
                    }
                })
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
        }

        fun destroyInstance() {
            instance = null
        }
    }

}