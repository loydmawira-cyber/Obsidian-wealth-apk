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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
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
import com.example.data.models.GeographicRegion
import com.example.data.models.GoalEntity
import com.example.data.models.SupportedCurrency
import com.example.ui.components.CircularProgressRing
import com.example.ui.components.D3FinancialTrendsDashboard
import com.example.ui.components.FinCard
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

@Composable
fun GoalsAndReportsScreen(
    viewModel: FinanceViewModel,
    onAddGoal: () -> Unit,
    onExportReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sym = userSettings.currency.symbol
    val isKenya = userSettings.region == GeographicRegion.EAST_AFRICA || userSettings.currency == SupportedCurrency.KES

    val totalGoalTarget = goals.sumOf { it.targetAmount }
    val totalGoalSaved = goals.sumOf { it.currentAmount }
    val overallGoalProgress = if (totalGoalTarget > 0) (totalGoalSaved / totalGoalTarget) * 100.0 else 68.7
    val contributeStep = if (isKenya) 25000.0 else 250.0

    val sampleReportText = if (isKenya) """
==================================================
           OBSIDIAN WEALTH AUDIT STATEMENT        
          Period: Current Month Financials (KES)
==================================================

1. NET WORTH SUMMARY
   • Total Assets:              ${viewModel.formatAmount(summary.totalAssets)}
   • Total Liabilities:         ${viewModel.formatAmount(summary.totalLiabilities)}
   -------------------------------------------------
   • NET WORTH:                 ${viewModel.formatAmount(summary.totalNetWorth)}
   • MoM Growth:                +KSh 165,000 (+2.1%)

2. CASH FLOW STATEMENT
   • Gross Inflow (Salary/Inv): ${viewModel.formatAmount(summary.totalInflow)}
   • Gross Outflow (Exp/EMI):   ${viewModel.formatAmount(summary.totalOutflow)}
   -------------------------------------------------
   • Net Retained Surplus:      ${viewModel.formatAmount(summary.netCashRetained)}
   • Savings Velocity:          ${"%.1f".format(summary.savingsRate)}%

3. PORTFOLIO ALLOCATION
   • Equities (SCOM/EQTY/EABL): ${sym} 3,450,000 (42%)
   • Infrastructure Bonds (IFB):${sym} 2,600,000 (31%)
   • Money Market Funds (MMF):  ${sym} 1,450,000 (17%)
   • Liquid Cash & M-Pesa:      ${sym} 800,000   (10%)
   • Blended Portfolio CAGR:    15.4% p.a.

4. DEBT AUDIT & DTI
   • Revolving Cards Balance:   ${viewModel.formatAmount(summary.totalDebt * 0.15)}
   • Term Loans & Car EMI:      ${viewModel.formatAmount(summary.totalDebt * 0.85)}
   • Debt Servicing / Mo:       ${viewModel.formatAmount(summary.monthlyDebtServicing)}/mo
   • Blended DTI Ratio:         ${"%.1f".format(summary.dtiRatio)}% (Prime)

5. OBSIDIAN AI RESILIENCE INDEX: 91/100 (Prime)
   Generated on: System Date
==================================================
""".trimIndent()
    else """
==================================================
           OBSIDIAN WEALTH AUDIT STATEMENT        
          Period: Current Month Financials        
==================================================

1. NET WORTH SUMMARY
   • Total Assets:              ${viewModel.formatAmount(summary.totalAssets)}
   • Total Liabilities:         ${viewModel.formatAmount(summary.totalLiabilities)}
   -------------------------------------------------
   • NET WORTH:                 ${viewModel.formatAmount(summary.totalNetWorth)}
   • MoM Change:                +$3,420.00 (+1.4%)

2. CASH FLOW STATEMENT
   • Gross Inflow (Salary/Div): ${viewModel.formatAmount(summary.totalInflow)}
   • Gross Outflow (Exp/EMI):   ${viewModel.formatAmount(summary.totalOutflow)}
   -------------------------------------------------
   • Net Retained Surplus:      ${viewModel.formatAmount(summary.netCashRetained)}
   • Savings Velocity:          ${"%.1f".format(summary.savingsRate)}%

3. PORTFOLIO ALLOCATION
   • Total Equities & S&P 500:  $138,240.00 (75%)
   • Fixed Income (BND):        $23,960.00  (13%)
   • Digital Gold / Crypto:     $22,120.45  (12%)
   • Blended Portfolio XIRR:    16.8% CAGR

4. DEBT AUDIT & DTI
   • Revolving Cards Balance:   ${viewModel.formatAmount((summary.totalDebt - 38690.0).coerceAtLeast(0.0))}
   • Term Amortization Loans:   $38,690.00
   • Blended DTI Ratio:         ${"%.1f".format(summary.dtiRatio)}% (Prime)

5. OBSIDIAN AI RESILIENCE INDEX: 88/100 (Prime)
   Generated on: System Date
==================================================
""".trimIndent()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Goals Hero Card
        item {
            HeroGradientCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL CAPITAL ACCUMULATED",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${viewModel.formatCompact(totalGoalSaved)} / ${viewModel.formatCompact(totalGoalTarget)}",
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    CircularProgressRing(
                        progressPercent = overallGoalProgress.toFloat(),
                        sizeDp = 64.dp,
                        gradientColors = listOf(EmeraldGrowth, CyanAccent)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, GoldBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Goals", color = TextMuted, fontSize = 11.sp)
                        Text("${goals.size} Milestones", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Monthly Inflow", color = TextMuted, fontSize = 11.sp)
                        Text("+${viewModel.formatCompact(goals.sumOf { it.monthlyContribution })}/mo", color = EmeraldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Next Milestone", color = TextMuted, fontSize = 11.sp)
                        Text(if (isKenya) "Diani Holiday (78%)" else "Japan Tour (90%)", color = GoldLight, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            D3FinancialTrendsDashboard(userSettings = userSettings)
        }

        // Savings Goals Section Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAVINGS GOALS & MILESTONES",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAddGoal() },
                    color = EmeraldGrowth.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Goal", color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(goals, key = { "goal_${it.id}" }) { goal ->
            val progress = goal.progressPercent.toFloat()
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
                                .size(40.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = goal.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${goal.category} • Target ${goal.targetYear}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.contributeGoal(goal, contributeStep) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+${viewModel.formatCompact(contributeStep)}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${viewModel.formatCompact(goal.currentAmount)} / ${viewModel.formatCompact(goal.targetAmount)}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${"%.0f".format(goal.progressPercent)}% • ${goal.monthsRemaining} mo remaining",
                        color = EmeraldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldGrowth,
                    trackColor = ObsidianBorderSubtle
                )
            }
        }

        // Monthly Financial Reports & Audited Statements
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MONTHLY FINANCIAL AUDITS & STATEMENTS",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                FinCard(
                    border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.5f)),
                    onClick = { onExportReport(sampleReportText) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(SovereignGold.copy(alpha = 0.18f), CircleShape)
                                    .border(1.dp, SovereignGold.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Monthly Financial Audit Statement",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Comprehensive P&L, DTI & Asset Growth",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Button(
                            onClick = { onExportReport(sampleReportText) },
                            colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF0A0700), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View & Export", color = Color(0xFF0A0700), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
