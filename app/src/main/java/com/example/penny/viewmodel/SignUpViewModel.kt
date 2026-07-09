package com.example.penny.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.penny.data.AuthResult
import com.example.penny.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {

    // Repository instance
    private val repository = AuthRepository()

    // ─── FORM STATE ───────────────────────────────────────────────────────────
    // These hold what the user is typing in real time
    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isTermsAccepted = MutableStateFlow(false)
    val isTermsAccepted = _isTermsAccepted.asStateFlow()
    // ──────────────────────────────────────────────────────────────────────────


    // ─── AUTH STATE ───────────────────────────────────────────────────────────
    // This tells the screen whether we are loading, succeeded, or failed
    private val _authState = MutableStateFlow<AuthResult?>(null)
    val authState = _authState.asStateFlow()
    // ──────────────────────────────────────────────────────────────────────────


    // ─── FORM UPDATE FUNCTIONS ────────────────────────────────────────────────
    // These are called by the screen when the user types or toggles
    fun onNameChange(value: String) { _name.value = value }
    fun onEmailChange(value: String) { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }
    fun onTermsCheckedChange(value: Boolean) { _isTermsAccepted.value = value }
    // ──────────────────────────────────────────────────────────────────────────


    // ─── SIGNUP FUNCTION ──────────────────────────────────────────────────────
    fun signUp() {
        viewModelScope.launch {
            // Tell the screen we are busy
            _authState.value = AuthResult.Loading

            // Call the repository and get the result
            val result = repository.signUp(_name.value, _email.value, _password.value)

            // Update the state with whatever Firebase returned
            _authState.value = result
        }
    }
    // ──────────────────────────────────────────────────────────────────────────

}