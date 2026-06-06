package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.MaintenanceCategory
import com.cartracker.data.db.entities.MaintenanceLog
import kotlinx.coroutines.launch

class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val maintenanceLogs: LiveData<List<MaintenanceLog>> = _carId.switchMap { carId ->
        repository.getMaintenanceLogsForCar(carId).asLiveData()
    }

    fun setCarId(carId: Long) { _carId.value = carId }

    fun addMaintenanceLog(
        carId: Long, category: MaintenanceCategory, serviceType: String,
        date: Long, mileage: Double, cost: Double, garage: String,
        nextServiceKm: Double?, notes: String
    ) {
        viewModelScope.launch {
            repository.insertMaintenanceLog(
                MaintenanceLog(
                    carId = carId, category = category, serviceType = serviceType,
                    date = date, mileage = mileage, cost = cost,
                    garage = garage, nextServiceKm = nextServiceKm, notes = notes
                )
            )
            // Auto-create reminder for next service if specified
            nextServiceKm?.let { km ->
                repository.insertReminder(
                    com.cartracker.data.db.entities.Reminder(
                        carId = carId,
                        title = "Next $serviceType",
                        type = com.cartracker.data.db.entities.ReminderType.MILEAGE,
                        targetMileage = km,
                        notes = "Auto-created from service log"
                    )
                )
            }
        }
    }

    fun updateLog(
        existing: MaintenanceLog, category: MaintenanceCategory, serviceType: String,
        date: Long, mileage: Double, cost: Double, garage: String,
        nextServiceKm: Double?, notes: String
    ) {
        viewModelScope.launch {
            repository.updateMaintenanceLog(
                existing.copy(
                    category = category, serviceType = serviceType,
                    date = date, mileage = mileage, cost = cost,
                    garage = garage, nextServiceKm = nextServiceKm, notes = notes
                )
            )
        }
    }

    fun deleteLog(log: MaintenanceLog) = viewModelScope.launch { repository.deleteMaintenanceLog(log) }
}

class MaintenanceViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MaintenanceViewModel(application) as T
    }
}
