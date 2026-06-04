package com.cartracker.data.db.dao

import androidx.room.*
import com.cartracker.data.db.entities.MaintenanceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceLogDao {
    @Query("SELECT * FROM maintenance_logs WHERE carId = :carId ORDER BY date DESC")
    fun getMaintenanceLogsForCar(carId: Long): Flow<List<MaintenanceLog>>

    @Query("SELECT * FROM maintenance_logs WHERE carId = :carId ORDER BY date DESC LIMIT 1")
    suspend fun getLastServiceForCar(carId: Long): MaintenanceLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenanceLog(log: MaintenanceLog): Long

    @Update
    suspend fun updateMaintenanceLog(log: MaintenanceLog)

    @Delete
    suspend fun deleteMaintenanceLog(log: MaintenanceLog)
}
