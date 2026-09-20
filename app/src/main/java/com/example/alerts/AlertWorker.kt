package com.example.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.repository.PreferencesManager

/**
 * Daily/Periodic WorkManager Worker executing the Proactive Alert Engine and Reminders.
 * Posts local Android notifications when permissions are granted and enabled in settings.
 */
class AlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val preferencesManager = PreferencesManager(appContext)
            val settings = preferencesManager.settings.value

            if (!settings.enableNotifications) {
                return Result.success()
            }

            val database = AppDatabase.getDatabase(appContext, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
            val dao = database.financeDao()
            val alerts = ProactiveAlertEngine.evaluateAlerts(dao)

            if (alerts.isNotEmpty()) {
                alerts.take(3).forEachIndexed { idx, alert ->
                    NotificationReminderManager.sendAlertNotification(appContext, alert, 1001 + idx)
                }
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("AlertWorker", "Alert worker execution error", e)
            Result.retry()
        }
    }
}
