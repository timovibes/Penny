package com.example.penny.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.currencyDataStore: DataStore<Preferences> by preferencesDataStore(name = "currency_prefs")

class CurrencyPreferences(private val context: Context) {

    companion object {
        private val CURRENCY_CODE_KEY = stringPreferencesKey("currency_code")
        const val DEFAULT_CURRENCY = "KES"
    }

    // Emits the saved currency code, or KES if nothing has been picked yet
    val currencyCode: Flow<String> = context.currencyDataStore.data
        .map { prefs -> prefs[CURRENCY_CODE_KEY] ?: DEFAULT_CURRENCY }

    suspend fun setCurrency(code: String) {
        context.currencyDataStore.edit { prefs ->
            prefs[CURRENCY_CODE_KEY] = code
        }
    }
}