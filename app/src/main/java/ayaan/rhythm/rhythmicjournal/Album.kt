package ayaan.rhythm.rhythmicjournal

data class Album(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val normalizedTitle: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class AlbumPreview(
    val album: Album = Album(),
    val postCount: Int = 0,
    val coverUrls: List<String> = emptyList()
)