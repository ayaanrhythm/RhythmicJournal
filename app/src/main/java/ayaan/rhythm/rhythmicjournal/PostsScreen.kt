package ayaan.rhythm.rhythmicjournal

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditPost: (String) -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val context = LocalContext.current
    val journalRepository = remember { JournalRepository() }
    val profileRepository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val posts = remember { mutableStateListOf<JournalEntry>() }

    var username by remember { mutableStateOf("username") }
    var profileImageUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMenuPostId by rememberSaveable { mutableStateOf("") }
    var selectedCommentsPostId by rememberSaveable { mutableStateOf("") }
    var refreshTick by remember { mutableIntStateOf(0) }

    val selectedMenuPost = posts.firstOrNull { it.id == selectedMenuPostId }

    fun sharePost(post: JournalEntry) {
        val shareText = buildString {
            append(post.title.ifBlank { "RhythmicJournal post" })

            if (post.reflection.isNotBlank()) {
                append("\n\n")
                append(post.reflection)
            }

            if (post.tags.isNotEmpty()) {
                append("\n\n")
                append(post.tags.joinToString(" ") { "#$it" })
            }

            if (post.locationName.isNotBlank()) {
                append("\n")
                append(post.locationName)
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share post"))
    }

    fun updatePostInList(updated: JournalEntry) {
        val index = posts.indexOfFirst { it.id == updated.id }
        if (index != -1) posts[index] = updated
    }

    fun loadFeed() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val profile = profileRepository.getOrCreateCurrentUserProfile()
                val loadedPosts = journalRepository.getCurrentUserJournals()

                username = profile.username.ifBlank { "username" }
                profileImageUrl = profile.profileImageUrl

                posts.clear()
                posts.addAll(loadedPosts.sortedByDescending { it.createdAt })
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not load posts."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit, refreshTick) {
        loadFeed()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (selectedMenuPost != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedMenuPostId = "" },
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
                PostMenuItem(
                    text = "Share",
                    onClick = {
                        sharePost(selectedMenuPost)
                        selectedMenuPostId = ""
                    }
                )

                PostMenuItem(
                    text = "Edit Post",
                    onClick = {
                        onEditPost(selectedMenuPost.id)
                        selectedMenuPostId = ""
                    }
                )

                PostMenuItem(
                    text = "Delete Post",
                    isDestructive = true,
                    onClick = {
                        scope.launch {
                            try {
                                journalRepository.deleteJournal(selectedMenuPost.id)
                                posts.removeAll { it.id == selectedMenuPost.id }
                                selectedMenuPostId = ""
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Could not delete post."
                            }
                        }
                    }
                )

                PostMenuItem(
                    text = "Cancel",
                    onClick = { selectedMenuPostId = "" }
                )
            }
        }
    }

    if (selectedCommentsPostId.isNotBlank()) {
        CommentsBottomSheet(
            journalId = selectedCommentsPostId,
            onDismiss = { selectedCommentsPostId = "" },
            onCommentCountChanged = { }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AppScreenHeader(
            title = "Posts",
            subtitle = username,
            onBack = onBack
        )

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Loading posts...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage ?: "Could not load posts.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            posts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No posts yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(posts, key = { it.id }) { post ->
                        PostFeedCard(
                            post = post,
                            username = username,
                            profileImageUrl = profileImageUrl,
                            onMore = { selectedMenuPostId = post.id },
                            onToggleLove = {
                                scope.launch {
                                    try {
                                        val newValue = !post.isLoved
                                        journalRepository.setLoved(post.id, newValue)
                                        updatePostInList(post.copy(isLoved = newValue))
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage ?: "Could not update post."
                                    }
                                }
                            },
                            onComments = { selectedCommentsPostId = post.id },
                            onShare = { sharePost(post) },
                            onSave = {
                                scope.launch {
                                    try {
                                        val newValue = !post.isFavorite
                                        journalRepository.setFavorite(post.id, newValue)
                                        updatePostInList(post.copy(isFavorite = newValue))
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage ?: "Could not update saved post."
                                    }
                                }
                            },
                            onOpenEntry = { onOpenEntry(post.id) },
                            onOpenAlbum = {
                                if (post.albumId.isNotBlank()) {
                                    onOpenAlbum(post.albumId)
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PostFeedCard(
    post: JournalEntry,
    username: String,
    profileImageUrl: String,
    onMore: () -> Unit,
    onToggleLove: () -> Unit,
    onComments: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onOpenEntry: () -> Unit,
    onOpenAlbum: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                        text = post.locationName.ifBlank { "No location" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onMore)
            )
        }

        if (post.photoUrl.isNotBlank()) {
            AsyncImage(
                model = post.photoUrl,
                contentDescription = "Post photo",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(onClick = onOpenEntry)
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clickable(onClick = onOpenEntry),
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
                    imageVector = if (post.isLoved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Love",
                    tint = if (post.isLoved) Color.Red else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable(onClick = onToggleLove)
                )

                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable(onClick = onComments)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable(onClick = onShare)
                )
            }

            Icon(
                imageVector = if (post.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Save",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(onClick = onSave)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = post.title.ifBlank { "Untitled post" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable(onClick = onOpenEntry)
            )

            if (post.reflection.isNotBlank()) {
                Text(
                    text = post.reflection,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (post.tags.isNotEmpty()) {
                Text(
                    text = post.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF6F9BFF),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (post.album.isNotBlank()) {
                Text(
                    text = post.album,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable(onClick = onOpenAlbum)
                )
            }

            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
private fun PostMenuItem(
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