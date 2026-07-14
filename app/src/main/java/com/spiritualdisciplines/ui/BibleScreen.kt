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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.data.DailyRecord
import com.spiritualdisciplines.data.ReadingPlanRepository
import com.spiritualdisciplines.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val readingDateFormatter = DateTimeFormatter.ofPattern("MMM d")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(viewModel: MainViewModel) {
    val haptics = rememberExpressiveHaptics()
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val readingPlanId by viewModel.readingPlanId.collectAsStateWithLifecycle()
    val startDateEpoch by viewModel.readingPlanStartDate.collectAsStateWithLifecycle()
    val preferredTranslation by viewModel.preferences.bibleTranslation.collectAsStateWithLifecycle(initialValue = "NIV")
    val allRecords by viewModel.allDailyRecords.collectAsStateWithLifecycle()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var readerInitialBook by remember { mutableStateOf<String?>(null) }
    var readerInitialChapter by remember { mutableStateOf<Int?>(null) }

    val currentPlan = remember(readingPlanId) { ReadingPlanRepository.getPlan(readingPlanId) }
    
    val today = remember { LocalDate.now() }
    val planStartDate = remember(startDateEpoch) { LocalDate.ofEpochDay(startDateEpoch) }
    val dayOfYear = remember(startDateEpoch, today) {
        val daysBetween = ChronoUnit.DAYS.between(planStartDate, today).toInt()
        val day = daysBetween + 1
        if (day > 0) day else 1 
    }

    var selectedPlanDay by remember(readingPlanId, startDateEpoch) { mutableIntStateOf(dayOfYear) }
    val selectedDate = remember(planStartDate, selectedPlanDay) {
        planStartDate.plusDays((selectedPlanDay - 1).toLong())
    }
    val selectedDateString = remember(selectedDate) { selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE) }
    val selectedReading = remember(currentPlan, selectedPlanDay) {
        currentPlan.getReadingForDay(selectedPlanDay)
    }
    val selectedRecord = if (selectedDate == today) {
        todayRecord
    } else {
        allRecords.firstOrNull { it.date == selectedDateString } ?: DailyRecord(selectedDateString)
    }

    // Parse the bibleProgress string (e.g. "0,2")
    val completedIndexes = remember(selectedRecord.bibleProgress) {
        selectedRecord.bibleProgress.split(",").mapNotNullTo(mutableSetOf()) { it.toIntOrNull() }
    }

    val allCompleted = selectedReading.passages.isNotEmpty() &&
        selectedReading.passages.indices.all { completedIndexes.contains(it) }

    val nextPlanDay = remember(allRecords, currentPlan, planStartDate, dayOfYear) {
        val recordsByDate = allRecords.associateBy { it.date }
        (1 until dayOfYear).firstOrNull { planDay ->
            val reading = currentPlan.getReadingForDay(planDay)
            val date = planStartDate.plusDays((planDay - 1).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            reading.passages.isNotEmpty() && recordsByDate[date]?.readBible != true
        } ?: dayOfYear
    }
    val isBehind = nextPlanDay < dayOfYear

    // Synchronize the selected day's master `readBible` boolean with its passages.
    LaunchedEffect(allCompleted, selectedRecord.readBible, selectedDateString) {
        if (allCompleted && !selectedRecord.readBible) {
            viewModel.updateDailyRecord(selectedDateString) { it.copy(readBible = true) }
        } else if (!allCompleted && selectedRecord.readBible) {
            viewModel.updateDailyRecord(selectedDateString) { it.copy(readBible = false) }
        }
    }

    val tabs = remember { listOf("Daily Plan", "Read Bible") }

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
            Column(modifier = Modifier.statusBarsPadding()) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = selectedTabIndex == index,
                            onClick = { haptics.selected { selectedTabIndex = index } }
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
                    Text(currentPlan.name, style = MaterialTheme.typography.titleLarge)
                    if (isBehind && selectedPlanDay == dayOfYear) {
                        TextButton(
                            onClick = {
                                haptics.selected { selectedPlanDay = nextPlanDay }
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Catch up from Day $nextPlanDay")
                        }
                    }
                }
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReadingDayNavigator(
                            planDay = selectedPlanDay,
                            date = selectedDate,
                            isToday = selectedPlanDay == dayOfYear,
                            canGoBack = selectedPlanDay > 1,
                            canGoForward = selectedPlanDay < dayOfYear,
                            onPrevious = { selectedPlanDay-- },
                            onNext = { selectedPlanDay++ },
                            onToday = { selectedPlanDay = dayOfYear }
                        )
                        
                        if (selectedReading.passages.isEmpty()) {
                            Text(
                                "No scheduled readings for this day.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        selectedReading.passages.forEachIndexed { index, passage ->
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
                                            if (isChecked) haptics.toggle(false) else haptics.confirm()
                                            viewModel.updateDailyRecord(selectedDateString) {
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
                                IconButton(onClick = { haptics.pressed { openReader(passage) } }) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read $passage", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
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

@Composable
private fun ReadingDayNavigator(
    planDay: Int,
    date: LocalDate,
    isToday: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { haptics.selected(onPrevious) },
            enabled = canGoBack
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous reading day")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Day $planDay",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (isToday) {
                Text("Today", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    date.format(readingDateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Back to today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { haptics.selected(onToday) }
                        .padding(top = 2.dp)
                )
            }
        }
        IconButton(
            onClick = { haptics.selected(onNext) },
            enabled = canGoForward
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next reading day")
        }
    }
}
