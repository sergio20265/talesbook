package ru.norahobbits.talesbook.ui.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun shareTextFile(
    context: Context,
    fileName: String,
    mimeType: String,
    content: String,
    chooserTitle: String
) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeText(content, Charsets.UTF_8)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

fun shareBinaryFile(
    context: Context,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
    chooserTitle: String
) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeBytes(bytes)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

fun safeFileName(value: String): String {
    return value
        .trim()
        .ifBlank { "book-of-tales" }
        .replace(Regex("[\\\\/:*?\"<>|]+"), "-")
        .take(80)
}
