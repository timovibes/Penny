package com.example.penny.util

import com.example.penny.data.model.PendingTransaction
import com.google.firebase.Timestamp

/**
 * Parses M-Pesa and generic bank SMS/notification text into a PendingTransaction.
 * Shared by SmsReceiver and PennyNotificationListenerService so both sources
 * go through identical parsing logic. Returns null when the text doesn't look
 * like a financial transaction at all (most notifications won't).
 */
object TransactionTextParser {

    private val amountRegex = Regex("""(?:Ksh|KES|KSH)\s?([\d,]+\.?\d{0,2})""", RegexOption.IGNORE_CASE)

    // Each pattern: keyword trigger -> (type, category, note extractor)
    private val received = Regex("""you have received\s+(?:Ksh|KES)\s?[\d,]+\.?\d{0,2}\s+from\s+([A-Za-z0-9 .'-]+?)\s+\d""", RegexOption.IGNORE_CASE)
    private val sentTo = Regex("""sent to\s+([A-Za-z0-9 .'-]+?)\s+\d""", RegexOption.IGNORE_CASE)
    private val paidTo = Regex("""paid to\s+([A-Za-z0-9 .'-]+?)(?:\s+for account|\.|\s+on\s)""", RegexOption.IGNORE_CASE)
    private val withdrawnFrom = Regex("""withdrawn\s+(?:Ksh|KES)\s?[\d,]+\.?\d{0,2}\s+from\s+([A-Za-z0-9 .'-]+?)\s+on\s""", RegexOption.IGNORE_CASE)
    private val boughtAirtime = Regex("""bought\s+(?:Ksh|KES)\s?[\d,]+\.?\d{0,2}\s+of airtime""", RegexOption.IGNORE_CASE)

    // Generic bank fallback — looser, so it's flagged low-confidence for user review
    private val genericDebit = Regex("""(debited|withdrawn|spent|purchase of)""", RegexOption.IGNORE_CASE)
    private val genericCredit = Regex("""(credited|deposited|received)""", RegexOption.IGNORE_CASE)

    fun parse(rawText: String, sourceLabel: String, source: String): PendingTransaction? {
        val amountMatch = amountRegex.find(rawText) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        // M-Pesa specific, high confidence
        received.find(rawText)?.let {
            return build(amount, "income", "Uncategorized", it.groupValues[1].trim(), rawText, sourceLabel, source, "high")
        }
        boughtAirtime.find(rawText)?.let {
            return build(amount, "expense", "Airtime", "Airtime purchase", rawText, sourceLabel, source, "high")
        }
        withdrawnFrom.find(rawText)?.let {
            return build(amount, "expense", "Uncategorized", "Withdrawal: ${it.groupValues[1].trim()}", rawText, sourceLabel, source, "high")
        }
        paidTo.find(rawText)?.let {
            return build(amount, "expense", "Uncategorized", it.groupValues[1].trim(), rawText, sourceLabel, source, "high")
        }
        sentTo.find(rawText)?.let {
            return build(amount, "expense", "Uncategorized", it.groupValues[1].trim(), rawText, sourceLabel, source, "high")
        }

        // Generic bank fallback, low confidence — always needs user review
        if (genericDebit.containsMatchIn(rawText)) {
            return build(amount, "expense", "Uncategorized", sourceLabel, rawText, sourceLabel, source, "low")
        }
        if (genericCredit.containsMatchIn(rawText)) {
            return build(amount, "income", "Uncategorized", sourceLabel, rawText, sourceLabel, source, "low")
        }

        return null
    }

    private fun build(
        amount: Double,
        type: String,
        category: String,
        note: String,
        rawText: String,
        sourceLabel: String,
        source: String,
        confidence: String
    ) = PendingTransaction(
        amount = amount,
        type = type,
        category = category,
        note = note,
        currency = "KES",
        date = Timestamp.now(),
        source = source,
        sourceLabel = sourceLabel,
        rawText = rawText,
        confidence = confidence
    )
}