package com.example.penny.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.biometricDataStore: DataStore<Preferences> by preferencesDataStore(name = "biometric_prefs")

class BiometricPreferences(private val context: Context) {

    companion object {
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    }

    val isEnabled: Flow<Boolean> = context.biometricDataStore.data
        .map { prefs -> prefs[BIOMETRIC_ENABLED_KEY] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.biometricDataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }
}