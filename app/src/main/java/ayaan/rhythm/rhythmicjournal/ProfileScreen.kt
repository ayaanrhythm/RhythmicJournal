package ayaan.rhythm.rhythmicjournal

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

private const val PROFILE_TAB_RECENT = "RECENT"
private const val PROFILE_TAB_ALBUMS = "ALBUMS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenDrawer: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository() }
    val journalRepository = remember { JournalRepository() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var journals by remember { mutableStateOf<List<JournalEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(PROFILE_TAB_RECENT) }
    var showMenu by rememberSaveable { mutableStateOf(false) }

    fun loadProfileData() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                profile = profileRepository.getOrCreateCurrentUserProfile()
                journals = journalRepository.getCurrentUserJournals()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not load profile."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProfileData()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadProfileData()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                ProfileMenuItem("Edit Profile") {
                    showMenu = false
                    onOpenEditProfile()
                }

                ProfileMenuItem("Share Profile") {
                    val currentProfile = profile
                    val shareText = buildString {
                        append("Check out ")
                        append(currentProfile?.name?.ifBlank { "my" } ?: "my")
                        append(" profile on RhythmicJournal")
                        currentProfile?.username?.takeIf { it.isNotBlank() }?.let {
                            append(" (@")
                            append(it)
                            append(")")
                        }
                    }

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }

                    context.startActivity(Intent.createChooser(shareIntent, "Share profile"))
                    showMenu = false
                }

                ProfileMenuItem("Create New Album") {
                    selectedTab = PROFILE_TAB_ALBUMS
                    showMenu = false
                }

                Spacer(modifier = Modifier.height(12.dp))

                ProfileMenuItem("Cancel") {
                    showMenu = false
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    ScreenContainer(
        backgroundColor = MaterialTheme.colorScheme.background,
        horizontalPadding = 20.dp,
        verticalPadding = 18.dp
    ) {
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile?.username?.ifBlank { "profile" } ?: "profile",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "⋯",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { showMenu = true }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        when {
            isLoading -> {
                SoftCard {
                    Text(
                        text = "Loading profile...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            errorMessage != null -> {
                SoftCard {
                    Text(
                        text = errorMessage ?: "Could not load profile.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            profile != null -> {
                val currentProfile = profile!!
                val albums = journals
                    .filter { it.album.isNotBlank() }
                    .groupBy { it.album.trim() }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    ProfileAvatarLarge(
                        imageUrl = currentProfile.profileImageUrl,
                        name = currentProfile.name
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currentProfile.name.ifBlank { "Your Name" },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentProfile.about.isNotBlank()) {
                    Text(
                        text = currentProfile.about,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (currentProfile.school.isNotBlank()) {
                    Text(
                        text = buildSchoolLine(currentProfile),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val buttonWidth = maxWidth / 2

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        SmallProfilePillButton(
                            text = "MY GALLERIES",
                            filled = true,
                            width = buttonWidth,
                            onClick = { selectedTab = PROFILE_TAB_RECENT }
                        )

                        SmallProfilePillButton(
                            text = "EDIT PROFILE",
                            filled = false,
                            width = buttonWidth,
                            onClick = onOpenEditProfile
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    SlimProfileTab(
                        text = "RECENT",
                        selected = selectedTab == PROFILE_TAB_RECENT,
                        onClick = { selectedTab = PROFILE_TAB_RECENT }
                    )

                    SlimProfileTab(
                        text = "ALBUMS",
                        selected = selectedTab == PROFILE_TAB_ALBUMS,
                        onClick = { selectedTab = PROFILE_TAB_ALBUMS }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (selectedTab == PROFILE_TAB_RECENT) {
                    if (journals.isEmpty()) {
                        Text(
                            text = "No posts yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val gap = 10.dp
                            val tileWidth = (maxWidth - gap) / 2

                            journals.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(gap)
                                ) {
                                    rowItems.forEach { journal ->
                                        RecentPhotoTile(
                                            journal = journal,
                                            tileWidth = tileWidth,
                                            onClick = {
                                                if (journal.id.isNotBlank()) {
                                                    onOpenEntry(journal.id)
                                                }
                                            }
                                        )
                                    }

                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.width(tileWidth))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                } else {
                    if (albums.isEmpty()) {
                        Text(
                            text = "No albums yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val gap = 10.dp
                            val tileWidth = (maxWidth - gap) / 2

                            albums.entries.toList().chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(gap)
                                ) {
                                    rowItems.forEach { entry ->
                                        AlbumTile(
                                            albumName = entry.key,
                                            entries = entry.value,
                                            tileWidth = tileWidth
                                        )
                                    }

                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.width(tileWidth))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatarLarge(
    imageUrl: String,
    name: String
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Text(
                text = profileInitials(name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmallProfilePillButton(
    text: String,
    filled: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (filled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background,
        border = if (filled) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (filled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SlimProfileTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .width(42.dp)
                .height(2.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.onBackground
                    else androidx.compose.ui.graphics.Color.Transparent
                )
        )
    }
}

@Composable
private fun RecentPhotoTile(
    journal: JournalEntry,
    tileWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(tileWidth)
            .clickable(onClick = onClick)
    ) {
        if (journal.photoUrl.isNotBlank()) {
            AsyncImage(
                model = journal.photoUrl,
                contentDescription = "Journal photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
            )
        } else {
            MiniPhoto(
                text = "PHOTO",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
            )
        }
    }
}

@Composable
private fun AlbumTile(
    albumName: String,
    entries: List<JournalEntry>,
    tileWidth: androidx.compose.ui.unit.Dp
) {
    val coverPhoto = entries.firstOrNull { it.photoUrl.isNotBlank() }?.photoUrl.orEmpty()

    Column(
        modifier = Modifier.width(tileWidth)
    ) {
        if (coverPhoto.isNotBlank()) {
            AsyncImage(
                model = coverPhoto,
                contentDescription = "Album cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
            )
        } else {
            MiniPhoto(
                text = "ALBUM",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = albumName,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "${entries.size} posts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileMenuItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
    )
}

private fun profileInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
    if (parts.isEmpty()) return "RJ"
    return parts.joinToString("") { it.first().uppercase() }
}

private fun buildSchoolLine(profile: UserProfile): String {
    val gradYear = profile.graduationYear.trim()
    val suffix = when {
        gradYear.length >= 2 -> " '${gradYear.takeLast(2)}"
        gradYear.isNotBlank() -> " '$gradYear"
        else -> ""
    }
    return "🎓 ${profile.school}$suffix"
}