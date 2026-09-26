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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TimeToLeave
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CreditCardEntity
import com.example.data.models.GeographicRegion
import com.example.data.models.LoanEntity
import com.example.data.models.SupportedCurrency
import com.example.ui.components.CreditCardPurchaseDialog
import com.example.ui.components.DebtPayoffCalculator
import com.example.ui.components.FinCard
import com.example.ui.components.HeroGradientCard
import com.example.ui.components.LoanTopUpDialog
import com.example.ui.components.MetricBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDebt
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldBorder
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
import com.example.ui.viewmodel.PayoffStrategy

@Composable
fun DebtCenterScreen(
    viewModel: FinanceViewModel,
    onPayCard: (CreditCardEntity) -> Unit,
    onPayLoan: (LoanEntity) -> Unit,
    onAddLoan: () -> Unit,
    onAddCreditCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val creditCards by viewModel.creditCards.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val strategy by viewModel.payoffStrategy.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sym = userSettings.currency.symbol
    val isKenya = userSettings.region == GeographicRegion.EAST_AFRICA || userSettings.currency == SupportedCurrency.KES

    val selectedCards = creditCards.filter { !it.currencyCode.isNullOrBlank() }
    val selectedLoans = loans.filter { !it.currencyCode.isNullOrBlank() }
    var cardToSpend by remember { mutableStateOf<CreditCardEntity?>(null) }
    var loanToTopUp by remember { mutableStateOf<LoanEntity?>(null) }
    val totalCardBalance = selectedCards.sumOf { it.currentBalance }
    val totalLoanBalance = selectedLoans.sumOf { it.remainingBalance }
    val aggregateDebt = totalCardBalance + totalLoanBalance
    val totalLimit = selectedCards.sumOf { it.creditLimit }
    val overallUtilization = if (totalLimit > 0) (totalCardBalance / totalLimit) * 100.0 else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Debt Card
        item {
            HeroGradientCard {
                Text("Amounts keep their recorded numbers and are labeled ${userSettings.currency.code}; different currencies are not converted before totals are combined.", color = TextMuted, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL LIABILITIES & DEBT",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = viewModel.formatAmount(aggregateDebt),
                            color = Color(0xFFFB7185),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    val hasRecentIncome = summary.recentInflow > 0 && selectedLoans.isNotEmpty()
                    MetricBadge(
                        text = if (!hasRecentIncome) "Loan EMI/inflow: N/A" else "Loan EMI/inflow: ${"%.1f".format(summary.dtiRatio)}% est.",
                        isPositive = false
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
                        Text("Recorded loan EMIs", color = TextMuted, fontSize = 11.sp)
                        Text("${viewModel.formatCompact(summary.monthlyDebtServicing)}/mo", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Credit Utilization", color = TextMuted, fontSize = 11.sp)
                        Text("${"%.1f".format(overallUtilization)}%", color = EmeraldLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Payoff estimate", color = TextMuted, fontSize = 11.sp)
                        Text(
                            if (creditCards.isEmpty() && loans.isEmpty()) "No debt recorded"
                            else "See calculator below",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Payoff Strategy Selector (Avalanche vs Snowball)
        item {
            FinCard(
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "ACCELERATED DEBT PAYOFF STRATEGY",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Avalanche Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setPayoffStrategy(PayoffStrategy.AVALANCHE) },
                        color = if (strategy == PayoffStrategy.AVALANCHE) SovereignGold else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                "Debt Avalanche (Max APR)",
                                color = if (strategy == PayoffStrategy.AVALANCHE) Color(0xFF0A0700) else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (strategy == PayoffStrategy.AVALANCHE) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        }
                    }

                    // Snowball Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setPayoffStrategy(PayoffStrategy.SNOWBALL) },
                        color = if (strategy == PayoffStrategy.SNOWBALL) SovereignGold else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                "Debt Snowball (Min Bal)",
                                color = if (strategy == PayoffStrategy.SNOWBALL) Color(0xFF0A0700) else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (strategy == PayoffStrategy.SNOWBALL) FontWeight.ExtraBold else FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (creditCards.isEmpty() && loans.isEmpty()) {
                        "✨ Freedom: No active debt accounts or credit liabilities recorded. You are currently 100% debt-free!"
                    } else if (strategy == PayoffStrategy.AVALANCHE) {
                        if (isKenya)
                            "⚡ Strategy: Target highest APR card first to save on interest charges."
                        else
                            "⚡ Strategy: Target highest APR card first to save on interest charges."
                    } else {
                        if (isKenya)
                            "🎯 Strategy: Clear smallest card balance first for an immediate psychological momentum win."
                        else
                            "🎯 Strategy: Clear smallest card balance first for an immediate psychological momentum win."
                    },
                    color = GoldLight,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Interactive Debt Payoff Calculator & Visualizer
        item {
            // No fallback demo numbers: a debt-free/new vault must show blank inputs, not a
            // fabricated $15,000 balance / 16.5% APR / $450 payment as if it were the user's own.
            val maxApr = selectedCards.maxByOrNull { it.apr }?.apr
                ?: (selectedLoans.maxByOrNull { it.interestRate }?.interestRate ?: 0.0)
            val suggestedPayment = if (summary.monthlyDebtServicing > 50.0) summary.monthlyDebtServicing else 0.0

            DebtPayoffCalculator(
                initialBalance = aggregateDebt,
                initialApr = maxApr,
                initialMonthlyPayment = suggestedPayment,
                currencySymbol = sym,
                formatAmount = { viewModel.formatAmount(it, summary.currencyCode) },
                totalVaultDebt = aggregateDebt
            )
        }

        // Revolving Credit Cards Section Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REVOLVING CREDIT CARDS",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = onAddCreditCard,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Text("Add", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(creditCards, key = { "card_${it.id}" }) { card ->
            val util = card.utilizationPercent.toFloat()
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
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = card.cardName,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "•••• ${card.lastFour} • ${card.apr}% APR • Due in ${card.dueDateDays} days",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Button(
                            onClick = { onPayCard(card) },
                            enabled = !card.currencyCode.isNullOrBlank() && accounts.any { it.isActive && it.currencyCode == card.currencyCode },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp)
                        ) { Text("Pay", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = { cardToSpend = card },
                            enabled = !card.currencyCode.isNullOrBlank() && card.creditLimit > card.currentBalance,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp)
                        ) { Text("Spend", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Balance: ${viewModel.formatAmount(card.currentBalance, card.currencyCode)}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Limit: ${viewModel.formatAmount(card.creditLimit, card.currencyCode)} (${"%.1f".format(card.utilizationPercent)}% used)",
                        color = if (util > 30f) AmberWarning else EmeraldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (util / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (util > 30f) AmberWarning else CyanAccent,
                    trackColor = ObsidianBorderSubtle
                )
                if (card.currencyCode.isNullOrBlank()) {
                    Text("Confirm this legacy balance's currency before including it in totals or recording a payment:", color = AmberWarning, fontSize = 10.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SupportedCurrency.values().toList()) { currency ->
                            FilterChip(selected = false, onClick = { viewModel.resolveCardCurrency(card, currency.code) }, label = { Text(currency.code, fontSize = 10.sp) })
                        }
                    }
                }
            }
        }

        // Fixed Amortization Loans (EMIs)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FIXED AMORTIZATION LOANS & EMIS",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Button(
                    onClick = onAddLoan,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Add", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        items(loans, key = { "loan_${it.id}" }) { loan ->
            val paidPct = loan.paidPercent.toFloat()
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
                                .background(ElectricIndigo.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (loan.loanName.contains("Mortgage", ignoreCase = true) || loan.loanName.contains("Home", ignoreCase = true)) Icons.Default.Home else Icons.Default.TimeToLeave,
                                contentDescription = null,
                                tint = IndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = loan.loanName,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${loan.lender} • ${loan.interestRate}% APR • ${loan.remainingMonths} mo left",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Button(
                            onClick = { onPayLoan(loan) },
                            enabled = !loan.currencyCode.isNullOrBlank() && accounts.any { it.isActive && it.currencyCode == loan.currencyCode },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp)
                        ) { Text("Pay EMI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = { loanToTopUp = loan },
                            enabled = !loan.currencyCode.isNullOrBlank() && accounts.any { it.isActive && it.currencyCode == loan.currencyCode },
                            colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp)
                        ) { Text("Top up", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Remaining: ${viewModel.formatAmount(loan.remainingBalance, loan.currencyCode)}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "EMI: ${viewModel.formatAmount(loan.emiAmount, loan.currencyCode)}/mo (${"%.0f".format(loan.paidPercent)}% Paid)",
                        color = EmeraldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (paidPct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = EmeraldGrowth,
                    trackColor = ObsidianBorderSubtle
                )
                if (loan.currencyCode.isNullOrBlank()) {
                    Text("Confirm this legacy balance's currency before including it in totals or recording an EMI:", color = AmberWarning, fontSize = 10.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(SupportedCurrency.values().toList()) { currency ->
                            FilterChip(selected = false, onClick = { viewModel.resolveLoanCurrency(loan, currency.code) }, label = { Text(currency.code, fontSize = 10.sp) })
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    cardToSpend?.let { card ->
        CreditCardPurchaseDialog(
            card = card,
            formatAmount = { viewModel.formatAmount(it, card.currencyCode) },
            onDismiss = { cardToSpend = null },
            onConfirm = { merchant, amount, category, note ->
                viewModel.recordCreditCardPurchase(card, merchant, amount, category, note)
                cardToSpend = null
            }
        )
    }
    loanToTopUp?.let { loan ->
        LoanTopUpDialog(
            loan = loan, accounts = accounts,
            formatAmount = { viewModel.formatAmount(it, loan.currencyCode) },
            onDismiss = { loanToTopUp = null },
            onConfirm = { amount, account, note ->
                viewModel.recordLoanTopUp(loan, amount, account, note)
                loanToTopUp = null
            }
        )
    }
}
