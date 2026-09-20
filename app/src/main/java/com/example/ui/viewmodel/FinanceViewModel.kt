package com.example.ui.viewmodel

import android.app.Application
import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AdvisorSnapshot
import com.example.ai.GeminiClient
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
import kotlinx.coroutines.flow.combine
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
    val totalInflow: Double = 0.0,
    val totalOutflow: Double = 0.0,
    val netCashRetained: Double = 0.0,
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
                        database.financeDao().clearAllTransactions()
                        database.financeDao().clearAllHoldings()
                        database.financeDao().clearAllSips()
                        database.financeDao().clearAllCreditCards()
                        database.financeDao().clearAllLoans()
                        database.financeDao().clearAllGoals()
                    }

                    // Auto-seed demo data for the single designated demo/review account.
                    // Runs on first-ever login on this device, or again after switching
                    // back into this account from a different one (local was just wiped).
                    if (currentUser.email.equals(DEMO_SEED_EMAIL, ignoreCase = true) &&
                        (!preferencesManager.hasAutoSeededDemoData() || isAccountSwitch)
                    ) {
                        preferencesManager.setAutoSeededDemoData(true)
                        AppDatabase.reseedDatabase(database.financeDao())
                        preferencesManager.applyRegionPreset(GeographicRegion.EAST_AFRICA)
                    }
                }
            }
        }

        // Firebase Auth is authoritative. Do not restore the old local-only login state.
        _isPinEnabled.value = preferencesManager.isPinEnabled()
        _isPinLocked.value = preferencesManager.isPinEnabled() && preferencesManager.isLoggedIn()
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
            val res = firestoreSyncManager.syncLocalVaultToCloud(
                dao = database.financeDao(),
                settings = userSettings.value
            )
            if (res.success) {
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

            // Safety net: the shared demo/tester account must never end up blank,
            // even if this function is somehow reached for it. Immediately re-seed.
            if (_userEmail.value.equals(DEMO_SEED_EMAIL, ignoreCase = true)) {
                AppDatabase.reseedDatabase(database.financeDao())
                preferencesManager.applyRegionPreset(GeographicRegion.EAST_AFRICA)
            }
        }
    }

    fun reseedWithKenyanData() {
        viewModelScope.launch {
            AppDatabase.reseedDatabase(database.financeDao())
            applyRegionPreset(GeographicRegion.EAST_AFRICA)
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
        validateSelectedTab()
    }

    fun updateCurrency(currency: SupportedCurrency) {
        preferencesManager.updateCurrency(currency)
    }

    fun toggleHideBalances() {
        preferencesManager.toggleHideBalances()
    }

    fun toggleModule(module: String, enabled: Boolean) {
        preferencesManager.toggleModule(module, enabled)
        validateSelectedTab()
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
    val summary: StateFlow<FinanceSummary> = combine(
        transactions, holdings, creditCards, loans, goals
    ) { txList, hList, cList, lList, gList ->
        // No demo-value substitution. A zero total means the user has recorded nothing, and that
        // is what the UI and the advisor must both be told.
        val inflow = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val outflow = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netCash = inflow - outflow
        val savingsRate = if (inflow > 0) (netCash / inflow) * 100.0 else 0.0

        val portVal = hList.sumOf { it.totalValue }
        val portCost = hList.sumOf { it.totalCost }
        val dayGain = hList.sumOf { it.totalValue * (it.dailyChangePercent / 100.0) }
        val dayGainPct = if (portVal > 0) (dayGain / portVal) * 100.0 else 0.0
        // Simple return on cost basis. Not XIRR — contribution dates are not recorded.
        val portReturnPct = if (portCost > 0) ((portVal - portCost) / portCost) * 100.0 else 0.0

        val cardDebt = cList.sumOf { it.currentBalance }
        val loanDebt = lList.sumOf { it.remainingBalance }
        val totalDebtVal = cardDebt + loanDebt
        val monthlyDebt = cList.sumOf { it.currentBalance * 0.03 } + lList.sumOf { it.emiAmount }
        val dti = if (inflow > 0) (monthlyDebt / inflow) * 100.0 else 0.0

        val goalsSum = gList.sumOf { it.currentAmount }
        // Liquid cash was previously a hardcoded 850,000. Until a cash-account entity exists,
        // net cash retained from recorded transactions is the only defensible figure.
        val liquidCash = if (netCash > 0) netCash else 0.0
        val totalAssetsVal = portVal + goalsSum + liquidCash
        val totalLiabilitiesVal = totalDebtVal
        val netWorthVal = totalAssetsVal - totalLiabilitiesVal

        FinanceSummary(
            totalNetWorth = netWorthVal,
            totalAssets = totalAssetsVal,
            totalLiabilities = totalLiabilitiesVal,
            totalInflow = inflow,
            totalOutflow = outflow,
            netCashRetained = netCash,
            savingsRate = savingsRate,
            portfolioValue = portVal,
            portfolioCost = portCost,
            portfolioDayGain = dayGain,
            portfolioDayGainPercent = dayGainPct,
            portfolioReturnPercent = portReturnPct,
            totalDebt = totalDebtVal,
            monthlyDebtServicing = monthlyDebt,
            dtiRatio = dti,
            transactionCount = txList.size,
            holdingCount = hList.size,
            debtAccountCount = cList.size + lList.size,
            goalCount = gList.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceSummary())

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
                monthlyInflow = s.totalInflow,
                monthlyOutflow = s.totalOutflow,
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

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
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
            repository.addHolding(
                HoldingEntity(
                    symbol = symbol.uppercase(),
                    name = name,
                    type = type,
                    shares = shares,
                    avgBuyPrice = avgBuyPrice,
                    currentPrice = currentPrice,
                    dailyChangePercent = 0.5
                )
            )
            syncVaultToCloud()
        }
    }

    fun deleteHolding(holding: HoldingEntity) {
        viewModelScope.launch {
            repository.deleteHolding(holding)
            syncVaultToCloud()
        }
    }

    fun addSip(
        fundName: String,
        category: String,
        monthlyAmount: Double,
        debitDayOfMonth: Int
    ) {
        viewModelScope.launch {
            repository.addSip(
                SipEntity(
                    fundName = fundName,
                    category = category,
                    monthlyAmount = monthlyAmount,
                    debitDayOfMonth = debitDayOfMonth,
                    isActive = true,
                    totalInvested = monthlyAmount * 6
                )
            )
            syncVaultToCloud()
        }
    }

    fun toggleSip(sip: SipEntity) {
        viewModelScope.launch {
            repository.toggleSipActive(sip)
            syncVaultToCloud()
        }
    }

    fun deleteSip(sip: SipEntity) {
        viewModelScope.launch {
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

    fun addGoal(
        title: String,
        category: String,
        targetAmount: Double,
        currentAmount: Double,
        monthlyContribution: Double
    ) {
        viewModelScope.launch {
            repository.addGoal(
                GoalEntity(
                    title = title,
                    category = category,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    monthlyContribution = monthlyContribution
                )
            )
            syncVaultToCloud()
        }
    }

    fun contributeGoal(goal: GoalEntity, amount: Double) {
        viewModelScope.launch {
            repository.contributeToGoal(goal, amount)
            repository.addTransaction(
                TransactionEntity(
                    title = "Goal Deposit: ${goal.title}",
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = Category.OTHER,
                    account = "Chase Checking",
                    note = "Capital reserve deposit"
                )
            )
            syncVaultToCloud()
        }
    }

    companion object {
        // Only this account receives auto-seeded Kenyan demo/sample data on first login
        // (used as a shared demo login for testers/reviewers). All other accounts start blank.
        const val DEMO_SEED_EMAIL = "smarttechlab.apps@gmail.com"
    }
}
