package com.vibenote.app.presentation.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.scale
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.model.StrokeType
import kotlin.math.hypot

@Composable
fun CanvasPreview(
    strokes: List<Stroke>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (strokes.isEmpty()) return@Canvas

        // Assume original canvas size is roughly 1080x1920 or similar.
        // For preview, we scale everything to fit the current canvas size.
        // We find the bounding box of all strokes to center and scale them.
        
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        strokes.forEach { stroke ->
            stroke.points.forEach { point ->
                minX = minOf(minX, point.x)
                minY = minOf(minY, point.y)
                maxX = maxOf(maxX, point.x)
                maxY = maxOf(maxY, point.y)
            }
        }

        if (minX == Float.MAX_VALUE) return@Canvas

        val contentWidth = maxX - minX
        val contentHeight = maxY - minY
        
        val scaleX = if (contentWidth > 0) size.width / contentWidth else 1f
        val scaleY = if (contentHeight > 0) size.height / contentHeight else 1f
        val scale = minOf(scaleX, scaleY) * 0.8f // 80% fit

        val offsetX = (size.width - contentWidth * scale) / 2f - minX * scale
        val offsetY = (size.height - contentHeight * scale) / 2f - minY * scale

        strokes.forEach { stroke ->
            val pointsList = stroke.points
            if (pointsList.size >= 2) {
                val strokeColor = if (stroke.isHighlighter) {
                    Color(stroke.colorValue).copy(alpha = 0.4f)
                } else {
                    Color(stroke.colorValue)
                }
                // Don't render erasers in preview for simplicity, or render as background color
                if (stroke.isEraser) return@forEach
                
                val scaledWidth = stroke.strokeWidth * scale * (if (stroke.isHighlighter) 3f else 1f)

                when (stroke.strokeType) {
                    StrokeType.CIRCLE -> {
                        val center = Offset(pointsList.first().x * scale + offsetX, pointsList.first().y * scale + offsetY)
                        val radius = hypot(
                            (pointsList.last().x - pointsList.first().x) * scale,
                            (pointsList.last().y - pointsList.first().y) * scale
                        )
                        drawCircle(
                            color = strokeColor,
                            radius = radius,
                            center = center,
                            style = DrawStroke(width = scaledWidth)
                        )
                    }
                    StrokeType.RECTANGLE -> {
                        val topLeft = Offset(pointsList.first().x * scale + offsetX, pointsList.first().y * scale + offsetY)
                        val bottomRight = Offset(pointsList.last().x * scale + offsetX, pointsList.last().y * scale + offsetY)
                        drawRect(
                            color = strokeColor,
                            topLeft = topLeft,
                            size = Size(
                                bottomRight.x - topLeft.x,
                                bottomRight.y - topLeft.y
                            ),
                            style = DrawStroke(width = scaledWidth)
                        )
                    }
                    StrokeType.LINE -> {
                        drawLine(
                            color = strokeColor,
                            start = Offset(pointsList.first().x * scale + offsetX, pointsList.first().y * scale + offsetY),
                            end = Offset(pointsList.last().x * scale + offsetX, pointsList.last().y * scale + offsetY),
                            strokeWidth = scaledWidth,
                            cap = StrokeCap.Round
                        )
                    }
                    else -> {
                        val path = Path()
                        val firstPoint = pointsList.first()
                        path.moveTo(firstPoint.x * scale + offsetX, firstPoint.y * scale + offsetY)
                        
                        for (i in 1 until pointsList.size - 1) {
                            val p1 = pointsList[i]
                            val p2 = pointsList[i+1]
                            val midX = ((p1.x + p2.x) / 2f) * scale + offsetX
                            val midY = ((p1.y + p2.y) / 2f) * scale + offsetY
                            path.quadraticBezierTo(
                                p1.x * scale + offsetX, 
                                p1.y * scale + offsetY, 
                                midX, 
                                midY
                            )
                        }
                        val lastPoint = pointsList.last()
                        path.lineTo(lastPoint.x * scale + offsetX, lastPoint.y * scale + offsetY)

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = DrawStroke(
                                width = scaledWidth,
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
