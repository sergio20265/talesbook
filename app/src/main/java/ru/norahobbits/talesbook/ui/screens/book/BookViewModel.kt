package ru.norahobbits.talesbook.ui.screens.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.norahobbits.talesbook.data.model.Book
import ru.norahobbits.talesbook.data.model.Chapter
import ru.norahobbits.talesbook.data.repository.BackupRepository
import ru.norahobbits.talesbook.data.repository.BookRepository
import ru.norahobbits.talesbook.data.repository.ChapterRepository
import javax.inject.Inject

data class BookStats(
    val words: Int = 0,
    val charsWithSpaces: Int = 0,
    val charsWithoutSpaces: Int = 0
) {
    val authorSheets: Double get() = charsWithSpaces / 40_000.0
}

@HiltViewModel
class BookViewModel @Inject constructor(
    private val bookRepo: BookRepository,
    private val chapterRepo: ChapterRepository,
    private val backupRepository: BackupRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: Long = checkNotNull(savedStateHandle["bookId"])

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    val chapters = chapterRepo.observeByBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _stats = MutableStateFlow(BookStats())
    val stats: StateFlow<BookStats> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            bookRepo.getById(bookId)?.let { _book.value = it }
        }
        viewModelScope.launch {
            chapters.collect {
                _stats.value = BookStats(
                    words = chapterRepo.totalWordCount(bookId),
                    charsWithSpaces = chapterRepo.totalCharCountWithSpaces(bookId),
                    charsWithoutSpaces = chapterRepo.totalCharCountWithoutSpaces(bookId)
                )
            }
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            bookRepo.update(book)
            _book.value = book
        }
    }

    fun createChapter(title: String) {
        viewModelScope.launch { chapterRepo.create(bookId, title) }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch { chapterRepo.delete(chapter) }
    }

    fun renameChapter(chapter: Chapter, newTitle: String) {
        viewModelScope.launch { chapterRepo.update(chapter.copy(title = newTitle)) }
    }

    fun updateCover(uri: String?) {
        val current = _book.value ?: return
        updateBook(current.copy(coverImageUri = uri))
    }

    fun updateBackground(uri: String?) {
        val current = _book.value ?: return
        updateBook(current.copy(backgroundImageUri = uri))
    }

    fun updateChapterBackground(chapter: Chapter, uri: String?) {
        viewModelScope.launch { chapterRepo.update(chapter.copy(backgroundImageUri = uri)) }
    }

    suspend fun exportBookHtml(): String = backupRepository.exportBookHtml(bookId)
}
