package com.example

import com.example.data.models.AccountEntity
import com.example.data.models.AccountLedger
import com.example.data.models.CurrencyTotals
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionKind
import com.example.data.models.TransactionType
import com.example.data.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountLedgerTest {
    @Test
    fun currentBalanceIncludesOnlyLinkedConfirmedTransactionsOnOrAfterOpeningDate() {
        val account = AccountEntity(
            id = 4,
            name = "Everyday",
            currencyCode = "KES",
            openingBalance = 1_000.0,
            openingBalanceMillis = 100L,
            openingBalanceConfirmed = true
        )
        val rows = listOf(
            tx(4, 200L, 500.0, TransactionType.INCOME),
            tx(4, 300L, 250.0, TransactionType.EXPENSE),
            tx(4, 400L, 90.0, TransactionType.EXPENSE, status = "PENDING_REVIEW"),
            tx(4, 450L, 700.0, TransactionType.INCOME).copy(currencyCode = "USD"),
            tx(9, 400L, 2_000.0, TransactionType.INCOME),
            tx(4, 50L, 7_000.0, TransactionType.INCOME)
        )
        assertEquals(1_250.0, AccountLedger.currentBalance(account, rows), 0.0001)
    }

    @Test
    fun reconciliationDifferenceIsStatementLessLedgerBalance() {
        assertEquals(-125.5, AccountLedger.reconciliationAdjustment(1_000.0, 874.5), 0.0001)
        assertEquals(125.5, AccountLedger.reconciliationAdjustment(874.5, 1_000.0), 0.0001)
    }

    @Test
    fun openingBalanceIsAvailableAtOpeningTimestampAndEarlierSnapshotIsZero() {
        val account = AccountEntity(
            id = 8, name = "New account", currencyCode = "USD", openingBalance = 300.0,
            openingBalanceMillis = 100L, openingBalanceConfirmed = true
        )
        val openingInstantTransaction = tx(8, 100L, 25.0, TransactionType.EXPENSE).copy(currencyCode = "USD")

        assertEquals(0.0, AccountLedger.balanceAt(account, listOf(openingInstantTransaction), 99L), 0.0001)
        assertEquals(300.0, AccountLedger.balanceAt(account, listOf(openingInstantTransaction), 100L), 0.0001)
        assertEquals(275.0, AccountLedger.balanceAt(account, listOf(openingInstantTransaction), 101L), 0.0001)
    }

    @Test
    fun unconfirmedLegacyOpeningAmountIsExcludedUntilReviewed() {
        val unreviewed = AccountEntity(
            id = 7, name = "Legacy starting balance", currencyCode = "USD", openingBalance = 2_500.0,
            openingBalanceMillis = 0L, openingBalanceConfirmed = false
        )
        val reviewed = unreviewed.copy(openingBalanceConfirmed = true)

        assertEquals(0.0, AccountLedger.currentBalance(unreviewed, emptyList()), 0.0001)
        assertEquals(2_500.0, AccountLedger.currentBalance(reviewed, emptyList()), 0.0001)
    }

    @Test
    fun transferKindsDoNotChangeCurrencyArithmetic() {
        val outgoing = tx(1, 1L, 75.0, TransactionType.EXPENSE).copy(
            transactionKind = TransactionKind.TRANSFER,
            category = Category.ACCOUNT_TRANSFER,
            transferGroupId = "pair-1"
        )
        val incoming = tx(2, 1L, 75.0, TransactionType.INCOME).copy(
            transactionKind = TransactionKind.TRANSFER,
            category = Category.ACCOUNT_TRANSFER,
            transferGroupId = "pair-1"
        )
        assertEquals(-75.0, AccountLedger.signedEffect(outgoing), 0.0001)
        assertEquals(75.0, AccountLedger.signedEffect(incoming), 0.0001)
        assertEquals(0.0, AccountLedger.signedEffect(outgoing) + AccountLedger.signedEffect(incoming), 0.0001)
    }

    @Test
    fun currencyTotalsNeverCombineCurrenciesAndDetectUnknowns() {
        val totals = CurrencyTotals.sumByCurrency(listOf("KES" to 100.0, "KES" to -25.0, "USD" to 50.0))
        assertEquals(75.0, totals["KES"]!!, 0.0001)
        assertEquals(50.0, totals["USD"]!!, 0.0001)
        assertEquals(2, totals.size)
        assertFalse(CurrencyTotals.isComplete(listOf("KES" to 100.0, null to 20.0)))
        assertTrue(CurrencyTotals.isComplete(listOf("KES" to 100.0, null to 0.0)))
    }

    @Test
    fun pairedSameCurrencyTransferMovesValueBetweenAccountsWithoutChangingCurrencyTotal() {
        val checking = AccountEntity(
            id = 1, name = "Checking", currencyCode = "KES", openingBalance = 500.0,
            openingBalanceMillis = 0L, openingBalanceConfirmed = true
        )
        val savings = AccountEntity(
            id = 2, name = "Savings", currencyCode = "KES", openingBalance = 100.0,
            openingBalanceMillis = 0L, openingBalanceConfirmed = true
        )
        val out = tx(1, 10L, 125.0, TransactionType.EXPENSE).copy(transactionKind = TransactionKind.TRANSFER)
        val into = tx(2, 10L, 125.0, TransactionType.INCOME).copy(transactionKind = TransactionKind.TRANSFER)

        assertEquals(375.0, AccountLedger.currentBalance(checking, listOf(out, into)), 0.0001)
        assertEquals(225.0, AccountLedger.currentBalance(savings, listOf(out, into)), 0.0001)
        val totals = AccountLedger.totalsByCurrency(listOf(checking, savings), listOf(out, into))
        assertEquals(600.0, totals["KES"]!!, 0.0001)
    }

    @Test
    fun accountTotalsRemainSeparatedAcrossCurrenciesAndIgnoreInactiveAccounts() {
        val accounts = listOf(
            AccountEntity(id = 1, name = "KES cash", currencyCode = "KES", openingBalance = 1_000.0, openingBalanceMillis = 0L, openingBalanceConfirmed = true),
            AccountEntity(id = 2, name = "USD cash", currencyCode = "USD", openingBalance = 50.0, openingBalanceMillis = 0L, openingBalanceConfirmed = true),
            AccountEntity(id = 3, name = "Closed", currencyCode = "KES", openingBalance = 900.0, openingBalanceMillis = 0L, isActive = false, openingBalanceConfirmed = true),
            AccountEntity(id = 4, name = "Unresolved", currencyCode = null, openingBalance = 400.0, openingBalanceMillis = 0L, openingBalanceConfirmed = true)
        )

        val totals = AccountLedger.totalsByCurrency(accounts, emptyList())

        assertEquals(mapOf("KES" to 1_000.0, "USD" to 50.0), totals)
    }

    @Test
    fun reconciliationAdjustmentCanRepresentEitherDirectionWithoutMutatingPriorLedgerRows() {
        val prior = tx(1, 10L, 200.0, TransactionType.EXPENSE)
        val original = listOf(prior)
        val adjustment = AccountLedger.reconciliationAdjustment(800.0, 925.0)
        val correction = tx(1, 20L, kotlin.math.abs(adjustment), TransactionType.INCOME)
            .copy(transactionKind = TransactionKind.ADJUSTMENT, category = Category.ACCOUNT_ADJUSTMENT)

        assertEquals(125.0, adjustment, 0.0001)
        assertEquals(925.0, AccountLedger.currentBalance(
            AccountEntity(id = 1, name = "Checking", currencyCode = "KES", openingBalance = 1_000.0, openingBalanceMillis = 0L, openingBalanceConfirmed = true),
            original + correction
        ), 0.0001)
        assertEquals(1, original.size)
    }

    private fun tx(accountId: Long, date: Long, amount: Double, type: TransactionType, status: String = "MANUAL") =
        TransactionEntity(
            id = date,
            title = "Test",
            amount = amount,
            type = type,
            category = Category.OTHER,
            account = "Account $accountId",
            dateMillis = date,
            accountId = accountId,
            currencyCode = "KES",
            importStatus = status
        )
}
