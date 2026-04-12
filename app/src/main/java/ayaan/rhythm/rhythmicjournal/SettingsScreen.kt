package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit
) {
    var autoLocation by rememberSaveable { mutableStateOf(true) }
    var reminderOn by rememberSaveable { mutableStateOf(true) }

    ScreenContainer {
        SimpleHeader(
            leftText = "‹",
            title = "Settings",
            onLeftClick = onBack
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "App theme",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeChoiceChip(
                text = "Light",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) }
            )
            ThemeChoiceChip(
                text = "Dark",
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) }
            )
            ThemeChoiceChip(
                text = "System",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsRow("Default privacy", "Private  ›")
        SettingsRow("Daily reminder", "8:00 PM  ›")
        SettingsRowWithSwitch("Auto-location tagging", autoLocation) { autoLocation = it }
        SettingsRowWithSwitch("Daily reminder enabled", reminderOn) { reminderOn = it }
        SettingsRow("Storage used", "2.3 GB of 5 GB")

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Text("Done")
        }
    }
}