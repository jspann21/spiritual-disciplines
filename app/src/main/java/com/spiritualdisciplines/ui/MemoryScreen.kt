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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.data.BibleBooks
import com.spiritualdisciplines.data.BibleVerseCounts
import com.spiritualdisciplines.data.MemoryVerse
import com.spiritualdisciplines.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(viewModel: MainViewModel) {
    val verses by viewModel.memoryVerses.collectAsStateWithLifecycle()
    val translation by viewModel.bibleTranslation.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Library", "Practice")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { 
            Column {
                TopAppBar(title = { Text("Scripture Memory") })
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Verse")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(verses) { verse ->
                        var isHidden by remember { mutableStateOf(true) }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(verse.reference, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (isHidden) {
                                    Button(onClick = { isHidden = false }) {
                                        Text("Reveal Verse")
                                    }
                                } else {
                                    Text(
                                        verse.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = LocalBibleFontFamily.current)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { isHidden = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                        Text("Hide")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                val isReviewedToday = verse.lastReviewedDate == viewModel.todayDateString
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isReviewedToday, onCheckedChange = { if(it) viewModel.markVerseReviewed(verse.id) })
                                    Text("Reviewed Today")
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.deleteMemoryVerse(verse.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                PracticeView(viewModel, verses)
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
    val wordsList = text.split(" ")
    val hideCount = (wordsList.size * hidePercentage).toInt().coerceAtLeast(1)
    val hideIndices = wordsList.indices.shuffled().take(hideCount).toSet()

    return wordsList.mapIndexed { index, word ->
        if (hideIndices.contains(index)) hideWord(word) else word
    }.joinToString(" ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeView(viewModel: MainViewModel, verses: List<MemoryVerse>) {
    val today = viewModel.todayDateString
    val dueVerses = verses.filter { 
        it.nextReviewDate == null || it.nextReviewDate <= today 
    }
    
    var completedVerseIds by remember { mutableStateOf(setOf<Int>()) }
    val remainingVerses = dueVerses.filter { it.id !in completedVerseIds }
    
    if (remainingVerses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val text = if (dueVerses.isEmpty()) "You're all caught up for today!" else "Review complete!"
            Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    
    val currentVerse = remainingVerses.first()
    var mode by remember { mutableStateOf("Read") }
    val modes = listOf("Read", "First-Letter", "Blanks")
    
    val displayVerseText = remember(currentVerse.id, mode) {
        when (mode) {
            "First-Letter" -> toFirstLetter(currentVerse.text)
            "Blanks" -> toFillInTheBlanks(currentVerse.text)
            else -> currentVerse.text
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val total = dueVerses.size
        val current = total - remainingVerses.size + 1
        Text("Verse $current of $total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentVerse.reference, 
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = displayVerseText, 
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = LocalBibleFontFamily.current,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("How well did you remember?", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { 
                    viewModel.updateVerseReview(currentVerse.id, 1) 
                    completedVerseIds = completedVerseIds + currentVerse.id
                    mode = "Read"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Hard")
            }
            Button(
                onClick = { 
                    viewModel.updateVerseReview(currentVerse.id, 3) 
                    completedVerseIds = completedVerseIds + currentVerse.id
                    mode = "Read"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Good")
            }
            Button(
                onClick = { 
                    viewModel.updateVerseReview(currentVerse.id, 5) 
                    completedVerseIds = completedVerseIds + currentVerse.id
                    mode = "Read"
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Easy")
            }
        }
    }
}

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
        buildList {
            for (i in 0 until returnedVerses.length()) {
                add(
                    returnedVerses.getJSONObject(i).getString("text")
                        .replace(Regex("<.*?>"), "")
                        .trim()
                )
            }
        }.joinToString(" ")
    } finally {
        connection.disconnect()
    }
}

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
                                Button(onClick = {
                                    if (fetchedReference.isNotBlank() && fetchedText.isNotBlank()) {
                                        onAdd(fetchedReference, fetchedText)
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
