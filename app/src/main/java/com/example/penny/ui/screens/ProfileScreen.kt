package com.example.penny.ui.screens

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
import com.example.penny.util.BiometricAuthHelper
import com.example.penny.util.CurrencyFormatter


// ── Data passed in — swap defaults for real data once wired to a ViewModel ────
data class ProfileUiState(
    val name: String = "Alex Johnson",
    val email: String = "alex.j@example.com",
    val avatarUrl: String? = null,          // null for now, plug a real URL + Coil later
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
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditAvatarClick: () -> Unit = {},
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
            .statusBarsPadding()
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
            }
            Text(
                text = "My Profile",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground
            )
            Spacer(Modifier.width(48.dp))
        }

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
                    Text(
                        text = state.name.firstOrNull()?.uppercase() ?: "?",
                        color = colors.onPrimary,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.onBackground)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Coming soon", Toast.LENGTH_LONG).show()
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
                                title = "Enable fingerprint lock",
                                subtitle = "Confirm your fingerprint to turn this on",
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
                onClick = onChangePasswordClick
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
    }
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