package com.example.penny.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Privacy Policy screen for Penny.
 *
 * NOTE: Replace the placeholder values below (LAST_UPDATED_DATE, CONTACT_EMAIL,
 * COMPANY_NAME) before shipping. Also double check the "AI Processing" section
 * once you've confirmed exactly what data (if any) gets sent to Gemini.
 */

private const val LAST_UPDATED_DATE = "17 July 2026"
private const val CONTACT_EMAIL = "support@Penny.com" // TODO: replace
private const val COMPANY_NAME = "Penny" // TODO: replace with your legal entity name if you have one

private data class PolicySection(
    val title: String,
    val body: String
)

private val policySections = listOf(
    PolicySection(
        title = "Introduction",
        body = "This Privacy Policy explains how $COMPANY_NAME (\"we\", \"us\", \"the app\") " +
                "collects, uses, stores, and protects your information when you use Penny. " +
                "By using the app, you agree to the practices described here. If you don't " +
                "agree, please don't use the app."
    ),
    PolicySection(
        title = "Information We Collect",
        body = "We collect the following types of information:\n\n" +
                "• Account information: your email address and any profile details you provide " +
                "when you sign up (via Firebase Authentication).\n\n" +
                "• Financial data: transactions, balances, budgets, and other financial " +
                "information you enter into the app so we can show you insights and track " +
                "your spending.\n\n" +
                "• Biometric authentication data: if you enable fingerprint or face unlock, " +
                "your device's biometric system handles the actual biometric data — we never " +
                "see or store your raw fingerprint or facial data. Penny only stores an " +
                "encrypted credential/token to confirm you unlocked the app.\n\n" +
                "• Device information: device model, OS version, and app version, used for " +
                "debugging and improving stability.\n\n" +
                "• Location data: only if you grant location permission, used for [state the " +
                "actual reason, e.g. tagging transactions with a location]. You can deny or " +
                "revoke this anytime in your device settings."
    ),
    PolicySection(
        title = "How We Use Your Information",
        body = "We use your information to:\n\n" +
                "• Provide core app features (tracking balances, budgets, transaction history)\n" +
                "• Authenticate you securely and protect your account\n" +
                "• Generate personalized financial insights\n" +
                "• Fix bugs and improve app performance\n" +
                "• Communicate with you about your account, if needed"
    ),
    PolicySection(
        title = "AI-Powered Features",
        body = "Penny may use Google's Gemini AI to power certain features (such as " +
                "spending insights or suggestions). [TODO: confirm and state clearly whether " +
                "any personal or financial data is sent to Gemini for processing, or whether " +
                "these features run only on anonymized/aggregated data. Until this is confirmed " +
                "and documented, avoid publishing this app update to production.] Any data sent " +
                "to third-party AI providers is subject to their own privacy and data-handling " +
                "terms in addition to this policy."
    ),
    PolicySection(
        title = "Data Storage & Security",
        body = "Your data is stored using Firebase (Firestore and Firebase Authentication), " +
                "a service provided by Google. We use industry-standard security practices, " +
                "including encrypted storage for sensitive credentials, to protect your " +
                "information. However, no method of storage or transmission over the internet " +
                "is 100% secure, and we can't guarantee absolute security."
    ),
    PolicySection(
        title = "Data Sharing",
        body = "We do not sell your personal or financial data. We may share limited data " +
                "with:\n\n" +
                "• Firebase/Google Cloud, as our infrastructure provider\n" +
                "• Google Gemini AI, for AI-powered features (see above)\n" +
                "• Law enforcement or regulators, only if required by law\n\n" +
                "We do not share your data with advertisers or data brokers."
    ),
    PolicySection(
        title = "Your Rights",
        body = "Depending on where you live, you may have the right to access, correct, " +
                "or delete your personal data, and to withdraw consent for optional features " +
                "like location tracking. To exercise any of these rights, contact us at " +
                "$CONTACT_EMAIL. If you're in Kenya, this also applies under the Data " +
                "Protection Act, 2019."
    ),
    PolicySection(
        title = "Data Retention",
        body = "We keep your data for as long as your account is active. If you delete " +
                "your account, we will delete your personal and financial data within a " +
                "reasonable period, except where we're required to retain it for legal reasons."
    ),
    PolicySection(
        title = "Children's Privacy",
        body = "Penny is not intended for use by children under 18. We do not knowingly " +
                "collect data from children."
    ),
    PolicySection(
        title = "Changes to This Policy",
        body = "We may update this Privacy Policy from time to time. We'll notify you of " +
                "significant changes through the app or via email."
    ),
    PolicySection(
        title = "Contact Us",
        body = "If you have questions about this Privacy Policy or how your data is " +
                "handled, contact us at $CONTACT_EMAIL."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Last updated: $LAST_UPDATED_DATE",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            items(policySections) { section ->
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Text(
                        text = section.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = section.body,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}