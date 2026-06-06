package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.FuelLog
import com.cartracker.data.db.entities.MaintenanceLog
import kotlinx.coroutines.flow.combine

sealed class HistoryEvent {
    data class Fuel(val log: FuelLog) : HistoryEvent()
    data class Service(val log: MaintenanceLog) : HistoryEvent()

    val date: Long get() = when (this) {
        is Fuel    -> log.date
        is Service -> log.date
    }
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val events: LiveData<List<HistoryEvent>> = _carId.switchMap { carId ->
        combine(
            repository.getFuelLogsForCar(carId),
            repository.getMaintenanceLogsForCar(carId)
        ) { fuelLogs, maintenanceLogs ->
            val events = mutableListOf<HistoryEvent>()
            fuelLogs.forEach { events += HistoryEvent.Fuel(it) }
            maintenanceLogs.forEach { events += HistoryEvent.Service(it) }
            events.sortedByDescending { it.date }
        }.asLiveData()
    }

    fun setCarId(carId: Long) { _carId.value = carId }
}

class HistoryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HistoryViewModel(application) as T
    }
}
