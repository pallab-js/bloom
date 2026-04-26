package com.vibenote.app.data.repository

import com.vibenote.app.data.local.NoteDao
import com.vibenote.app.data.local.FolderDao
import com.vibenote.app.data.local.NoteEntity
import com.vibenote.app.domain.model.CanvasBackground
import com.vibenote.app.domain.model.Note
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NoteRepositoryImplTest {

    private lateinit var noteDao: NoteDao
    private lateinit var folderDao: FolderDao
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setup() {
        noteDao = mockk(relaxed = true)
        folderDao = mockk(relaxed = true)
        repository = NoteRepositoryImpl(noteDao, folderDao)
    }

    @Test
    fun `getAllNotes returns mapped notes`() = runTest {
        val entities = listOf(
            NoteEntity(
                id = "1",
                title = "Test Note",
                createdAt = 1000L,
                updatedAt = 2000L,
                strokeDataPath = "path1",
                isFavorite = false,
                tags = "tag1|tag2",
                folder = "folder1",
                canvasBackground = CanvasBackground.DARK
            )
        )
        every { noteDao.getAllNotes() } returns flowOf(entities)

        val notes = repository.getAllNotes().first()

        assertEquals(1, notes.size)
        assertEquals("1", notes[0].id)
        assertEquals("Test Note", notes[0].title)
        assertEquals(listOf("tag1", "tag2"), notes[0].tags)
    }

    @Test
    fun `insertNote converts and inserts entity`() = runTest {
        val note = Note(
            id = "1",
            title = "Test",
            createdAt = 1000L,
            updatedAt = 2000L,
            strokeDataPath = "path",
            isFavorite = true,
            tags = listOf("tag1", "tag2"),
            folder = "folder",
            canvasBackground = CanvasBackground.WHITE
        )

        repository.insertNote(note)

        coVerify {
            noteDao.insertNote(match {
                it.id == "1" && it.tags == "tag1|tag2"
            })
        }
    }

    @Test
    fun `getNoteById returns null when not found`() = runTest {
        coEvery { noteDao.getNoteById("1") } returns null

        val note = repository.getNoteById("1")

        assertNull(note)
    }

    @Test
    fun `tags with blank entries are filtered`() = runTest {
        val entities = listOf(
            NoteEntity(
                id = "1",
                title = "Test",
                createdAt = 1000L,
                strokeDataPath = "path",
                tags = "tag1||tag2|",
                folder = ""
            )
        )
        every { noteDao.getAllNotes() } returns flowOf(entities)

        val notes = repository.getAllNotes().first()

        assertEquals(listOf("tag1", "tag2"), notes[0].tags)
    }

    @Test
    fun `empty tags string returns empty list`() = runTest {
        val entities = listOf(
            NoteEntity(
                id = "1",
                title = "Test",
                createdAt = 1000L,
                strokeDataPath = "path",
                tags = "",
                folder = ""
            )
        )
        every { noteDao.getAllNotes() } returns flowOf(entities)

        val notes = repository.getAllNotes().first()

        assertEquals(emptyList<String>(), notes[0].tags)
    }
}
