package com.example.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MainActivity
import com.example.data.models.UserSettings
import java.util.concurrent.TimeUnit

/**
 * Enterprise-grade Notification & Reminders Manager for Obsidian Wealth.
 * Handles proactive alerts, bill reminders, SIP standing order notifications,
 * and daily financial briefings.
 */
object NotificationReminderManager {

    const val CHANNEL_ALERTS = "obsidian_wealth_alerts_channel"
    const val CHANNEL_REMINDERS = "obsidian_reminders_channel"
    const val CHANNEL_BRIEFINGS = "obsidian_briefings_channel"

    private const val WORK_NAME_PERIODIC_ALERTS = "obsidian_proactive_alerts"
    private const val WORK_NAME_SIP_DEBITS = "obsidian_sip_debit_engine"

    /**
     * Initializes all required notification channels.
     */
    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Critical Alerts & Wealth Risk Channel
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Obsidian Financial Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts for spending surges, cash deficits, and risk triggers"
                enableLights(true)
                lightColor = AndroidColor.RED
                enableVibration(true)
            }

            // 2. Bill & Payment Reminders Channel
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Bill & Debt Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Upcoming credit card due dates, loan EMIs, and SIP standing orders"
                enableLights(true)
                lightColor = AndroidColor.rgb(202, 160, 55) // Gold
            }

            // 3. Daily / Weekly Briefing Channel
            val briefingsChannel = NotificationChannel(
                CHANNEL_BRIEFINGS,
                "Daily Wealth Briefing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily portfolio updates and goal milestone velocity"
            }

            notificationManager.createNotificationChannels(listOf(alertsChannel, remindersChannel, briefingsChannel))
        }
    }

    /**
     * Checks if the app has permission to post notifications.
     */
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getLaunchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    /**
     * Sends an immediate test reminder to verify notifications work on device.
     */
    fun sendTestReminder(context: Context, currencySymbol: String = "$") {
        if (!hasPermission(context)) return
        initChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 Obsidian Wealth Reminders Active")
            .setContentText("Your automated bill due dates, SIP standing orders, and wealth alerts are enabled.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Obsidian Wealth will proactively monitor your cash flow, upcoming credit card due dates, and SIP investments to keep you on track for debt-freedom."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)

        notificationManager.notify(9001, builder.build())
    }

    /**
     * Sends a proactive alert notification.
     */
    fun sendAlertNotification(context: Context, alert: AlertItem, notificationId: Int = 1001) {
        if (!hasPermission(context)) return
        initChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val icon = when (alert.severity) {
            AlertSeverity.HIGH -> android.R.drawable.stat_notify_error
            AlertSeverity.WARNING -> android.R.drawable.stat_notify_more
            AlertSeverity.INFO -> android.R.drawable.ic_dialog_info
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(icon)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(
                if (alert.severity == AlertSeverity.HIGH) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * Schedules periodic background evaluation using WorkManager.
     */
    fun schedulePeriodicAlerts(context: Context) {
        try {
            initChannels(context)
            val alertWork = PeriodicWorkRequestBuilder<AlertWorker>(12, TimeUnit.HOURS)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC_ALERTS,
                ExistingPeriodicWorkPolicy.UPDATE,
                alertWork
            )
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Error scheduling WorkManager alerts", e)
        }
    }

    /**
     * Cancels all scheduled reminder tasks.
     */
    fun cancelAll(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC_ALERTS)
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Error cancelling alert work", e)
        }
    }

    /**
     * Schedules the recurring SIP debit engine (see [SipDebitWorker] / [SipDebitEngine]) to run
     * periodically in the background, so monthly SIP debits still record as cash-flow outflows
     * even on days the app is never opened. Unlike alert notifications, this is not gated by the
     * user's notification preference - it is financial record-keeping, not a notification.
     */
    fun scheduleSipDebitEngine(context: Context) {
        try {
            val sipDebitWork = PeriodicWorkRequestBuilder<SipDebitWorker>(12, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_SIP_DEBITS,
                ExistingPeriodicWorkPolicy.KEEP,
                sipDebitWork
            )
        } catch (e: Exception) {
            android.util.Log.e("NotificationManager", "Error scheduling SIP debit engine", e)
        }
    }
}
