package com.spiritualdisciplines.data

object ReadingPlanGenerator {
    private fun plan(id: String): List<DailyReading> = ReadingPlanData.plan(id)

    fun generateMCheyne(): List<DailyReading> = plan("mcheyne")

    fun generateChronological(): List<DailyReading> = plan("chronological")

    fun generateBibleRecap(): List<DailyReading> = plan("biblerecap")

    fun generateLigonier(): List<DailyReading> = plan("ligonier")

    fun generateNavigators(): List<DailyReading> = plan("navigators")

    fun generateFiveDay(): List<DailyReading> = plan("fiveday")

    fun generateF260(): List<DailyReading> = plan("f260")

    fun generateBibleProject(): List<DailyReading> = plan("bibleproject")
}
