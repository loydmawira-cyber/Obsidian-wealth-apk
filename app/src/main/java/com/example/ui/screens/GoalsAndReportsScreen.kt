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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onAskAiToExplain: (String) -> Unit = { viewModel.askAi(it) },
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sym = userSettings.currency.symbol
    val isKenya = userSettings.region == GeographicRegion.EAST_AFRICA || userSettings.currency == SupportedCurrency.KES

    val totalGoalTarget = goals.sumOf { it.targetAmount }
    val totalGoalSaved = goals.sumOf { it.currentAmount }
    val overallGoalProgress = if (totalGoalTarget > 0) (totalGoalSaved / totalGoalTarget) * 100.0 else 0.0
    // Which goal the deposit / withdraw dialog is open for (second value: true = deposit).
    var goalMoneyTarget by remember { mutableStateOf<Pair<com.example.data.models.GoalEntity, Boolean>?>(null) }
    goalMoneyTarget?.let { (targetGoal, isDeposit) ->
        com.example.ui.components.GoalMoneyDialog(
            goalTitle = targetGoal.title,
            isDeposit = isDeposit,
            maxAmount = if (isDeposit) null else targetGoal.currentAmount,
            formatAmount = { viewModel.formatAmount(it) },
            onDismiss = { goalMoneyTarget = null },
            onConfirm = { amt ->
                if (isDeposit) viewModel.contributeGoal(targetGoal, amt) else viewModel.withdrawFromGoal(targetGoal, amt)
            }
        )
    }

    // Audit statement built ONLY from recorded data.
    //
    // Both previous branches hardcoded figures and presented them as audited output: a fixed
    // "MoM Growth +KSh 165,000 (+2.1%)", a fixed portfolio allocation (Equities 3,450,000 /
    // IFB 2,600,000 / MMF 1,450,000 / Cash 800,000), a fixed "Blended Portfolio CAGR 15.4%"
    // and "XIRR 16.8% CAGR", a split of revolving vs term debt invented as 15%/85% of the
    // total, and an "OBSIDIAN AI RESILIENCE INDEX" of 91 or 88 out of 100. None of it derived
    // from the user's records. A document labelled "audit statement" must not contain an
    // invented number.
    val hasData = summary.transactionCount > 0 || summary.holdingCount > 0 ||
        summary.debtAccountCount > 0 || summary.goalCount > 0

    val sampleReportText = if (!hasData) """
==================================================
           OBSIDIAN WEALTH AUDIT STATEMENT
==================================================

No financial records in this vault.

Add transactions, holdings, cards, loans or goals and regenerate this
statement to produce an audit of your actual position.
==================================================
""".trimIndent() else """
==================================================
           OBSIDIAN WEALTH AUDIT STATEMENT
          Period: Current Month Financials
==================================================

1. NET WORTH SUMMARY
   • Total Assets:              ${viewModel.formatAmount(summary.totalAssets)}
   • Total Liabilities:         ${viewModel.formatAmount(summary.totalLiabilities)}
   -------------------------------------------------
   • NET WORTH:                 ${viewModel.formatAmount(summary.totalNetWorth)}
   • MoM Change:                not available (no prior-period snapshot recorded)

2. CASH FLOW STATEMENT
   • Gross Inflow:              ${viewModel.formatAmount(summary.totalInflow)}
   • Gross Outflow:             ${viewModel.formatAmount(summary.totalOutflow)}
   -------------------------------------------------
   • Net Retained Surplus:      ${viewModel.formatAmount(summary.netCashRetained)}
   • Savings Rate:              ${if (summary.totalInflow > 0) "%.1f%%".format(summary.savingsRate) else "not computable (no income recorded)"}
   • Transactions Recorded:     ${summary.transactionCount}

3. PORTFOLIO
   • Holdings Recorded:         ${summary.holdingCount}
   • Market Value:              ${viewModel.formatAmount(summary.portfolioValue)}
   • Cost Basis:                ${viewModel.formatAmount(summary.portfolioCost)}
   • Unrealised Gain/Loss:      ${viewModel.formatAmount(summary.portfolioValue - summary.portfolioCost)}
   • Return on Cost Basis:      ${if (summary.portfolioCost > 0) "%.1f%%".format(summary.portfolioReturnPercent) else "not computable (no cost basis)"}
   • Note: simple return on cost, not XIRR/CAGR. A money-weighted return
     requires the date and amount of every contribution, which is not recorded.

4. DEBT AUDIT & DTI
   • Debt Accounts Recorded:    ${summary.debtAccountCount}
   • Total Outstanding:         ${viewModel.formatAmount(summary.totalDebt)}
   • Debt Servicing / Mo:       ${viewModel.formatAmount(summary.monthlyDebtServicing)}
   • DTI Ratio:                 ${if (summary.totalInflow > 0) "%.1f%%".format(summary.dtiRatio) else "not computable (no income recorded)"}
   • Note: the revolving/term split is not itemised here because it is not
     derivable from the recorded totals alone.

5. GOALS
   • Goals Recorded:            ${summary.goalCount}
   • Total Target:              ${viewModel.formatAmount(totalGoalTarget)}
   • Total Saved:               ${viewModel.formatAmount(totalGoalSaved)}
   • Overall Progress:          ${if (totalGoalTarget > 0) "%.1f%%".format(overallGoalProgress) else "not computable (no targets set)"}
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
                        Text(if (goals.isEmpty()) "No Goals Set" else "${goals.maxByOrNull { it.currentAmount / it.targetAmount.coerceAtLeast(1.0) }?.title ?: "Goal"} (${"%.0f".format((goals.maxByOrNull { it.currentAmount / it.targetAmount.coerceAtLeast(1.0) }?.let { it.currentAmount / it.targetAmount } ?: 0.0) * 100)}%)", color = GoldLight, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            D3FinancialTrendsDashboard(
                userSettings = userSettings,
                summary = summary,
                snapshots = viewModel.snapshots.collectAsState().value
            )
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

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { goalMoneyTarget = goal to false },
                            enabled = goal.currentAmount > 0,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Withdraw", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { goalMoneyTarget = goal to true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Deposit", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
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

        item {
            WhatIfCalculatorCard(
                summary = summary,
                goals = goals,
                viewModel = viewModel,
                onAskAiToExplain = onAskAiToExplain
            )
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

@Composable
fun WhatIfCalculatorCard(
    summary: com.example.ui.viewmodel.FinanceSummary,
    goals: List<GoalEntity>,
    viewModel: FinanceViewModel,
    onAskAiToExplain: (String) -> Unit
) {
    var extraSavingsInput by remember { mutableStateOf("10000") }
    val extraSavings = extraSavingsInput.toDoubleOrNull() ?: 0.0

    val currentInflow = summary.totalInflow
    val currentRetained = summary.netCashRetained
    val newMonthlyRetainedCash = currentRetained + extraSavings
    val newSavingsRate = if (currentInflow > 0) ((currentRetained + extraSavings) / currentInflow * 100.0).coerceIn(0.0, 100.0) else 0.0
    val additionalAnnualSavings = extraSavings * 12.0

    val primaryGoal = goals.firstOrNull()
    val goalImpactText = if (primaryGoal != null && primaryGoal.targetAmount > 0) {
        val remaining = (primaryGoal.targetAmount - primaryGoal.currentAmount).coerceAtLeast(0.0)
        val currentMonthly = primaryGoal.monthlyContribution
        val currentMonths = if (currentMonthly > 0) Math.ceil(remaining / currentMonthly).toInt() else primaryGoal.monthsRemaining
        val newMonthly = currentMonthly + extraSavings
        val newMonths = if (newMonthly > 0) Math.ceil(remaining / newMonthly).toInt() else 0
        val timeSaved = maxOf(0, currentMonths - newMonths)
        "${primaryGoal.title}: $newMonths mos ($timeSaved mos faster)"
    } else {
        "No active goals set"
    }

    FinCard(
        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("WHAT-IF FINANCIAL CALCULATOR", color = SovereignGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Simulate extra monthly savings. All calculations are strictly deterministic Kotlin logic.", color = TextSecondary, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = extraSavingsInput,
                onValueChange = { extraSavingsInput = it },
                label = { Text("Extra Monthly Savings (${viewModel.userSettings.value.currency.symbol})", fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = SovereignGold,
                    unfocusedBorderColor = ObsidianBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(2000.0, 5000.0, 10000.0, 25000.0).forEach { amt ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { extraSavingsInput = amt.toInt().toString() },
                        color = ObsidianSurfaceVariant,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Text(
                            text = "+${viewModel.formatCompact(amt)}",
                            color = IndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("New Monthly Retained Cash", color = TextSecondary, fontSize = 11.sp)
                    Text(viewModel.formatAmount(newMonthlyRetainedCash), color = EmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("New Savings Rate", color = TextSecondary, fontSize = 11.sp)
                    Text("${"%.1f".format(newSavingsRate)}%", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Additional Annual Savings", color = TextSecondary, fontSize = 11.sp)
                    Text("+${viewModel.formatAmount(additionalAnnualSavings)}/yr", color = SovereignGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goal Timeline Impact", color = TextSecondary, fontSize = 11.sp)
                    Text(goalImpactText, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val prompt = "Explain my What-If calculation: If I save an extra ${viewModel.formatAmount(extraSavings)} per month, my retained cash becomes ${viewModel.formatAmount(newMonthlyRetainedCash)}, my savings rate increases to ${"%.1f".format(newSavingsRate)}%, and my additional annual savings is ${viewModel.formatAmount(additionalAnnualSavings)}."
                    onAskAiToExplain(prompt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask AI Advisor to Explain What-If", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
