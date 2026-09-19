package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.TextPrimary

@Composable
fun CircularProgressRing(
    progressPercent: Float, // 0 to 100
    modifier: Modifier = Modifier,
    sizeDp: Dp = 80.dp,
    strokeWidthDp: Dp = 7.dp,
    trackColor: Color = ObsidianBorderSubtle,
    gradientColors: List<Color> = listOf(EmeraldGrowth, EmeraldLight),
    centerContent: (@Composable () -> Unit)? = null
) {
    val targetSweep = (progressPercent.coerceIn(0f, 100f) / 100f) * 360f
    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = tween(durationMillis = 800),
        label = "sweep_anim"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidthDp.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2f
            val center = this.center

            // Background Track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // Progress Arc
            if (animatedSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(gradientColors),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        if (centerContent != null) {
            centerContent()
        } else {
            Text(
                text = "${progressPercent.toInt()}%",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
