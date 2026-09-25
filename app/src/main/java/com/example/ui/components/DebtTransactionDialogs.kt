package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.*

private val debtCategories = listOf(Category.FOOD_DINING, Category.HOUSING, Category.UTILITIES, Category.TRANSPORT, Category.SHOPPING, Category.HEALTHCARE, Category.SUBSCRIPTIONS, Category.ENTERTAINMENT, Category.OTHER)

@Composable
fun CreditCardPurchaseDialog(card: CreditCardEntity, formatAmount: (Double) -> String, onDismiss: () -> Unit, onConfirm: (String, Double, Category, String) -> Unit) {
    var merchant by remember(card.id) { mutableStateOf("") }
    var amountText by remember(card.id) { mutableStateOf("") }
    var category by remember(card.id) { mutableStateOf(Category.FOOD_DINING) }
    var note by remember(card.id) { mutableStateOf("") }
    val available = (card.creditLimit - card.currentBalance).coerceAtLeast(0.0)
    val amount = amountText.toDoubleOrNull()
    val valid = merchant.isNotBlank() && amount != null && amount.isFinite() && amount > 0 && amount <= available && !card.currencyCode.isNullOrBlank()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
                Text("Confirm card purchase", style = MaterialTheme.typography.titleLarge)
                Text("${card.cardName} · available ${formatAmount(available)}")
                Text("Adds spending and card balance; does not reduce a cash account.", fontSize = 12.sp)
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant / description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amountText, { amountText = it }, label = { Text("Amount (${card.currencyCode ?: "currency not set"})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), isError = amount != null && amount > available)
                if (amount != null && amount > available) Text("Not enough available credit on this card.", color = Color.Red)
                Row(Modifier.horizontalScroll(rememberScrollState())) { debtCategories.forEach { c -> FilterChip(category == c, { category = c }, label = { Text(c.name.replace('_', ' '), fontSize = 10.sp) }) } }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onConfirm(merchant.trim(), amount ?: 0.0, category, note.trim()); onDismiss() }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Confirm & record purchase") }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun LoanTopUpDialog(loan: LoanEntity, accounts: List<AccountEntity>, formatAmount: (Double) -> String, onDismiss: () -> Unit, onConfirm: (Double, AccountEntity, String) -> Unit) {
    var amountText by remember(loan.id) { mutableStateOf("") }
    val eligible = accounts.filter { it.isActive && it.currencyCode == loan.currencyCode }
    var account by remember(loan.id, accounts) { mutableStateOf(eligible.firstOrNull()) }
    var note by remember(loan.id) { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()
    val valid = amount != null && amount.isFinite() && amount > 0 && account != null && eligible.any { it.id == account?.id }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
                Text("Confirm loan top-up", style = MaterialTheme.typography.titleLarge)
                Text("${loan.loanName} · outstanding ${formatAmount(loan.remainingBalance)}")
                Text("Increases the loan balance and adds proceeds to your selected account. No lender transfer is initiated; borrowing is not income.", fontSize = 12.sp)
                OutlinedTextField(amountText, { amountText = it }, label = { Text("Amount (${loan.currencyCode ?: "currency not set"})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Text("Same-currency deposit account")
                if (eligible.isEmpty()) Text("Add an active account in ${loan.currencyCode ?: "the loan currency"} first.")
                eligible.forEach { candidate ->
                    Row(Modifier.fillMaxWidth().clickable { account = candidate }.padding(10.dp)) { RadioButton(account?.id == candidate.id, onClick = { account = candidate }); Text("${candidate.name} · ${candidate.currencyCode}") }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { val selected = account ?: return@Button; onConfirm(amount ?: 0.0, selected, note.trim()); onDismiss() }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Confirm & record top-up") }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
