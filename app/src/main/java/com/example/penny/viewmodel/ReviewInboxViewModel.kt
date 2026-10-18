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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class ReviewInboxUiState(
    val pending: List<PendingTransaction> = emptyList(),
    val history: List<PendingTransaction> = emptyList(), // confirmed + dismissed, newest first
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null // one-shot info message shown as a snackbar
)

class ReviewInboxViewModel @JvmOverloads constructor(
    application: Application,
    private val pendingRepository: PendingTransactionRepository = PendingTransactionRepository(),
    private val transactionRepository: TransactionRepository = TransactionRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReviewInboxUiState())
    val uiState: StateFlow<ReviewInboxUiState> = _uiState.asStateFlow()

    // Ids currently being confirmed — stops repeated taps from creating duplicate transactions
    private val inFlight = mutableSetOf<String>()

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
        if (item.status != "pending" || !inFlight.add(item.id)) return
        viewModelScope.launch {
            try {
                val addTask = transactionRepository.addTransactionQueued(
                    Transaction(
                        amount = amount,
                        type = item.type,
                        category = category,
                        note = note,
                        date = item.date,
                        currency = item.currency
                    )
                )
                val statusTask = pendingRepository.updateStatusQueued(item.id, "confirmed")

                // Online, the server acknowledges almost instantly. Offline it never
                // does, so after 1.5s we tell the user it's saved locally.
                val synced = withTimeoutOrNull(1500) {
                    addTask.await()
                    statusTask.await()
                    true
                } ?: false

                _uiState.value = _uiState.value.copy(
                    message = if (synced) "Transaction added"
                    else "Saved on this device. It will sync when you're back online."
                )
            } catch (e: Exception) {
                inFlight.remove(item.id)
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

    fun messageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}