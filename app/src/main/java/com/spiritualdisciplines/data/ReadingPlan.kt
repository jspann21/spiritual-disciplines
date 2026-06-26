package com.spiritualdisciplines.data

data class DailyReading(
    val dayOfYear: Int,
    val passages: List<String>
)

interface BibleReadingPlan {
    val id: String
    val name: String
    val description: String
    val maxDays: Int
    fun getReadingForDay(dayOfYear: Int): DailyReading
}

abstract class GeneratedReadingPlan(
    override val id: String,
    override val name: String,
    override val description: String,
    private val generator: () -> List<DailyReading>
) : BibleReadingPlan {
    // Lazy initialization so we don't calculate everything immediately on app start
    private val schedule: Map<Int, List<String>> by lazy {
        generator().associate { it.dayOfYear to it.passages }
    }
    
    override val maxDays: Int
        get() = schedule.size

    override fun getReadingForDay(dayOfYear: Int): DailyReading {
        if (schedule.isEmpty()) return DailyReading(dayOfYear, emptyList())
        val safeDay = if (schedule.containsKey(dayOfYear)) dayOfYear else ((dayOfYear - 1) % schedule.size) + 1
        return DailyReading(dayOfYear, schedule[safeDay] ?: emptyList())
    }
}

object MCheynePlan : GeneratedReadingPlan(
    "mcheyne", 
    "M'Cheyne Reading Plan", 
    "Read the New Testament and Psalms twice a year, and the rest of the Bible once.",
    ReadingPlanGenerator::generateMCheyne
)

object ChronologicalPlan : GeneratedReadingPlan(
    "chronological",
    "Chronological Bible-in-a-Year",
    "Read the Bible in the chronological order of events.",
    ReadingPlanGenerator::generateChronological
)

object BibleRecapPlan : GeneratedReadingPlan(
    "biblerecap",
    "The Bible Recap",
    "A chronological reading plan designed to be used with The Bible Recap.",
    ReadingPlanGenerator::generateBibleRecap
)

object LigonierPlan : GeneratedReadingPlan(
    "ligonier",
    "Ligonier / Tabletalk",
    "Two readings each day, one from the Old Testament and one from the New Testament.",
    ReadingPlanGenerator::generateLigonier
)

object NavigatorsPlan : GeneratedReadingPlan(
    "navigators",
    "Navigators / Discipleship Journal Plan",
    "Read from four separate places in the Scriptures every day, 25 days a month.",
    ReadingPlanGenerator::generateNavigators
)

object FiveDayPlan : GeneratedReadingPlan(
    "fiveday",
    "Five Day Bible Reading Program",
    "Read the entire Bible in a year, reading only five days a week.",
    ReadingPlanGenerator::generateFiveDay
)

object F260Plan : GeneratedReadingPlan(
    "f260",
    "F-260 / Foundations 260",
    "A 260-day reading plan that highlights the foundational passages of Scripture.",
    ReadingPlanGenerator::generateF260
)

object BibleProjectPlan : GeneratedReadingPlan(
    "bibleproject",
    "BibleProject Read Scripture",
    "Read through the entire Bible in one year, with a Psalm prayed each day.",
    ReadingPlanGenerator::generateBibleProject
)

object ReadingPlanRepository {
    val plans = listOf(
        MCheynePlan,
        ChronologicalPlan,
        BibleRecapPlan,
        LigonierPlan,
        NavigatorsPlan,
        FiveDayPlan,
        F260Plan,
        BibleProjectPlan
    )
    
    fun getPlan(id: String): BibleReadingPlan {
        return plans.find { it.id == id } ?: MCheynePlan
    }
}
