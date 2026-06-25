package ru.norahobbits.talesbook.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.norahobbits.talesbook.data.model.Book
import ru.norahobbits.talesbook.data.repository.BookRepository
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepo: BookRepository
) : ViewModel() {

    val books = bookRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createBook(title: String, description: String) {
        viewModelScope.launch { bookRepo.create(title, description) }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch { bookRepo.delete(book) }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch { bookRepo.update(book) }
    }
}
