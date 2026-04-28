package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val albumRepository = remember { AlbumRepository() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val contentWidth = screenWidth - 40.dp
    val cardGap = 12.dp
    val cardWidth = (contentWidth - cardGap) / 2

    val albums = remember { mutableStateListOf<AlbumPreview>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    var newAlbumTitle by rememberSaveable { mutableStateOf("") }
    var isCreatingAlbum by remember { mutableStateOf(false) }

    fun loadAlbums() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val loadedAlbums = albumRepository.getAlbumPreviews()
                albums.clear()
                albums.addAll(loadedAlbums)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not load albums."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAlbums()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadAlbums()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                showCreateSheet = false
                newAlbumTitle = ""
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Create New Album",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newAlbumTitle,
                    onValueChange = { newAlbumTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Album name") },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .width(160.dp)
                            .clickable {
                                showCreateSheet = false
                                newAlbumTitle = ""
                            },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = "Cancel",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            val trimmedTitle = newAlbumTitle.trim()
                            if (trimmedTitle.isNotBlank() && !isCreatingAlbum) {
                                scope.launch {
                                    isCreatingAlbum = true
                                    try {
                                        val createdAlbum = albumRepository.getOrCreateAlbum(trimmedTitle)
                                        loadAlbums()
                                        showCreateSheet = false
                                        newAlbumTitle = ""
                                        onOpenAlbum(createdAlbum.id)
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage ?: "Could not create album."
                                    } finally {
                                        isCreatingAlbum = false
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text(if (isCreatingAlbum) "Creating..." else "Create")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    ScreenContainer {
        AppScreenHeader(
            title = "Albums",
            onBack = onBack,
            trailingContent = {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable { showCreateSheet = true }
                )
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        when {
            isLoading -> {
                Text(
                    text = "Loading albums...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Could not load albums.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            albums.isEmpty() -> {
                Text(
                    text = "No albums yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                albums.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(cardGap)
                    ) {
                        rowItems.forEach { preview ->
                            AlbumCard(
                                preview = preview,
                                cardWidth = cardWidth,
                                onClick = {
                                    if (preview.album.id.isNotBlank()) {
                                        onOpenAlbum(preview.album.id)
                                    }
                                }
                            )
                        }

                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.width(cardWidth))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCreateSheet = true },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = "Create new album",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AlbumCard(
    preview: AlbumPreview,
    cardWidth: Dp,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            AlbumCoverGrid(
                coverUrls = preview.coverUrls,
                coverWidth = cardWidth - 28.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = preview.album.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${preview.postCount} ${if (preview.postCount == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumCoverGrid(
    coverUrls: List<String>,
    coverWidth: Dp,
    modifier: Modifier = Modifier
) {
    val cells = coverUrls.take(4)
    val innerGap = 6.dp
    val cellSize = (coverWidth - innerGap - 12.dp) / 2

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        when {
            cells.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "COVER",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            cells.size == 1 -> {
                AsyncImage(
                    model = cells.first(),
                    contentDescription = "Album cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                val firstRow = cells.take(2)
                val secondRow = cells.drop(2).take(2)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(innerGap)
                ) {
                    AlbumCoverRow(firstRow, cellSize)
                    AlbumCoverRow(secondRow, cellSize)
                }
            }
        }
    }
}

@Composable
private fun AlbumCoverRow(
    urls: List<String>,
    cellSize: Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        urls.forEach { url ->
            AsyncImage(
                model = url,
                contentDescription = "Album cover image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(cellSize)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        repeat(2 - urls.size) {
            Box(
                modifier = Modifier
                    .width(cellSize)
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}