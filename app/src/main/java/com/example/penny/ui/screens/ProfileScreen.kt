package com.example.penny.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import android.Manifest
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.pm.PackageManager
import com.example.penny.util.BiometricAuthHelper
import com.example.penny.util.CurrencyFormatter
import com.example.penny.util.NotificationAccessHelper
import com.example.penny.util.rememberAvatarBitmap


// ── Data passed in — swap defaults for real data once wired to a ViewModel ────
data class ProfileUiState(
    val name: String = "Alex Johnson",
    val email: String = "alex.j@example.com",
    val avatarUrl: String? = null,          // legacy field, unused now — avatarBase64 param carries the real image
    val isProMember: Boolean = true,
    val faceIdEnabled: Boolean = true,
    val twoFactorEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val currencyLabel: String = "USD"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState = ProfileUiState(),
    currentCurrency: String = state.currencyLabel, // <- live value from ProfileViewModel.currencyCode
    biometricEnabled: Boolean = state.faceIdEnabled, // <- live value from ProfileViewModel.biometricEnabled
    avatarBase64: String? = null, // <- live value from ProfileViewModel.avatarBase64
    isUploadingAvatar: Boolean = false,   // <- live value from ProfileViewModel.isUploadingAvatar
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditAvatarClick: () -> Unit = {},
    onAvatarSelected: (android.net.Uri) -> Unit = {},
    onPersonalInfoClick: () -> Unit = {},
    onBankAccountsClick: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onFaceIdToggle: (Boolean) -> Unit = {},
    onChangePasswordClick: () -> Unit = {},
    onTwoFactorClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onCurrencySelected: (String) -> Unit = {},
    onDarkModeToggle: (Boolean) -> Unit = {},
    onHelpCenterClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showChangePasswordSheet by remember { mutableStateOf(false) }
    val avatarBitmap = rememberAvatarBitmap(avatarBase64)

    // ── Auto-detect transactions: SMS + notification access status ────────
    var smsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var notificationAccessEnabled by remember { mutableStateOf(NotificationAccessHelper.isEnabled(context)) }
    var postNotificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        smsPermissionGranted = results[Manifest.permission.RECEIVE_SMS] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotificationsGranted = results[Manifest.permission.POST_NOTIFICATIONS] ?: postNotificationsGranted
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> postNotificationsGranted = granted }

    // Notification access is granted on a separate system Settings screen, not a dialog,
    // so re-check status when the user comes back to this screen rather than in a callback
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessEnabled = NotificationAccessHelper.isEnabled(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    postNotificationsGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onAvatarSelected(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // ── Avatar + name + email + badge ───────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = state.name.firstOrNull()?.uppercase() ?: "?",
                                color = colors.onPrimary,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (isUploadingAvatar) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.onBackground)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                                onEditAvatarClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit photo",
                            tint = colors.background,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(state.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = colors.onBackground)
                Text(state.email, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)

                if (state.isProMember) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.primaryContainer)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Pro Member",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.onPrimaryContainer
                        )
                    }
                }
            }

            // ── Account ──────────────────────────────────────────────────────────
            SectionLabel("ACCOUNT")
            SectionCard {
                ProfileRow(Icons.Default.Star, "Subscription Plan", trailingLabel = "PRO", onClick = onSubscriptionClick)
            }

            // ── Security ─────────────────────────────────────────────────────────
            SectionLabel("SECURITY")
            SectionCard {
                ProfileRow(
                    icon = Icons.Default.Fingerprint,
                    label = "Face ID / Biometrics",
                    trailingContent = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { checked ->
                                if (!checked) {
                                    // Turning off never needs a prompt
                                    onFaceIdToggle(false)
                                    return@Switch
                                }
                                if (!BiometricAuthHelper.isAvailable(context)) {
                                    Toast.makeText(
                                        context,
                                        "No fingerprint set up on this device",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Switch
                                }
                                val activity = context as? FragmentActivity
                                if (activity == null) {
                                    Toast.makeText(context, "Couldn't open fingerprint setup", Toast.LENGTH_LONG).show()
                                    return@Switch
                                }
                                BiometricAuthHelper.showPrompt(
                                    activity = activity,
                                    onSuccess = { onFaceIdToggle(true) },
                                    onError = { /* stays off */ },
                                    onFailed = { /* stays off */ }
                                )
                            }
                        )
                    }
                )
                RowDivider()
                ProfileRow(
                    Icons.Default.Lock, "Change Password",
                    onClick = { showChangePasswordSheet = true }
                )
            }

            // ── Auto-detect transactions ────────────────────────────────────────
            SectionLabel("AUTO-DETECT TRANSACTIONS")
            SectionCard {
                ProfileRow(
                    icon = Icons.Default.Sms,
                    label = "Read M-Pesa / Bank SMS",
                    trailingContent = {
                        Switch(
                            checked = smsPermissionGranted,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    smsPermissionLauncher.launch(perms.toTypedArray())
                                } else {
                                    // Android has no API to revoke a permission from within the app —
                                    // send the user to the app's own permission settings instead
                                    Toast.makeText(
                                        context,
                                        "To turn this off, disable SMS permission in system Settings",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }
                )
                RowDivider()
                ProfileRow(
                    icon = Icons.Default.NotificationsActive,
                    label = "Read Payment Notifications",
                    trailingLabel = if (notificationAccessEnabled) "On" else "Off",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !postNotificationsGranted) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        NotificationAccessHelper.openSettings(context)
                    }
                )
            }

            // ── Support ──────────────────────────────────────────────────────────
            SectionLabel("SUPPORT")
            SectionCard {
                ProfileRow(
                    Icons.Default.Payments,
                    "Currency ($currentCurrency)",
                    onClick = { showCurrencyDialog = true }
                )
                RowDivider()
                ProfileRow(Icons.Default.HelpOutline, "Help Center", onClick = onHelpCenterClick)
                RowDivider()
                ProfileRow(Icons.Default.PrivacyTip, "Privacy Policy", onClick = onPrivacyPolicyClick)
                RowDivider()

                ProfileRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Log Out",
                    labelColor = colors.error,
                    iconTint = colors.error,
                    onClick = { showLogoutDialog = true }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Log out?") },
                    text = { Text("Are you sure you want to log out?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                onLogoutClick()
                            }
                        ) {
                            Text("Log out", color = colors.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showCurrencyDialog) {
                CurrencyPickerDialog(
                    currentCurrency = currentCurrency,
                    onDismiss = { showCurrencyDialog = false },
                    onSelect = { code ->
                        onCurrencySelected(code)
                        showCurrencyDialog = false
                    }
                )
            }
            if (showChangePasswordSheet) {
                ChangePasswordSheet(
                    onDismiss = { showChangePasswordSheet = false }
                )
            }
        } // closes Column
    } // closes Scaffold content lambda
}

// ── Currency picker dialog ───────────────────────────────────────────────────
@Composable
private fun CurrencyPickerDialog(
    currentCurrency: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose currency") },
        text = {
            Column {
                CurrencyFormatter.supportedCurrencies.forEach { code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = code == currentCurrency,
                                onClick = { onSelect(code) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = code == currentCurrency,
                            onClick = { onSelect(code) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(code)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// ── Reusable pieces ─────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    trailingLabel: String? = null,
    trailingLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingContent: (@Composable () -> Unit)? = null,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor
        )

        when {
            trailingContent != null -> trailingContent()
            trailingLabel != null -> Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelMedium,
                color = trailingLabelColor
            )
            else -> Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant
            )
        }
    }
}