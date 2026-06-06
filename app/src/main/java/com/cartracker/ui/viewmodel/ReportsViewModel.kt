package com.cartracker.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.dao.MaintenanceCategoryTotal
import com.cartracker.data.db.entities.TripPurpose
import com.cartracker.util.ExportUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ReportsData(
    val totalFuelCost: Double = 0.0,
    val totalMaintenanceCost: Double = 0.0,
    val totalExpenseCost: Double = 0.0,
    val totalOwnershipCost: Double = 0.0,
    val totalKmDriven: Double = 0.0,
    val costPerKm: Double = 0.0,
    val workKm: Double = 0.0,
    val personalKm: Double = 0.0,
    val maintenanceByCategory: List<MaintenanceCategoryTotal> = emptyList(),
    val avgMonthlyFuel: Double = 0.0,
    val avgMonthlyMaint: Double = 0.0,
    val totalFillUps: Int = 0,
    val avgEfficiency: Double = 0.0
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _data = MutableLiveData(ReportsData())
    val data: LiveData<ReportsData> = _data

    fun loadReports(carId: Long) {
        viewModelScope.launch {
            val fuelLogs = repository.getAllFuelLogsSorted(carId)
            val trips = repository.getTripsForCar(carId).first()
            val totalFuelCost = repository.getFuelTotalAllTime(carId) ?: 0.0
            val totalMaintCost = repository.getMaintenanceTotalAllTime(carId) ?: 0.0
            val totalExpenseCost = repository.getExpenseTotalAllTime(carId) ?: 0.0
            val totalOwnershipCost = totalFuelCost + totalMaintCost + totalExpenseCost

            val totalKm = repository.getTotalMileage(carId) ?: 0.0
            val costPerKm = if (totalKm > 0) totalOwnershipCost / totalKm else 0.0

            val workKm = trips.filter { it.purpose == TripPurpose.WORK }.sumOf { it.distance }
            val personalKm = trips.filter { it.purpose == TripPurpose.PERSONAL }.sumOf { it.distance }

            val maintByCategory = repository.getMaintenanceTotalByCategory(carId)

            // Average monthly cost (based on months with data)
            val avgEfficiency = repository.getAverageFuelEfficiency(carId) ?: 0.0
            val totalFillUps = fuelLogs.size

            // Rough monthly averages (divide total by months of data)
            val car = repository.getCarById(carId)
            val createdAt = car?.createdAt ?: System.currentTimeMillis()
            val monthsSinceCreated = ((System.currentTimeMillis() - createdAt) / (1000L * 60 * 60 * 24 * 30.44)).coerceAtLeast(1.0)
            val avgMonthlyFuel = totalFuelCost / monthsSinceCreated
            val avgMonthlyMaint = totalMaintCost / monthsSinceCreated

            _data.value = ReportsData(
                totalFuelCost = totalFuelCost,
                totalMaintenanceCost = totalMaintCost,
                totalExpenseCost = totalExpenseCost,
                totalOwnershipCost = totalOwnershipCost,
                totalKmDriven = totalKm,
                costPerKm = costPerKm,
                workKm = workKm,
                personalKm = personalKm,
                maintenanceByCategory = maintByCategory,
                avgMonthlyFuel = avgMonthlyFuel,
                avgMonthlyMaint = avgMonthlyMaint,
                totalFillUps = totalFillUps,
                avgEfficiency = avgEfficiency
            )
        }
    }

    fun exportFuelCsv(context: Context, carId: Long, currency: String) {
        viewModelScope.launch {
            val logs = repository.getAllFuelLogsSorted(carId)
            ExportUtil.shareText(context, "fuel_logs.csv", ExportUtil.buildFuelCsv(logs, currency))
        }
    }

    fun exportMaintenanceCsv(context: Context, carId: Long, currency: String) {
        viewModelScope.launch {
            val logs = repository.getMaintenanceLogsForCar(carId).first()
            ExportUtil.shareText(context, "maintenance_log.csv", ExportUtil.buildMaintenanceCsv(logs, currency))
        }
    }

    fun exportExpensesCsv(context: Context, carId: Long, currency: String) {
        viewModelScope.launch {
            val expenses = repository.getExpensesForCar(carId).first()
            ExportUtil.shareText(context, "expenses.csv", ExportUtil.buildExpenseCsv(expenses, currency))
        }
    }

    fun exportPdfReport(context: Context, carId: Long, currency: String) {
        viewModelScope.launch {
            val car = repository.getCarById(carId)
            val fuelLogs = repository.getAllFuelLogsSorted(carId)
            val maintLogs = repository.getMaintenanceLogsForCar(carId).first()
            val expenses = repository.getExpensesForCar(carId).first()
            val totalFuel = repository.getFuelTotalAllTime(carId) ?: 0.0
            val totalMaint = repository.getMaintenanceTotalAllTime(carId) ?: 0.0
            val totalExpense = repository.getExpenseTotalAllTime(carId) ?: 0.0
            ExportUtil.buildPdfReport(context, car, fuelLogs, maintLogs, expenses, totalFuel, totalMaint, totalExpense, currency)
        }
    }
}

class ReportsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ReportsViewModel(application) as T
    }
}
