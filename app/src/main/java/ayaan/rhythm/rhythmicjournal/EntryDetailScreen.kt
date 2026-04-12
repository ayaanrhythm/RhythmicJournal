package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    journalId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onShare: () -> Unit,
    onDeleted: () -> Unit
) {
    val journalRepository = remember { JournalRepository() }
    val profileRepository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()

    var journal by remember { mutableStateOf<JournalEntry?>(null) }
    var username by remember { mutableStateOf("username") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }

    fun loadEntry() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                journal = journalRepository.getJournalById(journalId)
                username = profileRepository.getOrCreateCurrentUserProfile().username.ifBlank { "username" }

                if (journal == null) {
                    errorMessage = "Post not found."
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not load post."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(journalId) {
        loadEntry()
    }

    if (showMenu) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                DetailMenuItem(
                    text = "Share options",
                    onClick = {
                        showMenu = false
                        onShare()
                    }
                )

                DetailMenuItem(
                    text = "Edit post",
                    onClick = {
                        val entry = journal
                        if (entry != null) {
                            showMenu = false
                            onEdit(entry.id)
                        }
                    }
                )

                DetailMenuItem(
                    text = "Delete post",
                    isDestructive = true,
                    onClick = {
                        val entry = journal
                        if (entry != null && !isDeleting) {
                            scope.launch {
                                isDeleting = true
                                try {
                                    journalRepository.deleteJournal(entry.id)
                                    showMenu = false
                                    onDeleted()
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Could not delete post."
                                } finally {
                                    isDeleting = false
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailMenuItem(
                    text = "Cancel",
                    onClick = { showMenu = false }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(34.dp)
                    .clickable(onClick = onBack)
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(30.dp)
                    .clickable { showMenu = true }
            )
        }

        when {
            isLoading -> {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Loading post...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            errorMessage != null -> {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = errorMessage ?: "Could not load post.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            journal != null -> {
                val entry = journal!!
                val tagsLine = entry.tags.joinToString(separator = " ") { "#$it" }

                if (entry.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = entry.photoUrl,
                        contentDescription = "Post photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    )
                } else {
                    LargePhotoPlaceholder(label = "NO PHOTO")
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )

                        EntryIconAction(
                            icon = if (entry.isFavorite) Icons.Rounded.Star else Icons.Outlined.Star,
                            contentDescription = "Love",
                            onClick = {
                                scope.launch {
                                    try {
                                        val newValue = !entry.isFavorite
                                        journalRepository.setFavorite(entry.id, newValue)
                                        journal = entry.copy(isFavorite = newValue)
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage ?: "Could not update favorites."
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(18.dp))

                        EntryIconAction(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            onClick = { }
                        )

                        Spacer(modifier = Modifier.width(18.dp))

                        EntryIconAction(
                            icon = Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            onClick = onShare
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = entry.title.ifBlank { "Untitled post" },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (entry.reflection.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = entry.reflection,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (tagsLine.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tagsLine,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    DetailMetaRow(
                        icon = Icons.Outlined.LocationOn,
                        leadingText = entry.entryDateText.ifBlank { "No date" },
                        trailingText = entry.locationName.ifBlank { "No location" }
                    )

                    if (entry.album.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailSingleMetaRow(
                            icon = Icons.Outlined.Collections,
                            text = entry.album
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments (0)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "View all",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Join the discussion...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun EntryIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun DetailMetaRow(
    icon: ImageVector,
    leadingText: String,
    trailingText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leadingText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(18.dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = trailingText,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DetailSingleMetaRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun DetailMenuItem(
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
    )
}