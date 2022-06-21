package com.email.contact.kanasrs.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


// TODO: Give preset kana data a description column, gif link column, drawable int, mp3 int


@Database(entities = [User::class, Kana::class], version = 5)
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

            return Room.databaseBuilder(context, KanaSRSDatabase::class.java, "kanasrs.db").createFromAsset("kanasrs.db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()

            // Code below is for manual data entry

//            return Room.databaseBuilder(context, KanaSRSDatabase::class.java, "kanasrs.db")
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