package ru.norahobbits.talesbook.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.norahobbits.talesbook.data.model.Book
import ru.norahobbits.talesbook.data.model.Chapter

@Database(
    entities = [Book::class, Chapter::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
}
