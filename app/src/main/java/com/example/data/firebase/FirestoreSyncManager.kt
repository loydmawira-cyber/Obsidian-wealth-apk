package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.dao.FinanceDao
import com.example.data.models.Category
import com.example.data.models.CreditCardEntity
import com.example.data.models.FiscalCalendar
import com.example.data.models.GeographicRegion
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.HoldingType
import com.example.data.models.LoanEntity
import com.example.data.models.NumberFormatStyle
import com.example.data.models.SipEntity
import com.example.data.models.SupportedCurrency
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import com.example.data.models.UserSettings
import com.example.data.repository.PreferencesManager
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class FirestoreConnectionStatus {
    object Active : FirestoreConnectionStatus()
    data class NotConfigured(val reason: String) : FirestoreConnectionStatus()
    data class Error(val message: String) : FirestoreConnectionStatus()
}

data class CloudSyncResult(
    val success: Boolean,
    val message: String,
    val totalItems: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class FirestoreSyncManager(private val context: Context) {

    private val TAG = "FirestoreSyncManager"

    /**
     * Checks if Firebase is initialized in this runtime environment.
     * FirebaseApp is initialized automatically if google-services.json is present,
     * or can be manually checked.
     */
    fun checkStatus(): FirestoreConnectionStatus {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                FirestoreConnectionStatus.NotConfigured(
                    "FirebaseApp is not initialized yet. Add your google-services.json to link your Firebase project."
                )
            } else {
                FirestoreConnectionStatus.Active
            }
        } catch (e: Throwable) {
            FirestoreConnectionStatus.Error("Initialization check failed: ${e.message}")
        }
    }

    private fun getAuthInstance(): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) null
            else FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to get FirebaseAuth instance", e)
            null
        }
    }

    private fun getFirestoreInstance(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) null
            else {
                val db = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
                db
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to get FirebaseFirestore instance", e)
            null
        }
    }

    /**
     * Pushes all local Room entities and settings to Cloud Firestore.
     */
    suspend fun syncLocalVaultToCloud(
        dao: FinanceDao,
        settings: UserSettings
    ): CloudSyncResult = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance()
            ?: return@withContext CloudSyncResult(
                success = false,
                message = "Firestore is unconfigured. Link your Firebase project by adding google-services.json."
            )

        try {
            val authUser = getAuthInstance()?.currentUser
                ?: return@withContext CloudSyncResult(false, "Please sign in before syncing to Firestore.")
            firestore.collection("users").document(authUser.uid).set(
                mapOf("uid" to authUser.uid, "email" to (authUser.email ?: ""), "vaultId" to authUser.uid, "lastSeenMillis" to System.currentTimeMillis()),
                SetOptions.merge()
            ).awaitTask()
            val transactions = dao.getTransactionsSnapshot()
            val holdings = dao.getHoldingsSnapshot()
            val sips = dao.getSipsSnapshot()
            val cards = dao.getCreditCardsSnapshot()
            val loans = dao.getLoansSnapshot()
            val goals = dao.getGoalsSnapshot()

            val vaultRef = firestore.collection("wealth_vaults").document(authUser.uid)

            // 1. Vault Metadata
            val metaData = hashMapOf(
                // The authenticated uid IS the vault id — the caller-supplied `vaultId` parameter
                // was removed in the auth-binding fix, and this was its last surviving use.
                "vaultId" to authUser.uid,
                "currency" to settings.currency.name,
                "region" to settings.region.id,
                "lastSyncMillis" to System.currentTimeMillis(),
                "transactionsCount" to transactions.size,
                "holdingsCount" to holdings.size,
                "sipsCount" to sips.size,
                "cardsCount" to cards.size,
                "loansCount" to loans.size,
                "goalsCount" to goals.size
            )
            vaultRef.set(metaData, SetOptions.merge()).awaitTask()

            // 2. Sync Transactions
            val txCollection = vaultRef.collection("transactions")
            transactions.forEach { tx ->
                val txMap = hashMapOf(
                    "id" to tx.id,
                    "title" to tx.title,
                    "amount" to tx.amount,
                    "type" to tx.type.name,
                    "category" to tx.category.name,
                    "account" to tx.account,
                    "dateMillis" to tx.dateMillis,
                    "note" to tx.note,
                    "isRecurring" to tx.isRecurring,
                    "statementFingerprint" to tx.statementFingerprint
                )
                txCollection.document(tx.id.toString()).set(txMap, SetOptions.merge()).awaitTask()
            }

            // 3. Sync Holdings
            val holdingsCollection = vaultRef.collection("holdings")
            holdings.forEach { h ->
                val hMap = hashMapOf(
                    "id" to h.id,
                    "symbol" to h.symbol,
                    "name" to h.name,
                    "type" to h.type.name,
                    "shares" to h.shares,
                    "avgBuyPrice" to h.avgBuyPrice,
                    "currentPrice" to h.currentPrice,
                    "dailyChangePercent" to h.dailyChangePercent
                )
                holdingsCollection.document(h.id.toString()).set(hMap, SetOptions.merge()).awaitTask()
            }

            // 4. Sync SIPs
            val sipsCollection = vaultRef.collection("sips")
            sips.forEach { s ->
                val sMap = hashMapOf(
                    "id" to s.id,
                    "fundName" to s.fundName,
                    "category" to s.category,
                    "monthlyAmount" to s.monthlyAmount,
                    "debitDayOfMonth" to s.debitDayOfMonth,
                    "isActive" to s.isActive,
                    "totalInvested" to s.totalInvested,
                    "annualizedReturnPercent" to s.annualizedReturnPercent
                )
                sipsCollection.document(s.id.toString()).set(sMap, SetOptions.merge()).awaitTask()
            }

            // 5. Sync Credit Cards
            val cardsCollection = vaultRef.collection("credit_cards")
            cards.forEach { c ->
                val cMap = hashMapOf(
                    "id" to c.id,
                    "cardName" to c.cardName,
                    "lastFour" to c.lastFour,
                    "currentBalance" to c.currentBalance,
                    "creditLimit" to c.creditLimit,
                    "apr" to c.apr,
                    "dueDateDays" to c.dueDateDays,
                    "colorHex" to c.colorHex
                )
                cardsCollection.document(c.id.toString()).set(cMap, SetOptions.merge()).awaitTask()
            }

            // 6. Sync Loans
            val loansCollection = vaultRef.collection("loans")
            loans.forEach { l ->
                val lMap = hashMapOf(
                    "id" to l.id,
                    "loanName" to l.loanName,
                    "lender" to l.lender,
                    "totalAmount" to l.totalAmount,
                    "remainingBalance" to l.remainingBalance,
                    "emiAmount" to l.emiAmount,
                    "interestRate" to l.interestRate,
                    "totalMonths" to l.totalMonths,
                    "remainingMonths" to l.remainingMonths,
                    "dueDayOfMonth" to l.dueDayOfMonth
                )
                loansCollection.document(l.id.toString()).set(lMap, SetOptions.merge()).awaitTask()
            }

            // 7. Sync Goals
            val goalsCollection = vaultRef.collection("goals")
            goals.forEach { g ->
                val gMap = hashMapOf(
                    "id" to g.id,
                    "title" to g.title,
                    "category" to g.category,
                    "targetAmount" to g.targetAmount,
                    "currentAmount" to g.currentAmount,
                    "monthlyContribution" to g.monthlyContribution,
                    "targetYear" to g.targetYear,
                    "colorHex" to g.colorHex
                )
                goalsCollection.document(g.id.toString()).set(gMap, SetOptions.merge()).awaitTask()
            }

            // 8. Sync User Settings
            val settingsMap = hashMapOf(
                "currency" to settings.currency.name,
                "region" to settings.region.id,
                "fiscalCalendar" to settings.fiscalCalendar.name,
                "numberFormatStyle" to settings.numberFormatStyle.name,
                "enableCashFlow" to settings.enableCashFlow,
                "enableInvestments" to settings.enableInvestments,
                "enableDebtCenter" to settings.enableDebtCenter,
                "enableGoals" to settings.enableGoals,
                "showFloatingAiAdvisor" to settings.showFloatingAiAdvisor,
                "aiRiskProfile" to settings.aiRiskProfile.name,
                "hideBalances" to settings.hideBalances,
                "biometricProtection" to settings.biometricProtection,
                "enableNotifications" to settings.enableNotifications,
                "enableBillDueReminders" to settings.enableBillDueReminders,
                "enableSipReminders" to settings.enableSipReminders,
                "enableDailyBriefingReminders" to settings.enableDailyBriefingReminders
            )
            vaultRef.collection("settings").document("preferences")
                .set(settingsMap, SetOptions.merge()).awaitTask()

            val totalCount = transactions.size + holdings.size + sips.size + cards.size + loans.size + goals.size
            CloudSyncResult(
                success = true,
                message = "Successfully synchronized $totalCount financial records to Cloud Firestore.",
                totalItems = totalCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up to Firestore", e)
            CloudSyncResult(
                success = false,
                message = "Sync failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Restores all entities from Cloud Firestore down to the local Room database.
     *
     * The vault id is derived from the authenticated user and is NOT a parameter. Previously this
     * function took a caller-supplied vaultId and performed no auth check at all, so any value
     * passed in (and the id was user-editable in Settings) would read that vault's complete
     * financial history. A signed-in user can now only ever restore their own vault.
     */
    suspend fun restoreCloudVaultToLocal(
        dao: FinanceDao,
        preferencesManager: PreferencesManager
    ): CloudSyncResult = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance()
            ?: return@withContext CloudSyncResult(
                success = false,
                message = "Firestore is unconfigured. Link your Firebase project by adding google-services.json."
            )

        try {
            val authUser = getAuthInstance()?.currentUser
                ?: return@withContext CloudSyncResult(false, "Please sign in before restoring from Firestore.")
            val vaultRef = firestore.collection("wealth_vaults").document(authUser.uid)

            // 1. Transactions
            val txSnap = vaultRef.collection("transactions").get().awaitTask()
            val txList = txSnap.documents.mapNotNull { doc ->
                try {
                    TransactionEntity(
                        id = doc.getLong("id") ?: 0L,
                        title = doc.getString("title") ?: "Restored Transaction",
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = try { TransactionType.valueOf(doc.getString("type") ?: "EXPENSE") } catch (e: Exception) { TransactionType.EXPENSE },
                        category = try { Category.valueOf(doc.getString("category") ?: "OTHER") } catch (e: Exception) { Category.OTHER },
                        account = doc.getString("account") ?: "Checking",
                        dateMillis = doc.getLong("dateMillis") ?: System.currentTimeMillis(),
                        note = doc.getString("note") ?: "",
                        isRecurring = doc.getBoolean("isRecurring") ?: false,
                        statementFingerprint = doc.getString("statementFingerprint")
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 2. Holdings
            val hSnap = vaultRef.collection("holdings").get().awaitTask()
            val hList = hSnap.documents.mapNotNull { doc ->
                try {
                    HoldingEntity(
                        id = doc.getLong("id") ?: 0L,
                        symbol = doc.getString("symbol") ?: "",
                        name = doc.getString("name") ?: "",
                        type = try { HoldingType.valueOf(doc.getString("type") ?: "STOCK") } catch (e: Exception) { HoldingType.STOCK },
                        shares = doc.getDouble("shares") ?: 0.0,
                        avgBuyPrice = doc.getDouble("avgBuyPrice") ?: 0.0,
                        currentPrice = doc.getDouble("currentPrice") ?: 0.0,
                        dailyChangePercent = doc.getDouble("dailyChangePercent") ?: 0.0
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 3. SIPs
            val sipsSnap = vaultRef.collection("sips").get().awaitTask()
            val sList = sipsSnap.documents.mapNotNull { doc ->
                try {
                    SipEntity(
                        id = doc.getLong("id") ?: 0L,
                        fundName = doc.getString("fundName") ?: "",
                        category = doc.getString("category") ?: "",
                        monthlyAmount = doc.getDouble("monthlyAmount") ?: 0.0,
                        debitDayOfMonth = doc.getLong("debitDayOfMonth")?.toInt() ?: 1,
                        isActive = doc.getBoolean("isActive") ?: true,
                        totalInvested = doc.getDouble("totalInvested") ?: 0.0,
                        annualizedReturnPercent = doc.getDouble("annualizedReturnPercent") ?: 12.0
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 4. Cards
            val cSnap = vaultRef.collection("credit_cards").get().awaitTask()
            val cList = cSnap.documents.mapNotNull { doc ->
                try {
                    CreditCardEntity(
                        id = doc.getLong("id") ?: 0L,
                        cardName = doc.getString("cardName") ?: "",
                        lastFour = doc.getString("lastFour") ?: "0000",
                        currentBalance = doc.getDouble("currentBalance") ?: 0.0,
                        creditLimit = doc.getDouble("creditLimit") ?: 0.0,
                        apr = doc.getDouble("apr") ?: 18.0,
                        dueDateDays = doc.getLong("dueDateDays")?.toInt() ?: 15,
                        colorHex = doc.getString("colorHex") ?: "#1E293B"
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 5. Loans
            val lSnap = vaultRef.collection("loans").get().awaitTask()
            val lList = lSnap.documents.mapNotNull { doc ->
                try {
                    LoanEntity(
                        id = doc.getLong("id") ?: 0L,
                        loanName = doc.getString("loanName") ?: "",
                        lender = doc.getString("lender") ?: "",
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        remainingBalance = doc.getDouble("remainingBalance") ?: 0.0,
                        emiAmount = doc.getDouble("emiAmount") ?: 0.0,
                        interestRate = doc.getDouble("interestRate") ?: 12.0,
                        totalMonths = doc.getLong("totalMonths")?.toInt() ?: 60,
                        remainingMonths = doc.getLong("remainingMonths")?.toInt() ?: 0,
                        dueDayOfMonth = doc.getLong("dueDayOfMonth")?.toInt() ?: 15
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // 6. Goals
            val gSnap = vaultRef.collection("goals").get().awaitTask()
            val gList = gSnap.documents.mapNotNull { doc ->
                try {
                    GoalEntity(
                        id = doc.getLong("id") ?: 0L,
                        title = doc.getString("title") ?: "",
                        category = doc.getString("category") ?: "Savings",
                        targetAmount = doc.getDouble("targetAmount") ?: 0.0,
                        currentAmount = doc.getDouble("currentAmount") ?: 0.0,
                        monthlyContribution = doc.getDouble("monthlyContribution") ?: 0.0,
                        targetYear = doc.getLong("targetYear")?.toInt() ?: 2026,
                        colorHex = doc.getString("colorHex") ?: "#10B981"
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Insert into Room
            if (txList.isNotEmpty()) dao.insertTransactions(txList)
            if (hList.isNotEmpty()) dao.insertHoldings(hList)
            if (sList.isNotEmpty()) dao.insertSips(sList)
            if (cList.isNotEmpty()) dao.insertCreditCards(cList)
            if (lList.isNotEmpty()) dao.insertLoans(lList)
            if (gList.isNotEmpty()) dao.insertGoals(gList)

            // 7. Settings
            val settingsDoc = vaultRef.collection("settings").document("preferences").get().awaitTask()
            if (settingsDoc.exists()) {
                val curStr = settingsDoc.getString("currency") ?: SupportedCurrency.KES.name
                val regStr = settingsDoc.getString("region") ?: GeographicRegion.EAST_AFRICA.id
                val cur = try { SupportedCurrency.valueOf(curStr) } catch (e: Exception) { SupportedCurrency.KES }
                val reg = GeographicRegion.values().find { it.id == regStr } ?: GeographicRegion.EAST_AFRICA
                val fiscStr = settingsDoc.getString("fiscalCalendar") ?: FiscalCalendar.MID_YEAR.name
                val numStr = settingsDoc.getString("numberFormatStyle") ?: NumberFormatStyle.WESTERN_STANDARD.name

                val restoredSettings = UserSettings(
                    currency = cur,
                    region = reg,
                    fiscalCalendar = try { FiscalCalendar.valueOf(fiscStr) } catch (e: Exception) { FiscalCalendar.MID_YEAR },
                    numberFormatStyle = try { NumberFormatStyle.valueOf(numStr) } catch (e: Exception) { NumberFormatStyle.WESTERN_STANDARD },
                    enableCashFlow = settingsDoc.getBoolean("enableCashFlow") ?: true,
                    enableInvestments = settingsDoc.getBoolean("enableInvestments") ?: true,
                    enableDebtCenter = settingsDoc.getBoolean("enableDebtCenter") ?: true,
                    enableGoals = settingsDoc.getBoolean("enableGoals") ?: true,
                    showFloatingAiAdvisor = settingsDoc.getBoolean("showFloatingAiAdvisor") ?: true,
                    hideBalances = settingsDoc.getBoolean("hideBalances") ?: false,
                    biometricProtection = settingsDoc.getBoolean("biometricProtection") ?: true,
                    enableNotifications = settingsDoc.getBoolean("enableNotifications") ?: true,
                    enableBillDueReminders = settingsDoc.getBoolean("enableBillDueReminders") ?: true,
                    enableSipReminders = settingsDoc.getBoolean("enableSipReminders") ?: true,
                    enableDailyBriefingReminders = settingsDoc.getBoolean("enableDailyBriefingReminders") ?: true
                )
                preferencesManager.updateSettings(restoredSettings)
            }

            val totalCount = txList.size + hList.size + sList.size + cList.size + lList.size + gList.size
            CloudSyncResult(
                success = true,
                message = "Successfully restored $totalCount financial records from Cloud Firestore.",
                totalItems = totalCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from Firestore", e)
            CloudSyncResult(
                success = false,
                message = "Restore failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }
}

/**
 * Lightweight coroutine await extension for Google Play Services Task.
 */
private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        cont.resume(result)
    }
    addOnFailureListener { exception ->
        cont.resumeWithException(exception)
    }
}
