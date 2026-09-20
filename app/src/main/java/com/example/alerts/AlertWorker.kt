package com.example.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase

/**
 * Daily WorkManager Worker executing the Proactive Alert Engine.
 * Posts local Android notifications when permissions are granted.
 */
class AlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(appContext, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
            val dao = database.financeDao()
            val alerts = ProactiveAlertEngine.evaluateAlerts(dao)

            if (alerts.isNotEmpty()) {
                sendNotifications(alerts)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("AlertWorker", "Alert worker execution error", e)
            Result.retry()
        }
    }

    private fun sendNotifications(alerts: List<AlertItem>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = "obsidian_proactive_alerts_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Obsidian Financial Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Proactive wealth insights and critical liquidity warnings"
            }
            notificationManager.createNotificationChannel(channel)
        }

        alerts.take(3).forEachIndexed { idx, alert ->
            val builder = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(alert.title)
                .setContentText(alert.message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(1001 + idx, builder.build())
        }
    }
}
