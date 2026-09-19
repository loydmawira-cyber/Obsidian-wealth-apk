package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.models.CreditCardEntity
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.LoanEntity
import com.example.data.models.SipEntity
import com.example.data.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val dao: FinanceDao) {

    // Streams
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allHoldings: Flow<List<HoldingEntity>> = dao.getAllHoldings()
    val allSips: Flow<List<SipEntity>> = dao.getAllSips()
    val allCreditCards: Flow<List<CreditCardEntity>> = dao.getAllCreditCards()
    val allLoans: Flow<List<LoanEntity>> = dao.getAllLoans()
    val allGoals: Flow<List<GoalEntity>> = dao.getAllGoals()

    // Transaction Operations
    suspend fun addTransaction(transaction: TransactionEntity): Long = dao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = dao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = dao.deleteTransaction(transaction)

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
    suspend fun payCreditCard(card: CreditCardEntity, paymentAmount: Double) {
        val newBalance = (card.currentBalance - paymentAmount).coerceAtLeast(0.0)
        dao.updateCreditCard(card.copy(currentBalance = newBalance))
    }

    // Loan Operations
    suspend fun addLoan(loan: LoanEntity): Long = dao.insertLoan(loan)
    suspend fun updateLoan(loan: LoanEntity) = dao.updateLoan(loan)
    suspend fun deleteLoan(loan: LoanEntity) = dao.deleteLoan(loan)
    suspend fun payLoanEmi(loan: LoanEntity) {
        val newBalance = (loan.remainingBalance - (loan.emiAmount * 0.88)).coerceAtLeast(0.0)
        val newRemainingMonths = (loan.remainingMonths - 1).coerceAtLeast(0)
        dao.updateLoan(loan.copy(remainingBalance = newBalance, remainingMonths = newRemainingMonths))
    }

    // Goal Operations
    suspend fun addGoal(goal: GoalEntity): Long = dao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goal: GoalEntity) = dao.deleteGoal(goal)
    suspend fun contributeToGoal(goal: GoalEntity, amount: Double) {
        val newAmount = goal.currentAmount + amount
        dao.updateGoal(goal.copy(currentAmount = newAmount))
    }
}
