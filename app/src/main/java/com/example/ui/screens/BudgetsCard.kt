package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.BudgetEntity
import com.example.data.models.Category
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import com.example.data.models.SupportedCurrency
import com.example.ui.components.FinCard
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDebt
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar

private fun prettyCategory(c: Category): String =
    c.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }

private val budgetCategories = listOf(
    Category.HOUSING, Category.FOOD_DINING, Category.UTILITIES, Category.TRANSPORT,
    Category.SHOPPING, Category.HEALTHCARE, Category.SUBSCRIPTIONS, Category.ENTERTAINMENT,
    Category.LOAN_EMI, Category.OTHER
)

@Composable
private fun SmallAddButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = EmeraldGrowth.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, EmeraldGrowth.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (selected) EmeraldGrowth.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
        border = BorderStroke(1.dp, if (selected) EmeraldGrowth else ObsidianBorderSubtle)
    ) {
        Text(
            text = label,
            color = if (selected) EmeraldLight else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun planningFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = EmeraldGrowth,
    unfocusedBorderColor = ObsidianBorder
)

// ---------------------------------------------------------------------------------------------
// Budgets
// ---------------------------------------------------------------------------------------------

@Composable
fun BudgetsCard(
    viewModel: FinanceViewModel,
    transactions: List<TransactionEntity>,
    monthStart: Long,
    monthEnd: Long
) {
    val budgets by viewModel.budgets.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val selectedBudgets = budgets.filter { it.currencyCode == userSettings.currency.code }
    var dialogFor by remember { mutableStateOf<BudgetEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val spentByCategory = transactions
        .filter {
            it.type == TransactionType.EXPENSE && it.currencyCode == userSettings.currency.code && it.dateMillis >= monthStart && it.dateMillis < monthEnd &&
                it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" &&
                it.transactionKind == com.example.data.models.TransactionKind.STANDARD
        }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    FinCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MONTHLY BUDGETS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Spending in the selected month against your limits", color = TextMuted, fontSize = 11.sp)
                }
                SmallAddButton("Set budget") { dialogFor = null; showDialog = true }
            }

            if (selectedBudgets.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("No budgets yet. Set a limit for a category to track it here.", color = TextMuted, fontSize = 12.sp)
            }

            selectedBudgets.forEach { b ->
                val spent = spentByCategory[b.category] ?: 0.0
                val fraction = if (b.monthlyLimit > 0) (spent / b.monthlyLimit).toFloat() else 0f
                val barColor = when {
                    fraction >= 1f -> CrimsonDebt
                    fraction >= 0.8f -> AmberWarning
                    else -> EmeraldGrowth
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dialogFor = b; showDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(prettyCategory(b.category), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${viewModel.formatAmount(spent, b.currencyCode)} / ${viewModel.formatAmount(b.monthlyLimit, b.currencyCode)}",
                            color = if (fraction >= 1f) Color(0xFFFB7185) else TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(ObsidianSurfaceVariant, RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(barColor, RoundedCornerShape(3.dp))
                        )
                    }
                    if (fraction >= 1f) {
                        Text(
                            "Over budget by ${viewModel.formatAmount(spent - b.monthlyLimit, b.currencyCode)}",
                            color = Color(0xFFFB7185),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        SetBudgetDialog(
            existing = dialogFor,
            defaultCurrency = userSettings.currency,
            onDismiss = { showDialog = false },
            onSave = { cat, limit, currencyCode -> viewModel.saveBudget(cat, limit, currencyCode) },
            onRemove = { dialogFor?.let { viewModel.deleteBudget(it) } }
        )
    }
}

@Composable
private fun SetBudgetDialog(
    existing: BudgetEntity?,
    defaultCurrency: SupportedCurrency,
    onDismiss: () -> Unit,
    onSave: (Category, Double, String) -> Unit,
    onRemove: () -> Unit
) {
    var category by remember { mutableStateOf(existing?.category ?: Category.FOOD_DINING) }
    var limitText by remember { mutableStateOf(existing?.monthlyLimit?.toString() ?: "") }
    var currency by remember { mutableStateOf(SupportedCurrency.values().firstOrNull { it.code == existing?.currencyCode } ?: defaultCurrency) }
    val limit = limitText.toDoubleOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(if (existing == null) "Set monthly budget" else "Edit budget", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                if (existing == null) {
                    Text("Category", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(budgetCategories) { c ->
                            ChoiceChip(prettyCategory(c), category == c) { category = c }
                        }
                    }
                } else {
                    Text(prettyCategory(existing.category), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly limit", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = planningFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Budget currency", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SupportedCurrency.values().toList()) { c -> ChoiceChip(c.code, currency == c) { currency = c } }
                }
                Text("Only transactions recorded in this currency count; no FX conversion is applied.", color = TextMuted, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { onSave(category, limit ?: 0.0, currency.code); onDismiss() },
                    enabled = limit != null && limit > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save budget", color = Color.Black, fontWeight = FontWeight.Bold) }
                if (existing != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onRemove(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Remove budget", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
