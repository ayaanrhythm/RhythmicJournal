package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ayaan.rhythm.rhythmicjournal.ui.theme.ArtisticWordmarkStyle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onNewEntry: () -> Unit
) {
    val journalRepository = remember { JournalRepository() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var journals by remember { mutableStateOf<List<JournalEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadJournals() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                journals = journalRepository.getCurrentUserJournals()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not load journals."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadJournals()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadJournals()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ScreenContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "☰",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable(onClick = onOpenDrawer)
            )

            androidx.compose.foundation.layout.Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Rhythmic Journal",
                    style = ArtisticWordmarkStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(36.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Journals",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Only the journals created by this signed-in user appear here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading -> {
                SoftCard {
                    Text(
                        text = "Loading your journals...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            errorMessage != null -> {
                SoftCard {
                    Text(
                        text = errorMessage ?: "Could not load journals.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SmallActionButton(
                        text = "Retry",
                        onClick = { loadJournals() }
                    )
                }
            }

            journals.isEmpty() -> {
                AccentAnnouncementCard(
                    title = "No journals yet.",
                    body = "Create your first entry and it will appear here for your account.",
                    primaryAction = "NEW JOURNAL",
                    secondaryAction = "REFRESH",
                    onPrimaryAction = onNewEntry,
                    onSecondaryAction = { loadJournals() }
                )
            }

            else -> {
                journals.forEachIndexed { index, journal ->
                    HomeJournalCard(
                        journal = journal,
                        onClick = {
                            if (journal.id.isNotBlank()) {
                                onOpenEntry(journal.id)
                            }
                        }
                    )

                    if (index != journals.lastIndex) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SolidPillButton(
            text = "NEW JOURNAL",
            modifier = Modifier.fillMaxWidth(),
            onClick = onNewEntry
        )
    }
}

@Composable
private fun HomeJournalCard(
    journal: JournalEntry,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (journal.photoUrl.isBlank()) {
                MiniPhoto(
                    text = "PHOTO",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            } else {
                AsyncImage(
                    model = journal.photoUrl,
                    contentDescription = "Journal photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = journal.title.ifBlank { "Untitled entry" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildSubtitle(journal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildDescription(journal),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun buildSubtitle(journal: JournalEntry): String {
    val datePart = journal.entryDateText.ifBlank { "No date" }
    val locationPart = journal.locationName.ifBlank { "No location" }
    return "$datePart • $locationPart"
}

private fun buildDescription(journal: JournalEntry): String {
    val reflectionPreview = journal.reflection
        .trim()
        .replace("\n", " ")
        .take(120)
        .let {
            if (journal.reflection.trim().length > 120) "$it..." else it
        }

    return when {
        reflectionPreview.isNotBlank() -> reflectionPreview
        journal.album.isNotBlank() -> "Album: ${journal.album}"
        journal.tags.isNotEmpty() -> journal.tags.joinToString(prefix = "#", separator = " #")
        else -> "Open this journal entry."
    }
}