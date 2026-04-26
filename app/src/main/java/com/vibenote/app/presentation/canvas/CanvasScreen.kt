package com.vibenote.app.presentation.canvas

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibenote.app.core.theme.VibeColors
import androidx.compose.ui.platform.LocalContext
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.model.StrokeType
import kotlin.math.hypot
import kotlin.math.abs

fun List<Offset>.toSmoothedPath(): Path {
    val path = Path()
    if (size < 2) return path
    path.moveTo(this[0].x, this[0].y)
    for (i in 1 until size - 1) {
        val midX = (this[i].x + this[i + 1].x) / 2f
        val midY = (this[i].y + this[i + 1].y) / 2f
        path.quadraticBezierTo(this[i].x, this[i].y, midX, midY)
    }
    path.lineTo(last().x, last().y)
    return path
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    noteId: String,
    noteTitle: String,
    viewModel: CanvasViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var currentPath by remember { mutableStateOf(Path()) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var canvasScale by remember { mutableFloatStateOf(1f) }
    var canvasOffsetX by remember { mutableFloatStateOf(0f) }
    var canvasOffsetY by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showEditTitleDialog = true }
                    ) {
                        Text(state.noteTitle.ifEmpty { "Untitled" })
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit title",
                            tint = VibeColors.TextMuted,
                            modifier = Modifier.padding(start = 8.dp).size(16.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNow()
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VibeColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = state.canUndo) {
                        Icon(
                            Icons.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) VibeColors.TextPrimary else VibeColors.TextMuted
                        )
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = state.canRedo) {
                        Icon(
                            Icons.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) VibeColors.TextPrimary else VibeColors.TextMuted
                        )
                    }
                    IconButton(onClick = { 
                        canvasScale = 1f
                        canvasOffsetX = 0f
                        canvasOffsetY = 0f
                        viewModel.resetTransform()
                    }) {
                        Icon(
                            Icons.Filled.ZoomOutMap,
                            contentDescription = "Reset zoom",
                            tint = VibeColors.TextPrimary
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = VibeColors.TextPrimary
                        )
                    }
                    IconButton(onClick = {
                        viewModel.exportAsPng(context) { uri ->
                            exportedUri = uri
                            showExportDialog = true
                        }
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Export as PNG",
                            tint = VibeColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VibeColors.SurfaceDeep,
                    titleContentColor = VibeColors.TextPrimary
                )
            )
        },
        containerColor = VibeColors.BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(VibeColors.BackgroundDark)
        ) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VibeColors.SurfaceDeep)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(state.selectedColor))
                        .border(2.dp, VibeColors.BorderStandard, CircleShape)
                        .clickable { showColorPicker = !showColorPicker }
                )

                // Tool toggles
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.toggleEraser() }) {
                        Text(
                            text = "Eraser",
                            color = if (state.isEraser) VibeColors.BrandGreen else VibeColors.TextMuted
                        )
                    }
                    TextButton(onClick = { viewModel.toggleHighlighter() }) {
                        Text(
                            text = "HL",
                            color = if (state.isHighlighter) VibeColors.BrandGreen else VibeColors.TextMuted
                        )
                    }
                    TextButton(onClick = { viewModel.toggleShapeMode() }) {
                        Text(
                            text = "Shape",
                            color = if (state.isShapeMode) VibeColors.BrandGreen else VibeColors.TextMuted
                        )
                    }
                    IconButton(onClick = { showBackgroundPicker = !showBackgroundPicker }) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = "Canvas background",
                            tint = if (state.canvasBackground != "dark") VibeColors.BrandGreen else VibeColors.TextMuted
                        )
                    }
                }
            }

            // Color picker
            if (showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VibeColors.SurfaceDeep)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        0xFFFFFFFF.toInt(),
                        0xFF3ECF8E.toInt(),
                        0xFFFF6B6B.toInt(),
                        0xFF4ECDC4.toInt(),
                        0xFFFFE66D.toInt(),
                        0xFF95E1D0.toInt(),
                        0xFFF38181.toInt(),
                        0xFFAA96DA.toInt()
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    if (state.selectedColor == color) 3.dp else 1.dp,
                                    if (state.selectedColor == color) VibeColors.BrandGreen else VibeColors.BorderStandard,
                                    CircleShape
                                )
                                .clickable {
                                    viewModel.setColor(color)
                                    showColorPicker = false
                                }
                        )
                    }
                }
            }

            // Background picker
            if (showBackgroundPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VibeColors.SurfaceDeep)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "dark" to VibeColors.BackgroundDark,
                        "white" to Color.White,
                        "lined" to VibeColors.SurfaceDeep,
                        "dotted" to VibeColors.SurfaceDeep,
                        "grid" to VibeColors.SurfaceDeep
                    ).forEach { (type, color) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (state.canvasBackground == type) 3.dp else 1.dp,
                                    if (state.canvasBackground == type) VibeColors.BrandGreen else VibeColors.BorderStandard,
                                    CircleShape
                                )
                                .clickable {
                                    viewModel.setCanvasBackground(type)
                                    showBackgroundPicker = false
                                }
                        )
                    }
                }
            }

            // Stroke width slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VibeColors.SurfaceDeep)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Size", color = VibeColors.TextMuted, modifier = Modifier.width(40.dp))
                Slider(
                    value = state.strokeWidth,
                    onValueChange = { viewModel.setStrokeWidth(it) },
                    valueRange = 2f..20f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = VibeColors.BrandGreen,
                        activeTrackColor = VibeColors.BrandGreen
                    )
                )
                Text(
                    "${state.strokeWidth.toInt()}",
                    color = VibeColors.TextMuted,
                    modifier = Modifier.width(30.dp)
                )
            }

// Canvas with gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                canvasScale = (canvasScale * zoom).coerceIn(0.5f, 3f)
                                canvasOffsetX += pan.x
                                canvasOffsetY += pan.y
                                viewModel.updateTransform(canvasScale, canvasOffsetX, canvasOffsetY)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (currentPoints.isNotEmpty()) return@detectDragGestures
                                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPoints = listOf(offset)
                                    val newStroke = Stroke(
                                        colorValue = state.selectedColor,
                                        strokeWidth = state.strokeWidth,
                                        isEraser = state.isEraser,
                                        isHighlighter = state.isHighlighter
                                    )
                                    viewModel.startStroke(newStroke)
                                },
                                onDrag = { change, _ ->
                                    currentPath.lineTo(change.position.x, change.position.y)
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    val pointString = currentPoints.joinToString(";") { "${it.x},${it.y}" }
                                    
                                    if (state.isShapeMode && currentPoints.size >= 5) {
                                        val shapeStroke = viewModel.applyShapeRecognition(currentPoints)
                                        if (shapeStroke != null) {
                                            viewModel.startStroke(shapeStroke.copy(points = pointString))
                                            viewModel.finishStroke()
                                            currentPath = Path()
                                            currentPoints = emptyList()
                                            return@detectDragGestures
                                        }
                                    }
                                    
                                    viewModel.updateStroke(pointString)
                                    viewModel.finishStroke()
                                    currentPath = Path()
                                    currentPoints = emptyList()
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = canvasScale,
                                scaleY = canvasScale,
                                translationX = canvasOffsetX,
                                translationY = canvasOffsetY
                            )
                    ) {
                        when (state.canvasBackground) {
                            "white" -> drawRect(Color.White, size = size)
                            "lined" -> {
                                val spacing = 80f
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = Color(0xFF2E2E2E),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                    y += spacing
                                }
                            }
                            "dotted" -> {
                                val spacing = 80f
                                var x = 0f
                                var y = 0f
                                while (y < size.height) {
                                    while (x < size.width) {
                                        drawCircle(
                                            color = Color(0xFF2E2E2E),
                                            radius = 2f,
                                            center = Offset(x, y)
                                        )
                                        x += spacing
                                    }
                                    x = 0f
                                    y += spacing
                                }
                            }
                            "grid" -> {
                                val spacing = 80f
                                var x = 0f
                                while (x < size.width) {
                                    drawLine(
                                        color = Color(0xFF2E2E2E),
                                        start = Offset(x, 0f),
                                        end = Offset(x, size.height),
                                        strokeWidth = 1f
                                    )
                                    x += spacing
                                }
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = Color(0xFF2E2E2E),
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1f
                                    )
                                    y += spacing
                                }
                            }
                        }
                        
                        state.strokes.forEach { stroke ->
                            if (stroke.points.isNotEmpty()) {
                                val pointsList = stroke.points.split(";").mapNotNull { pair ->
                                    val coords = pair.split(",")
                                    if (coords.size == 2) Offset(coords[0].toFloat(), coords[1].toFloat()) else null
                                }
                                if (pointsList.size >= 2) {
                                    val strokeColor = when {
                                        stroke.isEraser -> VibeColors.BackgroundDark
                                        stroke.isHighlighter -> Color(stroke.colorValue).copy(alpha = 0.4f)
                                        else -> Color(stroke.colorValue)
                                    }
                                    val strokeWidth = if (stroke.isHighlighter) stroke.strokeWidth * 3 else stroke.strokeWidth
                                    
                                    when (stroke.strokeType) {
                                        StrokeType.CIRCLE -> {
                                            val center = pointsList.first()
                                            val radius = hypot(
                                                pointsList.last().x - center.x,
                                                pointsList.last().y - center.y
                                            )
                                            drawCircle(
                                                color = strokeColor,
                                                radius = radius,
                                                center = center,
                                                style = DrawStroke(width = strokeWidth)
                                            )
                                        }
                                        StrokeType.RECTANGLE -> {
                                            val topLeft = pointsList.first()
                                            val bottomRight = pointsList.last()
                                            drawRect(
                                                color = strokeColor,
                                                topLeft = topLeft,
                                                size = androidx.compose.ui.geometry.Size(
                                                    bottomRight.x - topLeft.x,
                                                    bottomRight.y - topLeft.y
                                                ),
                                                style = DrawStroke(width = strokeWidth)
                                            )
                                        }
                                        StrokeType.LINE -> {
                                            drawLine(
                                                color = strokeColor,
                                                start = pointsList.first(),
                                                end = pointsList.last(),
                                                strokeWidth = strokeWidth,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                        else -> {
                                            val path = pointsList.toSmoothedPath()
                                            drawPath(
                                                path = path,
                                                color = strokeColor,
                                                style = DrawStroke(
                                                    width = strokeWidth,
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (currentPoints.isNotEmpty()) {
                            val renderColor = when {
                                state.isEraser -> VibeColors.BackgroundDark
                                state.isHighlighter -> Color(state.selectedColor).copy(alpha = 0.4f)
                                else -> Color(state.selectedColor)
                            }
                            val renderWidth = if (state.isHighlighter) state.strokeWidth * 3 else state.strokeWidth
                            val path = currentPoints.toSmoothedPath()
                            drawPath(
                                path = path,
                                color = renderColor,
                                style = DrawStroke(
                                    width = renderWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit title dialog
    if (showEditTitleDialog) {
        var editedTitle by remember { mutableStateOf(state.noteTitle) }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Edit Title", color = VibeColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VibeColors.TextPrimary,
                        unfocusedTextColor = VibeColors.TextPrimary,
                        focusedBorderColor = VibeColors.BrandGreen,
                        unfocusedBorderColor = VibeColors.BorderStandard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTitle(editedTitle.ifBlank { "Untitled" })
                    showEditTitleDialog = false
                }) {
                    Text("Save", color = VibeColors.BrandGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel", color = VibeColors.TextMuted)
                }
            },
            containerColor = VibeColors.BackgroundDark
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note", color = VibeColors.TextPrimary) },
            text = { Text("Are you sure you want to delete this note?", color = VibeColors.TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote { onNavigateBack() }
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = VibeColors.BrandGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = VibeColors.TextMuted)
                }
            },
            containerColor = VibeColors.BackgroundDark
        )
    }

    // Export success dialog
    if (showExportDialog && exportedUri != null) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportedUri = null
            },
            title = { Text("Export Successful", color = VibeColors.TextPrimary) },
            text = { Text("Note exported to Pictures/VibeNote", color = VibeColors.TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, exportedUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                    showExportDialog = false
                    exportedUri = null
                }) {
                    Text("Share", color = VibeColors.BrandGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportedUri = null
                }) {
                    Text("Close", color = VibeColors.TextMuted)
                }
            },
            containerColor = VibeColors.BackgroundDark
        )
    }
}