package ayaan.rhythm.rhythmicjournal

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class UploadedPhoto(
    val downloadUrl: String,
    val storagePath: String
)

class JournalRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun uploadJournalPhoto(localPhotoUri: Uri): UploadedPhoto {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No signed-in user found.")

        val uid = currentUser.uid
        val storagePath = "users/$uid/journal_photos/${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val storageRef = storage.reference.child(storagePath)

        storageRef.putFile(localPhotoUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        return UploadedPhoto(
            downloadUrl = downloadUrl,
            storagePath = storagePath
        )
    }

    suspend fun saveJournal(
        title: String,
        reflection: String,
        photoUrl: String = "",
        photoStoragePath: String = "",
        entryDateText: String,
        locationName: String,
        mood: String,
        album: String,
        tags: List<String>,
        latitude: Double? = null,
        longitude: Double? = null,
        isFavorite: Boolean = false,
        isDraft: Boolean = false
    ): JournalEntry {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No signed-in user found.")

        val uid = currentUser.uid
        val journalDocRef = firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .document()

        val now = System.currentTimeMillis()

        val journalEntry = JournalEntry(
            id = journalDocRef.id,
            userId = uid,
            title = title,
            reflection = reflection,
            photoUrl = photoUrl,
            photoStoragePath = photoStoragePath,
            entryDateText = entryDateText,
            locationName = locationName,
            mood = mood,
            album = album,
            tags = tags,
            latitude = latitude,
            longitude = longitude,
            isFavorite = isFavorite,
            isDraft = isDraft,
            createdAt = now,
            updatedAt = now
        )

        journalDocRef.set(journalEntry).await()
        return journalEntry
    }

    suspend fun getCurrentUserJournals(): List<JournalEntry> {
        val currentUser = auth.currentUser ?: return emptyList()
        val uid = currentUser.uid

        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(JournalEntry::class.java)?.copy(id = document.id)
        }
    }

    suspend fun getFavoriteJournals(): List<JournalEntry> {
        val currentUser = auth.currentUser ?: return emptyList()
        val uid = currentUser.uid

        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .whereEqualTo("isFavorite", true)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(JournalEntry::class.java)?.copy(id = document.id)
        }.sortedByDescending { it.createdAt }
    }

    suspend fun getJournalById(journalId: String): JournalEntry? {
        val currentUser = auth.currentUser ?: return null
        val uid = currentUser.uid

        val document = firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .document(journalId)
            .get()
            .await()

        if (!document.exists()) return null

        return document.toObject(JournalEntry::class.java)?.copy(id = document.id)
    }

    suspend fun setFavorite(journalId: String, isFavorite: Boolean) {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No signed-in user found.")

        val uid = currentUser.uid

        firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .document(journalId)
            .update(
                mapOf(
                    "isFavorite" to isFavorite,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    suspend fun updateJournal(
        journalId: String,
        title: String,
        reflection: String,
        photoUrl: String = "",
        photoStoragePath: String = "",
        entryDateText: String,
        locationName: String,
        mood: String,
        album: String,
        tags: List<String>
    ): JournalEntry {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No signed-in user found.")

        val uid = currentUser.uid
        val journalDocRef = firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .document(journalId)

        val existing = journalDocRef.get().await().toObject(JournalEntry::class.java)
            ?: throw IllegalStateException("Journal not found.")

        val updatedJournal = existing.copy(
            id = journalId,
            userId = uid,
            title = title,
            reflection = reflection,
            photoUrl = photoUrl,
            photoStoragePath = photoStoragePath,
            entryDateText = entryDateText,
            locationName = locationName,
            mood = mood,
            album = album,
            tags = tags,
            updatedAt = System.currentTimeMillis()
        )

        journalDocRef.set(updatedJournal).await()

        if (
            existing.photoStoragePath.isNotBlank() &&
            existing.photoStoragePath != photoStoragePath
        ) {
            runCatching {
                storage.reference.child(existing.photoStoragePath).delete().await()
            }
        }

        return updatedJournal
    }

    suspend fun deleteJournal(journalId: String) {
        val currentUser = auth.currentUser
            ?: throw IllegalStateException("No signed-in user found.")

        val uid = currentUser.uid
        val journalDocRef = firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .document(journalId)

        val existing = journalDocRef.get().await().toObject(JournalEntry::class.java)

        if (existing?.photoStoragePath?.isNotBlank() == true) {
            runCatching {
                storage.reference.child(existing.photoStoragePath).delete().await()
            }
        }

        journalDocRef.delete().await()
    }
}