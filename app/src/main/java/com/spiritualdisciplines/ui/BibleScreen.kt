package com.spiritualdisciplines.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.data.ReadingPlanRepository
import com.spiritualdisciplines.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(viewModel: MainViewModel) {
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val todayJournal by viewModel.todayJournal.collectAsStateWithLifecycle()
    val readingPlanId by viewModel.readingPlanId.collectAsStateWithLifecycle()
    val startDateEpoch by viewModel.readingPlanStartDate.collectAsStateWithLifecycle()
    val preferredTranslation by viewModel.preferences.bibleTranslation.collectAsStateWithLifecycle(initialValue = "NIV")
    val allRecords by viewModel.allDailyRecords.collectAsStateWithLifecycle()
    
    var showHistory by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var readerInitialBook by remember { mutableStateOf<String?>(null) }
    var readerInitialChapter by remember { mutableStateOf<Int?>(null) }

    val currentPlan = remember(readingPlanId) { ReadingPlanRepository.getPlan(readingPlanId) }
    
    val dayOfYear = remember(startDateEpoch) { 
        val startDate = LocalDate.ofEpochDay(startDateEpoch)
        val today = LocalDate.now()
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, today).toInt()
        val day = daysBetween + 1
        if (day > 0) day else 1 
    }
    
    val todaysReading = remember(currentPlan, dayOfYear) { currentPlan.getReadingForDay(dayOfYear) }
    
    // Parse the bibleProgress string (e.g. "0,2")
    val completedIndexes = todayRecord.bibleProgress.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }.toSet()
    
    val allCompleted = todaysReading.passages.isNotEmpty() && todaysReading.passages.indices.all { completedIndexes.contains(it) }

    val isBehind = remember(allRecords, startDateEpoch, dayOfYear) {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val pastRecords = allRecords.filter { it.date < todayStr && (it.readBible || it.bibleProgress.isNotEmpty()) }
        val lastRecord = pastRecords.maxByOrNull { it.date }
        
        val expectedNextDay = if (lastRecord != null) {
            val lastDate = LocalDate.parse(lastRecord.date, DateTimeFormatter.ISO_LOCAL_DATE)
            val expectedPlanDayOnLastDate = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.ofEpochDay(startDateEpoch), lastDate).toInt() + 1
            if (lastRecord.readBible) expectedPlanDayOnLastDate + 1 else expectedPlanDayOnLastDate
        } else {
            1
        }
        
        expectedNextDay < dayOfYear
    }

    // Synchronize the master `readBible` boolean if all passages are checked off
    LaunchedEffect(allCompleted, todayRecord.readBible) {
        if (allCompleted && !todayRecord.readBible) {
            viewModel.updateDailyRecord { it.copy(readBible = true) }
        } else if (!allCompleted && todayRecord.readBible) {
            viewModel.updateDailyRecord { it.copy(readBible = false) }
        }
    }

    val tabs = listOf("Daily Plan", "Read Bible")

    fun openReader(passage: String) {
        val parts = passage.split(" ")
        if (parts.size >= 2) {
            val book = parts.dropLast(1).joinToString(" ")
            val chapterPart = parts.last().split("-").first().split(":").first()
            val chapter = chapterPart.toIntOrNull()
            
            readerInitialBook = book
            readerInitialChapter = chapter
            selectedTabIndex = 1
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { 
            Column {
                if (selectedTabIndex == 0) {
                    TopAppBar(
                        title = { Text("Bible Reading") },
                        // MainScreen already applies the status-bar inset to each destination.
                        // Avoid applying it again inside this nested Scaffold.
                        windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                        actions = {
                            IconButton(onClick = { showHistory = true }) {
                                Icon(Icons.Default.History, contentDescription = "Reading History")
                            }
                        }
                    )
                }
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (selectedTabIndex == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Today's Reading: ${currentPlan.name}", style = MaterialTheme.typography.titleLarge)
                    if (isBehind) {
                        TextButton(
                            onClick = {
                                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val pastRecords = allRecords.filter { it.date < todayStr && (it.readBible || it.bibleProgress.isNotEmpty()) }
                                val lastRecord = pastRecords.maxByOrNull { it.date }
                                
                                val nextPlanDay = if (lastRecord != null) {
                                    val lastDate = LocalDate.parse(lastRecord.date, DateTimeFormatter.ISO_LOCAL_DATE)
                                    val expectedPlanDayOnLastDate = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.ofEpochDay(startDateEpoch), lastDate).toInt() + 1
                                    if (lastRecord.readBible) expectedPlanDayOnLastDate + 1 else expectedPlanDayOnLastDate
                                } else {
                                    1
                                }
                                
                                val today = LocalDate.now()
                                val newStartDate = today.minusDays((nextPlanDay - 1).toLong())
                                viewModel.preferences.setReadingPlanStartDate(newStartDate.toEpochDay())
                                viewModel.updateDailyRecord { it.copy(bibleProgress = "", readBible = false) }
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Fell behind? Catch me up")
                        }
                    }
                }
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Day ${todaysReading.dayOfYear}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        
                        if (todaysReading.passages.isEmpty()) {
                            Text(
                                "No scheduled readings today.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        todaysReading.passages.forEachIndexed { index, passage ->
                            val isChecked = completedIndexes.contains(index)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val newIndexes = if (isChecked) {
                                                completedIndexes - index
                                            } else {
                                                completedIndexes + index
                                            }
                                            viewModel.updateDailyRecord { 
                                                it.copy(bibleProgress = newIndexes.joinToString(",")) 
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        passage,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { openReader(passage) }) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read $passage", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Text("Notes", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = todayJournal.whatDidIRead,
                    onValueChange = { newValue -> 
                        viewModel.updateJournalEntry { it.copy(whatDidIRead = newValue) }
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text("What did you learn today?") }
                )
            }
            
            if (showHistory) {
                ReadingHistoryModal(viewModel = viewModel, onDismiss = { showHistory = false })
            }
        } else {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                BibleReader(
                    translation = preferredTranslation,
                    initialBook = readerInitialBook,
                    initialChapter = readerInitialChapter,
                    onClose = null,
                    getCachedChapter = { id -> viewModel.getCachedChapter(id) },
                    insertCachedChapter = { chapter -> viewModel.insertCachedChapter(chapter) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHistoryModal(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val allJournalEntries by viewModel.allJournalEntries.collectAsStateWithLifecycle()
    
    // Filter to only entries where there's some reading note
    val readingHistory = allJournalEntries.filter { it.whatDidIRead.isNotBlank() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Reading History Notes", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (readingHistory.isEmpty()) {
                Text("No reading notes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(readingHistory) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val parsedDate = try {
                                    LocalDate.parse(entry.date).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                                } catch (e: Exception) {
                                    entry.date
                                }
                                Text(parsedDate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(entry.whatDidIRead, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}
