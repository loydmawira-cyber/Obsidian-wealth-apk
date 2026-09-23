package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import android.widget.Toast
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import com.example.alerts.NotificationReminderManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirestoreConnectionStatus
import com.example.data.models.AiRiskProfile
import com.example.data.models.FiscalCalendar
import com.example.data.models.GeographicRegion
import com.example.data.models.NumberFormatStyle
import com.example.data.models.SupportedCurrency
import com.example.data.models.UserSettings
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldGradientStart
import com.example.ui.theme.GoldLight
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ObsidianSettingsSheet(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onRequestClearAllData: () -> Unit = {},
    onOpenNotificationAccess: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settings by viewModel.userSettings.collectAsState()
    val isSyncing by viewModel.isSyncingCloud.collectAsState()
    val syncResult by viewModel.cloudSyncResult.collectAsState()
    val cloudVaultId by viewModel.cloudVaultId.collectAsState()
    val lastSyncTime by viewModel.lastCloudSyncTime.collectAsState()
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isDemoAccount = userEmail.equals(FinanceViewModel.DEMO_SEED_EMAIL, ignoreCase = true)
    val isPremium by viewModel.isPremium.collectAsState()
    val billingMessage by viewModel.billingMessage.collectAsState()
    val context = LocalContext.current

    var activeTab by remember { mutableStateOf(0) } // 0: Regional, 1: Modules, 2: AI & Privacy, 3: Cloud Vault
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showNotificationAppPicker by remember { mutableStateOf(false) }
    // Saving a PIN derives a salted hash off the main thread, so it needs a scope.
    val pinScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianBg,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .testTag("settings_bottom_sheet")
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                Brush.linearGradient(listOf(GoldGradientStart, Color(0xFF1E2638))),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.5.dp, SovereignGold, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Preferences",
                            tint = GoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "PREFERENCES & LOCALIZATION",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tailor modules & regional financial formatting",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(ObsidianSurfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Format Preview Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ObsidianSurfaceVariant,
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LIVE REGIONAL PREVIEW",
                            color = SovereignGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = settings.formatAmount(248650.80, forceVisible = true),
                            color = GoldBright,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Region: ${settings.region.title} · ${settings.currency.code}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF141C28),
                        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${settings.currency.flag} ${settings.currency.code}",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Selector: Regional vs Modules vs Alerts vs Privacy vs Cloud Sync
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    SettingsCategoryTab(
                        title = "Region",
                        icon = Icons.Default.Public,
                        isSelected = activeTab == 0,
                        onClick = { activeTab = 0 }
                    )
                }
                item {
                    SettingsCategoryTab(
                        title = "Modules",
                        icon = Icons.Default.Tune,
                        isSelected = activeTab == 1,
                        onClick = { activeTab = 1 }
                    )
                }
                item {
                    SettingsCategoryTab(
                        title = "Alerts & Reminders",
                        icon = Icons.Default.NotificationsActive,
                        isSelected = activeTab == 2,
                        onClick = { activeTab = 2 }
                    )
                }
                item {
                    SettingsCategoryTab(
                        title = "Privacy",
                        icon = Icons.Default.Security,
                        isSelected = activeTab == 3,
                        onClick = { activeTab = 3 }
                    )
                }
                item {
                    SettingsCategoryTab(
                        title = "Cloud Sync",
                        icon = Icons.Default.Refresh,
                        isSelected = activeTab == 4,
                        onClick = { activeTab = 4 }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Content Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (activeTab == 0) {
                    // GEOGRAPHICAL & REGIONAL SETTINGS
                    item {
                        Text(
                            text = "REGIONAL PRESET",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Quickly adapt currency, fiscal periods, and numbering to your country",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(GeographicRegion.values()) { reg ->
                                val isSelected = settings.region == reg
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) SovereignGold.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) SovereignGold else ObsidianBorderSubtle
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.applyRegionPreset(reg)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(reg.defaultCurrency.flag, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = reg.title,
                                            color = if (isSelected) GoldBright else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "BASE CURRENCY",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All valuations, ledgers, and portfolio gains are presented in this currency",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SupportedCurrency.values().forEach { cur ->
                                val isSelected = settings.currency == cur
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) SovereignGold.copy(alpha = 0.25f) else ObsidianSurfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) SovereignGold else ObsidianBorderSubtle
                                    ),
                                    modifier = Modifier.clickable {
                                        viewModel.updateCurrency(cur)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cur.flag, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = cur.code,
                                                color = if (isSelected) GoldBright else TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = cur.symbol,
                                                color = if (isSelected) SovereignGold else TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "FISCAL & TAX CALENDAR",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FiscalCalendar.values().forEach { fiscal ->
                            val isSelected = settings.fiscalCalendar == fiscal
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SovereignGold.copy(alpha = 0.15f) else ObsidianSurfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) SovereignGold else ObsidianBorderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.updateUserSettings(settings.copy(fiscalCalendar = fiscal))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fiscal.title,
                                            color = if (isSelected) GoldBright else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = fiscal.subtitle,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = SovereignGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "NUMBER SCALE & SEPARATOR NOTATION",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        NumberFormatStyle.values().forEach { style ->
                            val isSelected = settings.numberFormatStyle == style
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SovereignGold.copy(alpha = 0.15f) else ObsidianSurfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) SovereignGold else ObsidianBorderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.updateUserSettings(settings.copy(numberFormatStyle = style))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = style.title,
                                            color = if (isSelected) GoldBright else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = style.example,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = SovereignGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (activeTab == 1) {
                    // WORKSPACE MODULES ("WHAT WE NEED & DONT NEED")
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, ObsidianBorderSubtle)
                        ) {
                            Text(
                                text = "Customize which modules are enabled in your workspace. Disabled modules are cleanly omitted from your bottom navigation and overview cards so you only see what matters.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    item {
                        ModuleToggleCard(
                            title = "Investments & Portfolio",
                            description = "Stock holdings, mutual funds, automated SIPs, XIRR returns, and asset allocation breakdown.",
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            isEnabled = settings.enableInvestments,
                            onToggle = { viewModel.toggleModule("investments", it) }
                        )
                    }

                    item {
                        ModuleToggleCard(
                            title = "Debt & Credit Center",
                            description = "Credit card limits & utilization, loans amortizations, and Debt Avalanche / Snowball payoff models.",
                            icon = Icons.Default.CreditCard,
                            isEnabled = settings.enableDebtCenter,
                            onToggle = { viewModel.toggleModule("debt", it) }
                        )
                    }

                    item {
                        ModuleToggleCard(
                            title = "Cash Flow & Daily Ledger",
                            description = "Income, expense transactions, burn rate tracking, and AI Natural Language smart receipts.",
                            icon = Icons.Default.AccountBalanceWallet,
                            isEnabled = settings.enableCashFlow,
                            onToggle = { viewModel.toggleModule("cash_flow", it) }
                        )
                    }

                    item {
                        ModuleToggleCard(
                            title = "Goals & Milestones",
                            description = "Target capital accumulation, emergency liquid reserves, and FIRE independence progress.",
                            icon = Icons.Default.Flag,
                            isEnabled = settings.enableGoals,
                            onToggle = { viewModel.toggleModule("goals", it) }
                        )
                    }

                    item {
                        ModuleToggleCard(
                            title = "Floating Advisor AI Button",
                            description = "Persistent circular AI assistant button on the bottom corner of screens.",
                            icon = Icons.Default.AutoAwesome,
                            isEnabled = settings.showFloatingAiAdvisor,
                            onToggle = { viewModel.toggleModule("ai_advisor", it) }
                        )
                    }
                } else if (activeTab == 2) {
                    // PUSH NOTIFICATIONS & REMINDERS
                    item {
                        val trustedPackages by viewModel.trustedNotificationPackages.collectAsState()
                        val selectedLabels = trustedPackages

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("AUTOMATIC TRANSACTION DETECTION", color = SovereignGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Reads transaction notifications locally, only from apps you choose below, and places detected salary, payment, and transfer entries in review. Raw notification text is not uploaded, and notifications from apps you haven't selected are never read.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    if (selectedLabels.isEmpty()) "No apps selected yet" else "${selectedLabels.size} app(s) selected",
                                    color = if (selectedLabels.isEmpty()) TextMuted else SovereignGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showNotificationAppPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (selectedLabels.isEmpty()) "Choose apps to monitor" else "Change apps to monitor") }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (selectedLabels.isEmpty()) {
                                            showNotificationAppPicker = true
                                        } else {
                                            onOpenNotificationAccess()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Enable notification access", color = TextPrimary) }
                            }
                        }
                    }
                    item {
                        val hasNotificationPermission = NotificationReminderManager.hasPermission(context)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, if (settings.enableNotifications) SovereignGold.copy(alpha = 0.6f) else ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (settings.enableNotifications) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = "Push Notifications",
                                            tint = if (settings.enableNotifications) GoldBright else TextMuted,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Push Notifications & Alerts",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (settings.enableNotifications) "Automated wealth intelligence active" else "System alerts currently paused",
                                                color = if (settings.enableNotifications) EmeraldGrowth else TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = settings.enableNotifications,
                                        onCheckedChange = { viewModel.toggleNotificationOption("all", it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = GoldBright,
                                            checkedTrackColor = SovereignGold.copy(alpha = 0.4f),
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = ObsidianSurface
                                        )
                                    )
                                }

                                if (!hasNotificationPermission) {
                                    val amber = Color(0xFFF59E0B)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2D1600),
                                        border = BorderStroke(1.dp, amber.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = amber, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "System notification permission is required for alerts to appear in your device notification shade.",
                                                color = Color(0xFFFDE68A),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "AUTOMATED REMINDERS & SCHEDULES",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure which financial events and triggers send push alerts",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    // Bill & Debt Due Reminders
                    item {
                        ModuleToggleCard(
                            title = "Bill & Credit Due Reminders",
                            description = "Receive early alerts 1–3 days before revolving credit card payments and loan EMI schedules are due.",
                            icon = Icons.Default.CreditCard,
                            isEnabled = settings.enableBillDueReminders && settings.enableNotifications,
                            onToggle = { viewModel.toggleNotificationOption("bills", it) }
                        )
                    }

                    // SIP Investment Reminders
                    item {
                        ModuleToggleCard(
                            title = "SIP Standing Order Reminders",
                            description = "Timely notifications when scheduled monthly mutual fund and equity SIP debits are upcoming.",
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            isEnabled = settings.enableSipReminders && settings.enableNotifications,
                            onToggle = { viewModel.toggleNotificationOption("sips", it) }
                        )
                    }

                    // Daily Financial Briefing Reminders
                    item {
                        ModuleToggleCard(
                            title = "Daily Wealth Briefing",
                            description = "Evening push recap of today's cash burn, net savings velocity, and progress toward financial goals.",
                            icon = Icons.Default.Flag,
                            isEnabled = settings.enableDailyBriefingReminders && settings.enableNotifications,
                            onToggle = { viewModel.toggleNotificationOption("briefing", it) }
                        )
                    }

                    // Test Push Notification Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF141A24),
                            border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Test Notification",
                                        tint = SovereignGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TEST DEVICE NOTIFICATIONS",
                                        color = GoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Send an immediate test reminder to verify notification delivery and sound on your Android device.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.triggerTestNotification()
                                        Toast.makeText(context, "🔔 Test push reminder sent to notifications tray!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF0D0A00), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Test Push Reminder", color = Color(0xFF0D0A00), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    // Proactive Intelligence Triggers Summary
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "PROACTIVE ALERTS ENGINE",
                                    color = SovereignGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Obsidian's background engine periodically evaluates:\n• Monthly spending surges exceeding 20% vs prior period\n• Outsized category concentration (>40% of outflow)\n• Accounts reaching payment due milestones\n• Goals falling behind monthly target velocity",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else if (activeTab == 3) {
                    // AI & PRIVACY SETTINGS
                    item {
                        Text(
                            text = "PRIVACY & VISUAL DISCRETION",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (settings.hideBalances) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Hide Balances",
                                        tint = SovereignGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Stealth / Discreet Mode",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Mask balances with •••••• when viewing in public spaces",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = settings.hideBalances,
                                    onCheckedChange = { viewModel.toggleHideBalances() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = GoldBright,
                                        checkedTrackColor = SovereignGold.copy(alpha = 0.4f),
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = ObsidianSurface
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Biometrics",
                                        tint = EmeraldGrowth,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Biometric Vault Security",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Require fingerprint/face authentication on resume",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = settings.biometricProtection,
                                    onCheckedChange = {
                                        viewModel.updateUserSettings(settings.copy(biometricProtection = it))
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = EmeraldGrowth,
                                        checkedTrackColor = EmeraldGrowth.copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = ObsidianSurface
                                    )
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, if (isPinEnabled) SovereignGold else ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "4-Digit Quick PIN",
                                            tint = SovereignGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Quick 4-Digit PIN Unlock (Optional)",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isPinEnabled) "4-Digit Quick PIN active for fast vault login" else "Set a 4-digit PIN to login without typing password every time",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isPinEnabled) {
                                        Button(
                                            onClick = { showSetPinDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = SovereignGold.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, SovereignGold)
                                        ) {
                                            Text("CHANGE PIN", color = SovereignGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.disableQuickPin() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33F43F5E)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFFF43F5E))
                                        ) {
                                            Text("DISABLE PIN", color = Color(0xFFFB7185), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { showSetPinDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("SET 4-DIGIT PIN", color = ObsidianBg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, if (isPremium) EmeraldGrowth else SovereignGold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isPremium) "PREMIUM MARKET PRICES ACTIVE" else "PREMIUM MARKET PRICES",
                                            color = if (isPremium) EmeraldGrowth else SovereignGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.7.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isPremium) "Automatic stock and crypto price updates are enabled." else "All finance features remain unlimited. Premium only adds automatic market prices.",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                    if (!isPremium) {
                                        Button(
                                            onClick = { (context as? android.app.Activity)?.let(viewModel::launchPremiumPurchase) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("GO PREMIUM", color = ObsidianBg, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isPremium) "Google Play subscription: premium_annual" else "Annual subscription • Managed by Google Play",
                                    color = if (isPremium) EmeraldGrowth else TextSecondary,
                                    fontSize = 10.sp
                                )
                                billingMessage?.let { message ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = message, color = Color(0xFFFCA5A5), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Account: ${if (userEmail.isNotBlank()) userEmail else "investor@sovereign.io"}",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Sign out of your Obsidian Vault session",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.logout()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22F43F5E)),
                                    border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LOG OUT", color = Color(0xFFFB7185), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "AI WEALTH ADVISOR RISK PERSONA",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        AiRiskProfile.values().forEach { profile ->
                            val isSelected = settings.aiRiskProfile == profile
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SovereignGold.copy(alpha = 0.15f) else ObsidianSurfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) SovereignGold else ObsidianBorderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.updateUserSettings(settings.copy(aiRiskProfile = profile))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.title,
                                            color = if (isSelected) GoldBright else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = profile.subtitle,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = SovereignGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (activeTab == 4) {
                    // CLOUD FIRESTORE VAULT SYNCHRONIZATION
                    item {
                        Text(
                            text = "CLOUD FIRESTORE VAULT",
                            color = SovereignGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Synchronize your entire financial workspace to Google Cloud Firestore with real-time replication and multi-device backup.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    // Connection Status
                    item {
                        val status = remember { viewModel.getFirestoreConnectionStatus() }
                        val isConfigured = status is FirestoreConnectionStatus.Active
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isConfigured) EmeraldGrowth.copy(alpha = 0.12f) else SovereignGold.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (isConfigured) EmeraldGrowth.copy(alpha = 0.5f) else SovereignGold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(if (isConfigured) EmeraldGrowth else SovereignGold, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isConfigured) "CLOUD FIRESTORE ACTIVE" else "FIRESTORE CONFIGURED & READY",
                                        color = if (isConfigured) EmeraldGrowth else GoldBright,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isConfigured)
                                        "Firebase App is active. Room entities synchronize seamlessly with Firestore collections under your unique Vault ID."
                                    else
                                        "The Firestore SDK, schema mappings, and Room synchronization bridges are completely configured. To connect to a live Firebase backend, drop your google-services.json file into the app/ directory.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Sync Result Banner
                    syncResult?.let { result ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (result.success) EmeraldGrowth.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (result.success) EmeraldGrowth else Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (result.success) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (result.success) EmeraldGrowth else Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = result.message,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearCloudSyncResult() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Vault ID Configuration Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "CLOUD VAULT IDENTIFIER",
                                    color = SovereignGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your vault is bound to your signed-in account. Sign in with the same account on another device to synchronize the same financial records.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                // Read-only. The vault id is the authenticated uid; making it
                                // editable allowed pointing a restore at another user's vault.
                                Text(
                                    text = "Vault ID",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (cloudVaultId.isBlank()) "Not signed in" else cloudVaultId,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Last Cloud Synchronization:",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = if (lastSyncTime > 0)
                                            java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastSyncTime))
                                        else
                                            "Never synced",
                                        color = if (lastSyncTime > 0) EmeraldGrowth else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Cloud Operations (Push & Restore)
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(1.dp, ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "CLOUD OPERATIONS",
                                    color = SovereignGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Push button
                                Button(
                                    onClick = { viewModel.syncVaultToCloud() },
                                    enabled = !isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            color = ObsidianBg,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("SYNCHRONIZING...", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = ObsidianBg, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("PUSH LOCAL VAULT TO CLOUD", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Restore button
                                Button(
                                    onClick = { viewModel.restoreVaultFromCloud() },
                                    enabled = !isSyncing,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                                    border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            color = GoldBright,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("FETCHING CLOUD SNAPSHOT...", color = GoldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GoldBright, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("RESTORE LOCAL VAULT FROM CLOUD", color = GoldBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Schema Hierarchy Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, ObsidianBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "FIRESTORE SCHEMA HIERARCHY",
                                    color = SovereignGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val schemaItems = listOf(
                                    "wealth_vaults/{vaultId}/transactions" to "Transactions, dates, accounts, notes",
                                    "wealth_vaults/{vaultId}/holdings" to "Equities, MMFs, bonds, real-time gain/loss",
                                    "wealth_vaults/{vaultId}/sips" to "Automated standing orders, monthly debit days",
                                    "wealth_vaults/{vaultId}/credit_cards" to "Balances, limits, APR, due dates",
                                    "wealth_vaults/{vaultId}/loans" to "Principal, EMI servicing, amortizations",
                                    "wealth_vaults/{vaultId}/goals" to "Accumulation milestones, FIRE progress",
                                    "wealth_vaults/{vaultId}/settings" to "Currency (KES/USD), region, privacy masks"
                                )
                                schemaItems.forEach { (col, desc) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = col, color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                        Text(text = desc, color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save / Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
            ) {
                Text(
                    text = "APPLY PREFERENCES",
                    color = ObsidianBg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showSetPinDialog) {
            SetPinDialog(
                onDismiss = { showSetPinDialog = false },
                onSavePin = { pin ->
                    pinScope.launch { viewModel.saveQuickPin(pin) }
                    showSetPinDialog = false
                }
            )
        }

        if (showNotificationAppPicker) {
            val currentlyTrusted by viewModel.trustedNotificationPackages.collectAsState()
            NotificationSourceAppsDialog(
                apps = remember { viewModel.getLaunchableApps() },
                initiallySelected = currentlyTrusted,
                onDismiss = { showNotificationAppPicker = false },
                onConfirm = { picked ->
                    viewModel.setTrustedNotificationPackages(picked)
                    showNotificationAppPicker = false
                    onOpenNotificationAccess()
                }
            )
        }
    }
}

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, SovereignGold)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SovereignGold,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SET 4-DIGIT QUICK PIN",
                    color = SovereignGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter a 4-digit PIN to unlock your vault quickly without re-typing email & password every session.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInput = it },
                    label = { Text("4-Digit PIN", fontSize = 11.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
                    value = confirmInput,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmInput = it },
                    label = { Text("Confirm 4-Digit PIN", fontSize = 11.sp) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SovereignGold,
                        unfocusedBorderColor = ObsidianBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                            if (pinInput.length != 4) {
                                errorMessage = "PIN must be exactly 4 digits."
                                return@Button
                            }
                            if (pinInput != confirmInput) {
                                errorMessage = "PINs do not match. Try again."
                                return@Button
                            }
                            isSaving = true
                            onSavePin(pinInput)
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = ObsidianBg, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVING…", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("SAVE PIN", color = ObsidianBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (isSelected) SovereignGold.copy(alpha = 0.22f) else Color.Transparent,
        label = "tab_bg"
    )
    val textColor by animateColorAsState(
        if (isSelected) GoldBright else TextMuted,
        label = "tab_text"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(
            1.dp,
            if (isSelected) SovereignGold.copy(alpha = 0.5f) else Color.Transparent
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ModuleToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ObsidianSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isEnabled) SovereignGold.copy(alpha = 0.35f) else ObsidianBorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isEnabled) SovereignGold.copy(alpha = 0.2f) else ObsidianSurface,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = if (isEnabled) SovereignGold else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isEnabled) EmeraldGrowth.copy(alpha = 0.2f) else Color(0xFF242B38)
                        ) {
                            Text(
                                text = if (isEnabled) "ENABLED" else "DISABLED",
                                color = if (isEnabled) EmeraldGrowth else TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GoldBright,
                    checkedTrackColor = SovereignGold.copy(alpha = 0.4f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = ObsidianSurface
                )
            )
        }
    }
}
