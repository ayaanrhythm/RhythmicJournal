package ayaan.rhythm.rhythmicjournal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NewEntryScreen(
    journalId: String? = null,
    onCancel: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val journalRepository = remember { JournalRepository() }
    val scope = rememberCoroutineScope()

    val isEditMode = !journalId.isNullOrBlank()
    val defaultDate = remember {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
    }

    var title by rememberSaveable { mutableStateOf("") }
    var reflection by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var entryDateText by rememberSaveable { mutableStateOf(defaultDate) }

    var photoUrl by rememberSaveable { mutableStateOf("") }
    var photoStoragePath by rememberSaveable { mutableStateOf("") }
    var selectedPhotoUri by rememberSaveable { mutableStateOf("") }

    var isInitialLoading by remember { mutableStateOf(isEditMode) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

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
                    album = existingJournal.album
                    tags = existingJournal.tags.joinToString(", ")
                    entryDateText = existingJournal.entryDateText.ifBlank { defaultDate }
                    photoUrl = existingJournal.photoUrl
                    photoStoragePath = existingJournal.photoStoragePath
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
        val cleanedAlbum = album.trim()

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

                if (selectedPhotoUri.isNotBlank()) {
                    val uploadedPhoto = journalRepository.uploadJournalPhoto(Uri.parse(selectedPhotoUri))
                    finalPhotoUrl = uploadedPhoto.downloadUrl
                    finalPhotoStoragePath = uploadedPhoto.storagePath
                }

                if (isEditMode && !journalId.isNullOrBlank()) {
                    journalRepository.updateJournal(
                        journalId = journalId,
                        title = cleanedTitle,
                        reflection = cleanedReflection,
                        photoUrl = finalPhotoUrl,
                        photoStoragePath = finalPhotoStoragePath,
                        entryDateText = entryDateText,
                        locationName = cleanedLocation,
                        mood = cleanedMood,
                        album = cleanedAlbum,
                        tags = tagList
                    )
                } else {
                    journalRepository.saveJournal(
                        title = cleanedTitle,
                        reflection = cleanedReflection,
                        photoUrl = finalPhotoUrl,
                        photoStoragePath = finalPhotoStoragePath,
                        entryDateText = entryDateText,
                        locationName = cleanedLocation,
                        mood = cleanedMood,
                        album = cleanedAlbum,
                        tags = tagList,
                        latitude = null,
                        longitude = null,
                        isFavorite = false,
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
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
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
                                androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
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
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
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
                    label = "Album",
                    value = album,
                    onValueChange = { album = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                LabeledField(
                    label = "Tags",
                    value = tags,
                    onValueChange = { tags = it }
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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