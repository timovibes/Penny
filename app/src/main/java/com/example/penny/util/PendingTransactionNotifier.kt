package com.example.penny.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.penny.MainActivity
import com.example.penny.data.model.PendingTransaction
import kotlin.random.Random

/**
 * Posts a system tray notification when SmsReceiver or
 * PennyNotificationListenerService stages a new PendingTransaction, so the
 * user sees it show up like any other notification, not just the in-app badge.
 */
object PendingTransactionNotifier {

    private const val CHANNEL_ID = "auto_detected_transactions"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto-detected transactions",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when Penny detects a transaction from SMS or a payment notification"
        }
        manager.createNotificationChannel(channel)
    }

    fun notify(context: Context, pending: PendingTransaction) {
        // Requires POST_NOTIFICATIONS on Android 13+; if not granted, just skip —
        // the item is still staged and visible via the in-app badge regardless.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sign = if (pending.type == "income") "+" else "-"
        val title = "$sign${pending.amount.toInt()} KES detected"
        val text = pending.note.ifBlank { pending.category } + " · tap to review"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.penny.R.drawable.logo)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Unique-ish ID per notification so multiple detected transactions stack
        // instead of overwriting each other
        manager.notify(Random.nextInt(10_000, 99_999), notification)
    }
}