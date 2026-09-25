package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.data.models.AccountEntity
import com.example.data.models.CreditCardEntity
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.LoanEntity
import com.example.data.models.BudgetEntity
import com.example.data.models.NetWorthSnapshotEntity
import com.example.data.models.SipEntity
import com.example.data.models.TransactionEntity
import com.example.data.models.AccountLedger
import com.example.data.models.TransactionKind
import com.example.data.models.TransactionType
import com.example.data.models.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Cash / bank / mobile-money accounts ---
    @Query("SELECT * FROM accounts ORDER BY isActive DESC, name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY isActive DESC, name ASC")
    suspend fun getAccountsSnapshot(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccount(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE transactions SET currencyCode = :currencyCode WHERE accountId = :accountId AND currencyCode IS NULL")
    suspend fun resolveTransactionCurrency(accountId: Long, currencyCode: String)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()

    @Transaction
    suspend fun recordTransfer(source: TransactionEntity, destination: TransactionEntity): Boolean {
        val sourceId = source.accountId ?: return false
        val destinationId = destination.accountId ?: return false
        val from = getAccount(sourceId) ?: return false
        val to = getAccount(destinationId) ?: return false
        val amount = source.amount
        if (!from.isActive || !to.isActive || sourceId == destinationId || amount <= 0.0 || !amount.isFinite() ||
            destination.amount != amount || from.currencyCode.isNullOrBlank() || from.currencyCode != to.currencyCode ||
            source.currencyCode != from.currencyCode || destination.currencyCode != to.currencyCode ||
            source.transactionKind != TransactionKind.TRANSFER || destination.transactionKind != TransactionKind.TRANSFER ||
            source.type != TransactionType.EXPENSE || destination.type != TransactionType.INCOME ||
            source.category != Category.ACCOUNT_TRANSFER || destination.category != Category.ACCOUNT_TRANSFER ||
            source.transferGroupId.isNullOrBlank() || source.transferGroupId != destination.transferGroupId) return false
        if (!AccountLedger.hasSufficientBalance(from, getTransactionsSnapshot(), amount, source.dateMillis + 1L)) return false
        insertTransaction(source)
        insertTransaction(destination)
        return true
    }

    @Transaction
    suspend fun recordReconciliation(account: AccountEntity, adjustment: TransactionEntity) {
        insertTransaction(adjustment)
        updateAccount(account)
    }

    @Transaction
    suspend fun recordCreditCardSettlement(card: CreditCardEntity, transaction: TransactionEntity): Boolean {
        val freshCard = getCreditCard(card.id) ?: return false
        val account = transaction.accountId?.let { getAccount(it) } ?: return false
        val amount = transaction.amount
        if (!account.isActive || account.currencyCode.isNullOrBlank() || account.currencyCode != freshCard.currencyCode ||
            transaction.account != account.name || transaction.loanId != null || transaction.category != Category.DEBT_PAYMENT ||
            transaction.currencyCode != account.currencyCode || transaction.creditCardId != freshCard.id ||
            transaction.transactionKind != TransactionKind.DEBT_SETTLEMENT || transaction.type != TransactionType.EXPENSE ||
            !amount.isFinite() || amount <= 0.0 || amount > freshCard.currentBalance + 0.000001 ||
            !AccountLedger.hasSufficientBalance(account, getTransactionsSnapshot(), amount, transaction.dateMillis + 1L)) return false
        updateCreditCard(freshCard.copy(currentBalance = (freshCard.currentBalance - amount).coerceAtLeast(0.0)))
        insertTransaction(transaction.copy(amount = amount))
        return true
    }

    @Transaction
    suspend fun recordLoanSettlement(loan: LoanEntity, transaction: TransactionEntity): Boolean {
        val freshLoan = getLoan(loan.id) ?: return false
        val account = transaction.accountId?.let { getAccount(it) } ?: return false
        val amount = transaction.amount
        val monthlyInterest = freshLoan.remainingBalance * (freshLoan.interestRate.coerceAtLeast(0.0) / 100.0) / 12.0
        val expectedPayment = freshLoan.emiAmount.coerceAtLeast(0.0)
            .coerceAtMost(freshLoan.remainingBalance + monthlyInterest)
        val principalPaid = (expectedPayment - monthlyInterest).coerceIn(0.0, freshLoan.remainingBalance)
        val expectedBalance = (freshLoan.remainingBalance - principalPaid).coerceAtLeast(0.0)
        val expectedMonths = if (principalPaid > 0.0) (freshLoan.remainingMonths - 1).coerceAtLeast(0) else freshLoan.remainingMonths
        if (!account.isActive || account.currencyCode.isNullOrBlank() || account.currencyCode != freshLoan.currencyCode ||
            transaction.account != account.name || transaction.currencyCode != account.currencyCode ||
            transaction.loanId != freshLoan.id || transaction.creditCardId != null ||
            transaction.transactionKind != TransactionKind.DEBT_SETTLEMENT || transaction.type != TransactionType.EXPENSE ||
            transaction.category != Category.DEBT_PAYMENT || !amount.isFinite() || amount <= 0.0 ||
            freshLoan.remainingMonths <= 0 || kotlin.math.abs(amount - expectedPayment) > 0.000001 ||
            kotlin.math.abs(loan.remainingBalance - expectedBalance) > 0.000001 || loan.remainingMonths != expectedMonths ||
            loan.totalAmount != freshLoan.totalAmount ||
            !AccountLedger.hasSufficientBalance(account, getTransactionsSnapshot(), amount, transaction.dateMillis + 1L) ||
            loan.remainingBalance > freshLoan.remainingBalance + 0.000001 || loan.remainingBalance < 0.0 ||
            loan.remainingMonths > freshLoan.remainingMonths) return false
        updateLoan(loan)
        insertTransaction(transaction)
        return true
    }

    @Transaction
    suspend fun recordCreditCardPurchase(cardId: Long, transaction: TransactionEntity): Boolean {
        val card = getCreditCard(cardId) ?: return false
        val amount = transaction.amount
        if (!amount.isFinite() || amount <= 0.0 || card.currentBalance < 0.0 || card.creditLimit <= 0.0 ||
            card.currentBalance + amount > card.creditLimit + 0.000001 ||
            card.currencyCode.isNullOrBlank() || transaction.currencyCode != card.currencyCode ||
            transaction.creditCardId != card.id || transaction.loanId != null || transaction.accountId != null ||
            transaction.account != card.cardName || transaction.type != TransactionType.EXPENSE ||
            transaction.transactionKind != TransactionKind.CREDIT_CARD_PURCHASE ||
            transaction.category !in setOf(Category.HOUSING, Category.FOOD_DINING, Category.UTILITIES, Category.TRANSPORT,
                Category.SHOPPING, Category.HEALTHCARE, Category.SUBSCRIPTIONS, Category.ENTERTAINMENT, Category.OTHER)) return false
        val newBalance = card.currentBalance + amount
        if (!newBalance.isFinite()) return false
        updateCreditCard(card.copy(currentBalance = newBalance))
        insertTransaction(transaction)
        return true
    }

    @Transaction
    suspend fun recordLoanTopUp(loanId: Long, accountId: Long, amount: Double, transaction: TransactionEntity): Boolean {
        val loan = getLoan(loanId) ?: return false
        val account = getAccount(accountId) ?: return false
        if (!amount.isFinite() || amount <= 0.0 || loan.remainingBalance < 0.0 || loan.totalAmount <= 0.0 ||
            loan.currencyCode.isNullOrBlank() || !account.isActive || account.currencyCode != loan.currencyCode ||
            transaction.amount != amount || transaction.currencyCode != loan.currencyCode ||
            transaction.accountId != account.id || transaction.account != account.name ||
            transaction.loanId != loan.id || transaction.creditCardId != null ||
            transaction.type != TransactionType.INCOME || transaction.category != Category.LOAN_TOP_UP ||
            transaction.transactionKind != TransactionKind.LOAN_TOP_UP) return false
        val newTotal = loan.totalAmount + amount
        val newBalance = loan.remainingBalance + amount
        if (!newTotal.isFinite() || !newBalance.isFinite()) return false
        updateLoan(loan.copy(totalAmount = newTotal, remainingBalance = newBalance))
        insertTransaction(transaction)
        return true
    }

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    suspend fun getTransactionsSnapshot(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Transaction
    suspend fun insertCashTransaction(transaction: TransactionEntity): Boolean {
        val account = transaction.accountId?.let { getAccount(it) } ?: return false
        if (!account.isActive || account.currencyCode.isNullOrBlank() || account.currencyCode != transaction.currencyCode ||
            !transaction.amount.isFinite() || transaction.amount <= 0.0 || transaction.transactionKind != TransactionKind.STANDARD ||
            transaction.creditCardId != null || transaction.loanId != null) return false
        if (transaction.type == TransactionType.EXPENSE &&
            !AccountLedger.hasSufficientBalance(account, getTransactionsSnapshot(), transaction.amount, transaction.dateMillis + 1L)) return false
        insertTransaction(transaction)
        return true
    }

    @Transaction
    suspend fun updateCashTransaction(transaction: TransactionEntity): Boolean {
        val original = getTransactionById(transaction.id) ?: return false
        val account = transaction.accountId?.let { getAccount(it) } ?: return false
        if (!account.isActive || account.currencyCode.isNullOrBlank() || account.currencyCode != transaction.currencyCode ||
            !transaction.amount.isFinite() || transaction.amount <= 0.0 || transaction.transactionKind != TransactionKind.STANDARD ||
            transaction.creditCardId != null || transaction.loanId != null) return false
        if (transaction.type == TransactionType.EXPENSE) {
            val remainingLedger = getTransactionsSnapshot().filter { it.id != original.id }
            if (!AccountLedger.hasSufficientBalance(account, remainingLedger, transaction.amount, transaction.dateMillis + 1L)) return false
        }
        updateTransaction(transaction)
        return true
    }

    @Transaction
    suspend fun confirmCashTransaction(transaction: TransactionEntity): Boolean {
        val original = getTransactionById(transaction.id) ?: return false
        if (original.importStatus != "PENDING_REVIEW") return false
        val account = transaction.accountId?.let { getAccount(it) } ?: return false
        if (!account.isActive || account.currencyCode.isNullOrBlank() || account.currencyCode != transaction.currencyCode ||
            !transaction.amount.isFinite() || transaction.amount <= 0.0 || transaction.importStatus != "CONFIRMED") return false
        if (transaction.type == TransactionType.EXPENSE &&
            !AccountLedger.hasSufficientBalance(account, getTransactionsSnapshot(), transaction.amount, transaction.dateMillis + 1L)) return false
        updateTransaction(transaction)
        return true
    }

    @Transaction
    suspend fun recordGoalContribution(goalId: Long, transaction: TransactionEntity): Boolean {
        val goal = getGoal(goalId) ?: return false
        val account = transaction.accountId?.let { getAccount(it) } ?: return false
        val amount = transaction.amount
        if (!account.isActive || account.currencyCode.isNullOrBlank() || account.currencyCode != goal.currencyCode ||
            transaction.account != account.name || transaction.currencyCode != account.currencyCode ||
            transaction.category != Category.GOAL_SAVINGS || transaction.type != TransactionType.EXPENSE ||
            transaction.transactionKind != TransactionKind.ASSET_CONVERSION || transaction.creditCardId != null ||
            transaction.loanId != null || !amount.isFinite() || amount <= 0.0 ||
            transaction.sourceReference != "goal-contribution:${goal.id}") return false
        if (!AccountLedger.hasSufficientBalance(account, getTransactionsSnapshot(), amount, transaction.dateMillis + 1L)) return false
        val newCurrentAmount = goal.currentAmount + amount
        if (!newCurrentAmount.isFinite()) return false
        updateGoal(goal.copy(currentAmount = newCurrentAmount))
        insertTransaction(transaction)
        return true
    }

    @Transaction
    suspend fun deleteGoalContribution(transactionId: Long): Boolean {
        val transaction = getTransactionById(transactionId) ?: return false
        val prefix = "goal-contribution:"
        if (!transaction.sourceReference.orEmpty().startsWith(prefix)) return false
        val goalId = transaction.sourceReference!!.removePrefix(prefix).toLongOrNull() ?: return false
        val goal = getGoal(goalId) ?: return false
        if (transaction.category != Category.GOAL_SAVINGS || transaction.type != TransactionType.EXPENSE ||
            transaction.transactionKind != TransactionKind.ASSET_CONVERSION || !transaction.amount.isFinite() || transaction.amount <= 0.0) return false
        updateGoal(goal.copy(currentAmount = (goal.currentAmount - transaction.amount).coerceAtLeast(0.0)))
        deleteTransaction(transaction)
        return true
    }

    @Query("SELECT * FROM transactions WHERE statementFingerprint = :fingerprint LIMIT 1")
    suspend fun findTransactionByFingerprint(fingerprint: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE statementFingerprint = :fingerprint")
    suspend fun hasTransactionWithFingerprint(fingerprint: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE sourceReference = :reference")
    suspend fun hasTransactionWithSourceReference(reference: String): Int

    @Query("DELETE FROM transactions WHERE sourceReference = :reference")
    suspend fun deleteTransactionsBySourceReference(reference: String)

    @Query("SELECT * FROM net_worth_snapshots ORDER BY dayKey ASC")
    fun getAllSnapshots(): Flow<List<NetWorthSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: NetWorthSnapshotEntity)

    @Query("DELETE FROM net_worth_snapshots")
    suspend fun clearAllSnapshots()

    // --- Budgets ---
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()

    @Query("SELECT * FROM budgets")
    suspend fun getBudgetsSnapshot(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<BudgetEntity>)

    @Query("SELECT * FROM net_worth_snapshots")
    suspend fun getNetWorthSnapshotsSnapshot(): List<NetWorthSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshots(snapshots: List<NetWorthSnapshotEntity>)

    @Query("UPDATE transactions SET importStatus = :status WHERE id = :id")
    suspend fun updateImportStatus(id: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // --- Holdings ---
    @Query("SELECT * FROM holdings ORDER BY shares * currentPrice DESC")
    fun getAllHoldings(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings ORDER BY shares * currentPrice DESC")
    suspend fun getHoldingsSnapshot(): List<HoldingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: HoldingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldings(holdings: List<HoldingEntity>)

    @Update
    suspend fun updateHolding(holding: HoldingEntity)

    @Delete
    suspend fun deleteHolding(holding: HoldingEntity)

    // --- SIPs ---
    @Query("SELECT * FROM sips ORDER BY monthlyAmount DESC")
    fun getAllSips(): Flow<List<SipEntity>>

    @Query("SELECT * FROM sips ORDER BY monthlyAmount DESC")
    suspend fun getSipsSnapshot(): List<SipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSip(sip: SipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSips(sips: List<SipEntity>)

    @Update
    suspend fun updateSip(sip: SipEntity)

    @Delete
    suspend fun deleteSip(sip: SipEntity)

    // --- Credit Cards ---
    @Query("SELECT * FROM credit_cards ORDER BY currentBalance DESC")
    fun getAllCreditCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY currentBalance DESC")
    suspend fun getCreditCardsSnapshot(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    suspend fun getCreditCard(id: Long): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCards(cards: List<CreditCardEntity>)

    @Update
    suspend fun updateCreditCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCreditCard(card: CreditCardEntity)

    // --- Loans ---
    @Query("SELECT * FROM loans ORDER BY remainingBalance DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans ORDER BY remainingBalance DESC")
    suspend fun getLoansSnapshot(): List<LoanEntity>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoan(id: Long): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    // --- Goals ---
    @Query("SELECT * FROM goals ORDER BY currentAmount / targetAmount DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY currentAmount / targetAmount DESC")
    suspend fun getGoalsSnapshot(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getGoal(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    // --- Reseed / Clear Utilities ---
    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM holdings")
    suspend fun clearAllHoldings()

    @Query("DELETE FROM sips")
    suspend fun clearAllSips()

    @Query("DELETE FROM credit_cards")
    suspend fun clearAllCreditCards()

    @Query("DELETE FROM loans")
    suspend fun clearAllLoans()

    @Query("DELETE FROM goals")
    suspend fun clearAllGoals()
}
