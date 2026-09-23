package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDebt
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Result data holder for Debt Amortization Simulation
 */
data class DebtPayoffCalculation(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val totalMonths: Int = 0,
    val years: Int = 0,
    val remainingMonths: Int = 0,
    val freedomDateString: String = "",
    val totalInterestPaid: Double = 0.0,
    val totalAmountPaid: Double = 0.0,
    val monthlyInterestFirstMonth: Double = 0.0,
    val monthlyPrincipalFirstMonth: Double = 0.0,
    val amortizationCurvePoints: List<Float> = emptyList(), // Normalized 0..1 balance over time
    // Accelerated Comparison
    val acceleratedMonths: Int = 0,
    val monthsSaved: Int = 0,
    val interestSaved: Double = 0.0,
    val acceleratedFreedomDateString: String = "",
    val acceleratedCurvePoints: List<Float> = emptyList()
)

/**
 * Institutional Interactive Debt Payoff Calculator & Visualizer
 */
@Composable
fun DebtPayoffCalculator(
    // Zero, not a fabricated demo debt. A user with no debt accounts must see blank inputs and
    // an "enter your numbers" prompt, never a pre-filled $15,000 balance / 16.5% APR / $500
    // payment that looks like it came from their own vault.
    initialBalance: Double = 0.0,
    initialApr: Double = 0.0,
    initialMonthlyPayment: Double = 0.0,
    currencySymbol: String = "$",
    formatAmount: (Double) -> String = { "$currencySymbol${"%,.0f".format(it)}" },
    totalVaultDebt: Double = 0.0,
    modifier: Modifier = Modifier
) {
    var balanceInput by remember { mutableStateOf(if (initialBalance > 0) initialBalance.toInt().toString() else "") }
    var aprInput by remember { mutableStateOf(if (initialApr > 0) initialApr.toString() else "") }
    var monthlyPaymentInput by remember { mutableStateOf(if (initialMonthlyPayment > 0) initialMonthlyPayment.toInt().toString() else "") }
    var extraMonthlyPayment by remember { mutableFloatStateOf(100f) }
    var isExpanded by remember { mutableStateOf(true) }

    val currentBalance = balanceInput.toDoubleOrNull() ?: 0.0
    val currentApr = aprInput.toDoubleOrNull() ?: 0.0
    val currentMonthlyPayment = monthlyPaymentInput.toDoubleOrNull() ?: 0.0

    // Auto-calculate payoff metrics
    val calculation = remember(currentBalance, currentApr, currentMonthlyPayment, extraMonthlyPayment) {
        computeDebtPayoff(
            balance = currentBalance,
            annualRate = currentApr,
            monthlyPayment = currentMonthlyPayment,
            extraPayment = extraMonthlyPayment.toDouble()
        )
    }

    FinCard(
        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        // Card Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SovereignGold.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        tint = GoldLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "DEBT PAYOFF CALCULATOR",
                        color = GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Interactive Amortization & Freedom Simulator",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (totalVaultDebt > 0 && currentBalance != totalVaultDebt) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable {
                            balanceInput = totalVaultDebt.toInt().toString()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = GoldLight, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Vault Debt", color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                // Inputs Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Loan Balance Input
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Loan Balance ($currencySymbol)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = balanceInput,
                            onValueChange = { balanceInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SovereignGold,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = ObsidianSurfaceVariant,
                                unfocusedContainerColor = ObsidianSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Interest Rate (APR %)
                    Column(modifier = Modifier.weight(0.9f)) {
                        Text(
                            text = "Interest Rate (APR %)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = aprInput,
                            onValueChange = { aprInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SovereignGold,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = ObsidianSurfaceVariant,
                                unfocusedContainerColor = ObsidianSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly Payment Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Payment ($currencySymbol)",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val minInterest = (currentBalance * (currentApr / 100.0 / 12.0))
                            if (minInterest > 0) {
                                Text(
                                    text = "Min Interest: ${formatAmount(minInterest)}/mo",
                                    color = if (currentMonthlyPayment <= minInterest) CrimsonDebt else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = monthlyPaymentInput,
                            onValueChange = { monthlyPaymentInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (!calculation.isValid) CrimsonDebt else SovereignGold,
                                unfocusedBorderColor = if (!calculation.isValid) CrimsonDebt.copy(alpha = 0.5f) else ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = ObsidianSurfaceVariant,
                                unfocusedContainerColor = ObsidianSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Quick Balance & APR Preset Chips
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetChip(
                        label = "$currencySymbol 5k",
                        isSelected = balanceInput == "5000",
                        onClick = { balanceInput = "5000" }
                    )
                    PresetChip(
                        label = "$currencySymbol 15k",
                        isSelected = balanceInput == "15000",
                        onClick = { balanceInput = "15000" }
                    )
                    PresetChip(
                        label = "$currencySymbol 30k",
                        isSelected = balanceInput == "30000",
                        onClick = { balanceInput = "30000" }
                    )
                    PresetChip(
                        label = "8% APR",
                        isSelected = aprInput == "8" || aprInput == "8.0",
                        onClick = { aprInput = "8.0" }
                    )
                    PresetChip(
                        label = "18% APR",
                        isSelected = aprInput == "18" || aprInput == "18.0",
                        onClick = { aprInput = "18.0" }
                    )
                    PresetChip(
                        label = "24% APR",
                        isSelected = aprInput == "24" || aprInput == "24.0",
                        onClick = { aprInput = "24.0" }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Extra Payment Booster Slider
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ObsidianSurfaceVariant,
                    border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ACCELERATION BOOSTER",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "+${formatAmount(extraMonthlyPayment.toDouble())}/month",
                                color = EmeraldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Slider(
                            value = extraMonthlyPayment,
                            onValueChange = { extraMonthlyPayment = it },
                            valueRange = 0f..500f,
                            steps = 9, // increments of $50
                            colors = SliderDefaults.colors(
                                thumbColor = SovereignGold,
                                activeTrackColor = SovereignGold,
                                inactiveTrackColor = ObsidianBorderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("+${formatAmount(0.0)}", color = TextMuted, fontSize = 10.sp)
                            Text("+${formatAmount(250.0)}", color = TextMuted, fontSize = 10.sp)
                            Text("+${formatAmount(500.0)}/mo", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!calculation.isValid) {
                    // Invalid State Warning
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CrimsonDebt.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, CrimsonDebt.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonDebt, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = calculation.errorMessage ?: "Monthly payment must be greater than the monthly interest charge to pay off this debt.",
                                color = Color(0xFFFB7185),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    // Debt Freedom Hero Result Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F1B16),
                        border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "DEBT FREEDOM DATE",
                                        color = EmeraldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (extraMonthlyPayment > 0) calculation.acceleratedFreedomDateString else calculation.freedomDateString,
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldGrowth.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val activeMonths = if (extraMonthlyPayment > 0) calculation.acceleratedMonths else calculation.totalMonths
                                        val yrs = activeMonths / 12
                                        val mos = activeMonths % 12
                                        val timeStr = if (yrs > 0) "${yrs}y ${mos}m" else "${mos} months"
                                        Text(
                                            text = timeStr,
                                            color = EmeraldLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            if (extraMonthlyPayment > 0 && calculation.monthsSaved > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GoldLight.copy(alpha = 0.15f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = GoldLight, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${calculation.monthsSaved} mo faster",
                                                color = GoldLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CyanAccent.copy(alpha = 0.15f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${formatAmount(calculation.interestSaved)} saved",
                                                color = CyanAccent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Visual Payoff Trajectory Chart (Canvas)
                    Text(
                        text = "DEBT PAYOFF TRAJECTORY",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    DebtPayoffCanvasChart(
                        baselinePoints = calculation.amortizationCurvePoints,
                        acceleratedPoints = if (extraMonthlyPayment > 0) calculation.acceleratedCurvePoints else emptyList(),
                        currencySymbol = currencySymbol,
                        startBalance = currentBalance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(ObsidianSurfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, ObsidianBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown Summary Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricSummaryCard(
                            title = "Principal",
                            value = formatAmount(currentBalance),
                            color = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Total Interest",
                            value = formatAmount(calculation.totalInterestPaid),
                            color = Color(0xFFFB7185),
                            modifier = Modifier.weight(1f)
                        )
                        MetricSummaryCard(
                            title = "Total Cost",
                            value = formatAmount(calculation.totalAmountPaid),
                            color = GoldLight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) SovereignGold else ObsidianSurfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) SovereignGold else ObsidianBorderSubtle),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF0A0700) else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ObsidianSurfaceVariant,
        border = BorderStroke(1.dp, ObsidianBorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Custom Canvas drawing interactive smooth Bezier curve for Debt Freedom Trajectory
 */
@Composable
private fun DebtPayoffCanvasChart(
    baselinePoints: List<Float>,
    acceleratedPoints: List<Float>,
    currencySymbol: String,
    startBalance: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val paddingBottom = 16f
        val paddingTop = 10f
        val chartHeight = h - paddingBottom - paddingTop

        if (baselinePoints.isEmpty()) return@Canvas

        // Draw baseline grid lines
        drawLine(
            color = Color(0x22FFFFFF),
            start = Offset(0f, paddingTop),
            end = Offset(w, paddingTop),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0x22FFFFFF),
            start = Offset(0f, paddingTop + chartHeight * 0.5f),
            end = Offset(w, paddingTop + chartHeight * 0.5f),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(0f, h - paddingBottom),
            end = Offset(w, h - paddingBottom),
            strokeWidth = 1f
        )

        // Draw Baseline Path (Crimson/Amber)
        val basePath = Path()
        val baseFillPath = Path()

        val stepX = w / max(1, baselinePoints.size - 1)

        baselinePoints.forEachIndexed { i, ratio ->
            val x = i * stepX
            val y = paddingTop + (1f - ratio) * chartHeight
            if (i == 0) {
                basePath.moveTo(x, y)
                baseFillPath.moveTo(x, h - paddingBottom)
                baseFillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevY = paddingTop + (1f - baselinePoints[i - 1]) * chartHeight
                val cx = (prevX + x) / 2f
                basePath.cubicTo(cx, prevY, cx, y, x, y)
                baseFillPath.cubicTo(cx, prevY, cx, y, x, y)
            }
        }

        baseFillPath.lineTo(w, h - paddingBottom)
        baseFillPath.close()

        // Draw baseline fill & stroke
        drawPath(
            path = baseFillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x33F43F5E), Color(0x00F43F5E)),
                startY = paddingTop,
                endY = h - paddingBottom
            )
        )
        drawPath(
            path = basePath,
            color = Color(0xFFFB7185),
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Draw Accelerated Path if present (Emerald Green)
        if (acceleratedPoints.isNotEmpty()) {
            val accelPath = Path()
            val accelFillPath = Path()
            val accelStepX = w / max(1, acceleratedPoints.size - 1)

            acceleratedPoints.forEachIndexed { i, ratio ->
                val x = i * accelStepX
                val y = paddingTop + (1f - ratio) * chartHeight
                if (i == 0) {
                    accelPath.moveTo(x, y)
                    accelFillPath.moveTo(x, h - paddingBottom)
                    accelFillPath.lineTo(x, y)
                } else {
                    val prevX = (i - 1) * accelStepX
                    val prevY = paddingTop + (1f - acceleratedPoints[i - 1]) * chartHeight
                    val cx = (prevX + x) / 2f
                    accelPath.cubicTo(cx, prevY, cx, y, x, y)
                    accelFillPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            accelFillPath.lineTo(w, h - paddingBottom)
            accelFillPath.close()

            drawPath(
                path = accelFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x4410B981), Color(0x0010B981)),
                    startY = paddingTop,
                    endY = h - paddingBottom
                )
            )
            drawPath(
                path = accelPath,
                color = EmeraldGrowth,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
        }

        // Target Freedom Dot (Zero Balance)
        drawCircle(
            color = EmeraldLight,
            radius = 5f,
            center = Offset(w, h - paddingBottom)
        )
    }
}

/**
 * Robust mathematical amortization engine
 */
private fun computeDebtPayoff(
    balance: Double,
    annualRate: Double,
    monthlyPayment: Double,
    extraPayment: Double
): DebtPayoffCalculation {
    if (balance <= 0 || monthlyPayment <= 0) {
        return DebtPayoffCalculation(
            isValid = false,
            errorMessage = "Please enter a valid loan balance and monthly payment."
        )
    }

    val monthlyRate = annualRate / 100.0 / 12.0
    val minRequiredInterest = balance * monthlyRate

    if (monthlyRate > 0 && monthlyPayment <= minRequiredInterest) {
        return DebtPayoffCalculation(
            isValid = false,
            errorMessage = "Monthly payment ($${"%.0f".format(monthlyPayment)}) is less than monthly interest ($${"%.0f".format(minRequiredInterest)}). The loan will never be paid off. Increase payment."
        )
    }

    // 1. Simulate Baseline Payoff
    val baselineSim = simulateAmortization(balance, monthlyRate, monthlyPayment)
    // 2. Simulate Accelerated Payoff (Payment + Extra)
    val accelSim = if (extraPayment > 0) {
        simulateAmortization(balance, monthlyRate, monthlyPayment + extraPayment)
    } else {
        baselineSim
    }

    val now = Calendar.getInstance()
    val baselineCalendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, baselineSim.totalMonths)
    }
    val accelCalendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, accelSim.totalMonths)
    }

    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.US)
    val baselineDateStr = dateFormat.format(baselineCalendar.time)
    val accelDateStr = dateFormat.format(accelCalendar.time)

    val monthsSaved = max(0, baselineSim.totalMonths - accelSim.totalMonths)
    val interestSaved = max(0.0, baselineSim.totalInterest - accelSim.totalInterest)

    return DebtPayoffCalculation(
        isValid = true,
        totalMonths = baselineSim.totalMonths,
        years = baselineSim.totalMonths / 12,
        remainingMonths = baselineSim.totalMonths % 12,
        freedomDateString = baselineDateStr,
        totalInterestPaid = baselineSim.totalInterest,
        totalAmountPaid = balance + baselineSim.totalInterest,
        monthlyInterestFirstMonth = minRequiredInterest,
        monthlyPrincipalFirstMonth = monthlyPayment - minRequiredInterest,
        amortizationCurvePoints = baselineSim.curvePoints,
        acceleratedMonths = accelSim.totalMonths,
        monthsSaved = monthsSaved,
        interestSaved = interestSaved,
        acceleratedFreedomDateString = accelDateStr,
        acceleratedCurvePoints = accelSim.curvePoints
    )
}

private data class SimResult(
    val totalMonths: Int,
    val totalInterest: Double,
    val curvePoints: List<Float>
)

private fun simulateAmortization(
    principal: Double,
    monthlyRate: Double,
    monthlyPayment: Double
): SimResult {
    var bal = principal
    var totalInt = 0.0
    var months = 0
    val maxMonthsLimit = 1200 // 100 years safeguard

    val balances = mutableListOf<Float>()
    balances.add(1f) // initial normalized 100%

    while (bal > 0.01 && months < maxMonthsLimit) {
        val interest = bal * monthlyRate
        val principalPortion = min(bal, monthlyPayment - interest)
        bal -= principalPortion
        totalInt += interest
        months++

        // Sample balance points
        if (months % max(1, (months / 20)) == 0 || bal <= 0.01) {
            balances.add((max(0.0, bal) / principal).toFloat())
        }
    }

    if (balances.lastOrNull() != 0f) {
        balances.add(0f)
    }

    // Sample down to consistent ~15-20 points for smooth rendering
    val step = max(1, balances.size / 20)
    val sampled = balances.filterIndexed { index, _ -> index % step == 0 || index == balances.lastIndex }

    return SimResult(
        totalMonths = max(1, months),
        totalInterest = totalInt,
        curvePoints = sampled
    )
}
