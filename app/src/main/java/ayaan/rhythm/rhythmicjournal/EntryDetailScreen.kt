package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreHoriz
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onDeleted: () -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val journalRepository = remember { JournalRepository() }
    val profileRepository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()

    var journal by remember { mutableStateOf<JournalEntry?>(null) }
    var username by remember { mutableStateOf("username") }
    var commentCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showComments by rememberSaveable { mutableStateOf(false) }

    fun loadEntry() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                journal = journalRepository.getJournalById(journalId)
                commentCount = journalRepository.getComments(journalId).size
                username = profileRepository.getOrCreateCurrentUserProfile().username.ifBlank { "username" }
                if (journal == null) errorMessage = "Post not found."
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

    if (showComments) {
        CommentsBottomSheet(
            journalId = journalId,
            onDismiss = { showComments = false },
            onCommentCountChanged = { count -> commentCount = count }
        )
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
                DetailMenuItem("Share") {
                    showMenu = false
                    onShare()
                }

                DetailMenuItem("Edit Post") {
                    journal?.let {
                        showMenu = false
                        onEdit(it.id)
                    }
                }

                DetailMenuItem(
                    text = "Delete Post",
                    isDestructive = true
                ) {
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

                DetailMenuItem("Cancel") {
                    showMenu = false
                }
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
        AppScreenHeader(
            onBack = onBack,
            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { showMenu = true }
                )
            }
        )

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
                val tagsLine = entry.tags.joinToString(" ") { "#$it" }

                if (entry.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = entry.photoUrl,
                        contentDescription = "Post photo",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                } else {
                    PostPhotoPlaceholder()
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
                            modifier = Modifier.fillMaxWidth(0.45f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            EntryIconAction(
                                icon = if (entry.isLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Love",
                                tint = if (entry.isLoved) Color.Red else MaterialTheme.colorScheme.onBackground,
                                onClick = {
                                    scope.launch {
                                        try {
                                            val newValue = !entry.isLoved
                                            journalRepository.setLoved(entry.id, newValue)
                                            journal = entry.copy(isLoved = newValue)
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Could not update liked state."
                                        }
                                    }
                                }
                            )

                            EntryIconAction(
                                icon = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comments",
                                onClick = { showComments = true }
                            )

                            EntryIconAction(
                                icon = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Share",
                                onClick = onShare
                            )

                            EntryIconAction(
                                icon = if (entry.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                onClick = {
                                    scope.launch {
                                        try {
                                            val newValue = !entry.isFavorite
                                            journalRepository.setFavorite(entry.id, newValue)
                                            journal = entry.copy(isFavorite = newValue)
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Could not update saved state."
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Text(
                        text = entry.title.ifBlank { "Untitled post" },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 18.dp)
                    )

                    if (entry.reflection.isNotBlank()) {
                        Text(
                            text = entry.reflection,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (tagsLine.isNotBlank()) {
                        Text(
                            text = tagsLine,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF6F9BFF),
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    DetailMetaRow(
                        leadingText = entry.entryDateText.ifBlank { "No date" },
                        trailingText = entry.locationName.ifBlank { "No location" }
                    )

                    if (entry.album.isNotBlank()) {
                        DetailSingleMetaRow(
                            icon = Icons.Outlined.Collections,
                            text = entry.album,
                            onClick = {
                                if (entry.albumId.isNotBlank()) {
                                    onOpenAlbum(entry.albumId)
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 18.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showComments = true }
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comments ($commentCount)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "View all",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun DetailMetaRow(
    leadingText: String,
    trailingText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 18.dp)
    ) {
        Text(
            text = leadingText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            imageVector = Icons.Outlined.LocationOn,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 12.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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

@Composable
private fun PostPhotoPlaceholder() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "NO PHOTO",
            modifier = Modifier.padding(vertical = 80.dp, horizontal = 20.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}