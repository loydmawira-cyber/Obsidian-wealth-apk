package com.example.alerts

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.database.AppDatabase
import com.example.data.models.TransactionEntity
import com.example.data.repository.PreferencesManager
import com.example.data.util.TransactionNotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Parses supported financial notifications locally and stores them as PENDING_REVIEW.
 *
 * Scope: only notifications from packages the user has explicitly added in Settings ->
 * Notification Access are ever inspected. Every other app's notification is discarded
 * immediately, before its title/text is even read. Do not remove this gate — it is what
 * keeps this listener's access to android.permission.BIND_NOTIFICATION_LISTENER_SERVICE
 * (which technically grants visibility into every notification on the device) limited in
 * practice to the money apps the user opted in.
 */
class TransactionNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferencesManager by lazy { PreferencesManager(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.packageName !in preferencesManager.getTrustedNotificationPackagesSnapshot()) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val parsed = TransactionNotificationParser.parse(sbn.packageName, title, text, sbn.postTime) ?: return
        serviceScope.launch {
            val dao = AppDatabase.getDatabase(applicationContext, serviceScope).financeDao()
            if (dao.hasTransactionWithSourceReference(parsed.reference) > 0) return@launch
            dao.insertTransaction(
                TransactionEntity(
                    title = parsed.title,
                    amount = parsed.amount,
                    type = parsed.type,
                    category = parsed.category,
                    account = sbn.packageName,
                    dateMillis = parsed.dateMillis,
                    note = "Detected from notification; review before confirmation",
                    statementFingerprint = parsed.reference,
                    importStatus = "PENDING_REVIEW",
                    importSource = "NOTIFICATION",
                    sourceReference = parsed.reference
                )
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
