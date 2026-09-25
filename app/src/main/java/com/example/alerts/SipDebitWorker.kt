package com.example.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Legacy WorkManager worker. New versions cancel its schedule; a run that was already queued
 * succeeds without changing customer financial records.
 */
class SipDebitWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return Result.success()
    }
}
