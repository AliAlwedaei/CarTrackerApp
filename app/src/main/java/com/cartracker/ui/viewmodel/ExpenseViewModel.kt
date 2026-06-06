package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.Expense
import com.cartracker.data.db.entities.ExpenseCategory
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val expenses: LiveData<List<Expense>> = _carId.switchMap { carId ->
        repository.getExpensesForCar(carId).asLiveData()
    }

    fun setCarId(carId: Long) { _carId.value = carId }

    fun addExpense(
        carId: Long, category: ExpenseCategory, description: String,
        date: Long, amount: Double, isRecurring: Boolean, recurrenceDays: Int?, notes: String
    ) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    carId = carId, category = category, description = description,
                    date = date, amount = amount, isRecurring = isRecurring,
                    recurrenceDays = recurrenceDays, notes = notes
                )
            )
        }
    }

    fun updateExpense(
        existing: Expense, category: ExpenseCategory, description: String,
        date: Long, amount: Double, isRecurring: Boolean, recurrenceDays: Int?, notes: String
    ) {
        viewModelScope.launch {
            repository.updateExpense(
                existing.copy(
                    category = category, description = description, date = date,
                    amount = amount, isRecurring = isRecurring, recurrenceDays = recurrenceDays, notes = notes
                )
            )
        }
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch { repository.deleteExpense(expense) }
}

class ExpenseViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ExpenseViewModel(application) as T
    }
}
