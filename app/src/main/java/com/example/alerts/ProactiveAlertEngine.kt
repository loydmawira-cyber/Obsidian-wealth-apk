package com.example.alerts

import com.example.data.dao.FinanceDao
import com.example.data.models.TransactionType
import java.util.Calendar
import java.util.Locale

data class AlertItem(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity
)

enum class AlertSeverity {
    INFO, WARNING, HIGH
}

/**
 * Deterministic Proactive Alert Engine.
 * Evaluates recorded financial records to produce real-time alerts.
 */
object ProactiveAlertEngine {

    suspend fun evaluateAlerts(dao: FinanceDao): List<AlertItem> {
        val alerts = mutableListOf<AlertItem>()

        try {
            val transactions = dao.getTransactionsSnapshot()
            val cards = dao.getCreditCardsSnapshot()
            val goals = dao.getGoalsSnapshot()

            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH)

            // Current Month Range
            val startCurrent = Calendar.getInstance().apply {
                set(currentYear, currentMonth, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Previous Month Range
            val prevMonthCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }
            val startPrev = Calendar.getInstance().apply {
                set(prevMonthCal.get(Calendar.YEAR), prevMonthCal.get(Calendar.MONTH), 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val currentMonthTxs = transactions.filter { it.dateMillis >= startCurrent }
            val prevMonthTxs = transactions.filter { it.dateMillis in startPrev until startCurrent }

            val currentInflow = currentMonthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val currentOutflow = currentMonthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val prevOutflow = prevMonthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            // 1. Spending > 20% above previous month
            if (prevOutflow > 0 && currentOutflow > prevOutflow * 1.20) {
                val pct = ((currentOutflow - prevOutflow) / prevOutflow * 100.0)
                alerts.add(
                    AlertItem(
                        id = "spending_spike",
                        title = "Monthly Outflow Surge",
                        message = "Your spending this month is ${String.format(Locale.US, "%.1f", pct)}% higher than last month.",
                        severity = AlertSeverity.WARNING
                    )
                )
            }

            // 2. Unusually high category spending (> 40% of monthly expenses)
            if (currentOutflow > 0) {
                val categoryGrouped = currentMonthTxs
                    .filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                categoryGrouped.forEach { (cat, sum) ->
                    val ratio = sum / currentOutflow
                    if (ratio >= 0.40) {
                        val pct = ratio * 100.0
                        alerts.add(
                            AlertItem(
                                id = "cat_high_${cat.name}",
                                title = "Concentrated Category Spending",
                                message = "${cat.name.replace("_", " ")} accounts for ${String.format(Locale.US, "%.1f", pct)}% of this month's expenses.",
                                severity = AlertSeverity.WARNING
                            )
                        )
                    }
                }
            }

            // 3. Credit cards due within seven days
            cards.forEach { card ->
                if (card.currentBalance > 0 && card.dueDateDays in 1..7) {
                    alerts.add(
                        AlertItem(
                            id = "card_due_${card.id}",
                            title = "Payment Due Soon",
                            message = "${card.cardName} balance of ${card.currentBalance} is due in ${card.dueDateDays} day(s).",
                            severity = AlertSeverity.HIGH
                        )
                    )
                }
            }

            // 4. Goals falling behind
            goals.forEach { goal ->
                if (goal.targetAmount > 0) {
                    val progress = goal.currentAmount / goal.targetAmount
                    if (goal.monthsRemaining <= 3 && progress < 0.50) {
                        alerts.add(
                            AlertItem(
                                id = "goal_behind_${goal.id}",
                                title = "Goal Trailing Velocity",
                                message = "Goal '${goal.title}' is only ${String.format(Locale.US, "%.0f", progress * 100)}% complete with ${goal.monthsRemaining} month(s) remaining.",
                                severity = AlertSeverity.WARNING
                            )
                        )
                    }
                }
            }

            // 5. Negative monthly cash flow
            if (currentInflow > 0 && currentOutflow > currentInflow) {
                val deficit = currentOutflow - currentInflow
                alerts.add(
                    AlertItem(
                        id = "negative_cash_flow",
                        title = "Negative Monthly Net Cash",
                        message = "Monthly expenses exceed income by ${String.format(Locale.US, "%.2f", deficit)}.",
                        severity = AlertSeverity.HIGH
                    )
                )
            }

        } catch (e: Exception) {
            android.util.Log.e("ProactiveAlertEngine", "Error evaluating alerts", e)
        }

        return alerts
    }
}
