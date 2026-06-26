package com.spiritualdisciplines.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.viewmodel.MainViewModel
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val bibleTranslation by viewModel.bibleTranslation.collectAsStateWithLifecycle()
    val readingPlanId by viewModel.readingPlanId.collectAsStateWithLifecycle()
    val showStreak by viewModel.showStreak.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val notificationHour by viewModel.notificationHour.collectAsStateWithLifecycle()
    val notificationMinute by viewModel.notificationMinute.collectAsStateWithLifecycle()
    val accentColorInt by viewModel.accentColor.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Spacer(modifier = Modifier.height(8.dp))

            // Theme Selection
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOption("system", "System", themeMode) { viewModel.preferences.setThemeMode(it) }
                    ThemeOption("light", "Light", themeMode) { viewModel.preferences.setThemeMode(it) }
                    ThemeOption("dark", "Dark", themeMode) { viewModel.preferences.setThemeMode(it) }
                }

                var showColorDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showColorDialog = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accent Color", style = MaterialTheme.typography.bodyLarge)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(accentColorInt))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }

                if (showColorDialog) {
                    AlertDialog(
                        onDismissRequest = { showColorDialog = false },
                        title = { Text("Select Accent Color") },
                        text = {
                            var currentHue by remember(accentColorInt) {
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(accentColorInt, hsv)
                                mutableFloatStateOf(hsv[0])
                            }

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                ColorWheel(
                                    modifier = Modifier.size(200.dp),
                                    hue = currentHue,
                                    onHueChange = { hue ->
                                        currentHue = hue
                                        val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                                        viewModel.preferences.setAccentColor(color)
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showColorDialog = false }) {
                                Text("Done")
                            }
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Bible Translation
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bible Translation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                var expanded by remember { mutableStateOf(false) }
                val translations = listOf("ESV", "NIV", "KJV", "NKJV", "NLT", "NASB", "LSB")
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = bibleTranslation,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        translations.forEach { translation ->
                            DropdownMenuItem(
                                text = { Text(translation) },
                                onClick = {
                                    viewModel.preferences.setBibleTranslation(translation)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Reading Plan Selection
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reading Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                var expandedPlan by remember { mutableStateOf(false) }
                val plans = com.spiritualdisciplines.data.ReadingPlanRepository.plans
                val selectedPlan = plans.find { it.id == readingPlanId } ?: plans.first()
                
                ExposedDropdownMenuBox(
                    expanded = expandedPlan,
                    onExpandedChange = { expandedPlan = !expandedPlan }
                ) {
                    OutlinedTextField(
                        value = selectedPlan.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPlan) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPlan,
                        onDismissRequest = { expandedPlan = false }
                    ) {
                        plans.forEach { plan ->
                            DropdownMenuItem(
                                text = { Text(plan.name) },
                                onClick = {
                                    viewModel.preferences.setReadingPlanId(plan.id)
                                    expandedPlan = false
                                }
                            )
                        }
                    }
                }
                Text(selectedPlan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(4.dp))
                
                val context = androidx.compose.ui.platform.LocalContext.current
                val startDateEpoch by viewModel.readingPlanStartDate.collectAsStateWithLifecycle()
                val startDate = remember(startDateEpoch) { java.time.LocalDate.ofEpochDay(startDateEpoch) }
                val formattedStartDate = remember(startDate) { startDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")) }
                
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val newDate = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                        viewModel.preferences.setReadingPlanStartDate(newDate.toEpochDay())
                    },
                    startDate.year,
                    startDate.monthValue - 1,
                    startDate.dayOfMonth
                )
                
                OutlinedTextField(
                    value = formattedStartDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start Date") },
                    trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = "Edit Date") },
                    modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                    enabled = false, 
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                val today = java.time.LocalDate.now()
                val currentDayInt = java.time.temporal.ChronoUnit.DAYS.between(startDate, today).toInt() + 1
                var dayInput by remember(currentDayInt) { mutableStateOf(currentDayInt.toString()) }
                
                OutlinedTextField(
                    value = dayInput,
                    onValueChange = { 
                        dayInput = it
                        val d = it.toIntOrNull()
                        if (d != null && d > 0) {
                            val newStartDate = today.minusDays((d - 1).toLong())
                            viewModel.preferences.setReadingPlanStartDate(newStartDate.toEpochDay())
                        }
                    },
                    label = { Text("Current Plan Day (e.g., 1-${selectedPlan.maxDays})") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Show Streak
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.preferences.setShowStreak(!showStreak) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Streak Counter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Show your daily streak on the dashboard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = showStreak,
                    onCheckedChange = { viewModel.preferences.setShowStreak(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Dashboard Items
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Dashboard Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                val showReadBible by viewModel.showReadBible.collectAsStateWithLifecycle()
                val showPray by viewModel.showPray.collectAsStateWithLifecycle()
                val showReviewVerse by viewModel.showReviewVerse.collectAsStateWithLifecycle()
                val showJournal by viewModel.showJournal.collectAsStateWithLifecycle()
                val showGiveThanks by viewModel.showGiveThanks.collectAsStateWithLifecycle()
                val showPrayForOthers by viewModel.showPrayForOthers.collectAsStateWithLifecycle()
                val showObeyApply by viewModel.showObeyApply.collectAsStateWithLifecycle()

                ToggleItem("Read Bible chapter", "Daily assigned reading", showReadBible) { viewModel.preferences.setShowReadBible(it) }
                ToggleItem("Pray", "Talk with God", showPray) { viewModel.preferences.setShowPray(it) }
                ToggleItem("Scripture Memory", "Review today's verses", showReviewVerse) { viewModel.preferences.setShowReviewVerse(it) }
                ToggleItem("Evening Reflection", "Journal thoughts", showJournal) { viewModel.preferences.setShowJournal(it) }
                ToggleItem("Give thanks", "Gratitude practice", showGiveThanks) { viewModel.preferences.setShowGiveThanks(it) }
                ToggleItem("Pray for someone", "Intercession", showPrayForOthers) { viewModel.preferences.setShowPrayForOthers(it) }
                ToggleItem("Obey / Apply", "Actionable truth", showObeyApply) { viewModel.preferences.setShowObeyApply(it) }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            
            val context = androidx.compose.ui.platform.LocalContext.current
            
            // Notification Permission
            var hasNotificationPermission by remember { mutableStateOf(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            )}

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
                if (isGranted && notificationsEnabled) {
                    com.spiritualdisciplines.worker.NotificationWorker.schedule(context, notificationHour, notificationMinute)
                } else if (!isGranted) {
                    viewModel.preferences.setNotificationsEnabled(false)
                }
            }
            
            // Push Notifications
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            val newState = !notificationsEnabled
                            if (newState && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                viewModel.preferences.setNotificationsEnabled(true)
                            } else {
                                viewModel.preferences.setNotificationsEnabled(newState)
                                if (newState) {
                                    com.spiritualdisciplines.worker.NotificationWorker.schedule(context, notificationHour, notificationMinute)
                                } else {
                                    com.spiritualdisciplines.worker.NotificationWorker.cancel(context)
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Get notified to complete your daily spiritual disciplines", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { 
                            if (it && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                viewModel.preferences.setNotificationsEnabled(true)
                            } else {
                                viewModel.preferences.setNotificationsEnabled(it)
                                if (it) {
                                    com.spiritualdisciplines.worker.NotificationWorker.schedule(context, notificationHour, notificationMinute)
                                } else {
                                    com.spiritualdisciplines.worker.NotificationWorker.cancel(context)
                                }
                            }
                        }
                    )
                }
                
                if (notificationsEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val timePickerDialog = android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        viewModel.preferences.setNotificationTime(hourOfDay, minute)
                                        com.spiritualdisciplines.worker.NotificationWorker.schedule(context, hourOfDay, minute)
                                    },
                                    notificationHour,
                                    notificationMinute,
                                    false
                                )
                                timePickerDialog.show()
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reminder Time", style = MaterialTheme.typography.bodyLarge)
                        val amPm = if (notificationHour >= 12) "PM" else "AM"
                        val hour12 = if (notificationHour % 12 == 0) 12 else notificationHour % 12
                        val minStr = notificationMinute.toString().padStart(2, '0')
                        Text("$hour12:$minStr $amPm", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Data & Cache
            var showClearCacheDialog by remember { mutableStateOf(false) }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Data & Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearCacheDialog = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Cache", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Free up space by clearing downloaded verses and chapters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatFileSize(cacheSizeBytes),
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showClearCacheDialog) {
                AlertDialog(
                    onDismissRequest = { showClearCacheDialog = false },
                    title = { Text("Clear Cache") },
                    text = {
                        Text(
                            "Clear ${formatFileSize(cacheSizeBytes)} of downloaded Bible verses and chapters? They will be re-downloaded when needed."
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.clearAllCache {
                                showClearCacheDialog = false
                            }
                        }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearCacheDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0)
    val kilobytes = safeBytes / 1024.0

    val (size, unit) = when {
        kilobytes < 1024 -> kilobytes to "KB"
        kilobytes < 1024 * 1024 -> kilobytes / 1024 to "MB"
        else -> kilobytes / (1024 * 1024) to "GB"
    }

    return when {
        safeBytes == 0L -> "0 KB"
        size >= 10 -> String.format(Locale.US, "%.0f %s", size, unit)
        else -> String.format(Locale.US, "%.1f %s", size, unit)
    }
}

@Composable
fun ToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun RowScope.ThemeOption(mode: String, label: String, selectedMode: String, onSelect: (String) -> Unit) {
    val isSelected = mode == selectedMode
    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable { onSelect(mode) },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    val wheelColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val pos = change.position
                    val dx = pos.x - center.x
                    val dy = pos.y - center.y
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f
                    // the SweepGradient maps 0-360 starting from 3 o'clock
                    onHueChange(angle)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0) angle += 360f
                    onHueChange(angle)
                }
            }
        ) {
            val strokeWidth = 32.dp.toPx()
            drawCircle(
                brush = Brush.sweepGradient(wheelColors),
                radius = size.minDimension / 2 - strokeWidth / 2,
                style = Stroke(strokeWidth)
            )

            // Draw thumb indicator
            val r = size.minDimension / 2 - strokeWidth / 2
            val angleRad = Math.toRadians(hue.toDouble())
            val thumbX = center.x + r * cos(angleRad).toFloat()
            val thumbY = center.y + r * sin(angleRad).toFloat()
            
            drawCircle(
                color = Color.White,
                radius = strokeWidth / 1.5f,
                center = Offset(thumbX, thumbY)
            )
            drawCircle(
                color = Color.Black,
                radius = strokeWidth / 1.5f,
                center = Offset(thumbX, thumbY),
                style = Stroke(2.dp.toPx())
            )
        }
    }
}
