package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Category
import com.example.data.models.CreditCardEntity
import com.example.data.models.TransactionType
import com.example.ui.components.AddLoanDialog
import com.example.ui.components.AddCreditCardDialog
import com.example.ui.components.AddGoalDialog
import com.example.ui.components.ConfirmClearDataDialog
import com.example.ui.components.ConfirmLoanPaymentDialog
import com.example.data.models.UserSettings
import com.example.data.models.ThemeMode
import com.example.data.models.AccentColor
import com.example.ui.components.ThemePickerDialog
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.PinLockScreen
import com.example.ui.components.AddHoldingDialog
import com.example.ui.components.ConfirmDeleteHoldingDialog
import com.example.ui.components.EditHoldingDialog
import com.example.ui.components.SellHoldingDialog
import com.example.ui.components.HoldingActionDialog
import com.example.ui.components.AddSipDialog
import com.example.ui.components.ConfirmDeleteSipDialog
import com.example.ui.components.EditSipDialog
import com.example.ui.components.SipActionDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.AiSmartLogDialog
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.ObsidianAiAdvisorSheet
import com.example.ui.components.ObsidianSettingsSheet
import com.example.ui.components.ImportedTransactionsDialog
import com.example.ui.components.PayCreditCardDialog
import com.example.ui.screens.CashFlowScreen
import com.example.ui.screens.DebtCenterScreen
import com.example.ui.screens.GoalsAndReportsScreen
import com.example.ui.screens.InvestmentsScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanLight
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldGrowth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldGradientStart
import com.example.ui.theme.GoldLight
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import com.example.ui.viewmodel.FinanceTab
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("MainActivity", "POST_NOTIFICATIONS granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val appCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                val debugFactory = com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                appCheck.installAppCheckProviderFactory(debugFactory)
                android.util.Log.d("ObsidianAppCheck", "Initialized App Check with DebugAppCheckProviderFactory.")
            } else {
                val integrityFactory = com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
                appCheck.installAppCheckProviderFactory(integrityFactory)
                android.util.Log.d("ObsidianAppCheck", "Initialized App Check with PlayIntegrityAppCheckProviderFactory.")
            }
        } catch (e: Exception) {
            android.util.Log.e("ObsidianAppCheck", "App Check init notice", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        try {
            com.example.alerts.NotificationReminderManager.initChannels(this)
            com.example.alerts.NotificationReminderManager.schedulePeriodicAlerts(this)
            // Cancel legacy automatic SIP ledger writes. SIPs are plans/reminders only unless
            // a real contribution is confirmed or imported by the customer.
            com.example.alerts.NotificationReminderManager.cancelSipDebitEngine(this)
        } catch (e: Exception) {
            android.util.Log.e("ObsidianAlerts", "Notification init notice", e)
        }

        enableEdgeToEdge()
        setContent {
            val userSettings by viewModel.userSettings.collectAsState()
            MyApplicationTheme(
                themeMode = userSettings.themeMode,
                accentColor = userSettings.accentColor
            ) {
                ObsidianApp(viewModel)
            }
        }
    }
}

@Composable
fun ObsidianApp(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isPinLocked by viewModel.isPinLocked.collectAsState()
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()

    if (!isLoggedIn) {
        AuthScreen(
            viewModel = viewModel,
            onUsePinUnlock = if (isPinEnabled) { { viewModel.openPinLock() } } else null
        )
        return
    }

    if (isPinLocked) {
        PinLockScreen(
            viewModel = viewModel,
            onUseEmailPassword = { viewModel.logout() }
        )
        return
    }

    val selectedTab by viewModel.selectedTab.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    // Dialog & Sheet States
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showAiSmartLogDialog by remember { mutableStateOf(false) }
    var showAddHoldingDialog by remember { mutableStateOf(false) }
    var showAddSipDialog by remember { mutableStateOf(false) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showAddCreditCardDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAiAdvisorSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showThemePickerDialog by remember { mutableStateOf(false) }
    var showExportReportDialog by remember { mutableStateOf(false) }
    var showImportedTransactionsDialog by remember { mutableStateOf(false) }
    var exportReportContent by remember { mutableStateOf("") }
    var cardToPay by remember { mutableStateOf<CreditCardEntity?>(null) }
    var loanToPay by remember { mutableStateOf<com.example.data.models.LoanEntity?>(null) }
    var sipActionTarget by remember { mutableStateOf<com.example.data.models.SipEntity?>(null) }
    var sipToEdit by remember { mutableStateOf<com.example.data.models.SipEntity?>(null) }
    var sipToDelete by remember { mutableStateOf<com.example.data.models.SipEntity?>(null) }
    var holdingActionTarget by remember { mutableStateOf<com.example.data.models.HoldingEntity?>(null) }
    var holdingToSell by remember { mutableStateOf<com.example.data.models.HoldingEntity?>(null) }
    var holdingToEdit by remember { mutableStateOf<com.example.data.models.HoldingEntity?>(null) }
    var holdingToDelete by remember { mutableStateOf<com.example.data.models.HoldingEntity?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ObsidianTopBar(
                userSettings = userSettings,
                onOpenSettings = { showSettingsSheet = true },
                onToggleHideBalances = { viewModel.toggleHideBalances() },
                onOpenThemePicker = { showThemePickerDialog = true }
            )
        },
        floatingActionButton = {
            if (userSettings.showFloatingAiAdvisor) {
                FloatingAiAdvisorButton(
                    onClick = { showAiAdvisorSheet = true }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            ObsidianBottomNavigation(
                currentTab = selectedTab,
                userSettings = userSettings,
                onTabSelect = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    FinanceTab.OVERVIEW -> OverviewScreen(
                        viewModel = viewModel,
                        onOpenAiAdvisor = { prompt ->
                            if (prompt != null) {
                                viewModel.askAi(prompt)
                            }
                            showAiAdvisorSheet = true
                        },
                        onQuickAddTransaction = { showAddTransactionDialog = true }
                    )

                    FinanceTab.CASH_FLOW -> CashFlowScreen(
                        viewModel = viewModel,
                        onQuickAdd = { showAddTransactionDialog = true },
                        onAiSmartLog = { showAiSmartLogDialog = true },
                        onReviewImported = { showImportedTransactionsDialog = true }
                    )

                    FinanceTab.INVEST -> InvestmentsScreen(
                        viewModel = viewModel,
                        onAddHolding = { showAddHoldingDialog = true },
                        onHoldingLongPress = { holdingActionTarget = it },
                        onAddSip = { showAddSipDialog = true },
                        onSipLongPress = { sipActionTarget = it },
                        onOpenAiAdvisor = { prompt ->
                            viewModel.askAi(prompt)
                            showAiAdvisorSheet = true
                        }
                    )

                    FinanceTab.DEBT -> DebtCenterScreen(
                        viewModel = viewModel,
                        onPayCard = { cardToPay = it },
                        onPayLoan = { loanToPay = it },
                        onAddLoan = { showAddLoanDialog = true },
                        onAddCreditCard = { showAddCreditCardDialog = true }
                    )

                    FinanceTab.GOALS_REPORTS -> GoalsAndReportsScreen(
                        viewModel = viewModel,
                        onAddGoal = { showAddGoalDialog = true },
                        onExportReport = { report ->
                            exportReportContent = report
                            showExportReportDialog = true
                        },
                        onAskAiToExplain = { prompt ->
                            viewModel.askAi(prompt)
                            showAiAdvisorSheet = true
                        }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            accounts = accounts,
            onDismiss = { showAddTransactionDialog = false },
            onAdd = { title, amt, type, cat, account, note ->
                viewModel.addTransaction(title, amt, type, cat, account, note = note)
            },
            onAiSmartLog = {
                showAddTransactionDialog = false
                showAiSmartLogDialog = true
            }
        )
    }

    if (showAiSmartLogDialog) {
        AiSmartLogDialog(
            accounts = accounts,
            onDismiss = { showAiSmartLogDialog = false },
            onConfirm = { transaction ->
                viewModel.addTransaction(transaction)
                showAiSmartLogDialog = false
            }
        )
    }

    if (showAddHoldingDialog) {
        AddHoldingDialog(
            defaultCurrency = userSettings.currency,
            accounts = accounts,
            onDismiss = { showAddHoldingDialog = false },
            onAdd = { sym, name, type, shares, avg, cur, currencyCode, purchaseAccount ->
                viewModel.addHolding(sym, name, type, shares, avg, cur, currencyCode, purchaseAccount)
            }
        )
    }

    if (showAddSipDialog) {
        AddSipDialog(
            defaultCurrency = userSettings.currency,
            accounts = accounts,
            onDismiss = { showAddSipDialog = false },
            onAdd = { fund, cat, amt, day, currencyCode, contributionAccount ->
                viewModel.addSip(fund, cat, amt, day, currencyCode, contributionAccount)
            }
        )
    }

    if (holdingActionTarget != null) {
        val holding = holdingActionTarget!!
        HoldingActionDialog(
            holding = holding,
            onDismiss = { holdingActionTarget = null },
            onEdit = { holdingToEdit = holding },
            onDelete = { holdingToDelete = holding },
            onSell = { holdingToSell = holding }
        )
    }

    if (holdingToSell != null) {
        val holding = holdingToSell!!
        SellHoldingDialog(
            holding = holding,
            accounts = accounts,
            formatAmount = { amount, currencyCode -> viewModel.formatAmount(amount, currencyCode) },
            onDismiss = { holdingToSell = null },
            onSell = { qty, price, account -> viewModel.sellHolding(holding, qty, price, account) }
        )
    }

    if (holdingToEdit != null) {
        val holding = holdingToEdit!!
        EditHoldingDialog(
            holding = holding,
            defaultCurrency = userSettings.currency,
            onDismiss = { holdingToEdit = null },
            onSave = { symbol, name, type, shares, avgBuy, current, currencyCode ->
                viewModel.updateHolding(
                    holding.copy(
                        symbol = symbol,
                        name = name,
                        type = type,
                        shares = shares,
                        avgBuyPrice = avgBuy,
                        currentPrice = current,
                        currencyCode = currencyCode
                    )
                )
            }
        )
    }

    if (holdingToDelete != null) {
        val holding = holdingToDelete!!
        ConfirmDeleteHoldingDialog(
            holdingName = holding.name,
            onDismiss = { holdingToDelete = null },
            onConfirm = { viewModel.deleteHolding(holding) }
        )
    }

    if (sipActionTarget != null) {
        val sip = sipActionTarget!!
        SipActionDialog(
            sip = sip,
            onDismiss = { sipActionTarget = null },
            onEdit = { sipToEdit = sip },
            onDelete = { sipToDelete = sip }
        )
    }

    if (sipToEdit != null) {
        val sip = sipToEdit!!
        EditSipDialog(
            sip = sip,
            onDismiss = { sipToEdit = null },
            onSave = { fund, cat, amt, day ->
                viewModel.updateSip(
                    sip.copy(
                        fundName = fund,
                        category = cat,
                        monthlyAmount = amt,
                        debitDayOfMonth = day
                    )
                )
            }
        )
    }

    if (sipToDelete != null) {
        val sip = sipToDelete!!
        ConfirmDeleteSipDialog(
            fundName = sip.fundName,
            onDismiss = { sipToDelete = null },
            onConfirm = { viewModel.deleteSip(sip) }
        )
    }

    if (showAddLoanDialog) {
        AddLoanDialog(defaultCurrency = userSettings.currency, onDismiss = { showAddLoanDialog = false }) { name, lender, total, remaining, emi, apr, months, currencyCode ->
            viewModel.addLoan(name, lender, total, remaining, emi, apr, months, currencyCode)
        }
    }

    if (showAddCreditCardDialog) {
        AddCreditCardDialog(defaultCurrency = userSettings.currency, onDismiss = { showAddCreditCardDialog = false }) { name, balance, limit, apr, due, currencyCode ->
            viewModel.addCreditCard(name, balance, limit, apr, due, currencyCode)
        }
    }

    if (showAddGoalDialog) {
        AddGoalDialog(defaultCurrency = userSettings.currency, onDismiss = { showAddGoalDialog = false }) { title, category, target, current, monthly, currencyCode ->
            viewModel.addGoal(title, category, target, current, monthly, currencyCode)
        }
    }

    if (showThemePickerDialog) {
        ThemePickerDialog(
            currentMode = userSettings.themeMode,
            currentAccent = userSettings.accentColor,
            onDismiss = { showThemePickerDialog = false },
            onModeChange = { viewModel.setThemeMode(it) },
            onAccentChange = { viewModel.setAccentColor(it) }
        )
    }

    if (loanToPay != null) {
        val loan = loanToPay!!
        ConfirmLoanPaymentDialog(
            accounts = accounts,
            loanName = loan.loanName,
            paymentAmount = loan.emiAmount.coerceAtLeast(0.0).coerceAtMost(
                loan.remainingBalance + loan.remainingBalance * loan.interestRate.coerceAtLeast(0.0) / 1200.0
            ),
            currencySymbol = com.example.data.models.SupportedCurrency.values().firstOrNull { it.code == loan.currencyCode }?.symbol ?: "",
            currencyCode = loan.currencyCode,
            remainingBalance = loan.remainingBalance,
            interestRate = loan.interestRate,
            onDismiss = { loanToPay = null },
            onConfirm = { account -> viewModel.payLoanEmi(loan, account) }
        )
    }

    if (showClearDataDialog) {
        ConfirmClearDataDialog(
            onDismiss = { showClearDataDialog = false },
            onConfirm = { viewModel.clearAllFinancialData() }
        )
    }

    if (cardToPay != null) {
        PayCreditCardDialog(
            card = cardToPay!!,
            accounts = accounts,
            onDismiss = { cardToPay = null },
            onPay = { amt, account ->
                viewModel.payCreditCard(cardToPay!!, amt, account)
            }
        )
    }

    if (showExportReportDialog) {
        ExportReportDialog(
            summaryText = exportReportContent,
            onDismiss = { showExportReportDialog = false }
        )
    }

    if (showAiAdvisorSheet) {
        ObsidianAiAdvisorSheet(
            messages = chatMessages,
            isThinking = isAiThinking,
            onSendMessage = { q -> viewModel.askAi(q) },
            onDismiss = { showAiAdvisorSheet = false },
            onOpenGoalForm = { showAddGoalDialog = true },
            onOpenTransactionForm = { showAddTransactionDialog = true }
        )
    }

    if (showSettingsSheet) {
        ObsidianSettingsSheet(
            viewModel = viewModel,
            onDismiss = { showSettingsSheet = false },
            onRequestClearAllData = { showClearDataDialog = true }
        )
    }
    if (showImportedTransactionsDialog) {
        ImportedTransactionsDialog(
            transactions = transactions.filter { it.importStatus == "PENDING_REVIEW" },
            accounts = accounts,
            formatAmount = { amount, currencyCode -> viewModel.formatAmount(amount, currencyCode) },
            onConfirm = { transaction, account -> viewModel.confirmImportedTransaction(transaction, account) },
            onIgnore = viewModel::ignoreImportedTransaction,
            onDismiss = { showImportedTransactionsDialog = false }
        )
    }
}

@Composable
fun ObsidianTopBar(
    userSettings: UserSettings,
    onOpenSettings: () -> Unit = {},
    onToggleHideBalances: () -> Unit = {},
    onOpenThemePicker: () -> Unit = {}
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val accent = MaterialTheme.colorScheme.primary
    val barBg = MaterialTheme.colorScheme.background
    val barSurface = MaterialTheme.colorScheme.surface

    Surface(
        color = barBg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            barSurface,
                            barBg
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = statusBarPadding.calculateTopPadding() + 8.dp,
                        bottom = 12.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Brand Monogram & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(GoldGradientStart, Color(0xFF1E2638))
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .border(1.5.dp, accent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Obsidian Shield",
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "OBSIDIAN WEALTH",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(EmeraldGrowth, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vault Encrypted",
                                color = accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }

                // Right: Regional Badge, Privacy Toggle, Theme Picker & Settings menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Regional Currency Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF121824),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clickable { onOpenSettings() }
                            .testTag("currency_selector_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = userSettings.currency.flag,
                                fontSize = 11.sp
                            )
                            Text(
                                text = userSettings.currency.code,
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Discreet / Hide Balances Toggle
                    IconButton(
                        onClick = onToggleHideBalances,
                        modifier = Modifier
                            .size(32.dp)
                            .background(ObsidianSurfaceVariant, CircleShape)
                            .border(1.dp, GoldBorder.copy(alpha = 0.35f), CircleShape)
                            .testTag("toggle_hide_balances_button")
                    ) {
                        Icon(
                            imageVector = if (userSettings.hideBalances) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (userSettings.hideBalances) "Show Balances" else "Hide Balances",
                            tint = if (userSettings.hideBalances) accent else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Appearance / Theme Picker Button
                    IconButton(
                        onClick = onOpenThemePicker,
                        modifier = Modifier
                            .size(32.dp)
                            .background(ObsidianSurfaceVariant, CircleShape)
                            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                            .testTag("open_theme_picker_button")
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Appearance",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 3-Dash (Hamburger) Menu Button -> Opens Settings
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .background(ObsidianSurfaceVariant, CircleShape)
                            .border(1.2.dp, accent.copy(alpha = 0.65f), CircleShape)
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu & Settings",
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom Accent Horizon Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accent.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun FloatingAiAdvisorButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(0xFF161C26),
        border = BorderStroke(1.5.dp, accent),
        shadowElevation = 8.dp,
        modifier = modifier
            .padding(bottom = 6.dp, end = 2.dp)
            .size(48.dp)
            .testTag("floating_ai_advisor_button")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF382914),
                            Color(0xFF181E2A),
                            Color(0xFF0F141E)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "Advisor AI",
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            // Tiny active pulse dot
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 9.dp)
                    .size(5.dp)
                    .background(EmeraldGrowth, CircleShape)
            )
        }
    }
}

@Composable
fun ObsidianBottomNavigation(
    currentTab: FinanceTab,
    userSettings: UserSettings,
    onTabSelect: (FinanceTab) -> Unit
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    val allTabs = listOf(
        NavigationTabItem(FinanceTab.OVERVIEW, "Overview", Icons.AutoMirrored.Filled.TrendingUp, true),
        NavigationTabItem(FinanceTab.CASH_FLOW, "Cash Flow", Icons.Default.AccountBalanceWallet, userSettings.enableCashFlow),
        NavigationTabItem(FinanceTab.INVEST, "Invest", Icons.AutoMirrored.Filled.ShowChart, userSettings.enableInvestments),
        NavigationTabItem(FinanceTab.DEBT, "Debt", Icons.Default.CreditCard, userSettings.enableDebtCenter),
        NavigationTabItem(FinanceTab.GOALS_REPORTS, "Goals", Icons.Default.Flag, userSettings.enableGoals)
    )
    val activeTabs = allTabs.filter { it.isEnabled }
    val accent = MaterialTheme.colorScheme.primary
    val barBg = MaterialTheme.colorScheme.background

    Surface(
        color = barBg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = navBarPadding.calculateBottomPadding() + 4.dp
                ),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            activeTabs.forEach { item ->
                val isSelected = currentTab == item.tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTabSelect(item.tab) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isSelected) accent.copy(alpha = 0.22f) else Color.Transparent,
                                CircleShape
                            )
                            .then(
                                if (isSelected) Modifier.border(1.dp, accent.copy(alpha = 0.6f), CircleShape) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) accent else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        color = if (isSelected) accent else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

data class NavigationTabItem(
    val tab: FinanceTab,
    val label: String,
    val icon: ImageVector,
    val isEnabled: Boolean = true
)
