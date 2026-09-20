package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.PinCredentialStore
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.GoldBright
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinLockScreen(
    viewModel: FinanceViewModel,
    onUseEmailPassword: () -> Unit
) {
    var pinDigits by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    // Seeded from the store so a lockout survives backgrounding and process death.
    var lockoutRemaining by remember { mutableStateOf(viewModel.pinLockoutRemainingMillis()) }
    val userEmail by viewModel.userEmail.collectAsState()
    val scope = rememberCoroutineScope()

    val isLockedOut = lockoutRemaining > 0L
    val keypadEnabled = !isLockedOut && !isVerifying

    // Counts an active lockout down so the keypad re-enables on its own, re-reading the store
    // rather than trusting a local timer.
    LaunchedEffect(isLockedOut) {
        while (lockoutRemaining > 0L) {
            delay(1_000L)
            lockoutRemaining = viewModel.pinLockoutRemainingMillis()
        }
    }

    fun onDigitPress(digit: String) {
        if (!keypadEnabled) return
        if (pinDigits.length < 4) {
            errorMessage = null
            val newPin = pinDigits + digit
            pinDigits = newPin
            if (newPin.length == 4) {
                // Verification is suspend now: the PIN is checked against a salted hash, which
                // is deliberately slow, so it cannot run inline on the main thread.
                isVerifying = true
                scope.launch {
                    when (val result = viewModel.verifyAndUnlockPin(newPin)) {
                        is PinCredentialStore.Verification.Success -> {
                            errorMessage = null
                        }
                        is PinCredentialStore.Verification.Incorrect -> {
                            val left = result.attemptsRemaining
                            errorMessage = if (left > 0) {
                                "Incorrect PIN. $left attempt${if (left == 1) "" else "s"} remaining."
                            } else {
                                "Incorrect PIN."
                            }
                            pinDigits = ""
                        }
                        is PinCredentialStore.Verification.LockedOut -> {
                            lockoutRemaining = result.retryAfterMillis
                            errorMessage = null
                            pinDigits = ""
                        }
                        is PinCredentialStore.Verification.NotSet -> {
                            errorMessage = "No PIN is set. Use email & password to sign in."
                            pinDigits = ""
                        }
                    }
                    isVerifying = false
                }
            }
        }
    }

    fun onBackspacePress() {
        if (!keypadEnabled) return
        if (pinDigits.isNotEmpty()) {
            errorMessage = null
            pinDigits = pinDigits.dropLast(1)
        }
    }

    fun onClearPress() {
        if (!keypadEnabled) return
        errorMessage = null
        pinDigits = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        ObsidianBg,
                        Color(0xFF070B14)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Vault Security Shield Icon
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = SovereignGold.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, SovereignGold)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = SovereignGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "QUICK PIN UNLOCK",
                color = SovereignGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )

            if (userEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userEmail,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4 Pin Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pinDigits.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) SovereignGold else ObsidianSurfaceVariant
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) SovereignGold else ObsidianBorder,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error / lockout / verifying status
            if (isVerifying) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = SovereignGold,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Checking PIN…",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                val statusMessage = when {
                    isLockedOut -> "Too many incorrect attempts. Try again in ${formatLockoutDuration(lockoutRemaining)}."
                    else -> errorMessage
                }
                statusMessage?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } ?: Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad Matrix (3x4 Grid)
            val keypadGrid = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keypadGrid.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        row.forEach { item ->
                            Surface(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = keypadEnabled) {
                                        when (item) {
                                            "C" -> onClearPress()
                                            "DEL" -> onBackspacePress()
                                            else -> onDigitPress(item)
                                        }
                                    },
                                shape = CircleShape,
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, ObsidianBorderSubtle)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    // Dimmed while locked out or verifying, so the keypad reads
                                    // as unavailable rather than unresponsive.
                                    val keyAlpha = if (keypadEnabled) 1f else 0.35f
                                    when (item) {
                                        "DEL" -> Icon(
                                            Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = SovereignGold.copy(alpha = keyAlpha),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        "C" -> Text(
                                            text = "CLEAR",
                                            color = TextMuted.copy(alpha = keyAlpha),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        else -> Text(
                                            text = item,
                                            color = TextPrimary.copy(alpha = keyAlpha),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Switch to Email & Password option
            TextButton(onClick = onUseEmailPassword) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = SovereignGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Use Email & Password Instead",
                        color = SovereignGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** Renders a remaining lockout as "2m 05s" / "45s". Rounds up so it never shows "0s". */
private fun formatLockoutDuration(millis: Long): String {
    val totalSeconds = ((millis + 999L) / 1000L).toInt().coerceAtLeast(1)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${"%02d".format(seconds)}s" else "${seconds}s"
}
