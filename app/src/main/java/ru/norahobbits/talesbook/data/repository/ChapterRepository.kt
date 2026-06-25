package ru.norahobbits.talesbook.data.repository

import android.text.Html
import kotlinx.coroutines.flow.Flow
import ru.norahobbits.talesbook.data.db.BookDao
import ru.norahobbits.talesbook.data.db.ChapterDao
import ru.norahobbits.talesbook.data.model.Chapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao,
    private val bookDao: BookDao
) {

    fun observeByBook(bookId: Long): Flow<List<Chapter>> = chapterDao.observeByBook(bookId)

    suspend fun getById(id: Long): Chapter? = chapterDao.getById(id)

    suspend fun create(bookId: Long, title: String): Long {
        return chapterDao.insert(Chapter(bookId = bookId, title = title))
    }

    suspend fun update(chapter: Chapter) {
        val plainContent = Html.fromHtml(chapter.content, Html.FROM_HTML_MODE_LEGACY).toString()
        val wordCount = plainContent.trim()
            .split("\\s+".toRegex())
            .count { it.isNotEmpty() }
        val charCountWithSpaces = plainContent.length
        val charCountWithoutSpaces = plainContent.count { !it.isWhitespace() }
        val updated = chapter.copy(
            updatedAt = System.currentTimeMillis(),
            wordCount = wordCount,
            charCountWithSpaces = charCountWithSpaces,
            charCountWithoutSpaces = charCountWithoutSpaces
        )
        chapterDao.update(updated)
        bookDao.getById(chapter.bookId)?.let {
            bookDao.update(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun delete(chapter: Chapter) = chapterDao.delete(chapter)

    suspend fun totalWordCount(bookId: Long): Int = chapterDao.totalWordCount(bookId) ?: 0
    suspend fun totalCharCountWithSpaces(bookId: Long): Int = chapterDao.totalCharCountWithSpaces(bookId) ?: 0
    suspend fun totalCharCountWithoutSpaces(bookId: Long): Int = chapterDao.totalCharCountWithoutSpaces(bookId) ?: 0
}
