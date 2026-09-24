package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.HoldingType
import com.example.ui.components.ActionPillButton
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.D3FinancialTrendsDashboard
import com.example.ui.components.FinCard
import com.example.ui.components.GoldBadge
import com.example.ui.components.HeroGradientCard
import com.example.ui.components.MetricBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardGradientStart
import com.example.ui.theme.CrimsonDebt
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.GoldGradientStart
import com.example.ui.theme.GoldLight
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FinanceSummary
import com.example.ui.viewmodel.FinanceTab
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun OverviewScreen(
    viewModel: FinanceViewModel,
    onOpenAiAdvisor: (initialPrompt: String?) -> Unit,
    onQuickAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sips by viewModel.sips.collectAsState()
    val creditCards by viewModel.creditCards.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val holdings by viewModel.holdings.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Net Worth Header
        item {
            HeroGradientCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL NET WORTH",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatAmount(summary.totalNetWorth),
                                color = GoldLight,
                                fontSize = if (Math.abs(summary.totalNetWorth) >= 1_000_000) 26.sp else 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // No prior-period snapshot is recorded, so a real MoM % cannot be
                        // computed (same reasoning as the Reports screen's MoM line). Showing a
                        // fixed "+1.4%" made every vault, including an empty new one, look like
                        // it had a month of real gains.
                        if (summary.transactionCount > 0 || summary.holdingCount > 0) {
                            GoldBadge(text = "MoM: N/A")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clean Assets vs Liabilities Side-by-Side Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, ObsidianBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(SovereignGold, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Assets", color = TextMuted, fontSize = 10.sp)
                                Text(
                                    text = viewModel.formatAmount(summary.totalAssets),
                                    color = GoldLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(ObsidianBorderSubtle)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(CrimsonDebt, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "Liabilities", color = TextMuted, fontSize = 10.sp)
                                Text(
                                    text = viewModel.formatAmount(summary.totalLiabilities),
                                    color = Color(0xFFFB7185),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    NetWorthBreakdown(summary = summary, format = { viewModel.formatAmount(it) })
                }
            }
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (userSettings.enableCashFlow) {
                    ActionPillButton(
                        icon = Icons.Default.ArrowDownward,
                        label = "+ Inflow",
                        onClick = onQuickAddTransaction,
                        modifier = Modifier.weight(1f),
                        iconTint = SovereignGold
                    )
                    ActionPillButton(
                        icon = Icons.Default.ArrowUpward,
                        label = "- Expense",
                        onClick = onQuickAddTransaction,
                        modifier = Modifier.weight(1f),
                        iconTint = Color(0xFFFB7185)
                    )
                }
                if (userSettings.enableInvestments) {
                    ActionPillButton(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "Invest",
                        onClick = { viewModel.selectTab(FinanceTab.INVEST) },
                        modifier = Modifier.weight(1f),
                        iconTint = EmeraldLight
                    )
                }
                if (userSettings.enableDebtCenter) {
                    ActionPillButton(
                        icon = Icons.Default.CreditCard,
                        label = "Pay Debt",
                        onClick = { viewModel.selectTab(FinanceTab.DEBT) },
                        modifier = Modifier.weight(1f),
                        iconTint = CyanAccent
                    )
                }
            }
        }

        // D3 Financial Trends Analytics
        item {
            D3FinancialTrendsDashboard(
                userSettings = userSettings,
                summary = summary
            )
        }

        // AI Financial Resilience Index — built only from pillars we can actually measure.
        // Liquidity (emergency-fund months) and Budget adherence have no backing data model
        // yet (no cash-account or budget entity), so they are left out rather than faked.
        // DTI and Savings need real income transactions to mean anything; Diversify needs at
        // least one holding. When none of the three have real data, the card shows a prompt
        // instead of a score.
        item {
            val dtiScore = if (summary.totalInflow > 0) (100.0 - summary.dtiRatio).coerceIn(0.0, 100.0) else null
            val savingsScore = if (summary.totalInflow > 0) summary.savingsRate.coerceIn(0.0, 100.0) else null
            val distinctHoldingTypes = holdings.map { it.type }.distinct().size
            val diversifyScore = if (holdings.isNotEmpty()) {
                (distinctHoldingTypes.toDouble() / HoldingType.entries.size * 100.0).coerceIn(0.0, 100.0)
            } else null

            val pillars = listOfNotNull(
                dtiScore?.let { "DTI" to it },
                savingsScore?.let { "Savings" to it },
                diversifyScore?.let { "Diversify" to it }
            )

            FinCard(
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.45f)),
                gradient = Brush.linearGradient(
                    listOf(
                        GoldGradientStart,
                        CardGradientStart,
                        ObsidianSurface
                    )
                ),
                onClick = { onOpenAiAdvisor("Give me a comprehensive audit of my financial position") }
            ) {
                if (pillars.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = SovereignGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AI FINANCIAL RESILIENCE INDEX",
                                color = GoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add income, expenses, or holdings to unlock your score.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    val overallScore = pillars.map { it.second }.average()
                    val tier = when {
                        overallScore >= 80 -> "Strong"
                        overallScore >= 60 -> "Moderate"
                        overallScore >= 40 -> "Building"
                        else -> "Needs Attention"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = SovereignGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI FINANCIAL RESILIENCE INDEX",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${overallScore.toInt()} / 100 — $tier",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Based on ${pillars.joinToString(", ") { it.first }} — based only on what's recorded so far.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        CircularProgressRing(
                            progressPercent = overallScore.toFloat(),
                            sizeDp = 70.dp,
                            gradientColors = listOf(SovereignGold, GoldLight)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Only the pillars we can actually compute are shown — no placeholder
                    // tiles for Liquidity or Budget, which this app doesn't track yet.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurfaceVariant, RoundedCornerShape(10.dp))
                            .border(1.dp, ObsidianBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        pillars.forEach { (name, score) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = name, color = TextMuted, fontSize = 10.sp)
                                Text(
                                    text = "${score.toInt()}/100",
                                    color = SovereignGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actionable AI Alpha Feed
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIONABLE AI ALPHA FEED",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Ask Obsidian AI →",
                        color = SovereignGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenAiAdvisor(null) }
                    )
                }

                // Every card below is derived from the user's own recorded data, and a card with no
                // underlying data is not shown. These cards previously asserted fixed figures,
                // institution names and dates (e.g. "CIC MMF (KSh 50k)", "saves KSh 148,000",
                // "debt-free March 2026") that did not come from the user's records.
                val retainedCash = summary.netCashRetained
                val activeSips = sips.filter { it.isActive }
                val debtLines = (
                    creditCards.filter { it.currentBalance > 0 }
                        .map { DebtLine(it.cardName, it.apr, it.currentBalance) } +
                        loans.filter { it.remainingBalance > 0 }
                            .map { DebtLine(it.loanName, it.interestRate, it.remainingBalance) }
                    ).sortedByDescending { it.rate }

                val showRetainedCard = retainedCash > 0
                val showSipCard = userSettings.enableInvestments && activeSips.isNotEmpty()
                val showDebtCard = userSettings.enableDebtCenter && debtLines.isNotEmpty()

                if (!showRetainedCard && !showSipCard && !showDebtCard) {
                    Text(
                        text = "Record transactions, SIPs or debts and insights based on your own data will appear here.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                // Card 1: retained cash (needs a positive net cash figure from recorded cash flow)
                if (showRetainedCard) {
                    FinCard(
                        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.4f)),
                        backgroundColor = ObsidianSurfaceVariant,
                        onClick = { onOpenAiAdvisor("How do I eliminate my cash drag and optimize uninvested liquidity?") }
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(AmberWarning.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Retained Cash Opportunity",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Your recorded cash flow retains ${userSettings.formatAmount(retainedCash)} " +
                                        "(${String.format(Locale.US, "%.1f", summary.savingsRate)}% of inflow). " +
                                        "Cash left in a low-yield account loses value to inflation. Compare your deposit rate " +
                                        "with a money market or treasury instrument, and keep only your emergency reserve " +
                                        "in instant-access cash.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Card 2: active SIPs / standing orders (from the SIPs the user has recorded)
                if (showSipCard) {
                    val sipTitle = if (activeSips.size == 1) {
                        "1 Active SIP / Standing Order"
                    } else {
                        "${activeSips.size} Active SIPs / Standing Orders"
                    }
                    val sipListed = activeSips.take(3).joinToString(", ") {
                        "${it.fundName} (${userSettings.formatAmount(it.monthlyAmount)}, day ${it.debitDayOfMonth})"
                    }
                    val sipMore = if (activeSips.size > 3) " and ${activeSips.size - 3} more" else ""
                    val sipTotal = userSettings.formatAmount(activeSips.sumOf { it.monthlyAmount })
                    FinCard(
                        border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.3f)),
                        backgroundColor = Color(0xFF0F1822),
                        onClick = { viewModel.selectTab(FinanceTab.INVEST) }
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(EmeraldGrowth.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = EmeraldGrowth,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sipTitle,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$sipListed$sipMore. Total $sipTotal per month.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Card 3: highest-rate debt (avalanche ordering over the cards and loans recorded)
                if (showDebtCard) {
                    val topDebt = debtLines.first()
                    val totalRecordedDebt = debtLines.sumOf { it.balance }
                    val accountWord = if (debtLines.size == 1) "account" else "accounts"
                    FinCard(
                        border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.3f)),
                        backgroundColor = Color(0xFF131726),
                        onClick = { viewModel.selectTab(FinanceTab.DEBT) }
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(ElectricIndigo.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = IndigoLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Highest-Rate Debt: ${topDebt.name}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${topDebt.name} carries ${String.format(Locale.US, "%.1f", topDebt.rate)}% " +
                                        "on ${userSettings.formatAmount(topDebt.balance)}. Directing extra payments to the " +
                                        "highest-rate balance first (the avalanche method) reduces total interest. " +
                                        "Recorded debt across ${debtLines.size} $accountWord: " +
                                        "${userSettings.formatAmount(totalRecordedDebt)}.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/** One line of recorded debt (a credit card or a loan) used to order payoff by interest rate. */
private data class DebtLine(val name: String, val rate: Double, val balance: Double)


/** Compact "what your net worth is made of" strip: asset mix bar plus one line per component. */
@Composable
private fun NetWorthBreakdown(
    summary: com.example.ui.viewmodel.FinanceSummary,
    format: (Double) -> String
) {
    val cashColor = Color(0xFF34D399)
    val investColor = SovereignGold
    val goalsColor = Color(0xFF60A5FA)
    val debtColor = Color(0xFFFB7185)

    val parts = listOf(
        Triple("Cash", summary.liquidCash, cashColor),
        Triple("Investments", summary.portfolioValue, investColor),
        Triple("Goal savings", summary.goalsValue, goalsColor)
    )
    val assetTotal = parts.sumOf { it.second.coerceAtLeast(0.0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "NET WORTH BREAKDOWN",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (assetTotal > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(ObsidianSurfaceVariant, RoundedCornerShape(3.dp))
            ) {
                parts.filter { it.second > 0 }.forEach { (_, amount, color) ->
                    Box(
                        modifier = Modifier
                            .weight(amount.toFloat())
                            .height(6.dp)
                            .background(color)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        parts.forEach { (label, amount, color) ->
            BreakdownLine(
                label = label,
                value = format(amount),
                share = if (assetTotal > 0) "${Math.round(amount.coerceAtLeast(0.0) / assetTotal * 100)}%" else "",
                color = color,
                valueColor = GoldLight
            )
        }
        BreakdownLine(
            label = "Debt",
            value = "-" + format(summary.totalLiabilities),
            share = "",
            color = debtColor,
            valueColor = debtColor
        )
    }
}

@Composable
private fun BreakdownLine(
    label: String,
    value: String,
    share: String,
    color: Color,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = TextSecondary, fontSize = 12.sp)
            if (share.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = share, color = TextMuted, fontSize = 10.sp)
            }
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
