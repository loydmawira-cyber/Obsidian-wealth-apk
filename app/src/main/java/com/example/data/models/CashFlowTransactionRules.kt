package com.example.data.models

/** Shared classification for the selected-month cash-flow totals and category breakdown. */
object CashFlowTransactionRules {
    fun isConfirmed(transaction: TransactionEntity): Boolean =
        transaction.importStatus != "PENDING_REVIEW" && transaction.importStatus != "IGNORED"

    fun isConfirmedInRange(transaction: TransactionEntity, monthStart: Long, monthEnd: Long): Boolean =
        isConfirmed(transaction) &&
            transaction.dateMillis >= monthStart &&
            transaction.dateMillis < monthEnd

    /** Internal transfers and balance adjustments are reported separately, not as operating cash flow. */
    fun countsAsInflow(transaction: TransactionEntity): Boolean =
        transaction.type == TransactionType.INCOME &&
            transaction.transactionKind !in setOf(TransactionKind.TRANSFER, TransactionKind.ADJUSTMENT)

    /**
     * Includes confirmed investment/goal asset conversions as outflows from cash.
     * A credit-card repayment is excluded because the original card purchase is already counted.
     */
    fun countsAsOutflow(transaction: TransactionEntity): Boolean =
        transaction.type == TransactionType.EXPENSE &&
            transaction.transactionKind !in setOf(TransactionKind.TRANSFER, TransactionKind.ADJUSTMENT) &&
            !(transaction.transactionKind == TransactionKind.DEBT_SETTLEMENT && transaction.creditCardId != null)

    fun outflowsByCategory(transactions: Iterable<TransactionEntity>): Map<Category, Double> =
        transactions.asSequence()
            .filter(::countsAsOutflow)
            .groupBy { it.category }
            .mapValues { (_, rows) -> rows.sumOf { it.amount } }
}
