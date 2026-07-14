package com.spiritualdisciplines.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE) ?: DEFAULT_THEME_MODE)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _bibleFont = MutableStateFlow(prefs.getString(KEY_BIBLE_FONT, DEFAULT_BIBLE_FONT) ?: DEFAULT_BIBLE_FONT)
    val bibleFont: StateFlow<String> = _bibleFont.asStateFlow()

    private val _bibleTextSize = MutableStateFlow(prefs.getInt(KEY_BIBLE_TEXT_SIZE, DEFAULT_BIBLE_TEXT_SIZE))
    val bibleTextSize: StateFlow<Int> = _bibleTextSize.asStateFlow()

    private val _bibleTranslation = MutableStateFlow(prefs.getString(KEY_BIBLE_TRANSLATION, DEFAULT_BIBLE_TRANSLATION) ?: DEFAULT_BIBLE_TRANSLATION)
    val bibleTranslation: StateFlow<String> = _bibleTranslation.asStateFlow()

    private val _readingPlanId = MutableStateFlow(prefs.getString(KEY_READING_PLAN_ID, DEFAULT_READING_PLAN_ID) ?: DEFAULT_READING_PLAN_ID)
    val readingPlanId: StateFlow<String> = _readingPlanId.asStateFlow()

    private val _readingPlanStartDate = MutableStateFlow(prefs.getLong(KEY_READING_PLAN_START_DATE, LocalDate.now().toEpochDay()))
    val readingPlanStartDate: StateFlow<Long> = _readingPlanStartDate.asStateFlow()

    private val _showStreak = MutableStateFlow(prefs.getBoolean(KEY_SHOW_STREAK, true))
    val showStreak: StateFlow<Boolean> = _showStreak.asStateFlow()

    private val _showReadBible = MutableStateFlow(prefs.getBoolean(KEY_SHOW_READ_BIBLE, true))
    val showReadBible: StateFlow<Boolean> = _showReadBible.asStateFlow()

    private val _showPray = MutableStateFlow(prefs.getBoolean(KEY_SHOW_PRAY, true))
    val showPray: StateFlow<Boolean> = _showPray.asStateFlow()

    private val _showReviewVerse = MutableStateFlow(prefs.getBoolean(KEY_SHOW_REVIEW_VERSE, true))
    val showReviewVerse: StateFlow<Boolean> = _showReviewVerse.asStateFlow()

    private val _showJournal = MutableStateFlow(prefs.getBoolean(KEY_SHOW_JOURNAL, true))
    val showJournal: StateFlow<Boolean> = _showJournal.asStateFlow()

    private val _showGiveThanks = MutableStateFlow(prefs.getBoolean(KEY_SHOW_GIVE_THANKS, true))
    val showGiveThanks: StateFlow<Boolean> = _showGiveThanks.asStateFlow()

    private val _showPrayForOthers = MutableStateFlow(prefs.getBoolean(KEY_SHOW_PRAY_FOR_OTHERS, true))
    val showPrayForOthers: StateFlow<Boolean> = _showPrayForOthers.asStateFlow()

    private val _showObeyApply = MutableStateFlow(prefs.getBoolean(KEY_SHOW_OBEY_APPLY, true))
    val showObeyApply: StateFlow<Boolean> = _showObeyApply.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationHour = MutableStateFlow(prefs.getInt(KEY_NOTIFICATION_HOUR, DEFAULT_NOTIFICATION_HOUR))
    val notificationHour: StateFlow<Int> = _notificationHour.asStateFlow()

    private val _notificationMinute = MutableStateFlow(prefs.getInt(KEY_NOTIFICATION_MINUTE, DEFAULT_NOTIFICATION_MINUTE))
    val notificationMinute: StateFlow<Int> = _notificationMinute.asStateFlow()

    private val _accentColor = MutableStateFlow(prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR))
    val accentColor: StateFlow<Int> = _accentColor.asStateFlow()

    fun setThemeMode(mode: String) {
        setString(KEY_THEME_MODE, mode, _themeMode)
    }

    fun setBibleFont(font: String) {
        setString(KEY_BIBLE_FONT, font, _bibleFont)
    }

    fun setBibleTextSize(size: Int) {
        prefs.edit { putInt(KEY_BIBLE_TEXT_SIZE, size) }
        _bibleTextSize.value = size
    }

    fun setBibleTranslation(translation: String) {
        setString(KEY_BIBLE_TRANSLATION, translation, _bibleTranslation)
    }

    fun setReadingPlanId(planId: String) {
        setString(KEY_READING_PLAN_ID, planId, _readingPlanId)
    }

    fun setReadingPlanStartDate(epochDay: Long) {
        prefs.edit { putLong(KEY_READING_PLAN_START_DATE, epochDay) }
        _readingPlanStartDate.value = epochDay
    }

    fun setShowStreak(show: Boolean) {
        setBoolean(KEY_SHOW_STREAK, show, _showStreak)
    }

    fun setShowReadBible(show: Boolean) {
        setBoolean(KEY_SHOW_READ_BIBLE, show, _showReadBible)
    }

    fun setShowPray(show: Boolean) {
        setBoolean(KEY_SHOW_PRAY, show, _showPray)
    }

    fun setShowReviewVerse(show: Boolean) {
        setBoolean(KEY_SHOW_REVIEW_VERSE, show, _showReviewVerse)
    }

    fun setShowJournal(show: Boolean) {
        setBoolean(KEY_SHOW_JOURNAL, show, _showJournal)
    }

    fun setShowGiveThanks(show: Boolean) {
        setBoolean(KEY_SHOW_GIVE_THANKS, show, _showGiveThanks)
    }

    fun setShowPrayForOthers(show: Boolean) {
        setBoolean(KEY_SHOW_PRAY_FOR_OTHERS, show, _showPrayForOthers)
    }

    fun setShowObeyApply(show: Boolean) {
        setBoolean(KEY_SHOW_OBEY_APPLY, show, _showObeyApply)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        setBoolean(KEY_NOTIFICATIONS_ENABLED, enabled, _notificationsEnabled)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit {
            putInt(KEY_NOTIFICATION_HOUR, hour)
            putInt(KEY_NOTIFICATION_MINUTE, minute)
        }
        _notificationHour.value = hour
        _notificationMinute.value = minute
    }

    fun setAccentColor(color: Int) {
        prefs.edit { putInt(KEY_ACCENT_COLOR, color) }
        _accentColor.value = color
    }

    private fun setString(key: String, value: String, state: MutableStateFlow<String>) {
        prefs.edit { putString(key, value) }
        state.value = value
    }

    private fun setBoolean(key: String, value: Boolean, state: MutableStateFlow<Boolean>) {
        prefs.edit { putBoolean(key, value) }
        state.value = value
    }

    private companion object {
        const val PREFS_NAME = "app_settings"

        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_BIBLE_FONT = "bible_font"
        const val KEY_BIBLE_TEXT_SIZE = "bible_text_size"
        const val KEY_BIBLE_TRANSLATION = "bible_translation"
        const val KEY_READING_PLAN_ID = "reading_plan_id"
        const val KEY_READING_PLAN_START_DATE = "reading_plan_start_date"
        const val KEY_SHOW_STREAK = "show_streak"
        const val KEY_SHOW_READ_BIBLE = "show_read_bible"
        const val KEY_SHOW_PRAY = "show_pray"
        const val KEY_SHOW_REVIEW_VERSE = "show_review_verse"
        const val KEY_SHOW_JOURNAL = "show_journal"
        const val KEY_SHOW_GIVE_THANKS = "show_give_thanks"
        const val KEY_SHOW_PRAY_FOR_OTHERS = "show_pray_for_others"
        const val KEY_SHOW_OBEY_APPLY = "show_obey_apply"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_NOTIFICATION_HOUR = "notification_hour"
        const val KEY_NOTIFICATION_MINUTE = "notification_minute"
        const val KEY_ACCENT_COLOR = "accent_color"

        const val DEFAULT_THEME_MODE = "system"
        const val DEFAULT_BIBLE_FONT = "literata"
        const val DEFAULT_BIBLE_TEXT_SIZE = 18
        const val DEFAULT_BIBLE_TRANSLATION = "ESV"
        const val DEFAULT_READING_PLAN_ID = "mcheyne"
        const val DEFAULT_NOTIFICATION_HOUR = 8
        const val DEFAULT_NOTIFICATION_MINUTE = 0
        const val DEFAULT_ACCENT_COLOR = 0xFF8B4513.toInt()
    }
}
