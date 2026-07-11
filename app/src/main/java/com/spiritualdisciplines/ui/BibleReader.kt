package com.spiritualdisciplines.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.spiritualdisciplines.data.BibleBooks
import com.spiritualdisciplines.data.CachedChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class BibleReaderState {
    BOOKS, CHAPTERS, READER
}

private enum class BibleDisplayMode {
    PARAGRAPH, VERSES
}

private enum class BibleFontSize(val label: String, val sp: Int) {
    SMALL("Small", 16),
    MEDIUM("Medium", 18),
    LARGE("Large", 21)
}

private data class BibleVerse(
    val number: Int,
    val text: String,
    val startsParagraph: Boolean,
    val heading: String? = null
)

private data class BibleParagraph(
    val verses: List<BibleVerse>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReader(
    translation: String,
    initialBook: String? = null,
    initialChapter: Int? = null,
    onClose: (() -> Unit)? = null, // If provided, shows a close button
    getCachedChapter: suspend (String) -> CachedChapter? = { null },
    insertCachedChapter: suspend (CachedChapter) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val books = BibleBooks.all
    val paragraphBreakIndex = remember(context.applicationContext) {
        ParagraphBreakIndex.load(context.applicationContext)
    }

    var currentState by remember { mutableStateOf(BibleReaderState.BOOKS) }
    var selectedBookIndex by remember { mutableIntStateOf(0) }
    var selectedChapter by remember { mutableIntStateOf(1) }
    var displayMode by remember { mutableStateOf(BibleDisplayMode.PARAGRAPH) }
    var fontSize by remember { mutableStateOf(BibleFontSize.MEDIUM) }
    var returnToBookIndex by remember { mutableStateOf<Int?>(null) }
    var returnToChapter by remember { mutableStateOf<Int?>(null) }

    fun openChapterPicker() {
        returnToBookIndex = selectedBookIndex
        returnToChapter = selectedChapter
        currentState = BibleReaderState.CHAPTERS
    }

    fun dismissChapterPicker() {
        val bookIndex = returnToBookIndex ?: return
        val chapter = returnToChapter ?: return
        selectedBookIndex = bookIndex
        selectedChapter = chapter
        returnToBookIndex = null
        returnToChapter = null
        currentState = BibleReaderState.READER
    }

    LaunchedEffect(initialBook, initialChapter) {
        if (initialBook != null) {
            val idx = BibleBooks.indexOf(initialBook)
            if (idx != -1) {
                selectedBookIndex = idx
                if (initialChapter != null) {
                    selectedChapter = initialChapter
                    currentState = BibleReaderState.READER
                } else {
                    currentState = BibleReaderState.CHAPTERS
                }
            }
        }
    }

    var chapterContent by remember { mutableStateOf<List<BibleVerse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var displayedChapterKey by remember { mutableStateOf<String?>(null) }

    fun fetchChapter() {
        val bookId = books[selectedBookIndex].id
        val chapter = selectedChapter
        val cacheId = "$translation-$bookId-$chapter"

        isLoading = true
        error = null
        chapterContent = emptyList()
        displayedChapterKey = cacheId
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val cachedChapter = getCachedChapter(cacheId)

                    if (cachedChapter != null) {
                        val jsonArray = JSONArray(cachedChapter.versesJson)
                        val paragraphBreaks = fetchParagraphBreakVerseNumbers(
                            translation = translation,
                            bookId = bookId,
                            chapter = chapter,
                            currentChapterJson = jsonArray,
                            paragraphBreakIndex = paragraphBreakIndex
                        )
                        val verses = parseChapterVerses(jsonArray, paragraphBreaks)
                        withContext(Dispatchers.Main) {
                            chapterContent = verses
                            isLoading = false
                        }
                    } else {
                        val urlString = "https://bolls.life/get-chapter/$translation/$bookId/$selectedChapter/"
                        val url = URL(urlString)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000

                        if (connection.responseCode == 200) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val jsonArray = JSONArray(response)
                            val paragraphBreaks = fetchParagraphBreakVerseNumbers(
                                translation = translation,
                                bookId = bookId,
                                chapter = chapter,
                                currentChapterJson = jsonArray,
                                paragraphBreakIndex = paragraphBreakIndex
                            )
                            val verses = parseChapterVerses(jsonArray, paragraphBreaks)

                            // Save to cache
                            val newCachedChapter = CachedChapter(
                                id = cacheId,
                                translation = translation,
                                bookId = bookId,
                                chapter = chapter,
                                versesJson = response
                            )
                            insertCachedChapter(newCachedChapter)

                            withContext(Dispatchers.Main) {
                                chapterContent = verses
                                isLoading = false
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                error = "Failed to load chapter. ($translation might not be supported)"
                                isLoading = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        error = "Could not load chapter. Please check your internet connection."
                        isLoading = false
                    }
                }
            }
        }
    }

    LaunchedEffect(translation, currentState, selectedBookIndex, selectedChapter) {
        val selectedChapterKey = "$translation-${books[selectedBookIndex].id}-$selectedChapter"
        if (currentState == BibleReaderState.READER && displayedChapterKey != selectedChapterKey) {
            fetchChapter()
        }
    }

    val canDismissChapterPicker =
        currentState != BibleReaderState.READER &&
            returnToBookIndex != null &&
            returnToChapter != null

    BackHandler(enabled = currentState == BibleReaderState.CHAPTERS || canDismissChapterPicker) {
        if (canDismissChapterPicker) {
            dismissChapterPicker()
        } else {
            currentState = BibleReaderState.BOOKS
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                when (currentState) {
                    BibleReaderState.BOOKS -> Text("Bible Reader")
                    BibleReaderState.CHAPTERS -> Text(books[selectedBookIndex].name)
                    BibleReaderState.READER -> {
                        TextButton(onClick = { openChapterPicker() }) {
                            Text(
                                "${books[selectedBookIndex].name} $selectedChapter",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change Chapter", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            },
            navigationIcon = {
                if (currentState != BibleReaderState.BOOKS) {
                    IconButton(onClick = {
                        when (currentState) {
                            BibleReaderState.CHAPTERS -> currentState = BibleReaderState.BOOKS
                            BibleReaderState.READER -> openChapterPicker()
                            BibleReaderState.BOOKS -> Unit
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = when (currentState) {
                                BibleReaderState.CHAPTERS -> "Choose book"
                                BibleReaderState.READER -> "Choose chapter"
                                BibleReaderState.BOOKS -> null
                            }
                        )
                    }
                }
            },
            actions = {
                if (canDismissChapterPicker) {
                    IconButton(onClick = { dismissChapterPicker() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Return to ${books[returnToBookIndex!!].name} $returnToChapter"
                        )
                    }
                }
                if (currentState == BibleReaderState.READER) {
                    IconButton(
                        onClick = {
                            displayMode = when (displayMode) {
                                BibleDisplayMode.PARAGRAPH -> BibleDisplayMode.VERSES
                                BibleDisplayMode.VERSES -> BibleDisplayMode.PARAGRAPH
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (displayMode == BibleDisplayMode.PARAGRAPH) {
                                Icons.AutoMirrored.Filled.Subject
                            } else {
                                Icons.AutoMirrored.Filled.FormatListBulleted
                            },
                            contentDescription = if (displayMode == BibleDisplayMode.PARAGRAPH) {
                                "Paragraph layout"
                            } else {
                                "Verse-by-verse layout"
                            },
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    val fontSizes = BibleFontSize.entries
                    val fontSizeIndex = fontSizes.indexOf(fontSize)
                    IconButton(
                        enabled = fontSizeIndex > 0,
                        onClick = { fontSize = fontSizes[fontSizeIndex - 1] }
                    ) {
                        Text(
                            text = "Aa-",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (fontSizeIndex > 0) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
                            }
                        )
                    }
                    IconButton(
                        enabled = fontSizeIndex < fontSizes.lastIndex,
                        onClick = { fontSize = fontSizes[fontSizeIndex + 1] }
                    ) {
                        Text(
                            text = "Aa+",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (fontSizeIndex < fontSizes.lastIndex) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Reader")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        )
        
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (currentState) {
                BibleReaderState.BOOKS -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(books.size) { index ->
                            val book = books[index]
                            ListItem(
                                headlineContent = { Text(book.name) },
                                trailingContent = { Text("${book.chapters} Ch") },
                                modifier = Modifier.clickable {
                                    selectedBookIndex = index
                                    currentState = BibleReaderState.CHAPTERS
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                BibleReaderState.CHAPTERS -> {
                    val chapterCount = books[selectedBookIndex].chapters
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(48.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chapterCount) { i ->
                            val chap = i + 1
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        selectedChapter = chap
                                        returnToBookIndex = null
                                        returnToChapter = null
                                        currentState = BibleReaderState.READER
                                    }
                            ) {
                                Text(
                                    text = chap.toString(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                BibleReaderState.READER -> {
                    when {
                        isLoading -> {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator()
                            }
                        }
                        error != null -> {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { fetchChapter() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        else -> {
                            val scrollState = rememberScrollState()
                            val verseTextStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSize.sp.sp,
                                lineHeight = (fontSize.sp + 8).sp
                            )
                            val verseNumberColor = MaterialTheme.colorScheme.primary
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(16.dp)
                            ) {
                                if (displayMode == BibleDisplayMode.PARAGRAPH) {
                                    buildParagraphs(chapterContent).forEach { paragraph ->
                                        paragraph.verses.firstOrNull()?.heading?.let { heading ->
                                            Text(
                                                text = heading,
                                                style = verseTextStyle.copy(
                                                    fontSize = (fontSize.sp - 1).sp
                                                ),
                                                color = verseNumberColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = buildParagraphText(
                                                verses = paragraph.verses,
                                                fontSizeSp = fontSize.sp,
                                                verseNumberColor = verseNumberColor
                                            ),
                                            style = verseTextStyle.copy(
                                                textIndent = TextIndent(
                                                    firstLine = 18.sp
                                                )
                                            ),
                                            modifier = Modifier.padding(bottom = 18.dp)
                                        )
                                    }
                                } else {
                                    chapterContent.forEach { verse ->
                                        verse.heading?.let { heading ->
                                            Text(
                                                text = heading,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = verseNumberColor,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                                            )
                                        }
                                        Text(
                                            text = buildVerseText(
                                                verse = verse,
                                                fontSizeSp = fontSize.sp,
                                                verseNumberColor = verseNumberColor
                                            ),
                                            style = verseTextStyle,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = if (verse.startsParagraph) 8.dp else 0.dp,
                                                    bottom = 12.dp
                                                )
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val hasPrev = selectedChapter > 1 || selectedBookIndex > 0
                                    val hasNext = selectedChapter < books[selectedBookIndex].chapters || selectedBookIndex < books.size - 1
                                    
                                    Button(
                                        onClick = {
                                            when {
                                                selectedChapter > 1 -> selectedChapter -= 1
                                                selectedBookIndex > 0 -> {
                                                    selectedBookIndex -= 1
                                                    selectedChapter = books[selectedBookIndex].chapters
                                                }
                                            }
                                        },
                                        enabled = hasPrev
                                    ) {
                                        Text("Previous")
                                    }
                                    
                                    Button(
                                        onClick = {
                                            when {
                                                selectedChapter < books[selectedBookIndex].chapters -> selectedChapter += 1
                                                selectedBookIndex < books.size - 1 -> {
                                                    selectedBookIndex += 1
                                                    selectedChapter = 1
                                                }
                                            }
                                        },
                                        enabled = hasNext
                                    ) {
                                        Text("Next")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun fetchParagraphBreakVerseNumbers(
    translation: String,
    bookId: Int,
    chapter: Int,
    currentChapterJson: JSONArray,
    paragraphBreakIndex: ParagraphBreakIndex
): Set<Int> =
    extractParagraphBreakVerseNumbers(currentChapterJson) +
        paragraphBreakIndex.breaksFor(translation, bookId, chapter)

private class ParagraphBreakIndex private constructor(
    private val metadata: JSONObject
) {
    fun breaksFor(translation: String, bookId: Int, chapter: Int): Set<Int> {
        val sourceTranslation = BUNDLED_PARAGRAPH_VERSIONS[translation] ?: return emptySet()
        val chapterBreaks = metadata
            .optJSONObject(sourceTranslation)
            ?.optJSONObject(bookId.toString())
            ?.optJSONArray(chapter.toString())
            ?: return emptySet()

        return (0 until chapterBreaks.length())
            .mapTo(mutableSetOf()) { chapterBreaks.getInt(it) }
    }

    companion object {
        fun load(context: Context): ParagraphBreakIndex {
            val metadata = runCatching {
                context.assets.open(PARAGRAPH_BREAK_ASSET).bufferedReader().use { reader ->
                    JSONObject(reader.readText())
                }
            }.getOrElse { JSONObject() }

            return ParagraphBreakIndex(metadata)
        }
    }
}

private fun extractParagraphBreakVerseNumbers(jsonArray: JSONArray): Set<Int> {
    val breakVerses = mutableSetOf<Int>()

    for (i in 1 until jsonArray.length()) {
        val verseObj = jsonArray.getJSONObject(i)
        val rawText = verseObj.getString("text")
        if (hasParagraphBreak(rawText)) {
            breakVerses.add(verseObj.getInt("verse"))
        }
    }

    return breakVerses
}

private fun parseChapterVerses(
    jsonArray: JSONArray,
    paragraphBreakVerseNumbers: Set<Int> = emptySet()
): List<BibleVerse> {
    val verses = mutableListOf<BibleVerse>()

    for (i in 0 until jsonArray.length()) {
        val verseObj = jsonArray.getJSONObject(i)
        val rawText = verseObj.getString("text")
        val heading = extractLeadingHeading(rawText)
        val cleanedText = cleanBibleHtml(rawText.removeLeadingHeading())
        verses.add(
            BibleVerse(
                number = verseObj.getInt("verse"),
                text = cleanedText,
                startsParagraph = i > 0 &&
                    (hasParagraphBreak(rawText) || paragraphBreakVerseNumbers.contains(verseObj.getInt("verse"))),
                heading = heading
            )
        )
    }

    return verses
}

private fun cleanBibleHtml(rawHtml: String): String {
    val layoutAwareHtml = rawHtml
        .replace(Regex("(?is)<sup\\b[^>]*>.*?</sup>"), "")
        .replace(Regex("(?is)<S\\b[^>]*>.*?</S>"), "")
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p\\s*>"), "\n\n")
        .replace(Regex("(?i)<p\\b[^>]*>"), "")
        .replace(Regex("(?i)</div\\s*>"), "\n\n")
        .replace(Regex("(?i)<div\\b[^>]*>"), "")

    return HtmlCompat.fromHtml(layoutAwareHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(Regex("(^|[;.!?]) {2,}(?=\\S)"), "$1\n")
        .replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex(" *\n *"), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}

private fun hasParagraphBreak(rawHtml: String): Boolean {
    val trimmed = rawHtml.trimStart()

    return trimmed.contains(Regex("(?i)^<(b|strong)\\b")) ||
        extractLeadingHeading(rawHtml) != null ||
        trimmed.contains(Regex("(?i)^<p\\b")) ||
        trimmed.contains(Regex("(?i)</p\\s*>\\s*<p\\b")) ||
        trimmed.contains(Regex("(?i)<br\\s*/?>\\s*<br\\s*/?>"))
}

private fun extractLeadingHeading(rawHtml: String): String? {
    val explicitMatch = Regex("(?is)^\\s*<(b|strong)\\b[^>]*>(.*?)</\\1>\\s*<br\\s*/?>")
        .find(rawHtml)
    if (explicitMatch != null) {
        return htmlToPlainText(explicitMatch.groupValues[2])
            .takeIf { it.isNotBlank() }
    }

    val plainMatch = Regex("(?is)^\\s*([^<\\n]{1,90})<br\\s*/?>").find(rawHtml)
        ?: return null
    val headingText = htmlToPlainText(plainMatch.groupValues[1])

    return headingText.takeIf { isPlainHeading(it) }
}

private fun String.removeLeadingHeading(): String =
    when {
        Regex("(?is)^\\s*<(b|strong)\\b[^>]*>.*?</\\1>\\s*<br\\s*/?>").containsMatchIn(this) ->
            replaceFirst(Regex("(?is)^\\s*<(b|strong)\\b[^>]*>.*?</\\1>\\s*<br\\s*/?>"), "")
        extractLeadingHeading(this) != null ->
            replaceFirst(Regex("(?is)^\\s*[^<\\n]{1,90}<br\\s*/?>"), "")
        else -> this
    }

private fun htmlToPlainText(rawHtml: String): String =
    HtmlCompat.fromHtml(rawHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(Regex("\\s+"), " ")
        .trim()

private fun isPlainHeading(text: String): Boolean {
    if (text.isBlank() || text.length > 90) return false
    if (text.last() in listOf('.', ',', ';', ':', '?', '!', '”', '"')) return false
    if (Regex("^Psalm\\s+\\d+", RegexOption.IGNORE_CASE).matches(text)) return true

    val significantWords = Regex("[\\p{L}\\p{N}’']+")
        .findAll(text)
        .map { it.value }
        .filter { it.length > 3 }
        .toList()

    if (significantWords.isEmpty()) return false

    val titleLikeWords = significantWords.count { word ->
        word.firstOrNull()?.isUpperCase() == true || word.all { it.isUpperCase() || !it.isLetter() }
    }

    return titleLikeWords.toFloat() / significantWords.size >= 0.75f
}

private fun buildParagraphs(verses: List<BibleVerse>): List<BibleParagraph> {
    val paragraphs = mutableListOf<MutableList<BibleVerse>>()

    verses.forEach { verse ->
        if (paragraphs.isEmpty() || (verse.startsParagraph && paragraphs.last().isNotEmpty())) {
            paragraphs.add(mutableListOf())
        }
        paragraphs.last().add(verse)
    }

    return paragraphs.map { paragraph ->
        BibleParagraph(verses = paragraph)
    }
}

private fun buildParagraphText(
    verses: List<BibleVerse>,
    fontSizeSp: Int,
    verseNumberColor: Color
) = buildAnnotatedString {
    verses.forEachIndexed { index, verse ->
        if (index > 0) append(" ")
        appendVerseNumberAndText(verse, fontSizeSp, verseNumberColor)
    }
}

private fun buildVerseText(
    verse: BibleVerse,
    fontSizeSp: Int,
    verseNumberColor: Color
) = buildAnnotatedString {
    appendVerseNumberAndText(verse, fontSizeSp, verseNumberColor)
}

/** Keeps a verse marker with the first word that follows it when the text wraps. */
private fun AnnotatedString.Builder.appendVerseNumberAndText(
    verse: BibleVerse,
    fontSizeSp: Int,
    verseNumberColor: Color
) {
    withStyle(
        SpanStyle(
            color = verseNumberColor,
            fontSize = (fontSizeSp - 5).sp,
            baselineShift = BaselineShift.Superscript
        )
    ) {
        append(verse.number.toString())
    }

    // A non-breaking space makes the marker and first word one line-breaking unit.
    append('\u00A0')
    append(verse.text.trimStart())
}

private const val PARAGRAPH_BREAK_ASSET = "bible_paragraph_breaks.json"

private val BUNDLED_PARAGRAPH_VERSIONS = mapOf(
    "ESV" to "ESV",
    "NIV" to "NIV",
    // The available KJV layout is verse-by-verse, so use ESV's editorial paragraphs.
    "KJV" to "ESV",
    "NKJV" to "NKJV",
    "NLT" to "NLT",
    "NASB" to "NASB",
    "LSB" to "LSB"
)
