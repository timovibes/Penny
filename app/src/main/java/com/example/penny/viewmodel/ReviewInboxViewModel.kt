package com.example.penny.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.penny.data.model.PendingTransaction
import com.example.penny.data.model.Transaction
import com.example.penny.data.repository.PendingTransactionRepository
import com.example.penny.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewInboxUiState(
    val pending: List<PendingTransaction> = emptyList(),
    val history: List<PendingTransaction> = emptyList(), // confirmed + dismissed, newest first
    val isLoading: Boolean = true,
    val error: String? = null
)

class ReviewInboxViewModel @JvmOverloads constructor(
    application: Application,
    private val pendingRepository: PendingTransactionRepository = PendingTransactionRepository(),
    private val transactionRepository: TransactionRepository = TransactionRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReviewInboxUiState())
    val uiState: StateFlow<ReviewInboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            pendingRepository.observeAll().collect { all ->
                _uiState.value = _uiState.value.copy(
                    pending = all.filter { it.status == "pending" },
                    history = all.filter { it.status != "pending" },
                    isLoading = false
                )
            }
        }
    }

    // Confirms a pending item, optionally with user-edited amount/category/note,
    // writes it to the real ledger, then marks the staged item "confirmed"
    // (kept, not deleted, so it shows up in History).
    fun confirm(
        item: PendingTransaction,
        amount: Double = item.amount,
        category: String = item.category,
        note: String = item.note
    ) {
        viewModelScope.launch {
            try {
                transactionRepository.addTransaction(
                    Transaction(
                        amount = amount,
                        type = item.type,
                        category = category,
                        note = note,
                        date = item.date,
                        currency = item.currency
                    )
                )
                pendingRepository.updateStatus(item.id, "confirmed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun dismiss(item: PendingTransaction) {
        viewModelScope.launch {
            try {
                pendingRepository.updateStatus(item.id, "dismissed")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}