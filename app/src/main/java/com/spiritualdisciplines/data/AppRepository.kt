package com.spiritualdisciplines.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    fun getDailyRecord(date: String): Flow<DailyRecord?> = appDao.getDailyRecord(date)
    fun getAllDailyRecords(): Flow<List<DailyRecord>> = appDao.getAllDailyRecords()
    suspend fun insertDailyRecord(record: DailyRecord) = appDao.insertDailyRecord(record)

    fun getAllPrayerRequests(): Flow<List<PrayerRequest>> = appDao.getAllPrayerRequests()
    suspend fun insertPrayerRequest(request: PrayerRequest) = appDao.insertPrayerRequest(request)
    suspend fun updatePrayerAnswered(id: Int, isAnswered: Boolean) = appDao.updatePrayerAnswered(id, isAnswered)
    suspend fun updatePrayerLastPrayed(id: Int, date: String) = appDao.updatePrayerLastPrayed(id, date)
    suspend fun deletePrayerRequest(id: Int) = appDao.deletePrayerRequest(id)

    fun getAllMemoryVerses(): Flow<List<MemoryVerse>> = appDao.getAllMemoryVerses()
    suspend fun insertMemoryVerse(verse: MemoryVerse) = appDao.insertMemoryVerse(verse)
    suspend fun updateVerseLastReviewed(id: Int, date: String) = appDao.updateVerseLastReviewed(id, date)
    suspend fun deleteMemoryVerse(id: Int) = appDao.deleteMemoryVerse(id)

    fun getAllJournalEntries(): Flow<List<JournalEntry>> = appDao.getAllJournalEntries()
    suspend fun insertJournalEntry(entry: JournalEntry) = appDao.insertJournalEntry(entry)

    suspend fun getCachedVerse(id: String): CachedVerse? = appDao.getCachedVerse(id)
    suspend fun insertCachedVerse(verse: CachedVerse) = appDao.insertCachedVerse(verse)
    suspend fun clearOldCachedVerses(keepIds: List<String>) = appDao.clearOldCachedVerses(keepIds)
    suspend fun clearAllCachedVerses() = appDao.clearAllCachedVerses()

    suspend fun getCachedChapter(id: String): CachedChapter? = appDao.getCachedChapter(id)
    suspend fun insertCachedChapter(chapter: CachedChapter) =
        appDao.insertCachedChapterBounded(chapter, MAX_CACHED_CHAPTERS)
    suspend fun clearAllCachedChapters() = appDao.clearAllCachedChapters()
    fun getCacheSizeBytes(): Flow<Long> = appDao.getCacheSizeBytes()

    private companion object {
        const val MAX_CACHED_CHAPTERS = 50
    }
}
