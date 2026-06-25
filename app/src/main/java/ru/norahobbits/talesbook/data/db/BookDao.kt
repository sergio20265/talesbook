package ru.norahobbits.talesbook.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.norahobbits.talesbook.data.model.Book

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY sortOrder ASC, updatedAt DESC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM books ORDER BY sortOrder ASC, updatedAt DESC")
    suspend fun getAll(): List<Book>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<Book>)

    @Query("DELETE FROM books")
    suspend fun deleteAll()
}
