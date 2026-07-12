@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.spiritualdisciplines.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.data.PrayerRequest
import com.spiritualdisciplines.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(viewModel: MainViewModel) {
    val haptics = rememberExpressiveHaptics()
    val requests by viewModel.prayerRequests.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingRequest by remember { mutableStateOf<PrayerRequest?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val filteredRequests = requests.filter { 
        if (selectedTabIndex == 0) !it.isArchived else it.isArchived 
    }

    var sessionRequests by remember { mutableStateOf<List<PrayerRequest>?>(null) }

    sessionRequests?.let { requests ->
        PrayerSessionScreen(
            requests = requests,
            onClose = { sessionRequests = null },
            onPrayed = { req -> viewModel.markPrayerPrayed(req.id) }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Prayer List") }) },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(onClick = { haptics.pressed { showDialog = true } }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Prayer")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { haptics.selected { selectedTabIndex = 0 } },
                    text = { Text("Active") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { haptics.selected { selectedTabIndex = 1 } },
                    text = { Text("Archived") }
                )
            }
            
            val unprayedRequests = filteredRequests.filter { !it.isAnswered && it.lastPrayedDate != viewModel.todayDateString }
            if (selectedTabIndex == 0 && unprayedRequests.isNotEmpty()) {
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { haptics.pressed { sessionRequests = unprayedRequests } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Prayer Session (${unprayedRequests.size})")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRequests) { req ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = req.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (req.isAnswered) TextDecoration.LineThrough else null
                                    )
                                    Text(text = "Category: ${req.category}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = req.isAnswered,
                                        onCheckedChange = {
                                            haptics.toggle(it)
                                            viewModel.markPrayerAnswered(req.id, it)
                                        }
                                    )
                                    Box {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                        }
                                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Edit") },
                                                onClick = { 
                                                    editingRequest = req
                                                    showDialog = true
                                                    menuExpanded = false 
                                                }
                                            )
                                            if (selectedTabIndex == 0) {
                                                DropdownMenuItem(
                                                    text = { Text("Archive") },
                                                    onClick = { 
                                                        viewModel.archivePrayerRequest(req.id, true)
                                                        menuExpanded = false 
                                                    }
                                                )
                                            } else {
                                                DropdownMenuItem(
                                                    text = { Text("Unarchive") },
                                                    onClick = { 
                                                        viewModel.archivePrayerRequest(req.id, false)
                                                        menuExpanded = false 
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = { 
                                                    viewModel.deletePrayerRequest(req.id)
                                                    menuExpanded = false 
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val isPrayedToday = req.lastPrayedDate == viewModel.todayDateString
                            var prayedInUI by remember(req.id) { mutableStateOf(false) }
                            
                            val isPrayed = isPrayedToday || prayedInUI
                            
                            Button(
                                shapes = ButtonDefaults.shapes(),
                                onClick = { 
                                    haptics.confirm()
                                    viewModel.markPrayerPrayed(req.id)
                                    prayedInUI = true
                                },
                                enabled = !isPrayed && !req.isAnswered && selectedTabIndex == 0,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isPrayed) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Prayed Today")
                                } else {
                                    Text("Mark as Prayed")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            PrayerDialog(
                initialTitle = editingRequest?.title ?: "",
                initialCategory = editingRequest?.category ?: "Personal",
                isEdit = editingRequest != null,
                onDismiss = { 
                    showDialog = false
                    editingRequest = null
                },
                onSave = { title, cat ->
                    val requestBeingEdited = editingRequest
                    if (requestBeingEdited != null) {
                        viewModel.updatePrayerRequest(requestBeingEdited.id, title, cat)
                    } else {
                        viewModel.addPrayerRequest(title, cat)
                    }
                    showDialog = false
                    editingRequest = null
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PrayerSessionScreen(
    requests: List<PrayerRequest>,
    onClose: () -> Unit,
    onPrayed: (PrayerRequest) -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    val pagerState = rememberPagerState(pageCount = { requests.size })
    val coroutineScope = rememberCoroutineScope()
    // Keep track of which requests were marked as prayed in this session to update the UI immediately
    val prayedIndices = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prayer Session") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "End Session")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val req = requests[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Calculate the absolute offset for the current page from the
                            // scroll position. We use the absolute value which allows us to mirror
                            // any effects for both directions
                            val pageOffset = (
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).absoluteValue

                            // We animate the alpha, between 0% and 100%
                            alpha = lerp(
                                start = 0f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                            
                            // Animate the scale
                            val scale = lerp(
                                start = 0.85f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                            scaleX = scale
                            scaleY = scale

                            // Slide transition
                            val slideDistance = 200f
                            translationX = if (page < pagerState.currentPage) {
                                -slideDistance * pageOffset
                            } else {
                                slideDistance * pageOffset
                            }
                        }
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = req.category.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = req.title,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Bottom controls
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} of ${requests.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val isCurrentPrayed = prayedIndices[pagerState.currentPage] == true
                    
                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            haptics.confirm()
                            if (!isCurrentPrayed) {
                                val req = requests[pagerState.currentPage]
                                onPrayed(req)
                                prayedIndices[pagerState.currentPage] = true
                            }
                            
                            if (pagerState.currentPage < requests.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onClose()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentPrayed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (isCurrentPrayed) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Next", fontSize = 16.sp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (pagerState.currentPage == requests.size - 1) "Finish" else "Mark & Next", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerDialog(
    initialTitle: String,
    initialCategory: String,
    isEdit: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val haptics = rememberExpressiveHaptics()
    var title by remember { mutableStateOf(initialTitle) }
    var category by remember { mutableStateOf(initialCategory) }
    val categories = listOf("Personal", "Family", "Church", "Unbelievers", "Missionaries", "Nation/leaders")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .systemBarsPadding()
            ) {
                TopAppBar(
                    title = { Text(if (isEdit) "Edit Prayer Request" else "Add Prayer Request") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                if (title.isNotBlank()) haptics.confirmed { onSave(title, category) }
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Save")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { haptics.selected { category = cat } },
                                label = { Text(cat) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (title.isEmpty()) {
                            Text(
                                text = "What's on your heart?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}
