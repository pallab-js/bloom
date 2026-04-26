package com.vibenote.app.di

import android.content.Context
import androidx.room.Room
import com.vibenote.app.data.local.NoteDao
import com.vibenote.app.data.local.NoteDatabase
import com.vibenote.app.data.repository.NoteRepositoryImpl
import com.vibenote.app.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNoteDatabase(
        @ApplicationContext context: Context
    ): NoteDatabase {
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            "vibenote.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: NoteDatabase): NoteDao {
        return database.noteDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        impl: NoteRepositoryImpl
    ): NoteRepository
}