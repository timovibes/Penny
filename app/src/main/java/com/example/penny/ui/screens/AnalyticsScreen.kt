package com.example.penny.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.penny.util.CurrencyFormatter
import com.example.penny.viewmodel.AnalyticsViewModel
import com.example.penny.viewmodel.BudgetProgress
import com.example.penny.viewmodel.CategorySpending
import com.example.penny.viewmodel.MonthlyTrendPoint
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private val BUDGET_CATEGORIES = listOf("Food", "Transport", "Rent", "Utilities", "Health", "Shopping", "Entertainment", "Other")

private val SliceColors = listOf(
    Color(0xFF4CAF82), Color(0xFFE57373), Color(0xFF64B5F6),
    Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFF4DB6AC),
    Color(0xFFF06292), Color(0xFF90A4AE)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBackClick: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    var showBudgetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Spending Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                MonthSelector(
                    year = state.displayedYear,
                    month = state.displayedMonth,
                    onPrevious = viewModel::goToPreviousMonth,
                    onNext = viewModel::goToNextMonth
                )
            }

            item {
                Text("By Category", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(12.dp))
                if (state.categoryBreakdown.isEmpty() && !state.isLoading) {
                    EmptyHint("No expenses logged this month yet")
                } else {
                    CategoryPieChart(state.categoryBreakdown, state.currencyCode, state.exchangeRates)
                }
            }

            item {
                Text("6-Month Trend", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(12.dp))
                if (state.monthlyTrend.isNotEmpty()) {
                    MonthlyTrendChart(state.monthlyTrend)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Budgets", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    TextButton(onClick = { showBudgetDialog = true }) { Text("Set Budget") }
                }
            }

            if (state.budgetProgress.isEmpty()) {
                item { EmptyHint("No budgets set for this month") }
            } else {
                items(state.budgetProgress) { progress ->
                    BudgetProgressRow(
                        progress = progress,
                        currencyCode = state.currencyCode,
                        exchangeRates = state.exchangeRates,
                        onDelete = { viewModel.deleteBudget(progress.category) }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showBudgetDialog) {
        SetBudgetDialog(
            onDismiss = { showBudgetDialog = false },
            onConfirm = { category, limit ->
                viewModel.setBudget(category, limit)
                showBudgetDialog = false
            }
        )
    }
}

// ── Month selector ───────────────────────────────────────────────────────────
@Composable
private fun MonthSelector(year: Int, month: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = colors.onSurfaceVariant)
        }
        Text(
            "$monthName $year",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = colors.onSurfaceVariant)
        }
    }
}

// ── Pie chart ─────────────────────────────────────────────────────────────────
@Composable
private fun CategoryPieChart(
    breakdown: List<CategorySpending>,
    currencyCode: String,
    exchangeRates: Map<String, Double>
) {
    val colors = MaterialTheme.colorScheme

    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            val strokeWidth = size.minDimension * 0.28f
            breakdown.forEachIndexed { index, slice ->
                val sweep = slice.percentage * 360f
                drawArc(
                    color = SliceColors[index % SliceColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth)
                )
                startAngle += sweep
            }
        }

        Spacer(Modifier.width(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            breakdown.take(6).forEachIndexed { index, slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SliceColors[index % SliceColors.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(slice.category, style = MaterialTheme.typography.labelMedium, color = colors.onSurface)
                        Text(
                            "${CurrencyFormatter.format(slice.amount, currencyCode, exchangeRates)} · ${(slice.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Monthly trend bar chart ────────────────────────────────────────────────────
@Composable
private fun MonthlyTrendChart(points: List<MonthlyTrendPoint>) {
    val colors = MaterialTheme.colorScheme
    val maxValue = points.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(120.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight(fraction = (point.income / maxValue).toFloat().coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(Color(0xFF4CAF82))
                    )
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .fillMaxHeight(fraction = (point.expense / maxValue).toFloat().coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(colors.error)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    Month.of(point.month).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

// ── Budget progress row ────────────────────────────────────────────────────────
@Composable
private fun BudgetProgressRow(
    progress: BudgetProgress,
    currencyCode: String,
    exchangeRates: Map<String, Double>,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val barColor = when {
        progress.isOverBudget -> colors.error
        progress.isNearLimit -> Color(0xFFFFB74D)
        else -> colors.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainerLow)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(progress.category, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove budget", tint = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.percentage.coerceAtMost(1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = colors.surfaceContainerHigh
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${CurrencyFormatter.format(progress.spent, currencyCode, exchangeRates)} of ${CurrencyFormatter.format(progress.limit, currencyCode, exchangeRates)}" +
                    if (progress.isOverBudget) " — over budget" else "",
            style = MaterialTheme.typography.labelSmall,
            color = if (progress.isOverBudget) colors.error else colors.onSurfaceVariant
        )
    }
}

// ── Set budget dialog ────────────────────────────────────────────────────────
@Composable
private fun SetBudgetDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var selectedCategory by remember { mutableStateOf(BUDGET_CATEGORIES.first()) }
    var limitText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BUDGET_CATEGORIES) { cat ->
                        val selected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) colors.primary else colors.surfaceContainerHigh)
                                .border(1.dp, if (selected) colors.primary else colors.outlineVariant, RoundedCornerShape(50))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                cat,
                                color = if (selected) colors.onPrimary else colors.onSurface,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { limitText.toDoubleOrNull()?.let { onConfirm(selectedCategory, it) } },
                enabled = limitText.toDoubleOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyHint(text: String) {
    val colors = MaterialTheme.colorScheme
    Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
}