package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val journalRepository = remember { JournalRepository() }

    val posts = remember { mutableStateListOf<JournalEntry>() }
    var albumName by remember { mutableStateOf("Album") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(albumId) {
        loading = true
        error = null
        try {
            val albumPosts = journalRepository
                .getCurrentUserJournals()
                .filter { it.albumId == albumId }
                .sortedByDescending { it.createdAt }

            posts.clear()
            posts.addAll(albumPosts)

            albumName = albumPosts.firstOrNull()?.album?.ifBlank { "Album" } ?: "Album"
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load album."
        } finally {
            loading = false
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 8.dp,
            bottom = 110.dp
        )
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            AlbumDetailTopBar(
                title = albumName,
                onBack = onBack
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(6.dp))
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Text(
                text = "${posts.size} ${if (posts.size == 1) "post" else "posts"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            loading -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "Loading...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }

            error != null -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = error ?: "Something went wrong.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp
                    )
                }
            }

            posts.isEmpty() -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "No posts in this album yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }

            else -> {
                items(
                    items = posts,
                    key = { it.id }
                ) { post ->
                    AlbumRecentPostTile(
                        journal = post,
                        onClick = { onOpenEntry(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailTopBar(
    title: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppBackButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun AlbumRecentPostTile(
    journal: JournalEntry,
    onClick: () -> Unit
) {
    if (journal.photoUrl.isNotBlank()) {
        AsyncImage(
            model = journal.photoUrl,
            contentDescription = journal.title.ifBlank { "Post" },
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable { onClick() }
        )
    } else {
        Text(
            text = journal.title.ifBlank { "Untitled" },
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        )
    }
}