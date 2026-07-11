package com.spiritualdisciplines.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val date: String,
    val readBible: Boolean = false,
    val bibleProgress: String = "", // e.g., "0,2" for passages index 0 and 2
    val pray: Boolean = false,
    val reviewVerse: Boolean = false,
    val journal: Boolean = false,
    val giveThanks: Boolean = false,
    val prayForOthers: Boolean = false,
    val obeyApply: Boolean = false
)

@Entity(tableName = "prayer_requests")
data class PrayerRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val lastPrayedDate: String? = null,
    val isAnswered: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "memory_verses")
data class MemoryVerse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reference: String,
    val text: String,
    val lastReviewedDate: String? = null,
    val interval: Int = 1,
    val easeFactor: Float = 2.5f,
    val nextReviewDate: String? = null
)

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val date: String,
    val content: String = ""
)

@Entity(tableName = "cached_verses")
data class CachedVerse(
    @PrimaryKey val id: String, // e.g. "NIV-1-1-1"
    val translation: String,
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

@Entity(tableName = "cached_chapters")
data class CachedChapter(
    @PrimaryKey val id: String, // e.g. "NIV-1-1"
    val translation: String,
    val bookId: Int,
    val chapter: Int,
    val versesJson: String
)
