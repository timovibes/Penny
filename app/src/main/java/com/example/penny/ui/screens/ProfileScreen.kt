package com.example.penny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    onCurrencyClick: () -> Unit = {},
    onDarkModeToggle: (Boolean) -> Unit = {},
    onHelpCenterClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    versionLabel: String = "Version 2.4.1 (8291)"
) {
    val colors = MaterialTheme.colorScheme

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
                .padding(horizontal = 12.dp, vertical = 20.dp),   // <- more vertical space too
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
            Spacer(Modifier.width(48.dp))   // <- balances the back arrow's width so "Profile" stays centered
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
                    // Placeholder letter avatar — swap for AsyncImage(state.avatarUrl) via Coil later
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
                        .clickable(onClick = onEditAvatarClick),
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
            ProfileRow(Icons.Default.Person, "Personal Information", onClick = onPersonalInfoClick)
            RowDivider()
            ProfileRow(Icons.Default.Star, "Subscription Plan", trailingLabel = "PRO", onClick = onSubscriptionClick)
        }

        // ── Security ─────────────────────────────────────────────────────────
        SectionLabel("SECURITY")
        SectionCard {
            ProfileRow(
                icon = Icons.Default.Fingerprint,
                label = "Face ID / Biometrics",
                trailingContent = {
                    Switch(checked = state.faceIdEnabled, onCheckedChange = onFaceIdToggle)
                }
            )
            RowDivider()
            ProfileRow(
                Icons.Default.Lock, "Change Password",
                onClick = onChangePasswordClick
            )
        }

//        // ── Preferences ──────────────────────────────────────────────────────
//        SectionLabel("PREFERENCES")
//        SectionCard {
//            ProfileRow(Icons.Default.Notifications, "Notifications", onClick = onNotificationsClick)
//            RowDivider()
//            ProfileRow(Icons.Default.Payments, "Currency (${state.currencyLabel})", onClick = onCurrencyClick)
//            RowDivider()
//            ProfileRow(
//                icon = Icons.Default.DarkMode,
//                label = "Dark Mode",
//                trailingContent = {
//                    Switch(checked = state.darkModeEnabled, onCheckedChange = onDarkModeToggle)
//                }
//            )
//        }

        // ── Support ──────────────────────────────────────────────────────────
        SectionLabel("SUPPORT")
        SectionCard {
            ProfileRow(Icons.Default.Payments, "Currency (${state.currencyLabel})", onClick = onCurrencyClick)
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
                onClick = onLogoutClick
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = versionLabel,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
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