package com.email.contact.kanasrs.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, Kana::class], version = 7)
abstract class KanaSRSDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun kanaDao(): KanaDao

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: KanaSRSDatabase? = null

        fun getInstance(context: Context): KanaSRSDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): KanaSRSDatabase {

            // Note for future, if you need to change the db file in assets,
            // use migration and exec sql statements, for example:

//            val MIGRATION_4_5 = object : Migration(4, 5) {
//                override fun migrate(database: SupportSQLiteDatabase) {
//                    database.execSQL("ALTER TABLE Kana ADD COLUMN mnemonicDescription TEXT")
//                    database.execSQL("ALTER TABLE Kana ADD COLUMN writingLevel INTEGER")
//                    database.execSQL("ALTER TABLE Kana ADD COLUMN totalAnswered INTEGER")
//                    database.execSQL("ALTER TABLE Kana ADD COLUMN totalCorrect INTEGER")
//                }
//            }

            return Room.databaseBuilder(context, KanaSRSDatabase::class.java, "kanasrs.db")
                .createFromAsset("kanasrs.db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
        }

        fun destroyInstance() {
            instance = null
        }
    }

}