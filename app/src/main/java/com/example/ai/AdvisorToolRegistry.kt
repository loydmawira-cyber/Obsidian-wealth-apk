package com.example.ai

import com.example.data.dao.FinanceDao
import com.example.data.models.TransactionType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.Calendar
import java.util.Locale

/**
 * Read-only advisor tool registry for AI function calling.
 *
 * Rules:
 * - Tools may ONLY read data.
 * - Tools MUST NEVER create, edit, delete, or sync financial records.
 * - Reuses existing Room FinanceDao and data models.
 * - Returns structured JSON-compatible results.
 */
class AdvisorToolRegistry(
    private val dao: FinanceDao,
    private val canAccessPremiumGoals: () -> Boolean = { false }
) {

    /**
     * Tool 1: get_spending_by_category(months: Int)
     * Returns category breakdown of expenses over the given number of months.
     */
    suspend fun getSpendingByCategory(months: Int): JsonObject {
        return try {
            val validMonths = if (months <= 0) 1 else months
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -validMonths)
            val minDate = cal.timeInMillis

            val transactions = dao.getTransactionsSnapshot()
            val filteredExpenses = transactions.filter {
                it.type == TransactionType.EXPENSE && it.dateMillis >= minDate &&
                    (canAccessPremiumGoals() || it.sourceReference?.startsWith("goal-contribution:") != true)
            }

            val grouped = filteredExpenses.groupBy { it.category.name }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
            val total = grouped.values.sum()

            buildJsonObject {
                put("months", validMonths)
                put("totalExpenses", total)
                put("recordCount", filteredExpenses.size)
                putJsonArray("categories") {
                    grouped.forEach { (cat, sum) ->
                        add(buildJsonObject {
                            put("category", cat)
                            put("amount", sum)
                            val pct = if (total > 0) (sum / total * 100.0) else 0.0
                            put("percentage", pct)
                        })
                    }
                }
                put("summary", if (filteredExpenses.isEmpty()) "No recorded expenses in the last $validMonths month(s)." else "Total expenses of $total across ${grouped.size} categories in the last $validMonths month(s).")
            }
        } catch (e: Exception) {
            buildJsonObject {
                put("error", e.message ?: "Failed to retrieve spending by category")
            }
        }
    }

    /**
     * Tool 2: get_debts()
     * Returns list of recorded credit cards and loans with APR, balance, and servicing costs.
     */
    suspend fun getDebts(): JsonObject {
        return try {
            val cards = dao.getCreditCardsSnapshot()
            val loans = dao.getLoansSnapshot()
            val totalCardDebt = cards.sumOf { it.currentBalance }
            val totalLoanDebt = loans.sumOf { it.remainingBalance }

            buildJsonObject {
                put("totalDebt", totalCardDebt + totalLoanDebt)
                put("creditCardCount", cards.size)
                put("loanCount", loans.size)
                putJsonArray("creditCards") {
                    cards.forEach { c ->
                        add(buildJsonObject {
                            put("cardName", c.cardName)
                            put("lastFour", c.lastFour)
                            put("currentBalance", c.currentBalance)
                            put("creditLimit", c.creditLimit)
                            put("apr", c.apr)
                            put("dueDateDays", c.dueDateDays)
                            put("utilizationPercent", c.utilizationPercent)
                        })
                    }
                }
                putJsonArray("loans") {
                    loans.forEach { l ->
                        add(buildJsonObject {
                            put("loanName", l.loanName)
                            put("lender", l.lender)
                            put("totalAmount", l.totalAmount)
                            put("remainingBalance", l.remainingBalance)
                            put("emiAmount", l.emiAmount)
                            put("interestRate", l.interestRate)
                            put("totalMonths", l.totalMonths)
                            put("remainingMonths", l.remainingMonths)
                        })
                    }
                }
                put("summary", "Total outstanding debt: ${totalCardDebt + totalLoanDebt} across ${cards.size} credit card(s) and ${loans.size} loan(s).")
            }
        } catch (e: Exception) {
            buildJsonObject {
                put("error", e.message ?: "Failed to retrieve debts")
            }
        }
    }

    /**
     * Tool 3: get_goals()
     * Returns list of recorded financial goals, progress, target dates, and monthly contributions.
     */
    suspend fun getGoals(): JsonObject {
        if (!canAccessPremiumGoals()) {
            return buildJsonObject { put("error", "Goals are available with Premium.") }
        }
        return try {
            val goals = dao.getGoalsSnapshot()
            val totalSaved = goals.sumOf { it.currentAmount }
            val totalTarget = goals.sumOf { it.targetAmount }

            buildJsonObject {
                put("totalSaved", totalSaved)
                put("totalTarget", totalTarget)
                put("goalCount", goals.size)
                putJsonArray("goals") {
                    goals.forEach { g ->
                        add(buildJsonObject {
                            put("id", g.id)
                            put("title", g.title)
                            put("category", g.category)
                            put("currentAmount", g.currentAmount)
                            put("targetAmount", g.targetAmount)
                            put("progressPercent", g.progressPercent)
                            put("monthlyContribution", g.monthlyContribution)
                            put("targetYear", g.targetYear)
                            put("monthsRemaining", g.monthsRemaining)
                        })
                    }
                }
                put("summary", if (goals.isEmpty()) "No financial goals recorded." else "${goals.size} goal(s) recorded with total saved $totalSaved of $totalTarget.")
            }
        } catch (e: Exception) {
            buildJsonObject {
                put("error", e.message ?: "Failed to retrieve goals")
            }
        }
    }

    /**
     * Tool 4: get_recent_transactions(days: Int)
     * Returns transactions within the last [days] days.
     */
    suspend fun getRecentTransactions(days: Int): JsonObject {
        return try {
            val validDays = if (days <= 0) 7 else days
            val cutoff = System.currentTimeMillis() - (validDays * 86400000L)
            val transactions = dao.getTransactionsSnapshot().filter {
                it.dateMillis >= cutoff &&
                    (canAccessPremiumGoals() || it.sourceReference?.startsWith("goal-contribution:") != true)
            }

            buildJsonObject {
                put("days", validDays)
                put("count", transactions.size)
                putJsonArray("transactions") {
                    transactions.take(30).forEach { tx ->
                        add(buildJsonObject {
                            put("id", tx.id)
                            put("title", tx.title)
                            put("amount", tx.amount)
                            put("type", tx.type.name)
                            put("category", tx.category.name)
                            put("account", tx.account)
                            put("dateMillis", tx.dateMillis)
                        })
                    }
                }
                put("summary", if (transactions.isEmpty()) "No transactions in the last $validDays day(s)." else "Found ${transactions.size} transaction(s) in the last $validDays day(s).")
            }
        } catch (e: Exception) {
            buildJsonObject {
                put("error", e.message ?: "Failed to retrieve recent transactions")
            }
        }
    }

    /**
     * Tool 5: get_portfolio()
     * Returns holdings and active SIPs/standing orders.
     */
    suspend fun getPortfolio(): JsonObject {
        return try {
            val holdings = dao.getHoldingsSnapshot()
            val sips = dao.getSipsSnapshot()
            val totalValue = holdings.sumOf { it.totalValue }
            val totalCost = holdings.sumOf { it.totalCost }
            val totalGain = totalValue - totalCost

            buildJsonObject {
                put("totalPortfolioValue", totalValue)
                put("totalPortfolioCost", totalCost)
                put("unrealizedGain", totalGain)
                put("holdingCount", holdings.size)
                put("sipCount", sips.size)
                putJsonArray("holdings") {
                    holdings.forEach { h ->
                        add(buildJsonObject {
                            put("symbol", h.symbol)
                            put("name", h.name)
                            put("type", h.type.name)
                            put("shares", h.shares)
                            put("avgBuyPrice", h.avgBuyPrice)
                            put("currentPrice", h.currentPrice)
                            put("totalValue", h.totalValue)
                            put("dailyChangePercent", h.dailyChangePercent)
                        })
                    }
                }
                putJsonArray("sips") {
                    sips.forEach { s ->
                        add(buildJsonObject {
                            put("fundName", s.fundName)
                            put("category", s.category)
                            put("monthlyAmount", s.monthlyAmount)
                            put("debitDayOfMonth", s.debitDayOfMonth)
                            put("isActive", s.isActive)
                        })
                    }
                }
                put("summary", "Portfolio contains ${holdings.size} holding(s) (Value: $totalValue, Cost: $totalCost) and ${sips.size} SIP mandate(s).")
            }
        } catch (e: Exception) {
            buildJsonObject {
                put("error", e.message ?: "Failed to retrieve portfolio")
            }
        }
    }

    /**
     * Executes a tool by name with arguments.
     */
    suspend fun executeTool(toolName: String, args: Map<String, JsonElement>? = null): JsonObject {
        return when (toolName.lowercase(Locale.ROOT)) {
            "get_spending_by_category" -> {
                val months = args?.get("months")?.jsonPrimitive?.intOrNull ?: 3
                getSpendingByCategory(months)
            }
            "get_debts" -> getDebts()
            "get_goals" -> getGoals()
            "get_recent_transactions" -> {
                val days = args?.get("days")?.jsonPrimitive?.intOrNull ?: 14
                getRecentTransactions(days)
            }
            "get_portfolio" -> getPortfolio()
            else -> buildJsonObject { put("error", "Unknown tool call: $toolName") }
        }
    }
}
