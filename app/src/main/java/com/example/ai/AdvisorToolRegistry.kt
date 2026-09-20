package com.example.ai

import com.example.data.dao.FinanceDao
import com.example.data.models.TransactionType
import java.util.Calendar
import java.util.Locale

/**
 * Read-only advisor tool registry for AI function calling.
 *
 * Rules:
 * - Tools may ONLY read data.
 * - Tools MUST NEVER create, edit, delete, or sync financial records.
 * - Reuses existing Room FinanceDao and data models.
 */
class AdvisorToolRegistry(private val dao: FinanceDao) {

    /**
     * Tool 1: get_spending_by_category(months: Int)
     * Returns category breakdown of expenses over the given number of months.
     */
    suspend fun getSpendingByCategory(months: Int): String {
        return try {
            val validMonths = if (months <= 0) 1 else months
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -validMonths)
            val minDate = cal.timeInMillis

            val transactions = dao.getTransactionsSnapshot()
            val filteredExpenses = transactions.filter {
                it.type == TransactionType.EXPENSE && it.dateMillis >= minDate
            }

            if (filteredExpenses.isEmpty()) {
                return "No recorded expense transactions in the last $validMonths month(s)."
            }

            val grouped = filteredExpenses.groupBy { it.category.name }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val total = grouped.values.sum()
            buildString {
                appendLine("Expense Breakdown ($validMonths month(s) - Total: $total):")
                grouped.forEach { (cat, sum) ->
                    val pct = if (total > 0) (sum / total * 100.0) else 0.0
                    appendLine("  - $cat: ${String.format(Locale.US, "%.2f", sum)} (${String.format(Locale.US, "%.1f", pct)}%)")
                }
            }
        } catch (e: Exception) {
            "Error retrieving spending by category: ${e.message}"
        }
    }

    /**
     * Tool 2: get_debts()
     * Returns list of recorded credit cards and loans with APR, balance, and servicing costs.
     */
    suspend fun getDebts(): String {
        return try {
            val cards = dao.getCreditCardsSnapshot()
            val loans = dao.getLoansSnapshot()

            if (cards.isEmpty() && loans.isEmpty()) {
                return "No credit cards or loan liabilities are recorded in this vault."
            }

            buildString {
                if (cards.isNotEmpty()) {
                    appendLine("Credit Cards (${cards.size}):")
                    cards.forEach { c ->
                        appendLine("  - ${c.cardName} (*${c.lastFour}): Balance ${c.currentBalance}, Limit ${c.creditLimit}, APR ${c.apr}%, Due in ${c.dueDateDays} days")
                    }
                }
                if (loans.isNotEmpty()) {
                    appendLine("Loans (${loans.size}):")
                    loans.forEach { l ->
                        appendLine("  - ${l.loanName} (${l.lender}): Remaining ${l.remainingBalance}, Total ${l.totalAmount}, Interest Rate ${l.interestRate}%, EMI ${l.emiAmount}")
                    }
                }
            }
        } catch (e: Exception) {
            "Error retrieving debts: ${e.message}"
        }
    }

    /**
     * Tool 3: get_goals()
     * Returns list of recorded financial goals, progress, target dates, and monthly contributions.
     */
    suspend fun getGoals(): String {
        return try {
            val goals = dao.getGoalsSnapshot()
            if (goals.isEmpty()) {
                return "No savings goals or milestones are recorded in this vault."
            }

            buildString {
                appendLine("Financial Goals (${goals.size}):")
                goals.forEach { g ->
                    val pct = if (g.targetAmount > 0) (g.currentAmount / g.targetAmount * 100.0) else 0.0
                    appendLine("  - ${g.title} (${g.category}): Saved ${g.currentAmount} / Target ${g.targetAmount} (${String.format(Locale.US, "%.1f", pct)}%), Target Year ${g.targetYear}, Monthly Contribution ${g.monthlyContribution}")
                }
            }
        } catch (e: Exception) {
            "Error retrieving goals: ${e.message}"
        }
    }

    /**
     * Tool 4: get_recent_transactions(days: Int)
     * Returns transactions within the last [days] days.
     */
    suspend fun getRecentTransactions(days: Int): String {
        return try {
            val validDays = if (days <= 0) 7 else days
            val cutoff = System.currentTimeMillis() - (validDays * 86400000L)

            val transactions = dao.getTransactionsSnapshot().filter { it.dateMillis >= cutoff }
            if (transactions.isEmpty()) {
                return "No transactions recorded in the last $validDays day(s)."
            }

            buildString {
                appendLine("Recent Transactions ($validDays day(s) - Count: ${transactions.size}):")
                transactions.take(20).forEach { tx ->
                    appendLine("  - [${tx.type}] ${tx.title}: ${tx.amount} (${tx.category.name}) on ${tx.account}")
                }
                if (transactions.size > 20) {
                    appendLine("  - ... and ${transactions.size - 20} more transactions")
                }
            }
        } catch (e: Exception) {
            "Error retrieving recent transactions: ${e.message}"
        }
    }

    /**
     * Tool 5: get_portfolio()
     * Returns holdings and active SIPs/standing orders.
     */
    suspend fun getPortfolio(): String {
        return try {
            val holdings = dao.getHoldingsSnapshot()
            val sips = dao.getSipsSnapshot()

            if (holdings.isEmpty() && sips.isEmpty()) {
                return "No holdings or SIPs recorded in this portfolio."
            }

            buildString {
                if (holdings.isNotEmpty()) {
                    appendLine("Investment Holdings (${holdings.size}):")
                    holdings.forEach { h ->
                        val valNow = h.shares * h.currentPrice
                        val valCost = h.shares * h.avgBuyPrice
                        appendLine("  - ${h.symbol} (${h.name}): ${h.shares} shares @ ${h.currentPrice} (Value: ${String.format(Locale.US, "%.2f", valNow)}, Cost: ${String.format(Locale.US, "%.2f", valCost)}, Type: ${h.type.name})")
                    }
                }
                if (sips.isNotEmpty()) {
                    appendLine("Active SIPs / Mandates (${sips.size}):")
                    sips.forEach { s ->
                        val status = if (s.isActive) "Active" else "Paused"
                        appendLine("  - ${s.fundName} (${s.category}): Amount ${s.monthlyAmount}/mo, Debit Day ${s.debitDayOfMonth}, Status $status")
                    }
                }
            }
        } catch (e: Exception) {
            "Error retrieving portfolio: ${e.message}"
        }
    }

    /**
     * Executes a tool by name with arguments.
     */
    suspend fun executeTool(toolName: String, argsJson: String): String {
        return when (toolName.lowercase(Locale.ROOT)) {
            "get_spending_by_category" -> {
                val months = parseIntegerArg(argsJson, "months") ?: 3
                getSpendingByCategory(months)
            }
            "get_debts" -> getDebts()
            "get_goals" -> getGoals()
            "get_recent_transactions" -> {
                val days = parseIntegerArg(argsJson, "days") ?: 14
                getRecentTransactions(days)
            }
            "get_portfolio" -> getPortfolio()
            else -> "Unknown tool call: $toolName"
        }
    }

    private fun parseIntegerArg(json: String, paramName: String): Int? {
        val regex = Regex("\"$paramName\"\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val match = regex.find(json)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
}
