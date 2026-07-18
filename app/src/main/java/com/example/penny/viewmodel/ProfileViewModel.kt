package com.example.penny.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.penny.data.local.BiometricPreferences
import com.example.penny.data.local.CurrencyPreferences
import com.example.penny.data.repository.AuthRepository
import com.example.penny.ui.screens.ProfileUiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val repository = AuthRepository()
    private val currencyPreferences = CurrencyPreferences(application)
    private val biometricPreferences = BiometricPreferences(application)

    // Exposed separately (not baked into loadState()) so the UI updates live —
    // loadState() is a one-off snapshot, these two are reactive.
    private val _currencyCode = MutableStateFlow(CurrencyPreferences.DEFAULT_CURRENCY)
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            currencyPreferences.currencyCode.collect { code ->
                _currencyCode.value = code
            }
        }
        viewModelScope.launch {
            biometricPreferences.isEnabled.collect { enabled ->
                _biometricEnabled.value = enabled
            }
        }
    }

    fun loadState(): ProfileUiState {
        val user = auth.currentUser
        val name = user?.displayName?.takeIf { it.isNotBlank() } ?: "User"
        val email = user?.email ?: ""

        return ProfileUiState(
            name = name,
            email = email
        )
    }

    fun setCurrency(code: String) {
        viewModelScope.launch {
            currencyPreferences.setCurrency(code)
        }
    }

    // Called only after the fingerprint prompt has already succeeded (see ProfileScreen),
    // or directly when turning the toggle off — no prompt needed to disable.
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            biometricPreferences.setEnabled(enabled)
        }
    }

    fun logout() {
        repository.signOut()
    }
}