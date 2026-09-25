package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.TransactionEntity
import com.example.data.models.AccountEntity
import com.example.data.models.TransactionType
import com.example.ui.theme.CrimsonDebt
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImportedTransactionsDialog(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    formatAmount: (Double, String?) -> String,
    availableBalance: (AccountEntity, Long) -> Double,
    onConfirm: (TransactionEntity, AccountEntity) -> Unit,
    onIgnore: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                if (transactions.isEmpty()) "Notifications" else "Pending transaction approvals (${transactions.size})",
                color = GoldBright,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (transactions.isEmpty()) {
                Text("No transactions are waiting for review.", color = TextSecondary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Confirm only transactions that actually happened. Unconfirmed records are excluded from your totals.", color = TextMuted, fontSize = 12.sp)
                    }
                    items(transactions, key = { it.id }) { transaction ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(transaction.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            var selectedAccount by remember(accounts, transaction.id) {
                                mutableStateOf(accounts.firstOrNull { it.isActive && it.id == transaction.accountId && !it.currencyCode.isNullOrBlank() }
                                    ?: accounts.firstOrNull { it.isActive && it.name.equals(transaction.account, true) && !it.currencyCode.isNullOrBlank() })
                            }
                            val available = selectedAccount?.let { availableBalance(it, transaction.dateMillis) }?.coerceAtLeast(0.0) ?: 0.0
                            val insufficientBalance = transaction.type == TransactionType.EXPENSE && transaction.amount > available + 0.000001
                            Text(
                                "${transaction.type.name} · ${formatAmount(transaction.amount, transaction.currencyCode ?: selectedAccount?.currencyCode)} · ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(transaction.dateMillis))}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text("Original source: ${transaction.account}", color = TextMuted, fontSize = 10.sp)
                            Text("Choose account and currency", color = TextSecondary, fontSize = 10.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(accounts.filter { it.isActive && !it.currencyCode.isNullOrBlank() }) { candidate ->
                                    FilterChip(selected = candidate.id == selectedAccount?.id, onClick = { selectedAccount = candidate }, label = { Text("${candidate.name} · ${candidate.currencyCode}", fontSize = 9.sp) })
                                }
                            }
                            if (transaction.type == TransactionType.EXPENSE && selectedAccount != null) {
                                Text("Available: ${formatAmount(available, selectedAccount?.currencyCode)}", color = TextMuted, fontSize = 10.sp)
                            }
                            if (insufficientBalance) Text("Not enough balance in the selected account. This confirmation stays pending.", color = CrimsonDebt, fontSize = 11.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { selectedAccount?.let { onConfirm(transaction, it) } },
                                    enabled = selectedAccount != null && !insufficientBalance && (transaction.currencyCode == null || transaction.currencyCode == selectedAccount?.currencyCode),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Confirm", fontSize = 11.sp) }
                                Button(
                                    onClick = { onIgnore(transaction) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDebt),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Ignore", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}
