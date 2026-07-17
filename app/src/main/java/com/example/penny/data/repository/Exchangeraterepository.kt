package com.example.penny.data.repository


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ExchangeRateRepository {

    // Free, no API key required. Base is KES since that's how Penny stores amounts internally.
    private val endpoint = "https://open.er-api.com/v6/latest/KES"

    /**
     * Returns a map like {"USD" -> 0.0078, "EUR" -> 0.0072, "KES" -> 1.0, ...}
     * Each value is: 1 KES = X of that currency.
     * Falls back to KES-only (1:1) if the network call fails, so the UI never crashes.
     */
    suspend fun getRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val ratesJson = json.getJSONObject("rates")

            val rates = mutableMapOf<String, Double>()
            ratesJson.keys().forEach { key ->
                rates[key] = ratesJson.getDouble(key)
            }
            rates
        } catch (e: Exception) {
            mapOf("KES" to 1.0)
        }
    }
}