package ayaan.rhythm.rhythmicjournal

data class JournalComment(
    val id: String = "",
    val journalId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)