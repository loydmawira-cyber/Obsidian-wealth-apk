package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.Category
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shown when a ledger entry is long-pressed: choose to edit or delete it. */
@Composable
fun TransactionActionDialog(
    transaction: TransactionEntity,
    formatAmount: (Double) -> String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(transaction.title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${if (transaction.type == TransactionType.INCOME) "+" else "-"}${formatAmount(transaction.amount)}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))
                if (transaction.transactionKind == com.example.data.models.TransactionKind.STANDARD && transaction.sourceReference == null) {
                    Button(
                        onClick = { onEdit(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Edit entry", color = Color.Black, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onDelete(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete entry", color = Color.White, fontWeight = FontWeight.Bold) }
                } else {
                    Text("This entry is linked to another account, debt, or generated record. Edit it through the related action to keep balances reconciled.", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun EditChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = EmeraldGrowth,
    unfocusedBorderColor = ObsidianBorder
)

/** Edits an existing ledger entry in place. */
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    accounts: List<com.example.data.models.AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false } }
    val originalDate = remember(transaction.dateMillis) { dateFormat.format(Date(transaction.dateMillis)) }

    var title by remember { mutableStateOf(transaction.title) }
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var type by remember { mutableStateOf(transaction.type) }
    var category by remember { mutableStateOf(transaction.category) }
    var account by remember(accounts, transaction.accountId) {
        mutableStateOf(accounts.firstOrNull { it.id == transaction.accountId } ?: accounts.firstOrNull { it.name.equals(transaction.account, true) })
    }
    var note by remember { mutableStateOf(transaction.note) }
    var dateText by remember { mutableStateOf(originalDate) }

    val amount = amountText.toDoubleOrNull()
    val parsedDate = if (dateText == originalDate) transaction.dateMillis
    else try { dateFormat.parse(dateText)?.time?.plus(12L * 3_600_000L) } catch (e: Exception) { null }
    val valid = title.isNotBlank() && amount != null && amount.isFinite() && amount > 0.0 && parsedDate != null && account != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Edit entry", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (transaction.sourceReference != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This entry was created by a holding, SIP or goal. Changing it here does not change that item.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditChip("Expense", type == TransactionType.EXPENSE) { type = TransactionType.EXPENSE }
                    EditChip("Income", type == TransactionType.INCOME) { type = TransactionType.INCOME }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = editFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description / Merchant", color = TextSecondary) },
                    colors = editFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Category", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Category.values().filter { it !in setOf(Category.ACCOUNT_TRANSFER, Category.ACCOUNT_ADJUSTMENT, Category.INVESTMENT_SALE) }) { c ->
                        EditChip(c.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, category == c) { category = c }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Account and currency", color = TextSecondary, fontSize = 11.sp)
                Text("Selecting an account applies its currency to this amount; the numeric value is not converted.", color = TextMuted, fontSize = 10.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts.filter { it.isActive && !it.currencyCode.isNullOrBlank() }) { candidate ->
                        EditChip("${candidate.name} · ${candidate.currencyCode}", account?.id == candidate.id) { account = candidate }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (yyyy-MM-dd)", color = TextSecondary) },
                    isError = parsedDate == null,
                    colors = editFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)", color = TextSecondary) },
                    colors = editFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val selectedAccount = account ?: return@Button
                        onSave(
                            transaction.copy(
                                title = title.trim(),
                                amount = amount ?: transaction.amount,
                                type = type,
                                category = category,
                                account = selectedAccount.name,
                                accountId = selectedAccount.id,
                                currencyCode = selectedAccount.currencyCode,
                                note = note,
                                dateMillis = parsedDate ?: transaction.dateMillis
                            )
                        )
                        onDismiss()
                    },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save changes", color = Color.Black, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
