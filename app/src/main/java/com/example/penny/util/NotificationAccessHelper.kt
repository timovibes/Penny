package com.example.penny.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Notification access can't be requested via a runtime permission dialog —
 * the user has to manually enable it on a system Settings screen. This
 * helper checks current status and opens that screen.
 */
object NotificationAccessHelper {

    fun isEnabled(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return context.packageName in enabledPackages
    }

    fun openSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}