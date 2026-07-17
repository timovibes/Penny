package com.example.penny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.penny.viewmodel.AddTransactionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val INCOME_CATEGORIES  = listOf("Salary", "Freelance", "Business", "Investment", "Gift", "Other")
private val EXPENSE_CATEGORIES = listOf("Food", "Transport", "Rent", "Utilities", "Health", "Shopping", "Entertainment", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors     = MaterialTheme.colorScheme

    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = colors.surfaceContainerLow,
        tonalElevation   = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Add Transaction",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )

            // ── Type toggle ───────────────────────────────────────────────
            TypeToggle(
                selected = viewModel.type,
                onSelect = { viewModel.type = it }
            )

            // ── Amount ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = viewModel.amount,
                onValueChange = { viewModel.amount = it },
                label = { Text("Amount (${viewModel.currencyCode})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Category chips ────────────────────────────────────────────
            val categories = if (viewModel.type == "income") INCOME_CATEGORIES else EXPENSE_CATEGORIES

            // Reset category when type changes so a stale pick isn't carried over
            LaunchedEffect(viewModel.type) {
                if (viewModel.category !in categories) viewModel.category = ""
            }

            Text("Category", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    CategoryChip(
                        label      = cat,
                        selected   = viewModel.category == cat,
                        onSelected = { viewModel.category = cat }
                    )
                }
            }

            // ── Note ──────────────────────────────────────────────────────
            OutlinedTextField(
                value         = viewModel.note,
                onValueChange = { viewModel.note = it },
                label         = { Text("Note (optional)") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Date ──────────────────────────────────────────────────────
            val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d yyyy")
            OutlinedTextField(
                value         = viewModel.date.format(dateFmt),
                onValueChange = {},
                label         = { Text("Date") },
                readOnly      = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            // ── Error ─────────────────────────────────────────────────────
            if (viewModel.saveError != null) {
                Text(
                    viewModel.saveError!!,
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Save button ───────────────────────────────────────────────
            Button(
                onClick  = { viewModel.saveTransaction(onSuccess = onDismiss) },
                enabled  = !viewModel.isSaving && viewModel.amount.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary
                    )
                } else {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ── Type toggle ───────────────────────────────────────────────────────────────
@Composable
private fun TypeToggle(selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceContainerHigh),
        horizontalArrangement = Arrangement.Center
    ) {
        listOf("expense", "income").forEach { t ->
            val isSelected = selected == t
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) colors.primary else colors.surfaceContainerHigh)
                    .clickable { onSelect(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = t.replaceFirstChar { it.uppercase() },
                    color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ── Category chip ─────────────────────────────────────────────────────────────
@Composable
private fun CategoryChip(label: String, selected: Boolean, onSelected: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primary else colors.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onSelected)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text  = label,
            color = if (selected) colors.onPrimary else colors.onSurface,
            style = MaterialTheme.typography.labelMedium
        )
    }
}