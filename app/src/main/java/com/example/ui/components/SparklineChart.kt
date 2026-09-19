package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight

@Composable
fun SparklineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = EmeraldGrowth,
    gradientStart: Color = EmeraldLight.copy(alpha = 0.35f),
    gradientEnd: Color = Color.Transparent,
    strokeWidth: Float = 5f
) {
    if (points.isEmpty()) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 1f
        val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

        val stepX = width / (points.size - 1).coerceAtLeast(1)

        val strokePath = Path()
        val fillPath = Path()

        val firstY = height - ((points[0] - minVal) / range * height * 0.8f + height * 0.1f)
        strokePath.moveTo(0f, firstY)
        fillPath.moveTo(0f, height)
        fillPath.lineTo(0f, firstY)

        for (i in 1 until points.size) {
            val x = i * stepX
            val y = height - ((points[i] - minVal) / range * height * 0.8f + height * 0.1f)
            
            // Cubic bezier smoothing
            val prevX = (i - 1) * stepX
            val prevY = height - ((points[i - 1] - minVal) / range * height * 0.8f + height * 0.1f)
            val cx1 = prevX + (x - prevX) / 2
            val cy1 = prevY
            val cx2 = prevX + (x - prevX) / 2
            val cy2 = y

            strokePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
            fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw gradient area below curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientStart, gradientEnd),
                startY = 0f,
                endY = height
            )
        )

        // Draw line stroke
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
