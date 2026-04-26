package com.vibenote.app.presentation.canvas

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibenote.app.core.theme.LocalVibeColors
import com.vibenote.app.core.theme.VibeColors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import com.vibenote.app.domain.model.CanvasBackground
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.model.StrokeType
import kotlin.math.hypot
import kotlin.math.abs
import kotlinx.coroutines.launch

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

@Composable
fun BackgroundSwatch(type: CanvasBackground, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) LocalVibeColors.current.brand else LocalVibeColors.current.borderStandard
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (type) {
                CanvasBackground.DARK -> drawRect(Color(0xFF171717))
                CanvasBackground.WHITE -> drawRect(Color.White)
                CanvasBackground.LINED -> {
                    drawRect(Color(0xFF171717))
                    for (y in 0..size.height.toInt() step 8) {
                        drawLine(Color(0xFF2E2E2E), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
                    }
                }
                CanvasBackground.DOTTED -> {
                    drawRect(Color(0xFF171717))
                    for (y in 0..size.height.toInt() step 8)
                        for (x in 0..size.width.toInt() step 8)
                            drawCircle(Color(0xFF2E2E2E), 1f, Offset(x.toFloat(), y.toFloat()))
                }
                CanvasBackground.GRID -> {
                    drawRect(Color(0xFF171717))
                    for (v in 0..size.height.toInt() step 8)
                        drawLine(Color(0xFF2E2E2E), Offset(0f, v.toFloat()), Offset(size.width, v.toFloat()), 1f)
                    for (h in 0..size.width.toInt() step 8)
                        drawLine(Color(0xFF2E2E2E), Offset(h.toFloat(), 0f), Offset(h.toFloat(), size.height), 1f)
                }
            }
        }
    }
}

@Composable
fun ToolButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) LocalVibeColors.current.brand.copy(alpha = 0.2f) else Color.Transparent
    val textColor = if (isActive) LocalVibeColors.current.brand else LocalVibeColors.current.textMuted
    val borderColor = if (isActive) LocalVibeColors.current.brand.copy(alpha = 0.5f) else Color.Transparent
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var currentPath by remember { mutableStateOf(Path()) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
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
                            tint = LocalVibeColors.current.textMuted,
                            modifier = Modifier.padding(start = 8.dp).size(16.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            viewModel.saveNow()
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LocalVibeColors.current.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = state.canUndo) {
                        Icon(
                            Icons.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) LocalVibeColors.current.textPrimary else LocalVibeColors.current.textMuted
                        )
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = state.canRedo) {
                        Icon(
                            Icons.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) LocalVibeColors.current.textPrimary else LocalVibeColors.current.textMuted
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
                            tint = LocalVibeColors.current.textPrimary
                        )
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            Icons.Filled.LayersClear,
                            contentDescription = "Clear canvas",
                            tint = LocalVibeColors.current.textPrimary
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = LocalVibeColors.current.textPrimary
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
                            tint = LocalVibeColors.current.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalVibeColors.current.surface,
                    titleContentColor = LocalVibeColors.current.textPrimary
                )
            )
        },
        containerColor = LocalVibeColors.current.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LocalVibeColors.current.background)
        ) {
            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalVibeColors.current.surface)
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
                        .border(2.dp, LocalVibeColors.current.borderStandard, CircleShape)
                        .clickable { 
                            showColorPicker = !showColorPicker 
                            showBackgroundPicker = false
                        }
                )

                // Tool toggles - Grouped
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(LocalVibeColors.current.background.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            if (state.isEraser) viewModel.toggleEraser()
                            if (state.isHighlighter) viewModel.toggleHighlighter()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Pen",
                            tint = if (!state.isEraser && !state.isHighlighter) LocalVibeColors.current.brand else LocalVibeColors.current.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.toggleEraser() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.LayersClear, // Or similar for eraser
                            contentDescription = "Eraser",
                            tint = if (state.isEraser) LocalVibeColors.current.brand else LocalVibeColors.current.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.toggleHighlighter() }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.BorderColor,
                            contentDescription = "Highlighter",
                            tint = if (state.isHighlighter) LocalVibeColors.current.brand else LocalVibeColors.current.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolButton(
                        label = "Shape",
                        isActive = state.isShapeMode,
                        onClick = { viewModel.toggleShapeMode() }
                    )
                    IconButton(onClick = { 
                        showBackgroundPicker = !showBackgroundPicker 
                        showColorPicker = false
                    }) {
                        Icon(
                            Icons.Default.GridOn,
                            contentDescription = "Canvas background",
                            tint = if (state.canvasBackground != com.vibenote.app.domain.model.CanvasBackground.DARK) LocalVibeColors.current.brand else LocalVibeColors.current.textMuted
                        )
                    }
                }
            }

            // Color picker
            if (showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LocalVibeColors.current.surface)
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
                                    if (state.selectedColor == color) LocalVibeColors.current.brand else LocalVibeColors.current.borderStandard,
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
                        .background(LocalVibeColors.current.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        CanvasBackground.DARK,
                        CanvasBackground.WHITE,
                        CanvasBackground.LINED,
                        CanvasBackground.DOTTED,
                        CanvasBackground.GRID
                    ).forEach { type ->
                        BackgroundSwatch(
                            type = type,
                            isSelected = state.canvasBackground == type,
                            onClick = {
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
                    .background(LocalVibeColors.current.surface)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Size", color = LocalVibeColors.current.textMuted, modifier = Modifier.width(40.dp))
                Slider(
                    value = state.strokeWidth,
                    onValueChange = { viewModel.setStrokeWidth(it) },
                    valueRange = 2f..20f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = LocalVibeColors.current.brand,
                        activeTrackColor = LocalVibeColors.current.brand
                    )
                )
                Text(
                    "${state.strokeWidth.toInt()}",
                    color = LocalVibeColors.current.textMuted,
                    modifier = Modifier.width(30.dp)
                )
            }

// Canvas with gestures
Box(
    modifier = Modifier
        .fillMaxSize()
        .weight(1f)
) {
    if (state.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = LocalVibeColors.current.brand
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        canvasScale = 1f
                        canvasOffsetX = 0f
                        canvasOffsetY = 0f
                        viewModel.resetTransform()
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                                val down = awaitFirstDown()
                                var isTransforming = false
                                var points = listOf(down.position)
                                
                                val newStroke = Stroke(
                                    colorValue = state.selectedColor,
                                    strokeWidth = state.strokeWidth,
                                    isEraser = state.isEraser,
                                    isHighlighter = state.isHighlighter
                                )
                                viewModel.startStroke(newStroke)
                                currentPoints = points
                                currentPath = Path().apply { moveTo(down.position.x, down.position.y) }

                                var wasCanceled = false
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.isConsumed }) {
                                        wasCanceled = true
                                        break
                                    }
                                    
                                    if (event.changes.size > 1) {
                                        if (!isTransforming) {
                                            isTransforming = true
                                            currentPoints = emptyList()
                                            currentPath = Path()
                                        }
                                    }

                                    if (isTransforming) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        
                                        if (zoomChange != 1f || panChange != Offset.Zero) {
                                            canvasScale = (canvasScale * zoomChange).coerceIn(0.5f, 3f)
                                            canvasOffsetX += panChange.x
                                            canvasOffsetY += panChange.y
                                            viewModel.updateTransform(canvasScale, canvasOffsetX, canvasOffsetY)
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else {
                                        val change = event.changes.first()
                                        if (change.pressed) {
                                            points = points + change.position
                                            currentPoints = points
                                            currentPath.lineTo(change.position.x, change.position.y)
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (!wasCanceled && !isTransforming && points.size > 1) {
                                    if (state.isShapeMode && points.size >= 5) {
                                        val shapeStroke = viewModel.applyShapeRecognition(points)
                                        if (shapeStroke != null) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            viewModel.startStroke(shapeStroke)
                                        } else {
                                            viewModel.updateStroke(points)
                                        }
                                    } else {
                                        viewModel.updateStroke(points)
                                    }
                                    viewModel.finishStroke()
                                }
                                
                                currentPoints = emptyList()
                                currentPath = Path()
                            }
                        }
                ) {
                    val themeBgColor = LocalVibeColors.current.background
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
                            CanvasBackground.WHITE -> drawRect(Color.White, size = size)
                            CanvasBackground.LINED -> {
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
                            CanvasBackground.DOTTED -> {
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
                            CanvasBackground.GRID -> {
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
                            else -> {}
                        }
                        
                        val bgColor = when (state.canvasBackground) {
                            CanvasBackground.WHITE -> Color.White
                            else -> themeBgColor
                        }
                        
                        state.strokes.forEach { stroke ->
                            val pointsList = stroke.points
                            if (pointsList.size >= 2) {
                                val strokeColor = when {
                                    stroke.isEraser -> bgColor
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

                        if (currentPoints.isNotEmpty()) {
                            val renderColor = when {
                                state.isEraser -> bgColor
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
    }

    // Edit title dialog
    if (showEditTitleDialog) {
        var editedTitle by remember { mutableStateOf(state.noteTitle) }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Edit Title", color = LocalVibeColors.current.textPrimary) },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LocalVibeColors.current.textPrimary,
                        unfocusedTextColor = LocalVibeColors.current.textPrimary,
                        focusedBorderColor = LocalVibeColors.current.brand,
                        unfocusedBorderColor = LocalVibeColors.current.borderStandard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateTitle(editedTitle.ifBlank { "Untitled" })
                    showEditTitleDialog = false
                }) {
                    Text("Save", color = LocalVibeColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.background
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note", color = LocalVibeColors.current.textPrimary) },
            text = { Text("Are you sure you want to delete this note?", color = LocalVibeColors.current.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote { onNavigateBack() }
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.background
        )
    }

    // Clear Canvas confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Canvas", color = LocalVibeColors.current.textPrimary) },
            text = { Text("Remove all strokes? This can be undone.", color = LocalVibeColors.current.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCanvas()
                    showClearDialog = false
                }) {
                    Text("Clear", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.background
        )
    }

    // Export success dialog
    if (showExportDialog && exportedUri != null) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportedUri = null
            },
            title = { Text("Export Successful", color = LocalVibeColors.current.textPrimary) },
            text = { Text("Note exported to Pictures/VibeNote", color = LocalVibeColors.current.textMuted) },
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
                    Text("Share", color = LocalVibeColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportedUri = null
                }) {
                    Text("Close", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.background
        )
    }
}ssRequest = {
                showExportDialog = false
                exportedUri = null
            },
            title = { Text("Export Successful", color = LocalVibeColors.current.textPrimary) },
            text = { Text("Note exported to Pictures/VibeNote", color = LocalVibeColors.current.textMuted) },
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
                    Text("Share", color = LocalVibeColors.current.brand)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportedUri = null
                }) {
                    Text("Close", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.background
        )
    }
}