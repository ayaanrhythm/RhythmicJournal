package ayaan.rhythm.rhythmicjournal

data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val reflection: String = "",
    val photoUrl: String = "",
    val photoStoragePath: String = "",
    val imageSizeBytes: Long = 0L,
    val entryDateText: String = "",
    val locationName: String = "",
    val mood: String = "",
    val tags: List<String> = emptyList(),
    val albumId: String = "",
    val album: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isFavorite: Boolean = false,
    val isLoved: Boolean = false,
    val isDraft: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)