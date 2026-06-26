package com.spiritualdisciplines.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spiritualdisciplines.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: MainViewModel) {
    val journal by viewModel.todayJournal.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Daily Journal") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            JournalPromptField(
                label = "What did I read?",
                value = journal.whatDidIRead,
                onValueChange = { viewModel.updateJournalEntry { entry -> entry.copy(whatDidIRead = it) } }
            )
            JournalPromptField(
                label = "What should I obey?",
                value = journal.whatShouldIObey,
                onValueChange = { viewModel.updateJournalEntry { entry -> entry.copy(whatShouldIObey = it) } }
            )
            JournalPromptField(
                label = "What should I pray about?",
                value = journal.whatShouldIPrayAbout,
                onValueChange = { viewModel.updateJournalEntry { entry -> entry.copy(whatShouldIPrayAbout = it) } }
            )
            JournalPromptField(
                label = "What am I thankful for?",
                value = journal.whatAmIThankfulFor,
                onValueChange = { viewModel.updateJournalEntry { entry -> entry.copy(whatAmIThankfulFor = it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun JournalPromptField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )
}
