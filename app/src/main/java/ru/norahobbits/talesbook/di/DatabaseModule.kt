package ru.norahobbits.talesbook.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.norahobbits.talesbook.data.db.AppDatabase
import ru.norahobbits.talesbook.data.db.BookDao
import ru.norahobbits.talesbook.data.db.ChapterDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE chapters ADD COLUMN charCountWithSpaces INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE chapters ADD COLUMN charCountWithoutSpaces INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "talesbook.db")
            .addMigrations(migration1To2)
            .build()

    @Provides
    fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()

    @Provides
    fun provideChapterDao(db: AppDatabase): ChapterDao = db.chapterDao()
}
