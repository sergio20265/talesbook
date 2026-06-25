package ru.norahobbits.talesbook.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "talesbook.db").build()

    @Provides
    fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()

    @Provides
    fun provideChapterDao(db: AppDatabase): ChapterDao = db.chapterDao()
}
