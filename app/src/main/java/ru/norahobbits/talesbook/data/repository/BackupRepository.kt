package ru.norahobbits.talesbook.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
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
                backgroundImageUri = it.optStringOrNull("backgroundImageUri")
            )
        }
    }

    private fun JSONObject.optStringOrNull(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).takeIf { it.isNotBlank() && it != "null" }
    }
}
