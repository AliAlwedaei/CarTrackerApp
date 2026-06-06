package com.cartracker.util

import android.content.Context

object BudgetPrefs {
    private const val PREF_NAME = "budget_prefs"
    private const val KEY_MONTHLY_FUEL = "monthly_fuel_budget"
    private const val KEY_MONTHLY_TOTAL = "monthly_total_budget"

    fun getMonthlyFuelBudget(context: Context): Double? {
        val v = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_MONTHLY_FUEL, -1f)
        return if (v < 0) null else v.toDouble()
    }

    fun setMonthlyFuelBudget(context: Context, amount: Double?) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .apply { if (amount != null) putFloat(KEY_MONTHLY_FUEL, amount.toFloat()) else remove(KEY_MONTHLY_FUEL) }
            .apply()
    }

    fun getMonthlyTotalBudget(context: Context): Double? {
        val v = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_MONTHLY_TOTAL, -1f)
        return if (v < 0) null else v.toDouble()
    }

    fun setMonthlyTotalBudget(context: Context, amount: Double?) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .apply { if (amount != null) putFloat(KEY_MONTHLY_TOTAL, amount.toFloat()) else remove(KEY_MONTHLY_TOTAL) }
            .apply()
    }
}
