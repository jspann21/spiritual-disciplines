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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
    
    val filteredRequests = remember(requests, selectedTabIndex) {
        requests.filter {
            if (selectedTabIndex == 0) !it.isArchived else it.isArchived
        }
    }
    val unprayedRequests = remember(filteredRequests, viewModel.todayDateString) {
        filteredRequests.filter {
            !it.isAnswered && it.lastPrayedDate != viewModel.todayDateString
        }
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
        topBar = {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.statusBarsPadding()
            ) {
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
        },
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
                items(filteredRequests, key = { it.id }) { req ->
                    var menuExpanded by remember(req.id) { mutableStateOf(false) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    MarkdownText(
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
                    MarkdownText(
                        text = req.title,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = if (req.title.hasMarkdownList()) TextAlign.Start else TextAlign.Center,
                        lineHeight = 40.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
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
    var title by remember { mutableStateOf(TextFieldValue(initialTitle)) }
    var category by remember { mutableStateOf(initialCategory) }
    val editorFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val categories = listOf("Personal", "Family", "Church", "Unbelievers", "Missionaries", "Nation/leaders")

    fun applyFormatting(transform: (TextFieldValue) -> TextFieldValue) {
        title = transform(title)
        editorFocusRequester.requestFocus()
        keyboardController?.show()
    }

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
                                if (title.text.isNotBlank()) haptics.confirmed { onSave(title.text, category) }
                            },
                            enabled = title.text.isNotBlank(),
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
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (title.text.isEmpty()) {
                            Text(
                                text = "What's on your heart?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it.withContinuedBulletFrom(title) },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(editorFocusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { applyFormatting { it.withInlineMarkdown("**") } }) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                            }
                            IconButton(onClick = { applyFormatting { it.withInlineMarkdown("_") } }) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                            }
                            IconButton(onClick = { applyFormatting { it.withToggledBullets() } }) {
                                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Bulleted list")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    textAlign: TextAlign? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    textDecoration: TextDecoration? = null
) {
    Text(
        text = text.toBasicMarkdown(),
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        lineHeight = lineHeight,
        textDecoration = textDecoration
    )
}

private fun String.hasMarkdownList(): Boolean =
    lineSequence().any {
        val content = it.trimStart()
        content.startsWith("- ") || content.startsWith("* ") || content.startsWith("• ")
    }

private fun String.toBasicMarkdown(): AnnotatedString = buildAnnotatedString {
    lines().forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) append('\n')
        val indentation = line.takeWhile { it == ' ' || it == '\t' }
        val content = line.drop(indentation.length)
        val isListItem = content.startsWith("- ") || content.startsWith("* ") || content.startsWith("• ")
        append(indentation)
        append(if (isListItem) "• " else "")
        appendInlineMarkdown(if (isListItem) content.drop(2) else content)
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(source: String) {
    var index = 0
    while (index < source.length) {
        when {
            source.startsWith("**", index) -> {
                val end = source.indexOf("**", index + 2)
                if (end >= 0) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(source.substring(index + 2, end))
                    pop()
                    index = end + 2
                } else {
                    append("**")
                    index += 2
                }
            }
            source[index] == '_' || source[index] == '*' -> {
                val marker = source[index]
                val end = source.indexOf(marker, index + 1)
                if (end >= 0) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(source.substring(index + 1, end))
                    pop()
                    index = end + 1
                } else {
                    append(marker)
                    index++
                }
            }
            else -> {
                append(source[index])
                index++
            }
        }
    }
}

private fun TextFieldValue.withInlineMarkdown(marker: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selectedText = text.substring(start, end)
    val replacement = marker + selectedText + marker
    val updatedText = text.replaceRange(start, end, replacement)
    val updatedSelection = if (selection.collapsed) {
        TextRange(start + marker.length)
    } else {
        TextRange(start + replacement.length)
    }
    return copy(text = updatedText, selection = updatedSelection)
}

private fun TextFieldValue.withToggledBullets(): TextFieldValue {
    val firstLineStart = if (selection.min == 0) {
        0
    } else {
        text.lastIndexOf('\n', selection.min - 1).let { if (it < 0) 0 else it + 1 }
    }
    val selectedLineEnd = text.indexOf('\n', selection.max).let { if (it < 0) text.length else it }
    val block = text.substring(firstLineStart, selectedLineEnd)
    val lines = block.split('\n')
    val nonBlankLines = lines.filter { it.isNotBlank() }
    val removeBullets = nonBlankLines.isNotEmpty() && nonBlankLines.all { line ->
        val content = line.trimStart()
        content.startsWith("- ") || content.startsWith("* ") || content.startsWith("• ")
    }
    val updatedBlock = lines.joinToString("\n") { line ->
        val indentation = line.takeWhile { it == ' ' || it == '\t' }
        val content = line.drop(indentation.length)
        when {
            line.isBlank() && lines.size == 1 -> "• "
            line.isBlank() -> line
            removeBullets && (
                content.startsWith("- ") || content.startsWith("* ") || content.startsWith("• ")
            ) -> indentation + content.drop(2)
            else -> "$indentation• $content"
        }
    }
    val updatedText = text.replaceRange(firstLineStart, selectedLineEnd, updatedBlock)
    val updatedSelection = if (selection.collapsed && lines.size == 1) {
        val offset = if (removeBullets) -2 else 2
        TextRange((selection.start + offset).coerceIn(firstLineStart, updatedText.length))
    } else {
        TextRange(firstLineStart + updatedBlock.length)
    }
    return copy(
        text = updatedText,
        selection = updatedSelection
    )
}

private fun TextFieldValue.withContinuedBulletFrom(previous: TextFieldValue): TextFieldValue {
    if (text.length != previous.text.length + 1 || !selection.collapsed || selection.start == 0) return this

    val newlineIndex = selection.start - 1
    if (text[newlineIndex] != '\n') return this

    val lineStart = if (newlineIndex == 0) {
        0
    } else {
        text.lastIndexOf('\n', newlineIndex - 1).let { if (it < 0) 0 else it + 1 }
    }
    val previousLine = text.substring(lineStart, newlineIndex)
    val indentation = previousLine.takeWhile { it == ' ' || it == '\t' }
    val content = previousLine.drop(indentation.length)
    val marker = listOf("• ", "- ", "* ").firstOrNull(content::startsWith) ?: return this

    if (content == marker) {
        val updatedText = text.removeRange(lineStart, newlineIndex)
        return copy(text = updatedText, selection = TextRange(lineStart))
    }

    val continuation = indentation + marker
    val updatedText = text.substring(0, selection.start) + continuation + text.substring(selection.start)
    return copy(
        text = updatedText,
        selection = TextRange(selection.start + continuation.length)
    )
}
