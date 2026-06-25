package ru.norahobbits.talesbook.data.repository

import kotlinx.coroutines.flow.Flow
import ru.norahobbits.talesbook.data.db.BookDao
import ru.norahobbits.talesbook.data.model.Book
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(private val dao: BookDao) {

    fun observeAll(): Flow<List<Book>> = dao.observeAll()

    suspend fun getById(id: Long): Book? = dao.getById(id)

    suspend fun create(title: String, description: String = ""): Long {
        return dao.insert(Book(title = title, description = description))
    }

    suspend fun update(book: Book) {
        dao.update(book.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(book: Book) = dao.delete(book)
}
