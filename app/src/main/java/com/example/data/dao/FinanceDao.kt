package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.CreditCardEntity
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.LoanEntity
import com.example.data.models.SipEntity
import com.example.data.models.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    suspend fun getTransactionsSnapshot(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE statementFingerprint = :fingerprint LIMIT 1")
    suspend fun findTransactionByFingerprint(fingerprint: String): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE statementFingerprint = :fingerprint")
    suspend fun hasTransactionWithFingerprint(fingerprint: String): Int

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
