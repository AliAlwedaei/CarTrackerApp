package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.Reminder
import com.cartracker.data.db.entities.ReminderType
import kotlinx.coroutines.launch

class RemindersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val reminders: LiveData<List<Reminder>> = _carId.switchMap { carId ->
        repository.getRemindersForCar(carId).asLiveData()
    }

    fun setCarId(carId: Long) { _carId.value = carId }

    fun addReminder(carId: Long, title: String, type: ReminderType,
                    targetMileage: Double?, targetDate: Long?, notes: String) {
        viewModelScope.launch {
            repository.insertReminder(
                Reminder(carId = carId, title = title, type = type,
                    targetMileage = targetMileage, targetDate = targetDate, notes = notes)
            )
        }
    }

    fun updateReminder(existing: Reminder, title: String, type: ReminderType,
                       targetMileage: Double?, targetDate: Long?, notes: String) {
        viewModelScope.launch {
            repository.updateReminder(
                existing.copy(title = title, type = type,
                    targetMileage = targetMileage, targetDate = targetDate, notes = notes)
            )
        }
    }

    fun markCompleted(reminderId: Long) = viewModelScope.launch {
        repository.markReminderCompleted(reminderId)
    }

    fun deleteReminder(reminder: Reminder) = viewModelScope.launch {
        repository.deleteReminder(reminder)
    }
}

class RemindersViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RemindersViewModel(application) as T
    }
}
