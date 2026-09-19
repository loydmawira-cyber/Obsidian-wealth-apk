package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.HoldingEntity
import com.example.data.models.HoldingType
import com.example.data.models.SipEntity
import com.example.ui.components.D3AllocationSlice
import com.example.ui.components.D3InteractiveDonutChart
import com.example.ui.components.FinCard
import com.example.ui.components.GoldBadge
import com.example.ui.components.HeroGradientCard
import com.example.ui.components.MetricBadge
import com.example.ui.components.SparklineChart
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
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
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun InvestmentsScreen(
    viewModel: FinanceViewModel,
    onAddHolding: () -> Unit,
    onAddSip: () -> Unit,
    onOpenAiAdvisor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val sips by viewModel.sips.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sym = userSettings.currency.symbol

    val totalPortfolioValue = if (holdings.isNotEmpty()) holdings.sumOf { it.totalValue } else summary.portfolioValue
    val totalUnrealizedGain = holdings.sumOf { it.unrealizedGain }
    val totalGainPercent = if (holdings.sumOf { it.totalCost } > 0) (totalUnrealizedGain / holdings.sumOf { it.totalCost }) * 100.0 else 17.4

    // Compute dynamic allocation across Equities, Mutual Funds, and Gold
    val equityHoldings = holdings.filter {
        it.type == HoldingType.STOCK || (it.type == HoldingType.ETF && it.symbol != "GLD" && it.symbol != "BND" && !it.symbol.startsWith("IFB"))
    }
    val equityVal = if (equityHoldings.isNotEmpty()) equityHoldings.sumOf { it.totalValue } else 4567250.0

    val mutualFundHoldings = holdings.filter {
        it.type == HoldingType.MUTUAL_FUND || it.symbol == "BND" || it.symbol.startsWith("IFB")
    }
    val sipsTotalInvested = sips.sumOf { it.totalInvested }.takeIf { it > 0 } ?: 1475000.0
    val mutualFundVal = (if (mutualFundHoldings.isNotEmpty()) mutualFundHoldings.sumOf { it.totalValue } else 2000000.0) + sipsTotalInvested

    val goldHoldings = holdings.filter {
        it.type == HoldingType.GOLD || it.symbol == "GLD"
    }
    val goldVal = if (goldHoldings.isNotEmpty()) goldHoldings.sumOf { it.totalValue } else 525000.0

    val cryptoHoldings = holdings.filter { it.type == HoldingType.CRYPTO }
    val cryptoVal = if (cryptoHoldings.isNotEmpty()) cryptoHoldings.sumOf { it.totalValue } else 507500.0

    val allocationSlices = listOf(
        D3AllocationSlice(
            key = "EQUITIES",
            name = "Equities",
            value = equityVal,
            primaryColor = EmeraldGrowth,
            gradientColors = listOf(EmeraldGrowth, CyanAccent),
            holdingsCount = equityHoldings.size.takeIf { it > 0 } ?: 4,
            xirrReturnPercent = 18.4,
            description = "High-conviction direct equities and index positions with long-term capital growth and dividend yield.",
            underlyingAssets = if (equityHoldings.isNotEmpty()) equityHoldings.map { "${it.symbol} (${viewModel.formatCompact(it.totalValue)})" } else listOf("SCOM (${viewModel.formatCompact(1875000.0)})", "EQTY (${viewModel.formatCompact(1292250.0)})", "EABL (${viewModel.formatCompact(775000.0)})", "KCB (${viewModel.formatCompact(625000.0)})")
        ),
        D3AllocationSlice(
            key = "MUTUAL_FUNDS",
            name = "Funds & Bonds",
            value = mutualFundVal,
            primaryColor = ElectricIndigo,
            gradientColors = listOf(ElectricIndigo, IndigoLight),
            holdingsCount = (mutualFundHoldings.size + sips.size).takeIf { it > 0 } ?: 4,
            xirrReturnPercent = 15.8,
            description = "Recurring SIP standing orders, institutional MMFs, and tax-free Treasury Infrastructure Bonds.",
            underlyingAssets = if (sips.isNotEmpty()) sips.map { "${it.fundName.take(16)}... (${viewModel.formatCompact(it.totalInvested)})" } else listOf("CIC MMF (${viewModel.formatCompact(720000.0)})", "IFB Bond (${viewModel.formatCompact(2000000.0)})", "Britam Balanced (${viewModel.formatCompact(455000.0)})")
        ),
        D3AllocationSlice(
            key = "GOLD",
            name = "Gold & Bullion",
            value = goldVal,
            primaryColor = Color(0xFFF59E0B),
            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFFDE68A)),
            holdingsCount = goldHoldings.size.takeIf { it > 0 } ?: 1,
            xirrReturnPercent = 12.8,
            description = "Macroeconomic inflation mitigation reserve allocated in vaulted physical bullion and sovereign gold shares.",
            underlyingAssets = if (goldHoldings.isNotEmpty()) goldHoldings.map { "${it.symbol} (${viewModel.formatCompact(it.totalValue)})" } else listOf("GLD Gold (${viewModel.formatCompact(525000.0)})")
        ),
        D3AllocationSlice(
            key = "CRYPTO",
            name = "Crypto Reserve",
            value = cryptoVal,
            primaryColor = CyanAccent,
            gradientColors = listOf(CyanAccent, Color(0xFF38BDF8)),
            holdingsCount = cryptoHoldings.size.takeIf { it > 0 } ?: 1,
            xirrReturnPercent = 24.6,
            description = "Asymmetric digital store-of-value exposure held in institutional cold-storage custody.",
            underlyingAssets = if (cryptoHoldings.isNotEmpty()) cryptoHoldings.map { "${it.symbol} (${viewModel.formatCompact(it.totalValue)})" } else listOf("BTC Reserve (${viewModel.formatCompact(507500.0)})")
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Portfolio Hero Card
        item {
            HeroGradientCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AGGREGATE PORTFOLIO VALUE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = viewModel.formatAmount(totalPortfolioValue),
                            color = GoldLight,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    GoldBadge(
                        text = "+${"%.1f".format(summary.portfolioXirr)}% XIRR"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day Gain: +${viewModel.formatAmount(summary.portfolioDayGain)} (+${"%.2f".format(summary.portfolioDayGainPercent)}%)",
                        color = EmeraldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "All-Time: +${viewModel.formatCompact(totalUnrealizedGain)}",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val basePv = totalPortfolioValue.toFloat()
                SparklineChart(
                    points = listOf(basePv * 0.77f, basePv * 0.83f, basePv * 0.87f, basePv * 0.86f, basePv * 0.93f, basePv),
                    lineColor = CyanAccent,
                    gradientStart = CyanAccent.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        }

        // Interactive D3-Style Asset Allocation Donut Chart Section
        item {
            FinCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Asset Allocation",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PORTFOLIO ASSET ALLOCATION",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        color = ObsidianSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ObsidianBorderSubtle)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "D3 Visualizer",
                                tint = EmeraldGrowth,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "D3 Interactive",
                                color = EmeraldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                D3InteractiveDonutChart(
                    slices = allocationSlices,
                    totalPortfolioValue = totalPortfolioValue,
                    chartSize = 220.dp
                )
            }
        }

        // Active SIP Automated Engine Header & List
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AUTOMATED RECURRING SIPS",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$${"%,.0f".format(sips.filter { it.isActive }.sumOf { it.monthlyAmount })}/month automated capital deployment",
                            color = EmeraldLight,
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddSip() },
                        color = EmeraldGrowth.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ New SIP", color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // SIP Items
        items(sips, key = { "sip_${it.id}" }) { sip ->
            FinCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    if (sip.isActive) EmeraldGrowth.copy(alpha = 0.15f) else ObsidianSurfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (sip.isActive) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = if (sip.isActive) EmeraldLight else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sip.fundName,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${sip.category} • Debits on ${sip.debitDayOfMonth}st",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Total Invested: ${viewModel.formatCompact(sip.totalInvested)} (+${sip.annualizedReturnPercent}% CAGR)",
                                color = CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${viewModel.formatCompact(sip.monthlyAmount)}/mo",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Switch(
                            checked = sip.isActive,
                            onCheckedChange = { viewModel.toggleSip(sip) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldGrowth,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ObsidianBorder
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }

        // Live Watchlist & Equity Holdings Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE EQUITY & ASSET HOLDINGS",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddHolding() },
                        color = CyanAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Holding", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Holdings List
        items(holdings, key = { "holding_${it.id}" }) { h ->
            val isPositive = h.unrealizedGain >= 0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (h.type) {
                                        HoldingType.CRYPTO -> Color(0x33F59E0B)
                                        HoldingType.ETF -> CyanAccent.copy(alpha = 0.15f)
                                        else -> ElectricIndigo.copy(alpha = 0.15f)
                                    },
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = h.symbol.take(4),
                                color = when (h.type) {
                                    HoldingType.CRYPTO -> Color(0xFFF59E0B)
                                    HoldingType.ETF -> CyanAccent
                                    else -> IndigoLight
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = h.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${if (h.shares % 1.0 == 0.0) h.shares.toInt() else h.shares} shares @ ${sym}${"%,.2f".format(h.currentPrice)} (Avg ${sym}${"%,.2f".format(h.avgBuyPrice)})",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = viewModel.formatAmount(h.totalValue),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = (if (isPositive) "+$sym" else "-$sym") + "%,.0f".format(Math.abs(h.unrealizedGain)) + " (${if (isPositive) "+" else ""}${"%.1f".format(h.unrealizedGainPercent)}%)",
                            color = if (isPositive) EmeraldLight else Color(0xFFFB7185),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { viewModel.deleteHolding(h) },
                        modifier = Modifier.size(28.dp).padding(start = 4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
