package ru.norahobbits.talesbook.settings

data class AppSettings(
    val selectedTheme: AppTheme = AppTheme.EVENING_FOREST,
    val fontSize: Float = 16f,
    val fontFamily: String = "default",
    val textColor: Long = 0xFFEFE8D8,
    val accentColor: Long = 0xFFB89B5E,
    val editorBackgroundImageUri: String? = null,
    val appBackgroundImageUri: String? = null,
    val bookViewMode: ContentViewMode = ContentViewMode.LIST,
    val chapterViewMode: ContentViewMode = ContentViewMode.LIST,
    val musicEnabled: Boolean = false,
    val selectedMusicUri: String? = null,
    val musicVolume: Float = 0.5f
)

enum class AppTheme {
    EVENING_FOREST,
    WITCH_LIBRARY,
    MOON_GARDEN,
    HOBBIT_ROOM
}

enum class ContentViewMode {
    LIST,
    CARDS
}
