package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class DonutSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 140.dp,
    strokeWidthDp: Dp = 16.dp,
    centerTitle: String = "Total",
    centerSubtitle: String = "$3,820"
) {
    val totalValue = slices.sumOf { it.value }.takeIf { it > 0 } ?: 1.0

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidthDp.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2f
            var currentStartAngle = -90f

            slices.forEach { slice ->
                val sweepAngle = ((slice.value / totalValue) * 360f).toFloat()
                if (sweepAngle > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = currentStartAngle,
                        sweepAngle = (sweepAngle - 2f).coerceAtLeast(0.5f), // subtle gap
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                    )
                    currentStartAngle += sweepAngle
                }
            }
        }

        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = centerTitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = centerSubtitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
