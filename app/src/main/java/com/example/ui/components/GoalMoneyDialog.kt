package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.AccountEntity
import com.example.ui.theme.CrimsonDebt
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** Adds a goal contribution from a selected account, or lowers the tracking amount on withdrawal. */
@Composable
fun GoalMoneyDialog(
    goalTitle: String,
    goalCurrencyCode: String?,
    isDeposit: Boolean,
    maxAmount: Double?,
    accounts: List<AccountEntity>,
    availableBalance: (AccountEntity) -> Double,
    formatAmount: (Double, String?) -> String,
    onDismiss: () -> Unit,
    onConfirm: (Double, AccountEntity?) -> Unit
) {
    val eligibleAccounts = remember(accounts, goalCurrencyCode) {
        accounts.filter { it.isActive && !it.currencyCode.isNullOrBlank() && it.currencyCode == goalCurrencyCode }
    }
    var text by remember { mutableStateOf("") }
    var selectedAccount by remember(eligibleAccounts) { mutableStateOf(eligibleAccounts.firstOrNull()) }
    val amount = text.toDoubleOrNull()
    val balance = selectedAccount?.let(availableBalance) ?: 0.0
    val insufficientBalance = isDeposit && amount != null && amount > balance + 0.000001
    val valid = amount != null && amount.isFinite() && amount > 0.0 &&
        (maxAmount == null || amount <= maxAmount + 1e-9) &&
        (!isDeposit || (selectedAccount != null && !insufficientBalance))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (isDeposit) "Add to $goalTitle" else "Withdraw from $goalTitle",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isDeposit) "Choose the account to withdraw from. The deposit updates goal progress and records the cash debit."
                    else "This reduces the tracked goal balance only; it does not move cash. Available in the goal: ${formatAmount(maxAmount ?: 0.0, goalCurrencyCode)}.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                if (isDeposit) {
                    Spacer(Modifier.height(12.dp))
                    Text("Withdraw from account · ${goalCurrencyCode ?: "currency unresolved"}", color = TextSecondary, fontSize = 11.sp)
                    if (eligibleAccounts.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No active account in ${goalCurrencyCode ?: "the goal currency"}. Add or resolve an account in that currency before depositing.",
                            color = SovereignGold,
                            fontSize = 11.sp
                        )
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            eligibleAccounts.forEach { account ->
                                val accountBalance = availableBalance(account).coerceAtLeast(0.0)
                                FilterChip(
                                    selected = account.id == selectedAccount?.id,
                                    onClick = { selectedAccount = account },
                                    label = {
                                        Text(
                                            "${account.name} · ${formatAmount(accountBalance, account.currencyCode)}",
                                            fontSize = 10.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SovereignGold,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                        selectedAccount?.let { account ->
                            Text(
                                "Available balance: ${formatAmount(balance.coerceAtLeast(0.0), account.currencyCode)}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        if (insufficientBalance) {
                            Text("Not enough balance in the selected account.", color = CrimsonDebt, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Amount", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGrowth,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onConfirm(amount ?: 0.0, if (isDeposit) selectedAccount else null); onDismiss() },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isDeposit) "Deposit from account" else "Withdraw", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
