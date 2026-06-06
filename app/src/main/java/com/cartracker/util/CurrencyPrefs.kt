package com.cartracker.util

import android.content.Context

object CurrencyPrefs {
    private const val PREF_NAME = "currency_prefs"
    private const val KEY_SYMBOL = "currency_symbol"
    private const val DEFAULT = "BD"

    val supported = listOf("BD", "USD", "EUR", "GBP", "AED", "SAR", "KWD", "QAR")

    fun getSymbol(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SYMBOL, DEFAULT) ?: DEFAULT

    fun setSymbol(context: Context, symbol: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SYMBOL, symbol).apply()
    }

    fun format(amount: Double, symbol: String): String =
        "$symbol %.3f".format(amount)
}
