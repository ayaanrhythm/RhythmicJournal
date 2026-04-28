package ayaan.rhythm.rhythmicjournal

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

class AlbumRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getAlbums(): List<Album> {
        val uid = currentUid()

        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("albums")
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(Album::class.java)?.copy(id = document.id)
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun getAlbum(albumId: String): Album? {
        val uid = currentUid()

        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("albums")
            .document(albumId)
            .get()
            .await()

        if (!snapshot.exists()) return null
        return snapshot.toObject(Album::class.java)?.copy(id = snapshot.id)
    }

    suspend fun getOrCreateAlbum(rawTitle: String): Album {
        val uid = currentUid()
        val title = rawTitle.trim()

        if (title.isBlank()) {
            throw IllegalArgumentException("Album title cannot be empty.")
        }

        val normalized = normalizeTitle(title)

        val existing = firestore
            .collection("users")
            .document(uid)
            .collection("albums")
            .whereEqualTo("normalizedTitle", normalized)
            .get()
            .await()
            .documents
            .firstOrNull()

        if (existing != null) {
            return existing.toObject(Album::class.java)?.copy(id = existing.id)
                ?: Album(
                    id = existing.id,
                    userId = uid,
                    title = title,
                    normalizedTitle = normalized
                )
        }

        val now = System.currentTimeMillis()
        val docRef = firestore
            .collection("users")
            .document(uid)
            .collection("albums")
            .document()

        val album = Album(
            id = docRef.id,
            userId = uid,
            title = title,
            normalizedTitle = normalized,
            createdAt = now,
            updatedAt = now
        )

        docRef.set(album).await()
        return album
    }

    suspend fun getAlbumPosts(albumId: String): List<JournalEntry> {
        val uid = currentUid()

        val snapshot = firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(JournalEntry::class.java)?.copy(id = document.id)
        }
            .filter { it.albumId == albumId }
            .sortedByDescending { it.createdAt }
    }

    suspend fun getAlbumPreviews(): List<AlbumPreview> {
        val albums = getAlbums()
        if (albums.isEmpty()) return emptyList()

        val uid = currentUid()
        val journalSnapshot = firestore
            .collection("users")
            .document(uid)
            .collection("journals")
            .get()
            .await()

        val journals = journalSnapshot.documents.mapNotNull { document ->
            document.toObject(JournalEntry::class.java)?.copy(id = document.id)
        }

        val grouped = journals.groupBy { it.albumId }

        return albums.map { album ->
            val posts = grouped[album.id]
                .orEmpty()
                .sortedByDescending { it.createdAt }

            AlbumPreview(
                album = album,
                postCount = posts.size,
                coverUrls = posts
                    .mapNotNull { it.photoUrl.takeIf(String::isNotBlank) }
                    .take(4)
            )
        }.sortedByDescending { preview ->
            val latestPostTime = grouped[preview.album.id]
                .orEmpty()
                .maxOfOrNull { it.createdAt }
                ?: 0L

            maxOf(preview.album.updatedAt, latestPostTime)
        }
    }

    private fun normalizeTitle(title: String): String {
        return title.trim().lowercase(Locale.getDefault())
    }

    private fun currentUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("No signed-in user found.")
    }
}