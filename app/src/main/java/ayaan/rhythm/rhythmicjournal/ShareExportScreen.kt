package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ShareExportScreen(
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    ScreenContainer {
        SimpleHeader(
            leftText = "‹",
            title = "Share Entry",
            rightText = "Done",
            onLeftClick = onBack,
            onRightClick = onDone
        )

        Spacer(modifier = Modifier.height(18.dp))

        SoftCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniPhoto(
                    text = "IMG",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Text(
                    text = "Sunday walk after class",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Private entry preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        ShareOptionButton("Email")
        Spacer(modifier = Modifier.height(10.dp))
        ShareOptionButton("SMS")
        Spacer(modifier = Modifier.height(10.dp))
        ShareOptionButton("Social")
        Spacer(modifier = Modifier.height(10.dp))
        ShareOptionButton("Download JPG")
        Spacer(modifier = Modifier.height(10.dp))
        ShareOptionButton("Download PDF")
        Spacer(modifier = Modifier.height(10.dp))
        ShareOptionButton("Copy share link")
        Spacer(modifier = Modifier.height(10.dp))
        ShareOptionButton("Export media ZIP")
    }
}