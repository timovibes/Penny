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
            pendingRepository.observePending().collect { pending ->
                _uiState.value = _uiState.value.copy(pending = pending, isLoading = false)
            }
        }
    }

    // Confirms a pending item, optionally with user-edited amount/category/note,
    // writes it to the real ledger, then removes it from the staging collection.
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
                pendingRepository.deletePending(item.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun dismiss(item: PendingTransaction) {
        viewModelScope.launch {
            try {
                pendingRepository.deletePending(item.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}