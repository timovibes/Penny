package com.example.penny.util


object CurrencyFormatter {

    val supportedCurrencies = listOf("KES", "USD", "EUR", "GBP", "UGX", "TZS")

    private val symbols = mapOf(
        "KES" to "KSh",
        "USD" to "$",
        "EUR" to "\u20AC",
        "GBP" to "\u00A3",
        "UGX" to "USh",
        "TZS" to "TSh"
    )

    /**
     * @param amountInKes the raw amount as stored in Firestore/Room (always KES)
     * @param targetCurrency the currency the user picked in Profile
     * @param rates map of currency code -> rate relative to KES, from ExchangeRateRepository
     */
    fun format(amountInKes: Double, targetCurrency: String, rates: Map<String, Double>): String {
        val rate = rates[targetCurrency] ?: 1.0
        val converted = amountInKes * rate
        val symbol = symbols[targetCurrency] ?: targetCurrency
        return "$symbol %,.2f".format(converted)
    }
}