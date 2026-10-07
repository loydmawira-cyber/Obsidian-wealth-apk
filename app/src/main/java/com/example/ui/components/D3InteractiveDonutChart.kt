package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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

private data class DonutLabelPlacement(
    val index: Int,
    val x: Float,      // dp, top-left of label box
    val y: Float,      // dp, top-left of label box
    val side: Int,     // 1 = right of ring, -1 = left, 0 = top/bottom
    val vAnchor: Int   // for side == 0: -1 = above ring (bottom aligned), 1 = below ring (top aligned)
)

/** Places one label per slice just outside the ring and nudges labels apart so they don't overlap. */
private fun computeLabelPlacements(
    slices: List<D3AllocationSlice>,
    totalValue: Double,
    cx: Float,
    cy: Float,
    labelR: Float,
    labelW: Float,
    labelH: Float,
    boxW: Float,
    boxH: Float
): List<DonutLabelPlacement> {
    class Raw(val index: Int, val side: Int, val vAnchor: Int, var x: Float, var y: Float)

    var start = 0f
    val raws = slices.mapIndexed { i, s ->
        val sweep = ((s.value / totalValue) * 360.0).toFloat()
        val mid = start + sweep / 2f
        start += sweep
        val rad = Math.toRadians((mid - 90f).toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val side = when {
            cosA > 0.25f -> 1
            cosA < -0.25f -> -1
            else -> 0
        }
        val ax = cx + cosA * labelR
        val ay = cy + sinA * labelR
        val vAnchor = if (side != 0) 0 else if (sinA < 0f) -1 else 1
        val y = when (vAnchor) {
            -1 -> ay - labelH
            1 -> ay
            else -> ay - labelH / 2f
        }
        val x = when (side) {
            1 -> ax
            -1 -> ax - labelW
            else -> ax - labelW / 2f
        }
        Raw(i, side, vAnchor, x, y)
    }

    val gap = 2f
    // Labels on the left/right: spread vertically so they never overlap.
    listOf(-1, 1).forEach { sd ->
        val group = raws.filter { it.side == sd }.sortedBy { it.y }
        group.forEachIndexed { k, r ->
            r.y = r.y.coerceIn(0f, boxH - labelH)
            if (k > 0) r.y = max(r.y, group[k - 1].y + labelH + gap)
        }
        for (k in group.indices.reversed()) {
            val limit = if (k == group.lastIndex) boxH - labelH else group[k + 1].y - labelH - gap
            group[k].y = min(group[k].y, limit)
        }
    }
    // Labels above/below the ring: spread horizontally so neighbours don't sit on top of each other.
    listOf(-1, 1).forEach { va ->
        val group = raws.filter { it.side == 0 && it.vAnchor == va }.sortedBy { it.x }
        group.forEachIndexed { k, r ->
            if (k > 0) r.x = max(r.x, group[k - 1].x + labelW + gap)
        }
    }

    return raws.map {
        DonutLabelPlacement(it.index, it.x.coerceIn(0f, max(0f, boxW - labelW)), it.y, it.side, it.vAnchor)
    }
}

private fun formatPercent(p: Double): String = "%.1f".format(p).removeSuffix(".0") + "%"

/**
 * Allocation donut: thick segmented ring, outside labels with leader dots (name + bold % in the
 * slice colour), and a bold total in the centre. Tap a segment or its label to focus it; tap the
 * centre to reset.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun D3InteractiveDonutChart(
    slices: List<D3AllocationSlice>,
    totalPortfolioValue: Double,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    formatAmount: ((Double) -> String)? = null,
    chartSize: Dp = 230.dp,
    ringThicknessFraction: Float = 0.20f,
    selectedGrow: Dp = 8.dp,
    centerTitle: String = "TOTAL PORTFOLIO",
    aggregateDetail: String? = null,
    initialSelectedIndex: Int? = null,
    onSliceSelected: ((D3AllocationSlice?) -> Unit)? = null
) {
    var selectedIndex by remember(initialSelectedIndex) { mutableStateOf<Int?>(initialSelectedIndex) }
    val totalValue = remember(slices) { slices.sumOf { it.value }.takeIf { it > 0 } ?: 1.0 }

    // Entry sweep animation
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val activeSlice = selectedIndex?.let { slices.getOrNull(it) }

    fun select(idx: Int?) {
        selectedIndex = if (idx == null || selectedIndex == idx) null else idx
        onSliceSelected?.invoke(selectedIndex?.let { slices.getOrNull(it) })
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val labelW = 58.dp
            val labelH = 38.dp
            val anchorGap = 10.dp
            val vMargin = labelH + anchorGap

            val ringD = min(chartSize.value, (maxWidth - (labelW + anchorGap) * 2).value)
                .coerceAtLeast(120f).dp
            val outerR = ringD / 2
            val thickness = ringD * ringThicknessFraction
            val boxW = maxWidth
            val boxH = ringD + vMargin * 2

            val placements = remember(slices, totalValue, boxW, ringD) {
                computeLabelPlacements(
                    slices = slices,
                    totalValue = totalValue,
                    cx = boxW.value / 2f,
                    cy = boxH.value / 2f,
                    labelR = outerR.value + anchorGap.value,
                    labelW = labelW.value,
                    labelH = labelH.value,
                    boxW = boxW.value,
                    boxH = boxH.value
                )
            }

            Box(
                modifier = Modifier
                    .width(boxW)
                    .height(boxH)
                    .testTag("d3_interactive_donut_chart"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(slices, totalValue, ringD) {
                            detectTapGestures { offset ->
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - c.x
                                val dy = offset.y - c.y
                                val dist = sqrt(dx * dx + dy * dy)
                                val outerPx = outerR.toPx()
                                val innerPx = outerPx - thickness.toPx()
                                val slop = 6.dp.toPx()

                                if (dist < innerPx - slop) {
                                    select(null)
                                    return@detectTapGestures
                                }
                                if (dist > outerPx + selectedGrow.toPx() + slop) return@detectTapGestures

                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                val normalized = (angle + 90f) % 360f

                                var start = 0f
                                for (i in slices.indices) {
                                    val sweep = ((slices[i].value / totalValue) * 360.0).toFloat()
                                    if (normalized >= start && normalized < start + sweep) {
                                        select(i)
                                        break
                                    }
                                    start += sweep
                                }
                            }
                        }
                ) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val outerPx = outerR.toPx()
                    val thicknessPx = thickness.toPx()
                    val growPx = selectedGrow.toPx()
                    val sweepLimit = 360f * animProgress.value
                    val gapDeg = if (slices.size > 1) 1.6f else 0f
                    val anyOneSelected = selectedIndex != null

                    var start = 0f
                    slices.forEachIndexed { index, slice ->
                        val isSelected = selectedIndex == index
                        val target = ((slice.value / totalValue) * 360.0).toFloat()
                        val available = (sweepLimit - start).coerceAtLeast(0f)
                        val sweep = min(target, available)

                        if (sweep > 0f) {
                            val strokeW = if (isSelected) thicknessPx + growPx else thicknessPx
                            // Selected segment grows outward; its inner edge stays put.
                            val midR = outerPx - thicknessPx / 2f + if (isSelected) growPx / 2f else 0f
                            val alpha = if (isSelected || !anyOneSelected) 1f else 0.35f
                            val drawSweep = (sweep - gapDeg).coerceAtLeast(0.5f)

                            drawArc(
                                color = slice.primaryColor.copy(alpha = alpha),
                                startAngle = -90f + start + gapDeg / 2f,
                                sweepAngle = drawSweep,
                                useCenter = false,
                                topLeft = Offset(c.x - midR, c.y - midR),
                                size = Size(midR * 2f, midR * 2f),
                                style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                            )

                            // Leader dot + short line out to the label
                            if (sweep >= target - 0.01f) {
                                val rad = Math.toRadians((-90f + start + target / 2f).toDouble())
                                val dirX = cos(rad).toFloat()
                                val dirY = sin(rad).toFloat()
                                val r0 = outerPx + 3.dp.toPx() + if (isSelected) growPx else 0f
                                val r1 = outerPx + anchorGap.toPx() - 1.dp.toPx()
                                val lineColor = slice.primaryColor.copy(alpha = if (isSelected || !anyOneSelected) 0.9f else 0.35f)
                                drawLine(
                                    color = lineColor,
                                    start = Offset(c.x + dirX * r0, c.y + dirY * r0),
                                    end = Offset(c.x + dirX * r1, c.y + dirY * r1),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawCircle(
                                    color = lineColor,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(c.x + dirX * r0, c.y + dirY * r0)
                                )
                            }
                        }
                        start += target
                    }
                }

                // Outside labels: name + bold percent in the slice colour
                val labelAlpha = ((animProgress.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                placements.forEach { p ->
                    val slice = slices[p.index]
                    val isSelected = selectedIndex == p.index
                    val dimmed = selectedIndex != null && !isSelected
                    val percent = (slice.value / totalValue) * 100.0
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = p.x.dp, y = p.y.dp)
                            .width(labelW)
                            .height(labelH)
                            .alpha(labelAlpha * if (dimmed) 0.45f else 1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { select(p.index) }
                            .testTag("donut_label_${slice.key.lowercase()}"),
                        horizontalAlignment = when (p.side) {
                            1 -> Alignment.Start
                            -1 -> Alignment.End
                            else -> Alignment.CenterHorizontally
                        },
                        verticalArrangement = when {
                            p.side != 0 -> Arrangement.Center
                            p.vAnchor < 0 -> Arrangement.Bottom
                            else -> Arrangement.Top
                        }
                    ) {
                        val align = when (p.side) {
                            1 -> TextAlign.Start
                            -1 -> TextAlign.End
                            else -> TextAlign.Center
                        }
                        Text(
                            text = slice.name,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            textAlign = align,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatPercent(percent),
                            color = slice.primaryColor,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = align
                        )
                    }
                }

                // Centre hub: bold total, gold hairline ring
                Box(
                    modifier = Modifier
                        .size(ringD - thickness * 2 - 6.dp)
                        .clip(CircleShape)
                        .background(ObsidianBg)
                        .border(1.5.dp, SovereignGold.copy(alpha = 0.85f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { select(null) }
                        .padding(8.dp),
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
                                    text = centerTitle,
                                    color = TextPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 11.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val totalText = formatAmount?.invoke(totalPortfolioValue)
                                    ?: "$currencySymbol${"%,.0f".format(totalPortfolioValue)}"
                                Text(
                                    text = totalText,
                                    color = SovereignGold,
                                    fontSize = if (totalText.length > 11) 14.sp else 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                                if (aggregateDetail != null) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = aggregateDetail,
                                        color = TextMuted,
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            } else {
                                val percent = (slice.value / totalValue) * 100.0
                                Text(
                                    text = slice.name.uppercase(),
                                    color = slice.primaryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 11.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val sliceText = formatAmount?.invoke(slice.value)
                                    ?: "$currencySymbol${"%,.0f".format(slice.value)}"
                                Text(
                                    text = sliceText,
                                    color = TextPrimary,
                                    fontSize = if (sliceText.length > 11) 14.sp else 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (slice.xirrReturnPercent != 0.0)
                                        "${formatPercent(percent)} • +${slice.xirrReturnPercent}% XIRR"
                                    else "${formatPercent(percent)} of portfolio",
                                    color = slice.primaryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (activeSlice == null) {
            Text(
                text = "Tap a segment for details",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Granular breakdown card for the selected asset class
        activeSlice?.let { slice ->
            val percent = (slice.value / totalValue) * 100.0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ObsidianSurface,
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

                        val deepDiveAmountText = formatAmount?.invoke(slice.value)
                            ?: "$currencySymbol${"%,.2f".format(slice.value)}"
                        Text(
                            text = "$deepDiveAmountText (${formatPercent(percent)})",
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
                                    color = ObsidianSurfaceVariant,
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
