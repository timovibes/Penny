package com.example.penny.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.penny.data.local.CurrencyPreferences
import com.example.penny.data.model.Transaction
import com.example.penny.data.repository.ExchangeRateRepository
import com.example.penny.data.repository.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DaySummary(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val transactionCount: Int = 0
)

data class HomeUiState(
    val displayedYear: Int = LocalDate.now().year,
    val displayedMonth: Int = LocalDate.now().monthValue, // 1–12
    val selectedDate: LocalDate? = null,
    val daySummaries: Map<LocalDate, DaySummary> = emptyMap(),
    val selectedDayTransactions: List<Transaction> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val totalBalance: Double = 0.0,
    val userInitial: String = "?",
    val isLoading: Boolean = true,
    val error: String? = null,
    // ── Currency ──
    val currencyCode: String = CurrencyPreferences.DEFAULT_CURRENCY,
    val exchangeRates: Map<String, Double> = mapOf("KES" to 1.0),
    val isRatesLoading: Boolean = false
)

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: TransactionRepository = TransactionRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var monthObserverJob: Job? = null

    private val currencyPreferences = CurrencyPreferences(application)
    private val exchangeRateRepository = ExchangeRateRepository()

    init {
        observeCurrentMonth()
        observeTotalBalance()
        loadUserInitial()
        observeCurrency()
    }

    // Watches the saved currency choice and re-fetches rates whenever it changes
    private fun observeCurrency() {
        viewModelScope.launch {
            currencyPreferences.currencyCode.collect { code ->
                _uiState.update { it.copy(currencyCode = code) }
                refreshRates()
            }
        }
    }

    private fun refreshRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRatesLoading = true) }
            val rates = exchangeRateRepository.getRates()
            _uiState.update { it.copy(exchangeRates = rates, isRatesLoading = false) }
        }
    }

    private fun observeTotalBalance() {
        viewModelScope.launch {
            repository.observeAllTransactions()
                .catch { /* keep last known balance on error; don't crash UI */ }
                .collect { transactions ->
                    val balance = transactions
                        .filter { it.type == "income" }
                        .sumOf { it.amount } -
                            transactions
                                .filter { it.type == "expense" }
                                .sumOf { it.amount }

                    _uiState.update { it.copy(totalBalance = balance) }
                }
        }
    }

    private fun loadUserInitial() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val initial = user?.displayName?.trim()?.firstOrNull()?.uppercase()
            ?: user?.email?.trim()?.firstOrNull()?.uppercase()
            ?: "?"
        _uiState.update { it.copy(userInitial = initial) }
    }

    fun goToPreviousMonth() {
        val current = _uiState.value
        val (year, month) = if (current.displayedMonth == 1)
            current.displayedYear - 1 to 12
        else
            current.displayedYear to current.displayedMonth - 1

        _uiState.update { it.copy(displayedYear = year, displayedMonth = month, selectedDate = null, selectedDayTransactions = emptyList()) }
        observeCurrentMonth()
    }

    fun goToNextMonth() {
        val current = _uiState.value
        val (year, month) = if (current.displayedMonth == 12)
            current.displayedYear + 1 to 1
        else
            current.displayedYear to current.displayedMonth + 1

        _uiState.update { it.copy(displayedYear = year, displayedMonth = month, selectedDate = null, selectedDayTransactions = emptyList()) }
        observeCurrentMonth()
    }

    fun selectDay(date: LocalDate) {
        if (_uiState.value.selectedDate == date) {
            _uiState.update { it.copy(selectedDate = null, selectedDayTransactions = emptyList()) }
            return
        }
        val dayTransactions = _allMonthTransactions.filter { tx ->
            tx.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == date
        }
        _uiState.update { it.copy(selectedDate = date, selectedDayTransactions = dayTransactions) }
    }

    fun clearSelectedDay() {
        _uiState.update { it.copy(selectedDate = null, selectedDayTransactions = emptyList()) }
    }

    private var _allMonthTransactions: List<Transaction> = emptyList()

    private fun observeCurrentMonth() {
        monthObserverJob?.cancel()
        monthObserverJob = viewModelScope.launch {
            val year = _uiState.value.displayedYear
            val month = _uiState.value.displayedMonth
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.observeTransactionsForMonth(year, month)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { transactions ->
                    _allMonthTransactions = transactions

                    val summaries = transactions
                        .groupBy { tx ->
                            tx.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        .mapValues { (_, txList) ->
                            DaySummary(
                                totalIncome = txList.filter { it.type == "income" }.sumOf { it.amount },
                                totalExpenses = txList.filter { it.type == "expense" }.sumOf { it.amount },
                                transactionCount = txList.size
                            )
                        }

                    val selectedDate = _uiState.value.selectedDate
                    val updatedSelectedTx = if (selectedDate != null)
                        transactions.filter { tx ->
                            tx.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate
                        }
                    else emptyList()

                    _uiState.update {
                        it.copy(
                            daySummaries = summaries,
                            monthlyIncome = transactions.filter { tx -> tx.type == "income" }.sumOf { tx -> tx.amount },
                            monthlyExpenses = transactions.filter { tx -> tx.type == "expense" }.sumOf { tx -> tx.amount },
                            selectedDayTransactions = updatedSelectedTx,
                            isLoading = false
                        )
                    }
                }
        }
    }
}