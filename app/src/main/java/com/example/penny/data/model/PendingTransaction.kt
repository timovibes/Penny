package com.example.penny.data.model

import com.google.firebase.Timestamp

data class PendingTransaction(
    val id: String = "",
    val amount: Double = 0.0,
    val type: String = "", // "income" or "expense", inferred from parsed text
    val category: String = "Uncategorized",
    val note: String = "", // parsed counterparty / till / paybill name
    val currency: String = "KES",
    val date: Timestamp = Timestamp.now(),
    val source: String = "", // "sms" or "notification"
    val sourceLabel: String = "", // e.g. sender "MPESA" or app package name
    val rawText: String = "", // original SMS/notification body, kept for user review
    val confidence: String = "high" // "high" or "low" — low confidence surfaces a warning in the review UI
)