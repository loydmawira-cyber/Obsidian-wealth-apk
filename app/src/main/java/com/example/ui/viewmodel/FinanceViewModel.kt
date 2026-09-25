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
import com.example.data.models.AccountEntity
import com.example.data.models.AccountLedger
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
import com.example.data.models.TransactionKind
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
    val goalCount: Int = 0,
    val currencyCode: String = "KES",
    val hasUnresolvedLedgerData: Boolean = false
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
    val adjustments: Double = 0.0,
    val closing: Double = 0.0,
    val isCurrentMonth: Boolean = true
)

private data class SummaryAssets(
    val holdings: List<HoldingEntity>,
    val accounts: List<AccountEntity>,
    val currencyCode: String
)

/** Categories that move money into something you own rather than spend it. */
private val investmentCategories = setOf(Category.INVESTMENT_SIP, Category.GOAL_SAVINGS)
private val nonSpendingCategories = investmentCategories + Category.DEBT_PAYMENT

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

        // Convert the old all-in-one cash baseline once. It is intentionally currency-unknown:
        // the selected display currency is not proof of the bank account's currency.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val rows = repository.accountsSnapshot()
            val legacyName = "Legacy starting balance (review)"
            if (rows.none { it.name == legacyName } && _startingBalance.value != 0.0) {
                repository.addAccount(
                    AccountEntity(
                        name = legacyName,
                        accountType = "LEGACY",
                        currencyCode = null,
                        openingBalance = _startingBalance.value,
                        openingBalanceMillis = 0L,
                        openingBalanceConfirmed = false
                    )
                )
                preferencesManager.setStartingBalance(0.0)
                _startingBalance.value = 0.0
                syncVaultToCloud()
            }
        }

        // Recurring investment plans are reminders only. Do not create ledger entries or
        // increase invested value unless an actual contribution is confirmed or imported.
    }

    /**
     * Wipes every local vault table. Shared by the account-switch path in the auth-state
     * listener above and by [registerUser] below, which awaits this directly rather than relying
     * on the listener's own (differently-timed) coroutine — see the comment in registerUser for
     * why that race mattered.
     */
    private suspend fun clearLocalVaultTables() {
        database.financeDao().clearAllTransactions()
        database.financeDao().clearAllAccounts()
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
            database.financeDao().clearAllAccounts()
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

    fun formatAmount(amount: Double, currencyCode: String?, forceVisible: Boolean = false): String {
        val settings = userSettings.value
        val currency = SupportedCurrency.values().firstOrNull { it.code == currencyCode }
        if (currency == null) {
            val numberOnly = settings.formatAmount(amount, forceVisible)
                .removePrefix(settings.currency.symbol)
                .trim()
            return "${currencyCode?.takeIf { it.isNotBlank() } ?: "Currency unknown"} $numberOnly"
        }
        return settings.copy(currency = currency, hideBalances = settings.hideBalances && !forceVisible)
            .formatAmount(amount, forceVisible)
    }

    fun currentAccountBalance(account: AccountEntity): Double =
        AccountLedger.currentBalance(account, transactions.value)

    fun addAccount(name: String, accountType: String, currencyCode: String, openingBalance: Double) {
        val cleanName = name.trim()
        if (cleanName.isBlank() || !openingBalance.isFinite() ||
            SupportedCurrency.values().none { it.code == currencyCode }) return
        viewModelScope.launch {
            repository.addAccount(
                AccountEntity(
                    name = cleanName,
                    accountType = accountType,
                    currencyCode = currencyCode,
                    openingBalance = openingBalance,
                    openingBalanceMillis = System.currentTimeMillis(),
                    openingBalanceConfirmed = true
                )
            )
            syncVaultToCloud()
        }
    }

    fun resolveAccountCurrency(account: AccountEntity, currencyCode: String) {
        if (SupportedCurrency.values().none { it.code == currencyCode }) return
        viewModelScope.launch {
            val fresh = repository.account(account.id) ?: return@launch
            repository.updateAccount(fresh.copy(currencyCode = currencyCode))
            database.financeDao().resolveTransactionCurrency(account.id, currencyCode)
            syncVaultToCloud()
        }
    }

    fun confirmAccountOpeningBalance(account: AccountEntity, openingBalance: Double) {
        if (!openingBalance.isFinite() || account.currencyCode.isNullOrBlank()) return
        viewModelScope.launch {
            val fresh = repository.account(account.id) ?: return@launch
            if (fresh.currencyCode.isNullOrBlank()) return@launch
            repository.updateAccount(fresh.copy(openingBalance = openingBalance, openingBalanceConfirmed = true))
            syncVaultToCloud()
        }
    }

    fun recordReconciliation(account: AccountEntity, statementBalance: Double) {
        if (!statementBalance.isFinite() || account.currencyCode.isNullOrBlank()) return
        viewModelScope.launch {
            val fresh = repository.account(account.id) ?: return@launch
            val ledgerBalance = AccountLedger.currentBalance(fresh, repository.allTransactions.first())
            val difference = AccountLedger.reconciliationAdjustment(ledgerBalance, statementBalance)
            if (!difference.isFinite()) return@launch
            val now = System.currentTimeMillis()
            val adjustment = if (kotlin.math.abs(difference) > 0.005) TransactionEntity(
                title = "Balance reconciliation: ${fresh.name}",
                amount = kotlin.math.abs(difference),
                type = if (difference >= 0.0) TransactionType.INCOME else TransactionType.EXPENSE,
                category = Category.ACCOUNT_ADJUSTMENT,
                account = fresh.name,
                dateMillis = now,
                note = "Reconciled to a statement balance entered by the user",
                accountId = fresh.id,
                currencyCode = fresh.currencyCode,
                transactionKind = TransactionKind.ADJUSTMENT
            ) else null
            val updated = fresh.copy(statementBalance = statementBalance, lastReconciledMillis = now)
            if (adjustment != null) repository.recordReconciliation(updated, adjustment)
            else repository.updateAccount(updated)
            syncVaultToCloud()
        }
    }

    fun recordTransfer(from: AccountEntity, to: AccountEntity, amount: Double, note: String = "") {
        if (!amount.isFinite() || amount <= 0.0 || from.id == to.id || from.currencyCode.isNullOrBlank() ||
            from.currencyCode != to.currencyCode) return
        viewModelScope.launch {
            val freshFrom = repository.account(from.id) ?: return@launch
            val freshTo = repository.account(to.id) ?: return@launch
            if (!freshFrom.isActive || !freshTo.isActive || freshFrom.currencyCode != freshTo.currencyCode) return@launch
            if (AccountLedger.currentBalance(freshFrom, repository.allTransactions.first()) + 0.000001 < amount) return@launch
            val now = System.currentTimeMillis()
            val group = java.util.UUID.randomUUID().toString()
            val recorded = repository.recordTransfer(
                TransactionEntity(
                    title = "Transfer to ${freshTo.name}", amount = amount, type = TransactionType.EXPENSE,
                    category = Category.ACCOUNT_TRANSFER, account = freshFrom.name, dateMillis = now,
                    note = note, accountId = freshFrom.id, currencyCode = freshFrom.currencyCode,
                    transactionKind = TransactionKind.TRANSFER, transferGroupId = group
                ),
                TransactionEntity(
                    title = "Transfer from ${freshFrom.name}", amount = amount, type = TransactionType.INCOME,
                    category = Category.ACCOUNT_TRANSFER, account = freshTo.name, dateMillis = now,
                    note = note, accountId = freshTo.id, currencyCode = freshTo.currencyCode,
                    transactionKind = TransactionKind.TRANSFER, transferGroupId = group
                )
            )
            if (recorded) syncVaultToCloud()
        }
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
        // Locale changes must never replace or delete user-entered financial records.
        validateSelectedTab()
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

    val accounts = repository.allAccounts.stateIn(
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

    private val summaryAssets = combine(holdings, accounts, userSettings) { h, a, settings ->
        SummaryAssets(h, a, settings.currency.code)
    }

    val summary: StateFlow<FinanceSummary> = combine(
        transactions, summaryAssets, creditCards, loans, goals
    ) { txList, assetContext, cList, lList, gList ->
        val baseCurrency = assetContext.currencyCode
        val hList = assetContext.holdings
        val aList = assetContext.accounts
        val accountById = aList.associateBy { it.id }
        fun currency(tx: TransactionEntity): String? = tx.currencyCode ?: tx.accountId?.let { accountById[it]?.currencyCode }
        val confirmed = txList.filter { it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" }
        val activeBaseAccountIds = aList.filter { it.isActive && it.currencyCode == baseCurrency }.map { it.id }.toSet()
        val selected = confirmed.filter { it.accountId in activeBaseAccountIds && currency(it) == baseCurrency }
        val operatingIncome = selected.filter {
            it.type == TransactionType.INCOME && it.transactionKind !in setOf(TransactionKind.TRANSFER, TransactionKind.ADJUSTMENT, TransactionKind.ASSET_CONVERSION)
        }
        val operatingExpense = selected.filter {
            it.type == TransactionType.EXPENSE && it.transactionKind != TransactionKind.TRANSFER && it.category !in nonSpendingCategories
        }
        val inflow = operatingIncome.sumOf { it.amount }
        val outflow = selected.filter { it.type == TransactionType.EXPENSE && it.transactionKind !in setOf(TransactionKind.TRANSFER, TransactionKind.ADJUSTMENT) }.sumOf { it.amount }
        val netCash = inflow - outflow

        val windowStart = System.currentTimeMillis() - 30L * 86_400_000L
        val recent = selected.filter { it.dateMillis >= windowStart }
        val recentIn = recent.filter { it.type == TransactionType.INCOME && it.transactionKind !in setOf(TransactionKind.TRANSFER, TransactionKind.ADJUSTMENT, TransactionKind.ASSET_CONVERSION) }.sumOf { it.amount }
        val recentOut = recent.filter { it.type == TransactionType.EXPENSE && it.transactionKind !in setOf(TransactionKind.TRANSFER, TransactionKind.ADJUSTMENT) }.sumOf { it.amount }
        val operatingIn = recent.filter { it.type == TransactionType.INCOME && it.transactionKind !in setOf(TransactionKind.TRANSFER, TransactionKind.ADJUSTMENT, TransactionKind.ASSET_CONVERSION) && it.category !in nonSpendingCategories }.sumOf { it.amount }
        val operatingOut = recent.filter { it.type == TransactionType.EXPENSE && it.transactionKind != TransactionKind.TRANSFER && it.category !in nonSpendingCategories }.sumOf { it.amount }
        val savingsRate = if (operatingIn > 0) ((operatingIn - operatingOut) / operatingIn) * 100.0 else 0.0

        val baseHoldings = hList.filter { it.currencyCode == baseCurrency }
        val portVal = baseHoldings.sumOf { it.totalValue }
        val portCost = baseHoldings.sumOf { it.totalCost }
        val dayGain = baseHoldings.sumOf { it.totalValue * (it.dailyChangePercent / 100.0) }
        val dayGainPct = if (portVal > 0) (dayGain / portVal) * 100.0 else 0.0
        val portReturnPct = if (portCost > 0) ((portVal - portCost) / portCost) * 100.0 else 0.0

        val baseCards = cList.filter { it.currencyCode == baseCurrency }
        val baseLoans = lList.filter { it.currencyCode == baseCurrency }
        val cardDebt = baseCards.sumOf { it.currentBalance }
        val loanDebt = baseLoans.sumOf { it.remainingBalance }
        val totalDebtVal = cardDebt + loanDebt
        // Card minimum-payment terms are issuer-specific and unknown here; only explicit loan EMIs are shown.
        val monthlyDebt = baseLoans.sumOf { it.emiAmount }
        val dti = if (recentIn > 0) (monthlyDebt / recentIn) * 100.0 else 0.0

        val baseGoals = gList.filter { it.currencyCode == baseCurrency }
        val goalsSum = baseGoals.sumOf { it.currentAmount }
        val liquidCash = aList.filter { it.isActive && it.currencyCode == baseCurrency }
            .sumOf { AccountLedger.currentBalance(it, confirmed) }
        // A goal is a label/target, not a separate asset. Its saved amount may already be inside a cash account.
        val totalAssetsVal = portVal + liquidCash
        val totalLiabilitiesVal = totalDebtVal

        val unresolved = aList.any { it.isActive && (it.currencyCode.isNullOrBlank() || (it.openingBalance != 0.0 && !it.openingBalanceConfirmed)) } ||
            hList.any { it.totalValue != 0.0 && it.currencyCode.isNullOrBlank() } ||
            cList.any { it.currentBalance != 0.0 && it.currencyCode.isNullOrBlank() } ||
            lList.any { it.remainingBalance != 0.0 && it.currencyCode.isNullOrBlank() } ||
            gList.any { it.currentAmount != 0.0 && it.currencyCode.isNullOrBlank() } ||
            confirmed.any { tx ->
                val accountCurrency = tx.accountId?.let { accountById[it]?.currencyCode }
                tx.amount != 0.0 && (tx.accountId == null || currency(tx).isNullOrBlank() ||
                    (!accountCurrency.isNullOrBlank() && !tx.currencyCode.isNullOrBlank() && accountCurrency != tx.currencyCode))
            }

        FinanceSummary(
            totalNetWorth = totalAssetsVal - totalLiabilitiesVal,
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
            transactionCount = selected.size,
            holdingCount = baseHoldings.size,
            debtAccountCount = baseCards.size + baseLoans.size,
            goalCount = baseGoals.size,
            currencyCode = baseCurrency,
            hasUnresolvedLedgerData = unresolved
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceSummary())

    /** One calendar month of cash flow with opening and closing balances. */
    val monthCashFlow: StateFlow<MonthCashFlow> = combine(
        transactions, accounts, _selectedMonthStart, userSettings
    ) { txList, accountList, monthStart, settings ->
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = monthStart; add(java.util.Calendar.MONTH, 1) }
        val monthEnd = cal.timeInMillis
        val confirmed = txList.filter { it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" }
        val currency = settings.currency.code
        val selectedAccounts = accountList.filter { it.isActive && it.currencyCode == currency }
        val accountIds = selectedAccounts.map { it.id }.toSet()
        val selectedTransactions = confirmed.filter { it.accountId in accountIds }
        val inMonth = selectedTransactions.filter { it.dateMillis >= monthStart && it.dateMillis < monthEnd && it.transactionKind !in setOf(TransactionKind.TRANSFER, TransactionKind.ASSET_CONVERSION) }
        val opening = selectedAccounts.sumOf { AccountLedger.balanceAt(it, selectedTransactions, monthStart) }
        val closing = selectedAccounts.sumOf { AccountLedger.balanceAt(it, selectedTransactions, monthEnd) }
        val operatingMonth = inMonth.filter { it.transactionKind != TransactionKind.ADJUSTMENT }
        val inflow = operatingMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val outflow = operatingMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val adjustments = inMonth.filter { it.transactionKind == TransactionKind.ADJUSTMENT }.sumOf(AccountLedger::signedEffect)
        val invested = inMonth.filter { it.type == TransactionType.EXPENSE && it.category in investmentCategories }.sumOf { it.amount }
        MonthCashFlow(
            monthStart = monthStart,
            monthEnd = monthEnd,
            label = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(monthStart)),
            opening = opening,
            inflow = inflow,
            outflow = outflow,
            invested = invested,
            adjustments = adjustments,
            closing = closing,
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

    fun saveBudget(category: Category, monthlyLimit: Double, currencyCode: String) {
        if (!monthlyLimit.isFinite() || monthlyLimit <= 0.0 || SupportedCurrency.values().none { it.code == currencyCode }) return
        viewModelScope.launch {
            repository.saveBudget(com.example.data.models.BudgetEntity(category, monthlyLimit, currencyCode))
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
                text = "Welcome to Obsidian's finance assistant. I can help explain the financial records you choose to share. My responses are informational and may be incomplete or incorrect."
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

    fun addImportedTransactions(transactions: List<com.example.data.util.ParsedTransaction>, account: AccountEntity) {
        if (!account.isActive || account.currencyCode.isNullOrBlank()) return
        viewModelScope.launch {
            val currentAccount = repository.account(account.id) ?: return@launch
            if (!currentAccount.isActive || currentAccount.currencyCode != account.currencyCode) return@launch
            transactions.filter { !it.isDuplicate && !it.dateAmbiguous && it.dateMillis > 0L }.forEach { tx ->
                if (!repository.isFingerprintImported(tx.fingerprint)) {
                    repository.addTransaction(
                        TransactionEntity(
                            title = tx.title,
                            amount = tx.amount,
                            type = tx.type,
                            category = tx.category,
                            account = currentAccount.name,
                            dateMillis = tx.dateMillis,
                            statementFingerprint = tx.fingerprint,
                            note = "Imported Statement [FP: ${tx.fingerprint.take(8)}]",
                            accountId = currentAccount.id,
                            currencyCode = currentAccount.currencyCode
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
        account: AccountEntity,
        dateMillis: Long = System.currentTimeMillis(),
        note: String = "",
        statementFingerprint: String? = null
    ) {
        if (account.currencyCode.isNullOrBlank() || !account.isActive || !amount.isFinite() || amount <= 0.0 || title.isBlank() ||
            category in setOf(Category.ACCOUNT_TRANSFER, Category.ACCOUNT_ADJUSTMENT, Category.INVESTMENT_SALE)) return
        viewModelScope.launch {
            val freshAccount = repository.account(account.id) ?: return@launch
            if (!freshAccount.isActive || freshAccount.currencyCode != account.currencyCode) return@launch
            repository.addTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    account = freshAccount.name,
                    dateMillis = dateMillis,
                    note = note,
                    statementFingerprint = statementFingerprint,
                    accountId = freshAccount.id,
                    currencyCode = freshAccount.currencyCode
                )
            )
            syncVaultToCloud()
        }
    }

    fun addTransaction(transaction: TransactionEntity) {
        if (!transaction.amount.isFinite() || transaction.amount <= 0.0 || transaction.title.isBlank() ||
            transaction.category in setOf(Category.ACCOUNT_TRANSFER, Category.ACCOUNT_ADJUSTMENT, Category.INVESTMENT_SALE)) return
        viewModelScope.launch {
            val currentAccounts = repository.accountsSnapshot()
            val linked = transaction.accountId?.let { id -> currentAccounts.firstOrNull { it.id == id } }
                ?: currentAccounts.filter { it.isActive && it.name.equals(transaction.account.trim(), ignoreCase = true) }
                    .singleOrNull()
            if (linked == null || !linked.isActive || linked.currencyCode.isNullOrBlank() ||
                (transaction.currencyCode != null && transaction.currencyCode != linked.currencyCode)) return@launch
            val stored = transaction.copy(
                account = linked.name,
                accountId = linked.id,
                currencyCode = linked.currencyCode
            )
            repository.addTransaction(stored)
            syncVaultToCloud()
        }
    }

    fun confirmImportedTransaction(transaction: TransactionEntity, account: AccountEntity) {
        viewModelScope.launch {
            val current = repository.account(account.id) ?: return@launch
            if (!current.isActive || current.currencyCode.isNullOrBlank() ||
                (transaction.currencyCode != null && transaction.currencyCode != current.currencyCode)) return@launch
            repository.updateTransaction(
                transaction.copy(
                    account = current.name,
                    accountId = current.id,
                    currencyCode = current.currencyCode,
                    importStatus = "CONFIRMED"
                )
            )
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

    fun updateTransaction(transaction: TransactionEntity) {
        if (!transaction.amount.isFinite() || transaction.amount <= 0.0 || transaction.title.isBlank() ||
            transaction.transactionKind != TransactionKind.STANDARD || transaction.sourceReference != null ||
            transaction.category in setOf(Category.ACCOUNT_TRANSFER, Category.ACCOUNT_ADJUSTMENT, Category.INVESTMENT_SALE)) return
        viewModelScope.launch {
            val currentAccounts = repository.accountsSnapshot()
            val linked = transaction.accountId?.let { id -> currentAccounts.firstOrNull { it.id == id } }
                ?: currentAccounts.filter { it.isActive && it.name.equals(transaction.account.trim(), ignoreCase = true) }
                    .singleOrNull()
            if (linked == null || !linked.isActive || linked.currencyCode.isNullOrBlank()) return@launch
            repository.updateTransaction(transaction.copy(
                account = linked.name,
                accountId = linked.id,
                currencyCode = linked.currencyCode
            ))
            syncVaultToCloud()
        }
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
        currentPrice: Double,
        currencyCode: String,
        purchaseAccount: AccountEntity? = null
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
                    dailyChangePercent = 0.0, // no live price feed: day change is unknown until prices are edited
                    currencyCode = currencyCode
                )
            )
            // Recording a holding is a portfolio snapshot, not proof that a purchase just occurred.
            // If the user named the account it came from, list the cash side on Cash Flow, awaiting
            // their confirmation, the same way an imported bank line waits before it counts.
            if (purchaseAccount != null && purchaseAccount.isActive && purchaseAccount.currencyCode == currencyCode) {
                val cost = shares * avgBuyPrice
                if (cost > 0) {
                    repository.addTransaction(
                        TransactionEntity(
                            title = "Investment purchase: ${symbol.uppercase()}",
                            amount = cost,
                            type = TransactionType.EXPENSE,
                            category = Category.INVESTMENT_SIP,
                            account = purchaseAccount.name,
                            note = "Purchase of $shares x ${symbol.uppercase()}; confirm to deduct from cash",
                            accountId = purchaseAccount.id,
                            currencyCode = currencyCode,
                            transactionKind = TransactionKind.ASSET_CONVERSION,
                            sourceReference = "holding:$holdingId",
                            importStatus = "PENDING_REVIEW"
                        )
                    )
                }
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

    /** Records a portfolio sale as an asset conversion into a same-currency cash account. */
    fun sellHolding(holding: HoldingEntity, sharesToSell: Double, salePrice: Double, proceedsAccount: AccountEntity) {
        if (holding.currencyCode.isNullOrBlank() || proceedsAccount.currencyCode != holding.currencyCode || !proceedsAccount.isActive) return
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
                        title = "Investment sale: ${holding.symbol}",
                        amount = proceeds,
                        type = TransactionType.INCOME,
                        category = Category.INVESTMENT_SALE,
                        account = proceedsAccount.name,
                        note = "Manual asset conversion for $qty ${holding.symbol}; recorded gain/loss ${"%.2f".format(gain)}",
                        accountId = proceedsAccount.id,
                        currencyCode = holding.currencyCode,
                        transactionKind = TransactionKind.ASSET_CONVERSION
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
        currencyCode: String? = null,
        contributionAccount: AccountEntity? = null
    ) {
        viewModelScope.launch {
            val sipId = repository.addSip(
                SipEntity(
                    fundName = fundName,
                    category = category,
                    monthlyAmount = monthlyAmount,
                    debitDayOfMonth = debitDayOfMonth,
                    isActive = true,
                    // Creating a plan does not mean a contribution has happened.
                    totalInvested = 0.0,
                    annualizedReturnPercent = 0.0,
                    lastDebitedYearMonth = null,
                    currencyCode = currencyCode
                )
            )
            // If the user named the account funding this month's contribution, list it on Cash Flow,
            // awaiting their confirmation, rather than assuming the debit already happened.
            if (contributionAccount != null && contributionAccount.isActive &&
                contributionAccount.currencyCode == currencyCode && monthlyAmount > 0
            ) {
                repository.addTransaction(
                    TransactionEntity(
                        title = "SIP contribution: $fundName",
                        amount = monthlyAmount,
                        type = TransactionType.EXPENSE,
                        category = Category.INVESTMENT_SIP,
                        account = contributionAccount.name,
                        note = "Planned contribution to $fundName; confirm to deduct from cash",
                        accountId = contributionAccount.id,
                        currencyCode = currencyCode,
                        transactionKind = TransactionKind.ASSET_CONVERSION,
                        sourceReference = "sip:$sipId",
                        importStatus = "PENDING_REVIEW"
                    )
                )
            }
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
        dueDays: Int,
        currencyCode: String = userSettings.value.currency.code
    ) {
        if (SupportedCurrency.values().none { it.code == currencyCode } || balance < 0.0 || limit <= 0.0 || apr < 0.0) return
        viewModelScope.launch {
            repository.addCreditCard(
                CreditCardEntity(
                    cardName = cardName,
                    currentBalance = balance,
                    creditLimit = limit,
                    apr = apr,
                    dueDateDays = dueDays,
                    currencyCode = currencyCode
                )
            )
            syncVaultToCloud()
        }
    }

    fun resolveCardCurrency(card: CreditCardEntity, currencyCode: String) {
        if (SupportedCurrency.values().none { it.code == currencyCode }) return
        viewModelScope.launch {
            repository.updateCreditCard(card.copy(currencyCode = currencyCode))
            syncVaultToCloud()
        }
    }

    private val cardPaymentsInFlight = mutableSetOf<Long>()

    fun payCreditCard(card: CreditCardEntity, paymentAmount: Double, sourceAccount: AccountEntity) {
        if (!paymentAmount.isFinite() || paymentAmount <= 0.0) return
        synchronized(cardPaymentsInFlight) {
            if (!cardPaymentsInFlight.add(card.id)) return
        }
        viewModelScope.launch {
            try {
                val freshCard = repository.creditCardsSnapshot().firstOrNull { it.id == card.id } ?: return@launch
                val freshAccount = repository.account(sourceAccount.id) ?: return@launch
                if (freshCard.currencyCode.isNullOrBlank() || freshAccount.currencyCode != freshCard.currencyCode || !freshAccount.isActive) return@launch
                val actualPayment = paymentAmount.coerceAtMost(freshCard.currentBalance)
                if (actualPayment <= 0.0) return@launch
                repository.payCreditCard(
                    freshCard,
                    actualPayment,
                    TransactionEntity(
                        title = "Payment to ${freshCard.cardName}",
                        amount = actualPayment,
                        type = TransactionType.EXPENSE,
                        category = Category.DEBT_PAYMENT,
                        account = freshAccount.name,
                        note = "Card debt settlement recorded manually; no payment was initiated by the app",
                        accountId = freshAccount.id,
                        currencyCode = freshCard.currencyCode,
                        transactionKind = TransactionKind.DEBT_SETTLEMENT
                    )
                )
                syncVaultToCloud()
            } finally {
                synchronized(cardPaymentsInFlight) { cardPaymentsInFlight.remove(card.id) }
            }
        }
    }

    fun addLoan(
        loanName: String,
        lender: String,
        totalAmount: Double,
        remainingBalance: Double,
        emi: Double,
        apr: Double,
        months: Int,
        currencyCode: String = userSettings.value.currency.code
    ) {
        if (SupportedCurrency.values().none { it.code == currencyCode } || totalAmount <= 0.0 || remainingBalance < 0.0 || remainingBalance > totalAmount || emi < 0.0 || apr < 0.0 || months <= 0) return
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
                    remainingMonths = (months * (remainingBalance / totalAmount)).toInt().coerceAtLeast(1),
                    currencyCode = currencyCode
                )
            )
            syncVaultToCloud()
        }
    }

    fun resolveLoanCurrency(loan: LoanEntity, currencyCode: String) {
        if (SupportedCurrency.values().none { it.code == currencyCode }) return
        viewModelScope.launch {
            repository.updateLoan(loan.copy(currencyCode = currencyCode))
            syncVaultToCloud()
        }
    }

    private val loanPaymentsInFlight = mutableSetOf<Long>()

    fun payLoanEmi(loan: LoanEntity, sourceAccount: AccountEntity) {
        synchronized(loanPaymentsInFlight) {
            if (!loanPaymentsInFlight.add(loan.id)) return
        }
        viewModelScope.launch {
            try {
                val freshLoan = repository.loansSnapshot().firstOrNull { it.id == loan.id } ?: return@launch
                val freshAccount = repository.account(sourceAccount.id) ?: return@launch
                if (freshLoan.currencyCode.isNullOrBlank() || freshAccount.currencyCode != freshLoan.currencyCode || !freshAccount.isActive) return@launch
                val transaction = TransactionEntity(
                    title = "EMI: ${freshLoan.loanName}",
                    amount = freshLoan.emiAmount,
                    type = TransactionType.EXPENSE,
                    category = Category.DEBT_PAYMENT,
                    account = freshAccount.name,
                    note = "Manual EMI record; no payment was initiated by the app",
                    accountId = freshAccount.id,
                    currencyCode = freshLoan.currencyCode,
                    transactionKind = TransactionKind.DEBT_SETTLEMENT
                )
                if (repository.payLoanEmi(freshLoan, transaction) > 0.0) syncVaultToCloud()
            } finally {
                synchronized(loanPaymentsInFlight) { loanPaymentsInFlight.remove(loan.id) }
            }
        }
    }

    /** A goal is a tracking label and is not an asset or a cash movement. */
    fun addGoal(
        title: String,
        category: String,
        targetAmount: Double,
        currentAmount: Double,
        monthlyContribution: Double,
        currencyCode: String = userSettings.value.currency.code
    ) {
        if (title.isBlank() || targetAmount <= 0.0 || currentAmount < 0.0 || monthlyContribution < 0.0 ||
            !targetAmount.isFinite() || !currentAmount.isFinite() || !monthlyContribution.isFinite() ||
            SupportedCurrency.values().none { it.code == currencyCode }) return
        viewModelScope.launch {
            repository.addGoal(
                GoalEntity(
                    title = title,
                    category = category,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    monthlyContribution = monthlyContribution,
                    currencyCode = currencyCode
                )
            )
            syncVaultToCloud()
        }
    }

    fun resolveGoalCurrency(goal: GoalEntity, currencyCode: String) {
        if (SupportedCurrency.values().none { it.code == currencyCode }) return
        viewModelScope.launch {
            repository.updateGoal(goal.copy(currencyCode = currencyCode))
            syncVaultToCloud()
        }
    }

    /** Updates goal progress only; this action does not move money in a cash account. */
    fun contributeGoal(goal: GoalEntity, amount: Double) {
        if (amount <= 0.0 || !amount.isFinite() || goal.currencyCode.isNullOrBlank()) return
        viewModelScope.launch {
            repository.contributeToGoal(goal, amount)
            syncVaultToCloud()
        }
    }

    /** Reduces the tracking balance only; record any real bank movement as an account transfer. */
    fun withdrawFromGoal(goal: GoalEntity, amount: Double) {
        if (amount <= 0.0 || !amount.isFinite() || goal.currencyCode.isNullOrBlank()) return
        viewModelScope.launch {
            val withdrawn = amount.coerceAtMost(goal.currentAmount)
            if (withdrawn <= 0.0) return@launch
            repository.contributeToGoal(goal, -withdrawn)
            syncVaultToCloud()
        }
    }

    companion object {
        // Only this account receives auto-seeded Kenyan demo/sample data on first login
        // (used as a shared demo login for testers/reviewers). All other accounts start blank.
        const val DEMO_SEED_EMAIL = "smarttechlab.apps@gmail.com"
    }
}
