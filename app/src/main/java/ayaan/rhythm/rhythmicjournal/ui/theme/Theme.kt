package ayaan.rhythm.rhythmicjournal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = JournalBlack,
    onPrimary = JournalWhite,
    secondary = JournalSoft,
    onSecondary = JournalInk,
    background = JournalWhite,
    onBackground = JournalInk,
    surface = JournalWhite,
    onSurface = JournalInk,
    surfaceVariant = JournalSoft,
    onSurfaceVariant = JournalMutedInk,
    outline = JournalLine
)

private val DarkColors = darkColorScheme(
    primary = JournalWhite,
    onPrimary = JournalBlack,
    secondary = JournalDarkSurfaceSoft,
    onSecondary = JournalWhite,
    background = JournalDarkSurface,
    onBackground = JournalWhite,
    surface = JournalDarkSurface,
    onSurface = JournalWhite,
    surfaceVariant = JournalDarkSurfaceSoft,
    onSurfaceVariant = JournalDarkMuted,
    outline = JournalDarkLine
)

@Composable
fun RhythmTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}