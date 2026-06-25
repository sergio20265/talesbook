package ru.norahobbits.talesbook.ui.screens.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.norahobbits.talesbook.data.repository.BackupRepository
import ru.norahobbits.talesbook.settings.AppSettings
import ru.norahobbits.talesbook.settings.AppSettingsDataStore
import ru.norahobbits.talesbook.settings.AppTheme
import ru.norahobbits.talesbook.settings.ContentViewMode
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val dataStore: AppSettingsDataStore,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val settings = dataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setTheme(theme: AppTheme) = viewModelScope.launch { dataStore.updateTheme(theme) }
    fun setFontSize(size: Float) = viewModelScope.launch { dataStore.updateFontSize(size) }
    fun setFontFamily(fontFamily: String) = viewModelScope.launch { dataStore.updateFontFamily(fontFamily) }
    fun setTextColor(color: Long) = viewModelScope.launch { dataStore.updateTextColor(color) }
    fun setEditorBg(uri: String?) = viewModelScope.launch { dataStore.updateEditorBgUri(uri) }
    fun setAppBg(uri: String?) = viewModelScope.launch { dataStore.updateAppBgUri(uri) }
    fun setBookViewMode(mode: ContentViewMode) = viewModelScope.launch { dataStore.updateBookViewMode(mode) }
    fun setChapterViewMode(mode: ContentViewMode) = viewModelScope.launch { dataStore.updateChapterViewMode(mode) }
    fun setMusicEnabled(enabled: Boolean) = viewModelScope.launch { dataStore.updateMusicEnabled(enabled) }
    fun setMusicUri(uri: String?) = viewModelScope.launch { dataStore.updateMusicUri(uri) }
    fun setMusicVolume(vol: Float) = viewModelScope.launch { dataStore.updateMusicVolume(vol) }

    suspend fun exportBackup(): String = backupRepository.exportJson()

    suspend fun importBackup(json: String) = backupRepository.importJson(json)
}
