package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.Reminder
import com.cartracker.data.db.entities.ReminderType
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ReminderSuggestion(
    val title: String,
    val targetMileage: Double,
    val notes: String
)

class RemindersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val reminders: LiveData<List<Reminder>> = _carId.switchMap { carId ->
        repository.getRemindersForCar(carId).asLiveData()
    }

    // Smart suggestions: maintenance logs with nextServiceKm that have no matching active reminder
    val suggestions: LiveData<List<ReminderSuggestion>> = _carId.switchMap { carId ->
        combine(
            repository.getMaintenanceLogsForCar(carId),
            repository.getRemindersForCar(carId)
        ) { maintLogs, reminders ->
            val activeMileageTargets = reminders
                .filter { !it.isCompleted && it.type == ReminderType.MILEAGE }
                .mapNotNull { it.targetMileage }

            maintLogs
                .filter { it.nextServiceKm != null }
                .groupBy { it.serviceType }
                .mapNotNull { (_, logs) -> logs.maxByOrNull { it.date } }
                .filter { log ->
                    val target = log.nextServiceKm ?: return@filter false
                    activeMileageTargets.none { existing -> kotlin.math.abs(existing - target) < 1000.0 }
                }
                .map { log ->
                    ReminderSuggestion(
                        title = "Next ${log.serviceType}",
                        targetMileage = log.nextServiceKm!!,
                        notes = "From ${log.category.displayName} service"
                    )
                }
        }.asLiveData()
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

    fun addSuggestion(carId: Long, suggestion: ReminderSuggestion) {
        viewModelScope.launch {
            repository.insertReminder(
                Reminder(
                    carId = carId,
                    title = suggestion.title,
                    type = ReminderType.MILEAGE,
                    targetMileage = suggestion.targetMileage,
                    notes = suggestion.notes
                )
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
