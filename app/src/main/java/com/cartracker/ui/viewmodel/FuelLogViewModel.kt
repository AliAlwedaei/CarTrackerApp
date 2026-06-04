package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.FuelLog
import kotlinx.coroutines.launch

class FuelLogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val fuelLogs: LiveData<List<FuelLog>> = _carId.switchMap { carId ->
        repository.getFuelLogsForCar(carId).asLiveData()
    }

    private val _lastOdometer = MutableLiveData<Double?>(null)
    val lastOdometer: LiveData<Double?> = _lastOdometer

    fun setCarId(carId: Long) {
        _carId.value = carId
        loadLastOdometer(carId)
    }

    fun loadLastOdometer(carId: Long) {
        viewModelScope.launch {
            _lastOdometer.value = repository.getLatestFuelLog(carId)?.odometer
        }
    }

    fun addFuelLog(carId: Long, date: Long, odometer: Double, liters: Double, costPerLiter: Double, notes: String) {
        viewModelScope.launch {
            val prev = repository.getLatestFuelLog(carId)
            val efficiency = calcEfficiency(prev?.odometer, odometer, liters)
            repository.insertFuelLog(
                FuelLog(
                    carId = carId, date = date, odometer = odometer, liters = liters,
                    costPerLiter = costPerLiter, totalCost = liters * costPerLiter,
                    fuelEfficiency = efficiency, notes = notes
                )
            )
            repository.updateOdometer(carId, odometer)
            _lastOdometer.value = odometer
        }
    }

    fun updateFuelLog(existing: FuelLog, date: Long, odometer: Double, liters: Double, costPerLiter: Double, notes: String) {
        viewModelScope.launch {
            val prev = repository.getPrevFuelLog(existing.carId, odometer)
            val efficiency = calcEfficiency(prev?.odometer, odometer, liters)
            repository.updateFuelLog(
                existing.copy(
                    date = date, odometer = odometer, liters = liters,
                    costPerLiter = costPerLiter, totalCost = liters * costPerLiter,
                    fuelEfficiency = if (efficiency > 0) efficiency else existing.fuelEfficiency,
                    notes = notes
                )
            )
        }
    }

    fun deleteFuelLog(log: FuelLog) = viewModelScope.launch { repository.deleteFuelLog(log) }

    private fun calcEfficiency(prevOdo: Double?, currentOdo: Double, liters: Double): Double {
        if (prevOdo == null || currentOdo <= prevOdo || liters <= 0) return 0.0
        val l100km = liters / (currentOdo - prevOdo) * 100.0
        return if (l100km > 0) 100.0 / l100km else 0.0
    }
}

class FuelLogViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FuelLogViewModel(application) as T
    }
}
