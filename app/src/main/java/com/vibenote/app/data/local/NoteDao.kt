package com.vibenote.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE '|' || tags || '|' LIKE '%|' || :tag || '|%' ORDER BY createdAt DESC")
    fun getNotesByTag(tag: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folder = :folder ORDER BY createdAt DESC")
    fun getNotesByFolder(folder: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("UPDATE notes SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: String, timestamp: Long)
}