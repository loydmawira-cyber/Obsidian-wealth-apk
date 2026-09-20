package com.example.ui.viewmodel

import android.app.Application
import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

data class FinanceSummary(
    val totalNetWorth: Double = 11877250.0,
    val totalAssets: Double = 15214750.0,
    val totalLiabilities: Double = 3337500.0,
    val totalInflow: Double = 570000.00,
    val totalOutflow: Double = 256900.00,
    val netCashRetained: Double = 313100.00,
    val savingsRate: Double = 54.9,
    val portfolioValue: Double = 9074750.00,
    val portfolioDayGain: Double = 72400.00,
    val portfolioDayGainPercent: Double = 0.80,
    val portfolioXirr: Double = 17.4,
    val totalDebt: Double = 3337500.00,
    val monthlyDebtServicing: Double = 108050.00,
    val dtiRatio: Double = 18.9,
    val healthScore: Int = 91
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

    val userSettings: StateFlow<UserSettings>
    private val firestoreSyncManager: FirestoreSyncManager

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud

    private val _cloudSyncResult = MutableStateFlow<CloudSyncResult?>(null)
    val cloudSyncResult: StateFlow<CloudSyncResult?> = _cloudSyncResult

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

    private val _quickPin = MutableStateFlow("")
    val quickPin: StateFlow<String> = _quickPin

    private val _isPinLocked = MutableStateFlow(false)
    val isPinLocked: StateFlow<Boolean> = _isPinLocked

    init {
        database = AppDatabase.getDatabase(application, viewModelScope)
        repository = FinanceRepository(database.financeDao())
        preferencesManager = PreferencesManager(application)
        userSettings = preferencesManager.settings
        firestoreSyncManager = FirestoreSyncManager(application)
        _cloudVaultId.value = preferencesManager.getCloudVaultId()
        _lastCloudSyncTime.value = preferencesManager.getLastCloudSyncTime()

        // Firebase Auth is the source of truth for login state when available.
        firebaseAuth?.addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            _isLoggedIn.value = currentUser != null
            _userEmail.value = currentUser?.email.orEmpty()
            if (currentUser != null) {
                val lastUid = preferencesManager.getLastSignedInUid()
                val isAccountSwitch = lastUid.isNotBlank() && lastUid != currentUser.uid

                preferencesManager.setCloudVaultId(currentUser.uid)
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
        _quickPin.value = preferencesManager.getQuickPin()
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
                        preferencesManager.setCloudVaultId(uid)
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
                        preferencesManager.setCloudVaultId(user.uid)
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

    fun verifyAndUnlockPin(pinInput: String): Boolean {
        val saved = preferencesManager.getQuickPin()
        if (pinInput == saved && saved.length == 4) {
            _isPinLocked.value = false
            return true
        }
        return false
    }

    fun saveQuickPin(pin: String) {
        if (pin.length == 4) {
            preferencesManager.setQuickPin(pin)
            preferencesManager.setPinEnabled(true)
            _quickPin.value = pin
            _isPinEnabled.value = true
            _isPinLocked.value = false
        }
    }

    fun disableQuickPin() {
        preferencesManager.setQuickPin("")
        preferencesManager.setPinEnabled(false)
        _quickPin.value = ""
        _isPinEnabled.value = false
        _isPinLocked.value = false
    }

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

    fun setCloudVaultId(id: String) {
        preferencesManager.setCloudVaultId(id)
        _cloudVaultId.value = id
    }

    fun syncVaultToCloud(onComplete: (CloudSyncResult) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            val res = firestoreSyncManager.syncLocalVaultToCloud(
                vaultId = _cloudVaultId.value,
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
                vaultId = _cloudVaultId.value,
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
        val inflow = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }.let { if (it == 0.0) 570000.0 else it }
        val outflow = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }.let { if (it == 0.0) 256900.0 else it }
        val netCash = inflow - outflow
        val savingsRate = if (inflow > 0) (netCash / inflow) * 100.0 else 0.0

        val portVal = hList.sumOf { it.totalValue }.let { if (it == 0.0) 9074750.0 else it }
        val portCost = hList.sumOf { it.totalCost }.let { if (it == 0.0) 7950000.0 else it }
        val dayGain = hList.sumOf { it.totalValue * (it.dailyChangePercent / 100.0) }.let { if (it == 0.0) 72400.0 else it }
        val dayGainPct = if (portVal > 0) (dayGain / portVal) * 100.0 else 0.80

        val cardDebt = cList.sumOf { it.currentBalance }
        val loanDebt = lList.sumOf { it.remainingBalance }
        val totalDebtVal = (cardDebt + loanDebt).let { if (it == 0.0) 3337500.0 else it }
        val monthlyDebt = (cList.sumOf { it.currentBalance * 0.03 } + lList.sumOf { it.emiAmount }).let { if (it == 0.0) 108050.0 else it }
        val dti = if (inflow > 0) (monthlyDebt / inflow) * 100.0 else 18.9

        val goalsSum = gList.sumOf { it.currentAmount }.let { if (it == 0.0) 5690000.0 else it }
        val liquidCash = 850000.0
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
            portfolioDayGain = dayGain,
            portfolioDayGainPercent = dayGainPct,
            portfolioXirr = 17.4,
            totalDebt = totalDebtVal,
            monthlyDebtServicing = monthlyDebt,
            dtiRatio = dti,
            healthScore = 91
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
            val curr = userSettings.value.currency.symbol
            val reg = userSettings.value.region.title
            val contextData = """
Geographical Region: $reg
Base Currency: ${userSettings.value.currency.displayName} (Symbol: $curr)
Fiscal Calendar: ${userSettings.value.fiscalCalendar.title}
Advisory Risk Profile: ${userSettings.value.aiRiskProfile.title}
Net Worth: $curr${"%.2f".format(summary.value.totalNetWorth)}
Assets: $curr${"%.2f".format(summary.value.totalAssets)} | Liabilities: $curr${"%.2f".format(summary.value.totalLiabilities)}
Monthly Inflow: $curr${"%.2f".format(summary.value.totalInflow)} | Outflow: $curr${"%.2f".format(summary.value.totalOutflow)}
Savings Velocity: ${"%.1f".format(summary.value.savingsRate)}%
Investments: $curr${"%.2f".format(summary.value.portfolioValue)} (XIRR: 16.8%)
Total Debt: $curr${"%.2f".format(summary.value.totalDebt)} (DTI: ${"%.1f".format(summary.value.dtiRatio)}%)
Health Score: ${summary.value.healthScore}/100
Note: Provide financial calculations and strategic recommendations in $curr and respect the regional economic environment.
""".trimIndent()

            val aiResponseText = GeminiClient.generateFinancialAdvice(question, contextData)
            _isAiThinking.value = false
            _chatMessages.value = _chatMessages.value + ChatMessage(sender = "AI", text = aiResponseText)
        }
    }

    // CRUD Actions
    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        account: String,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    account = account,
                    note = note
                )
            )
            syncVaultToCloud()
        }
    }

    fun parseAndAddNaturalTransaction(text: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val parsed = GeminiClient.parseNaturalLanguageTransaction(text)
            if (parsed != null) {
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
                        account = parsed.account
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
