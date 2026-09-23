package com.example.alerts

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.database.AppDatabase
import com.example.data.models.TransactionEntity
import com.example.data.util.TransactionNotificationParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Parses supported financial notifications locally and stores them as PENDING_REVIEW. */
class TransactionNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
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
