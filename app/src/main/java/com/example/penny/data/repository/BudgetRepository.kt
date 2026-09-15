package com.example.penny.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.penny.data.model.Budget
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BudgetRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun budgetsCollection() =
        firestore
            .collection("users")
            .document(auth.currentUser!!.uid)
            .collection("budgets")

    // Deterministic doc ID so setting a budget for a category+month always
    // overwrites the existing one instead of creating duplicates
    private fun docId(year: Int, month: Int, category: String) = "${year}-${month}-${category}"

    suspend fun setBudget(budget: Budget) {
        val id = docId(budget.year, budget.month, budget.category)
        budgetsCollection()
            .document(id)
            .set(budget.copy(id = id))
            .await()
    }

    suspend fun deleteBudget(year: Int, month: Int, category: String) {
        budgetsCollection()
            .document(docId(year, month, category))
            .delete()
            .await()
    }

    // Real-time listener — used by AnalyticsViewModel so progress bars update
    // live as transactions are added, same pattern as TransactionRepository
    fun observeBudgetsForMonth(year: Int, month: Int): Flow<List<Budget>> = callbackFlow {
        val listener = budgetsCollection()
            .whereEqualTo("year", year)
            .whereEqualTo("month", month)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val budgets = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Budget::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(budgets)
            }

        awaitClose { listener.remove() }
    }
}