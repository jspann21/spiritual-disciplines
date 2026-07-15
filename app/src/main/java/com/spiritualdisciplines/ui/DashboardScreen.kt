package com.spiritualdisciplines.ui

import com.spiritualdisciplines.ui.theme.LocalBibleFontFamily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.data.CachedVerse
import com.spiritualdisciplines.network.BollsBibleApi
import com.spiritualdisciplines.network.BollsVerseRequest
import com.spiritualdisciplines.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, onSettingsClick: () -> Unit) {
    val record by viewModel.todayRecord.collectAsStateWithLifecycle()
    val allRecords by viewModel.allDailyRecords.collectAsStateWithLifecycle()
    val showStreak by viewModel.showStreak.collectAsStateWithLifecycle()
    val bibleTranslation by viewModel.bibleTranslation.collectAsStateWithLifecycle()

    val showReadBible by viewModel.showReadBible.collectAsStateWithLifecycle()
    val showPray by viewModel.showPray.collectAsStateWithLifecycle()
    val showReviewVerse by viewModel.showReviewVerse.collectAsStateWithLifecycle()
    val showJournal by viewModel.showJournal.collectAsStateWithLifecycle()
    val showGiveThanks by viewModel.showGiveThanks.collectAsStateWithLifecycle()
    val showPrayForOthers by viewModel.showPrayForOthers.collectAsStateWithLifecycle()
    val showObeyApply by viewModel.showObeyApply.collectAsStateWithLifecycle()

    var votdText by remember { mutableStateOf<String?>(null) }
    var votdError by remember { mutableStateOf<String?>(null) }
    
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val dailyVerse = dailyVerses[(dayOfYear - 1) % dailyVerses.size]

    LaunchedEffect(bibleTranslation, dailyVerse) {
        votdText = null
        votdError = null
        val result = withContext(Dispatchers.IO) {
            val cacheId = "$bibleTranslation-${dailyVerse.book}-${dailyVerse.chapter}-${dailyVerse.verse}"
            val cachedVerse = viewModel.getCachedVerse(cacheId)
            
            if (cachedVerse != null) {
                return@withContext cachedVerse.text to null
            }

            try {
                val verseItems = BollsBibleApi.fetchVerseGroups(
                    listOf(
                        BollsVerseRequest(
                            translation = bibleTranslation,
                            bookId = dailyVerse.book,
                            chapter = dailyVerse.chapter,
                            verses = listOf(dailyVerse.verse)
                        )
                    )
                ).firstOrNull().orEmpty()

                if (verseItems.isEmpty()) {
                    null to "Translation might not be supported"
                } else {
                    val fetchedText = verseItems.joinToString(" ") { rawText ->
                        rawText.replace(Regex("<.*?>"), "").trim()
                    }
                    viewModel.insertCachedVerse(
                        CachedVerse(
                            id = cacheId,
                            translation = bibleTranslation,
                            bookId = dailyVerse.book,
                            chapter = dailyVerse.chapter,
                            verse = dailyVerse.verse,
                            text = fetchedText
                        )
                    )
                    fetchedText to null
                }
            } catch (_: Exception) {
                null to "Could not load verse. Please check your internet connection."
            }
        }
        votdText = result.first
        votdError = result.second
    }

    val stats = remember(
        record,
        allRecords,
        showReadBible,
        showPray,
        showReviewVerse,
        showJournal,
        showGiveThanks,
        showPrayForOthers,
        showObeyApply
    ) {
        calculateDashboardStats(
            record = record,
            recordsNewestFirst = allRecords,
            visibility = DisciplineVisibility(
                readBible = showReadBible,
                pray = showPray,
                reviewVerse = showReviewVerse,
                journal = showJournal,
                giveThanks = showGiveThanks,
                prayForOthers = showPrayForOthers,
                obeyApply = showObeyApply
            )
        )
    }

    val todayDateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 32.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = todayDateFormatted.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Faithful Walk",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                if (showStreak) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.streak} Days",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Today's Focus Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(20.dp)
            ) {
                // Background geometric element
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 16.dp, y = 16.dp)
                        .size(96.dp)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.05f), CircleShape)
                )
                
                if (votdText != null) {
                    Column {
                        Text(
                            text = "VERSE OF THE DAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "“${votdText}”",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = LocalBibleFontFamily.current,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "— ${dailyVerse.reference} ($bibleTranslation)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                } else if (votdError != null) {
                    val localVerse = dailyVerses[(dayOfYear - 1) % dailyVerses.size]
                    Column {
                        Text(
                            text = "VERSE OF THE DAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "“Could not load verse text. Please check connection.”",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "— ${localVerse.reference}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "VERSE OF THE DAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Skeleton lines
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(20.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(20.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(14.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .align(Alignment.End)
                        )
                    }
                }
            }

            // Progress Overview
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { stats.progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${stats.completedCount} / ${stats.enabledCount} Completed",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Daily Disciplines List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showReadBible) {
                    ChecklistItem("Read Bible chapter", "Daily assigned reading", record.readBible) { checked -> viewModel.updateDailyRecord { it.copy(readBible = checked) } }
                }
                if (showPray) {
                    ChecklistItem("Pray", "Talk with God", record.pray) { checked -> viewModel.updateDailyRecord { it.copy(pray = checked) } }
                }
                if (showReviewVerse) {
                    ChecklistItem("Scripture Memory", "Review today's verses", record.reviewVerse) { checked -> viewModel.updateDailyRecord { it.copy(reviewVerse = checked) } }
                }
                if (showJournal) {
                    ChecklistItem("Evening Reflection", "Journal thoughts", record.journal) { checked -> viewModel.updateDailyRecord { it.copy(journal = checked) } }
                }
                if (showGiveThanks) {
                    ChecklistItem("Give thanks", "Gratitude practice", record.giveThanks) { checked -> viewModel.updateDailyRecord { it.copy(giveThanks = checked) } }
                }
                if (showPrayForOthers) {
                    ChecklistItem("Pray for someone", "Intercession", record.prayForOthers) { checked -> viewModel.updateDailyRecord { it.copy(prayForOthers = checked) } }
                }
                if (showObeyApply) {
                    ChecklistItem("Obey / Apply", "Actionable truth", record.obeyApply) { checked -> viewModel.updateDailyRecord { it.copy(obeyApply = checked) } }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class DisciplineVisibility(
    val readBible: Boolean,
    val pray: Boolean,
    val reviewVerse: Boolean,
    val journal: Boolean,
    val giveThanks: Boolean,
    val prayForOthers: Boolean,
    val obeyApply: Boolean
)

private data class DashboardStats(
    val progress: Float,
    val streak: Int,
    val completedCount: Int,
    val enabledCount: Int
)

private fun calculateDashboardStats(
    record: com.spiritualdisciplines.data.DailyRecord,
    recordsNewestFirst: List<com.spiritualdisciplines.data.DailyRecord>,
    visibility: DisciplineVisibility
): DashboardStats {
    val enabledCount = visibility.enabledCount()
    val completedCount = record.completedCount(visibility)
    val progress = if (enabledCount > 0) completedCount.toFloat() / enabledCount else 1f

    var streak = 0
    for (i in recordsNewestFirst.indices) {
        val completed = recordsNewestFirst[i].completedCount(visibility)
        if (completed == enabledCount && enabledCount > 0) {
            streak++
        } else if (i > 0) {
            break
        }
    }
    return DashboardStats(
        progress = progress,
        streak = streak,
        completedCount = completedCount,
        enabledCount = enabledCount
    )
}

private fun DisciplineVisibility.enabledCount(): Int =
    readBible.toInt() +
        pray.toInt() +
        reviewVerse.toInt() +
        journal.toInt() +
        giveThanks.toInt() +
        prayForOthers.toInt() +
        obeyApply.toInt()

private fun com.spiritualdisciplines.data.DailyRecord.completedCount(
    visibility: DisciplineVisibility
): Int =
    (visibility.readBible && readBible).toInt() +
        (visibility.pray && pray).toInt() +
        (visibility.reviewVerse && reviewVerse).toInt() +
        (visibility.journal && journal).toInt() +
        (visibility.giveThanks && giveThanks).toInt() +
        (visibility.prayForOthers && prayForOthers).toInt() +
        (visibility.obeyApply && obeyApply).toInt()

private fun Boolean.toInt(): Int = if (this) 1 else 0

@Composable
fun ChecklistItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bgColor = if (checked) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
    val haptics = rememberExpressiveHaptics()
    
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val newValue = !checked
                if (newValue) haptics.confirm() else haptics.toggle(false)
                onCheckedChange(newValue)
            },
        tonalElevation = if (checked) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
            ) {
                if (checked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
                    ) {}
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (checked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
