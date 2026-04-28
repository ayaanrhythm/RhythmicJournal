package ayaan.rhythm.rhythmicjournal

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun getOrCreateCurrentUserProfile(): UserProfile {
        val user = auth.currentUser ?: return UserProfile()
        val uid = user.uid
        val docRef = firestore.collection("users").document(uid)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            val patch = mutableMapOf<String, Any>()

            if (snapshot.getString("uid").isNullOrBlank()) {
                patch["uid"] = uid
            }
            if (snapshot.getString("name").isNullOrBlank() && !user.displayName.isNullOrBlank()) {
                patch["name"] = user.displayName!!
            }
            if (snapshot.getString("email").isNullOrBlank() && !user.email.isNullOrBlank()) {
                patch["email"] = user.email!!
            }
            if (snapshot.getString("username").isNullOrBlank()) {
                patch["username"] = defaultUsername(
                    displayName = user.displayName.orEmpty(),
                    email = user.email.orEmpty()
                )
            }
            if (snapshot.getString("graduationYear") == null) {
                patch["graduationYear"] = ""
            }

            if (patch.isNotEmpty()) {
                docRef.set(patch, SetOptions.merge()).await()
            }

            return docRef.get().await().toObject(UserProfile::class.java) ?: UserProfile()
        }

        val baseProfile = mapOf(
            "uid" to uid,
            "name" to user.displayName.orEmpty(),
            "username" to defaultUsername(
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty()
            ),
            "about" to "",
            "school" to "",
            "graduationYear" to "",
            "email" to user.email.orEmpty(),
            "birthday" to "",
            "profileImageUrl" to "",
            "profileImageStoragePath" to ""
        )

        docRef.set(baseProfile, SetOptions.merge()).await()
        return docRef.get().await().toObject(UserProfile::class.java) ?: UserProfile()
    }

    suspend fun getProfileByUid(uid: String): UserProfile? {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
    }

    suspend fun updateProfile(
        name: String,
        username: String,
        about: String,
        school: String,
        graduationYear: String,
        email: String,
        birthday: String,
        profileImageUrl: String? = null
    ): UserProfile {
        val currentUser = auth.currentUser ?: error("No signed in user")
        val uid = currentUser.uid

        val updates = mutableMapOf<String, Any>(
            "uid" to uid,
            "name" to name.trim(),
            "username" to username.trim(),
            "about" to about.trim(),
            "school" to school.trim(),
            "graduationYear" to graduationYear.trim(),
            "email" to email.trim(),
            "birthday" to birthday.trim()
        )

        if (profileImageUrl != null) {
            updates["profileImageUrl"] = profileImageUrl
        }

        firestore.collection("users")
            .document(uid)
            .set(updates, SetOptions.merge())
            .await()

        return getOrCreateCurrentUserProfile()
    }

    suspend fun uploadProfileImage(localPhotoUri: Uri): String {
        val currentUser = auth.currentUser ?: error("No signed in user")
        val uid = currentUser.uid

        val profileDoc = firestore.collection("users").document(uid).get().await()
        val oldStoragePath = profileDoc.getString("profileImageStoragePath").orEmpty()

        val storagePath = "users/$uid/profile/profile_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val storageRef = storage.reference.child(storagePath)

        storageRef.putFile(localPhotoUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()

        firestore.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "profileImageUrl" to downloadUrl,
                    "profileImageStoragePath" to storagePath
                ),
                SetOptions.merge()
            )
            .await()

        if (oldStoragePath.isNotBlank() && oldStoragePath != storagePath) {
            runCatching {
                storage.reference.child(oldStoragePath).delete().await()
            }
        }

        return downloadUrl
    }

    private fun defaultUsername(displayName: String, email: String): String {
        val source = if (displayName.isNotBlank()) displayName else email.substringBefore("@")
        val cleaned = source
            .trim()
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9._]".toRegex(), "")

        return cleaned.ifBlank { "user" }
    }
}