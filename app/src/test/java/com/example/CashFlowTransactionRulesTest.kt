package com.example

import com.example.data.models.CashFlowTransactionRules
import com.example.data.models.Category
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionKind
import com.example.data.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CashFlowTransactionRulesTest {
    @Test
    fun confirmedSipAssetConversionCountsAsMonthlyOutflow() {
        val sip = transaction(
            type = TransactionType.EXPENSE,
            category = Category.INVESTMENT_SIP,
            kind = TransactionKind.ASSET_CONVERSION
        )
        assertTrue(CashFlowTransactionRules.isConfirmedInRange(sip, 100L, 200L))
        assertTrue(CashFlowTransactionRules.countsAsOutflow(sip))
    }

    @Test
    fun investmentSaleAssetConversionIsShownAsInflow() {
        val sale = transaction(
            type = TransactionType.INCOME,
            category = Category.INVESTMENT_SALE,
            kind = TransactionKind.ASSET_CONVERSION
        )
        assertTrue(CashFlowTransactionRules.countsAsInflow(sale))
        assertFalse(CashFlowTransactionRules.countsAsOutflow(sale))
    }

    @Test
    fun categoryBreakdownSumsToOutflowAndIncludesConfirmedSipConversion() {
        val sip = transaction(
            category = Category.INVESTMENT_SIP,
            kind = TransactionKind.ASSET_CONVERSION
        ).copy(amount = 250.0)
        val groceries = transaction(category = Category.FOOD_DINING).copy(amount = 75.0)
        val cardRepayment = transaction(TransactionType.EXPENSE, kind = TransactionKind.DEBT_SETTLEMENT)
            .copy(amount = 40.0, creditCardId = 8L)
        val pending = transaction(category = Category.OTHER, importStatus = "PENDING_REVIEW")
            .copy(amount = 500.0)

        val confirmedInMonth = listOf(sip, groceries, cardRepayment, pending)
            .filter { CashFlowTransactionRules.isConfirmedInRange(it, 100L, 200L) }
        val byCategory = CashFlowTransactionRules.outflowsByCategory(confirmedInMonth)

        assertEquals(325.0, byCategory.values.sum(), 0.0001)
        assertEquals(250.0, byCategory[Category.INVESTMENT_SIP]!!, 0.0001)
        assertEquals(75.0, byCategory[Category.FOOD_DINING]!!, 0.0001)
    }

    @Test
    fun cashFlowDoesNotDoubleCountTransfersAdjustmentsOrCardRepayments() {
        val transfer = transaction(TransactionType.EXPENSE, kind = TransactionKind.TRANSFER)
        val adjustment = transaction(TransactionType.EXPENSE, kind = TransactionKind.ADJUSTMENT)
        val cardRepayment = transaction(TransactionType.EXPENSE, kind = TransactionKind.DEBT_SETTLEMENT).copy(creditCardId = 8L)
        assertFalse(CashFlowTransactionRules.countsAsOutflow(transfer))
        assertFalse(CashFlowTransactionRules.countsAsOutflow(adjustment))
        assertFalse(CashFlowTransactionRules.countsAsOutflow(cardRepayment))
    }

    @Test
    fun pendingIgnoredAndOutOfRangeRowsAreNotIncludedInSelectedMonth() {
        val confirmed = transaction(dateMillis = 150L)
        val pending = transaction(dateMillis = 150L, importStatus = "PENDING_REVIEW")
        val ignored = transaction(dateMillis = 150L, importStatus = "IGNORED")
        val previousMonth = transaction(dateMillis = 99L)
        val nextMonthBoundary = transaction(dateMillis = 200L)
        assertTrue(CashFlowTransactionRules.isConfirmedInRange(confirmed, 100L, 200L))
        assertFalse(CashFlowTransactionRules.isConfirmedInRange(pending, 100L, 200L))
        assertFalse(CashFlowTransactionRules.isConfirmedInRange(ignored, 100L, 200L))
        assertFalse(CashFlowTransactionRules.isConfirmedInRange(previousMonth, 100L, 200L))
        assertFalse(CashFlowTransactionRules.isConfirmedInRange(nextMonthBoundary, 100L, 200L))
    }

    private fun transaction(
        type: TransactionType = TransactionType.EXPENSE,
        category: Category = Category.OTHER,
        kind: TransactionKind = TransactionKind.STANDARD,
        dateMillis: Long = 150L,
        importStatus: String = "MANUAL"
    ) = TransactionEntity(
        title = "test transaction",
        amount = 100.0,
        type = type,
        category = category,
        account = "Checking",
        dateMillis = dateMillis,
        transactionKind = kind,
        importStatus = importStatus
    )
}
