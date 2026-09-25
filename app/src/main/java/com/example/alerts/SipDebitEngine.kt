package com.example.alerts

import com.example.data.dao.FinanceDao
/**
 * Deprecated safety barrier for legacy scheduled work. A planned contribution is not evidence
 * that money left an account or arrived in an investment, so this engine deliberately does not
 * write transactions or increase invested balances.
 */
object SipDebitEngine {

    /** Legacy callers may remain queued during an app update; safely record nothing. */
    suspend fun processDueDebits(dao: FinanceDao): Int {
        return 0
    }
}
