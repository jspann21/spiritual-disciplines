package com.spiritualdisciplines.data

import kotlin.math.max
import kotlin.math.roundToInt

data class MemoryReviewSchedule(
    val intervalDays: Int,
    val easeFactor: Float
)

object MemoryReviewScheduler {
    fun schedule(verse: MemoryVerse, quality: Int): MemoryReviewSchedule {
        require(quality in 1..5) { "Quality must be between 1 and 5" }

        val easeFactor = max(
            1.3f,
            verse.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        )
        val intervalDays = when {
            quality < 3 -> 1
            quality == 3 && verse.interval <= 1 -> 3
            quality == 3 -> max(verse.interval + 1, (verse.interval * easeFactor).roundToInt())
            verse.interval <= 1 -> 7
            else -> (verse.interval * (easeFactor + 0.3f)).roundToInt()
        }

        return MemoryReviewSchedule(intervalDays, easeFactor)
    }
}
