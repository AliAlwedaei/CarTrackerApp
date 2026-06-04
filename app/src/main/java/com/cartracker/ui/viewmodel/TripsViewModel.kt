package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.Trip
import com.cartracker.data.db.entities.TripPurpose
import kotlinx.coroutines.launch

class TripsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val trips: LiveData<List<Trip>> = _carId.switchMap { carId ->
        repository.getTripsForCar(carId).asLiveData()
    }

    private val _totalMileage = MutableLiveData(0.0)
    val totalMileage: LiveData<Double> = _totalMileage

    private val _lastTrip = MutableLiveData<Trip?>(null)
    val lastTrip: LiveData<Trip?> = _lastTrip

    fun setCarId(carId: Long) {
        _carId.value = carId
        viewModelScope.launch {
            _totalMileage.value = repository.getTotalMileage(carId) ?: 0.0
            _lastTrip.value = repository.getLastTrip(carId)
        }
    }

    fun addTrip(carId: Long, date: Long, startOdo: Double, endOdo: Double, purpose: TripPurpose, notes: String) {
        viewModelScope.launch {
            val distance = endOdo - startOdo
            repository.insertTrip(
                Trip(carId = carId, date = date, startOdometer = startOdo,
                    endOdometer = endOdo, distance = distance, purpose = purpose, notes = notes)
            )
            if (endOdo > startOdo) repository.updateOdometer(carId, endOdo)
            _totalMileage.value = repository.getTotalMileage(carId) ?: 0.0
            _lastTrip.value = repository.getLastTrip(carId)
        }
    }

    fun updateTrip(existing: Trip, date: Long, startOdo: Double, endOdo: Double, purpose: TripPurpose, notes: String) {
        viewModelScope.launch {
            repository.updateTrip(
                existing.copy(
                    date = date, startOdometer = startOdo, endOdometer = endOdo,
                    distance = endOdo - startOdo, purpose = purpose, notes = notes
                )
            )
            _totalMileage.value = repository.getTotalMileage(existing.carId) ?: 0.0
        }
    }

    fun deleteTrip(trip: Trip) = viewModelScope.launch {
        repository.deleteTrip(trip)
        _carId.value?.let { _totalMileage.value = repository.getTotalMileage(it) ?: 0.0 }
    }
}

class TripsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return TripsViewModel(application) as T
    }
}
