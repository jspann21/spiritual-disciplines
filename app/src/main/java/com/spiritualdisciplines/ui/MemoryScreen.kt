@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.spiritualdisciplines.ui

import com.spiritualdisciplines.ui.theme.LocalBibleFontFamily

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.data.BibleBooks
import com.spiritualdisciplines.data.BibleVerseCounts
import com.spiritualdisciplines.data.MemoryVerse
import com.spiritualdisciplines.data.MemoryReviewScheduler
import com.spiritualdisciplines.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(viewModel: MainViewModel) {
    val haptics = rememberExpressiveHaptics()
    val verses by viewModel.memoryVerses.collectAsStateWithLifecycle()
    val translation by viewModel.bibleTranslation.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var practiceVerseId by remember { mutableStateOf<Int?>(null) }
    val dueCount = verses.count { it.nextReviewDate == null || it.nextReviewDate <= viewModel.todayDateString }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DisciplineTabRow(
                tabs = listOf("Library", "Practice"),
                selectedTabIndex = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    if (index == 0) practiceVerseId = null
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0 && verses.isNotEmpty()) {
                FloatingActionButton(onClick = { haptics.pressed { showDialog = true } }) {
                    Icon(Icons.Default.Add, contentDescription = "Add verse")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedTab == 0) {
                MemoryLibrary(
                    verses = verses,
                    dueCount = dueCount,
                    today = viewModel.todayDateString,
                    onPracticeDue = {
                        practiceVerseId = null
                        selectedTab = 1
                    },
                    onPracticeVerse = { id ->
                        practiceVerseId = id
                        selectedTab = 1
                    },
                    onDelete = viewModel::deleteMemoryVerse,
                    onAdd = { showDialog = true }
                )
            } else {
                PracticeView(
                    viewModel = viewModel,
                    verses = verses,
                    selectedVerseId = practiceVerseId,
                    onBackToLibrary = {
                        practiceVerseId = null
                        selectedTab = 0
                    }
                )
            }
        }

        if (showDialog) {
            AddVerseDialog(
                translation = translation,
                onDismiss = { showDialog = false },
                onAdd = { ref, txt ->
                    viewModel.addMemoryVerse(ref, txt)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
private fun MemoryLibrary(
    verses: List<MemoryVerse>,
    dueCount: Int,
    today: String,
    onPracticeDue: () -> Unit,
    onPracticeVerse: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: () -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (dueCount > 0) "$dueCount ${if (dueCount == 1) "verse" else "verses"} ready" else "You're caught up",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (dueCount > 0) "A short review now will strengthen what you remember."
                        else "Practice any verse below, or come back when the next one is due.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dueCount > 0) {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { haptics.pressed(onPracticeDue) }, shapes = ButtonDefaults.shapes()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start review")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your verses", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("${verses.size} total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (verses.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Begin with one verse", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Choose a passage you want to carry with you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { haptics.pressed(onAdd) }, shapes = ButtonDefaults.shapes()) { Text("Add verse") }
                }
            }
        } else {
            items(verses, key = { it.id }) { verse ->
                DisciplineCard(
                    onClick = { haptics.selected { onPracticeVerse(verse.id) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(verse.reference, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                verse.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LocalBibleFontFamily.current),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                reviewStatus(verse, today),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (verse.nextReviewDate == null || verse.nextReviewDate <= today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { haptics.pressed { onDelete(verse.id) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${verse.reference}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun reviewStatus(verse: MemoryVerse, today: String): String {
    val next = verse.nextReviewDate ?: return "New · ready to practice"
    if (next <= today) return "Due today"
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(next))
    return if (days == 1L) "Next review tomorrow" else "Next review in $days days"
}

fun toFirstLetter(text: String): String {
    val builder = StringBuilder()
    var inWord = false
    for (i in text.indices) {
        val c = text[i]
        if (c.isLetterOrDigit()) {
            if (!inWord) {
                builder.append(c)
                inWord = true
            }
        } else {
            builder.append(c)
            inWord = false
        }
    }
    return builder.toString()
}

fun hideWord(word: String): String {
    var prefix = ""
    var suffix = ""
    var core = word
    
    while (core.isNotEmpty() && !core.first().isLetterOrDigit()) {
        prefix += core.first()
        core = core.drop(1)
    }
    while (core.isNotEmpty() && !core.last().isLetterOrDigit()) {
        suffix = core.last() + suffix
        core = core.dropLast(1)
    }
    
    val blank = "_".repeat(core.length.coerceAtLeast(3))
    return if (core.isEmpty()) word else "$prefix$blank$suffix"
}

fun toFillInTheBlanks(text: String, hidePercentage: Float = 0.4f): String {
    val words = Regex("\\S+").findAll(text).toList()
    if (words.isEmpty()) return text

    val hideCount = (words.size * hidePercentage).toInt().coerceAtLeast(1)
    val hideIndices = words.indices.shuffled().take(hideCount).toSet()
    var cursor = 0

    return buildString {
        words.forEachIndexed { index, match ->
            append(text, cursor, match.range.first)
            append(if (index in hideIndices) hideWord(match.value) else match.value)
            cursor = match.range.last + 1
        }
        append(text, cursor, text.length)
    }
}

private data class LastMemoryReview(val verse: MemoryVerse, val days: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeView(
    viewModel: MainViewModel,
    verses: List<MemoryVerse>,
    selectedVerseId: Int? = null,
    onBackToLibrary: () -> Unit = {}
) {
    val haptics = rememberExpressiveHaptics()
    val today = viewModel.todayDateString
    val verseIds = remember(verses) { verses.map { it.id } }
    val sessionIds = remember(selectedVerseId, verseIds) {
        if (selectedVerseId != null) {
            listOfNotNull(selectedVerseId.takeIf { id -> verses.any { it.id == id } })
        } else {
            verses.filter { it.nextReviewDate == null || it.nextReviewDate <= today }.map { it.id }
        }
    }
    var currentIndex by remember(selectedVerseId, verseIds) { mutableIntStateOf(0) }
    var mode by remember(selectedVerseId, verseIds) { mutableStateOf("Study") }
    var showAnswer by remember(selectedVerseId, verseIds) { mutableStateOf(false) }
    var showRatingDialog by remember(selectedVerseId, verseIds) { mutableStateOf(false) }
    var blanksSeed by remember(selectedVerseId, verseIds) { mutableIntStateOf(0) }
    var lastReview by remember(selectedVerseId, verseIds) { mutableStateOf<LastMemoryReview?>(null) }

    fun undoLastReview() {
        val review = lastReview ?: return
        viewModel.restoreVerseReview(review.verse)
        currentIndex = (currentIndex - 1).coerceAtLeast(0)
        showAnswer = true
        showRatingDialog = false
        lastReview = null
    }

    if (sessionIds.isEmpty()) {
        PracticeEmptyState(onBackToLibrary)
        return
    }

    if (currentIndex >= sessionIds.size) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Review complete", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "You practiced ${sessionIds.size} ${if (sessionIds.size == 1) "verse" else "verses"}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            lastReview?.let { review ->
                Spacer(Modifier.height(20.dp))
                Text("${review.verse.reference} · ${intervalLabel(review.days)}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = ::undoLastReview) { Text("Undo last rating") }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBackToLibrary, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes()) { Text("Return to library") }
            TextButton(
                onClick = {
                    currentIndex = 0
                    mode = "Study"
                    showAnswer = false
                    showRatingDialog = false
                    lastReview = null
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Practice these again")
            }
        }
        return
    }

    val currentVerse = verses.firstOrNull { it.id == sessionIds[currentIndex] } ?: return
    val displayVerseText = remember(currentVerse.text, mode, showAnswer, blanksSeed) {
        when {
            showAnswer || mode == "Study" -> currentVerse.text
            mode == "First letters" -> toFirstLetter(currentVerse.text)
            mode == "Blanks" -> toFillInTheBlanks(currentVerse.text)
            else -> "Recite the verse aloud, then check your answer."
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToLibrary) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to library")
            }
            Text(
                if (selectedVerseId == null) "Review ${currentIndex + 1} of ${sessionIds.size}" else "Free practice",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (selectedVerseId == null) {
                Text("${sessionIds.size - currentIndex} left", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        lastReview?.let { review ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${review.verse.reference} · ${intervalLabel(review.days)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = ::undoLastReview) { Text("Undo") }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Study", "First letters", "Blanks").forEach { item ->
                FilterChip(
                    selected = mode == item,
                    onClick = {
                        haptics.select()
                        mode = item
                        showAnswer = false
                        showRatingDialog = false
                        if (item == "Blanks") blanksSeed++
                    },
                    label = { Text(item) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(currentVerse.reference, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Text(
                    displayVerseText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = LocalBibleFontFamily.current,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        when {
            mode == "Study" -> {
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        haptics.press()
                        mode = "Recall"
                        showAnswer = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Try it from memory") }
            }
            !showAnswer -> {
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { haptics.pressed { showAnswer = true } },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Show answer") }
            }
            else -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { haptics.pressed { showAnswer = false } },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Hide answer") }
                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = { haptics.pressed { showRatingDialog = true } },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Finish review") }
                }
            }
        }
    }

    if (showRatingDialog) {
        Dialog(onDismissRequest = { showRatingDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Recall result", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Before revealing, how much did you recall?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    listOf(
                        1 to "Missed it",
                        3 to "Mostly",
                        5 to "Fully"
                    ).forEachIndexed { index, (quality, label) ->
                        val days = nextInterval(currentVerse, quality)
                        Surface(
                            onClick = {
                                haptics.confirm()
                                val snapshot = currentVerse
                                viewModel.updateVerseReview(currentVerse.id, quality)
                                lastReview = LastMemoryReview(snapshot, days)
                                currentIndex++
                                showAnswer = false
                                showRatingDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(intervalLabel(days), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (index < 2) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { showRatingDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Continue practicing") }
                }
            }
        }
    }
}

@Composable
private fun PracticeEmptyState(onBackToLibrary: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Nothing due today", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Your next review will appear here when it's time.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onBackToLibrary, shapes = ButtonDefaults.shapes()) { Text("Choose a verse to practice") }
    }
}

private fun nextInterval(verse: MemoryVerse, quality: Int): Int {
    return MemoryReviewScheduler.schedule(verse, quality).intervalDays
}

private fun intervalLabel(days: Int) = if (days == 1) "Tomorrow" else "In $days days"

enum class PickerStage {
    BOOK, CHAPTER, VERSE, REVIEW
}

private suspend fun fetchMemoryVerses(
    translation: String,
    bookId: Int,
    chapter: Int,
    verses: List<Int>
): String = withContext(Dispatchers.IO) {
    val url = URL("https://bolls.life/get-verses/")
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", "SpiritualDisciplines/1.0")
        connection.doOutput = true
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000

        val request = JSONObject().apply {
            put("translation", translation)
            put("book", bookId)
            put("chapter", chapter)
            put("verses", JSONArray(verses))
        }
        val requestBody = JSONArray().put(request).toString().toByteArray(Charsets.UTF_8)
        connection.outputStream.use { it.write(requestBody) }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            error("Bolls returned HTTP $responseCode")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val groups = JSONArray(response)
        if (groups.length() == 0) error("Bolls returned no verse groups")
        val returnedVerses = groups.getJSONArray(0)
        if (returnedVerses.length() != verses.size) {
            error("Bolls returned ${returnedVerses.length()} of ${verses.size} requested verses")
        }
        val rawVerses = buildList {
            for (i in 0 until returnedVerses.length()) {
                add(returnedVerses.getJSONObject(i).getString("text"))
            }
        }

        buildString {
            rawVerses.forEachIndexed { index, rawVerse ->
                if (index > 0) {
                    append(if (rawVerses[index - 1].hasTrailingMemoryLineBreak()) '\n' else ' ')
                }
                append(cleanMemoryVerseHtml(rawVerse))
            }
        }.let(::normalizeMemoryVerseWhitespace)
    } finally {
        connection.disconnect()
    }
}

private fun cleanMemoryVerseHtml(rawHtml: String): String {
    val withoutHeading = rawHtml.removeLeadingMemoryHeading()
    val layoutAwareHtml = withoutHeading
        .replace(Regex("(?is)<sup\\b[^>]*>.*?</sup>"), "")
        .replace(Regex("(?is)<S\\b[^>]*>.*?</S>"), "")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p\\s*>"), "\n")
        .replace(Regex("(?i)<p\\b[^>]*>"), "")
        .replace(Regex("(?i)</div\\s*>"), "\n")
        .replace(Regex("(?i)<div\\b[^>]*>"), "")

    return HtmlCompat.fromHtml(layoutAwareHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        // Some source records omit a separator around layout tags, such as
        // "A Psalm of David.Yahweh". This applies only while fetching.
        .replace(Regex("(?<=[\\p{Ll}\\p{N}.,;:!?])(?=[\\p{Lu}“‘])"), "\n")
        .let(::normalizeMemoryVerseWhitespace)
}

private fun String.removeLeadingMemoryHeading(): String {
    val explicitHeading = Regex(
        "(?is)^\\s*<(b|strong)\\b[^>]*>.*?</\\1>\\s*<br\\s*/?>"
    )
    if (explicitHeading.containsMatchIn(this)) {
        return replaceFirst(explicitHeading, "")
    }

    val plainHeading = Regex("(?is)^\\s*([^<\\n]{1,90})<br\\s*/?>").find(this)
        ?: return this
    val headingText = HtmlCompat.fromHtml(
        plainHeading.groupValues[1],
        HtmlCompat.FROM_HTML_MODE_LEGACY
    ).toString().trim()

    return if (isLikelyMemoryHeading(headingText)) {
        removeRange(plainHeading.range)
    } else {
        this
    }
}

private fun isLikelyMemoryHeading(text: String): Boolean {
    if (text.isBlank() || text.length > 90) return false
    if (text.last() in listOf('.', ',', ';', ':', '?', '!', '”', '"')) return false

    val significantWords = Regex("[\\p{L}\\p{N}’']+")
        .findAll(text)
        .map { it.value }
        .filter { it.length > 3 }
        .toList()
    if (significantWords.isEmpty()) return false

    val titleLikeWords = significantWords.count { word ->
        word.firstOrNull()?.isUpperCase() == true ||
            word.all { it.isUpperCase() || !it.isLetter() }
    }
    return titleLikeWords.toFloat() / significantWords.size >= 0.75f
}

private fun String.hasTrailingMemoryLineBreak(): Boolean =
    Regex("(?is)<br\\s*/?>\\s*$").containsMatchIn(this)

private fun normalizeMemoryVerseWhitespace(text: String): String = text
    .replace("\r\n", "\n")
    .replace('\r', '\n')
    .replace(Regex("[ \\t]+"), " ")
    .replace(Regex(" *\\n *"), "\n")
    .replace(Regex("\\n{2,}"), "\n")
    .trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVerseDialog(translation: String, onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var stage by remember { mutableStateOf(PickerStage.BOOK) }
    var selectedBook by remember { mutableStateOf("") }
    var selectedChapter by remember { mutableIntStateOf(1) }
    var selectedVerses by remember { mutableStateOf(setOf<Int>()) }

    var fetchedReference by remember { mutableStateOf("") }
    var fetchedText by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    val books = BibleBooks.all

    fun formatVerses(verses: Set<Int>): String {
        if (verses.isEmpty()) return ""
        val sorted = verses.sorted()
        val parts = mutableListOf<String>()
        var start = sorted.first()
        var prev = start
        
        for (i in 1 until sorted.size) {
            val curr = sorted[i]
            if (curr == prev + 1) {
                prev = curr
            } else {
                if (start == prev) {
                    parts.add(start.toString())
                } else {
                    parts.add("$start-$prev")
                }
                start = curr
                prev = curr
            }
        }
        if (start == prev) {
            parts.add(start.toString())
        } else {
            parts.add("$start-$prev")
        }
        return parts.joinToString(",")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 700.dp)
        ) {
            Column {
                BiblePickerHeader(
                    title = when (stage) {
                        PickerStage.BOOK -> "Choose a book"
                        PickerStage.CHAPTER -> selectedBook
                        PickerStage.VERSE -> "$selectedBook $selectedChapter"
                        PickerStage.REVIEW -> "Review verse"
                    },
                    onBack = if (stage == PickerStage.BOOK) {
                        null
                    } else {
                        {
                            stage = when (stage) {
                                PickerStage.CHAPTER -> PickerStage.BOOK
                                PickerStage.VERSE -> PickerStage.CHAPTER
                                PickerStage.REVIEW -> PickerStage.VERSE
                                PickerStage.BOOK -> PickerStage.BOOK
                            }
                        }
                    },
                    onClose = onDismiss
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        when (stage) {
                            PickerStage.BOOK -> {
                                BibleBookGrid(
                                    books = books,
                                    onBookSelected = { book ->
                                        selectedBook = book.name
                                        stage = PickerStage.CHAPTER
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                )
                            }
                            PickerStage.CHAPTER -> {
                                val chapters = books.firstOrNull { it.name == selectedBook }?.chapters ?: 50
                                BibleNumberGrid(
                                    count = chapters,
                                    onNumberSelected = { chapter ->
                                        selectedChapter = chapter
                                        selectedVerses = emptySet()
                                        stage = PickerStage.VERSE
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                )
                            }
                            PickerStage.VERSE -> {
                                val bookId = BibleBooks.idForName(selectedBook)
                                val verseCount = bookId?.let {
                                    BibleVerseCounts.forChapter(it, selectedChapter, translation)
                                } ?: 0
                                Column {
                                    Text(
                                        "Select one or multiple verses",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BibleNumberGrid(
                                        count = verseCount,
                                        selectedNumbers = selectedVerses,
                                        onNumberSelected = { verse ->
                                            selectedVerses = if (verse in selectedVerses) {
                                                selectedVerses - verse
                                            } else {
                                                selectedVerses + verse
                                            }
                                        },
                                        modifier = Modifier.weight(1f, fill = false),
                                        contentPadding = PaddingValues(0.dp)
                                    )
                                }
                            }
                            PickerStage.REVIEW -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    BasicTextField(
                                        value = fetchedReference,
                                        onValueChange = { fetchedReference = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.titleLarge.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                    )
                                    BasicTextField(
                                        value = fetchedText,
                                        onValueChange = { fetchedText = it },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = LocalBibleFontFamily.current,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                                    )
                                    if (errorMessage != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    if (stage == PickerStage.VERSE || stage == PickerStage.REVIEW || isFetching) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            if (stage == PickerStage.VERSE) {
                                Button(
                                    shapes = ButtonDefaults.shapes(),
                                    onClick = {
                                        val ref = "$selectedBook $selectedChapter:${formatVerses(selectedVerses)}"
                                        errorMessage = null
                                        isFetching = true
                                        coroutineScope.launch {
                                            val requestedVerses = selectedVerses.sorted()
                                            try {
                                                val bookId = BibleBooks.idForName(selectedBook)
                                                    ?: error("Unknown Bible book: $selectedBook")
                                                fetchedText = fetchMemoryVerses(
                                                    translation = translation,
                                                    bookId = bookId,
                                                    chapter = selectedChapter,
                                                    verses = requestedVerses
                                                )
                                                fetchedReference = ref
                                                stage = PickerStage.REVIEW
                                            } catch (exception: Exception) {
                                                Log.e(
                                                    "AddVerseDialog",
                                                    "Failed to fetch $selectedBook $selectedChapter:${requestedVerses.joinToString()}",
                                                    exception
                                                )
                                                fetchedReference = ref
                                                fetchedText = ""
                                                errorMessage = "Could not fetch verse. Please check your internet connection."
                                                stage = PickerStage.REVIEW
                                            } finally {
                                                isFetching = false
                                            }
                                        }
                                    },
                                    enabled = !isFetching && selectedVerses.isNotEmpty()
                                ) {
                                    Text("Fetch")
                                }
                            } else if (stage == PickerStage.REVIEW) {
                                Button(shapes = ButtonDefaults.shapes(), onClick = {
                                    if (fetchedReference.isNotBlank() && fetchedText.isNotBlank()) {
                                        onAdd(
                                            fetchedReference,
                                            normalizeMemoryVerseWhitespace(fetchedText)
                                        )
                                    }
                                }) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
