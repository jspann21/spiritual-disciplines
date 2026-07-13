package com.spiritualdisciplines.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DailyRecord::class,
        PrayerRequest::class,
        MemoryVerse::class,
        JournalEntry::class,
        CachedChapter::class,
        CachedVerse::class
    ],
    version = 7,
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
                .addMigrations(MIGRATION_6_7)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `journal_entries_new` (
                        `date` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `journal_entries_new` (`date`, `content`)
                    SELECT `date`, RTRIM(
                        CASE WHEN TRIM(`whatDidIRead`) <> ''
                            THEN 'Receive' || char(10) || `whatDidIRead` || char(10) || char(10)
                            ELSE '' END ||
                        CASE WHEN TRIM(`whatShouldIObey`) <> ''
                            THEN 'Respond' || char(10) || `whatShouldIObey` || char(10) || char(10)
                            ELSE '' END ||
                        CASE WHEN TRIM(`whatShouldIPrayAbout`) <> ''
                            THEN 'Pray' || char(10) || `whatShouldIPrayAbout` || char(10) || char(10)
                            ELSE '' END ||
                        CASE WHEN TRIM(`whatAmIThankfulFor`) <> ''
                            THEN 'Give thanks' || char(10) || `whatAmIThankfulFor`
                            ELSE '' END
                    )
                    FROM `journal_entries`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `journal_entries`")
                db.execSQL("ALTER TABLE `journal_entries_new` RENAME TO `journal_entries`")
            }
        }
    }
}
