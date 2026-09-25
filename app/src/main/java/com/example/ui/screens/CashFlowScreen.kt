package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import com.example.ui.components.ReceiptPhotoDialog
import com.example.ui.components.StatementImportDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Category
import com.example.data.models.GeographicRegion
import com.example.data.models.SupportedCurrency
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import com.example.ui.components.DonutChart
import com.example.ui.components.DonutSlice
import com.example.ui.components.FinCard
import com.example.ui.components.GoldBadge
import com.example.ui.components.MetricBadge
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
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CashFlowScreen(
    viewModel: FinanceViewModel,
    onQuickAdd: () -> Unit,
    onAiSmartLog: () -> Unit,
    onReviewImported: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val month by viewModel.monthCashFlow.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val sym = userSettings.currency.symbol
    val isKenya = userSettings.region == GeographicRegion.EAST_AFRICA || userSettings.currency == SupportedCurrency.KES

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showStatementDialog by remember { mutableStateOf(false) }

    // The ledger opens short and grows on demand; it resets whenever the month, search or filter changes.
    var actionTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var editTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var visibleCount by remember(month.monthStart, searchQuery, selectedFilter) { mutableStateOf(15) }

    val filteredTransactions = transactions.filter { tx ->
        val matchesSearch = tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.account.contains(searchQuery, ignoreCase = true) ||
                tx.category.name.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "INFLOWS" -> tx.type == TransactionType.INCOME
            "OUTFLOWS" -> tx.type == TransactionType.EXPENSE
            "RECURRING" -> tx.isRecurring
            else -> true
        }
        val inMonth = tx.dateMillis >= month.monthStart && tx.dateMillis < month.monthEnd
        matchesSearch && matchesFilter && inMonth
    }

    // Dynamic Donut chart slices for expenses
    val expenseTransactions = transactions.filter {
        it.type == TransactionType.EXPENSE && it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" &&
            it.category != Category.INVESTMENT_SIP && it.category != Category.GOAL_SAVINGS &&
            it.category != Category.DEBT_PAYMENT &&
            it.dateMillis >= month.monthStart && it.dateMillis < month.monthEnd
    }
    val donutSlices = if (expenseTransactions.isNotEmpty()) {
        val categoryColors = mapOf(
            Category.HOUSING to ElectricIndigo,
            Category.FOOD_DINING to EmeraldGrowth,
            Category.TRANSPORT to CyanAccent,
            Category.SHOPPING to Color(0xFFF59E0B),
            Category.INVESTMENT_SIP to Color(0xFF34D399),
            Category.UTILITIES to Color(0xFFEC4899),
            Category.SUBSCRIPTIONS to Color(0xFFA855F7),
            Category.HEALTHCARE to Color(0xFF38BDF8),
            Category.OTHER to Color(0xFF94A3B8)
        )
        expenseTransactions.groupBy { it.category }
            .map { (cat, txs) ->
                val label = when (cat) {
                    Category.HOUSING -> "Housing & Rent"
                    Category.FOOD_DINING -> "Food & Dining"
                    Category.TRANSPORT -> "Transport & Fuel"
                    Category.INVESTMENT_SIP -> "Investment & SIP"
                    Category.UTILITIES -> "Utilities & Power"
                    Category.SUBSCRIPTIONS -> "Subscriptions"
                    Category.SHOPPING -> "Shopping"
                    Category.HEALTHCARE -> "Healthcare"
                    else -> "Other"
                }
                DonutSlice(
                    label = label,
                    value = txs.sumOf { it.amount },
                    color = categoryColors[cat] ?: Color(0xFF94A3B8)
                )
            }
            .sortedByDescending { it.value }
    } else {
        emptyList()
    }

    val hasCashFlowData = summary.totalInflow > 0 || summary.totalOutflow > 0 || summary.transactionCount > 0
    // The monthly cap is the sum of the user's own budgets; nothing is assumed.
    val budgetCap = budgets.sumOf { it.monthlyLimit }
    val budgetedCategories = budgets.map { it.category }.toSet()
    val budgetSpent = transactions
        .filter {
            it.type == TransactionType.EXPENSE && it.category in budgetedCategories &&
                it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" &&
                it.category != Category.DEBT_PAYMENT &&
                it.dateMillis >= month.monthStart && it.dateMillis < month.monthEnd
        }
        .sumOf { it.amount }
    val budgetProgress = if (budgetCap > 0) (budgetSpent / budgetCap).toFloat().coerceIn(0f, 1f) else 0f
    val bufferRemaining = if (budgetCap > 0) (budgetCap - budgetSpent).coerceAtLeast(0.0) else 0.0
    val nowCal = java.util.Calendar.getInstance()
    val daysLeft = nowCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH) - nowCal.get(java.util.Calendar.DAY_OF_MONTH) + 1
    val dailyPace = if (budgetCap > 0 && month.isCurrentMonth) bufferRemaining / daysLeft else 0.0
    val pendingImportedCount = transactions.count { it.importStatus == "PENDING_REVIEW" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (pendingImportedCount > 0) {
            item {
                Button(
                    onClick = onReviewImported,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
                ) {
                    Text("Review $pendingImportedCount detected transaction${if (pendingImportedCount == 1) "" else "s"}")
                }
            }
        }
        // Month switcher with opening / closing balances
        item { MonthCashFlowCard(viewModel = viewModel) }

        // Cash Flow Hero Waterfall Summary
        item {
            FinCard(
                border = BorderStroke(1.dp, GoldBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NET CASH THIS MONTH",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = (if ((month.inflow - month.outflow) >= 0) "+" else "") + viewModel.formatAmount((month.inflow - month.outflow)),
                            color = if ((month.inflow - month.outflow) >= 0) EmeraldLight else CrimsonDebt,
                            fontSize = if (Math.abs((month.inflow - month.outflow)) >= 100_000) 21.sp else 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    GoldBadge(
                        text = "${"%.1f".format(summary.savingsRate)}% saved (30 days)"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Inflow vs Outflow Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F1823),
                        border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(22.dp).background(EmeraldGrowth.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inflow", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatAmount(month.inflow),
                                color = EmeraldLight,
                                fontSize = if (month.inflow >= 100_000) 13.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF18111D),
                        border = BorderStroke(1.dp, CrimsonDebt.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(22.dp).background(CrimsonDebt.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Outflow", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatAmount(month.outflow),
                                color = Color(0xFFFB7185),
                                fontSize = if (month.outflow >= 100_000) 13.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Monthly Cap & Run-Rate Tracker
        item {
            FinCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MONTHLY CAP & PACING",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (budgetCap > 0) "${viewModel.formatCompact(budgetSpent)} / ${viewModel.formatCompact(budgetCap)}" else "No budgets set",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { budgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (budgetProgress > 0.85f) AmberWarning else EmeraldGrowth,
                    trackColor = ObsidianBorderSubtle
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (month.isCurrentMonth) "$daysLeft days left this month" else "Month closed",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (budgetCap > 0) {
                            if (month.isCurrentMonth) "${viewModel.formatCompact(bufferRemaining)} left (${viewModel.formatCompact(dailyPace)}/day)"
                            else "${viewModel.formatCompact(bufferRemaining)} unspent"
                        } else "Set budgets below to track a monthly cap",
                        color = if (budgetCap > 0) EmeraldLight else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Category Allocation & Donut Chart
        item {
            FinCard {
                Text(
                    text = "CATEGORY OUTFLOW BREAKDOWN",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (donutSlices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No outflow transactions recorded yet.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            slices = donutSlices,
                            sizeDp = 120.dp,
                            centerTitle = "Total Spent",
                            centerSubtitle = viewModel.formatCompact(expenseTransactions.sumOf { it.amount })
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            donutSlices.take(4).forEach { slice ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(slice.color, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(slice.label, color = TextPrimary, fontSize = 12.sp)
                                    }
                                    Text(
                                        viewModel.formatCompact(slice.value),
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { BudgetsCard(viewModel = viewModel, transactions = transactions, monthStart = month.monthStart, monthEnd = month.monthEnd) }

        // Ledger Activity Header & Search & Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REAL-TIME LEDGER (${filteredTransactions.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showReceiptDialog = true },
                            color = CyanAccent.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Receipt", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showStatementDialog = true },
                            color = SovereignGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Import", color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAiSmartLog() },
                            color = SovereignGold.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("AI Log", color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onQuickAdd() },
                            color = EmeraldGrowth.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+ Add", color = EmeraldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions, accounts, notes...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Filter Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("ALL", "INFLOWS", "OUTFLOWS", "RECURRING")
                    items(filters) { f ->
                        val isSel = selectedFilter == f
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedFilter = f },
                            color = if (isSel) SovereignGold else ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSel) SovereignGold else ObsidianBorderSubtle)
                        ) {
                            Text(
                                text = f,
                                color = if (isSel) Color(0xFF0A0700) else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Transactions list: newest first, grouped by day, limited to what has been revealed so far.
        val dayHeaderFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())
        filteredTransactions.take(visibleCount)
            .groupBy { dayHeaderFormat.format(Date(it.dateMillis)) }
            .forEach { (dayLabel, dayTx) ->
        item(key = "day_$dayLabel") {
            val dayNet = dayTx.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dayLabel, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (dayNet >= 0) "+${viewModel.formatAmount(dayNet)}" else "-${viewModel.formatAmount(-dayNet)}",
                    color = if (dayNet >= 0) EmeraldLight else TextMuted,
                    fontSize = 11.sp
                )
            }
        }
        items(dayTx, key = { "tx_${it.id}" }) { tx ->
            val isIncome = tx.type == TransactionType.INCOME
            val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
            val formattedDate = remember(tx.dateMillis) { dateFormatter.format(Date(tx.dateMillis)) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .combinedClickable(onClick = {}, onLongClick = { actionTx = tx }),
                shape = RoundedCornerShape(14.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    if (isIncome) EmeraldGrowth.copy(alpha = 0.15f) else Color(0x22F43F5E),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(tx.category),
                                contentDescription = null,
                                tint = if (isIncome) EmeraldLight else Color(0xFFFB7185),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = tx.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (tx.isRecurring) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CyanAccent.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "MANDATE",
                                            color = CyanAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = tx.account,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(text = " • ", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = formattedDate,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        val formattedAmt = viewModel.formatAmount(tx.amount)
                        val displayAmt = if (isIncome) "+$formattedAmt" else "-$formattedAmt"
                        Text(
                            text = displayAmt,
                            color = if (isIncome) EmeraldLight else TextPrimary,
                            fontSize = if (tx.amount >= 100_000) 12.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
            }

        if (filteredTransactions.size > 15) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (visibleCount < filteredTransactions.size) {
                        androidx.compose.material3.TextButton(onClick = { visibleCount += 15 }) {
                            Text(
                                "Show 15 more (${filteredTransactions.size - visibleCount} left)",
                                color = SovereignGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (visibleCount > 15) {
                        androidx.compose.material3.TextButton(onClick = { visibleCount = 15 }) {
                            Text("Show less", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    actionTx?.let { tx ->
        com.example.ui.components.TransactionActionDialog(
            transaction = tx,
            formatAmount = { viewModel.formatAmount(it) },
            onDismiss = { actionTx = null },
            onEdit = { editTx = tx },
            onDelete = { viewModel.deleteTransaction(tx) }
        )
    }

    editTx?.let { tx ->
        com.example.ui.components.EditTransactionDialog(
            transaction = tx,
            onDismiss = { editTx = null },
            onSave = { viewModel.updateTransaction(it) }
        )
    }

    if (showReceiptDialog) {
        ReceiptPhotoDialog(
            viewModel = viewModel,
            onDismiss = { showReceiptDialog = false }
        )
    }

    if (showStatementDialog) {
        StatementImportDialog(
            viewModel = viewModel,
            onDismiss = { showStatementDialog = false }
        )
    }
}

fun getCategoryIcon(category: Category): ImageVector {
    return when (category) {
        Category.SALARY, Category.FREELANCE, Category.DIVIDENDS, Category.RENTAL -> Icons.Default.LocalAtm
        Category.FOOD_DINING -> Icons.Default.Fastfood
        Category.HOUSING -> Icons.Default.Home
        Category.TRANSPORT -> Icons.Default.Payments
        Category.SHOPPING -> Icons.Default.ShoppingBag
        Category.HEALTHCARE -> Icons.Default.MedicalServices
        Category.SUBSCRIPTIONS -> Icons.Default.Subscriptions
        Category.INVESTMENT_SIP -> Icons.Default.Payments
        Category.LOAN_EMI -> Icons.Default.Payments
        Category.GOAL_SAVINGS -> Icons.Default.Savings
        else -> Icons.Default.LocalAtm
    }
}
