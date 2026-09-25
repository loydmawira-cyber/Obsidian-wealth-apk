package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.AccountEntity
import com.example.data.models.SupportedCurrency
import com.example.ui.components.FinCard
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun AccountManagementCard(viewModel: FinanceViewModel, modifier: Modifier = Modifier) {
    val accounts by viewModel.accounts.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var reconcileTarget by remember { mutableStateOf<AccountEntity?>(null) }
    var resolveTarget by remember { mutableStateOf<AccountEntity?>(null) }
    var openingBalanceTarget by remember { mutableStateOf<AccountEntity?>(null) }
    var showTransfer by remember { mutableStateOf(false) }

    FinCard(modifier = modifier, border = BorderStroke(1.dp, ObsidianBorder)) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("CASH ACCOUNTS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Enter balances yourself. Obsidian does not connect to or move money from your bank.", color = TextMuted, fontSize = 11.sp)
            if (accounts.isEmpty()) {
                Text("Add a cash, bank, wallet, or mobile-money account to start an account-level balance.", color = TextSecondary, fontSize = 12.sp)
            }
            accounts.forEach { account ->
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(account.accountType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, color = TextMuted, fontSize = 10.sp)
                        }
                        Column {
                            Text(
                                account.currencyCode?.let { viewModel.formatAmount(viewModel.currentAccountBalance(account), it) }
                                    ?: "Currency unknown",
                                color = if (account.currencyCode.isNullOrBlank() || (account.openingBalance != 0.0 && !account.openingBalanceConfirmed)) SovereignGold else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (account.currencyCode.isNullOrBlank() || (account.openingBalance != 0.0 && !account.openingBalanceConfirmed)) {
                                Text("Needs review", color = SovereignGold, fontSize = 9.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (account.currencyCode.isNullOrBlank()) {
                            Text("Set currency", color = SovereignGold, fontSize = 11.sp, modifier = Modifier.clickable { resolveTarget = account }.padding(vertical = 5.dp))
                        } else if (account.openingBalance != 0.0 && !account.openingBalanceConfirmed) {
                            Text("Review opening balance (${viewModel.formatAmount(account.openingBalance, account.currencyCode)})", color = SovereignGold, fontSize = 11.sp, modifier = Modifier.clickable { openingBalanceTarget = account }.padding(vertical = 5.dp))
                        } else {
                            Text("Reconcile statement", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.clickable { reconcileTarget = account }.padding(vertical = 5.dp))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) { Text("Add account", color = SovereignGold, fontSize = 11.sp) }
                OutlinedButton(onClick = { showTransfer = true }, enabled = accounts.count { it.isActive && !it.currencyCode.isNullOrBlank() } >= 2, modifier = Modifier.weight(1f)) { Text("Transfer", color = SovereignGold, fontSize = 11.sp) }
            }
        }
    }

    if (showAdd) AddAccountDialog(
        defaultCurrency = viewModel.userSettings.value.currency,
        onDismiss = { showAdd = false },
        onSave = { name, type, currency, balance -> viewModel.addAccount(name, type, currency, balance) }
    )
    reconcileTarget?.let { account -> ReconcileAccountDialog(
        account = account,
        currentBalance = viewModel.currentAccountBalance(account),
        formatAmount = { amount, code -> viewModel.formatAmount(amount, code) },
        onDismiss = { reconcileTarget = null },
        onSave = { balance -> viewModel.recordReconciliation(account, balance) }
    ) }
    resolveTarget?.let { account -> ResolveAccountCurrencyDialog(
        account = account,
        onDismiss = { resolveTarget = null },
        onSave = { code -> viewModel.resolveAccountCurrency(account, code) }
    ) }
    openingBalanceTarget?.let { account -> ConfirmOpeningBalanceDialog(
        account = account,
        onDismiss = { openingBalanceTarget = null },
        onSave = { amount -> viewModel.confirmAccountOpeningBalance(account, amount) }
    ) }
    if (showTransfer) TransferAccountsDialog(
        accounts = accounts.filter { it.isActive && !it.currencyCode.isNullOrBlank() },
        balance = { viewModel.currentAccountBalance(it) },
        formatAmount = { amount, code -> viewModel.formatAmount(amount, code) },
        onDismiss = { showTransfer = false },
        onTransfer = { from, to, amount, note -> viewModel.recordTransfer(from, to, amount, note) }
    )
}

@Composable
private fun AddAccountDialog(defaultCurrency: SupportedCurrency, onDismiss: () -> Unit, onSave: (String, String, String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("BANK") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var balanceText by remember { mutableStateOf("0") }
    val balance = balanceText.toDoubleOrNull()
    SimpleAccountDialog(onDismiss) {
        Text("Add account", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Opening balance is the amount in the account today. Enter it in the account's own currency; negative balances are allowed for overdrafts.", color = TextSecondary, fontSize = 11.sp)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account name") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
        Text("Account type", color = TextSecondary, fontSize = 11.sp)
        ChipChoices(listOf("BANK", "MOBILE MONEY", "CASH", "SAVINGS", "OTHER"), type.replace('_', ' ')) { type = it.replace(' ', '_') }
        CurrencyChoices(currency) { currency = it }
        OutlinedTextField(value = balanceText, onValueChange = { balanceText = it }, label = { Text("Opening balance (${currency.code})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(name, type, currency.code, balance ?: 0.0); onDismiss() }, enabled = name.isNotBlank() && balance != null && balance.isFinite(), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth()) { Text("Save account", color = Color.Black, fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun ResolveAccountCurrencyDialog(account: AccountEntity, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var currency by remember { mutableStateOf(SupportedCurrency.KES) }
    SimpleAccountDialog(onDismiss) {
        Text("Confirm account currency", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Legacy records for “${account.name}” have no stored currency. Choose the currency shown on the account's statements; this will update their display and totals, not convert amounts.", color = TextSecondary, fontSize = 12.sp)
        CurrencyChoices(currency) { currency = it }
        Button(onClick = { onSave(currency.code); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth()) { Text("Confirm ${currency.code}", color = Color.Black, fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun ConfirmOpeningBalanceDialog(account: AccountEntity, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember(account.id) { mutableStateOf(account.openingBalance.toString()) }
    val amount = text.toDoubleOrNull()
    SimpleAccountDialog(onDismiss) {
        Text("Review legacy opening balance", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("This saved starting amount is excluded from totals until you confirm it. Enter the opening amount to use for ${account.name} in ${account.currencyCode}. Confirming does not convert the amount or change later ledger entries.", color = TextSecondary, fontSize = 12.sp)
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Opening balance (${account.currencyCode})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(amount ?: 0.0); onDismiss() }, enabled = amount != null && amount.isFinite(), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth()) { Text("Confirm opening balance", color = Color.Black, fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun ReconcileAccountDialog(account: AccountEntity, currentBalance: Double, formatAmount: (Double, String?) -> String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    val statement = text.toDoubleOrNull()
    val currency = account.currencyCode
    SimpleAccountDialog(onDismiss) {
        Text("Reconcile ${account.name}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Ledger balance: ${formatAmount(currentBalance, currency)}. Enter the current statement balance; any difference is recorded as an adjustment, not income or spending.", color = TextSecondary, fontSize = 12.sp)
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Statement balance (${currency ?: "currency unknown"})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(statement ?: 0.0); onDismiss() }, enabled = statement != null && statement.isFinite(), colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth()) { Text("Reconcile account", color = Color.Black, fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun TransferAccountsDialog(accounts: List<AccountEntity>, balance: (AccountEntity) -> Double, formatAmount: (Double, String?) -> String, onDismiss: () -> Unit, onTransfer: (AccountEntity, AccountEntity, Double, String) -> Unit) {
    var from by remember(accounts) { mutableStateOf(accounts.firstOrNull()) }
    var to by remember(accounts) { mutableStateOf(accounts.getOrNull(1)) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()
    val available = from?.let { balance(it).coerceAtLeast(0.0) } ?: 0.0
    val insufficientBalance = amount != null && amount > available + 0.000001
    val eligible = from != null && to != null && from!!.id != to!!.id && from!!.currencyCode == to!!.currencyCode && amount != null && amount > 0.0 && !insufficientBalance
    SimpleAccountDialog(onDismiss) {
        Text("Transfer between your accounts", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Both entries are recorded together. Transfers are excluded from income, spending, and savings totals.", color = TextSecondary, fontSize = 11.sp)
        Text("From", color = TextSecondary, fontSize = 11.sp)
        AccountChoices(accounts, from) { from = it }
        Text("To", color = TextSecondary, fontSize = 11.sp)
        AccountChoices(accounts, to) { to = it }
        if (from != null && to != null && from!!.currencyCode != to!!.currencyCode) Text("Transfers require matching currencies; exchange conversion is not supported.", color = SovereignGold, fontSize = 11.sp)
        OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (${from?.currencyCode ?: "currency unknown"})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
        if (from != null) Text("Available: ${formatAmount(available, from!!.currencyCode)}", color = TextMuted, fontSize = 10.sp)
        if (insufficientBalance) Text("Not enough balance in the selected account.", color = Color(0xFFFB7185), fontSize = 11.sp)
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
        Button(onClick = { onTransfer(from!!, to!!, amount!!, note); onDismiss() }, enabled = eligible, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth), modifier = Modifier.fillMaxWidth()) { Text("Record transfer", color = Color.Black, fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun AccountChoices(accounts: List<AccountEntity>, selected: AccountEntity?, onSelect: (AccountEntity) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        accounts.forEach { account ->
            FilterChip(selected = account.id == selected?.id, onClick = { onSelect(account) }, label = { Text("${account.name} · ${account.currencyCode}", fontSize = 10.sp) })
        }
    }
}

@Composable
private fun CurrencyChoices(selected: SupportedCurrency, onSelect: (SupportedCurrency) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SupportedCurrency.values().forEach { currency ->
            FilterChip(selected = currency == selected, onClick = { onSelect(currency) }, label = { Text(currency.code, fontSize = 10.sp) })
        }
    }
}

@Composable
private fun ChipChoices(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item -> FilterChip(selected = item == selected, onClick = { onSelect(item) }, label = { Text(item, fontSize = 9.sp) }) }
    }
}

@Composable
private fun SimpleAccountDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = ObsidianSurface, border = BorderStroke(1.dp, ObsidianBorder)) {
            Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = EmeraldGrowth,
    unfocusedBorderColor = ObsidianBorder
)
