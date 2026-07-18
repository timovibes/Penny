package com.example.penny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.penny.data.local.BiometricPreferences
import com.example.penny.util.BiometricAuthHelper

/**
 * Wraps the whole app. If biometric lock is on, re-locks every time the app
 * comes back to the foreground (including returning from another app) and
 * shows the system fingerprint prompt. This does NOT touch Firebase Auth —
 * the user stays logged in underneath; this is purely a lock screen on top.
 */
@Composable
fun AppLockGate(
    activity: FragmentActivity,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val biometricPreferences = remember { BiometricPreferences(context) }
    // initial = true (not false): assume locked until DataStore tells us otherwise,
    // so we never briefly reveal real content before the actual preference loads.
    val biometricEnabled by biometricPreferences.isEnabled.collectAsState(initial = true)

    var isUnlocked by remember { mutableStateOf(true) }

    // Re-lock every time the app returns to the foreground
    DisposableEffect(biometricEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && biometricEnabled) {
                isUnlocked = false
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }

    // Trigger the prompt automatically whenever we're locked
    LaunchedEffect(isUnlocked, biometricEnabled) {
        if (!isUnlocked && biometricEnabled) {
            BiometricAuthHelper.showPrompt(
                activity = activity,
                onSuccess = { isUnlocked = true },
                onError = { /* stays locked; user can tap Unlock below to retry */ },
                onFailed = { /* stays locked */ }
            )
        }
    }

    // content() stays composed at all times — this is what preserves navigation state
    // (back stack, current screen) across lock/unlock cycles. The lock screen is drawn
    // ON TOP of it as an overlay, not swapped in as a replacement.
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (biometricEnabled && !isUnlocked) {
            LockedScreen(
                onRetry = {
                    BiometricAuthHelper.showPrompt(
                        activity = activity,
                        onSuccess = { isUnlocked = true }
                    )
                }
            )
        }
    }
}

// Deliberately plain — no logo/branding, just enough to not be a dead end if the
// user cancels the system prompt.
@Composable
private fun LockedScreen(onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Unlock Penny to continue", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Unlock")
            }
        }
    }
}