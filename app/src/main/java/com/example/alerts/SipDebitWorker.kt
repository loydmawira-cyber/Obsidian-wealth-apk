package com.example.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Periodic WorkManager worker that runs [SipDebitEngine] in the background so monthly SIP
 * debits are recorded even on days the app is never opened.
 */
class SipDebitWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(appContext, CoroutineScope(Dispatchers.IO))
            SipDebitEngine.processDueDebits(database.financeDao())
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SipDebitWorker", "SIP debit worker execution error", e)
            Result.retry()
        }
    }
}
