package com.example.alerts

import com.example.data.dao.FinanceDao
import com.example.data.models.Category
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import java.util.Calendar

/**
 * Recurring SIP debit engine.
 *
 * A SIP only records its very first debit at creation time (see FinanceViewModel.addSip).
 * Everything after that - every subsequent month - is fired from here: whenever this is
 * called (on app open, and periodically in the background via [SipDebitWorker]), any active
 * SIP whose debit day for the current calendar month has arrived, and that hasn't already
 * been charged for this month, gets one EXPENSE transaction recorded (so it shows up as a
 * cash outflow on the Cash Flow tab) and its totalInvested bumped by monthlyAmount.
 *
 * [SipEntity.lastDebitedYearMonth] ("yyyy-MM") is the guard against double-charging the same
 * month, including across multiple triggers (app open + background worker) on the same day.
 */
object SipDebitEngine {

    /**
     * Processes all due SIP debits. Safe to call as often as needed - it is a no-op for any
     * SIP already charged for the current month. Returns the number of debits recorded.
     */
    suspend fun processDueDebits(dao: FinanceDao): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed
        val daysInCurrentMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val currentYearMonth = "%04d-%02d".format(currentYear, currentMonth)

        var debitedCount = 0

        val sips = dao.getSipsSnapshot()
        for (sip in sips) {
            if (!sip.isActive) continue
            if (sip.lastDebitedYearMonth == currentYearMonth) continue

            // Clamp the debit day to the days actually in this month, so a "31st" SIP still
            // fires on the last day of a shorter month (e.g. February) instead of never firing.
            val triggerDay = sip.debitDayOfMonth.coerceIn(1, daysInCurrentMonth)
            if (todayOfMonth < triggerDay) continue

            dao.insertTransaction(
                TransactionEntity(
                    title = "SIP: ${sip.fundName}",
                    amount = sip.monthlyAmount,
                    type = TransactionType.EXPENSE,
                    category = Category.INVESTMENT_SIP,
                    account = "Cash / selected account",
                    note = "Automated recurring SIP debit recorded by Obsidian Wealth",
                    isRecurring = true,
                    sourceReference = "sip:${sip.id}"
                )
            )

            dao.updateSip(
                sip.copy(
                    totalInvested = sip.totalInvested + sip.monthlyAmount,
                    lastDebitedYearMonth = currentYearMonth
                )
            )

            debitedCount++
        }

        return debitedCount
    }
}
