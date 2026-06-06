package com.cartracker.data.db.dao

import androidx.room.*
import com.cartracker.data.db.entities.CustomHealthCheck
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomHealthCheckDao {
    @Query("SELECT * FROM custom_health_checks WHERE carId = :carId ORDER BY createdAt ASC")
    fun getCustomChecksForCar(carId: Long): Flow<List<CustomHealthCheck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCheck(check: CustomHealthCheck): Long

    @Update
    suspend fun updateCustomCheck(check: CustomHealthCheck)

    @Delete
    suspend fun deleteCustomCheck(check: CustomHealthCheck)

    @Query("UPDATE custom_health_checks SET lastCheckedAt = :timestamp, lastCheckedAtOdometer = :odometer WHERE id = :id")
    suspend fun markDone(id: Long, timestamp: Long, odometer: Double)
}
