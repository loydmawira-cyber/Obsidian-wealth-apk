package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InvestmentsScreen(
    viewModel: FinanceViewModel,
    onAddHolding: () -> Unit,
    onHoldingLongPress: (HoldingEntity) -> Unit = {},
    onAddSip: () -> Unit,
    onSipLongPress: (SipEntity) -> Unit = {},
    onOpenAiAdvisor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val sips by viewModel.sips.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sym = userSettings.currency.symbol
    val selectedHoldings = holdings.filter { it.currencyCode == userSettings.currency.code }
    val selectedSips = sips.filter { it.currencyCode == userSettings.currency.code }
    val activeSips = selectedSips.filter { it.isActive && it.monthlyAmount > 0.0 }
    val activeSipSum = activeSips.sumOf { it.monthlyAmount }

    val totalPortfolioValue = selectedHoldings.sumOf { it.totalValue }
    val totalUnrealizedGain = selectedHoldings.sumOf { it.unrealizedGain }

    // Assign each holding to exactly one class so allocation slices always add up to the portfolio total.
    val goldHoldings = selectedHoldings.filter {
        it.type == HoldingType.GOLD || it.symbol == "GLD"
    }
    val goldIds = goldHoldings.map { it.id }.toSet()
    val goldVal = goldHoldings.sumOf { it.totalValue }

    val mutualFundHoldings = selectedHoldings.filter {
        it.id !in goldIds && (it.type == HoldingType.MUTUAL_FUND || it.symbol == "BND" || it.symbol.startsWith("IFB"))
    }
    val mutualFundIds = mutualFundHoldings.map { it.id }.toSet()
    val mutualFundVal = mutualFundHoldings.sumOf { it.totalValue }

    val equityHoldings = selectedHoldings.filter {
        it.id !in goldIds && it.id !in mutualFundIds && (it.type == HoldingType.STOCK || it.type == HoldingType.ETF)
    }
    val equityIds = equityHoldings.map { it.id }.toSet()
    val equityVal = equityHoldings.sumOf { it.totalValue }

    val cryptoHoldings = selectedHoldings.filter { it.id !in goldIds && it.id !in mutualFundIds && it.id !in equityIds && it.type == HoldingType.CRYPTO }
    val cryptoVal = cryptoHoldings.sumOf { it.totalValue }

    val allocationSlices = buildList {
        if (equityVal > 0) {
            add(
                D3AllocationSlice(
                    key = "EQUITIES",
                    name = "Equities",
                    value = equityVal,
                    primaryColor = EmeraldGrowth,
                    gradientColors = listOf(EmeraldGrowth, CyanAccent),
                    holdingsCount = equityHoldings.size,
                    xirrReturnPercent = 0.0,
                    description = "Stocks and ETFs recorded by you.",
                    underlyingAssets = equityHoldings.map { "${it.symbol} (${viewModel.formatCompact(it.totalValue)})" }
                )
            )
        }
        if (mutualFundVal > 0) {
            add(
                D3AllocationSlice(
                    key = "MUTUAL_FUNDS",
                    name = "Funds & Bonds",
                    value = mutualFundVal,
                    primaryColor = ElectricIndigo,
                    gradientColors = listOf(ElectricIndigo, IndigoLight),
                    holdingsCount = mutualFundHoldings.size,
                    xirrReturnPercent = 0.0,
                    description = "Funds, bonds, and money-market holdings recorded by you.",
                    underlyingAssets = mutualFundHoldings.map { "${it.symbol} (${viewModel.formatCompact(it.totalValue)})" }
                )
            )
        }
        if (goldVal > 0) {
            add(
                D3AllocationSlice(
                    key = "GOLD",
                    name = "Gold & Bullion",
                    value = goldVal,
                    primaryColor = Color(0xFFF59E0B),
                    gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFFDE68A)),
                    holdingsCount = goldHoldings.size,
                    xirrReturnPercent = 0.0,
                    description = "Gold holdings recorded by you.",
                    underlyingAssets = goldHoldings.map { "${it.symbol} (${viewModel.formatCompact(it.totalValue)})" }
                )
            )
        }
        if (cryptoVal > 0) {
            add(
                D3AllocationSlice(
                    key = "CRYPTO",
                    name = "Crypto Reserve",
                    value = cryptoVal,
                    primaryColor = CyanAccent,
                    gradientColors = listOf(CyanAccent, Color(0xFF38BDF8)),
                    holdingsCount = cryptoHoldings.size,
                    xirrReturnPercent = 0.0,
                    description = "Digital asset holdings.",
                    underlyingAssets = cryptoHoldings.map { "${it.symbol} (${viewModel.formatCompact(it.totalValue)})" }
                )
            )
        }
    }

    val sipColors = listOf(EmeraldGrowth, CyanAccent, ElectricIndigo, SovereignGold, Color(0xFFFB7185), Color(0xFF84CC16))
    val sipPlanSlices = activeSips.mapIndexed { index, sip ->
        val color = sipColors[index % sipColors.size]
        D3AllocationSlice(
            key = "sip_${sip.id}",
            name = sip.fundName,
            value = sip.monthlyAmount,
            primaryColor = color,
            gradientColors = listOf(color, color.copy(alpha = 0.55f)),
            holdingsCount = 1,
            xirrReturnPercent = 0.0,
            description = "Planned monthly contribution; not automatically debited or included in portfolio value.",
            underlyingAssets = listOf("${viewModel.formatAmount(sip.monthlyAmount, sip.currencyCode)} per month")
        )
    }

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
                            text = "RECORDED PORTFOLIO VALUE",
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
                        text = if (summary.portfolioCost <= 0.0) {
                            "No cost basis"
                        } else {
                            // Simple return on cost basis, not XIRR (contribution dates are not
                            // recorded). Sign is derived, not assumed positive.
                            val r = summary.portfolioReturnPercent
                            "${if (r >= 0) "+" else ""}${"%.1f".format(r)}% return"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unrealized change on manually priced holdings",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                            text = "${if (totalUnrealizedGain > 0) "+" else ""}${viewModel.formatAmount(totalUnrealizedGain, userSettings.currency.code)}",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Prices are entered manually. Values are not live market quotes.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        item {
            FinCard {
                Text("Values are grouped by ${userSettings.currency.code}. Other or unresolved currency holdings remain listed below and are excluded from these totals; no FX conversion is applied.", color = TextMuted, fontSize = 11.sp)
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

                if (allocationSlices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No asset holdings found. Tap + Asset to build your portfolio.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    D3InteractiveDonutChart(
                        slices = allocationSlices,
                        totalPortfolioValue = totalPortfolioValue,
                        currencySymbol = sym,
                        formatAmount = { viewModel.formatAmount(it) },
                        chartSize = 220.dp
                    )
                }
            }
        }

        // Planned recurring contribution records
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RECURRING CONTRIBUTION PLANS",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${viewModel.formatAmount(activeSipSum, userSettings.currency.code)}/month planned — not debited by the app",
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
                            Text("Add plan", color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (sipPlanSlices.isNotEmpty()) {
            item {
                FinCard {
                    Text(
                        text = "MONTHLY SIP PLAN ALLOCATION",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Planned contributions only — separate from holdings. Other or unresolved currencies stay out of this total.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    D3InteractiveDonutChart(
                        slices = sipPlanSlices,
                        totalPortfolioValue = activeSipSum,
                        currencySymbol = sym,
                        formatAmount = { viewModel.formatAmount(it, userSettings.currency.code) },
                        chartSize = 190.dp,
                        centerTitle = "PLANNED / MONTH",
                        aggregateDetail = "${activeSips.size} active plans"
                    )
                }
            }
        }

        // SIP Items
        items(sips, key = { "sip_${it.id}" }) { sip ->
            FinCard(
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { onSipLongPress(sip) }
                )
            ) {
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
                                text = "${sip.category} • Planned for day ${sip.debitDayOfMonth}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${viewModel.formatAmount(sip.monthlyAmount, sip.currencyCode)}/mo",
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
                        text = "RECORDED HOLDINGS",
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
                            Text("Add investment", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Holdings List
        items(holdings, key = { "holding_${it.id}" }) { h ->
            val isPositive = h.unrealizedGain >= 0
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onHoldingLongPress(h) }
                    ),
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
                                text = "${if (h.shares % 1.0 == 0.0) h.shares.toInt() else h.shares} units • ${h.currencyCode ?: "currency unknown"} ${"%,.2f".format(h.currentPrice)} current (Avg ${"%,.2f".format(h.avgBuyPrice)})",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            if (h.currencyCode.isNullOrBlank()) {
                                Text("Currency unresolved — long-press, then Edit to confirm; amounts are not converted.", color = SovereignGold, fontSize = 9.sp)
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = viewModel.formatAmount(h.totalValue, h.currencyCode),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = (if (isPositive) "+" else "−") + viewModel.formatAmount(Math.abs(h.unrealizedGain), h.currencyCode) + " (${if (isPositive) "+" else ""}${"%.1f".format(h.unrealizedGainPercent)}%)",
                            color = if (isPositive) EmeraldLight else Color(0xFFFB7185),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
