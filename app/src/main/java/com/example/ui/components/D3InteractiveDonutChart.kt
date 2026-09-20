package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Institutional D3-style Asset Allocation Data Structure
 */
data class D3AllocationSlice(
    val key: String,
    val name: String,
    val value: Double,
    val primaryColor: Color,
    val gradientColors: List<Color>,
    val holdingsCount: Int,
    val xirrReturnPercent: Double,
    val description: String,
    val underlyingAssets: List<String> = emptyList()
)

/**
 * Interactive D3-Style Donut Chart with touch arc selection, dynamic center telemetry HUD,
 * smooth sweep transitions, interactive legend pills, and granular asset class breakdowns.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun D3InteractiveDonutChart(
    slices: List<D3AllocationSlice>,
    totalPortfolioValue: Double,
    modifier: Modifier = Modifier,
    chartSize: Dp = 230.dp,
    baseStrokeWidth: Dp = 22.dp,
    selectedStrokeWidth: Dp = 32.dp,
    initialSelectedIndex: Int? = null,
    onSliceSelected: ((D3AllocationSlice?) -> Unit)? = null
) {
    var selectedIndex by remember(initialSelectedIndex) { mutableStateOf<Int?>(initialSelectedIndex) }
    val totalValue = remember(slices) { slices.sumOf { it.value }.takeIf { it > 0 } ?: 1.0 }

    // Smooth entry sweep animation
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val activeSlice = selectedIndex?.let { slices.getOrNull(it) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Interactive Donut Canvas with Touch Detection
        Box(
            modifier = Modifier
                .size(chartSize)
                .testTag("d3_interactive_donut_chart"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices, totalValue) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)
                            val minDim = kotlin.math.min(size.width, size.height).toFloat()
                            val radiusOuter = minDim / 2f
                            val radiusInner = radiusOuter - (selectedStrokeWidth.toPx() * 1.5f)

                            // Tap in the center resets selection
                            if (dist < radiusInner) {
                                selectedIndex = null
                                onSliceSelected?.invoke(null)
                                return@detectTapGestures
                            }

                            // Calculate angle in degrees [0..360] starting from -90 deg (12 o'clock)
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            // Adjust for -90 start
                            val normalizedAngle = (angle + 90f) % 360f

                            var currentStartAngle = 0f
                            var tappedIdx: Int? = null

                            for (i in slices.indices) {
                                val sweep = ((slices[i].value / totalValue) * 360.0).toFloat()
                                if (normalizedAngle >= currentStartAngle && normalizedAngle < currentStartAngle + sweep) {
                                    tappedIdx = i
                                    break
                                }
                                currentStartAngle += sweep
                            }

                            if (tappedIdx != null) {
                                selectedIndex = if (selectedIndex == tappedIdx) null else tappedIdx
                                onSliceSelected?.invoke(selectedIndex?.let { slices[it] })
                            }
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseStrokePx = baseStrokeWidth.toPx()
                val selectedStrokePx = selectedStrokeWidth.toPx()
                val baseRadius = (size.minDimension - selectedStrokePx) / 2f

                // Track start angle starting from top (-90 degrees)
                var currentStartAngle = -90f
                val sweepLimit = 360f * animProgress.value

                // Background subtle track
                drawCircle(
                    color = Color(0xFF131A26),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = baseStrokePx, cap = StrokeCap.Round)
                )

                slices.forEachIndexed { index, slice ->
                    val isSelected = selectedIndex == index
                    val isAnySelected = selectedIndex != null
                    val sliceProportion = (slice.value / totalValue).toFloat()
                    val targetSweepAngle = sliceProportion * 360f

                    // Clip sweep angle by entry animation progress
                    val startAngle = currentStartAngle
                    val availableSweep = (sweepLimit - (startAngle + 90f)).coerceAtLeast(0f)
                    val sweepAngle = targetSweepAngle.coerceAtMost(availableSweep)

                    if (sweepAngle > 0f) {
                        val strokeW = if (isSelected) selectedStrokePx else if (isAnySelected) baseStrokePx * 0.85f else baseStrokePx
                        val currentRadius = if (isSelected) baseRadius + 3.dp.toPx() else baseRadius
                        val gap = if (slices.size > 1) 2.5f else 0f
                        val actualSweep = (sweepAngle - gap).coerceAtLeast(0.5f)

                        val alpha = if (isSelected) 1f else if (isAnySelected) 0.35f else 0.95f
                        val sliceColor = slice.primaryColor.copy(alpha = alpha)

                        // Outer glowing aura for selected slice (D3 aesthetic)
                        if (isSelected) {
                            drawArc(
                                color = slice.primaryColor.copy(alpha = 0.25f),
                                startAngle = startAngle + (gap / 2f),
                                sweepAngle = actualSweep,
                                useCenter = false,
                                topLeft = Offset(center.x - currentRadius - (strokeW * 0.4f), center.y - currentRadius - (strokeW * 0.4f)),
                                size = Size((currentRadius + (strokeW * 0.4f)) * 2f, (currentRadius + (strokeW * 0.4f)) * 2f),
                                style = Stroke(width = strokeW * 1.6f, cap = StrokeCap.Round)
                            )
                        }

                        // Main Arc
                        drawArc(
                            color = sliceColor,
                            startAngle = startAngle + (gap / 2f),
                            sweepAngle = actualSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - currentRadius, center.y - currentRadius),
                            size = Size(currentRadius * 2f, currentRadius * 2f),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    currentStartAngle += targetSweepAngle
                }
            }

            // Center Dynamic Telemetry HUD
            Box(
                modifier = Modifier
                    .size(chartSize - (selectedStrokeWidth * 2.2f))
                    .clip(CircleShape)
                    .background(Color(0xFF0C111A))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        selectedIndex = null
                        onSliceSelected?.invoke(null)
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = activeSlice,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                    label = "center_hud_animation"
                ) { slice ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (slice == null) {
                            Text(
                                text = "TOTAL PORTFOLIO",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${"%,.0f".format(totalPortfolioValue)}",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(EmeraldGrowth.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "100% • 3 Asset Classes",
                                    color = EmeraldLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            val percent = (slice.value / totalValue) * 100.0
                            Text(
                                text = slice.name.uppercase(),
                                color = slice.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${"%,.0f".format(slice.value)}",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(slice.primaryColor.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (slice.xirrReturnPercent != 0.0) "${"%.1f".format(percent)}% • +${slice.xirrReturnPercent}% XIRR" else "${"%.1f".format(percent)}% Allocation",
                                    color = slice.primaryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Legend Filter Chips (Equities, Mutual Funds, Gold, etc.)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEachIndexed { idx, slice ->
                val isSelected = selectedIndex == idx
                val percent = (slice.value / totalValue) * 100.0

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedIndex = if (selectedIndex == idx) null else idx
                            onSliceSelected?.invoke(selectedIndex?.let { slices[it] })
                        }
                        .testTag("legend_chip_${slice.key.lowercase()}"),
                    color = if (isSelected) slice.primaryColor.copy(alpha = 0.22f) else ObsidianSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) slice.primaryColor else ObsidianBorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(slice.primaryColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = slice.name,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${"%.0f".format(percent)}%",
                            color = slice.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Granular Breakdown HUD Card for Selected Asset Class
        activeSlice?.let { slice ->
            val percent = (slice.value / totalValue) * 100.0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF101725),
                border = BorderStroke(1.dp, slice.primaryColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(slice.primaryColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${slice.name} Deep Dive",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "$${"%,.2f".format(slice.value)} (${"%.1f".format(percent)}%)",
                            color = slice.primaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = slice.description,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    if (slice.underlyingAssets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "UNDERLYING HOLDINGS & VEHICLES",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            slice.underlyingAssets.forEach { asset ->
                                Surface(
                                    color = Color(0xFF182234),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, ObsidianBorderSubtle)
                                ) {
                                    Text(
                                        text = asset,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
