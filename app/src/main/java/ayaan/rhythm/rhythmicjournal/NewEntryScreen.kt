package ayaan.rhythm.rhythmicjournal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    journalId: String? = null,
    onCancel: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val journalRepository = remember { JournalRepository() }
    val albumRepository = remember { AlbumRepository() }
    val scope = rememberCoroutineScope()

    val isEditMode = !journalId.isNullOrBlank()
    val defaultDate = remember {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
    }

    var title by rememberSaveable { mutableStateOf("") }
    var reflection by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var entryDateText by rememberSaveable { mutableStateOf(defaultDate) }

    var selectedAlbumId by rememberSaveable { mutableStateOf("") }
    var selectedAlbumTitle by rememberSaveable { mutableStateOf("") }

    var photoUrl by rememberSaveable { mutableStateOf("") }
    var photoStoragePath by rememberSaveable { mutableStateOf("") }
    var imageSizeBytes by rememberSaveable { mutableStateOf(0L) }
    var selectedPhotoUri by rememberSaveable { mutableStateOf("") }

    var isInitialLoading by remember { mutableStateOf(isEditMode) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    var showAlbumSheet by rememberSaveable { mutableStateOf(false) }
    var createAlbumMode by rememberSaveable { mutableStateOf(false) }
    var newAlbumTitle by rememberSaveable { mutableStateOf("") }
    var isCreatingAlbum by remember { mutableStateOf(false) }
    val albums = remember { mutableStateListOf<Album>() }

    val imagePicker = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            selectedPhotoUri = uri.toString()
        }
    }

    fun refreshAlbums() {
        scope.launch {
            runCatching {
                val loaded = albumRepository.getAlbums()
                albums.clear()
                albums.addAll(loaded)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshAlbums()
    }

    LaunchedEffect(journalId) {
        if (!journalId.isNullOrBlank()) {
            isInitialLoading = true
            saveError = null

            try {
                val existingJournal = journalRepository.getJournalById(journalId)
                if (existingJournal == null) {
                    saveError = "Journal not found."
                } else {
                    title = existingJournal.title
                    reflection = existingJournal.reflection
                    location = existingJournal.locationName
                    mood = existingJournal.mood
                    tags = existingJournal.tags.joinToString(", ")
                    entryDateText = existingJournal.entryDateText.ifBlank { defaultDate }
                    selectedAlbumId = existingJournal.albumId
                    selectedAlbumTitle = existingJournal.album
                    photoUrl = existingJournal.photoUrl
                    photoStoragePath = existingJournal.photoStoragePath
                    imageSizeBytes = existingJournal.imageSizeBytes
                    selectedPhotoUri = ""
                }
            } catch (e: Exception) {
                saveError = e.localizedMessage ?: "Could not load journal."
            } finally {
                isInitialLoading = false
            }
        }
    }

    fun saveEntry() {
        if (isSaving) return

        val cleanedTitle = title.trim()
        val cleanedReflection = reflection.trim()
        val cleanedLocation = location.trim()
        val cleanedMood = mood.trim()

        val tagList = tags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cleanedTitle.isBlank()) {
            saveError = "Please enter a title."
            return
        }

        if (cleanedReflection.isBlank()) {
            saveError = "Please enter a reflection."
            return
        }

        scope.launch {
            isSaving = true
            saveError = null

            try {
                var finalPhotoUrl = photoUrl
                var finalPhotoStoragePath = photoStoragePath
                var finalImageSizeBytes = imageSizeBytes

                if (selectedPhotoUri.isNotBlank()) {
                    val uploadedPhoto = journalRepository.uploadJournalPhoto(Uri.parse(selectedPhotoUri))
                    finalPhotoUrl = uploadedPhoto.downloadUrl
                    finalPhotoStoragePath = uploadedPhoto.storagePath
                    finalImageSizeBytes = uploadedPhoto.fileSizeBytes
                }

                var finalAlbumId = selectedAlbumId.trim()
                var finalAlbumTitle = selectedAlbumTitle.trim()

                if (finalAlbumTitle.isNotBlank()) {
                    val album = albumRepository.getOrCreateAlbum(finalAlbumTitle)
                    finalAlbumId = album.id
                    finalAlbumTitle = album.title
                }

                if (isEditMode && !journalId.isNullOrBlank()) {
                    journalRepository.updateJournal(
                        journalId = journalId,
                        title = cleanedTitle,
                        reflection = cleanedReflection,
                        photoUrl = finalPhotoUrl,
                        photoStoragePath = finalPhotoStoragePath,
                        imageSizeBytes = finalImageSizeBytes,
                        entryDateText = entryDateText,
                        locationName = cleanedLocation,
                        mood = cleanedMood,
                        tags = tagList,
                        albumId = finalAlbumId,
                        album = finalAlbumTitle
                    )
                } else {
                    journalRepository.saveJournal(
                        title = cleanedTitle,
                        reflection = cleanedReflection,
                        photoUrl = finalPhotoUrl,
                        photoStoragePath = finalPhotoStoragePath,
                        imageSizeBytes = finalImageSizeBytes,
                        entryDateText = entryDateText,
                        locationName = cleanedLocation,
                        mood = cleanedMood,
                        tags = tagList,
                        albumId = finalAlbumId,
                        album = finalAlbumTitle,
                        latitude = null,
                        longitude = null,
                        isFavorite = false,
                        isLoved = false,
                        isDraft = false
                    )
                }

                onSaveSuccess()
            } catch (e: Exception) {
                saveError = e.localizedMessage ?: "Could not save journal."
            } finally {
                isSaving = false
            }
        }
    }

    val displayedPhotoModel = when {
        selectedPhotoUri.isNotBlank() -> selectedPhotoUri
        photoUrl.isNotBlank() -> photoUrl
        else -> ""
    }

    if (showAlbumSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                showAlbumSheet = false
                createAlbumMode = false
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
                    text = "Select album",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(14.dp))

                AlbumSheetRow(
                    title = "No album",
                    selected = selectedAlbumId.isBlank() && selectedAlbumTitle.isBlank(),
                    onClick = {
                        selectedAlbumId = ""
                        selectedAlbumTitle = ""
                        showAlbumSheet = false
                    }
                )

                albums.forEach { album ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AlbumSheetRow(
                        title = album.title,
                        selected = selectedAlbumId == album.id,
                        onClick = {
                            selectedAlbumId = album.id
                            selectedAlbumTitle = album.title
                            showAlbumSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { createAlbumMode = true },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Create New Album",
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }

                if (createAlbumMode) {
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
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                createAlbumMode = false
                                newAlbumTitle = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (newAlbumTitle.trim().isNotBlank() && !isCreatingAlbum) {
                                    scope.launch {
                                        isCreatingAlbum = true
                                        try {
                                            val album = albumRepository.getOrCreateAlbum(newAlbumTitle)
                                            selectedAlbumId = album.id
                                            selectedAlbumTitle = album.title
                                            refreshAlbums()
                                            showAlbumSheet = false
                                            createAlbumMode = false
                                            newAlbumTitle = ""
                                        } catch (e: Exception) {
                                            saveError = e.localizedMessage ?: "Could not create album."
                                        } finally {
                                            isCreatingAlbum = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(if (isCreatingAlbum) "Creating..." else "Create")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAlbumSheet = false
                            createAlbumMode = false
                            newAlbumTitle = ""
                        }
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    ScreenContainer {
        SimpleHeader(
            leftText = "Cancel",
            title = if (isEditMode) "Edit Entry" else "New Entry",
            rightText = if (isSaving) "Saving..." else "Save",
            onLeftClick = onCancel,
            onRightClick = { saveEntry() }
        )

        Spacer(modifier = Modifier.height(18.dp))

        when {
            isInitialLoading -> {
                SoftCard {
                    Text(
                        text = "Loading journal...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            else -> {
                if (displayedPhotoModel.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TAP GALLERY OR FILES TO ADD PHOTO",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AsyncImage(
                        model = displayedPhotoModel,
                        contentDescription = "Selected journal photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(26.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallActionButton(
                        text = "Gallery",
                        modifier = Modifier.weight(1f),
                        onClick = { imagePicker.launch(arrayOf("image/*")) }
                    )
                    SmallActionButton(
                        text = "Files",
                        modifier = Modifier.weight(1f),
                        onClick = { imagePicker.launch(arrayOf("image/*")) }
                    )
                    SmallActionButton(
                        text = "Clear",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPhotoUri = ""
                            photoUrl = ""
                            photoStoragePath = ""
                            imageSizeBytes = 0L
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                LabeledField(
                    label = "Title",
                    value = title,
                    onValueChange = { title = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                LabeledField(
                    label = "Reflection",
                    value = reflection,
                    onValueChange = { reflection = it },
                    singleLine = false,
                    minLines = 4
                )

                Spacer(modifier = Modifier.height(14.dp))

                StaticValueCard(
                    label = "Date",
                    value = entryDateText
                )

                Spacer(modifier = Modifier.height(14.dp))

                LabeledField(
                    label = "Location",
                    value = location,
                    onValueChange = { location = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                LabeledField(
                    label = "Mood",
                    value = mood,
                    onValueChange = { mood = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                LabeledField(
                    label = "Tags",
                    value = tags,
                    onValueChange = { tags = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                AlbumSelectionCard(
                    label = "Album",
                    value = selectedAlbumTitle.ifBlank { "Choose album" },
                    onClick = {
                        refreshAlbums()
                        showAlbumSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Save draft")
                    }

                    Button(
                        onClick = { saveEntry() },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            if (isSaving) {
                                "Saving..."
                            } else if (isEditMode) {
                                "Update entry"
                            } else {
                                "Publish entry"
                            }
                        )
                    }
                }

                saveError?.let { error ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumSelectionCard(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AlbumSheetRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}