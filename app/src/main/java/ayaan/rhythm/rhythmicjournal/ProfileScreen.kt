package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

private enum class ProfileTab {
    RECENT,
    ALBUMS
}

private data class ProfileAlbumUi(
    val albumId: String,
    val albumName: String,
    val count: Int,
    val coverUrls: List<String>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onOpenDrawer: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val profileRepository = remember { ProfileRepository() }
    val journalRepository = remember { JournalRepository() }

    var profile by remember { mutableStateOf(UserProfile()) }
    val posts = remember { mutableStateListOf<JournalEntry>() }

    var selectedTab by remember { mutableStateOf(ProfileTab.RECENT) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            profile = profileRepository.getOrCreateCurrentUserProfile()
            posts.clear()
            posts.addAll(
                journalRepository.getCurrentUserJournals()
                    .sortedByDescending { it.createdAt }
            )
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load profile."
        } finally {
            loading = false
        }
    }

    val albums = remember(posts.toList()) {
        posts
            .filter { it.albumId.isNotBlank() || it.album.isNotBlank() }
            .groupBy { if (it.albumId.isNotBlank()) it.albumId else it.album }
            .map { (_, groupedPosts) ->
                val first = groupedPosts.first()
                ProfileAlbumUi(
                    albumId = first.albumId,
                    albumName = first.album.ifBlank { "Album" },
                    count = groupedPosts.size,
                    coverUrls = groupedPosts
                        .sortedByDescending { it.createdAt }
                        .mapNotNull { entry -> entry.photoUrl.takeIf { url -> url.isNotBlank() } }
                        .take(4)
                )
            }
            .sortedBy { it.albumName.lowercase() }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 8.dp,
            bottom = 100.dp
        )
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            ProfileTopBar(
                username = profile.username.ifBlank { "ayaanrhythm" },
                onOpenDrawer = onOpenDrawer,
                onMore = onOpenEditProfile
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(2.dp))
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            CompactProfileHeader(profile = profile)
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(4.dp))
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            ButtonsRow(
                onMyGalleries = { selectedTab = ProfileTab.RECENT },
                onEditProfile = onOpenEditProfile
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(1.dp))
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
            )
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(2.dp))
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProfileTabItem(
                    text = "RECENT",
                    selected = selectedTab == ProfileTab.RECENT,
                    onClick = { selectedTab = ProfileTab.RECENT }
                )

                ProfileTabItem(
                    text = "ALBUMS",
                    selected = selectedTab == ProfileTab.ALBUMS,
                    onClick = { selectedTab = ProfileTab.ALBUMS }
                )
            }
        }

        item(span = StaggeredGridItemSpan.FullLine) {
            Spacer(modifier = Modifier.height(2.dp))
        }

        when {
            loading -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "Loading...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            error != null -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = error ?: "Something went wrong.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            selectedTab == ProfileTab.RECENT && posts.isEmpty() -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "No posts yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            selectedTab == ProfileTab.RECENT -> {
                items(
                    items = posts.sortedByDescending { it.createdAt },
                    key = { it.id }
                ) { post ->
                    RecentPostTile(
                        journal = post,
                        onClick = { onOpenEntry(post.id) }
                    )
                }
            }

            selectedTab == ProfileTab.ALBUMS && albums.isEmpty() -> {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "No albums yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                items(
                    items = albums,
                    key = { if (it.albumId.isNotBlank()) it.albumId else it.albumName }
                ) { album ->
                    AlbumCard(album = album)
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    username: String,
    onOpenDrawer: () -> Unit,
    onMore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "Menu",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(28.dp)
                .clickable { onOpenDrawer() }
        )

        Text(
            text = username.ifBlank { "ayaanrhythm" },
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(24.dp)
                .clickable { onMore() }
        )
    }
}

@Composable
private fun CompactProfileHeader(
    profile: UserProfile
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (profile.profileImageUrl.isNotBlank()) {
                AsyncImage(
                    model = profile.profileImageUrl,
                    contentDescription = "Profile image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = profile.name.ifBlank { "Ayaan Rhythm" },
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (profile.about.isNotBlank()) {
            Text(
                text = profile.about,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
        }

        val schoolLine = buildSchoolLine(
            school = profile.school,
            graduationYear = profile.graduationYear
        )

        if (schoolLine.isNotBlank()) {
            Text(
                text = "🎓 $schoolLine",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ButtonsRow(
    onMyGalleries: () -> Unit,
    onEditProfile: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val sidePadding = 28.dp
    val spacing = 10.dp
    val buttonWidth = (screenWidth - sidePadding - spacing) / 2

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        PillButton(
            text = "MY GALLERIES",
            selected = true,
            width = buttonWidth,
            onClick = onMyGalleries
        )

        PillButton(
            text = "EDIT PROFILE",
            selected = false,
            width = buttonWidth,
            onClick = onEditProfile
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    selected: Boolean,
    width: Dp,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(width)
            .height(44.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ProfileTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (selected) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}

@Composable
private fun RecentPostTile(
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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = journal.title.ifBlank { "No Image" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: ProfileAlbumUi
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            AlbumCoverGrid(album = album)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = album.albumName.ifBlank { "Album" },
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${album.count} ${if (album.count == 1) "entry" else "entries"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AlbumCoverGrid(
    album: ProfileAlbumUi
) {
    val urls = album.coverUrls.take(4)

    if (urls.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "COVER",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    if (urls.size == 1) {
        AsyncImage(
            model = urls.first(),
            contentDescription = "Album cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        return
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val sidePadding = 28.dp
    val gap = 10.dp + 4.dp
    val cardPadding = 24.dp
    val columnWidth = (screenWidth - sidePadding - 10.dp) / 2
    val innerWidth = columnWidth - cardPadding
    val tileSize = (innerWidth - 4.dp) / 2

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AlbumImageSlot(url = urls.getOrNull(0), size = tileSize)
            AlbumImageSlot(url = urls.getOrNull(1), size = tileSize)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AlbumImageSlot(url = urls.getOrNull(2), size = tileSize)
            AlbumImageSlot(url = urls.getOrNull(3), size = tileSize)
        }
    }
}

@Composable
private fun AlbumImageSlot(
    url: String?,
    size: Dp
) {
    if (url.isNullOrBlank()) {
        Spacer(modifier = Modifier.size(size))
    } else {
        AsyncImage(
            model = url,
            contentDescription = "Album image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

private fun buildSchoolLine(
    school: String,
    graduationYear: String
): String {
    val schoolText = school.trim()
    val yearText = graduationYear.trim()

    if (schoolText.isBlank() && yearText.isBlank()) return ""
    if (schoolText.isNotBlank() && yearText.isBlank()) return schoolText
    if (schoolText.isBlank() && yearText.isNotBlank()) return yearText

    val shortenedYear = if (yearText.length == 4) {
        yearText.takeLast(2)
    } else {
        yearText
    }

    return "$schoolText '$shortenedYear"
}