package com.spiritualdisciplines.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BollsVerseRequest(
    val translation: String,
    val bookId: Int,
    val chapter: Int,
    val verses: List<Int>
)

data class BollsChapterResponse(
    val rawJson: String,
    val verses: JSONArray
)

class BollsApiException(message: String) : Exception(message)

object BollsBibleApi {
    suspend fun fetchVerseGroups(requests: List<BollsVerseRequest>): List<List<String>> =
        withContext(Dispatchers.IO) {
            if (requests.isEmpty()) return@withContext emptyList()

            val requestBody = JSONArray().apply {
                requests.forEach { request ->
                    put(
                        JSONObject().apply {
                            put("translation", request.translation)
                            put("book", request.bookId)
                            put("chapter", request.chapter)
                            put("verses", JSONArray(request.verses))
                        }
                    )
                }
            }

            val response = execute(
                url = VERSES_URL,
                method = "POST",
                body = requestBody.toString()
            )
            val groups = JSONArray(response)

            buildList {
                for (groupIndex in 0 until groups.length()) {
                    val group = groups.getJSONArray(groupIndex)
                    add(
                        buildList {
                            for (verseIndex in 0 until group.length()) {
                                add(group.getJSONObject(verseIndex).getString("text"))
                            }
                        }
                    )
                }
            }
        }

    suspend fun fetchChapter(
        translation: String,
        bookId: Int,
        chapter: Int
    ): BollsChapterResponse = withContext(Dispatchers.IO) {
        val response = execute(
            url = "$CHAPTER_URL/$translation/$bookId/$chapter/",
            method = "GET"
        )
        BollsChapterResponse(rawJson = response, verses = JSONArray(response))
    }

    private fun execute(url: String, method: String, body: String? = null): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)

            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw BollsApiException("Bolls returned HTTP $responseCode")
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private const val VERSES_URL = "https://bolls.life/get-verses/"
    private const val CHAPTER_URL = "https://bolls.life/get-chapter"
    private const val TIMEOUT_MILLIS = 10_000
    private const val USER_AGENT = "Spiritual-Disciplines-Android"
}
