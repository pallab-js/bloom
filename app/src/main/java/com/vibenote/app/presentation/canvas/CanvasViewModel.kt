package com.vibenote.app.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.repository.NoteRepository
import com.vibenote.app.presentation.canvas.ShapeRecognitionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class CanvasState(
    val strokes: List<Stroke> = emptyList(),
    val currentStroke: Stroke? = null,
    val selectedColor: Int = Color.White.toArgb(),
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val isShapeMode: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val noteId: String = "",
    val noteTitle: String = "Untitled",
    val isLoading: Boolean = false
)

val PEN_COLORS = listOf(
    0xFFFFFFFF.toInt(), // White
    0xFF3ECF8E.toInt(), // Brand Green
    0xFFFF6B6B.toInt(), // Red
    0xFF4ECDC4.toInt(), // Teal
    0xFFFFE66D.toInt(), // Yellow
    0xFF95E1D0.toInt(), // Mint
    0xFFF38181.toInt(), // Coral
    0xFFAA96DA.toInt() // Lavender
)

@HiltViewModel
class CanvasViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CanvasState())
    val state: StateFlow<CanvasState> = _state.asStateFlow()

    private val undoStack = mutableListOf<List<Stroke>>()
    private val redoStack = mutableListOf<List<Stroke>>()
    private val gson = Gson()

    fun applyShapeRecognition(points: List<Offset>): Stroke? {
        val result = ShapeRecognitionHelper.recognize(points) ?: return null
        
        if (result.confidence < 0.7f) return null
        
        return Stroke(
            points = points.joinToString(";") { "${it.x},${it.y}" },
            colorValue = _state.value.selectedColor,
            strokeWidth = _state.value.strokeWidth,
            strokeType = result.strokeType
        )
    }

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, noteId = noteId) }
            
            val note = noteRepository.getNoteById(noteId)
            if (note != null) {
                _state.update { it.copy(noteTitle = note.title) }
                
                val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                if (strokesFile.exists()) {
                    try {
                        val json = strokesFile.readText()
                        val type = object : TypeToken<List<Stroke>>() {}.type
                        val strokes: List<Stroke> = gson.fromJson(json, type) ?: emptyList()
                        _state.update { it.copy(strokes = strokes, isLoading = false) }
                    } catch (e: Exception) {
                        _state.update { it.copy(strokes = emptyList(), isLoading = false) }
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun startStroke(stroke: Stroke) {
        _state.update { it.copy(currentStroke = stroke) }
    }

    fun updateStroke(points: String) {
        _state.update { current ->
            current.copy(currentStroke = current.currentStroke?.copy(points = points))
        }
    }

    fun finishStroke() {
        val currentStroke = _state.value.currentStroke ?: return
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = s.strokes + currentStroke,
                currentStroke = null,
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        saveStrokesToFile()
    }

    fun toggleEraser() {
        _state.update { it.copy(isEraser = !it.isEraser, isHighlighter = false) }
    }

    fun toggleHighlighter() {
        _state.update { it.copy(isHighlighter = !it.isHighlighter, isEraser = false, isShapeMode = false) }
    }

    fun toggleShapeMode() {
        _state.update { it.copy(isShapeMode = !it.isShapeMode, isEraser = false, isHighlighter = false) }
    }

    fun setColor(color: Int) {
        _state.update { it.copy(selectedColor = color, isEraser = false, isHighlighter = false, isShapeMode = false)
        }
    }

    fun setStrokeWidth(width: Float) {
        _state.update { it.copy(strokeWidth = width) }
    }

    fun updateTransform(scale: Float, offsetX: Float, offsetY: Float) {
        _state.update { it.copy(scale = scale, offsetX = offsetX, offsetY = offsetY) }
    }

    fun resetTransform() {
        _state.update { it.copy(scale = 1f, offsetX = 0f, offsetY = 0f) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentStrokes = _state.value.strokes
        redoStack.add(currentStrokes)
        val previousStrokes = undoStack.removeLastOrNull() ?: emptyList()
        _state.update { s ->
            s.copy(
                strokes = previousStrokes,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
        saveStrokesToFile()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentStrokes = _state.value.strokes
        undoStack.add(currentStrokes)
        val nextStrokes = redoStack.removeLastOrNull() ?: emptyList()
        _state.update { s ->
            s.copy(
                strokes = nextStrokes,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
        saveStrokesToFile()
    }

    fun clearCanvas() {
        saveToUndoStack()
        _state.update { s ->
            s.copy(
                strokes = emptyList(),
                canUndo = true,
                canRedo = false
            )
        }
        redoStack.clear()
        saveStrokesToFile()
    }

    fun updateTitle(newTitle: String) {
        viewModelScope.launch {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = note.copy(title = newTitle)
                    noteRepository.insertNote(updatedNote)
                    _state.update { it.copy(noteTitle = newTitle) }
                }
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    noteRepository.deleteNote(note)
                    val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                    if (strokesFile.exists()) {
                        strokesFile.delete()
                    }
                    onDeleted()
                }
            }
        }
    }

    private fun saveToUndoStack() {
        undoStack.add(_state.value.strokes)
    }

    private fun saveStrokesToFile() {
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return
        
        viewModelScope.launch {
            val strokes = _state.value.strokes
            val strokesFile = File(context.filesDir, "strokes_$noteId.json")
            try {
                strokesFile.writeText(gson.toJson(strokes))
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    fun getStrokeData(): List<Stroke> = _state.value.strokes

    fun loadStrokeData(strokes: List<Stroke>) {
        _state.update { s ->
            s.copy(strokes = strokes)
        }
    }
}