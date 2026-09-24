package com.example.ui.viewmodel

import android.app.Application
import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AdvisorSnapshot
import com.example.ai.GeminiClient
import com.example.alerts.NotificationReminderManager
import com.example.data.billing.BillingManager
import com.example.data.database.AppDatabase
import com.example.data.models.Category
import com.example.data.models.CreditCardEntity
import com.example.data.models.GeographicRegion
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.HoldingType
import com.example.data.models.LoanEntity
import com.example.data.models.SipEntity
import com.example.data.models.SupportedCurrency
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import com.example.data.models.UserSettings
import com.example.data.firebase.CloudSyncResult
import com.example.data.firebase.FirestoreConnectionStatus
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.repository.FinanceRepository
import com.example.data.repository.PreferencesManager
import com.example.data.security.PinCredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FinanceTab {
    OVERVIEW, CASH_FLOW, INVEST, DEBT, GOALS_REPORTS
}

enum class TimeFrame {
    ONE_MONTH, SIX_MONTHS, ONE_YEAR, ALL
}

enum class PayoffStrategy {
    AVALANCHE, SNOWBALL
}

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false
)

/**
 * Derived view of the user's position.
 *
 * Every default is zero on purpose. These fields previously defaulted to a demo dataset
 * (net worth 11,877,250 / inflow 570,000 / savings rate 54.9% / "XIRR" 17.4 / health score 91),
 * and the summary builder substituted those same literals whenever a real total came out at 0.0.
 * The effect was that an empty or partly-filled vault displayed fabricated figures as if they
 * were the user's own. An empty vault must read as empty.
 *
 * `portfolioReturnPercent` is simple return on cost basis — NOT XIRR. A money-weighted return
 * needs the date and amount of every contribution, which the schema does not record.
 */
data class FinanceSummary(
    val totalNetWorth: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val liquidCash: Double = 0.0,
    val goalsValue: Double = 0.0,
    val totalInflow: Double = 0.0,
    val totalOutflow: Double = 0.0,
    val netCashRetained: Double = 0.0,
    // Rolling last 30 days; used for savings rate, DTI and the advisor's "monthly" figures.
    val recentInflow: Double = 0.0,
    val recentOutflow: Double = 0.0,
    val savingsRate: Double = 0.0,
    val portfolioValue: Double = 0.0,
    val portfolioCost: Double = 0.0,
    val portfolioDayGain: Double = 0.0,
    val portfolioDayGainPercent: Double = 0.0,
    val portfolioReturnPercent: Double = 0.0,
    val totalDebt: Double = 0.0,
    val monthlyDebtServicing: Double = 0.0,
    val dtiRatio: Double = 0.0,
    val transactionCount: Int = 0,
    val holdingCount: Int = 0,
    val debtAccountCount: Int = 0,
    val goalCount: Int = 0
)

/** Cash flow for one calendar month, including the balance carried in and out. */
data class MonthCashFlow(
    val monthStart: Long = 0L,
    val monthEnd: Long = 0L,
    val label: String = "",
    val opening: Double = 0.0,
    val inflow: Double = 0.0,
    val outflow: Double = 0.0,
    val invested: Double = 0.0,
    val closing: Double = 0.0,
    val isCurrentMonth: Boolean = true
)

/** Categories that move money into something you own rather than spend it. */
private val nonSpendingCategories = setOf(Category.INVESTMENT_SIP, Category.GOAL_SAVINGS)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val billingManager = BillingManager(application)
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(application)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            android.util.Log.e("FinanceViewModel", "FirebaseAuth init error", e)
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(application)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            android.util.Log.e("FinanceViewModel", "FirebaseFirestore init error", e)
            null
        }
    }

    val isPremium: StateFlow<Boolean> = billingManager.isPremium
    val billingMessage: StateFlow<String?> = billingManager.message

    fun launchPremiumPurchase(activity: Activity) {
        billingManager.launchPremiumPurchase(activity)
    }

    fun clearBillingMessage() {
        billingManager.clearMessage()
    }

    private val repository: FinanceRepository
    private val preferencesManager: PreferencesManager
    private val database: AppDatabase
    private val advisorToolRegistry: com.example.ai.AdvisorToolRegistry

    val userSettings: StateFlow<UserSettings>
    private val firestoreSyncManager: FirestoreSyncManager

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud

    private val _cloudSyncResult = MutableStateFlow<CloudSyncResult?>(null)
    val cloudSyncResult: StateFlow<CloudSyncResult?> = _cloudSyncResult

    // Read-only. The vault id IS the Firebase Auth uid — it is never user-settable, because a
    // settable vault id let any signed-in user point the restore path at another user's vault.
    private val _cloudVaultId = MutableStateFlow("")
    val cloudVaultId: StateFlow<String> = _cloudVaultId

    private val _lastCloudSyncTime = MutableStateFlow(0L)
    val lastCloudSyncTime: StateFlow<Long> = _lastCloudSyncTime

    // AUTH & QUICK 4-PIN LOCK STATES
    private val _isLoggedIn = MutableStateFlow(firebaseAuth?.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userEmail: MutableStateFlow<String> = MutableStateFlow(firebaseAuth?.currentUser?.email.orEmpty())
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError
    val userEmail: StateFlow<String> = _userEmail

    private val _isPinEnabled = MutableStateFlow(false)
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled

    // The plaintext `quickPin` StateFlow was removed. It held the PIN in memory for the whole
    // ViewModel lifetime and exposed it to any composable that collected it — the UI never needs
    // the PIN's value, only whether one is set.

    private val _isPinLocked = MutableStateFlow(false)
    val isPinLocked: StateFlow<Boolean> = _isPinLocked

    init {
        database = AppDatabase.getDatabase(application, viewModelScope)
        repository = FinanceRepository(database.financeDao())
        advisorToolRegistry = com.example.ai.AdvisorToolRegistry(database.financeDao())
        preferencesManager = PreferencesManager(application)
        userSettings = preferencesManager.settings
        firestoreSyncManager = FirestoreSyncManager(application)
        // Vault id is derived from the authenticated uid by the auth listener below, never from
        // a locally generated/persisted value.
        _cloudVaultId.value = firebaseAuth?.currentUser?.uid.orEmpty()
        _lastCloudSyncTime.value = preferencesManager.getLastCloudSyncTime()

        // Firebase Auth is the source of truth for login state.
        firebaseAuth?.addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            _isLoggedIn.value = currentUser != null
            _userEmail.value = currentUser?.email.orEmpty()
            if (currentUser != null) {
                val lastUid = preferencesManager.getLastSignedInUid()
                val isAccountSwitch = lastUid.isNotBlank() && lastUid != currentUser.uid

                _cloudVaultId.value = currentUser.uid
                preferencesManager.setLastSignedInUid(currentUser.uid)

                viewModelScope.launch {
                    if (isAccountSwitch) {
                        // A different account was last active on this device.
                        // Its local data must not leak into this account's session.
                        clearLocalVaultTables()
                    }

                    // Auto-seed demo data for the single designated demo/review account.
                    // Runs on first-ever login on this device, or again after switching
                    // back into this account from a different one (local was just wiped).
                    if (currentUser.email.equals(DEMO_SEED_EMAIL, ignoreCase = true) &&
                        (!preferencesManager.hasAutoSeededDemoData() || isAccountSwitch)
                    ) {
                        preferencesManager.setAutoSeededDemoData(true)
                        val targetRegion = userSettings.value.region
                        AppDatabase.reseedDatabaseForRegion(database.financeDao(), targetRegion)
                    }
                }
            }
        }

        // Firebase Auth is authoritative. Do not restore the old local-only login state.
        _isPinEnabled.value = preferencesManager.isPinEnabled()
        _isPinLocked.value = preferencesManager.isPinEnabled() && preferencesManager.isLoggedIn()

        // Catch up any recurring SIP debits that have come due since this ViewModel was last
        // alive (the background worker in SipDebitScheduler covers gaps while the app is
        // closed; this covers the moment the app is opened, without waiting on WorkManager).
        viewModelScope.launch {
            val debited = com.example.alerts.SipDebitEngine.processDueDebits(database.financeDao())
            if (debited > 0) {
                syncVaultToCloud()
            }
        }
    }

    /**
     * Wipes every local vault table. Shared by the account-switch path in the auth-state
     * listener above and by [registerUser] below, which awaits this directly rather than relying
     * on the listener's own (differently-timed) coroutine — see the comment in registerUser for
     * why that race mattered.
     */
    private suspend fun clearLocalVaultTables() {
        database.financeDao().clearAllTransactions()
        database.financeDao().clearAllHoldings()
        database.financeDao().clearAllSips()
        database.financeDao().clearAllCreditCards()
        database.financeDao().clearAllLoans()
        database.financeDao().clearAllGoals()
            database.financeDao().clearAllSnapshots()
            database.financeDao().clearAllBudgets()
            resetPerAccountLocalState()
    }

    // AUTH & PIN ACTIONS
    fun clearAuthError() { _authError.value = null }

    fun loginWithEmail(email: String, pass: String, onResult: (Boolean) -> Unit = {}) {
        _authError.value = null
        val auth = firebaseAuth
        if (auth == null) {
            _authError.value = "Firebase Authentication is unavailable."
            onResult(false)
            return
        }
        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        _cloudVaultId.value = uid
                        _isLoggedIn.value = true
                        _userEmail.value = email.trim()
                        viewModelScope.launch {
                            firestore?.collection("users")?.document(uid)?.set(mapOf("email" to email.trim(), "uid" to uid), com.google.firebase.firestore.SetOptions.merge())
                            restoreVaultFromCloud()
                        }
                    }
                    onResult(true)
                } else {
                    _authError.value = task.exception?.localizedMessage ?: "Login failed."
                    onResult(false)
                }
            }
    }

    fun registerUser(email: String, pass: String, onResult: (Boolean) -> Unit = {}) {
        _authError.value = null
        val auth = firebaseAuth
        if (auth == null) {
            _authError.value = "Firebase Authentication is unavailable."
            onResult(false)
            return
        }
        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        _cloudVaultId.value = user.uid
                        _isLoggedIn.value = true
                        _userEmail.value = email.trim()
                        viewModelScope.launch {
                            // A brand-new account must start from an empty vault. The
                            // auth-state listener above also clears local data on an account
                            // switch, but it runs in its own, separately-scheduled coroutine —
                            // there's no guarantee it finishes before this one. If it hadn't,
                            // syncVaultToCloud() below would push whatever the *previous*
                            // account left in the local database straight into this brand-new
                            // user's cloud vault. Clearing here directly removes that race.
                            clearLocalVaultTables()
                            firestore?.collection("users")?.document(user.uid)?.set(mapOf("email" to email.trim(), "uid" to user.uid, "createdAt" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
                            syncVaultToCloud()
                        }
                    }
                    onResult(true)
                } else {
                    _authError.value = task.exception?.localizedMessage ?: "Registration failed."
                    onResult(false)
                }
            }
    }

    fun resetPassword(email: String, newPass: String, onResult: (Boolean) -> Unit = {}) {
        _authError.value = null
        val auth = firebaseAuth
        if (auth == null) {
            _authError.value = "Firebase Authentication is unavailable."
            onResult(false)
            return
        }
        auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener { task ->
            if (!task.isSuccessful) _authError.value = task.exception?.localizedMessage ?: "Password reset failed."
            onResult(task.isSuccessful)
        }
    }

    /**
     * Verifies the PIN against the stored hash and unlocks on success.
     *
     * Suspends because PBKDF2 derivation is deliberately slow and must not run on the main
     * thread. The old version compared plaintext with `==` and returned a bare Boolean, so the
     * UI had no way to show attempts remaining or a lockout.
     */
    suspend fun verifyAndUnlockPin(pinInput: String): PinCredentialStore.Verification {
        val result = preferencesManager.pinStore.verify(pinInput)
        if (result is PinCredentialStore.Verification.Success) {
            _isPinLocked.value = false
        }
        return result
    }

    /** Stores a new PIN as a salted hash. Returns false when the PIN is not 4 digits. */
    suspend fun saveQuickPin(pin: String): Boolean {
        if (!preferencesManager.pinStore.setPin(pin)) return false
        preferencesManager.setPinEnabled(true)
        _isPinEnabled.value = true
        _isPinLocked.value = false
        return true
    }

    fun disableQuickPin() {
        preferencesManager.pinStore.clear()
        preferencesManager.setPinEnabled(false)
        _isPinEnabled.value = false
        _isPinLocked.value = false
    }

    /** Milliseconds left on an active PIN lockout; 0 when not locked out. */
    fun pinLockoutRemainingMillis(): Long = preferencesManager.pinStore.lockoutRemainingMillis()

    fun logout() {
        firebaseAuth?.signOut()
        preferencesManager.setLoggedIn(false)
        _isLoggedIn.value = false
        _isPinLocked.value = false
    }

    fun openPinLock() {
        if (_isPinEnabled.value) {
            _isPinLocked.value = true
        }
    }

    fun onAppBackgrounded() {
        if (_isLoggedIn.value) {
            if (_isPinEnabled.value) {
                _isPinLocked.value = true
            } else {
                _isLoggedIn.value = false
                preferencesManager.setLoggedIn(false)
            }
        }
    }

    fun getFirestoreConnectionStatus(): FirestoreConnectionStatus {
        return firestoreSyncManager.checkStatus()
    }

    fun syncVaultToCloud(onComplete: (CloudSyncResult) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val pendingDeletions = preferencesManager.getPendingCloudDeletions()
            val res = firestoreSyncManager.syncLocalVaultToCloud(
                dao = database.financeDao(),
                settings = userSettings.value,
                startingBalance = _startingBalance.value,
                startingBalancePromptDone = _startingBalancePromptDone.value,
                pendingDeletions = pendingDeletions
            )
            if (res.success) {
                preferencesManager.removePendingCloudDeletions(pendingDeletions)
                val now = System.currentTimeMillis()
                preferencesManager.setLastCloudSyncTime(now)
                _lastCloudSyncTime.value = now
            }
            _cloudSyncResult.value = res
            _isSyncingCloud.value = false
            onComplete(res)
        }
    }

    fun restoreVaultFromCloud(onComplete: (CloudSyncResult) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val res = firestoreSyncManager.restoreCloudVaultToLocal(
                dao = database.financeDao(),
                preferencesManager = preferencesManager
            )
            if (res.success) {
                // Restore may have replaced the saved starting balance; refresh what the UI observes.
                _startingBalance.value = preferencesManager.getStartingBalance()
                _startingBalancePromptDone.value = preferencesManager.isStartingBalancePromptDone()
                val now = System.currentTimeMillis()
                preferencesManager.setLastCloudSyncTime(now)
                _lastCloudSyncTime.value = now
            }
            _cloudSyncResult.value = res
            _isSyncingCloud.value = false
            onComplete(res)
        }
    }

    fun clearCloudSyncResult() {
        _cloudSyncResult.value = null
    }

    /** Permanently clears local finance records. Cloud records require a separate cloud deletion flow. */
    fun clearAllFinancialData() {
        viewModelScope.launch {
            database.financeDao().clearAllTransactions()
            database.financeDao().clearAllHoldings()
            database.financeDao().clearAllSips()
            database.financeDao().clearAllCreditCards()
            database.financeDao().clearAllLoans()
            database.financeDao().clearAllGoals()
            database.financeDao().clearAllSnapshots()
            database.financeDao().clearAllBudgets()
            resetPerAccountLocalState()

            // Safety net: the shared demo/tester account must never end up blank,
            // even if this function is somehow reached for it. Immediately re-seed.
            if (_userEmail.value.equals(DEMO_SEED_EMAIL, ignoreCase = true)) {
                AppDatabase.reseedDatabaseForRegion(database.financeDao(), userSettings.value.region)
            }
        }
    }

    fun reseedWithRegionData(region: GeographicRegion = userSettings.value.region) {
        viewModelScope.launch {
            preferencesManager.applyRegionPreset(region)
            AppDatabase.reseedDatabaseForRegion(database.financeDao(), region)
            validateSelectedTab()
        }
    }

    fun formatAmount(amount: Double, forceVisible: Boolean = false): String {
        return userSettings.value.formatAmount(amount, forceVisible)
    }

    fun formatCompact(amount: Double, forceVisible: Boolean = false): String {
        return userSettings.value.formatCompact(amount, forceVisible)
    }

    fun updateUserSettings(newSettings: UserSettings) {
        preferencesManager.updateSettings(newSettings)
        validateSelectedTab()
    }

    fun applyRegionPreset(region: GeographicRegion) {
        preferencesManager.applyRegionPreset(region)
        viewModelScope.launch {
            AppDatabase.reseedDatabaseForRegion(database.financeDao(), region)
            validateSelectedTab()
        }
    }

    fun updateCurrency(currency: SupportedCurrency) {
        preferencesManager.updateCurrency(currency)
    }

    fun toggleHideBalances() {
        preferencesManager.toggleHideBalances()
    }

    fun setThemeMode(mode: com.example.data.models.ThemeMode) {
        preferencesManager.updateSettings(userSettings.value.copy(themeMode = mode))
    }

    fun setAccentColor(color: com.example.data.models.AccentColor) {
        preferencesManager.updateSettings(userSettings.value.copy(accentColor = color))
    }

    fun toggleModule(module: String, enabled: Boolean) {
        preferencesManager.toggleModule(module, enabled)
        validateSelectedTab()
    }

    fun toggleNotificationOption(type: String, enabled: Boolean) {
        val current = userSettings.value
        val updated = when (type) {
            "all" -> current.copy(enableNotifications = enabled)
            "bills" -> current.copy(enableBillDueReminders = enabled)
            "sips" -> current.copy(enableSipReminders = enabled)
            "briefing" -> current.copy(enableDailyBriefingReminders = enabled)
            else -> current
        }
        preferencesManager.updateSettings(updated)
        if (type == "all") {
            if (enabled) {
                NotificationReminderManager.schedulePeriodicAlerts(getApplication())
            } else {
                NotificationReminderManager.cancelAll(getApplication())
            }
        }
    }

    fun triggerTestNotification() {
        NotificationReminderManager.sendTestReminder(getApplication(), userSettings.value.currency.symbol)
    }

    private fun validateSelectedTab() {
        val s = userSettings.value
        val current = _selectedTab.value
        val isCurrentValid = when (current) {
            FinanceTab.OVERVIEW -> true
            FinanceTab.CASH_FLOW -> s.enableCashFlow
            FinanceTab.INVEST -> s.enableInvestments
            FinanceTab.DEBT -> s.enableDebtCenter
            FinanceTab.GOALS_REPORTS -> s.enableGoals
        }
        if (!isCurrentValid) {
            _selectedTab.value = FinanceTab.OVERVIEW
        }
    }

    // Tab Navigation
    private val _selectedTab = MutableStateFlow(FinanceTab.OVERVIEW)
    val selectedTab: StateFlow<FinanceTab> = _selectedTab

    fun selectTab(tab: FinanceTab) {
        _selectedTab.value = tab
    }

    // Timeframe
    private val _selectedTimeFrame = MutableStateFlow(TimeFrame.SIX_MONTHS)
    val selectedTimeFrame: StateFlow<TimeFrame> = _selectedTimeFrame

    fun selectTimeFrame(tf: TimeFrame) {
        _selectedTimeFrame.value = tf
    }

    // Payoff Strategy
    private val _payoffStrategy = MutableStateFlow(PayoffStrategy.AVALANCHE)
    val payoffStrategy: StateFlow<PayoffStrategy> = _payoffStrategy

    fun setPayoffStrategy(strategy: PayoffStrategy) {
        _payoffStrategy.value = strategy
    }

    // Flows from DB
    val transactions = repository.allTransactions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val holdings = repository.allHoldings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val sips = repository.allSips.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val creditCards = repository.allCreditCards.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val loans = repository.allLoans.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val goals = repository.allGoals.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Summary calculation
    // Cash the user had before their first recorded transaction. Entered by the user; 0 until then.
    private val _startingBalance = MutableStateFlow(preferencesManager.getStartingBalance())
    val startingBalance: StateFlow<Double> = _startingBalance.asStateFlow()
    private val _startingBalancePromptDone = MutableStateFlow(preferencesManager.isStartingBalancePromptDone())
    val startingBalancePromptDone: StateFlow<Boolean> = _startingBalancePromptDone.asStateFlow()

    private fun currentMonthStart(): Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val _selectedMonthStart = MutableStateFlow(currentMonthStart())

    val summary: StateFlow<FinanceSummary> = combine(
        transactions, combine(holdings, sips, _startingBalance) { h, sp, sb -> Triple(h, sp, sb) }, creditCards, loans, goals
    ) { txList, holdingsAndSips, cList, lList, gList ->
        val hList = holdingsAndSips.first
        val sList = holdingsAndSips.second
        val startBalance = holdingsAndSips.third
        // No demo-value substitution. A zero total means the user has recorded nothing, and that
        // is what the UI and the advisor must both be told.
        val confirmedTransactions = txList.filter { it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" }
        val inflow = confirmedTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val outflow = confirmedTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netCash = inflow - outflow
        // Savings rate and DTI use the last 30 days, not all-time totals, so they mean the same
        // thing in month 1 and month 24. Money moved into investments is saved, not spent, and
        // sale proceeds are not earnings, so investment transactions are left out of the rate.
        val windowStart = System.currentTimeMillis() - 30L * 86_400_000L
        val recentTx = confirmedTransactions.filter { it.dateMillis >= windowStart }
        val recentIn = recentTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val recentOut = recentTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val operatingIn = recentTx
            .filter { it.type == TransactionType.INCOME && it.category !in nonSpendingCategories }.sumOf { it.amount }
        val operatingOut = recentTx
            .filter { it.type == TransactionType.EXPENSE && it.category !in nonSpendingCategories }.sumOf { it.amount }
        val savingsRate = if (operatingIn > 0) ((operatingIn - operatingOut) / operatingIn) * 100.0 else 0.0

        // SIPs count toward the portfolio at the amount invested so far (both value and cost).
        val sipInvested = sList.sumOf { it.totalInvested }
        val portVal = hList.sumOf { it.totalValue } + sipInvested
        val portCost = hList.sumOf { it.totalCost } + sipInvested
        val dayGain = hList.sumOf { it.totalValue * (it.dailyChangePercent / 100.0) }
        val dayGainPct = if (portVal > 0) (dayGain / portVal) * 100.0 else 0.0
        // Simple return on cost basis. Not XIRR — contribution dates are not recorded.
        val portReturnPct = if (portCost > 0) ((portVal - portCost) / portCost) * 100.0 else 0.0

        val cardDebt = cList.sumOf { it.currentBalance }
        val loanDebt = lList.sumOf { it.remainingBalance }
        val totalDebtVal = cardDebt + loanDebt
        val monthlyDebt = cList.sumOf { it.currentBalance * 0.03 } + lList.sumOf { it.emiAmount }
        val dti = if (recentIn > 0) (monthlyDebt / recentIn) * 100.0 else 0.0

        val goalsSum = gList.sumOf { it.currentAmount }
        // Liquid cash was previously a hardcoded 850,000. Until a cash-account entity exists,
        // net cash retained from recorded transactions is the only defensible figure.
        // The user's starting cash plus everything recorded since. Shown as-is, even if negative.
        val liquidCash = startBalance + netCash
        val totalAssetsVal = portVal + goalsSum + liquidCash
        val totalLiabilitiesVal = totalDebtVal
        val netWorthVal = totalAssetsVal - totalLiabilitiesVal

        FinanceSummary(
            totalNetWorth = netWorthVal,
            totalAssets = totalAssetsVal,
            totalLiabilities = totalLiabilitiesVal,
            liquidCash = liquidCash,
            goalsValue = goalsSum,
            totalInflow = inflow,
            totalOutflow = outflow,
            netCashRetained = netCash,
            recentInflow = recentIn,
            recentOutflow = recentOut,
            savingsRate = savingsRate,
            portfolioValue = portVal,
            portfolioCost = portCost,
            portfolioDayGain = dayGain,
            portfolioDayGainPercent = dayGainPct,
            portfolioReturnPercent = portReturnPct,
            totalDebt = totalDebtVal,
            monthlyDebtServicing = monthlyDebt,
            dtiRatio = dti,
            transactionCount = confirmedTransactions.size,
            holdingCount = hList.size + sList.size,
            debtAccountCount = cList.size + lList.size,
            goalCount = gList.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceSummary())

    /** One calendar month of cash flow with opening and closing balances. */
    val monthCashFlow: StateFlow<MonthCashFlow> = combine(
        transactions, _selectedMonthStart, _startingBalance
    ) { txList, monthStart, startBalance ->
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = monthStart; add(java.util.Calendar.MONTH, 1) }
        val monthEnd = cal.timeInMillis
        val confirmed = txList.filter { it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" }
        fun signed(t: TransactionEntity) = if (t.type == TransactionType.INCOME) t.amount else -t.amount
        val opening = startBalance + confirmed.filter { it.dateMillis < monthStart }.sumOf { signed(it) }
        val inMonth = confirmed.filter { it.dateMillis >= monthStart && it.dateMillis < monthEnd }
        val inflow = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val outflow = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val invested = inMonth.filter { it.type == TransactionType.EXPENSE && it.category in nonSpendingCategories }.sumOf { it.amount }
        MonthCashFlow(
            monthStart = monthStart,
            monthEnd = monthEnd,
            label = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(monthStart)),
            opening = opening,
            inflow = inflow,
            outflow = outflow,
            invested = invested,
            closing = opening + inflow - outflow,
            isCurrentMonth = monthStart == currentMonthStart()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthCashFlow())

    /** Moves the Cash Flow view by whole months; the future is not selectable. */
    fun shiftMonth(delta: Int) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = _selectedMonthStart.value; add(java.util.Calendar.MONTH, delta) }
        _selectedMonthStart.value = minOf(cal.timeInMillis, currentMonthStart())
    }

    /**
     * Sets the cash the user has right now. The starting balance is derived from it, so that
     * today's balance equals what they entered and everything recorded from here adds to it.
     */
    fun setCurrentCashBalance(current: Double) {
        viewModelScope.launch {
            val net = repository.allTransactions.first()
                .filter { it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" }
                .sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
            val start = current - net
            preferencesManager.setStartingBalance(start)
            preferencesManager.setStartingBalancePromptDone(true)
            _startingBalance.value = start
            _startingBalancePromptDone.value = true
            syncVaultToCloud()
        }
    }

    fun dismissStartingBalancePrompt() {
        preferencesManager.setStartingBalancePromptDone(true)
        _startingBalancePromptDone.value = true
        syncVaultToCloud()
    }

    /** Called whenever local data is wiped so one account's balance and pending cloud deletions never carry into another. */
    private fun resetPerAccountLocalState() {
        preferencesManager.clearPendingCloudDeletions()
        preferencesManager.setStartingBalance(0.0)
        preferencesManager.setStartingBalancePromptDone(false)
        _startingBalance.value = 0.0
        _startingBalancePromptDone.value = false
    }

    val budgets: StateFlow<List<com.example.data.models.BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBudget(category: Category, monthlyLimit: Double) {
        viewModelScope.launch {
            repository.saveBudget(com.example.data.models.BudgetEntity(category, monthlyLimit))
            syncVaultToCloud()
        }
    }

    fun deleteBudget(budget: com.example.data.models.BudgetEntity) {
        viewModelScope.launch {
            queueCloudDeletion("budgets", budget.category.name)
            repository.deleteBudget(budget)
            syncVaultToCloud()
        }
    }

    val snapshots: StateFlow<List<com.example.data.models.NetWorthSnapshotEntity>> = repository.allSnapshots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Keep one net-worth snapshot per day (the latest value that day) for the trend and MoM change.
        viewModelScope.launch {
            summary.collect { s ->
                if (s.totalAssets == 0.0 && s.totalLiabilities == 0.0) return@collect
                val now = System.currentTimeMillis()
                repository.recordSnapshot(
                    com.example.data.models.NetWorthSnapshotEntity(
                        dayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(now)),
                        netWorth = s.totalNetWorth,
                        assets = s.totalAssets,
                        liabilities = s.totalLiabilities,
                        dateMillis = now,
                        investments = s.portfolioValue
                    )
                )
            }
        }
    }

    // AI Chat State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "AI",
                text = "Welcome to Obsidian Financial Intelligence. I have synchronized your portfolios, credit utilization, and recurring cash flows. How can I optimize your capital allocation today?"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking

    fun askAi(question: String) {
        if (question.isBlank()) return
        val userMsg = ChatMessage(sender = "USER", text = question)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            // The advisor receives a structured snapshot of real recorded values only.
            // The previous free-text context embedded a fixed "XIRR: 16.8%" and a fixed
            // "Health Score: 91/100" — neither was derived from the user's data.
            val s = summary.value
            val settings = userSettings.value
            val snapshot = AdvisorSnapshot(
                currencySymbol = settings.currency.symbol,
                regionTitle = settings.region.title,
                fiscalCalendar = settings.fiscalCalendar.title,
                riskProfile = settings.aiRiskProfile.title,
                netWorth = s.totalNetWorth,
                totalAssets = s.totalAssets,
                totalLiabilities = s.totalLiabilities,
                monthlyInflow = s.recentInflow,
                monthlyOutflow = s.recentOutflow,
                savingsRatePercent = s.savingsRate,
                portfolioValue = s.portfolioValue,
                portfolioCost = s.portfolioCost,
                totalDebt = s.totalDebt,
                monthlyDebtServicing = s.monthlyDebtServicing,
                dtiPercent = s.dtiRatio,
                transactionCount = s.transactionCount,
                holdingCount = s.holdingCount,
                debtAccountCount = s.debtAccountCount,
                goalCount = s.goalCount
            )

            val aiResponseText = GeminiClient.generateFinancialAdvice(question, snapshot, advisorToolRegistry)
            _isAiThinking.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage(sender = "AI", text = aiResponseText)
        }
    }

    fun analyzeReceipt(bitmap: android.graphics.Bitmap, onResult: (com.example.ai.ParsedTransaction) -> Unit) {
        viewModelScope.launch {
            val result = GeminiClient.analyzeReceiptImage(bitmap)
            onResult(result)
        }
    }

    suspend fun parseStatement(rawText: String): List<com.example.data.util.ParsedTransaction> {
        val parsed = com.example.data.util.StatementParser.parseStatementText(rawText)
        return parsed.map { tx ->
            val exists = repository.isFingerprintImported(tx.fingerprint)
            tx.copy(isDuplicate = exists)
        }
    }

    fun addImportedTransactions(transactions: List<com.example.data.util.ParsedTransaction>) {
        viewModelScope.launch {
            transactions.filter { !it.isDuplicate && !it.dateAmbiguous && it.dateMillis > 0L }.forEach { tx ->
                if (!repository.isFingerprintImported(tx.fingerprint)) {
                    repository.addTransaction(
                        TransactionEntity(
                            title = tx.title,
                            amount = tx.amount,
                            type = tx.type,
                            category = tx.category,
                            account = tx.account,
                            dateMillis = tx.dateMillis,
                            statementFingerprint = tx.fingerprint,
                            note = "Imported Statement [FP: ${tx.fingerprint.take(8)}]"
                        )
                    )
                }
            }
            syncVaultToCloud()
        }
    }

    // CRUD Actions
    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        account: String,
        dateMillis: Long = System.currentTimeMillis(),
        note: String = "",
        statementFingerprint: String? = null
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    account = account,
                    dateMillis = dateMillis,
                    note = note,
                    statementFingerprint = statementFingerprint
                )
            )
            syncVaultToCloud()
        }
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
            syncVaultToCloud()
        }
    }

    fun confirmImportedTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateImportStatus(transaction.id, "CONFIRMED")
            syncVaultToCloud()
        }
    }

    fun ignoreImportedTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateImportStatus(transaction.id, "IGNORED")
        }
    }

    fun parseNaturalTransaction(text: String, onResult: (com.example.ai.ParsedTransaction?) -> Unit) {
        viewModelScope.launch {
            val parsed = GeminiClient.parseNaturalLanguageTransaction(text)
            onResult(parsed)
        }
    }

    fun parseAndAddNaturalTransaction(text: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val parsed = GeminiClient.parseNaturalLanguageTransaction(text)
            if (parsed != null && parsed.title.isNotBlank() && parsed.amount > 0.0) {
                val cat = try {
                    Category.valueOf(parsed.category)
                } catch (e: Exception) {
                    Category.OTHER
                }
                val tType = if (parsed.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                repository.addTransaction(
                    TransactionEntity(
                        title = parsed.title,
                        amount = parsed.amount,
                        type = tType,
                        category = cat,
                        account = parsed.account,
                        dateMillis = parsed.dateMillis ?: System.currentTimeMillis()
                    )
                )
                syncVaultToCloud()
            }
            onDone()
        }
    }

    /** Writes transactions, holdings and SIPs to CSV files and opens the share sheet. */
    fun exportCsv() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val dir = java.io.File(app.cacheDir, "exports").apply { mkdirs() }
                // Quote every text cell; a leading = + - @ is neutralised so spreadsheets don't run it as a formula.
                fun cell(v: String): String {
                    val safe = if (v.isNotEmpty() && v[0] in "=+-@") "'" + v else v
                    return "\"" + safe.replace("\"", "\"\"") + "\""
                }
                val day = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

                val txFile = java.io.File(dir, "obsidian_transactions.csv")
                txFile.writeText(buildString {
                    appendLine("date,title,type,category,account,amount,note")
                    repository.allTransactions.first()
                        .filter { it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" }
                        .forEach { t ->
                            appendLine(listOf(day.format(java.util.Date(t.dateMillis)), cell(t.title), t.type.name,
                                t.category.name, cell(t.account), t.amount.toString(), cell(t.note)).joinToString(","))
                        }
                })
                val holdingsFile = java.io.File(dir, "obsidian_holdings.csv")
                holdingsFile.writeText(buildString {
                    appendLine("symbol,name,type,shares,avg_buy_price,current_price,value")
                    repository.allHoldings.first().forEach { h ->
                        appendLine(listOf(cell(h.symbol), cell(h.name), h.type.name, h.shares.toString(),
                            h.avgBuyPrice.toString(), h.currentPrice.toString(), h.totalValue.toString()).joinToString(","))
                    }
                })
                val sipsFile = java.io.File(dir, "obsidian_sips.csv")
                sipsFile.writeText(buildString {
                    appendLine("fund,category,monthly_amount,debit_day,active,total_invested")
                    repository.allSips.first().forEach { p ->
                        appendLine(listOf(cell(p.fundName), cell(p.category), p.monthlyAmount.toString(),
                            p.debitDayOfMonth.toString(), p.isActive.toString(), p.totalInvested.toString()).joinToString(","))
                    }
                })

                val uris = ArrayList(listOf(txFile, holdingsFile, sipsFile).map {
                    androidx.core.content.FileProvider.getUriForFile(app, app.packageName + ".fileprovider", it)
                })
                val send = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "text/csv"
                    putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                app.startActivity(
                    android.content.Intent.createChooser(send, "Export Obsidian data")
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "CSV export failed", e)
            }
        }
    }

    /** Remembers that a document was deleted locally so the next sync removes it from the cloud too. */
    private fun queueCloudDeletion(collection: String, id: Any) {
        preferencesManager.addPendingCloudDeletions(listOf("$collection/$id"))
    }

    /** Queues cloud deletion for every ledger entry created by [reference] (e.g. "holding:7"). */
    private suspend fun queueCloudDeletionsForReference(reference: String) {
        val ids = repository.allTransactions.first()
            .filter { it.sourceReference == reference }
            .map { "transactions/${it.id}" }
        preferencesManager.addPendingCloudDeletions(ids)
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            queueCloudDeletion("transactions", transaction.id)
            repository.deleteTransaction(transaction)
            syncVaultToCloud()
        }
    }

    fun addHolding(
        symbol: String,
        name: String,
        type: HoldingType,
        shares: Double,
        avgBuyPrice: Double,
        currentPrice: Double
    ) {
        viewModelScope.launch {
            val holdingId = repository.addHolding(
                HoldingEntity(
                    symbol = symbol.uppercase(),
                    name = name,
                    type = type,
                    shares = shares,
                    avgBuyPrice = avgBuyPrice,
                    currentPrice = currentPrice,
                    dailyChangePercent = 0.0 // no live price feed: day change is unknown until prices are edited
                )
            )
            // Buying an investment spends cash: record the purchase cost as an expense so it
            // shows on the Cash Flow tab and reduces available cash.
            val purchaseCost = shares * avgBuyPrice
            if (purchaseCost > 0) {
                repository.addTransaction(
                    TransactionEntity(
                        title = "Investment: ${symbol.uppercase()}",
                        amount = purchaseCost,
                        type = TransactionType.EXPENSE,
                        category = Category.INVESTMENT_SIP,
                        account = "Cash / selected account",
                        note = "Purchase of $shares x ${symbol.uppercase()} recorded in Obsidian Wealth",
                        sourceReference = "holding:$holdingId"
                    )
                )
            }
            syncVaultToCloud()
        }
    }

    fun updateHolding(holding: HoldingEntity) {
        viewModelScope.launch {
            repository.updateHolding(holding)
            syncVaultToCloud()
        }
    }

    /**
     * Sells [sharesToSell] units at [salePrice]: reduces (or removes) the holding and records the
     * proceeds as income on Cash Flow. Realised gain is shown in the note.
     */
    fun sellHolding(holding: HoldingEntity, sharesToSell: Double, salePrice: Double) {
        viewModelScope.launch {
            val qty = sharesToSell.coerceAtMost(holding.shares)
            if (qty <= 0.0 || salePrice < 0.0) return@launch
            val remaining = holding.shares - qty
            if (remaining <= 1e-9) {
                queueCloudDeletion("holdings", holding.id)
                repository.deleteHolding(holding)
            } else {
                repository.updateHolding(holding.copy(shares = remaining, currentPrice = salePrice))
            }
            val proceeds = qty * salePrice
            if (proceeds > 0) {
                val gain = qty * (salePrice - holding.avgBuyPrice)
                repository.addTransaction(
                    TransactionEntity(
                        title = "Sold: ${holding.symbol}",
                        amount = proceeds,
                        type = TransactionType.INCOME,
                        category = Category.INVESTMENT_SIP,
                        account = "Cash / selected account",
                        note = "Sale of $qty x ${holding.symbol}; realised ${if (gain >= 0) "gain" else "loss"} ${"%.2f".format(gain)}",
                        sourceReference = "holdingsale:${holding.id}"
                    )
                )
            }
            syncVaultToCloud()
        }
    }

    /** Deleting a holding undoes its entry: the purchase expense it created is removed too. */
    fun deleteHolding(holding: HoldingEntity) {
        viewModelScope.launch {
            queueCloudDeletionsForReference("holding:${holding.id}")
            queueCloudDeletion("holdings", holding.id)
            repository.deleteTransactionsBySourceReference("holding:${holding.id}")
            repository.deleteHolding(holding)
            syncVaultToCloud()
        }
    }

    fun addSip(
        fundName: String,
        category: String,
        monthlyAmount: Double,
        debitDayOfMonth: Int,
        annualizedReturnPercent: Double
    ) {
        viewModelScope.launch {
            val sipId = repository.addSip(
                SipEntity(
                    fundName = fundName,
                    category = category,
                    monthlyAmount = monthlyAmount,
                    debitDayOfMonth = debitDayOfMonth,
                    isActive = true,
                    // A brand-new SIP has had exactly one debit so far: the one that starts it.
                    // (Previously this was `monthlyAmount * 6`, pre-aging every new SIP by six
                    // months it never actually had.)
                    totalInvested = monthlyAmount,
                    annualizedReturnPercent = annualizedReturnPercent,
                    // First debit is recorded below, so mark this month as already charged.
                    lastDebitedYearMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
                        .format(java.util.Date())
                )
            )
            // The SIP's first debit happens immediately, so record it as a cash outflow
            // right away — it should hit the Cash Flow tab and reduce available cash,
            // just like any other expense.
            repository.addTransaction(
                TransactionEntity(
                    title = "SIP: $fundName",
                    amount = monthlyAmount,
                    type = TransactionType.EXPENSE,
                    category = Category.INVESTMENT_SIP,
                    account = "Cash / selected account",
                    note = "Automated SIP investment recorded in Obsidian Wealth",
                    isRecurring = true,
                    sourceReference = "sip:$sipId"
                )
            )
            syncVaultToCloud()
        }
    }

    fun updateSip(sip: SipEntity) {
        viewModelScope.launch {
            repository.updateSip(sip)
            syncVaultToCloud()
        }
    }

    fun toggleSip(sip: SipEntity) {
        viewModelScope.launch {
            repository.toggleSipActive(sip)
            syncVaultToCloud()
        }
    }

    /** Deleting a SIP undoes it: every debit it recorded is removed. Use pause to stop a SIP but keep its history. */
    fun deleteSip(sip: SipEntity) {
        viewModelScope.launch {
            queueCloudDeletionsForReference("sip:${sip.id}")
            queueCloudDeletion("sips", sip.id)
            repository.deleteTransactionsBySourceReference("sip:${sip.id}")
            repository.deleteSip(sip)
            syncVaultToCloud()
        }
    }

    fun addCreditCard(
        cardName: String,
        balance: Double,
        limit: Double,
        apr: Double,
        dueDays: Int
    ) {
        viewModelScope.launch {
            repository.addCreditCard(
                CreditCardEntity(
                    cardName = cardName,
                    currentBalance = balance,
                    creditLimit = limit,
                    apr = apr,
                    dueDateDays = dueDays
                )
            )
            syncVaultToCloud()
        }
    }

    fun payCreditCard(card: CreditCardEntity, paymentAmount: Double) {
        viewModelScope.launch {
            repository.payCreditCard(card, paymentAmount)
            repository.addTransaction(
                TransactionEntity(
                    title = "Payment to ${card.cardName}",
                    amount = paymentAmount,
                    type = TransactionType.EXPENSE,
                    category = Category.OTHER,
                    account = "Chase Checking",
                    note = "Card debt reduction"
                )
            )
            syncVaultToCloud()
        }
    }

    fun addLoan(
        loanName: String,
        lender: String,
        totalAmount: Double,
        remainingBalance: Double,
        emi: Double,
        apr: Double,
        months: Int
    ) {
        viewModelScope.launch {
            repository.addLoan(
                LoanEntity(
                    loanName = loanName,
                    lender = lender,
                    totalAmount = totalAmount,
                    remainingBalance = remainingBalance,
                    emiAmount = emi,
                    interestRate = apr,
                    totalMonths = months,
                    remainingMonths = (months * (remainingBalance / totalAmount)).toInt().coerceAtLeast(1)
                )
            )
            syncVaultToCloud()
        }
    }

    private val loanPaymentsInFlight = mutableSetOf<Long>()

    fun payLoanEmi(loan: LoanEntity) {
        synchronized(loanPaymentsInFlight) {
            if (!loanPaymentsInFlight.add(loan.id)) return
        }
        viewModelScope.launch {
            try {
                val paymentAmount = repository.payLoanEmi(loan)
                if (paymentAmount > 0.0) {
                    repository.addTransaction(
                        TransactionEntity(
                            title = "EMI: ${loan.loanName}",
                            amount = paymentAmount,
                            type = TransactionType.EXPENSE,
                            category = Category.LOAN_EMI,
                            account = "Cash / selected account",
                            note = "Manual EMI payment recorded in Obsidian Wealth",
                            isRecurring = false
                        )
                    )
                }
                syncVaultToCloud()
            } finally {
                synchronized(loanPaymentsInFlight) { loanPaymentsInFlight.remove(loan.id) }
            }
        }
    }

    /**
     * Creates a goal. If it starts with a balance, [deductFromCash] says whether that money is still
     * in the user's cash (deduct it) or was already set aside (leave cash alone).
     */
    fun addGoal(
        title: String,
        category: String,
        targetAmount: Double,
        currentAmount: Double,
        monthlyContribution: Double,
        deductFromCash: Boolean = false
    ) {
        viewModelScope.launch {
            val goalId = repository.addGoal(
                GoalEntity(
                    title = title,
                    category = category,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    monthlyContribution = monthlyContribution
                )
            )
            if (deductFromCash && currentAmount > 0) {
                recordGoalTransfer(title, currentAmount, toGoal = true, goalId = goalId, note = "Opening balance of goal")
            }
            syncVaultToCloud()
        }
    }

    /** Moves [amount] from cash into the goal and records it as savings. */
    fun contributeGoal(goal: GoalEntity, amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.contributeToGoal(goal, amount)
            recordGoalTransfer(goal.title, amount, toGoal = true, goalId = goal.id, note = "Deposit into goal")
            syncVaultToCloud()
        }
    }

    /** Moves [amount] (at most what the goal holds) from the goal back into cash. */
    fun withdrawFromGoal(goal: GoalEntity, amount: Double) {
        viewModelScope.launch {
            val withdrawn = amount.coerceAtMost(goal.currentAmount)
            if (withdrawn <= 0.0) return@launch
            repository.contributeToGoal(goal, -withdrawn)
            recordGoalTransfer(goal.title, withdrawn, toGoal = false, goalId = goal.id, note = "Withdrawal from goal")
            syncVaultToCloud()
        }
    }

    private suspend fun recordGoalTransfer(title: String, amount: Double, toGoal: Boolean, goalId: Long, note: String) {
        repository.addTransaction(
            TransactionEntity(
                title = if (toGoal) "Goal deposit: $title" else "Goal withdrawal: $title",
                amount = amount,
                type = if (toGoal) TransactionType.EXPENSE else TransactionType.INCOME,
                category = Category.GOAL_SAVINGS,
                account = "Cash / selected account",
                note = note,
                sourceReference = "goal:$goalId"
            )
        )
    }

    companion object {
        // Only this account receives auto-seeded Kenyan demo/sample data on first login
        // (used as a shared demo login for testers/reviewers). All other accounts start blank.
        const val DEMO_SEED_EMAIL = "smarttechlab.apps@gmail.com"
    }
}
