package com.example.jwriter.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


// TODO: Give preset kana data a description column, gif link column, drawable int, mp3 int


@Database(entities = [User::class, Kana::class], version = 3)
abstract class JWriterDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun kanaDao(): KanaDao

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: JWriterDatabase? = null

        fun getInstance(context: Context): JWriterDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): JWriterDatabase {

            return Room.databaseBuilder(context, JWriterDatabase::class.java, "jwriter.db").createFromAsset("jwriter.db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()

            //Code below is for manual data entry

//            return Room.databaseBuilder(context, JWriterDatabase::class.java, "jwriter.db")
//                .addCallback(object : RoomDatabase.Callback() {
//                    override fun onCreate(db: SupportSQLiteDatabase) {
//                        super.onCreate(db)
//                        //pre-populate data
//                        Executors.newSingleThreadExecutor().execute {
//                            instance?.let {
//                                //Create new user, and add fresh kana data to database
//
//                                val newUser = User(0)
//                                it.userDao().insertAll(newUser)
//                                var id = 0
//                                for (letter in ReviewActivity.hiraganaList) {
//                                    val newKana = Kana(
//                                        id = id,
//                                        letter = letter,
//                                        reviewTime = null,
//                                        level = null,
//                                        isHiragana = true,
//                                        hasLearned = false,
//                                        description = "yes",
//                                        gif = "yes")
//                                    it.kanaDao().insertAll(newKana)
//                                    id++
//                                }
//                                for (letter in ReviewActivity.katakanaList) {
//                                    val newKana = Kana(
//                                        id = id,
//                                        letter = letter,
//                                        reviewTime = null,
//                                        level = null,
//                                        isHiragana = false,
//                                        hasLearned = false,
//                                        description = "yes",
//                                        gif = "yes")
//                                    it.kanaDao().insertAll(newKana)
//                                    id++
//                                }
//                            }
//                        }
//                    }
//                })
//                .allowMainThreadQueries()
//                .fallbackToDestructiveMigration()
//                .build()
        }

        fun destroyInstance() {
            instance = null
        }
    }

}