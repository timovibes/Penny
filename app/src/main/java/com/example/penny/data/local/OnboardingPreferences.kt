package com.example.penny.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")

class OnboardingPreferences(private val context: Context) {

    companion object {
        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
    }

    // Emits true once the user has completed or skipped the walkthrough
    val hasSeenOnboarding: Flow<Boolean> = context.onboardingDataStore.data
        .map { prefs -> prefs[HAS_SEEN_ONBOARDING_KEY] ?: false }

    suspend fun setOnboardingSeen() {
        context.onboardingDataStore.edit { prefs ->
            prefs[HAS_SEEN_ONBOARDING_KEY] = true
        }
    }
}