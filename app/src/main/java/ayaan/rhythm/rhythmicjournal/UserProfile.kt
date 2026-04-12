package ayaan.rhythm.rhythmicjournal

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val about: String = "",
    val school: String = "",
    val graduationYear: String = "",
    val email: String = "",
    val birthday: String = "",
    val profileImageUrl: String = "",
    val profileImageStoragePath: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)