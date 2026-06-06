package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.Car
import kotlinx.coroutines.launch

class CarsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    val cars: LiveData<List<Car>> = repository.allCars.asLiveData()

    private val _selectedCarId = MutableLiveData<Long?>(null)
    val selectedCarId: LiveData<Long?> = _selectedCarId

    fun selectCar(carId: Long) { _selectedCarId.value = carId }

    fun insertCar(car: Car) = viewModelScope.launch { repository.insertCar(car) }
    fun updateCar(car: Car) = viewModelScope.launch { repository.updateCar(car) }
    fun deleteCar(car: Car) = viewModelScope.launch { repository.deleteCar(car) }
    fun updateCarPhoto(carId: Long, photoUri: String) = viewModelScope.launch { repository.updateCarPhoto(carId, photoUri) }
}

class CarsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CarsViewModel(application) as T
    }
}
