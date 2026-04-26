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
    val canvasBackground: String = "dark"
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
    private val parsedPointsCache = mutableMapOf<Int, List<Offset>>()

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
        updateNoteTimestamp()
        scheduleSave()
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
        clearCache()
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
        clearCache()
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
        clearCache()
        scheduleSave()
    }

    fun updateTitle(newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteId = _state.value.noteId
            if (noteId.isNotEmpty()) {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = note.copy(title = newTitle, updatedAt = System.currentTimeMillis())
                    noteRepository.insertNote(updatedNote)
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(noteTitle = newTitle) }
                    }
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

    private fun updateNoteTimestamp() {
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
                    noteRepository.updateNote(updatedNote)
                }
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    private fun saveStrokesToFile() {
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val strokes = _state.value.strokes
            val strokesFile = File(context.filesDir, "strokes_$noteId.json")
            try {
                strokesFile.writeText(gson.toJson(strokes))
            } catch (e: Exception) {
                // Silent fail
            }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            saveStrokesToFile()
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        saveStrokesToFile()
    }

    fun getParsedPoints(strokeIndex: Int, pointsString: String): List<Offset> {
        return parsedPointsCache.getOrPut(strokeIndex) {
            pointsString.split(";").mapNotNull { pair ->
                val coords = pair.split(",")
                if (coords.size == 2) Offset(coords[0].toFloat(), coords[1].toFloat()) else null
            }
        }
    }

    fun clearCache() {
        parsedPointsCache.clear()
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
                if (stroke.points.isNotEmpty()) {
                    val pointsList = stroke.points.split(";").mapNotNull { pair ->
                        val coords = pair.split(",")
                        if (coords.size == 2) Offset(coords[0].toFloat(), coords[1].toFloat()) else null
                    }
                    
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
            }
            
            val filename = "vibenote_${_state.value.noteTitle}_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VibeNote")
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

    fun setCanvasBackground(background: String) {
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