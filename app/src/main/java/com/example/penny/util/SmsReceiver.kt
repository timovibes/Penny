package com.example.penny.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
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
        val appContext = context.applicationContext

        for (message in messages) {
            val sender = message.originatingAddress?.uppercase() ?: continue
            val body = message.messageBody ?: continue

            val isKnownSender = knownSenders.any { sender.contains(it) }
            if (!isKnownSender) {
                Log.d("SmsReceiver", "Ignored SMS from unrecognized sender: $sender")
                continue
            }

            val pending = TransactionTextParser.parse(body, sourceLabel = sender, source = "sms")
            if (pending == null) {
                Log.w("SmsReceiver", "Sender $sender matched but parser found no transaction. Body: $body")
                continue
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.addPending(pending)
                    Log.d("SmsReceiver", "Staged pending transaction: ${pending.amount} ${pending.type} from $sender")
                    PendingTransactionNotifier.notify(appContext, pending)
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to save pending transaction from $sender", e)
                }
            }
        }
    }
}