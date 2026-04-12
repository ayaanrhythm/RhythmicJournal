package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val journalRepository = remember { JournalRepository() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var favorites by remember { mutableStateOf<List<JournalEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadFavorites() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                favorites = journalRepository.getFavoriteJournals()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not load favorites."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFavorites()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadFavorites()
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable(onClick = onBack)
            )

            Text(
                text = "Favorites",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(1.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        when {
            isLoading -> {
                Text(
                    text = "Loading favorites...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Could not load favorites.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            favorites.isEmpty() -> {
                Text(
                    text = "No loved posts yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                favorites.forEachIndexed { index, journal ->
                    JournalEntryPreviewCard(
                        title = journal.title.ifBlank { "Untitled post" },
                        subtitle = buildFavoriteSubtitle(journal),
                        description = journal.reflection.ifBlank { "Open this post." },
                        onClick = {
                            if (journal.id.isNotBlank()) {
                                onOpenEntry(journal.id)
                            }
                        }
                    )

                    if (index != favorites.lastIndex) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

private fun buildFavoriteSubtitle(journal: JournalEntry): String {
    val datePart = journal.entryDateText.ifBlank { "No date" }
    val locationPart = journal.locationName.ifBlank { "No location" }
    return "$datePart • $locationPart"
}