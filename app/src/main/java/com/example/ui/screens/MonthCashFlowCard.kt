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
            if (kotlin.math.abs(month.adjustments) > 0.005) {
                BalanceLine("± Reconciliation adjustments", viewModel.formatAmount(month.adjustments), SovereignGold)
            }
            if (month.invested > 0) {
                BalanceLine("   of which invested & saved", viewModel.formatAmount(month.invested), TextMuted, small = true)
            }
            Spacer(modifier = Modifier.height(4.dp))
            BalanceLine(
                if (month.isCurrentMonth) "Balance now" else "Closing balance",
                viewModel.formatAmount(month.closing),
                if (month.closing >= 0) EmeraldLight else CrimsonDebt,
                bold = true
            )

            Text(
                "Balances include only active accounts in ${viewModel.userSettings.value.currency.code}. Add and reconcile each account in the Cash Flow account panel below.",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
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
