package com.vibenote.app.data.repository

import com.vibenote.app.data.local.NoteDao
import com.vibenote.app.data.local.NoteEntity
import com.vibenote.app.data.local.FolderDao
import com.vibenote.app.data.local.FolderEntity
import com.vibenote.app.domain.model.Note
import com.vibenote.app.domain.model.Folder
import com.vibenote.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }

    override fun getFavoriteNotes(): Flow<List<Note>> {
        return noteDao.getFavoriteNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }

    override fun getNotesByTag(tag: String): Flow<List<Note>> {
        return noteDao.getNotesByTag(tag).map { entities ->
            entities.map { it.toNote() }
        }
    }

    override fun getNotesByFolder(folder: String): Flow<List<Note>> {
        return noteDao.getNotesByFolder(folder).map { entities ->
            entities.map { it.toNote() }
        }
    }

    override suspend fun insertNote(note: Note) {
        noteDao.insertNote(note.toEntity())
    }

    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.toEntity())
    }

    override suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note.toEntity())
    }

    override suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.toNote()
    }

    override suspend fun updateNoteTimestamp(id: String, timestamp: Long) {
        noteDao.updateTimestamp(id, timestamp)
    }

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { it.toFolder() }
        }
    }

    override suspend fun insertFolder(folder: Folder) {
        folderDao.insertFolder(folder.toEntity())
    }

    override suspend fun deleteFolder(folder: Folder) {
        folderDao.deleteFolder(folder.toEntity())
    }

    private fun NoteEntity.toNote() = Note(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        strokeDataPath = strokeDataPath,
        isFavorite = isFavorite,
        tags = if (tags.isBlank()) emptyList() else tags.split("|").filter { it.isNotBlank() },
        folder = folder,
        canvasBackground = canvasBackground,
        folderId = folderId,
        sourceUri = sourceUri,
        contentJson = contentJson
    )

    private fun Note.toEntity() = NoteEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        strokeDataPath = strokeDataPath,
        isFavorite = isFavorite,
        tags = tags.joinToString("|"),
        folder = folder,
        canvasBackground = canvasBackground,
        folderId = folderId,
        sourceUri = sourceUri,
        contentJson = contentJson
    )

    private fun FolderEntity.toFolder() = Folder(
        id = id,
        name = name,
        parentId = parentId,
        createdAt = createdAt
    )

    private fun Folder.toEntity() = FolderEntity(
        id = id,
        name = name,
        parentId = parentId,
        createdAt = createdAt
    )
}
