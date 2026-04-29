package ayaan.rhythm.rhythmicjournal

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Locale

suspend fun shareJournalPost(
    context: Context,
    post: JournalEntry,
    chooserTitle: String = "Share post"
) {
    val shareText = buildJournalShareText(post)
    val imageUri = resolveShareImageUri(context, post.photoUrl)

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_SUBJECT, post.title.ifBlank { "RhythmicJournal post" })
        putExtra(Intent.EXTRA_TEXT, shareText)

        if (imageUri != null) {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            clipData = ClipData.newUri(
                context.contentResolver,
                "shared_post_image",
                imageUri
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
    }

    val chooserIntent = Intent.createChooser(shareIntent, chooserTitle)
    if (context !is Activity) {
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(chooserIntent)
}

private fun buildJournalShareText(post: JournalEntry): String {
    return buildString {
        append(post.title.ifBlank { "RhythmicJournal post" })

        if (post.reflection.isNotBlank()) {
            append("\n\n")
            append(post.reflection.trim())
        }

        if (post.tags.isNotEmpty()) {
            append("\n\n")
            append(post.tags.joinToString(" ") { "#$it" })
        }

        if (post.locationName.isNotBlank()) {
            append("\n\nLocation: ")
            append(post.locationName)
        }

        if (post.album.isNotBlank()) {
            append("\nAlbum: ")
            append(post.album)
        }

        if (post.entryDateText.isNotBlank()) {
            append("\nDate: ")
            append(post.entryDateText)
        }
    }
}

private suspend fun resolveShareImageUri(
    context: Context,
    photoUrl: String
): Uri? = withContext(Dispatchers.IO) {
    if (photoUrl.isBlank()) return@withContext null

    when {
        photoUrl.startsWith("content://", ignoreCase = true) -> {
            Uri.parse(photoUrl)
        }

        photoUrl.startsWith("file://", ignoreCase = true) -> {
            val filePath = Uri.parse(photoUrl).path ?: return@withContext null
            val file = File(filePath)
            if (file.exists()) fileToContentUri(context, file) else null
        }

        photoUrl.startsWith("http://", ignoreCase = true) ||
                photoUrl.startsWith("https://", ignoreCase = true) -> {
            downloadRemoteImageToCache(context, photoUrl)
        }

        else -> {
            val file = File(photoUrl)
            if (file.exists()) fileToContentUri(context, file) else null
        }
    }
}

private fun fileToContentUri(context: Context, file: File): Uri {
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun downloadRemoteImageToCache(context: Context, imageUrl: String): Uri? {
    val connection = URL(imageUrl).openConnection()
    connection.connect()

    val extension = inferExtension(
        url = imageUrl,
        contentType = connection.contentType
    )

    val shareDir = File(context.cacheDir, "shared_posts").apply { mkdirs() }
    val outFile = File(shareDir, "post_${System.currentTimeMillis()}.$extension")

    connection.getInputStream().use { input ->
        FileOutputStream(outFile).use { output ->
            input.copyTo(output)
        }
    }

    return fileToContentUri(context, outFile)
}

private fun inferExtension(url: String, contentType: String?): String {
    val cleanUrl = url.substringBefore("?").lowercase(Locale.getDefault())

    return when {
        cleanUrl.endsWith(".png") -> "png"
        cleanUrl.endsWith(".webp") -> "webp"
        cleanUrl.endsWith(".jpeg") -> "jpeg"
        cleanUrl.endsWith(".jpg") -> "jpg"
        cleanUrl.endsWith(".heic") -> "heic"
        cleanUrl.endsWith(".heif") -> "heif"
        contentType?.contains("png", ignoreCase = true) == true -> "png"
        contentType?.contains("webp", ignoreCase = true) == true -> "webp"
        else -> "jpg"
    }
}