package com.spiritualdisciplines.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spiritualdisciplines.data.AppDatabase
import com.spiritualdisciplines.data.AppPreferences
import com.spiritualdisciplines.data.AppRepository
import com.spiritualdisciplines.data.CachedVerse
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

            val todayId = fetchAndCacheVerse(todayDayOfYear, translation, repository)
            val tomorrowId = fetchAndCacheVerse(tomorrowDayOfYear, translation, repository)

            val keepIds = listOfNotNull(todayId, tomorrowId)
            if (keepIds.isNotEmpty()) {
                repository.clearOldCachedVerses(keepIds)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun fetchAndCacheVerse(dayOfYear: Int, translation: String, repository: AppRepository): String? {
        val dailyVerse = dailyVerses[(dayOfYear - 1) % dailyVerses.size]
        val cacheId = "$translation-${dailyVerse.book}-${dailyVerse.chapter}-${dailyVerse.verse}"
        
        // Skip if already cached
        val existing = repository.getCachedVerse(cacheId)
        if (existing != null) return cacheId

        try {
            val url = URL("https://bolls.life/get-verses/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val reqObj = JSONObject()
            reqObj.put("translation", translation)
            reqObj.put("book", dailyVerse.book)
            reqObj.put("chapter", dailyVerse.chapter)
            val versesArray = JSONArray()
            versesArray.put(dailyVerse.verse)
            reqObj.put("verses", versesArray)
            
            val rootArray = JSONArray().put(reqObj)
            
            connection.outputStream.use { os ->
                val input = rootArray.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val resArray = JSONArray(response)
                if (resArray.length() > 0) {
                    val verseItems = resArray.getJSONArray(0)
                    if (verseItems.length() > 0) {
                        val sb = StringBuilder()
                        for (i in 0 until verseItems.length()) {
                            val vObj = verseItems.getJSONObject(i)
                            val vText = vObj.getString("text").replace(Regex("<.*?>"), "").trim()
                            sb.append(vText).append(" ")
                        }
                        val votdText = sb.toString().trim()
                        
                        repository.insertCachedVerse(
                            CachedVerse(
                                id = cacheId,
                                translation = translation,
                                bookId = dailyVerse.book,
                                chapter = dailyVerse.chapter,
                                verse = dailyVerse.verse,
                                text = votdText
                            )
                        )
                        return cacheId
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    companion object {
        private const val WORK_NAME = "verse_cache_worker"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<VerseCacheWorker>(12, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
