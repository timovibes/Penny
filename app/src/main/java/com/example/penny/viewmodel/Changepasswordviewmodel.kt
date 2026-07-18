package com.example.penny.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChangePasswordViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var isSaving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun changePassword(onSuccess: () -> Unit) {
        error = null

        if (currentPassword.isBlank()) {
            error = "Enter your current password"
            return
        }
        if (newPassword.length < 6) {
            error = "New password must be at least 6 characters"
            return
        }
        if (newPassword != confirmPassword) {
            error = "New passwords don't match"
            return
        }

        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            error = "No signed-in user found"
            return
        }

        viewModelScope.launch {
            isSaving = true
            try {
                // Firebase requires a recent sign-in before allowing a password change
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                reauthenticate(user, credential)
                updatePassword(user, newPassword)
                onSuccess()
            } catch (e: Exception) {
                error = mapError(e)
            } finally {
                isSaving = false
            }
        }
    }

    private suspend fun reauthenticate(
        user: com.google.firebase.auth.FirebaseUser,
        credential: com.google.firebase.auth.AuthCredential
    ) = suspendCancellableCoroutine<Unit> { cont ->
        user.reauthenticate(credential)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private suspend fun updatePassword(
        user: com.google.firebase.auth.FirebaseUser,
        newPassword: String
    ) = suspendCancellableCoroutine<Unit> { cont ->
        user.updatePassword(newPassword)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private fun mapError(e: Exception): String {
        return when {
            e.message?.contains("password is invalid", ignoreCase = true) == true ->
                "Current password is incorrect"
            e.message?.contains("credential", ignoreCase = true) == true ->
                "Current password is incorrect"
            else -> e.message ?: "Something went wrong. Try again."
        }
    }

    fun resetFields() {
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
        error = null
    }
}