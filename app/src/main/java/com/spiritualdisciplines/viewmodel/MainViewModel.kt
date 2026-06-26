package com.spiritualdisciplines.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spiritualdisciplines.data.AppPreferences
import com.spiritualdisciplines.data.AppRepository
import com.spiritualdisciplines.data.CachedChapter
import com.spiritualdisciplines.data.CachedVerse
import com.spiritualdisciplines.data.DailyRecord
import com.spiritualdisciplines.data.JournalEntry
import com.spiritualdisciplines.data.MemoryVerse
import com.spiritualdisciplines.data.PrayerRequest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

class MainViewModel(private val repository: AppRepository, val preferences: AppPreferences) : ViewModel() {

    val themeMode: StateFlow<String> = preferences.themeMode
    val bibleTranslation: StateFlow<String> = preferences.bibleTranslation
    val readingPlanId: StateFlow<String> = preferences.readingPlanId
    val readingPlanStartDate: StateFlow<Long> = preferences.readingPlanStartDate
    val showStreak: StateFlow<Boolean> = preferences.showStreak
    val showReadBible: StateFlow<Boolean> = preferences.showReadBible
    val showPray: StateFlow<Boolean> = preferences.showPray
    val showReviewVerse: StateFlow<Boolean> = preferences.showReviewVerse
    val showJournal: StateFlow<Boolean> = preferences.showJournal
    val showGiveThanks: StateFlow<Boolean> = preferences.showGiveThanks
    val showPrayForOthers: StateFlow<Boolean> = preferences.showPrayForOthers
    val showObeyApply: StateFlow<Boolean> = preferences.showObeyApply
    val accentColor: StateFlow<Int> = preferences.accentColor
    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled
    val notificationHour: StateFlow<Int> = preferences.notificationHour
    val notificationMinute: StateFlow<Int> = preferences.notificationMinute

    val todayDateString: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val todayRecord: StateFlow<DailyRecord> = repository.getDailyRecord(todayDateString)
        .map { it ?: DailyRecord(todayDateString) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyRecord(todayDateString))

    val allDailyRecords: StateFlow<List<DailyRecord>> = repository.getAllDailyRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prayerRequests: StateFlow<List<PrayerRequest>> = repository.getAllPrayerRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryVerses: StateFlow<List<MemoryVerse>> = repository.getAllMemoryVerses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayJournal: StateFlow<JournalEntry> = repository.getJournalEntry(todayDateString)
        .map { it ?: JournalEntry(todayDateString) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalEntry(todayDateString))

    val allJournalEntries: StateFlow<List<JournalEntry>> = repository.getAllJournalEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cacheSizeBytes: StateFlow<Long> = repository.getCacheSizeBytes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun updateDailyRecord(update: (DailyRecord) -> DailyRecord) {
        viewModelScope.launch {
            val current = todayRecord.value
            repository.insertDailyRecord(update(current))
        }
    }

    fun addPrayerRequest(title: String, category: String) {
        viewModelScope.launch {
            repository.insertPrayerRequest(PrayerRequest(title = title, category = category))
        }
    }

    fun updatePrayerRequest(id: Int, title: String, category: String) {
        viewModelScope.launch {
            prayerRequests.value.find { it.id == id }?.let { existing ->
                repository.insertPrayerRequest(existing.copy(title = title, category = category))
            }
        }
    }

    fun archivePrayerRequest(id: Int, archived: Boolean) {
        viewModelScope.launch {
            prayerRequests.value.find { it.id == id }?.let { existing ->
                repository.insertPrayerRequest(existing.copy(isArchived = archived))
            }
        }
    }

    fun markPrayerPrayed(id: Int) {
        viewModelScope.launch {
            repository.updatePrayerLastPrayed(id, todayDateString)
        }
    }

    fun markPrayerAnswered(id: Int, isAnswered: Boolean) {
        viewModelScope.launch {
            repository.updatePrayerAnswered(id, isAnswered)
        }
    }

    fun deletePrayerRequest(id: Int) {
        viewModelScope.launch {
            repository.deletePrayerRequest(id)
        }
    }

    fun addMemoryVerse(reference: String, text: String) {
        viewModelScope.launch {
            repository.insertMemoryVerse(MemoryVerse(reference = reference, text = text))
        }
    }

    fun markVerseReviewed(id: Int) {
        viewModelScope.launch {
            repository.updateVerseLastReviewed(id, todayDateString)
        }
    }

    fun updateVerseReview(verseId: Int, quality: Int) {
        viewModelScope.launch {
            val verse = memoryVerses.value.find { it.id == verseId } ?: return@launch
            
            val easeFactor = max(
                1.3f,
                verse.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
            )
            
            val interval = when {
                quality < 3 -> 1
                verse.interval == 1 -> 6
                else -> (verse.interval * easeFactor).roundToInt()
            }
            
            val nextReviewDate = LocalDate.now().plusDays(interval.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            val updatedVerse = verse.copy(
                lastReviewedDate = todayDateString,
                interval = interval,
                easeFactor = easeFactor,
                nextReviewDate = nextReviewDate
            )
            repository.insertMemoryVerse(updatedVerse)
        }
    }

    fun deleteMemoryVerse(id: Int) {
        viewModelScope.launch {
            repository.deleteMemoryVerse(id)
        }
    }

    fun updateJournalEntry(update: (JournalEntry) -> JournalEntry) {
        viewModelScope.launch {
            val current = todayJournal.value
            repository.insertJournalEntry(update(current))
        }
    }

    suspend fun getCachedVerse(id: String): CachedVerse? {
        return repository.getCachedVerse(id)
    }

    suspend fun insertCachedVerse(verse: CachedVerse) {
        repository.insertCachedVerse(verse)
    }

    suspend fun getCachedChapter(id: String): CachedChapter? {
        return repository.getCachedChapter(id)
    }

    suspend fun insertCachedChapter(chapter: CachedChapter) {
        repository.insertCachedChapter(chapter)
    }

    fun clearAllCache(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllCachedVerses()
            repository.clearAllCachedChapters()
            onComplete()
        }
    }
}

class MainViewModelFactory(private val repository: AppRepository, private val preferences: AppPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
