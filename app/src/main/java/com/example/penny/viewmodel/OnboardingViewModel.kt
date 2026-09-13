package com.example.penny.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.penny.data.local.OnboardingPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val onboardingPreferences = OnboardingPreferences(application)

    // null while we're still reading from disk, then true/false once known
    val hasSeenOnboarding: StateFlow<Boolean?> = onboardingPreferences.hasSeenOnboarding
        .map { seen -> seen as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun markOnboardingComplete(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            onboardingPreferences.setOnboardingSeen()
            onDone()
        }
    }
}