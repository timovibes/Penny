package com.example.penny.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.penny.data.model.Transaction
import com.example.penny.viewmodel.DaySummary
import com.example.penny.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter


private val IncomeGreen = Color(0xFF4CAF82) //just for income


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
//    totalBalance : Double = 0.0, // <- new, will come from state once ViewModel is wired
    onProfileClick : () -> Unit = {} // <- new, wire to navigation later
    ) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    var showAddSheet by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            TopBar(
                totalBalance = state.totalBalance,
                onProfileClick = onProfileClick
            )

            MonthHeader(
                year = state.displayedYear,
                month = state.displayedMonth,
                monthlyIncome = state.monthlyIncome,
                monthlyExpenses = state.monthlyExpenses,
                onPrevious = viewModel::goToPreviousMonth,
                onNext = viewModel::goToNextMonth
            )

            WeekdayRow()

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else {
                CalendarGrid(
                    year = state.displayedYear,
                    month = state.displayedMonth,
                    daySummaries = state.daySummaries,
                    selectedDate = state.selectedDate,
                    onDayClick = viewModel::selectDay
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = colors.primary,
            contentColor = colors.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add transaction")
        }

        if (state.selectedDate != null) {
            ModalBottomSheet(
                onDismissRequest = viewModel::clearSelectedDay,
                sheetState = sheetState,
                containerColor = colors.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                DayDetailSheet(
                    date = state.selectedDate!!,
                    transactions = state.selectedDayTransactions
                )

            }
        }
        if (showAddSheet) {
            AddTransactionSheet(onDismiss = { showAddSheet = false })
        }
    }
}

// ── Top bar: total balance + profile ──────────────────────────────────────────
@Composable
private fun TopBar(
    totalBalance: Double,
    onProfileClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Total Balance",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "KES %,.0f".format(totalBalance),
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        IconButton(onClick = onProfileClick) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "T", // placeholder initial, swap for user's initial or photo later
                    color = colors.onPrimary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// ── Month header ──────────────────────────────────────────────────────────────
@Composable
private fun MonthHeader(
    year: Int,
    month: Int,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val label = YearMonth.of(year, month)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = colors.onSurfaceVariant)
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = colors.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = colors.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryChip("Income", monthlyIncome, IncomeGreen, Modifier.weight(1f))
            SummaryChip("Expenses", monthlyExpenses, colors.error, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryChip(label: String, amount: Double, color: Color, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "KES %,.0f".format(amount),
            color = color,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

// ── Weekday labels ────────────────────────────────────────────────────────────
@Composable
private fun WeekdayRow() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
            Text(
                text = d,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ── Calendar grid ─────────────────────────────────────────────────────────────
@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    daySummaries: Map<LocalDate, DaySummary>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = LocalDate.of(year, month, 1)
    val startOffset = firstDay.dayOfWeek.value % 7
    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    val rows = (startOffset + daysInMonth + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = row * 7 + col - startOffset + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(0.85f))
                    } else {
                        val date = LocalDate.of(year, month, dayNum)
                        DayCell(
                            day = dayNum,
                            date = date,
                            summary = daySummaries[date],
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

// ── Single day cell ───────────────────────────────────────────────────────────
@Composable
private fun DayCell(
    day: Int,
    date: LocalDate,
    summary: DaySummary?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.surfaceContainerHigh else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                }
                Text(
                    text = day.toString(),
                    color = if (isToday) colors.onPrimary else colors.onBackground,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }

            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (summary != null && summary.totalIncome > 0) {
                    Dot(IncomeGreen)
                }
                if (summary != null && summary.totalExpenses > 0) {
                    Dot(colors.error)
                }
            }
            if (summary == null || (summary.totalIncome == 0.0 && summary.totalExpenses == 0.0)) {
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ── Day detail bottom sheet ───────────────────────────────────────────────────
@Composable
private fun DayDetailSheet(date: LocalDate, transactions: List<Transaction>) {
    val fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
    val expenses = transactions.filter { it.type == "expense" }.sumOf { it.amount }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            date.format(fmt),
            color = colors.onSurface,
            style = MaterialTheme.typography.titleMedium
        )

        if (transactions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (income > 0) Text(
                    "+KES %,.0f".format(income),
                    color = IncomeGreen,
                    style = MaterialTheme.typography.labelMedium
                )
                if (expenses > 0) Text(
                    "−KES %,.0f".format(expenses),
                    color = colors.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transactions on this day",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { tx -> TransactionRow(tx) }
            }
        }
    }
}

// ── Transaction row ───────────────────────────────────────────────────────────
@Composable
private fun TransactionRow(tx: Transaction) {
    val colors = MaterialTheme.colorScheme
    val isExpense = tx.type == "expense"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceContainerLow)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                tx.category.ifBlank { "Other" },
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = tx.note.ifBlank { "No description" },
            color = colors.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${if (isExpense) "−" else "+"}KES %,.0f".format(tx.amount),
            color = if (isExpense) colors.error else IncomeGreen,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}