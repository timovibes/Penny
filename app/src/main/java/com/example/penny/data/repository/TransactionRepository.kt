package com.example.penny.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.penny.data.model.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class TransactionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun transactionsCollection() =
        firestore
            .collection("users")
            .document(auth.currentUser!!.uid)
            .collection("transactions")

    suspend fun addTransaction(transaction: Transaction) {
        transactionsCollection()
            .add(transaction)
            .await()
    }

    suspend fun getTransactionsForMonth(year: Int, month: Int): List<Transaction> {
        val startCalendar = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = Calendar.getInstance().apply {
            set(year, month - 1, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val snapshot = transactionsCollection()
            .whereGreaterThanOrEqualTo("date", Timestamp(startCalendar.time))
            .whereLessThanOrEqualTo("date", Timestamp(endCalendar.time))
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Transaction::class.java)?.copy(id = doc.id)
        }
    }

    // Real-time listener version — used by HomeViewModel to react to Firestore changes live
    fun observeTransactionsForMonth(year: Int, month: Int): Flow<List<Transaction>> = callbackFlow {
        val startCalendar = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = Calendar.getInstance().apply {
            set(year, month - 1, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val listener = transactionsCollection()
            .whereGreaterThanOrEqualTo("date", Timestamp(startCalendar.time))
            .whereLessThanOrEqualTo("date", Timestamp(endCalendar.time))
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(transactions)
            }

        awaitClose { listener.remove() }
    }

    suspend fun deleteTransaction(transactionId: String) {
        transactionsCollection()
            .document(transactionId)
            .delete()
            .await()
    }
}