package ayaan.rhythm.rhythmicjournal

data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val reflection: String = "",
    val photoUrl: String = "",
    val photoStoragePath: String = "",
    val entryDateText: String = "",
    val locationName: String = "",
    val mood: String = "",
    val album: String = "",
    val tags: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isFavorite: Boolean = false,
    val isDraft: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)