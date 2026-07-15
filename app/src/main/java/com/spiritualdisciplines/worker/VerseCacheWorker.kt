package com.spiritualdisciplines.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spiritualdisciplines.data.AppDatabase
import com.spiritualdisciplines.data.AppPreferences
import com.spiritualdisciplines.data.AppRepository
import com.spiritualdisciplines.data.CachedVerse
import com.spiritualdisciplines.ui.DailyVerse
import com.spiritualdisciplines.ui.dailyVerses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.concurrent.TimeUnit

class VerseCacheWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val repository = AppRepository(database.appDao())
            val preferences = AppPreferences(context)
            val translation = preferences.bibleTranslation.first()
            
            val calendar = Calendar.getInstance()
            val todayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val tomorrowDayOfYear = (todayDayOfYear % 365) + 1

            val keepIds = fetchAndCacheVerses(
                dayOfYears = listOf(todayDayOfYear, tomorrowDayOfYear),
                translation = translation,
                repository = repository
            )
            if (keepIds.isNotEmpty()) {
                repository.clearOldCachedVerses(keepIds)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchAndCacheVerses(
        dayOfYears: List<Int>,
        translation: String,
        repository: AppRepository
    ): List<String> {
        val targets = dayOfYears.map { dayOfYear ->
            val verse = dailyVerses[(dayOfYear - 1) % dailyVerses.size]
            CacheTarget(
                verse = verse,
                cacheId = "$translation-${verse.book}-${verse.chapter}-${verse.verse}"
            )
        }
        val cachedIds = targets.mapNotNull { target ->
            target.cacheId.takeIf { repository.getCachedVerse(it) != null }
        }
        val missingTargets = targets.filterNot { it.cacheId in cachedIds }
        if (missingTargets.isEmpty()) return cachedIds

        val rootArray = JSONArray()
        missingTargets.forEach { target ->
            rootArray.put(
                JSONObject().apply {
                    put("translation", translation)
                    put("book", target.verse.book)
                    put("chapter", target.verse.chapter)
                    put("verses", JSONArray().put(target.verse.verse))
                }
            )
        }

        val connection = URL("https://bolls.life/get-verses/").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            connection.outputStream.use { os ->
                val input = rootArray.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) return cachedIds

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val responseGroups = JSONArray(response)
            val fetchedIds = buildList {
                missingTargets.forEachIndexed { index, target ->
                    if (index >= responseGroups.length()) return@forEachIndexed
                    val verseItems = responseGroups.getJSONArray(index)
                    if (verseItems.length() == 0) return@forEachIndexed

                    val text = buildString {
                        for (i in 0 until verseItems.length()) {
                            if (isNotEmpty()) append(' ')
                            append(
                                verseItems.getJSONObject(i).getString("text")
                                    .replace(HTML_TAG_REGEX, "")
                                    .trim()
                            )
                        }
                    }
                    repository.insertCachedVerse(
                        CachedVerse(
                            id = target.cacheId,
                            translation = translation,
                            bookId = target.verse.book,
                            chapter = target.verse.chapter,
                            verse = target.verse.verse,
                            text = text
                        )
                    )
                    add(target.cacheId)
                }
            }
            cachedIds + fetchedIds
        } catch (_: Exception) {
            cachedIds
        } finally {
            connection.disconnect()
        }
    }

    private data class CacheTarget(
        val verse: DailyVerse,
        val cacheId: String
    )

    companion object {
        private const val WORK_NAME = "verse_cache_worker"
        private val HTML_TAG_REGEX = Regex("<.*?>")

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = PeriodicWorkRequestBuilder<VerseCacheWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
