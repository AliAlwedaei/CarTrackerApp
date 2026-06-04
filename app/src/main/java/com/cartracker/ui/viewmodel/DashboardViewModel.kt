package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.FuelLog
import com.cartracker.data.db.entities.MaintenanceLog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class MonthlyFuelStat(val label: String, val cost: Double, val km: Double)

data class DashboardStats(
    val totalMileage: Double = 0.0,
    val lastServiceDate: Long? = null,
    val avgFuelEfficiency: Double = 0.0,
    val monthlyFuelCost: Double = 0.0,
    val recentFuelLogs: List<FuelLog> = emptyList(),
    val avgKmPerTank: Double = 0.0,
    val avgDaysBetweenFillups: Double = 0.0,
    val costPerKm: Double = 0.0,
    val monthlyStats: List<MonthlyFuelStat> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _stats = MutableLiveData(DashboardStats())
    val stats: LiveData<DashboardStats> = _stats

    fun loadStats(carId: Long) {
        viewModelScope.launch {
            val totalMileage = repository.getTotalMileage(carId) ?: 0.0
            val lastService: MaintenanceLog? = repository.getLastService(carId)
            val avgEfficiency = repository.getAverageFuelEfficiency(carId) ?: 0.0

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val monthlyCost = repository.getMonthlyCost(carId, monthStart) ?: 0.0
            val recentFrom = monthStart - 6L * 30 * 24 * 60 * 60 * 1000
            val recentLogs = repository.getFuelLogsFrom(carId, recentFrom)

            // Full history sorted by odometer for analytics
            val allLogs = repository.getAllFuelLogsSorted(carId)

            // Avg km per tank (odometer delta between consecutive fillups)
            val odometerDiffs = allLogs.zipWithNext { a, b -> b.odometer - a.odometer }
                .filter { it > 0 }
            val avgKmPerTank = if (odometerDiffs.isNotEmpty()) odometerDiffs.average() else 0.0

            // Avg days between fillups
            val dateDiffs = allLogs.sortedBy { it.date }
                .zipWithNext { a, b -> (b.date - a.date) / (1000.0 * 60 * 60 * 24) }
                .filter { it > 0 }
            val avgDaysBetweenFillups = if (dateDiffs.isNotEmpty()) dateDiffs.average() else 0.0

            // Cost per km
            val totalCost = allLogs.sumOf { it.totalCost }
            val totalKmFromFuel = allLogs.sumOf { it.fuelEfficiency * it.liters }
            val costPerKm = if (totalKmFromFuel > 0) totalCost / totalKmFromFuel else 0.0

            // Monthly stats — last 6 months
            val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
            val monthlyStats = buildList {
                for (offset in 5 downTo 0) {
                    val startCal = Calendar.getInstance().apply {
                        add(Calendar.MONTH, -offset)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val endCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    val label = monthFmt.format(startCal.time)
                    val logsInMonth = allLogs.filter {
                        it.date >= startCal.timeInMillis && it.date < endCal.timeInMillis
                    }
                    add(
                        MonthlyFuelStat(
                            label = label,
                            cost = logsInMonth.sumOf { it.totalCost },
                            km = logsInMonth.sumOf { it.fuelEfficiency * it.liters }
                        )
                    )
                }
            }

            _stats.value = DashboardStats(
                totalMileage = totalMileage,
                lastServiceDate = lastService?.date,
                avgFuelEfficiency = avgEfficiency,
                monthlyFuelCost = monthlyCost,
                recentFuelLogs = recentLogs,
                avgKmPerTank = avgKmPerTank,
                avgDaysBetweenFillups = avgDaysBetweenFillups,
                costPerKm = costPerKm,
                monthlyStats = monthlyStats
            )
        }
    }
}

class DashboardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(application) as T
    }
}
