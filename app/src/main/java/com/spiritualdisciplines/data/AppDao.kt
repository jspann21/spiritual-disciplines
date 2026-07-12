package com.spiritualdisciplines.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Daily Records
    @Query("SELECT * FROM daily_records WHERE date = :date")
    fun getDailyRecord(date: String): Flow<DailyRecord?>

    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAllDailyRecords(): Flow<List<DailyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRecord(record: DailyRecord)

    // Prayer Requests
    @Query("SELECT * FROM prayer_requests ORDER BY id DESC")
    fun getAllPrayerRequests(): Flow<List<PrayerRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerRequest(request: PrayerRequest)

    @Query("UPDATE prayer_requests SET isAnswered = :isAnswered WHERE id = :id")
    suspend fun updatePrayerAnswered(id: Int, isAnswered: Boolean)

    @Query("UPDATE prayer_requests SET lastPrayedDate = :date WHERE id = :id")
    suspend fun updatePrayerLastPrayed(id: Int, date: String)

    @Query("DELETE FROM prayer_requests WHERE id = :id")
    suspend fun deletePrayerRequest(id: Int)

    // Memory Verses
    @Query("SELECT * FROM memory_verses ORDER BY id DESC")
    fun getAllMemoryVerses(): Flow<List<MemoryVerse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryVerse(verse: MemoryVerse)

    @Query("UPDATE memory_verses SET lastReviewedDate = :date WHERE id = :id")
    suspend fun updateVerseLastReviewed(id: Int, date: String)

    @Query("DELETE FROM memory_verses WHERE id = :id")
    suspend fun deleteMemoryVerse(id: Int)

    // Journal Entries
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry)

    // Cached Verses
    @Query("SELECT * FROM cached_verses WHERE id = :id")
    suspend fun getCachedVerse(id: String): CachedVerse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedVerse(verse: CachedVerse)

    @Query("DELETE FROM cached_verses WHERE id NOT IN (:keepIds)")
    suspend fun clearOldCachedVerses(keepIds: List<String>)

    @Query("DELETE FROM cached_verses")
    suspend fun clearAllCachedVerses()

    // Cached Chapters
    @Query("SELECT * FROM cached_chapters WHERE id = :id")
    suspend fun getCachedChapter(id: String): CachedChapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedChapter(chapter: CachedChapter)

    @Query(
        """
        DELETE FROM cached_chapters
        WHERE rowid NOT IN (
            SELECT rowid FROM cached_chapters ORDER BY rowid DESC LIMIT :maxEntries
        )
        """
    )
    suspend fun trimCachedChapters(maxEntries: Int)

    @Transaction
    suspend fun insertCachedChapterBounded(chapter: CachedChapter, maxEntries: Int) {
        insertCachedChapter(chapter)
        trimCachedChapters(maxEntries)
    }

    @Query("DELETE FROM cached_chapters")
    suspend fun clearAllCachedChapters()

    @Transaction
    suspend fun clearAllCaches() {
        clearAllCachedVerses()
        clearAllCachedChapters()
    }

    @Query(
        """
        SELECT
            COALESCE((SELECT SUM(length(CAST(text AS BLOB))) FROM cached_verses), 0) +
            COALESCE((SELECT SUM(length(CAST(versesJson AS BLOB))) FROM cached_chapters), 0)
        """
    )
    fun getCacheSizeBytes(): Flow<Long>
}
