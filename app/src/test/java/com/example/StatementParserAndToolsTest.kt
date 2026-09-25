package com.example

import com.example.ai.AdvisorToolRegistry
import com.example.ai.GeminiClient
import com.example.data.dao.FinanceDao
import com.example.data.models.AccountEntity
import com.example.data.models.Category
import com.example.data.models.CreditCardEntity
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.HoldingType
import com.example.data.models.LoanEntity
import com.example.data.models.SipEntity
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import com.example.data.util.StatementParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatementParserAndToolsTest {

    @Test
    fun testStatementFingerprintDeterminism() {
        val fp1 = StatementParser.generateFingerprint(1710000000000L, 4500.0, "Carrefour Supermarket", TransactionType.EXPENSE)
        val fp2 = StatementParser.generateFingerprint(1710000000000L, 4500.0, "  carrefour supermarket  ", TransactionType.EXPENSE)
        assertEquals(fp1, fp2)
        assertEquals(64, fp1.length) // SHA-256 hex string length
    }

    @Test
    fun testParseDateStringStrict() {
        val d1 = StatementParser.parseDateString("2026-05-12")
        assertNotNull(d1)

        val d2 = StatementParser.parseDateString("12/05/2026")
        assertNotNull(d2)

        val d3 = StatementParser.parseDateString("12-May-2026")
        assertNotNull(d3)

        // Invalid date should return null
        val dInvalid = StatementParser.parseDateString("not a date string")
        assertNull(dInvalid)
    }

    @Test
    fun testParseStatementCsvAndAmbiguousDates() {
        val csv = """
            Date,Description,Amount,Account
            2026-01-15,Supermarket Grocery,4500.00,M-PESA
            ,Coffee Shop,350.00,Cash
        """.trimIndent()

        val parsed = StatementParser.parseStatementText(csv)
        assertEquals(2, parsed.size)

        val first = parsed[0]
        assertEquals("Supermarket Grocery", first.title)
        assertEquals(4500.00, first.amount, 0.001)
        assertFalse(first.dateAmbiguous)
        assertTrue(first.dateMillis > 0L)

        val second = parsed[1]
        assertEquals("Coffee Shop", second.title)
        assertEquals(350.00, second.amount, 0.001)
        assertTrue(second.dateAmbiguous)
        assertEquals(0L, second.dateMillis)
    }

    @Test
    fun testIsCurrentRateQuery() {
        assertTrue(GeminiClient.isCurrentRateQuery("What is the current T-bill yield in Kenya?"))
        assertTrue(GeminiClient.isCurrentRateQuery("Check latest interest rate for money-market rate"))
        assertFalse(GeminiClient.isCurrentRateQuery("How much did I spend on groceries last month?"))
    }

    @Test
    fun testParseNaturalLanguageTransaction() = runBlocking {
        val res = GeminiClient.parseNaturalLanguageTransaction("Paid KSh 4,500 for groceries at Carrefour via M-Pesa on 2026-04-10")
        assertNotNull(res)
        assertEquals(4500.0, res!!.amount, 0.001)
        assertEquals("FOOD_DINING", res.category)
        assertEquals("M-PESA", res.account)
        assertEquals("EXPENSE", res.type)
        assertNotNull(res.dateMillis)
        assertFalse(res.dateNeedsReview)
    }

    @Test
    fun testAdvisorToolRegistryStructuredOutput() = runBlocking {
        val fakeDao = object : FinanceDao {
            override fun getAllAccounts(): Flow<List<AccountEntity>> = flowOf(emptyList())
            override suspend fun getAccountsSnapshot(): List<AccountEntity> = emptyList()
            override suspend fun getAccount(id: Long): AccountEntity? = null
            override suspend fun insertAccount(account: AccountEntity): Long = 1L
            override suspend fun insertAccounts(accounts: List<AccountEntity>) {}
            override suspend fun updateAccount(account: AccountEntity) {}
            override suspend fun resolveTransactionCurrency(accountId: Long, currencyCode: String) {}
            override suspend fun clearAllAccounts() {}
            override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(emptyList())
            override suspend fun getTransactionsSnapshot(): List<TransactionEntity> = listOf(
                TransactionEntity(id = 1, title = "Groceries", amount = 1200.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "M-PESA", dateMillis = System.currentTimeMillis()),
                TransactionEntity(id = 2, title = "Fuel", amount = 3000.0, type = TransactionType.EXPENSE, category = Category.TRANSPORT, account = "NCBA", dateMillis = System.currentTimeMillis())
            )
            override suspend fun insertTransaction(transaction: TransactionEntity): Long = 1L
            override suspend fun insertTransactions(transactions: List<TransactionEntity>) {}
            override suspend fun updateTransaction(transaction: TransactionEntity) {}
            override suspend fun deleteTransaction(transaction: TransactionEntity) {}
            override suspend fun clearAllTransactions() {}
            override suspend fun findTransactionByFingerprint(fingerprint: String): TransactionEntity? = null
            override suspend fun hasTransactionWithFingerprint(fingerprint: String): Int = 0

            override fun getAllHoldings(): Flow<List<HoldingEntity>> = flowOf(emptyList())
            override suspend fun getHoldingsSnapshot(): List<HoldingEntity> = listOf(
                HoldingEntity(id = 1, symbol = "SCOM", name = "Safaricom PLC", type = HoldingType.STOCK, shares = 1000.0, avgBuyPrice = 18.5, currentPrice = 22.0, dailyChangePercent = 1.2)
            )
            override suspend fun insertHolding(holding: HoldingEntity): Long = 1L
            override suspend fun insertHoldings(holdings: List<HoldingEntity>) {}
            override suspend fun updateHolding(holding: HoldingEntity) {}
            override suspend fun deleteHolding(holding: HoldingEntity) {}
            override suspend fun clearAllHoldings() {}

            override fun getAllSips(): Flow<List<SipEntity>> = flowOf(emptyList())
            override suspend fun getSipsSnapshot(): List<SipEntity> = emptyList()
            override suspend fun insertSip(sip: SipEntity): Long = 1L
            override suspend fun insertSips(sips: List<SipEntity>) {}
            override suspend fun updateSip(sip: SipEntity) {}
            override suspend fun deleteSip(sip: SipEntity) {}
            override suspend fun clearAllSips() {}

            override fun getAllCreditCards(): Flow<List<CreditCardEntity>> = flowOf(emptyList())
            override suspend fun getCreditCardsSnapshot(): List<CreditCardEntity> = listOf(
                CreditCardEntity(id = 1, cardName = "NCBA Platinum", lastFour = "9876", creditLimit = 500000.0, currentBalance = 45000.0, apr = 14.0, dueDateDays = 12)
            )
            override suspend fun insertCreditCard(card: CreditCardEntity): Long = 1L
            override suspend fun insertCreditCards(cards: List<CreditCardEntity>) {}
            override suspend fun updateCreditCard(card: CreditCardEntity) {}
            override suspend fun deleteCreditCard(card: CreditCardEntity) {}
            override suspend fun clearAllCreditCards() {}

            override fun getAllLoans(): Flow<List<LoanEntity>> = flowOf(emptyList())
            override suspend fun getLoansSnapshot(): List<LoanEntity> = emptyList()
            override suspend fun insertLoan(loan: LoanEntity): Long = 1L
            override suspend fun insertLoans(loans: List<LoanEntity>) {}
            override suspend fun updateLoan(loan: LoanEntity) {}
            override suspend fun deleteLoan(loan: LoanEntity) {}
            override suspend fun clearAllLoans() {}

            override fun getAllGoals(): Flow<List<GoalEntity>> = flowOf(emptyList())
            override suspend fun getGoalsSnapshot(): List<GoalEntity> = listOf(
                GoalEntity(id = 1, title = "Emergency Fund", targetAmount = 500000.0, currentAmount = 250000.0, category = "Emergency", targetYear = 2026, monthlyContribution = 20000.0)
            )
            override suspend fun insertGoal(goal: GoalEntity): Long = 1L
            override suspend fun insertGoals(goals: List<GoalEntity>) {}
            override suspend fun updateGoal(goal: GoalEntity) {}
            override suspend fun deleteGoal(goal: GoalEntity) {}
            override suspend fun clearAllGoals() {}
        }

        val registry = AdvisorToolRegistry(fakeDao)

        val spending = registry.getSpendingByCategory(1)
        assertEquals(4200.0, spending["totalExpenses"]?.jsonPrimitive?.double ?: 0.0, 0.001)
        assertEquals(2, spending["recordCount"]?.jsonPrimitive?.int)

        val debts = registry.getDebts()
        assertEquals(45000.0, debts["totalDebt"]?.jsonPrimitive?.double ?: 0.0, 0.001)
        assertEquals(1, debts["creditCardCount"]?.jsonPrimitive?.int)

        val goals = registry.getGoals()
        assertEquals(250000.0, goals["totalSaved"]?.jsonPrimitive?.double ?: 0.0, 0.001)
        assertEquals(1, goals["goalCount"]?.jsonPrimitive?.int)

        val portfolio = registry.getPortfolio()
        assertEquals(22000.0, portfolio["totalPortfolioValue"]?.jsonPrimitive?.double ?: 0.0, 0.001)
        assertEquals(1, portfolio["holdingCount"]?.jsonPrimitive?.int)
    }
}
