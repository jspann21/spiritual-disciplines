package com.spiritualdisciplines.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val releaseUrl: String
)

sealed interface UpdateCheckResult {
    data class Available(val update: AppUpdate) : UpdateCheckResult
    data class UpToDate(val currentVersionName: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

class UpdateChecker(private val context: Context) {
    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentVersion = installedVersionName(context)
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            setRequestProperty("User-Agent", "Spiritual-Disciplines-Android")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext UpdateCheckResult.Error(
                    if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        "No published release was found."
                    } else {
                        "GitHub returned an error ($responseCode)."
                    }
                )
            }

            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val latestVersion = release.getString("tag_name").removePrefix("v")
            val releaseUrl = release.getString("html_url")

            if (VersionComparator.isNewer(latestVersion, currentVersion)) {
                UpdateCheckResult.Available(AppUpdate(latestVersion, releaseUrl))
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (_: Exception) {
            UpdateCheckResult.Error("Couldn't reach GitHub. Check your connection and try again.")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/jspann21/spiritual-disciplines/releases/latest"
        private const val TIMEOUT_MILLIS = 10_000

        fun installedVersionName(context: Context): String =
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    }
}

object VersionComparator {
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = numericParts(candidate)
        val currentParts = numericParts(current)
        val partCount = maxOf(candidateParts.size, currentParts.size)

        for (index in 0 until partCount) {
            val candidatePart = candidateParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (candidatePart != currentPart) return candidatePart > currentPart
        }

        return false
    }

    private fun numericParts(version: String): List<Int> = version
        .removePrefix("v")
        .substringBefore('-')
        .split('.')
        .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}
