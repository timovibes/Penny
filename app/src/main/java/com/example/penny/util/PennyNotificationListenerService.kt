package com.example.penny.util

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.penny.data.repository.PendingTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reads notifications from an allowlist of financial apps, parses them via
 * the same TransactionTextParser used by SmsReceiver, and stages a
 * PendingTransaction. Requires the user to manually grant notification
 * access via Settings — see NotificationAccessHelper.
 */
class PennyNotificationListenerService : NotificationListenerService() {

    // Package names Penny reads notifications from. Extend as more apps are supported.
    private val allowedPackages = setOf(
        "com.safaricom.mpesa",
        "com.google.android.apps.walletnfcrel", // Google Wallet / Pay
        "com.paypal.android.p2pmobile",
        "com.equitybankgroup.eazzybanking",
        "com.kcb.mobilebanking"
    )

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val repository = PendingTransactionRepository()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in allowedPackages) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val body = "$title $text".trim()
        if (body.isBlank()) return

        val pending = TransactionTextParser.parse(body, sourceLabel = sbn.packageName, source = "notification") ?: return

        serviceScope.launch {
            try {
                repository.addPending(pending)
            } catch (_: Exception) {
                // Silently drop on failure — no user-facing surface from a background service
            }
        }
    }
}