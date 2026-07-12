package com.spiritualdisciplines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spiritualdisciplines.ui.MainScreen
import com.spiritualdisciplines.ui.theme.MyApplicationTheme
import com.spiritualdisciplines.ui.theme.LocalBibleFontFamily
import com.spiritualdisciplines.ui.theme.bibleFontFamily
import com.spiritualdisciplines.viewmodel.MainViewModel
import com.spiritualdisciplines.viewmodel.MainViewModelFactory
import com.spiritualdisciplines.worker.VerseCacheWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            val bibleFont = viewModel.bibleFont.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = darkTheme, accentColor = accentColor.value) {
                CompositionLocalProvider(LocalBibleFontFamily provides bibleFontFamily(bibleFont.value)) {
                    MainScreen(viewModel)
                }
            }
        }

        window.decorView.post {
            lifecycleScope.launch(Dispatchers.Default) {
                VerseCacheWorker.schedule(applicationContext)
            }
        }
    }
}
