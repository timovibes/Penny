package com.example.penny.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.penny.data.local.CurrencyPreferences
import com.example.penny.data.model.Budget
import com.example.penny.data.repository.BudgetRepository
import com.example.penny.data.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Float // 0f..1f share of total expenses that month
)

data class MonthlyTrendPoint(
    val year: Int,
    val month: Int,
    val income: Double,
    val expense: Double
)

data class BudgetProgress(
    val category: String,
    val spent: Double,
    val limit: Double
) {
    val percentage: Float get() = if (limit <= 0.0) 0f else (spent / limit).toFloat().coerceAtLeast(0f)
    val isNearLimit: Boolean get() = percentage in 0.8f..1f
    val isOverBudget: Boolean get() = percentage > 1f
}

data class AnalyticsUiState(
    val displayedYear: Int = LocalDate.now().year,
    val displayedMonth: Int = LocalDate.now().monthValue,
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val monthlyTrend: List<MonthlyTrendPoint> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val currencyCode: String = CurrencyPreferences.DEFAULT_CURRENCY,
    val exchangeRates: Map<String, Double> = mapOf("KES" to 1.0),
    val isLoading: Boolean = true,
    val error: String? = null
)

class AnalyticsViewModel @JvmOverloads constructor(
    application: Application,
    private val transactionRepository: TransactionRepository = TransactionRepository(),
    private val budgetRepository: BudgetRepository = BudgetRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var monthObserverJob: Job? = null
    private val currencyPreferences = CurrencyPreferences(application)

    init {
        observeCurrentMonth()
        observeCurrency()
        loadMonthlyTrend()
    }

    private fun observeCurrency() {
        viewModelScope.launch {
            currencyPreferences.currencyCode.collect { code ->
                _uiState.update { it.copy(currencyCode = code) }
            }
        }
    }

    fun goToPreviousMonth() {
        val current = _uiState.value
        val (year, month) = if (current.displayedMonth == 1)
            current.displayedYear - 1 to 12
        else
            current.displayedYear to current.displayedMonth - 1

        _uiState.update { it.copy(displayedYear = year, displayedMonth = month) }
        observeCurrentMonth()
        loadMonthlyTrend()
    }

    fun goToNextMonth() {
        val current = _uiState.value
        val (year, month) = if (current.displayedMonth == 12)
            current.displayedYear + 1 to 1
        else
            current.displayedYear to current.displayedMonth + 1

        _uiState.update { it.copy(displayedYear = year, displayedMonth = month) }
        observeCurrentMonth()
        loadMonthlyTrend()
    }

    // Combines live transactions + live budgets for the displayed month so the
    // category breakdown pie and the budget progress bars update together
    private fun observeCurrentMonth() {
        monthObserverJob?.cancel()
        monthObserverJob = viewModelScope.launch {
            val year = _uiState.value.displayedYear
            val month = _uiState.value.displayedMonth
            _uiState.update { it.copy(isLoading = true, error = null) }

            transactionRepository.observeTransactionsForMonth(year, month)
                .combine(budgetRepository.observeBudgetsForMonth(year, month)) { transactions, budgets ->
                    transactions to budgets
                }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { (transactions, budgets) ->
                    val expenses = transactions.filter { it.type == "expense" }
                    val totalExpenses = expenses.sumOf { it.amount }

                    val spentByCategory = expenses
                        .groupBy { it.category.ifBlank { "Other" } }
                        .mapValues { (_, txList) -> txList.sumOf { it.amount } }

                    val breakdown = spentByCategory
                        .map { (category, amount) ->
                            CategorySpending(
                                category = category,
                                amount = amount,
                                percentage = if (totalExpenses > 0) (amount / totalExpenses).toFloat() else 0f
                            )
                        }
                        .sortedByDescending { it.amount }

                    val progress = budgets
                        .map { budget ->
                            BudgetProgress(
                                category = budget.category,
                                spent = spentByCategory[budget.category] ?: 0.0,
                                limit = budget.monthlyLimit
                            )
                        }
                        .sortedByDescending { it.percentage }

                    _uiState.update {
                        it.copy(
                            categoryBreakdown = breakdown,
                            budgetProgress = progress,
                            isLoading = false
                        )
                    }
                }
        }
    }

    // One-shot fetch (not live) for the last 6 months — a trend chart doesn't
    // need real-time updates the way the current month's breakdown does
    private fun loadMonthlyTrend() {
        viewModelScope.launch {
            val anchor = _uiState.value
            val points = (5 downTo 0).map { offset ->
                var year = anchor.displayedYear
                var month = anchor.displayedMonth - offset
                while (month < 1) { month += 12; year -= 1 }

                val transactions = try {
                    transactionRepository.getTransactionsForMonth(year, month)
                } catch (e: Exception) {
                    emptyList()
                }

                MonthlyTrendPoint(
                    year = year,
                    month = month,
                    income = transactions.filter { it.type == "income" }.sumOf { it.amount },
                    expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
                )
            }
            _uiState.update { it.copy(monthlyTrend = points) }
        }
    }

    fun setBudget(category: String, monthlyLimit: Double) {
        viewModelScope.launch {
            val state = _uiState.value
            budgetRepository.setBudget(
                Budget(
                    category = category,
                    monthlyLimit = monthlyLimit,
                    year = state.displayedYear,
                    month = state.displayedMonth
                )
            )
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            val state = _uiState.value
            budgetRepository.deleteBudget(state.displayedYear, state.displayedMonth, category)
        }
    }
}