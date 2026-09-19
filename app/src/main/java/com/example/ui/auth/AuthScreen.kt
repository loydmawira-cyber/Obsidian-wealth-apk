package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

@Composable
fun AuthScreen(
    viewModel: FinanceViewModel,
    onUsePinUnlock: (() -> Unit)? = null
) {
    var isRegisterTab by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val isPinEnabled by viewModel.isPinEnabled.collectAsState()

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
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Luxury Vault Header Logo
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = SovereignGold.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, SovereignGold)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Vault Icon",
                        tint = SovereignGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OBSIDIAN VAULT",
                color = SovereignGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Institutional Wealth & Multi-Asset Intelligence",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Auth Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Tab Bar: Log In vs Register
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianSurfaceVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (!isRegisterTab) SovereignGold else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    isRegisterTab = false
                                    errorMessage = null
                                    successMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "LOG IN",
                                color = if (!isRegisterTab) ObsidianBg else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (isRegisterTab) SovereignGold else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    isRegisterTab = true
                                    errorMessage = null
                                    successMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "REGISTER",
                                color = if (isRegisterTab) ObsidianBg else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Feedback banners
                    errorMessage?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = err,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    successMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldGrowth.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldGrowth),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = EmeraldGrowth,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Email Field
                    Text(text = "EMAIL ADDRESS", color = SovereignGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = { Text("e.g. investor@sovereign.io", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SovereignGold,
                            unfocusedBorderColor = ObsidianBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    Text(text = "PASSWORD", color = SovereignGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("Enter account password", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SovereignGold,
                            unfocusedBorderColor = ObsidianBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    // Confirm Password if Registering
                    if (isRegisterTab) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "CONFIRM PASSWORD", color = SovereignGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            placeholder = { Text("Re-enter password", color = TextMuted, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SovereignGold,
                                unfocusedBorderColor = ObsidianBorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    } else {
                        // Forgot password button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showForgotPasswordDialog = true }) {
                                Text(
                                    text = "Forgot Password?",
                                    color = GoldBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Auth Action Button
                    Button(
                        onClick = {
                            errorMessage = null
                            successMessage = null
                            if (isRegisterTab) {
                                if (emailInput.isBlank() || !emailInput.contains("@")) {
                                    errorMessage = "Please enter a valid email address."
                                    return@Button
                                }
                                if (passwordInput.length < 4) {
                                    errorMessage = "Password must be at least 4 characters long."
                                    return@Button
                                }
                                if (passwordInput != confirmPasswordInput) {
                                    errorMessage = "Passwords do not match."
                                    return@Button
                                }
                                val ok = viewModel.registerUser(emailInput, passwordInput)
                                if (ok) {
                                    successMessage = "Account registered! Welcome to Obsidian Vault."
                                } else {
                                    errorMessage = "Registration failed. Please check your credentials."
                                }
                            } else {
                                if (emailInput.isBlank()) {
                                    errorMessage = "Please enter your email."
                                    return@Button
                                }
                                if (passwordInput.isBlank()) {
                                    errorMessage = "Please enter your password."
                                    return@Button
                                }
                                val ok = viewModel.loginWithEmail(emailInput, passwordInput)
                                if (!ok) {
                                    errorMessage = "Invalid email or password. Please try again."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isRegisterTab) "CREATE VAULT ACCOUNT" else "LOG IN TO VAULT",
                            color = ObsidianBg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                    }

                    // Quick PIN alternative card if PIN is configured!
                    if (isPinEnabled && onUsePinUnlock != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUsePinUnlock() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LOGIN WITH 4-DIGIT QUICK PIN",
                                    color = SovereignGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Forgot Password Reset Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = emailInput,
            onDismiss = { showForgotPasswordDialog = false },
            onReset = { resetEmail, newPassword ->
                val ok = viewModel.resetPassword(resetEmail, newPassword)
                if (ok) {
                    successMessage = "Password successfully reset! You can now log in with your new password."
                    emailInput = resetEmail
                    passwordInput = newPassword
                } else {
                    errorMessage = "Failed to reset password. Ensure email is valid."
                }
                showForgotPasswordDialog = false
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onReset: (email: String, newPassword: String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SovereignGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RESET VAULT PASSWORD",
                        color = SovereignGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your account email and choose a new password to recover access.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                errorText?.let { err ->
                    Text(text = err, color = Color(0xFFFCA5A5), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Account Email", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = ObsidianBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password", fontSize = 11.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = ObsidianBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password", fontSize = 11.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = ObsidianBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = TextMuted, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (email.isBlank() || !email.contains("@")) {
                                errorText = "Enter a valid email address."
                                return@Button
                            }
                            if (newPassword.length < 4) {
                                errorText = "New password must be at least 4 characters."
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                errorText = "Passwords do not match."
                                return@Button
                            }
                            onReset(email.trim(), newPassword)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
                    ) {
                        Text("RESET & UPDATE", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
