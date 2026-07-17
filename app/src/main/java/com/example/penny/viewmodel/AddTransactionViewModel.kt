package com.example.penny.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.penny.data.local.CurrencyPreferences
import com.example.penny.data.model.Transaction
import com.example.penny.data.repository.ExchangeRateRepository
import com.example.penny.data.repository.TransactionRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class AddTransactionViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: TransactionRepository = TransactionRepository()
) : AndroidViewModel(application) {

    private val currencyPreferences = CurrencyPreferences(application)
    private val exchangeRateRepository = ExchangeRateRepository()

    var amount    by mutableStateOf("")
    var type      by mutableStateOf("expense")   // "income" | "expense"
    var category  by mutableStateOf("")
    var note      by mutableStateOf("")
    var date      by mutableStateOf(LocalDate.now())

    var isSaving  by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    // The currency the amount field is currently showing (follows the user's Profile choice)
    var currencyCode by mutableStateOf(CurrencyPreferences.DEFAULT_CURRENCY)
        private set

    private var exchangeRates: Map<String, Double> = mapOf("KES" to 1.0)

    init {
        viewModelScope.launch {
            currencyPreferences.currencyCode.collect { code ->
                currencyCode = code
                exchangeRates = exchangeRateRepository.getRates()
            }
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val enteredAmount = amount.toDoubleOrNull()
        if (enteredAmount == null || enteredAmount <= 0) return

        // Amount was typed in the user's chosen currency (e.g. USD).
        // Convert it back to KES before saving, since that's how Penny stores data internally.
        val rate = exchangeRates[currencyCode] ?: 1.0
        val amountInKes = if (rate == 0.0) enteredAmount else enteredAmount / rate

        val instant   = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timestamp = Timestamp(instant.epochSecond, 0)

        val transaction = Transaction(
            amount   = amountInKes,
            type     = type,
            category = category,
            note     = note.trim(),
            date     = timestamp
        )

        viewModelScope.launch {
            isSaving  = true
            saveError = null
            try {
                repository.addTransaction(transaction)
                onSuccess()
            } catch (e: Exception) {
                saveError = e.message
            } finally {
                isSaving = false
            }
        }
    }
}