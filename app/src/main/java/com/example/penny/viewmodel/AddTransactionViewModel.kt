package com.example.penny.viewmodel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.penny.data.model.Transaction
import com.example.penny.data.repository.TransactionRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class AddTransactionViewModel : ViewModel() {

    private val repository = TransactionRepository()

    var amount    by mutableStateOf("")
    var type      by mutableStateOf("expense")   // "income" | "expense"
    var category  by mutableStateOf("")
    var note      by mutableStateOf("")
    var date      by mutableStateOf(LocalDate.now())

    var isSaving  by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    fun saveTransaction(onSuccess: () -> Unit) {
        val parsedAmount = amount.toDoubleOrNull()
        if (parsedAmount == null || parsedAmount <= 0) return

        val instant   = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timestamp = Timestamp(instant.epochSecond, 0)

        val transaction = Transaction(
            amount   = parsedAmount,
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