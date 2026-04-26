package com.vibenote.app.presentation.dashboard

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibenote.app.data.local.toDomain
import com.vibenote.app.domain.model.Note
import com.vibenote.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow<FilterType>(FilterType.All)
    val filter: StateFlow<FilterType> = _filter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                val name = preferences[SORT_ORDER_KEY] ?: SortOrder.NEWEST_FIRST.name
                try { SortOrder.valueOf(name) } catch (e: Exception) { SortOrder.NEWEST_FIRST }
            }.collect { order ->
                _sortOrder.value = order
            }
        }
    }

    private val gson = com.google.gson.Gson()

    val notes: StateFlow<List<Note>> = combine(
        noteRepository.getAllNotes(),
        _searchQuery,
        _filter,
        _sortOrder
    ) { notes, query, filterType, sort ->
        var filtered = when (filterType) {
            FilterType.All -> notes
            FilterType.Favorites -> notes.filter { it.isFavorite }
            is FilterType.Tag -> notes.filter { filterType.tag in it.tags }
            is FilterType.Folder -> notes.filter { it.folder == filterType.folder }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.title.contains(query, ignoreCase = true) }
        }
        
        // Load strokes for each filtered note for preview
        val notesWithStrokes = filtered.map { note ->
            val strokesFile = File(context.filesDir, "strokes_${note.id}.json")
            if (strokesFile.exists()) {
                try {
                    val json = strokesFile.readText()
                    val type = object : com.google.gson.reflect.TypeToken<List<com.vibenote.app.data.local.StrokeDto>>() {}.type
                    val dtos: List<com.vibenote.app.data.local.StrokeDto> = gson.fromJson(json, type) ?: emptyList()
                    note.copy(strokes = dtos.map { it.toDomain() })
                } catch (e: Exception) {
                    note
                }
            } else {
                note
            }
        }

        when (sort) {
            SortOrder.NEWEST_FIRST -> notesWithStrokes.sortedByDescending { it.createdAt }
            SortOrder.OLDEST_FIRST -> notesWithStrokes.sortedBy { it.createdAt }
            SortOrder.LAST_MODIFIED -> notesWithStrokes.sortedByDescending { it.updatedAt }
            SortOrder.A_TO_Z -> notesWithStrokes.sortedBy { it.title.lowercase() }
            SortOrder.Z_TO_A -> notesWithStrokes.sortedByDescending { it.title.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: FilterType) {
        _filter.value = filter
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SORT_ORDER_KEY] = order.name
            }
        }
    }

    fun filterByTag(tag: String) {
        _filter.value = FilterType.Tag(tag)
    }

    fun filterByFolder(folder: String) {
        _filter.value = FilterType.Folder(folder)
    }

    fun clearFilter() {
        _filter.value = FilterType.All
    }

    fun moveToFolder(noteId: String, folder: String) {
        viewModelScope.launch {
            val note = noteRepository.getNoteById(noteId)
            if (note != null) {
                noteRepository.updateNote(note.copy(folder = folder))
            }
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            noteRepository.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    fun addTag(noteId: String, tag: String) {
        viewModelScope.launch {
            val note = noteRepository.getNoteById(noteId)
            if (note != null) {
                val newTags = note.tags + tag
                noteRepository.updateNote(note.copy(tags = newTags))
            }
        }
    }

    fun createNote(title: String) {
        viewModelScope.launch {
            val note = Note(title = title)
            noteRepository.insertNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.deleteNote(note)
            val strokesFile = File(context.filesDir, "strokes_${note.id}.json")
            if (strokesFile.exists()) {
                strokesFile.delete()
            }
        }
    }

    fun duplicateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = UUID.randomUUID().toString()
            val copy = note.copy(
                id = newId,
                title = "${note.title} (copy)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            noteRepository.insertNote(copy)
            val src = File(context.filesDir, "strokes_${note.id}.json")
            val dst = File(context.filesDir, "strokes_$newId.json")
            if (src.exists()) {
                src.copyTo(dst, overwrite = true)
            }
        }
    }
}

sealed class FilterType {
    data object All : FilterType()
    data object Favorites : FilterType()
    data class Tag(val tag: String) : FilterType()
    data class Folder(val folder: String) : FilterType()
}

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    LAST_MODIFIED,
    A_TO_Z,
    Z_TO_A
}