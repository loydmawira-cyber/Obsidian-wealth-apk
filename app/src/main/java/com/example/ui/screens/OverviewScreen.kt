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
import com.example.data.models.GeographicRegion
import com.example.data.models.SupportedCurrency
import com.example.ui.components.ActionPillButton
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.D3FinancialTrendsDashboard
import com.example.ui.components.FinCard
import com.example.ui.components.GoldBadge
import com.example.ui.components.HeroGradientCard
import com.example.ui.components.MetricBadge
import com.example.ui.components.SparklineChart
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
import com.example.ui.viewmodel.TimeFrame

@Composable
fun OverviewScreen(
    viewModel: FinanceViewModel,
    onOpenAiAdvisor: (initialPrompt: String?) -> Unit,
    onQuickAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val selectedTimeFrame by viewModel.selectedTimeFrame.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    val nw = summary.totalNetWorth.toFloat()
    val sparklinePoints = when (selectedTimeFrame) {
        TimeFrame.ONE_MONTH -> listOf(nw * 0.97f, nw * 0.98f, nw * 0.975f, nw * 0.985f, nw * 0.992f, nw)
        TimeFrame.SIX_MONTHS -> listOf(nw * 0.85f, nw * 0.88f, nw * 0.90f, nw * 0.93f, nw * 0.96f, nw)
        TimeFrame.ONE_YEAR -> listOf(nw * 0.73f, nw * 0.78f, nw * 0.82f, nw * 0.88f, nw * 0.94f, nw)
        TimeFrame.ALL -> listOf(nw * 0.48f, nw * 0.58f, nw * 0.71f, nw * 0.84f, nw * 0.93f, nw)
    }

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

                        GoldBadge(text = "+1.4% MoM")
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
            D3FinancialTrendsDashboard(userSettings = userSettings)
        }

        // AI Financial Resilience Index
        item {
            FinCard(
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.45f)),
                gradient = Brush.linearGradient(
                    listOf(
                        GoldGradientStart,
                        CardGradientStart,
                        ObsidianSurface
                    )
                ),
                onClick = { onOpenAiAdvisor("Give me a comprehensive audit of my 88/100 financial health index") }
            ) {
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
                            text = "88 / 100 — Institutional Prime",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Emergency buffer 6.3 mo • DTI 25.3% (Top tier) • Savings rate 54.7%",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    CircularProgressRing(
                        progressPercent = 88f,
                        sizeDp = 70.dp,
                        gradientColors = listOf(SovereignGold, GoldLight)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5 Sub-Pillars Horizontal Breakdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurfaceVariant, RoundedCornerShape(10.dp))
                        .border(1.dp, ObsidianBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "Liquidity" to "18/20",
                        "Debt DTI" to "17/20",
                        "Savings" to "19/20",
                        "Diversify" to "18/20",
                        "Budget" to "16/20"
                    ).forEach { (name, score) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = name, color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = score,
                                color = SovereignGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
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

                // Alert 1: Cash Drag
                val isKenya = userSettings.region == GeographicRegion.EAST_AFRICA || userSettings.currency == SupportedCurrency.KES
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
                                text = if (isKenya) "Cash Drag Yield Opportunity (+KSh 104k/yr)" else "Cash Drag Yield Opportunity (+$1,481/yr)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isKenya)
                                    "KSh 850,000 idle in 0.5% checking. Sweeping KSh 600k to 12.8% p.a. CIC MMF captures KSh 6,400/mo passive spread."
                                else
                                    "$31,500 idle in 0.45% APY checking. Moving $20k to 5.15% APY Treasury/HYSA captures $123/mo passive spread.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Alert 2: SIP Scheduled (Shown only if Investments enabled)
                if (userSettings.enableInvestments) {
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
                                    text = if (isKenya) "3 Standing Orders / SIPs Scheduled This Month" else "3 SIP Mandates Scheduled This Week",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isKenya)
                                        "CIC MMF (KSh 50k), Safaricom SIP (KSh 25k), Britam (KSh 20k) will execute automatically. Liquidity verified."
                                    else
                                        "VFIAX ($500), FBGRX ($300), SCHD ($250) will execute automatically. Account liquidity verified.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Alert 3: Avalanche Debt Advantage (Shown only if Debt Center enabled)
                if (userSettings.enableDebtCenter) {
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
                                    text = "Debt Avalanche Accelerates Freedom Date",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isKenya)
                                        "Targeting 24.0% APR Standard Chartered first saves KSh 148,000 in interest and brings debt-free target to March 2026."
                                    else
                                        "Targeting 21.9% APR Sapphire first saves $1,280 in interest and brings debt-free target to March 2026.",
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
