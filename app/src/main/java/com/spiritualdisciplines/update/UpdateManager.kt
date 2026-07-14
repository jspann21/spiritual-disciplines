package com.spiritualdisciplines.update

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val update: AppUpdate) : UpdateUiState
    data class UpToDate(val currentVersionName: String) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateManager(context: Context) {
    private val appContext = context.applicationContext
    private val checker = UpdateChecker(appContext)
    private val store = UpdateCheckStore(appContext)
    private val checkMutex = Mutex()
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    val currentVersionName: String
        get() = UpdateChecker.installedVersionName(appContext)

    suspend fun check(manual: Boolean) = checkMutex.withLock {
        if (!manual && !store.isAutomaticCheckDue()) return@withLock

        if (manual) _state.value = UpdateUiState.Checking

        when (val result = checker.check()) {
            is UpdateCheckResult.Available -> {
                store.markChecked()
                _state.value = UpdateUiState.Available(result.update)
            }
            is UpdateCheckResult.UpToDate -> {
                store.markChecked()
                _state.value = if (manual) {
                    UpdateUiState.UpToDate(result.currentVersionName)
                } else {
                    UpdateUiState.Idle
                }
            }
            is UpdateCheckResult.Error -> {
                _state.value = if (manual) {
                    UpdateUiState.Error(result.message)
                } else {
                    UpdateUiState.Idle
                }
            }
        }
    }

    fun clearStatus() {
        _state.value = UpdateUiState.Idle
    }
}

class UpdateCheckStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAutomaticCheckDue(now: Long = System.currentTimeMillis()): Boolean =
        now - prefs.getLong(KEY_LAST_CHECKED_AT, 0L) >= AUTO_CHECK_INTERVAL_MILLIS

    fun markChecked(now: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(KEY_LAST_CHECKED_AT, now) }
    }

    fun wasNotified(versionName: String): Boolean =
        prefs.getString(KEY_LAST_NOTIFIED_VERSION, null) == versionName

    fun markNotified(versionName: String) {
        prefs.edit { putString(KEY_LAST_NOTIFIED_VERSION, versionName) }
    }

    private companion object {
        const val PREFS_NAME = "app_update_checks"
        const val KEY_LAST_CHECKED_AT = "last_checked_at"
        const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
        val AUTO_CHECK_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(24)
    }
}
