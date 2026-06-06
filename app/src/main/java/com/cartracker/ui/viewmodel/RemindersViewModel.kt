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
                    targetMileage: Double?, targetDate: Long?, notes: String,
                    recurrenceKm: Int? = null, recurrenceDays: Int? = null) {
        viewModelScope.launch {
            repository.insertReminder(
                Reminder(carId = carId, title = title, type = type,
                    targetMileage = targetMileage, targetDate = targetDate,
                    notes = notes, recurrenceKm = recurrenceKm, recurrenceDays = recurrenceDays)
            )
        }
    }

    fun updateReminder(existing: Reminder, title: String, type: ReminderType,
                       targetMileage: Double?, targetDate: Long?, notes: String,
                       recurrenceKm: Int? = null, recurrenceDays: Int? = null) {
        viewModelScope.launch {
            repository.updateReminder(
                existing.copy(title = title, type = type,
                    targetMileage = targetMileage, targetDate = targetDate,
                    notes = notes, recurrenceKm = recurrenceKm, recurrenceDays = recurrenceDays)
            )
        }
    }

    fun markCompleted(reminder: Reminder) = viewModelScope.launch {
        repository.markReminderCompleted(reminder.id)
        // Auto-schedule next if recurring
        when {
            reminder.type == ReminderType.MILEAGE &&
            reminder.recurrenceKm != null &&
            reminder.targetMileage != null -> {
                repository.insertReminder(reminder.copy(
                    id = 0,
                    targetMileage = reminder.targetMileage + reminder.recurrenceKm,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis()
                ))
            }
            reminder.type == ReminderType.DATE &&
            reminder.recurrenceDays != null &&
            reminder.targetDate != null -> {
                repository.insertReminder(reminder.copy(
                    id = 0,
                    targetDate = reminder.targetDate + reminder.recurrenceDays * 24L * 60 * 60 * 1000,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis()
                ))
            }
        }
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
