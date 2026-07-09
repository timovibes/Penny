package com.example.penny.viewmodel

import androidx.lifecycle.ViewModel
import com.example.penny.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.example.penny.ui.screens.ProfileUiState

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = AuthRepository()

    fun loadState(): ProfileUiState {
        val user = auth.currentUser
        val name = user?.displayName?.takeIf { it.isNotBlank() } ?: "User"
        val email = user?.email ?: ""

        return ProfileUiState(
            name = name,
            email = email
        )
    }

    fun logout() {
        repository.signOut()
    }
}