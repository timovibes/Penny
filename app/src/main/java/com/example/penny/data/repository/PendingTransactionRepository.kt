package com.example.penny.data.repository

import com.example.penny.data.model.PendingTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PendingTransactionRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun pendingCollection() =
        firestore
            .collection("users")
            .document(auth.currentUser!!.uid)
            .collection("pending_transactions")

    suspend fun addPending(pending: PendingTransaction) {
        pendingCollection()
            .add(pending)
            .await()
    }

    // Real-time listener — drives the review inbox badge count and list
    fun observePending(): Flow<List<PendingTransaction>> = callbackFlow {
        val listener = pendingCollection()
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val pending = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PendingTransaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(pending)
            }

        awaitClose { listener.remove() }
    }

    suspend fun deletePending(pendingId: String) {
        pendingCollection()
            .document(pendingId)
            .delete()
            .await()
    }
}