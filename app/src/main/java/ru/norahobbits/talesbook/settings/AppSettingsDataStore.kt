package ru.norahobbits.talesbook.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val TEXT_COLOR = longPreferencesKey("text_color")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val EDITOR_BG_URI = stringPreferencesKey("editor_bg_uri")
        val APP_BG_URI = stringPreferencesKey("app_bg_uri")
        val BOOK_VIEW_MODE = stringPreferencesKey("book_view_mode")
        val CHAPTER_VIEW_MODE = stringPreferencesKey("chapter_view_mode")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val MUSIC_URI = stringPreferencesKey("music_uri")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            selectedTheme = runCatching {
                AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.EVENING_FOREST.name)
            }.getOrDefault(AppTheme.EVENING_FOREST),
            fontSize = prefs[Keys.FONT_SIZE] ?: 16f,
            textColor = prefs[Keys.TEXT_COLOR] ?: 0xFFEFE8D8,
            accentColor = prefs[Keys.ACCENT_COLOR] ?: 0xFFB89B5E,
            editorBackgroundImageUri = prefs[Keys.EDITOR_BG_URI],
            appBackgroundImageUri = prefs[Keys.APP_BG_URI],
            bookViewMode = readViewMode(prefs[Keys.BOOK_VIEW_MODE]),
            chapterViewMode = readViewMode(prefs[Keys.CHAPTER_VIEW_MODE]),
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: false,
            selectedMusicUri = prefs[Keys.MUSIC_URI],
            musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.5f
        )
    }

    suspend fun updateTheme(theme: AppTheme) = update { it[Keys.THEME] = theme.name }
    suspend fun updateFontSize(size: Float) = update { it[Keys.FONT_SIZE] = size }
    suspend fun updateTextColor(color: Long) = update { it[Keys.TEXT_COLOR] = color }
    suspend fun updateAccentColor(color: Long) = update { it[Keys.ACCENT_COLOR] = color }
    suspend fun updateEditorBgUri(uri: String?) = update {
        if (uri != null) it[Keys.EDITOR_BG_URI] = uri else it.remove(Keys.EDITOR_BG_URI)
    }
    suspend fun updateAppBgUri(uri: String?) = update {
        if (uri != null) it[Keys.APP_BG_URI] = uri else it.remove(Keys.APP_BG_URI)
    }
    suspend fun updateBookViewMode(mode: ContentViewMode) = update { it[Keys.BOOK_VIEW_MODE] = mode.name }
    suspend fun updateChapterViewMode(mode: ContentViewMode) = update { it[Keys.CHAPTER_VIEW_MODE] = mode.name }
    suspend fun updateMusicEnabled(enabled: Boolean) = update { it[Keys.MUSIC_ENABLED] = enabled }
    suspend fun updateMusicUri(uri: String?) = update {
        if (uri != null) it[Keys.MUSIC_URI] = uri else it.remove(Keys.MUSIC_URI)
    }
    suspend fun updateMusicVolume(volume: Float) = update { it[Keys.MUSIC_VOLUME] = volume }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun readViewMode(value: String?): ContentViewMode {
        return runCatching {
            ContentViewMode.valueOf(value ?: ContentViewMode.LIST.name)
        }.getOrDefault(ContentViewMode.LIST)
    }
}
