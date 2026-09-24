package com.example.penny.data.repository

import com.example.penny.data.model.PendingTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    // Streams every staged item regardless of status — ReviewInboxViewModel
    // splits this into "pending" and "history" (confirmed/dismissed) lists.
    // Kept as one listener/collection rather than two separate queries so
    // Home's badge and the review screen's History tab share the same data.
    fun observeAll(): Flow<List<PendingTransaction>> = callbackFlow {
        val listener = pendingCollection()
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Don't propagate — an uncaught listener error here would crash the app.
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                val pending = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PendingTransaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(pending)
            }

        awaitClose { listener.remove() }
    }

    // Confirmed/dismissed items are kept (not deleted) so History can show them
    suspend fun updateStatus(pendingId: String, status: String) {
        pendingCollection()
            .document(pendingId)
            .update("status", status)
            .await()
    }

    suspend fun deletePending(pendingId: String) {
        pendingCollection()
            .document(pendingId)
            .delete()
            .await()
    }
}