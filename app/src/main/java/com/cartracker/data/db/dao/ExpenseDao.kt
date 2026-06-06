package com.cartracker.data.db.dao

import androidx.room.*
import com.cartracker.data.db.entities.Expense
import com.cartracker.data.db.entities.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE carId = :carId ORDER BY date DESC")
    fun getExpensesForCar(carId: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE carId = :carId AND date >= :fromDate")
    suspend fun getTotalFrom(carId: Long, fromDate: Long): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE carId = :carId")
    suspend fun getTotalAllTime(carId: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE carId = :carId GROUP BY category")
    suspend fun getTotalByCategory(carId: Long): List<CategoryTotal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}

data class CategoryTotal(val category: ExpenseCategory, val total: Double)
