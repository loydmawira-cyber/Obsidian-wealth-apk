package com.example.ui.components

import com.example.data.models.AccountEntity
import com.example.data.models.TransactionEntity

/** Small icon that tells accounts apart when they all carry the same name: 🏠 bank, 📱 mobile money. */
fun accountTypeIcon(accountType: String): String? = when (accountType.trim().uppercase().replace(' ', '_')) {
    "BANK" -> "🏠"
    "MOBILE_MONEY" -> "📱"
    "CASH" -> "💵"
    "GOALS_RESERVE" -> "🎯"
    else -> null
}

/** Account name with its type icon in front, for chips, lists and rows. */
fun AccountEntity.displayName(): String = accountTypeIcon(accountType)?.let { "$it $name" } ?: name

/** The account name shown on a transaction row, with the icon of the account it belongs to. */
fun transactionAccountLabel(transaction: TransactionEntity, accounts: List<AccountEntity>): String {
    val account = accounts.firstOrNull { it.id == transaction.accountId && transaction.accountId != null }
        ?: accounts.firstOrNull { it.name.equals(transaction.account.trim(), ignoreCase = true) }
    return account?.displayName() ?: transaction.account
}
