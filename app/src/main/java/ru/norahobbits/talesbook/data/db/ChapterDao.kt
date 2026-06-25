package ru.norahobbits.talesbook.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.norahobbits.talesbook.data.model.Chapter

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeByBook(bookId: Long): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters ORDER BY bookId ASC, sortOrder ASC, createdAt ASC")
    suspend fun getAll(): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Long): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: Chapter): Long

    @Update
    suspend fun update(chapter: Chapter)

    @Delete
    suspend fun delete(chapter: Chapter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Query("DELETE FROM chapters")
    suspend fun deleteAll()

    @Query("SELECT SUM(wordCount) FROM chapters WHERE bookId = :bookId")
    suspend fun totalWordCount(bookId: Long): Int?

    @Query("SELECT SUM(charCountWithSpaces) FROM chapters WHERE bookId = :bookId")
    suspend fun totalCharCountWithSpaces(bookId: Long): Int?

    @Query("SELECT SUM(charCountWithoutSpaces) FROM chapters WHERE bookId = :bookId")
    suspend fun totalCharCountWithoutSpaces(bookId: Long): Int?
}
