package com.example.penny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.penny.data.model.PendingTransaction
import com.example.penny.util.CurrencyFormatter
import com.example.penny.viewmodel.ReviewInboxViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BUDGET_CATEGORIES = listOf("Food", "Transport", "Rent", "Utilities", "Health", "Shopping", "Entertainment", "Airtime", "Other", "Uncategorized")
private val dateFmt = DateTimeFormatter.ofPattern("MMM d, h:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewInboxScreen(
    onBackClick: () -> Unit,
    viewModel: ReviewInboxViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    var editingItem by remember { mutableStateOf<PendingTransaction?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Pending, 1 = History

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Review Transactions") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = colors.background) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pending${if (state.pending.isNotEmpty()) " (${state.pending.size})" else ""}") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("History") }
                )
            }

            val listToShow = if (selectedTab == 0) state.pending else state.history
            val emptyMessage = if (selectedTab == 0) "No auto-detected transactions to review" else "No history yet"

            if (listToShow.isEmpty() && !state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(emptyMessage, color = colors.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (selectedTab == 0) {
                        items(state.pending, key = { it.id }) { item ->
                            PendingTransactionCard(
                                item = item,
                                onConfirm = { viewModel.confirm(item) },
                                onEdit = { editingItem = item },
                                onDismiss = { viewModel.dismiss(item) }
                            )
                        }
                    } else {
                        items(state.history, key = { it.id }) { item ->
                            HistoryCard(item)
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { item ->
        EditPendingDialog(
            item = item,
            onDismissRequest = { editingItem = null },
            onSave = { amount, category, note ->
                viewModel.confirm(item, amount = amount, category = category, note = note)
                editingItem = null
            }
        )
    }
}

@Composable
private fun PendingTransactionCard(
    item: PendingTransaction,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = CurrencyFormatter.format(item.amount, item.currency, mapOf("KES" to 1.0)),
                    fontWeight = FontWeight.Bold,
                    color = if (item.type == "income") Color(0xFF4CAF82) else colors.onSurface
                )
                Text(item.note.ifBlank { item.category }, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = if (item.source == "sms") "SMS · ${item.sourceLabel}" else "Notification",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }

        if (item.confidence == "low") {
            Spacer(Modifier.height(8.dp))
            Text(
                "⚠ Low confidence match — please check the amount and category",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE57373)
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
            OutlinedButton(onClick = onEdit) { Text("Edit") }
            Button(onClick = onConfirm) { Text("Confirm") }
        }
    }
}

@Composable
private fun HistoryCard(item: PendingTransaction) {
    val colors = MaterialTheme.colorScheme
    val isConfirmed = item.status == "confirmed"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = CurrencyFormatter.format(item.amount, item.currency, mapOf("KES" to 1.0)),
                fontWeight = FontWeight.Bold,
                color = if (isConfirmed) colors.onSurface else colors.onSurfaceVariant
            )
            Text(item.note.ifBlank { item.category }, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(
                item.date.toDate().toInstant().atZone(ZoneId.systemDefault()).format(dateFmt),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isConfirmed) Color(0xFF4CAF82).copy(alpha = 0.15f) else colors.error.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isConfirmed) "Confirmed" else "Dismissed",
                color = if (isConfirmed) Color(0xFF4CAF82) else colors.error,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPendingDialog(
    item: PendingTransaction,
    onDismissRequest: () -> Unit,
    onSave: (amount: Double, category: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(item.amount.toString()) }
    var category by remember { mutableStateOf(item.category) }
    var note by remember { mutableStateOf(item.note) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true
                )
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        BUDGET_CATEGORIES.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = {
                                category = option
                                categoryExpanded = false
                            })
                        }
                    }
                }
                Text(item.rawText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull() ?: item.amount
                onSave(amount, category, note)
            }) { Text("Save & Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}