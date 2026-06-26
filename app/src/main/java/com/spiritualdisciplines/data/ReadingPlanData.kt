package com.spiritualdisciplines.data

// Fixed reading plan schedules used by the app.
object ReadingPlanData {
    val plansById: Map<String, List<DailyReading>> by lazy {
        mapOf(
            "mcheyne" to MCheyneReadingPlanData.readings,
            "chronological" to ChronologicalReadingPlanData.readings,
            "biblerecap" to BibleRecapReadingPlanData.readings,
            "ligonier" to LigonierReadingPlanData.readings,
            "navigators" to NavigatorsReadingPlanData.readings,
            "fiveday" to FiveDayReadingPlanData.readings,
            "f260" to F260ReadingPlanData.readings,
            "bibleproject" to BibleProjectReadingPlanData.readings
        )
    }

    fun plan(id: String): List<DailyReading> = when (id) {
        "mcheyne" -> MCheyneReadingPlanData.readings
        "chronological" -> ChronologicalReadingPlanData.readings
        "biblerecap" -> BibleRecapReadingPlanData.readings
        "ligonier" -> LigonierReadingPlanData.readings
        "navigators" -> NavigatorsReadingPlanData.readings
        "fiveday" -> FiveDayReadingPlanData.readings
        "f260" -> F260ReadingPlanData.readings
        "bibleproject" -> BibleProjectReadingPlanData.readings
        else -> error("Unknown reading plan: $id")
    }
}

