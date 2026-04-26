package com.vibenote.app.presentation.canvas

import androidx.compose.ui.geometry.Offset
import com.vibenote.app.domain.model.StrokeType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class ShapeRecognitionResult(
    val strokeType: StrokeType,
    val startPoint: Offset,
    val endPoint: Offset,
    val center: Offset,
    val bounds: Pair<Offset, Offset>,
    val confidence: Float
)

object ShapeRecognitionHelper {
    private const val MIN_CONFIDENCE = 0.7f
    private const val ANGLE_TOLERANCE = 25f
    private const val CLOSED_SHAPE_RATIO = 0.4f
    private const val MIN_POINTS = 10

    fun recognize(points: List<Offset>): ShapeRecognitionResult? {
        if (points.size < MIN_POINTS) {
            return null
        }

        val first = points.first()
        val last = points.last()
        val distance = hypot(last.x - first.x, last.y - first.y)
        
        val allPoints = points.map { Offset(it.x, it.y) }
        val pathLength = calculatePathLength(allPoints)
        
        if (pathLength < 50f) {
            return null
        }

        val straightness = calculateStraightness(allPoints)
        
        val isLine = straightness > 0.85f && isLinear(allPoints)
        if (isLine) {
            return ShapeRecognitionResult(
                strokeType = StrokeType.LINE,
                startPoint = first,
                endPoint = last,
                center = Offset((first.x + last.x) / 2, (first.y + last.y) / 2),
                bounds = Pair(first, last),
                confidence = straightness
            )
        }

        val circleFit = fitCircle(allPoints)
        if (circleFit != null && circleFit.confidence > MIN_CONFIDENCE) {
            return ShapeRecognitionResult(
                strokeType = StrokeType.CIRCLE,
                startPoint = first,
                endPoint = last,
                center = circleFit.center,
                bounds = Pair(
                    Offset(circleFit.center.x - circleFit.radius, circleFit.center.y - circleFit.radius),
                    Offset(circleFit.center.x + circleFit.radius, circleFit.center.y + circleFit.radius)
                ),
                confidence = circleFit.confidence
            )
        }

        val rectFit = fitRectangle(allPoints)
        if (rectFit != null && rectFit.confidence > MIN_CONFIDENCE) {
            return ShapeRecognitionResult(
                strokeType = StrokeType.RECTANGLE,
                startPoint = rectFit.topLeft,
                endPoint = rectFit.bottomRight,
                center = Offset(
                    (rectFit.topLeft.x + rectFit.bottomRight.x) / 2,
                    (rectFit.topLeft.y + rectFit.bottomRight.y) / 2
                ),
                bounds = Pair(rectFit.topLeft, rectFit.bottomRight),
                confidence = rectFit.confidence
            )
        }

        return null
    }

    private fun calculatePathLength(points: List<Offset>): Float {
        var length = 0f
        for (i in 1 until points.size) {
            length += hypot(points[i].x - points[i-1].x, points[i].y - points[i-1].y)
        }
        return length
    }

    private fun calculateStraightness(points: List<Offset>): Float {
        val first = points.first()
        val last = points.last()
        val directDistance = hypot(last.x - first.x, last.y - first.y)
        val pathLength = calculatePathLength(points)
        
        return if (pathLength > 0) directDistance / pathLength else 0f
    }

    private fun isLinear(points: List<Offset>): Boolean {
        if (points.size < 3) return true
        
        val first = points.first()
        val last = points.last()
        
        for (point in points) {
            val distance = pointToLineDistance(point, first, last)
            val pathLength = calculatePathLength(points)
            if (distance / pathLength > 0.15f) {
                return false
            }
        }
        return true
    }

    private fun pointToLineDistance(point: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0f) {
            return hypot(point.x - lineStart.x, point.y - lineStart.y)
        }
        
        val t = maxOf(0f, minOf(1f,
            ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lengthSquared
        ))
        
        val projX = lineStart.x + t * dx
        val projY = lineStart.y + t * dy
        
        return hypot(point.x - projX, point.y - projY)
    }

    data class CircleFit(val center: Offset, val radius: Float, val confidence: Float)

    private fun fitCircle(points: List<Offset>): CircleFit? {
        if (points.size < 5) return null
        
        val avgX = points.map { it.x }.average().toFloat()
        val avgY = points.map { it.y }.average().toFloat()
        val center = Offset(avgX, avgY)
        
        var avgRadius = 0f
        for (point in points) {
            avgRadius += hypot(point.x - center.x, point.y - center.y)
        }
        avgRadius /= points.size
        
        if (avgRadius < 10f) return null
        
        var radialVariance = 0f
        for (point in points) {
            val radius = hypot(point.x - center.x, point.y - center.y)
            radialVariance += kotlin.math.abs(radius - avgRadius)
        }
        radialVariance /= points.size
        
        val first = points.first()
        val last = points.last()
        val closure = hypot(last.x - first.x, last.y - first.y)
        val closureRatio = closure / avgRadius
        
        val confidence = 1f - (radialVariance / avgRadius) * 0.5f
        val closureBonus = if (closureRatio < CLOSED_SHAPE_RATIO) 0.2f else 0f
        
        return CircleFit(center, avgRadius, minOf(1f, confidence + closureBonus))
    }

    data class RectangleFit(val topLeft: Offset, val bottomRight: Offset, val confidence: Float)

    private fun fitRectangle(points: List<Offset>): RectangleFit? {
        if (points.size < 5) return null
        
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        
        val width = maxX - minX
        val height = maxY - minY
        
        if (width < 20f || height < 20f) return null
        
        val aspectRatio = width / height
        val isRectangular = aspectRatio > 0.5f && aspectRatio < 2f
        
        if (!isRectangular) return null
        
        val first = points.first()
        val last = points.last()
        val closure = hypot(last.x - first.x, last.y - first.y)
        val perimeter = 2 * (width + height)
        val closureRatio = closure / perimeter
        
        val cornerCount = countCorners(points)
        val confidence = when {
            cornerCount >= 3 && closureRatio < 0.3f -> 0.85f
            cornerCount >= 2 && closureRatio < 0.4f -> 0.75f
            closureRatio < 0.2f -> 0.7f
            else -> return null
        }
        
        return RectangleFit(
            Offset(minX, minY),
            Offset(maxX, maxY),
            confidence
        )
    }

    private fun countCorners(points: List<Offset>): Int {
        if (points.size < 6) return 0
        
        var corners = 0
        val windowSize = points.size / 8
        
        for (i in windowSize until points.size - windowSize) {
            val prev = points[i - windowSize]
            val curr = points[i]
            val next = points[i + windowSize]
            
            val angle1 = atan2((curr.y - prev.y).toDouble(), (curr.x - prev.x).toDouble())
            val angle2 = atan2((next.y - curr.y).toDouble(), (next.x - curr.x).toDouble())
            var angleDiff = kotlin.math.abs(angle2 - angle1)
            if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff
            val angleDiffFloat = Math.toDegrees(angleDiff).toFloat()
            
            if (angleDiffFloat > 45f) {
                corners++
            }
        }
        
        return corners
    }
}