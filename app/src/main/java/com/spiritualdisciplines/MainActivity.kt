package com.spiritualdisciplines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spiritualdisciplines.ui.MainScreen
import com.spiritualdisciplines.ui.theme.MyApplicationTheme
import com.spiritualdisciplines.viewmodel.MainViewModel
import com.spiritualdisciplines.viewmodel.MainViewModelFactory
import com.spiritualdisciplines.worker.VerseCacheWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        VerseCacheWorker.schedule(this)

        setContent {
            val app = application as MainApplication
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(app.repository, app.appPreferences)
            )

            val themeMode = viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode.value) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val accentColor = viewModel.accentColor.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = darkTheme, accentColor = accentColor.value) {
                MainScreen(viewModel)
            }
        }
    }
}
