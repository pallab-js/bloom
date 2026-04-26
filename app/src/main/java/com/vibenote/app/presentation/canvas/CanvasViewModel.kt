package com.vibenote.app.presentation.canvas

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vibenote.app.data.local.StrokeDto
import com.vibenote.app.data.local.toDomain
import com.vibenote.app.data.local.toDto
import com.vibenote.app.domain.model.CanvasBackground
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.repository.NoteRepository
import com.vibenote.app.presentation.canvas.ShapeRecognitionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val isLoading: Boolean = false,
    val canvasBackground: CanvasBackground = CanvasBackground.DARK,
    val errorMessage: String? = null
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
    private val MAX_UNDO = 50
    private var saveJob: Job? = null

    fun applyShapeRecognition(points: List<Offset>): Stroke? {
        val result = ShapeRecognitionHelper.recognize(points) ?: return null
        
        if (result.confidence < 0.7f) return null
        
        return Stroke(
            points = points,
            colorValue = _state.value.selectedColor,
            strokeWidth = _state.value.strokeWidth,
            strokeType = result.strokeType
        )
    }

    fun loadNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, noteId = noteId) }
            
            val note = noteRepository.getNoteById(noteId)
            if (note != null) {
                _state.update { it.copy(
                    noteTitle = note.title,
                    canvasBackground = note.canvasBackground
                ) }
                
                val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                if (strokesFile.exists()) {
                    try {
                        if (strokesFile.length() > 10_000_000) {
                            _state.update { it.copy(strokes = emptyList(), isLoading = false, errorMessage = "Note file too large") }
                            return@launch
                        }
                        val json = strokesFile.readText()
                        val type = object : TypeToken<List<StrokeDto>>() {}.type
                        val dtos: List<StrokeDto> = gson.fromJson(json, type) ?: emptyList()
                        val strokes = dtos.take(10000).map { it.toDomain() }
                        _state.update { it.copy(strokes = strokes, isLoading = false) }
                    } catch (e: Exception) {
                        _state.update { it.copy(strokes = emptyList(), isLoading = false, errorMessage = "Failed to load note") }
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

    fun updateStroke(points: List<Offset>) {
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
        scheduleSave()
    }

    fun toggleEraser() {
        _state.update { it.copy(isEraser = !it.isEraser, isHighlighter = false, isShapeMode = false) }
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
        scheduleSave()
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
        scheduleSave()
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
        scheduleSave()
    }

    fun updateTitle(newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = note.copy(title = newTitle, updatedAt = System.currentTimeMillis())
                    noteRepository.updateNote(updatedNote)
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(noteTitle = newTitle) }
                    }
                }
            }
        }
    }

    fun updateContent(content: String) {
        _state.update { it.copy(contentJson = content) }
        scheduleSaveContent()
    }

    private var contentSaveJob: Job? = null
    private fun scheduleSaveContent() {
        contentSaveJob?.cancel()
        contentSaveJob = viewModelScope.launch {
            delay(1500)
            val noteId = _state.value.noteId
            if (noteId.isEmpty()) return@launch
            val content = _state.value.contentJson
            withContext(Dispatchers.IO) {
                try {
                    val note = noteRepository.getNoteById(noteId)
                    if (note != null) {
                        noteRepository.updateNote(note.copy(contentJson = content, updatedAt = System.currentTimeMillis()))
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(errorMessage = "Failed to save text content") }
                }
            }
        }
    }

    fun deleteNote(onDeleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    noteRepository.deleteNote(note)
                    val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                    if (strokesFile.exists()) {
                        strokesFile.delete()
                    }
                    withContext(Dispatchers.Main) { onDeleted() }
                }
            }
        }
    }

    private fun saveToUndoStack() {
        undoStack.add(_state.value.strokes)
        if (undoStack.size > MAX_UNDO) undoStack.removeAt(0)
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1500)
            val noteId = _state.value.noteId
            if (noteId.isEmpty()) return@launch
            val strokes = _state.value.strokes
            val timestamp = System.currentTimeMillis()
            
            withContext(Dispatchers.IO) {
                try {
                    val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                    val dtos = strokes.map { it.toDto() }
                    val json = gson.toJson(dtos)
                    strokesFile.writeText(json)
                    noteRepository.updateNoteTimestamp(noteId, timestamp)
                } catch (e: Exception) {
                    _state.update { it.copy(errorMessage = "Failed to save note: ${e.message}") }
                }
            }
        }
    }

    suspend fun saveNow() {
        saveJob?.cancel()
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return
        val strokes = _state.value.strokes
        val timestamp = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            try {
                val strokesFile = File(context.filesDir, "strokes_$noteId.json")
                val dtos = strokes.map { it.toDto() }
                val json = gson.toJson(dtos)
                strokesFile.writeText(json)
                noteRepository.updateNoteTimestamp(noteId, timestamp)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to save note: ${e.message}") }
            }
        }
    }

    fun exportAsPng(context: Context, onExported: (Uri) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = Bitmap.createBitmap(2048, 1536, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(0xFF171717.toInt())
            
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            
            _state.value.strokes.forEach { stroke ->
                val pointsList = stroke.points
                if (pointsList.size >= 2) {
                    paint.color = stroke.colorValue
                    paint.strokeWidth = stroke.strokeWidth
                    
                    val path = android.graphics.Path()
                    path.moveTo(pointsList[0].x, pointsList[0].y)
                    for (i in 1 until pointsList.size) {
                        path.lineTo(pointsList[i].x, pointsList[i].y)
                    }
                    canvas.drawPath(path, paint)
                }
            }
            
            val filename = "vibenote_${_state.value.noteTitle}_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Bloom")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                bitmap.recycle()
                withContext(Dispatchers.Main) { onExported(it) }
            } ?: bitmap.recycle()
        }
    }

    fun setCanvasBackground(background: CanvasBackground) {
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return
        
        _state.update { it.copy(canvasBackground = background) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = note.copy(
                        canvasBackground = background,
                        updatedAt = System.currentTimeMillis()
                    )
                    noteRepository.updateNote(updatedNote)
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to update background: ${e.message}") }
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