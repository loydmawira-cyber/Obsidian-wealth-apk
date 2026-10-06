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
import com.example.data.models.GoalEntity
import com.example.ui.theme.CrimsonDebt
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val RESERVE_TYPE = "GOALS_RESERVE"

/**
 * Deposit: moves cash from a source account into the account that holds the goal's money
 * (Goals Reserve by default; chosen on the first deposit, then fixed).
 * Withdraw: moves the held part back into an account you pick.
 */
@Composable
fun GoalMoneyDialog(
    goal: GoalEntity,
    isDeposit: Boolean,
    accounts: List<AccountEntity>,
    heldInAccount: AccountEntity?,
    freeBalance: (AccountEntity) -> Double,
    formatAmount: (Double, String?) -> String,
    onDismiss: () -> Unit,
    // amount, source (deposit) or destination (withdraw), holding choice (first deposit only; null = Goals Reserve)
    onConfirm: (Double, AccountEntity?, AccountEntity?) -> Unit
) {
    val goalCurrencyCode = goal.currencyCode
    val sameCurrency = remember(accounts, goalCurrencyCode) {
        accounts.filter { it.isActive && !it.currencyCode.isNullOrBlank() && it.currencyCode == goalCurrencyCode }
    }
    val nonReserve = remember(sameCurrency) { sameCurrency.filter { it.accountType != RESERVE_TYPE } }

    var text by remember { mutableStateOf("") }
    // First deposit only: where the goal's money will live. null = Goals Reserve.
    var holdingChoice by remember(nonReserve) { mutableStateOf<AccountEntity?>(null) }
    val holding: AccountEntity? = heldInAccount ?: holdingChoice
    val holdingLabel = holding?.displayName() ?: "🎯 Goals Reserve"

    // Deposit sources are real accounts only. Withdraw destinations also include the holding account ("stay").
    var selectedAccount by remember(nonReserve) { mutableStateOf(nonReserve.firstOrNull { it.id != holding?.id } ?: nonReserve.firstOrNull()) }

    val amount = text.toDoubleOrNull()
    val free = selectedAccount?.let(freeBalance) ?: 0.0
    val insufficient = isDeposit && amount != null && amount > free + 0.000001
    val maxAmount = if (isDeposit) null else goal.currentAmount
    val valid = amount != null && amount.isFinite() && amount > 0.0 &&
        (maxAmount == null || amount <= maxAmount + 1e-9) &&
        (!isDeposit || (selectedAccount != null && !insufficient))
    val trackingOnly = (goal.currentAmount - goal.heldAmount).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (isDeposit) "Add to ${goal.title}" else "Withdraw from ${goal.title}",
                    color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isDeposit) "Money moves from your account into the account holding this goal. It's a transfer, not spending."
                    else "The money held for this goal moves back to the account you choose. Available in the goal: ${formatAmount(goal.currentAmount, goalCurrencyCode)}.",
                    color = TextSecondary, fontSize = 12.sp
                )

                if (isDeposit) {
                    Spacer(Modifier.height(12.dp))
                    if (heldInAccount != null) {
                        Text("Held in · $holdingLabel", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        Text("Hold this goal's money in", color = TextSecondary, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = holdingChoice == null,
                                onClick = { holdingChoice = null },
                                label = { Text("🎯 Goals Reserve", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SovereignGold, selectedLabelColor = Color.Black)
                            )
                            nonReserve.forEach { account ->
                                FilterChip(
                                    selected = holdingChoice?.id == account.id,
                                    onClick = { holdingChoice = account },
                                    label = { Text(account.displayName(), fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SovereignGold, selectedLabelColor = Color.Black)
                                )
                            }
                        }
                        Text("Chosen once with the first deposit.", color = TextMuted, fontSize = 10.sp)
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Take it from · ${goalCurrencyCode ?: "currency unresolved"}", color = TextSecondary, fontSize = 11.sp)
                    if (nonReserve.isEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No active account in ${goalCurrencyCode ?: "the goal currency"}. Add or resolve an account in that currency before depositing.",
                            color = SovereignGold, fontSize = 11.sp
                        )
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            nonReserve.forEach { account ->
                                FilterChip(
                                    selected = account.id == selectedAccount?.id,
                                    onClick = { selectedAccount = account },
                                    label = { Text("${account.displayName()} · ${formatAmount(freeBalance(account).coerceAtLeast(0.0), account.currencyCode)}", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SovereignGold, selectedLabelColor = Color.Black)
                                )
                            }
                        }
                        selectedAccount?.let { account ->
                            Text("Free to use: ${formatAmount(free.coerceAtLeast(0.0), account.currencyCode)}", color = TextMuted, fontSize = 10.sp)
                            Text(
                                if (account.id == holding?.id) "This account already holds the goal, so the money stays put and is set aside."
                                else "${formatAmount(amount ?: 0.0, account.currencyCode)} moves from ${account.displayName()} to $holdingLabel.",
                                color = TextMuted, fontSize = 10.sp
                            )
                        }
                        if (insufficient) {
                            Text("Not enough free balance in the selected account.", color = CrimsonDebt, fontSize = 11.sp)
                        }
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    if (heldInAccount != null && goal.heldAmount > 0.0) {
                        Text("Move it to", color = TextSecondary, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val others = nonReserve.filter { it.id != heldInAccount.id }
                            others.forEach { account ->
                                FilterChip(
                                    selected = account.id == selectedAccount?.id,
                                    onClick = { selectedAccount = account },
                                    label = { Text(account.displayName(), fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SovereignGold, selectedLabelColor = Color.Black)
                                )
                            }
                            FilterChip(
                                selected = selectedAccount?.id == heldInAccount.id || (selectedAccount == null),
                                onClick = { selectedAccount = heldInAccount },
                                label = { Text("Stay in ${heldInAccount.displayName()}", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SovereignGold, selectedLabelColor = Color.Black)
                            )
                        }
                        Text(
                            "${formatAmount(goal.heldAmount, goalCurrencyCode)} of this goal is held in ${heldInAccount.displayName()}.",
                            color = TextMuted, fontSize = 10.sp
                        )
                    }
                    if (trackingOnly > 0.005) {
                        Text(
                            "${formatAmount(trackingOnly, goalCurrencyCode)} is tracking-only (not in any account); withdrawing that part just lowers the goal.",
                            color = SovereignGold, fontSize = 10.sp
                        )
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
                    onClick = {
                        onConfirm(
                            amount ?: 0.0,
                            if (isDeposit) selectedAccount else selectedAccount?.takeIf { heldInAccount != null && goal.heldAmount > 0.0 },
                            if (isDeposit && heldInAccount == null) holdingChoice else null
                        )
                        onDismiss()
                    },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isDeposit) "Deposit" else "Withdraw", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
