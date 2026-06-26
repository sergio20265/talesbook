package ru.norahobbits.talesbook.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import ru.norahobbits.talesbook.data.db.AppDatabase
import ru.norahobbits.talesbook.data.model.Book
import ru.norahobbits.talesbook.data.model.Chapter
import ru.norahobbits.talesbook.settings.AppSettingsDataStore
import ru.norahobbits.talesbook.settings.AppTheme
import ru.norahobbits.talesbook.settings.ContentViewMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val db: AppDatabase,
    private val settingsDataStore: AppSettingsDataStore
) {
    suspend fun exportJson(): String {
        val settings = settingsDataStore.settings.first()
        return JSONObject()
            .put("version", 1)
            .put("createdAt", System.currentTimeMillis())
            .put("settings", JSONObject()
                .put("selectedTheme", settings.selectedTheme.name)
                .put("fontSize", settings.fontSize)
                .put("fontFamily", settings.fontFamily)
                .put("textColor", settings.textColor)
                .put("accentColor", settings.accentColor)
                .put("editorBackgroundImageUri", settings.editorBackgroundImageUri)
                .put("appBackgroundImageUri", settings.appBackgroundImageUri)
                .put("bookViewMode", settings.bookViewMode.name)
                .put("chapterViewMode", settings.chapterViewMode.name)
                .put("musicEnabled", settings.musicEnabled)
                .put("selectedMusicUri", settings.selectedMusicUri)
                .put("musicVolume", settings.musicVolume)
            )
            .put("books", JSONArray(db.bookDao().getAll().map { it.toJson() }))
            .put("chapters", JSONArray(db.chapterDao().getAll().map { it.toJson() }))
            .toString(2)
    }

    suspend fun importJson(json: String) {
        val root = JSONObject(json)
        val books = root.getJSONArray("books").toBookList()
        val chapters = root.getJSONArray("chapters").toChapterList()

        db.withTransaction {
            db.chapterDao().deleteAll()
            db.bookDao().deleteAll()
            db.bookDao().insertAll(books)
            db.chapterDao().insertAll(chapters)
        }

        root.optJSONObject("settings")?.let { settings ->
            settings.optStringOrNull("selectedTheme")?.let {
                settingsDataStore.updateTheme(runCatching { AppTheme.valueOf(it) }.getOrDefault(AppTheme.EVENING_FOREST))
            }
            if (settings.has("fontSize")) settingsDataStore.updateFontSize(settings.optDouble("fontSize", 16.0).toFloat())
            settings.optStringOrNull("fontFamily")?.let { settingsDataStore.updateFontFamily(it) }
            if (settings.has("textColor")) settingsDataStore.updateTextColor(settings.optLong("textColor", 0xFFEFE8D8))
            if (settings.has("accentColor")) settingsDataStore.updateAccentColor(settings.optLong("accentColor", 0xFFB89B5E))
            settingsDataStore.updateEditorBgUri(settings.optStringOrNull("editorBackgroundImageUri"))
            settingsDataStore.updateAppBgUri(settings.optStringOrNull("appBackgroundImageUri"))
            settings.optStringOrNull("bookViewMode")?.let {
                settingsDataStore.updateBookViewMode(runCatching { ContentViewMode.valueOf(it) }.getOrDefault(ContentViewMode.LIST))
            }
            settings.optStringOrNull("chapterViewMode")?.let {
                settingsDataStore.updateChapterViewMode(runCatching { ContentViewMode.valueOf(it) }.getOrDefault(ContentViewMode.LIST))
            }
            if (settings.has("musicEnabled")) settingsDataStore.updateMusicEnabled(settings.optBoolean("musicEnabled", false))
            settingsDataStore.updateMusicUri(settings.optStringOrNull("selectedMusicUri"))
            if (settings.has("musicVolume")) settingsDataStore.updateMusicVolume(settings.optDouble("musicVolume", 0.5).toFloat())
        }
    }

    suspend fun exportBookHtml(bookId: Long): String {
        val book = db.bookDao().getById(bookId) ?: error("Книга не найдена")
        val chapters = db.chapterDao().getAll()
            .filter { it.bookId == bookId }
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
        return buildString {
            appendLine("<!doctype html>")
            appendLine("<html><head><meta charset=\"utf-8\">")
            appendLine("<title>${book.title.escapeHtml()}</title>")
            appendLine("<style>")
            appendLine("body{font-family:Georgia,'Times New Roman',serif;line-height:1.65;margin:48px;color:#1f1a14;}")
            appendLine("h1{font-size:32px;margin-bottom:8px;} h2{font-size:24px;margin-top:36px;}")
            appendLine(".description{color:#665b4d;margin-bottom:32px;} p{margin:0 0 14px;}")
            appendLine("</style></head><body>")
            appendLine("<h1>${book.title.escapeHtml()}</h1>")
            if (book.description.isNotBlank()) {
                appendLine("<div class=\"description\">${book.description.escapeHtml()}</div>")
            }
            chapters.forEach { chapter ->
                appendLine("<h2>${chapter.title.escapeHtml()}</h2>")
                appendLine(chapter.content.exportContentHtml())
            }
            appendLine("</body></html>")
        }
    }

    /**
     * Android's [android.text.Html] serializes alignment as `text-align:start|end`, which
     * Word and older HTML renderers don't honour. Map them to `left`/`right` for export.
     */
    private fun String.exportContentHtml(): String = ifBlank { "<p></p>" }
        .replace("text-align:start", "text-align:left")
        .replace("text-align:end", "text-align:right")

    /**
     * Builds a real `.docx` (OpenXML) package. Unlike HTML-with-a-.doc-extension — which
     * desktop Word tolerates but Word for Android refuses to open — this is a proper
     * WordprocessingML zip that opens in Word mobile, Google Docs, etc.
     */
    suspend fun exportBookDocx(bookId: Long): ByteArray {
        val book = db.bookDao().getById(bookId) ?: error("Книга не найдена")
        val chapters = db.chapterDao().getAll()
            .filter { it.bookId == bookId }
            .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))

        val body = StringBuilder()
        body.append(docxHeading(book.title, sizeHalfPt = 48, jc = "center"))
        if (book.description.isNotBlank()) {
            body.append(docxHeading(book.description, sizeHalfPt = 24, jc = "center", italic = true))
        }
        chapters.forEach { chapter ->
            body.append(docxHeading(chapter.title, sizeHalfPt = 36, jc = "left"))
            body.append(docxBodyParagraphs(chapter.content))
        }

        val documentXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
            "<w:body>$body" +
            "<w:sectPr><w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\"/></w:sectPr>" +
            "</w:body></w:document>"

        return buildDocxZip(documentXml)
    }

    private fun buildDocxZip(documentXml: String): ByteArray {
        val contentTypes =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
            "</Types>"
        val rels =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>" +
            "</Relationships>"

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes)
            put("_rels/.rels", rels)
            put("word/document.xml", documentXml)
        }
        return out.toByteArray()
    }

    private fun docxHeading(text: String, sizeHalfPt: Int, jc: String, italic: Boolean = false): String {
        val rpr = buildString {
            append("<w:rPr><w:b/>")
            if (italic) append("<w:i/>")
            append("<w:sz w:val=\"$sizeHalfPt\"/></w:rPr>")
        }
        return "<w:p><w:pPr><w:jc w:val=\"$jc\"/><w:spacing w:before=\"240\" w:after=\"120\"/>$rpr</w:pPr>" +
            "<w:r>$rpr<w:t xml:space=\"preserve\">${text.xmlEscape()}</w:t></w:r></w:p>"
    }

    private val blockRegex = Regex(
        "<(p|div|h[1-6]|blockquote)\\b([^>]*)>(.*?)</\\1>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    /** Converts a chapter's stored content HTML into WordprocessingML paragraphs. */
    private fun docxBodyParagraphs(content: String): String {
        if (content.isBlank()) return "<w:p/>"
        val blocks = blockRegex.findAll(content).toList()
        if (blocks.isEmpty()) {
            // Legacy plain text: one paragraph per line.
            return content.split("\n").joinToString("") { line ->
                docxParagraph(docxRunsFromInline(line.htmlUnescape().xmlEscape()), null)
            }
        }
        return blocks.joinToString("") { m ->
            val attrs = m.groupValues[2]
            val inner = m.groupValues[3]
            val jc = when {
                Regex("text-align\\s*:\\s*center", RegexOption.IGNORE_CASE).containsMatchIn(attrs) -> "center"
                Regex("text-align\\s*:\\s*(right|end)", RegexOption.IGNORE_CASE).containsMatchIn(attrs) -> "right"
                else -> null
            }
            docxParagraph(docxRunsFromInline(inner), jc)
        }
    }

    private fun docxParagraph(runs: String, jc: String?): String {
        val ppr = if (jc != null) "<w:pPr><w:jc w:val=\"$jc\"/></w:pPr>" else ""
        return "<w:p>$ppr$runs</w:p>"
    }

    /** Parses inline `<b>/<i>/<u>/<br>` and entities of one paragraph into Word runs. */
    private fun docxRunsFromInline(inner: String): String {
        val out = StringBuilder()
        val buf = StringBuilder()
        var bold = false
        var italic = false
        var underline = false
        fun flush() {
            if (buf.isEmpty()) return
            out.append("<w:r><w:rPr>")
            if (bold) out.append("<w:b/>")
            if (italic) out.append("<w:i/>")
            if (underline) out.append("<w:u w:val=\"single\"/>")
            out.append("</w:rPr><w:t xml:space=\"preserve\">").append(buf).append("</w:t></w:r>")
            buf.clear()
        }
        var i = 0
        while (i < inner.length) {
            val c = inner[i]
            if (c == '<') {
                val gt = inner.indexOf('>', i)
                if (gt < 0) break
                when (inner.substring(i + 1, gt).trim().lowercase().removeSuffix("/").trim()) {
                    "b", "strong" -> { flush(); bold = true }
                    "/b", "/strong" -> { flush(); bold = false }
                    "i", "em" -> { flush(); italic = true }
                    "/i", "/em" -> { flush(); italic = false }
                    "u" -> { flush(); underline = true }
                    "/u" -> { flush(); underline = false }
                    "br" -> { flush(); out.append("<w:r><w:br/></w:r>") }
                }
                i = gt + 1
            } else if (c == '&') {
                val semi = inner.indexOf(';', i)
                if (semi in (i + 1)..(i + 7)) {
                    buf.append(inner.substring(i, semi + 1).htmlUnescape().xmlEscape())
                    i = semi + 1
                } else {
                    buf.append("&amp;"); i++
                }
            } else {
                buf.append(c.toString().xmlEscape()); i++
            }
        }
        flush()
        return out.toString()
    }

    private fun String.xmlEscape(): String = buildString {
        for (c in this@xmlEscape) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            else -> append(c)
        }
    }

    private fun String.htmlUnescape(): String = this
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")

    private fun Book.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("description", description)
        .put("coverImageUri", coverImageUri)
        .put("backgroundImageUri", backgroundImageUri)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("sortOrder", sortOrder)

    private fun Chapter.toJson() = JSONObject()
        .put("id", id)
        .put("bookId", bookId)
        .put("title", title)
        .put("content", content)
        .put("createdAt", createdAt)
        .put("updatedAt", updatedAt)
        .put("sortOrder", sortOrder)
        .put("wordCount", wordCount)
        .put("charCountWithSpaces", charCountWithSpaces)
        .put("charCountWithoutSpaces", charCountWithoutSpaces)
        .put("backgroundImageUri", backgroundImageUri)

    private fun JSONArray.toBookList(): List<Book> = List(length()) { index ->
        getJSONObject(index).let {
            Book(
                id = it.getLong("id"),
                title = it.getString("title"),
                description = it.optString("description", ""),
                coverImageUri = it.optStringOrNull("coverImageUri"),
                backgroundImageUri = it.optStringOrNull("backgroundImageUri"),
                createdAt = it.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = it.optLong("updatedAt", System.currentTimeMillis()),
                sortOrder = it.optInt("sortOrder", 0)
            )
        }
    }

    private fun JSONArray.toChapterList(): List<Chapter> = List(length()) { index ->
        getJSONObject(index).let {
            Chapter(
                id = it.getLong("id"),
                bookId = it.getLong("bookId"),
                title = it.getString("title"),
                content = it.optString("content", ""),
                createdAt = it.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = it.optLong("updatedAt", System.currentTimeMillis()),
                sortOrder = it.optInt("sortOrder", 0),
                wordCount = it.optInt("wordCount", 0),
                charCountWithSpaces = it.optInt("charCountWithSpaces", it.optString("content", "").length),
                charCountWithoutSpaces = it.optInt("charCountWithoutSpaces", it.optString("content", "").count { char -> !char.isWhitespace() }),
                backgroundImageUri = it.optStringOrNull("backgroundImageUri")
            )
        }
    }

    private fun JSONObject.optStringOrNull(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).takeIf { it.isNotBlank() && it != "null" }
    }

    private fun String.escapeHtml(): String = buildString {
        this@escapeHtml.forEach { char ->
            when (char) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }
}
