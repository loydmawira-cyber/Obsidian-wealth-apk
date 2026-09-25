package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME, EXPENSE
}

enum class TransactionKind {
    STANDARD, TRANSFER, DEBT_SETTLEMENT, ADJUSTMENT, ASSET_CONVERSION,
    CREDIT_CARD_PURCHASE, LOAN_TOP_UP
}

enum class HoldingType {
    STOCK, ETF, MUTUAL_FUND, CRYPTO, GOLD
}

enum class Category {
    SALARY, FREELANCE, DIVIDENDS, RENTAL,
    HOUSING, FOOD_DINING, UTILITIES, TRANSPORT,
    SHOPPING, HEALTHCARE, SUBSCRIPTIONS, ENTERTAINMENT,
    INVESTMENT_SIP, INVESTMENT_SALE, LOAN_EMI, LOAN_TOP_UP, DEBT_PAYMENT, ACCOUNT_TRANSFER, ACCOUNT_ADJUSTMENT, OTHER,
    // Money moved into or out of a savings goal: a transfer, not spending.
    GOAL_SAVINGS
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val account: String, // e.g., "Checking (Chase)", "Amex Gold", "Savings (High Yield)"
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val isRecurring: Boolean = false,
    val statementFingerprint: String? = null,
    val importStatus: String = "MANUAL",
    val importSource: String? = null,
    val sourceReference: String? = null,
    val accountId: Long? = null,
    val currencyCode: String? = null,
    val transactionKind: TransactionKind = TransactionKind.STANDARD,
    val transferGroupId: String? = null,
    val creditCardId: Long? = null,
    val loanId: Long? = null
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountType: String = "OTHER",
    val currencyCode: String? = null,
    val openingBalance: Double = 0.0,
    val openingBalanceMillis: Long = System.currentTimeMillis(),
    val statementBalance: Double? = null,
    val lastReconciledMillis: Long? = null,
    val isActive: Boolean = true,
    val openingBalanceConfirmed: Boolean = false
)

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val name: String,
    val type: HoldingType,
    val shares: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val dailyChangePercent: Double,
    val currencyCode: String? = null
) {
    val totalValue: Double get() = shares * currentPrice
    val totalCost: Double get() = shares * avgBuyPrice
    val unrealizedGain: Double get() = totalValue - totalCost
    val unrealizedGainPercent: Double get() = if (totalCost > 0) ((totalValue - totalCost) / totalCost) * 100.0 else 0.0
}

@Entity(tableName = "sips")
data class SipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fundName: String,
    val category: String, // e.g. "Index Fund", "Large Cap Growth", "Dividend Aristocrats"
    val monthlyAmount: Double,
    // Legacy field retained for compatibility; this is a planned date, not an executed debit.
    val debitDayOfMonth: Int,
    val isActive: Boolean = true,
    val totalInvested: Double = 0.0,
    // Legacy field retained for cloud/database compatibility; not shown as realized performance.
    val annualizedReturnPercent: Double = 0.0,
    // Legacy field retained for compatibility. New builds never run an automatic debit engine.
    val lastDebitedYearMonth: String? = null,
    val currencyCode: String? = null
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String,
    val lastFour: String = "4321",
    val currentBalance: Double,
    val creditLimit: Double,
    val apr: Double,
    val dueDateDays: Int, // e.g. Due in 6 days
    val colorHex: String = "#1E293B",
    val currencyCode: String? = null
) {
    val utilizationPercent: Double get() = if (creditLimit > 0) (currentBalance / creditLimit) * 100.0 else 0.0
}

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanName: String,
    val lender: String = "Chase Auto Finance",
    val totalAmount: Double,
    val remainingBalance: Double,
    val emiAmount: Double,
    val interestRate: Double,
    val totalMonths: Int,
    val remainingMonths: Int,
    val dueDayOfMonth: Int = 15,
    val currencyCode: String? = null
) {
    val paidPercent: Double get() = if (totalAmount > 0) ((totalAmount - remainingBalance) / totalAmount) * 100.0 else 0.0
}

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val monthlyContribution: Double,
    val targetYear: Int = 2026,
    val colorHex: String = "#10B981",
    val currencyCode: String? = null
) {
    val progressPercent: Double get() = if (targetAmount > 0) (currentAmount / targetAmount).coerceIn(0.0, 1.0) * 100.0 else 0.0
    val monthsRemaining: Int get() = if (monthlyContribution > 0) {
        val remaining = (targetAmount - currentAmount).coerceAtLeast(0.0)
        (remaining / monthlyContribution).toInt()
    } else 0
}

/** One row per calendar day (yyyy-MM-dd): the latest net worth recorded that day. Powers the trend line and MoM change. */
@Entity(tableName = "net_worth_snapshots")
data class NetWorthSnapshotEntity(
    @PrimaryKey val dayKey: String,
    val netWorth: Double,
    val assets: Double,
    val liabilities: Double,
    val dateMillis: Long,
    // Investment holdings value that day; null on rows saved before this was recorded.
    val investments: Double? = null
)

/** A monthly spending limit for one category. */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: Category,
    val monthlyLimit: Double,
    val currencyCode: String? = null
)
