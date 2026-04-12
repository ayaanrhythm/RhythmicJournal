package ayaan.rhythm.rhythmicjournal

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

private data class UploadedProfilePhoto(
    val downloadUrl: String,
    val storagePath: String
)

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun getOrCreateCurrentUserProfile(): UserProfile {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No signed-in user found.")

        val uid = currentUser.uid
        val docRef = firestore.collection("users").document(uid)
        val snapshot = docRef.get().await()
        val now = System.currentTimeMillis()

        val googleEmail = currentUser.email.orEmpty().trim()
        val fallbackName = buildFallbackName(currentUser.displayName.orEmpty(), googleEmail)
        val fallbackUsername = buildFallbackUsername(googleEmail, uid)

        if (!snapshot.exists()) {
            val newProfile = UserProfile(
                uid = uid,
                name = fallbackName,
                username = fallbackUsername,
                about = "",
                school = "",
                graduationYear = "",
                email = googleEmail,
                birthday = "",
                profileImageUrl = "",
                profileImageStoragePath = "",
                createdAt = now,
                updatedAt = now
            )

            docRef.set(newProfile).await()
            return newProfile
        }

        val existing = snapshot.toObject(UserProfile::class.java)
            ?: UserProfile(uid = uid)

        val merged = existing.copy(
            uid = uid,
            name = existing.name.ifBlank { fallbackName },
            username = existing.username.ifBlank { fallbackUsername },
            email = existing.email.ifBlank { googleEmail },
            createdAt = if (existing.createdAt == 0L) now else existing.createdAt,
            updatedAt = if (
                existing.name.isBlank() ||
                existing.username.isBlank() ||
                existing.email.isBlank() ||
                existing.createdAt == 0L
            ) now else existing.updatedAt
        )

        if (merged != existing.copy(uid = uid)) {
            docRef.set(merged).await()
        }

        return merged
    }

    suspend fun saveProfile(
        profile: UserProfile,
        newPhotoUriString: String? = null
    ): UserProfile {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No signed-in user found.")

        val existing = getOrCreateCurrentUserProfile()
        val uid = currentUser.uid

        var finalPhotoUrl = existing.profileImageUrl
        var finalPhotoStoragePath = existing.profileImageStoragePath

        if (!newPhotoUriString.isNullOrBlank()) {
            val uploaded = uploadProfilePhoto(Uri.parse(newPhotoUriString))
            finalPhotoUrl = uploaded.downloadUrl
            finalPhotoStoragePath = uploaded.storagePath
        }

        val now = System.currentTimeMillis()

        val cleanedEmail = profile.email.trim().ifBlank { existing.email }
        val cleanedName = profile.name.trim().ifBlank {
            buildFallbackName(currentUser.displayName.orEmpty(), cleanedEmail)
        }
        val cleanedUsername = sanitizeUsername(
            profile.username.trim().ifBlank {
                buildFallbackUsername(cleanedEmail, uid)
            },
            uid = uid
        )

        val updated = existing.copy(
            uid = uid,
            name = cleanedName,
            username = cleanedUsername,
            about = profile.about.trim(),
            school = profile.school.trim(),
            graduationYear = profile.graduationYear.trim(),
            email = cleanedEmail,
            birthday = profile.birthday.trim(),
            profileImageUrl = finalPhotoUrl,
            profileImageStoragePath = finalPhotoStoragePath,
            createdAt = if (existing.createdAt == 0L) now else existing.createdAt,
            updatedAt = now
        )

        firestore.collection("users").document(uid).set(updated).await()

        if (
            !newPhotoUriString.isNullOrBlank() &&
            existing.profileImageStoragePath.isNotBlank() &&
            existing.profileImageStoragePath != finalPhotoStoragePath
        ) {
            runCatching {
                storage.reference.child(existing.profileImageStoragePath).delete().await()
            }
        }

        return updated
    }

    private suspend fun uploadProfilePhoto(localPhotoUri: Uri): UploadedProfilePhoto {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No signed-in user found.")

        val uid = currentUser.uid
        val storagePath = "users/$uid/profile/profile_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val storageRef = storage.reference.child(storagePath)

        storageRef.putFile(localPhotoUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        return UploadedProfilePhoto(
            downloadUrl = downloadUrl,
            storagePath = storagePath
        )
    }

    private fun buildFallbackName(displayName: String, email: String): String {
        if (displayName.isNotBlank()) return displayName.trim()

        val prefix = email.substringBefore("@", missingDelimiterValue = "Rhythmic User")
        return prefix
            .replace(".", " ")
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
            }
            .ifBlank { "Rhythmic User" }
    }

    private fun buildFallbackUsername(email: String, uid: String): String {
        val prefix = email.substringBefore("@", missingDelimiterValue = "")
        return sanitizeUsername(prefix, uid)
    }

    private fun sanitizeUsername(raw: String, uid: String): String {
        val cleaned = raw
            .lowercase(Locale.getDefault())
            .map { char ->
                when {
                    char.isLetterOrDigit() -> char
                    char == '_' || char == '.' -> char
                    else -> '_'
                }
            }
            .joinToString("")
            .replace(Regex("_+"), "_")
            .trim('_', '.')

        return if (cleaned.isBlank()) "user${uid.takeLast(6)}" else cleaned
    }
}