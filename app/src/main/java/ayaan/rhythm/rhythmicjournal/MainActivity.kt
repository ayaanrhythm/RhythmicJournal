package ayaan.rhythm.rhythmicjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import ayaan.rhythm.rhythmicjournal.ui.theme.RhythmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val store = remember { LocalAppStore(applicationContext) }
            var themeMode by remember { mutableStateOf(store.loadThemeMode()) }

            val useDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            RhythmTheme(darkTheme = useDarkTheme) {
                RhythmicJournalApp(
                    store = store,
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        store.saveThemeMode(newMode)
                    }
                )
            }
        }
    }
}