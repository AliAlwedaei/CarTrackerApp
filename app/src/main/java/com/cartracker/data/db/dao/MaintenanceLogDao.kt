package com.cartracker.data.db.dao

import androidx.room.*
import com.cartracker.data.db.entities.MaintenanceCategory
import com.cartracker.data.db.entities.MaintenanceLog
import kotlinx.coroutines.flow.Flow

data class MaintenanceCategoryTotal(val category: MaintenanceCategory, val total: Double)

@Dao
interface MaintenanceLogDao {
    @Query("SELECT * FROM maintenance_logs WHERE carId = :carId ORDER BY date DESC")
    fun getMaintenanceLogsForCar(carId: Long): Flow<List<MaintenanceLog>>

    @Query("SELECT * FROM maintenance_logs WHERE carId = :carId ORDER BY date DESC LIMIT 1")
    suspend fun getLastServiceForCar(carId: Long): MaintenanceLog?

    @Query("SELECT SUM(cost) FROM maintenance_logs WHERE carId = :carId AND date >= :fromDate")
    suspend fun getTotalFrom(carId: Long, fromDate: Long): Double?

    @Query("SELECT SUM(cost) FROM maintenance_logs WHERE carId = :carId")
    suspend fun getTotalAllTime(carId: Long): Double?

    @Query("SELECT category, SUM(cost) as total FROM maintenance_logs WHERE carId = :carId GROUP BY category ORDER BY total DESC")
    suspend fun getTotalByCategory(carId: Long): List<MaintenanceCategoryTotal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceLog(log: MaintenanceLog): Long

    @Update
    suspend fun updateMaintenanceLog(log: MaintenanceLog)

    @Delete
    suspend fun deleteMaintenanceLog(log: MaintenanceLog)
}
