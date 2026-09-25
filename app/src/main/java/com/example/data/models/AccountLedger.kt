package com.example.data.models

/** Pure account-ledger calculations. Amounts are never combined across currencies. */
object AccountLedger {
    fun signedEffect(transaction: TransactionEntity): Double =
        if (transaction.type == TransactionType.INCOME) transaction.amount else -transaction.amount

    fun currentBalance(account: AccountEntity, transactions: Iterable<TransactionEntity>): Double {
        return balanceAt(account, transactions, Long.MAX_VALUE)
    }

    fun hasSufficientBalance(
        account: AccountEntity,
        transactions: Iterable<TransactionEntity>,
        amount: Double,
        asOfMillis: Long = Long.MAX_VALUE
    ): Boolean {
        if (!amount.isFinite() || amount <= 0.0) return false
        return balanceAt(account, transactions, asOfMillis) + 0.000001 >= amount
    }

    fun balanceAt(account: AccountEntity, transactions: Iterable<TransactionEntity>, asOfMillis: Long): Double {
        if (asOfMillis < account.openingBalanceMillis) return 0.0
        val ledgerEffect = transactions.asSequence()
            .filter { it.accountId == account.id }
            .filter {
                if (account.currencyCode.isNullOrBlank()) it.currencyCode.isNullOrBlank()
                else it.currencyCode.isNullOrBlank() || it.currencyCode == account.currencyCode
            }
            .filter { it.dateMillis >= account.openingBalanceMillis }
            .filter { it.dateMillis < asOfMillis }
            .filter { it.importStatus != "PENDING_REVIEW" && it.importStatus != "IGNORED" }
            .sumOf(::signedEffect)
        val confirmedOpeningBalance = if (account.openingBalanceConfirmed) account.openingBalance else 0.0
        return confirmedOpeningBalance + ledgerEffect
    }

    fun reconciliationAdjustment(accountBalance: Double, statementBalance: Double): Double =
        statementBalance - accountBalance

    fun totalsByCurrency(accounts: Iterable<AccountEntity>, transactions: Iterable<TransactionEntity>): Map<String, Double> =
        accounts.asSequence()
            .filter { it.isActive && !it.currencyCode.isNullOrBlank() }
            .groupBy { it.currencyCode!! }
            .mapValues { (_, rows) -> rows.sumOf { currentBalance(it, transactions) } }
            .filterValues { it != 0.0 }
}

/** No FX conversion is performed here. */
object CurrencyTotals {
    fun sumByCurrency(amounts: Iterable<Pair<String?, Double>>): Map<String, Double> =
        amounts.filter { !it.first.isNullOrBlank() }
            .groupBy { it.first!! }
            .mapValues { (_, values) -> values.sumOf { it.second } }
            .filterValues { it != 0.0 }

    fun isComplete(amounts: Iterable<Pair<String?, Double>>): Boolean =
        amounts.all { it.second == 0.0 || !it.first.isNullOrBlank() }
}
