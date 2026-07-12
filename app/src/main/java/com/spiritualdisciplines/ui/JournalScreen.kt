package com.spiritualdisciplines.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.data.JournalEntry
import com.spiritualdisciplines.data.ReadingPlanRepository
import com.spiritualdisciplines.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val journalDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
private val archiveDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private data class ReflectionPrompt(val label: String, val question: String)

private val reflectionPrompts = listOf(
    ReflectionPrompt("Receive", "What did I read or notice?"),
    ReflectionPrompt("Respond", "What is mine to obey?"),
    ReflectionPrompt("Pray", "What do I want to bring to God?"),
    ReflectionPrompt("Give thanks", "Where did I see grace today?")
)

@Composable
fun JournalScreen(viewModel: MainViewModel) {
    val entries by viewModel.allJournalEntries.collectAsStateWithLifecycle()
    val readingPlanId by viewModel.readingPlanId.collectAsStateWithLifecycle()
    val readingPlanStartDate by viewModel.readingPlanStartDate.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var showingArchive by remember { mutableStateOf(false) }

    val plan = remember(readingPlanId) { ReadingPlanRepository.getPlan(readingPlanId) }
    val planDay = remember(selectedDate, readingPlanStartDate) {
        (ChronoUnit.DAYS.between(LocalDate.ofEpochDay(readingPlanStartDate), selectedDate).toInt() + 1)
            .coerceAtLeast(1)
    }
    val passages = remember(plan, planDay) { plan.getReadingForDay(planDay).passages }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            JournalTopBar(
                showingArchive = showingArchive,
                onShowArchive = { showingArchive = true },
                onBack = { showingArchive = false }
            )
        }
    ) { padding ->
        if (showingArchive) {
            JournalArchive(
                modifier = Modifier.padding(padding),
                entries = entries,
                onEntryClick = { entry ->
                    selectedDate = LocalDate.parse(entry.date)
                    showingArchive = false
                }
            )
        } else {
            JournalEditor(
                modifier = Modifier.padding(padding),
                selectedDate = selectedDate,
                today = today,
                passages = passages,
                storedEntry = entries.firstOrNull { it.date == selectedDate.toString() },
                onDateChange = { selectedDate = it },
                onSave = viewModel::saveJournalEntry
            )
        }
    }
}

@Composable
private fun JournalTopBar(
    showingArchive: Boolean,
    onShowArchive: () -> Unit,
    onBack: () -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showingArchive) {
            IconButton(onClick = { haptics.pressed(onBack) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to journal")
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = if (showingArchive) "Past entries" else "Journal",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (!showingArchive) {
            IconButton(onClick = { haptics.pressed(onShowArchive) }) {
                Icon(Icons.Default.AutoStories, contentDescription = "Past entries")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalEditor(
    modifier: Modifier,
    selectedDate: LocalDate,
    today: LocalDate,
    passages: List<String>,
    storedEntry: JournalEntry?,
    onDateChange: (LocalDate) -> Unit,
    onSave: (JournalEntry) -> Unit
) {
    val dateKey = selectedDate.toString()
    var fieldValue by remember(dateKey) {
        mutableStateOf(TextFieldValue(storedEntry?.content.orEmpty()))
    }
    var saveLabel by remember(dateKey) { mutableStateOf("Saved") }
    var showPrompts by remember { mutableStateOf(false) }

    LaunchedEffect(storedEntry) {
        if (storedEntry != null && fieldValue.text.isBlank()) {
            fieldValue = TextFieldValue(
                text = storedEntry.content,
                selection = TextRange(storedEntry.content.length)
            )
        }
    }

    LaunchedEffect(fieldValue.text) {
        if (fieldValue.text == storedEntry?.content.orEmpty()) {
            saveLabel = "Saved"
            return@LaunchedEffect
        }
        saveLabel = "Saving…"
        delay(500)
        onSave(JournalEntry(date = dateKey, content = fieldValue.text))
        saveLabel = "Saved"
    }

    fun insertHeading(heading: String) {
        val start = fieldValue.selection.min
        val end = fieldValue.selection.max
        val needsLeadingSpace = start > 0 && fieldValue.text[start - 1] != '\n'
        val insertion = buildString {
            if (needsLeadingSpace) append("\n\n")
            append(heading)
            append("\n")
        }
        val updatedText = fieldValue.text.replaceRange(start, end, insertion)
        fieldValue = TextFieldValue(
            text = updatedText,
            selection = TextRange(start + insertion.length)
        )
    }

    if (showPrompts) {
        PromptDialog(
            onDismiss = { showPrompts = false },
            onInsert = { prompt ->
                insertHeading(prompt.label)
                showPrompts = false
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp)
    ) {
        item {
            CompactDateNavigator(
                date = selectedDate,
                today = today,
                onPrevious = { onDateChange(selectedDate.minusDays(1)) },
                onNext = { onDateChange(selectedDate.plusDays(1)) },
                onToday = { onDateChange(today) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Insert",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(saveLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { showPrompts = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Reflection prompts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                passages.forEach { passage ->
                    InsertChip(label = passage, onClick = { insertHeading(passage) })
                }
                reflectionPrompts.forEach { prompt ->
                    InsertChip(label = prompt.label, onClick = { insertHeading(prompt.label) })
                }
            }
        }

        item {
            JournalPage(
                value = fieldValue,
                onValueChange = { fieldValue = it },
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun CompactDateNavigator(
    date: LocalDate,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = date.format(journalDateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (date != today) {
                Text(
                    text = "Back to today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { haptics.selected(onToday) }.padding(top = 2.dp)
                )
            }
        }
        IconButton(onClick = { haptics.selected(onPrevious) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
        }
        IconButton(onClick = { haptics.selected(onNext) }, enabled = date < today) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun InsertChip(label: String, onClick: () -> Unit) {
    val haptics = rememberExpressiveHaptics()
    SuggestionChip(
        onClick = { haptics.selected(onClick) },
        label = { Text(label, maxLines = 1) },
        shape = RoundedCornerShape(9.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    )
}

@Composable
private fun JournalPage(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().heightIn(min = 460.dp).padding(18.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.text.isEmpty()) {
                        Text(
                            "Start writing…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PromptDialog(onDismiss: () -> Unit, onInsert: (ReflectionPrompt) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
        title = { Text("Reflection prompts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Use whichever prompt helps. Tap one to add its heading to your entry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reflectionPrompts.forEach { prompt ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onInsert(prompt) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(prompt.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(prompt.question, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun JournalArchive(
    modifier: Modifier,
    entries: List<JournalEntry>,
    onEntryClick: (JournalEntry) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val savedEntries = remember(entries, query) {
        entries
            .filter { it.content.isNotBlank() }
            .filter { query.isBlank() || it.content.contains(query, ignoreCase = true) }
            .sortedByDescending { it.date }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search entries") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (savedEntries.isEmpty()) {
            item { EmptyArchive(isSearching = query.isNotBlank()) }
        } else {
            items(savedEntries, key = { it.date }) { entry ->
                ArchiveEntry(entry = entry, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun EmptyArchive(isSearching: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 72.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                if (isSearching) Icons.Default.Search else Icons.Default.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(27.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (isSearching) "No entries found" else "Your past entries will appear here",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ArchiveEntry(entry: JournalEntry, onClick: () -> Unit) {
    val date = remember(entry.date) { LocalDate.parse(entry.date) }
    val haptics = rememberExpressiveHaptics()
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable { haptics.selected(onClick) }
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Today,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    date.format(archiveDateFormatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.content.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open entry", modifier = Modifier.size(20.dp))
        }
    }
}
