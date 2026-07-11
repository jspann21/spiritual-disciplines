package com.spiritualdisciplines.data

data class BibleBook(
    val id: Int,
    val name: String,
    val chapters: Int
)

object BibleBooks {
    val all = listOf(
        BibleBook(1, "Genesis", 50),
        BibleBook(2, "Exodus", 40),
        BibleBook(3, "Leviticus", 27),
        BibleBook(4, "Numbers", 36),
        BibleBook(5, "Deuteronomy", 34),
        BibleBook(6, "Joshua", 24),
        BibleBook(7, "Judges", 21),
        BibleBook(8, "Ruth", 4),
        BibleBook(9, "1 Samuel", 31),
        BibleBook(10, "2 Samuel", 24),
        BibleBook(11, "1 Kings", 22),
        BibleBook(12, "2 Kings", 25),
        BibleBook(13, "1 Chronicles", 29),
        BibleBook(14, "2 Chronicles", 36),
        BibleBook(15, "Ezra", 10),
        BibleBook(16, "Nehemiah", 13),
        BibleBook(17, "Esther", 10),
        BibleBook(18, "Job", 42),
        BibleBook(19, "Psalms", 150),
        BibleBook(20, "Proverbs", 31),
        BibleBook(21, "Ecclesiastes", 12),
        BibleBook(22, "Song of Solomon", 8),
        BibleBook(23, "Isaiah", 66),
        BibleBook(24, "Jeremiah", 52),
        BibleBook(25, "Lamentations", 5),
        BibleBook(26, "Ezekiel", 48),
        BibleBook(27, "Daniel", 12),
        BibleBook(28, "Hosea", 14),
        BibleBook(29, "Joel", 3),
        BibleBook(30, "Amos", 9),
        BibleBook(31, "Obadiah", 1),
        BibleBook(32, "Jonah", 4),
        BibleBook(33, "Micah", 7),
        BibleBook(34, "Nahum", 3),
        BibleBook(35, "Habakkuk", 3),
        BibleBook(36, "Zephaniah", 3),
        BibleBook(37, "Haggai", 2),
        BibleBook(38, "Zechariah", 14),
        BibleBook(39, "Malachi", 4),
        BibleBook(40, "Matthew", 28),
        BibleBook(41, "Mark", 16),
        BibleBook(42, "Luke", 24),
        BibleBook(43, "John", 21),
        BibleBook(44, "Acts", 28),
        BibleBook(45, "Romans", 16),
        BibleBook(46, "1 Corinthians", 16),
        BibleBook(47, "2 Corinthians", 13),
        BibleBook(48, "Galatians", 6),
        BibleBook(49, "Ephesians", 6),
        BibleBook(50, "Philippians", 4),
        BibleBook(51, "Colossians", 4),
        BibleBook(52, "1 Thessalonians", 5),
        BibleBook(53, "2 Thessalonians", 3),
        BibleBook(54, "1 Timothy", 6),
        BibleBook(55, "2 Timothy", 4),
        BibleBook(56, "Titus", 3),
        BibleBook(57, "Philemon", 1),
        BibleBook(58, "Hebrews", 13),
        BibleBook(59, "James", 5),
        BibleBook(60, "1 Peter", 5),
        BibleBook(61, "2 Peter", 3),
        BibleBook(62, "1 John", 5),
        BibleBook(63, "2 John", 1),
        BibleBook(64, "3 John", 1),
        BibleBook(65, "Jude", 1),
        BibleBook(66, "Revelation", 22)
    )

    fun indexOf(name: String): Int = all.indexOfFirst { it.name.equals(name, ignoreCase = true) }

    fun idForName(name: String): Int? = all.firstOrNull { it.name == name }?.id

    fun sblAbbreviationFor(name: String): String = sblAbbreviations[name] ?: name

    private val sblAbbreviations = mapOf(
        "Genesis" to "Gen", "Exodus" to "Exod", "Leviticus" to "Lev", "Numbers" to "Num",
        "Deuteronomy" to "Deut", "Joshua" to "Josh", "Judges" to "Judg", "Ruth" to "Ruth",
        "1 Samuel" to "1 Sam", "2 Samuel" to "2 Sam", "1 Kings" to "1 Kgs", "2 Kings" to "2 Kgs",
        "1 Chronicles" to "1 Chr", "2 Chronicles" to "2 Chr", "Ezra" to "Ezra", "Nehemiah" to "Neh",
        "Esther" to "Esth", "Job" to "Job", "Psalms" to "Ps", "Proverbs" to "Prov",
        "Ecclesiastes" to "Eccl", "Song of Solomon" to "Song", "Isaiah" to "Isa", "Jeremiah" to "Jer",
        "Lamentations" to "Lam", "Ezekiel" to "Ezek", "Daniel" to "Dan", "Hosea" to "Hos",
        "Joel" to "Joel", "Amos" to "Amos", "Obadiah" to "Obad", "Jonah" to "Jon",
        "Micah" to "Mic", "Nahum" to "Nah", "Habakkuk" to "Hab", "Zephaniah" to "Zeph",
        "Haggai" to "Hag", "Zechariah" to "Zech", "Malachi" to "Mal", "Matthew" to "Matt",
        "Mark" to "Mark", "Luke" to "Luke", "John" to "John", "Acts" to "Acts",
        "Romans" to "Rom", "1 Corinthians" to "1 Cor", "2 Corinthians" to "2 Cor", "Galatians" to "Gal",
        "Ephesians" to "Eph", "Philippians" to "Phil", "Colossians" to "Col", "1 Thessalonians" to "1 Thess",
        "2 Thessalonians" to "2 Thess", "1 Timothy" to "1 Tim", "2 Timothy" to "2 Tim", "Titus" to "Titus",
        "Philemon" to "Phlm", "Hebrews" to "Heb", "James" to "Jas", "1 Peter" to "1 Pet",
        "2 Peter" to "2 Pet", "1 John" to "1 John", "2 John" to "2 John", "3 John" to "3 John",
        "Jude" to "Jude", "Revelation" to "Rev"
    )
}
