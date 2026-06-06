package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.FuelLog
import com.cartracker.data.db.entities.FuelType
import kotlinx.coroutines.launch

class FuelLogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val fuelLogs: LiveData<List<FuelLog>> = _carId.switchMap { carId ->
        repository.getFuelLogsForCar(carId).asLiveData()
    }

    private val _lastOdometer = MutableLiveData<Double?>(null)
    val lastOdometer: LiveData<Double?> = _lastOdometer

    private val _lastPricePerLiter = MutableLiveData<Double?>(null)
    val lastPricePerLiter: LiveData<Double?> = _lastPricePerLiter

    fun setCarId(carId: Long) {
        _carId.value = carId
        loadLastValues(carId)
    }

    private fun loadLastValues(carId: Long) {
        viewModelScope.launch {
            _lastOdometer.value = repository.getLatestFuelLog(carId)?.odometer
            _lastPricePerLiter.value = repository.getLastPricePerLiter(carId)
        }
    }

    fun addFuelLog(
        carId: Long, date: Long, odometer: Double, liters: Double,
        costPerLiter: Double, isFullTank: Boolean, fuelType: FuelType, notes: String
    ) {
        viewModelScope.launch {
            val prev = repository.getLatestFuelLog(carId)
            val efficiency = calcEfficiency(prev?.odometer, odometer, liters, isFullTank)
            repository.insertFuelLog(
                FuelLog(
                    carId = carId, date = date, odometer = odometer, liters = liters,
                    costPerLiter = costPerLiter, totalCost = liters * costPerLiter,
                    fuelEfficiency = efficiency, isFullTank = isFullTank,
                    fuelType = fuelType, notes = notes
                )
            )
            repository.updateOdometer(carId, odometer)
            _lastOdometer.value = odometer
            _lastPricePerLiter.value = costPerLiter
        }
    }

    fun updateFuelLog(
        existing: FuelLog, date: Long, odometer: Double, liters: Double,
        costPerLiter: Double, isFullTank: Boolean, fuelType: FuelType, notes: String
    ) {
        viewModelScope.launch {
            val prev = repository.getPrevFuelLog(existing.carId, odometer)
            val efficiency = calcEfficiency(prev?.odometer, odometer, liters, isFullTank)
            repository.updateFuelLog(
                existing.copy(
                    date = date, odometer = odometer, liters = liters,
                    costPerLiter = costPerLiter, totalCost = liters * costPerLiter,
                    fuelEfficiency = if (efficiency > 0) efficiency else existing.fuelEfficiency,
                    isFullTank = isFullTank, fuelType = fuelType, notes = notes
                )
            )
        }
    }

    fun deleteFuelLog(log: FuelLog) = viewModelScope.launch { repository.deleteFuelLog(log) }

    private fun calcEfficiency(prevOdo: Double?, currentOdo: Double, liters: Double, isFullTank: Boolean): Double {
        if (!isFullTank) return 0.0
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
