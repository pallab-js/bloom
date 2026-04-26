package com.vibenote.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibenote.app.domain.model.Note
import com.vibenote.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow<FilterType>(FilterType.All)
    val filter: StateFlow<FilterType> = _filter.asStateFlow()

    val notes: StateFlow<List<Note>> = combine(
        noteRepository.getAllNotes(),
        _searchQuery,
        _filter
    ) { notes, query, filterType ->
        var filtered = when (filterType) {
            FilterType.All -> notes
            FilterType.Favorites -> notes.filter { it.isFavorite }
            is FilterType.Tag -> notes.filter { filterType.tag in it.tags }
            is FilterType.Folder -> notes.filter { it.folder == filterType.folder }
        }
        if (query.isBlank()) filtered
        else filtered.filter { it.title.contains(query, ignoreCase = true) }
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
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }
}

sealed class FilterType {
    data object All : FilterType()
    data object Favorites : FilterType()
    data class Tag(val tag: String) : FilterType()
    data class Folder(val folder: String) : FilterType()
}