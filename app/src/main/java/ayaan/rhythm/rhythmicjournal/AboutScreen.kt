package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenContainer {
        SimpleHeader(leftText = "‹", title = "About", onLeftClick = onBack)

        Spacer(modifier = Modifier.height(18.dp))

        SoftCard {
            Text(
                text = "Rhythmic Journal is a photo journaling app for capturing meaningful moments with a photo, a reflection, a date, and a place.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        SoftCard {
            Text(
                text = "This version keeps the VSCO-inspired image-first feeling, but it now has a functional local profile and theme switching.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}