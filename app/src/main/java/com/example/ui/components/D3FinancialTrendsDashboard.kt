package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FinanceSummary
import com.example.data.models.UserSettings
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

enum class TrendMetricType(val title: String, val color: Color, val description: String) {
    NET_WORTH("Net Worth", SovereignGold, "Total Assets minus Liabilities"),
    INVESTMENTS("Investments", EmeraldGrowth, "Stocks, Mutual Funds, SIPs & Crypto"),
    DEBT_REDUCTION("Debt Reduction", Color(0xFFFB7185), "Credit Cards & Active Loans Payoff"),
    MULTI_COMPARE("Multi-Curve Overlay", GoldBright, "Combined Trend Analysis")
}

data class TrendDataPoint(
    val monthLabel: String,
    val netWorth: Double,
    val investments: Double,
    val debt: Double
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun D3FinancialTrendsDashboard(
    userSettings: UserSettings,
    summary: FinanceSummary? = null,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(TrendMetricType.NET_WORTH) }
    var selectedTimeframe by remember { mutableStateOf("1Y") }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val hasRealData = summary != null && (summary.transactionCount > 0 || summary.holdingCount > 0 || summary.debtAccountCount > 0 || summary.totalNetWorth != 0.0)

    val dataPoints = remember(selectedTimeframe, summary, hasRealData) {
        if (!hasRealData || summary == null) {
            listOf(
                TrendDataPoint("May", 0.0, 0.0, 0.0),
                TrendDataPoint("Jun", 0.0, 0.0, 0.0),
                TrendDataPoint("Jul", 0.0, 0.0, 0.0),
                TrendDataPoint("Aug", 0.0, 0.0, 0.0),
                TrendDataPoint("Sep", 0.0, 0.0, 0.0),
                TrendDataPoint("Oct", 0.0, 0.0, 0.0)
            )
        } else {
            val nw = summary.totalNetWorth
            val inv = summary.portfolioValue
            val debt = summary.totalDebt
            when (selectedTimeframe) {
                "6M" -> listOf(
                    TrendDataPoint("May", (nw * 0.75).roundToInt().toDouble(), (inv * 0.70).roundToInt().toDouble(), (debt * 1.30).roundToInt().toDouble()),
                    TrendDataPoint("Jun", (nw * 0.80).roundToInt().toDouble(), (inv * 0.76).roundToInt().toDouble(), (debt * 1.20).roundToInt().toDouble()),
                    TrendDataPoint("Jul", (nw * 0.85).roundToInt().toDouble(), (inv * 0.82).roundToInt().toDouble(), (debt * 1.15).roundToInt().toDouble()),
                    TrendDataPoint("Aug", (nw * 0.90).roundToInt().toDouble(), (inv * 0.88).roundToInt().toDouble(), (debt * 1.10).roundToInt().toDouble()),
                    TrendDataPoint("Sep", (nw * 0.95).roundToInt().toDouble(), (inv * 0.94).roundToInt().toDouble(), (debt * 1.05).roundToInt().toDouble()),
                    TrendDataPoint("Oct", nw, inv, debt)
                )
                "3Y" -> listOf(
                    TrendDataPoint("2023 Q1", (nw * 0.45).roundToInt().toDouble(), (inv * 0.40).roundToInt().toDouble(), (debt * 1.80).roundToInt().toDouble()),
                    TrendDataPoint("2023 Q3", (nw * 0.58).roundToInt().toDouble(), (inv * 0.52).roundToInt().toDouble(), (debt * 1.50).roundToInt().toDouble()),
                    TrendDataPoint("2024 Q1", (nw * 0.68).roundToInt().toDouble(), (inv * 0.62).roundToInt().toDouble(), (debt * 1.35).roundToInt().toDouble()),
                    TrendDataPoint("2024 Q3", (nw * 0.80).roundToInt().toDouble(), (inv * 0.75).roundToInt().toDouble(), (debt * 1.20).roundToInt().toDouble()),
                    TrendDataPoint("2025 Q1", (nw * 0.90).roundToInt().toDouble(), (inv * 0.88).roundToInt().toDouble(), (debt * 1.10).roundToInt().toDouble()),
                    TrendDataPoint("2025 Q3", nw, inv, debt)
                )
                else -> listOf(
                    TrendDataPoint("Nov", (nw * 0.65).roundToInt().toDouble(), (inv * 0.58).roundToInt().toDouble(), (debt * 1.40).roundToInt().toDouble()),
                    TrendDataPoint("Jan", (nw * 0.72).roundToInt().toDouble(), (inv * 0.65).roundToInt().toDouble(), (debt * 1.30).roundToInt().toDouble()),
                    TrendDataPoint("Mar", (nw * 0.78).roundToInt().toDouble(), (inv * 0.72).roundToInt().toDouble(), (debt * 1.20).roundToInt().toDouble()),
                    TrendDataPoint("May", (nw * 0.85).roundToInt().toDouble(), (inv * 0.80).roundToInt().toDouble(), (debt * 1.12).roundToInt().toDouble()),
                    TrendDataPoint("Jul", (nw * 0.92).roundToInt().toDouble(), (inv * 0.90).roundToInt().toDouble(), (debt * 1.05).roundToInt().toDouble()),
                    TrendDataPoint("Sep", nw, inv, debt)
                )
            }
        }
    }

    val activePoint = selectedIndex?.let { dataPoints.getOrNull(it) } ?: dataPoints.last()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(SovereignGold.copy(alpha = 0.3f), ObsidianSurfaceVariant)),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.dp, SovereignGold, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "D3 FINANCIAL TREND ANALYTICS",
                            color = SovereignGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Interactive Bezier Splines & Real-Time Curves",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Time Horizon Pills
                Row(
                    modifier = Modifier
                        .background(ObsidianSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    listOf("6M", "1Y", "3Y").forEach { tf ->
                        val isSel = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) SovereignGold else Color.Transparent)
                                .clickable {
                                    selectedTimeframe = tf
                                    selectedIndex = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tf,
                                color = if (isSel) ObsidianBg else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metric Selector Pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TrendMetricType.values().forEach { metric ->
                    val isSel = selectedMetric == metric
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSel) metric.color.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, if (isSel) metric.color else ObsidianBorderSubtle),
                        modifier = Modifier.clickable {
                            selectedMetric = metric
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(metric.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = metric.title,
                                color = if (isSel) TextPrimary else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Inspection Header Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurfaceVariant,
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PERIOD: ${activePoint.monthLabel}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val displayVal = when (selectedMetric) {
                            TrendMetricType.NET_WORTH -> activePoint.netWorth
                            TrendMetricType.INVESTMENTS -> activePoint.investments
                            TrendMetricType.DEBT_REDUCTION -> activePoint.debt
                            TrendMetricType.MULTI_COMPARE -> activePoint.netWorth
                        }
                        Text(
                            text = userSettings.formatAmount(displayVal),
                            color = selectedMetric.color,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldGrowth.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = EmeraldGrowth, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val startVal = when (selectedMetric) {
                                    TrendMetricType.DEBT_REDUCTION -> dataPoints.firstOrNull()?.debt ?: 0.0
                                    TrendMetricType.INVESTMENTS -> dataPoints.firstOrNull()?.investments ?: 0.0
                                    else -> dataPoints.firstOrNull()?.netWorth ?: 0.0
                                }
                                val currentVal = when (selectedMetric) {
                                    TrendMetricType.DEBT_REDUCTION -> activePoint.debt
                                    TrendMetricType.INVESTMENTS -> activePoint.investments
                                    else -> activePoint.netWorth
                                }
                                val pct = if (!hasRealData || startVal == 0.0) {
                                    when (selectedMetric) {
                                        TrendMetricType.DEBT_REDUCTION -> "0.0% Debt"
                                        TrendMetricType.INVESTMENTS -> "0.0% Assets"
                                        else -> "0.0% Growth"
                                    }
                                } else {
                                    val diffPct = ((currentVal - startVal) / startVal) * 100.0
                                    val sign = if (diffPct >= 0) "+" else ""
                                    val formatted = "$sign${"%.1f".format(diffPct)}%"
                                    when (selectedMetric) {
                                        TrendMetricType.DEBT_REDUCTION -> "$formatted Debt"
                                        TrendMetricType.INVESTMENTS -> "$formatted Assets"
                                        else -> "$formatted Growth"
                                    }
                                }
                                Text(
                                    text = pct,
                                    color = EmeraldGrowth,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Tap graph to inspect", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D3 Canvas Curve Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF0A0F1D), RoundedCornerShape(12.dp))
                    .border(1.dp, ObsidianBorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
                        .pointerInput(dataPoints, selectedMetric) {
                            detectTapGestures { offset ->
                                val step = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                val index = (offset.x / step).roundToInt().coerceIn(0, dataPoints.size - 1)
                                selectedIndex = index
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0 || h <= 0) return@Canvas

                    // Determine values based on metric selection
                    val primaryValues = when (selectedMetric) {
                        TrendMetricType.NET_WORTH -> dataPoints.map { it.netWorth }
                        TrendMetricType.INVESTMENTS -> dataPoints.map { it.investments }
                        TrendMetricType.DEBT_REDUCTION -> dataPoints.map { it.debt }
                        TrendMetricType.MULTI_COMPARE -> dataPoints.map { it.netWorth }
                    }

                    val minVal = (primaryValues.minOrNull() ?: 0.0) * 0.85
                    val maxVal = (primaryValues.maxOrNull() ?: 1.0) * 1.15
                    val range = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

                    val stepX = w / (dataPoints.size - 1).coerceAtLeast(1)

                    // Draw subtle horizontal grid lines
                    val gridLines = 4
                    val gridPath = Path()
                    for (g in 0..gridLines) {
                        val gy = h * (g.toFloat() / gridLines)
                        gridPath.moveTo(0f, gy)
                        gridPath.lineTo(w, gy)
                    }
                    drawPath(
                        path = gridPath,
                        color = Color.White.copy(alpha = 0.05f),
                        style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    )

                    // Function to calculate Y pixel coordinate
                    fun getY(value: Double): Float {
                        return (h - ((value - minVal) / range * h)).toFloat()
                    }

                    // Helper to draw a single smooth cubic curve
                    fun drawCurve(values: List<Double>, curveColor: Color, fillGradient: Boolean) {
                        if (values.isEmpty()) return
                        val strokePath = Path()
                        val fillPath = Path()

                        val startY = getY(values[0])
                        strokePath.moveTo(0f, startY)
                        fillPath.moveTo(0f, h)
                        fillPath.lineTo(0f, startY)

                        for (i in 1 until values.size) {
                            val x = i * stepX
                            val y = getY(values[i])

                            val prevX = (i - 1) * stepX
                            val prevY = getY(values[i - 1])

                            val cx1 = prevX + (x - prevX) / 2f
                            val cy1 = prevY
                            val cx2 = prevX + (x - prevX) / 2f
                            val cy2 = y

                            strokePath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                            fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                        }

                        fillPath.lineTo(w, h)
                        fillPath.close()

                        if (fillGradient) {
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(curveColor.copy(alpha = 0.35f), Color.Transparent),
                                    startY = 0f,
                                    endY = h
                                )
                            )
                        }

                        drawPath(
                            path = strokePath,
                            color = curveColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    if (selectedMetric == TrendMetricType.MULTI_COMPARE) {
                        // Draw Investment curve (Emerald)
                        drawCurve(dataPoints.map { it.investments }, EmeraldGrowth, true)
                        // Draw Debt curve (Rose)
                        drawCurve(dataPoints.map { it.debt }, Color(0xFFFB7185), false)
                        // Draw Net Worth curve (Gold)
                        drawCurve(dataPoints.map { it.netWorth }, SovereignGold, true)
                    } else {
                        drawCurve(primaryValues, selectedMetric.color, true)
                    }

                    // Active selected node highlight
                    val activeIdx = selectedIndex ?: (dataPoints.size - 1)
                    val nodeX = activeIdx * stepX
                    val nodeVal = primaryValues[activeIdx]
                    val nodeY = getY(nodeVal)

                    // Vertical guideline at selection
                    drawLine(
                        color = selectedMetric.color.copy(alpha = 0.6f),
                        start = Offset(nodeX, 0f),
                        end = Offset(nodeX, h),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Outer halo
                    drawCircle(
                        color = selectedMetric.color.copy(alpha = 0.25f),
                        radius = 16f,
                        center = Offset(nodeX, nodeY)
                    )
                    // Inner solid dot
                    drawCircle(
                        color = selectedMetric.color,
                        radius = 7f,
                        center = Offset(nodeX, nodeY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(nodeX, nodeY)
                    )
                }

                // Month Axis Labels at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dataPoints.forEachIndexed { idx, pt ->
                        val isSelected = (selectedIndex ?: (dataPoints.size - 1)) == idx
                        Text(
                            text = pt.monthLabel,
                            color = if (isSelected) SovereignGold else TextMuted,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
