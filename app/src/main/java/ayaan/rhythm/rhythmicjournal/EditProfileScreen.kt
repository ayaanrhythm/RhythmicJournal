package ayaan.rhythm.rhythmicjournal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository() }
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var about by rememberSaveable { mutableStateOf("") }
    var school by rememberSaveable { mutableStateOf("") }
    var graduationYear by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var birthday by rememberSaveable { mutableStateOf("") }
    var profileImageUrl by rememberSaveable { mutableStateOf("") }
    var profileImageStoragePath by rememberSaveable { mutableStateOf("") }
    var selectedPhotoUri by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
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

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        try {
            val profile = profileRepository.getOrCreateCurrentUserProfile()
            name = profile.name
            username = profile.username
            about = profile.about
            school = profile.school
            graduationYear = profile.graduationYear
            email = profile.email
            birthday = profile.birthday
            profileImageUrl = profile.profileImageUrl
            profileImageStoragePath = profile.profileImageStoragePath
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Could not load profile."
        } finally {
            isLoading = false
        }
    }

    val displayedPhotoModel = when {
        selectedPhotoUri.isNotBlank() -> selectedPhotoUri
        profileImageUrl.isNotBlank() -> profileImageUrl
        else -> ""
    }

    fun saveProfile() {
        if (isSaving) return

        scope.launch {
            isSaving = true
            errorMessage = null

            try {
                profileRepository.saveProfile(
                    profile = UserProfile(
                        name = name,
                        username = username,
                        about = about,
                        school = school,
                        graduationYear = graduationYear,
                        email = email,
                        birthday = birthday,
                        profileImageUrl = profileImageUrl,
                        profileImageStoragePath = profileImageStoragePath
                    ),
                    newPhotoUriString = selectedPhotoUri.takeIf { it.isNotBlank() }
                )
                onClose()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Could not save profile."
            } finally {
                isSaving = false
            }
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Edit Profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(22.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(112.dp)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayedPhotoModel.isNotBlank()) {
                        AsyncImage(
                            model = displayedPhotoModel,
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

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { imagePicker.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        EditProfileField(
            label = "Name",
            value = name,
            onValueChange = { name = it },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        EditProfileField(
            label = "Username",
            value = username,
            onValueChange = { username = it },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        EditProfileField(
            label = "About",
            value = about,
            onValueChange = { about = it },
            singleLine = false
        )

        Spacer(modifier = Modifier.height(14.dp))

        EditProfileField(
            label = "School",
            value = school,
            onValueChange = { school = it },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        EditProfileField(
            label = "Graduation Year",
            value = graduationYear,
            onValueChange = { graduationYear = it },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        EditProfileField(
            label = "Email",
            value = email,
            onValueChange = { email = it },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        EditProfileField(
            label = "Birthday",
            value = birthday,
            onValueChange = { birthday = it },
            singleLine = true
        )

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Button(
                onClick = { saveProfile() },
                enabled = !isSaving && !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text(
                    text = if (isSaving) "Saving..." else "Save",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

private fun profileInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
    if (parts.isEmpty()) return "RJ"
    return parts.joinToString("") { it.first().uppercase() }
}