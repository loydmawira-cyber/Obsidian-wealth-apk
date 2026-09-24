package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.FinCard
import com.example.ui.theme.CrimsonDebt
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FinanceViewModel

/**
 * Month switcher with opening and closing balances. The closing balance of one month is the
 * opening balance of the next; it is always worked out from the ledger, never stored.
 */
@Composable
fun MonthCashFlowCard(viewModel: FinanceViewModel) {
    val month by viewModel.monthCashFlow.collectAsState()
    val promptDone by viewModel.startingBalancePromptDone.collectAsState()
    var showBalanceDialog by remember { mutableStateOf(false) }

    FinCard(border = BorderStroke(1.dp, GoldBorder)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.shiftMonth(-1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month", tint = TextPrimary)
                }
                Text(month.label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { viewModel.shiftMonth(1) },
                    enabled = !month.isCurrentMonth,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = if (month.isCurrentMonth) TextMuted else TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            BalanceLine("Opening balance", viewModel.formatAmount(month.opening), TextPrimary, bold = true)
            BalanceLine("+ Inflow", viewModel.formatAmount(month.inflow), EmeraldLight)
            BalanceLine("\u2212 Outflow", viewModel.formatAmount(month.outflow), Color(0xFFFB7185))
            if (month.invested > 0) {
                BalanceLine("   of which invested", viewModel.formatAmount(month.invested), TextMuted, small = true)
            }
            Spacer(modifier = Modifier.height(4.dp))
            BalanceLine(
                if (month.isCurrentMonth) "Balance now" else "Closing balance",
                viewModel.formatAmount(month.closing),
                if (month.closing >= 0) EmeraldLight else CrimsonDebt,
                bold = true
            )

            if (!promptDone) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SovereignGold.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Start from your real balance?", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Enter the cash you have right now so your balances match reality, or start from zero.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showBalanceDialog = true }) { Text("Set balance", color = SovereignGold) }
                            TextButton(onClick = { viewModel.dismissStartingBalancePrompt() }) { Text("Start from 0", color = TextSecondary) }
                        }
                    }
                }
            } else {
                Text(
                    text = "Change current balance",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { showBalanceDialog = true }
                )
            }
        }
    }

    if (showBalanceDialog) {
        SetBalanceDialog(
            onDismiss = { showBalanceDialog = false },
            onSave = { viewModel.setCurrentCashBalance(it) }
        )
    }
}

@Composable
private fun BalanceLine(label: String, value: String, valueColor: Color, bold: Boolean = false, small: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = if (bold) TextPrimary else TextSecondary, fontSize = if (small) 11.sp else 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            value,
            color = valueColor,
            fontSize = if (small) 11.sp else 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SetBalanceDialog(onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    val value = text.toDoubleOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Your current cash balance", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Add up the cash you have right now: bank accounts, wallet and mobile money. " +
                        "Balances for earlier months are worked back from this figure.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Current balance", color = TextSecondary) },
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
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { onSave(value ?: 0.0); onDismiss() },
                    enabled = value != null,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save balance", color = Color.Black, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
