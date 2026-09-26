package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.billing.PremiumOfferInfo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PremiumAccessGate(
    featureName: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = GoldBright)
            Spacer(Modifier.height(10.dp))
            Text("Premium required for $featureName", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("Unlock $featureName and every other feature with Premium.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                shape = RoundedCornerShape(12.dp)
            ) { Text("View Premium and trial", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun PremiumPaywallDialog(
    isPremium: Boolean,
    offer: PremiumOfferInfo?,
    billingMessage: String?,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("OBSIDIAN PREMIUM", color = GoldBright, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Everything for your financial life", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Premium includes Budgets, Goals, and every other feature in the app.", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = offer?.summary ?: "Loading current Google Play subscription offer…",
                    color = if (offer?.hasThreeMonthFreeTrial == true) EmeraldGrowth else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Trial eligibility, renewal price, billing date, and cancellation are confirmed by Google Play before you subscribe. A trial, if offered, renews at the displayed price unless canceled before renewal.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                if (!billingMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(billingMessage, color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onSubscribe,
                    enabled = !isPremium && offer != null,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGrowth),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            isPremium -> "Premium is active"
                            offer?.hasThreeMonthFreeTrial == true -> "Start 3-month free trial"
                            offer != null -> "Continue to Google Play"
                            else -> "Offer unavailable"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Not now", color = TextMuted)
                }
            }
        }
    }
}
