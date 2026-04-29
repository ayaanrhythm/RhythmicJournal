package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Suppress("UNUSED_PARAMETER")
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
    val context = LocalContext.current
    val journalRepository = remember { JournalRepository() }
    val profileRepository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()

    var post by remember { mutableStateOf<JournalEntry?>(null) }
    var username by remember { mutableStateOf("username") }
    var profileImageUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showComments by rememberSaveable { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    fun loadPost() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val profile = profileRepository.getOrCreateCurrentUserProfile()
                val loadedPost = journalRepository
                    .getCurrentUserJournals()
                    .firstOrNull { it.id == journalId }

                username = profile.username.ifBlank { "username" }
                profileImageUrl = profile.profileImageUrl

                if (loadedPost == null) {
                    errorMessage = "Post not found."
                    post = null
                } else {
                    post = loadedPost
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not load post."
                post = null
            } finally {
                isLoading = false
            }
        }
    }

    fun updateLoadedPost(updated: JournalEntry) {
        post = updated
    }

    fun shareLoadedPost() {
        val currentPost = post ?: return
        scope.launch {
            try {
                shareJournalPost(context, currentPost)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not share post."
            }
        }
    }

    LaunchedEffect(journalId, refreshTick) {
        loadPost()
    }

    if (showMenu && post != null) {
        val currentPost = post!!
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
                EntryDetailMenuItem(
                    text = "Share",
                    onClick = {
                        shareLoadedPost()
                        showMenu = false
                    }
                )

                EntryDetailMenuItem(
                    text = "Edit Post",
                    onClick = {
                        onEdit(currentPost.id)
                        showMenu = false
                    }
                )

                EntryDetailMenuItem(
                    text = "Delete Post",
                    isDestructive = true,
                    onClick = {
                        scope.launch {
                            try {
                                journalRepository.deleteJournal(currentPost.id)
                                showMenu = false
                                onDeleted()
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Could not delete post."
                            }
                        }
                    }
                )

                EntryDetailMenuItem(
                    text = "Cancel",
                    onClick = { showMenu = false }
                )
            }
        }
    }

    if (showComments && post != null) {
        CommentsBottomSheet(
            journalId = post!!.id,
            onDismiss = { showComments = false },
            onCommentCountChanged = { refreshTick += 1 }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                        .size(28.dp)
                        .clickable { showMenu = true }
                )
            }
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading post...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            errorMessage != null && post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Could not load post.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Post not found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                val currentPost = post!!

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (profileImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = username.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = username,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = currentPost.locationName.ifBlank { "No location" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (currentPost.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentPost.photoUrl,
                        contentDescription = "Post photo",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No photo",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Icon(
                            imageVector = if (currentPost.isLoved) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = "Love",
                            tint = if (currentPost.isLoved) Color.Red else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable {
                                    scope.launch {
                                        try {
                                            val newValue = !currentPost.isLoved
                                            journalRepository.setLoved(currentPost.id, newValue)
                                            updateLoadedPost(currentPost.copy(isLoved = newValue))
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Could not update post."
                                        }
                                    }
                                }
                        )

                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { showComments = true }
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { shareLoadedPost() }
                        )
                    }

                    Icon(
                        imageVector = if (currentPost.isFavorite) {
                            Icons.Filled.Bookmark
                        } else {
                            Icons.Outlined.BookmarkBorder
                        },
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                scope.launch {
                                    try {
                                        val newValue = !currentPost.isFavorite
                                        journalRepository.setFavorite(currentPost.id, newValue)
                                        updateLoadedPost(currentPost.copy(isFavorite = newValue))
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage ?: "Could not update saved post."
                                    }
                                }
                            }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentPost.title.ifBlank { "Untitled post" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (currentPost.reflection.isNotBlank()) {
                        Text(
                            text = currentPost.reflection,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (currentPost.tags.isNotEmpty()) {
                        Text(
                            text = currentPost.tags.joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF6F9BFF),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    if (currentPost.album.isNotBlank()) {
                        Text(
                            text = currentPost.album,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clickable {
                                    if (currentPost.albumId.isNotBlank()) {
                                        onOpenAlbum(currentPost.albumId)
                                    }
                                }
                        )
                    }

                    Spacer(modifier = Modifier.width(1.dp))
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                )
            }
        }
    }
}

@Composable
private fun EntryDetailMenuItem(
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
    )
}