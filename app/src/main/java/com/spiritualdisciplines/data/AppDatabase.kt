package com.spiritualdisciplines.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyRecord::class,
        PrayerRequest::class,
        MemoryVerse::class,
        JournalEntry::class,
        CachedChapter::class,
        CachedVerse::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spiritual_disciplines.db"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
