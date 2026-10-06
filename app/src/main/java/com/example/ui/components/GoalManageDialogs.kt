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

private const val RESERVE_ACCOUNT_TYPE = "GOALS_RESERVE"

@Composable
private fun GoalDialogSurface(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) { content() }
        }
    }
}

@Composable
private fun accountChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = SovereignGold,
    selectedLabelColor = Color.Black
)

/** Long-press menu for a goal. */
@Composable
fun GoalActionsDialog(
    goal: GoalEntity,
    canMove: Boolean,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    GoalDialogSurface(onDismiss) {
        Text(goal.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onDismiss(); onEdit() },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Edit goal", color = Color.Black, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onDismiss(); onMove() },
            enabled = canMove,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Change holding account", color = SovereignGold) }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onDismiss(); onDelete() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Delete goal", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

/** Edits name, category, target, monthly plan and target year. Saved money changes only via deposits/withdrawals. */
@Composable
fun EditGoalDialog(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, target: Double, monthly: Double, targetYear: Int) -> Unit
) {
    var title by remember(goal.id) { mutableStateOf(goal.title) }
    var category by remember(goal.id) { mutableStateOf(goal.category) }
    var target by remember(goal.id) { mutableStateOf(goal.targetAmount.toBigDecimal().stripTrailingZeros().toPlainString()) }
    var monthly by remember(goal.id) { mutableStateOf(goal.monthlyContribution.toBigDecimal().stripTrailingZeros().toPlainString()) }
    var year by remember(goal.id) { mutableStateOf(goal.targetYear.toString()) }
    val t = target.toDoubleOrNull()
    val m = monthly.toDoubleOrNull()
    val y = year.toIntOrNull()
    val valid = title.isNotBlank() && category.isNotBlank() && t != null && t.isFinite() && t > 0.0 &&
        m != null && m.isFinite() && m >= 0.0 && y != null && y in 2000..2200

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedBorderColor = EmeraldGrowth,
        unfocusedBorderColor = ObsidianBorder
    )
    GoalDialogSurface(onDismiss) {
        Text("Edit goal", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(title, { title = it }, label = { Text("Goal name", color = TextSecondary) }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(category, { category = it }, label = { Text("Category", color = TextSecondary) }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(target, { target = it }, label = { Text("Target amount", color = TextSecondary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(monthly, { monthly = it }, label = { Text("Monthly contribution", color = TextSecondary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(year, { year = it }, label = { Text("Target year", color = TextSecondary) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Saved money changes only through Deposit and Withdraw.", color = TextMuted, fontSize = 10.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSave(title, category, t ?: 0.0, m ?: 0.0, y ?: goal.targetYear); onDismiss() },
            enabled = valid,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save changes", color = Color.Black, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

/** Picks a new account to hold the goal's money; the held amount is transferred there. */
@Composable
fun MoveGoalHoldingDialog(
    goal: GoalEntity,
    accounts: List<AccountEntity>,
    heldInAccount: AccountEntity?,
    formatAmount: (Double, String?) -> String,
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity) -> Unit
) {
    val candidates = remember(accounts, goal.currencyCode, heldInAccount?.id) {
        accounts.filter {
            it.isActive && !it.currencyCode.isNullOrBlank() && it.currencyCode == goal.currencyCode &&
                it.accountType != RESERVE_ACCOUNT_TYPE && it.id != heldInAccount?.id
        }
    }
    var selected by remember(candidates) { mutableStateOf(candidates.firstOrNull()) }
    GoalDialogSurface(onDismiss) {
        Text("Change holding account", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (heldInAccount != null) "${goal.title} is held in ${heldInAccount.displayName()}."
            else "${goal.title} isn't held in an account yet.",
            color = TextSecondary, fontSize = 12.sp
        )
        Text(
            "Pick the real bank or mobile-money account where this money is.",
            color = TextMuted, fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))
        if (candidates.isEmpty()) {
            Text("No other active account in ${goal.currencyCode ?: "the goal currency"}.", color = SovereignGold, fontSize = 11.sp)
        } else {
            Text("New holding account", color = TextSecondary, fontSize = 11.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                candidates.forEach { account ->
                    FilterChip(
                        selected = account.id == selected?.id,
                        onClick = { selected = account },
                        label = { Text(account.displayName(), fontSize = 10.sp) },
                        colors = accountChipColors()
                    )
                }
            }
            selected?.let { account ->
                Spacer(Modifier.height(4.dp))
                Text(
                    if (heldInAccount != null && goal.heldAmount > 0.0)
                        "${formatAmount(goal.heldAmount, goal.currencyCode)} moves from ${heldInAccount.displayName()} to ${account.displayName()}. It is set aside for the goal there, so it isn't free to spend."
                    else "Future deposits will go to ${account.displayName()}.",
                    color = TextMuted, fontSize = 10.sp
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { selected?.let { onConfirm(it) }; onDismiss() },
            enabled = selected != null,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Move", color = Color.Black, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

/**
 * Confirmation for deleting a goal. Money held for the goal (and older deposits that never landed in an
 * account) goes back to the chosen account. A null choice means "stay where it is" (the holding account,
 * or Goals Reserve if the goal has no holding account yet).
 */
@Composable
fun DeleteGoalDialog(
    goal: GoalEntity,
    accounts: List<AccountEntity>,
    heldInAccount: AccountEntity?,
    legacyTotal: Double,
    formatAmount: (Double, String?) -> String,
    onDismiss: () -> Unit,
    onConfirm: (AccountEntity?) -> Unit
) {
    val others = remember(accounts, goal.currencyCode, heldInAccount?.id) {
        accounts.filter {
            it.isActive && !it.currencyCode.isNullOrBlank() && it.currencyCode == goal.currencyCode &&
                it.accountType != RESERVE_ACCOUNT_TYPE && it.id != heldInAccount?.id
        }
    }
    var destination by remember(others) { mutableStateOf<AccountEntity?>(others.firstOrNull()) }
    val stayLabel = heldInAccount?.displayName()?.let { "Stay in $it" } ?: "🎯 Goals Reserve"
    val destName = destination?.displayName() ?: heldInAccount?.displayName() ?: "🎯 Goals Reserve"
    val held = goal.heldAmount.coerceAtLeast(0.0)
    val trackingOnly = (goal.currentAmount - held).coerceAtLeast(0.0)
    val hasMoney = held > 0.005 || legacyTotal > 0.005

    GoalDialogSurface(onDismiss) {
        Text("Delete ${goal.title}?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("This removes the goal. Your money is not deleted; it goes back to an account.", color = TextSecondary, fontSize = 12.sp)
        if (hasMoney) {
            Spacer(Modifier.height(12.dp))
            Text("Send the money to", color = TextSecondary, fontSize = 11.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                others.forEach { account ->
                    FilterChip(
                        selected = destination?.id == account.id,
                        onClick = { destination = account },
                        label = { Text(account.displayName(), fontSize = 10.sp) },
                        colors = accountChipColors()
                    )
                }
                FilterChip(
                    selected = destination == null,
                    onClick = { destination = null },
                    label = { Text(stayLabel, fontSize = 10.sp) },
                    colors = accountChipColors()
                )
            }
            Spacer(Modifier.height(4.dp))
            if (held > 0.005) {
                Text(
                    if (destination == null || destination?.id == heldInAccount?.id)
                        "${formatAmount(held, goal.currencyCode)} stays in $destName and becomes free to use."
                    else "${formatAmount(held, goal.currencyCode)} moves from ${heldInAccount?.name ?: "its holding account"} to $destName.",
                    color = TextMuted, fontSize = 10.sp
                )
            }
            if (legacyTotal > 0.005) {
                Text(
                    "${formatAmount(legacyTotal, goal.currencyCode)} from earlier deposits is returned to $destName.",
                    color = TextMuted, fontSize = 10.sp
                )
            }
        }
        if (trackingOnly > 0.005) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatAmount(trackingOnly, goal.currencyCode)} was tracking-only (never in an account) and will just disappear with the goal.",
                color = SovereignGold, fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onConfirm(destination); onDismiss() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Delete goal", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Keep goal") }
    }
}
