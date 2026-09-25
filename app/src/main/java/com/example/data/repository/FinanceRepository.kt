package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.models.CreditCardEntity
import com.example.data.models.AccountEntity
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.LoanEntity
import com.example.data.models.BudgetEntity
import com.example.data.models.NetWorthSnapshotEntity
import com.example.data.models.SipEntity
import com.example.data.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val dao: FinanceDao) {

    val allAccounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    suspend fun accountsSnapshot(): List<AccountEntity> = dao.getAccountsSnapshot()
    suspend fun account(id: Long): AccountEntity? = dao.getAccount(id)
    suspend fun addAccount(account: AccountEntity): Long = dao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = dao.updateAccount(account)
    suspend fun recordTransfer(source: TransactionEntity, destination: TransactionEntity): Boolean = dao.recordTransfer(source, destination)
    suspend fun recordReconciliation(account: AccountEntity, adjustment: TransactionEntity) = dao.recordReconciliation(account, adjustment)
    suspend fun recordCreditCardPurchase(cardId: Long, transaction: TransactionEntity): Boolean =
        dao.recordCreditCardPurchase(cardId, transaction)
    suspend fun recordLoanTopUp(loanId: Long, accountId: Long, amount: Double, transaction: TransactionEntity): Boolean =
        dao.recordLoanTopUp(loanId, accountId, amount, transaction)

    // Streams
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allHoldings: Flow<List<HoldingEntity>> = dao.getAllHoldings()
    val allSips: Flow<List<SipEntity>> = dao.getAllSips()
    val allCreditCards: Flow<List<CreditCardEntity>> = dao.getAllCreditCards()
    val allLoans: Flow<List<LoanEntity>> = dao.getAllLoans()
    suspend fun creditCardsSnapshot(): List<CreditCardEntity> = dao.getCreditCardsSnapshot()
    suspend fun loansSnapshot(): List<LoanEntity> = dao.getLoansSnapshot()
    val allGoals: Flow<List<GoalEntity>> = dao.getAllGoals()

    val allSnapshots: Flow<List<NetWorthSnapshotEntity>> = dao.getAllSnapshots()
    suspend fun recordSnapshot(snapshot: NetWorthSnapshotEntity) = dao.upsertSnapshot(snapshot)

    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    suspend fun saveBudget(budget: BudgetEntity) = dao.upsertBudget(budget)
    suspend fun deleteBudget(budget: BudgetEntity) = dao.deleteBudget(budget)

    // Transaction Operations
    suspend fun deleteTransactionsBySourceReference(reference: String) = dao.deleteTransactionsBySourceReference(reference)
    suspend fun addTransaction(transaction: TransactionEntity): Long = dao.insertTransaction(transaction)
    suspend fun addCashTransaction(transaction: TransactionEntity): Boolean = dao.insertCashTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = dao.updateTransaction(transaction)
    suspend fun updateCashTransaction(transaction: TransactionEntity): Boolean = dao.updateCashTransaction(transaction)
    suspend fun confirmCashTransaction(transaction: TransactionEntity): Boolean = dao.confirmCashTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)
    suspend fun isFingerprintImported(fingerprint: String): Boolean = dao.hasTransactionWithFingerprint(fingerprint) > 0
    suspend fun getTransactionByFingerprint(fingerprint: String): TransactionEntity? = dao.findTransactionByFingerprint(fingerprint)
    suspend fun hasTransactionWithSourceReference(reference: String): Boolean = dao.hasTransactionWithSourceReference(reference) > 0
    suspend fun updateImportStatus(id: Long, status: String) = dao.updateImportStatus(id, status)

    // Holding Operations
    suspend fun addHolding(holding: HoldingEntity): Long = dao.insertHolding(holding)
    suspend fun updateHolding(holding: HoldingEntity) = dao.updateHolding(holding)
    suspend fun deleteHolding(holding: HoldingEntity) = dao.deleteHolding(holding)

    // SIP Operations
    suspend fun addSip(sip: SipEntity): Long = dao.insertSip(sip)
    suspend fun updateSip(sip: SipEntity) = dao.updateSip(sip)
    suspend fun deleteSip(sip: SipEntity) = dao.deleteSip(sip)
    suspend fun toggleSipActive(sip: SipEntity) {
        dao.updateSip(sip.copy(isActive = !sip.isActive))
    }

    // Credit Card Operations
    suspend fun addCreditCard(card: CreditCardEntity): Long = dao.insertCreditCard(card)
    suspend fun updateCreditCard(card: CreditCardEntity) = dao.updateCreditCard(card)
    suspend fun deleteCreditCard(card: CreditCardEntity) = dao.deleteCreditCard(card)
    suspend fun payCreditCard(card: CreditCardEntity, paymentAmount: Double, transaction: TransactionEntity): Double {
        val actualPayment = paymentAmount.coerceAtLeast(0.0).coerceAtMost(card.currentBalance)
        if (!actualPayment.isFinite() || actualPayment <= 0.0) return 0.0
        val recorded = dao.recordCreditCardSettlement(
            card.copy(currentBalance = (card.currentBalance - actualPayment).coerceAtLeast(0.0)),
            transaction.copy(amount = actualPayment)
        )
        return if (recorded) actualPayment else 0.0
    }

    // Loan Operations
    suspend fun addLoan(loan: LoanEntity): Long = dao.insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = dao.updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) = dao.deleteLoan(loan)
    suspend fun payLoanEmi(loan: LoanEntity, transaction: TransactionEntity): Double {
        if (loan.remainingBalance <= 0.0 || loan.remainingMonths <= 0) return 0.0
        val monthlyInterest = loan.remainingBalance * (loan.interestRate.coerceAtLeast(0.0) / 100.0) / 12.0
        val paymentAmount = loan.emiAmount.coerceAtLeast(0.0)
            .coerceAtMost(loan.remainingBalance + monthlyInterest)
        if (!paymentAmount.isFinite() || paymentAmount <= 0.0) return 0.0
        // Interest is covered first. If the installment does not cover it, do not pretend
        // principal fell or the payoff schedule advanced.
        val principalPaid = (paymentAmount - monthlyInterest).coerceIn(0.0, loan.remainingBalance)
        val newBalance = (loan.remainingBalance - principalPaid).coerceAtLeast(0.0)
        val newRemainingMonths = if (principalPaid > 0.0) (loan.remainingMonths - 1).coerceAtLeast(0) else loan.remainingMonths
        val recorded = dao.recordLoanSettlement(
            loan.copy(remainingBalance = newBalance, remainingMonths = newRemainingMonths),
            transaction.copy(amount = paymentAmount)
        )
        return if (recorded) paymentAmount else 0.0
    }

    // Goal Operations
    suspend fun addGoal(goal: GoalEntity): Long = dao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goal: GoalEntity) = dao.deleteGoal(goal)
    suspend fun recordGoalContribution(goalId: Long, transaction: TransactionEntity): Boolean =
        dao.recordGoalContribution(goalId, transaction)
    suspend fun deleteGoalContribution(transactionId: Long): Boolean =
        dao.deleteGoalContribution(transactionId)
    suspend fun contributeToGoal(goal: GoalEntity, amount: Double) {
        val newAmount = goal.currentAmount + amount
        dao.updateGoal(goal.copy(currentAmount = newAmount))
    }
}
