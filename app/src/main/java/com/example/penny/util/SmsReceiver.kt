package com.example.penny.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.penny.data.repository.PendingTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for incoming SMS, filters to known financial senders, parses via
 * TransactionTextParser, and stages a PendingTransaction for user review.
 * Registered in AndroidManifest for Telephony.Sms.Intents.SMS_RECEIVED_ACTION.
 */
class SmsReceiver : BroadcastReceiver() {

    // Sender IDs Penny recognizes as financial. Extend as more banks are supported.
    private val knownSenders = listOf("MPESA", "M-PESA", "EQUITY", "KCB", "COOP BANK", "ABSA", "NCBA")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val repository = PendingTransactionRepository()

        for (message in messages) {
            val sender = message.originatingAddress?.uppercase() ?: continue
            val body = message.messageBody ?: continue

            val isKnownSender = knownSenders.any { sender.contains(it) }
            if (!isKnownSender) continue

            val pending = TransactionTextParser.parse(body, sourceLabel = sender, source = "sms") ?: continue

            // Fire-and-forget write; receiver has no long-running lifecycle to hold a coroutine scope
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.addPending(pending)
                } catch (_: Exception) {
                    // Silently drop on failure (e.g. user signed out) — no user-facing surface from a background receiver
                }
            }
        }
    }
}