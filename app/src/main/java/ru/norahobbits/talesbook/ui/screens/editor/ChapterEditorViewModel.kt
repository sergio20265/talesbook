package ru.norahobbits.talesbook.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.norahobbits.talesbook.data.model.Chapter
import ru.norahobbits.talesbook.data.repository.ChapterRepository
import javax.inject.Inject

data class EditorUiState(
    val chapter: Chapter? = null,
    val chapters: List<Chapter> = emptyList(),
    val isSaving: Boolean = false,
    val savedAt: Long = 0L
)

@HiltViewModel
class ChapterEditorViewModel @Inject constructor(
    private val chapterRepo: ChapterRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapterId: Long = checkNotNull(savedStateHandle["chapterId"])

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val chapter = chapterRepo.getById(chapterId)
            _state.update { it.copy(chapter = chapter) }
            chapter?.let { loadedChapter ->
                chapterRepo.observeByBook(loadedChapter.bookId).collect { chapters ->
                    _state.update { it.copy(chapters = chapters) }
                }
            }
        }
    }

    fun updateTitle(title: String) {
        val chapter = _state.value.chapter ?: return
        _state.update { it.copy(chapter = chapter.copy(title = title)) }
        scheduleSave()
    }

    fun updateContent(content: String) {
        val chapter = _state.value.chapter ?: return
        _state.update { it.copy(chapter = chapter.copy(content = content)) }
        scheduleSave()
    }

    fun updateBackground(uri: String?) {
        val chapter = _state.value.chapter ?: return
        val updated = chapter.copy(backgroundImageUri = uri)
        _state.update { it.copy(chapter = updated) }
        viewModelScope.launch { chapterRepo.update(updated) }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(700)
            persistCurrentChapter()
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        persistCurrentChapter()
    }

    suspend fun saveNowAndWait() {
        saveJob?.cancelAndJoin()
        val chapter = _state.value.chapter ?: return
        withContext(NonCancellable) {
            _state.update { it.copy(isSaving = true) }
            chapterRepo.update(chapter)
            _state.update { it.copy(isSaving = false, savedAt = System.currentTimeMillis()) }
        }
    }

    private fun persistCurrentChapter() {
        val chapter = _state.value.chapter ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            chapterRepo.update(chapter)
            _state.update { it.copy(isSaving = false, savedAt = System.currentTimeMillis()) }
        }
    }

    override fun onCleared() {
        saveJob?.cancel()
        super.onCleared()
    }
}
