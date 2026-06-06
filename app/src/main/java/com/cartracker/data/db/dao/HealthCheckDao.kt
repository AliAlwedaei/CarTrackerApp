package com.cartracker.data.db.dao

import androidx.room.*
import com.cartracker.data.db.entities.HealthCheck
import com.cartracker.data.db.entities.HealthCheckType
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthCheckDao {
    @Query("SELECT * FROM health_checks WHERE carId = :carId ORDER BY checkType")
    fun getHealthChecksForCar(carId: Long): Flow<List<HealthCheck>>

    @Query("SELECT * FROM health_checks WHERE carId = :carId")
    suspend fun getHealthChecksOnce(carId: Long): List<HealthCheck>

    @Query("SELECT * FROM health_checks WHERE carId = :carId AND checkType = :type LIMIT 1")
    suspend fun getHealthCheck(carId: Long, type: HealthCheckType): HealthCheck?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHealthCheck(check: HealthCheck): Long

    @Update
    suspend fun updateHealthCheck(check: HealthCheck)

    @Query("UPDATE health_checks SET lastCheckedAt = :timestamp, lastCheckedAtOdometer = :odometer, notes = :notes WHERE carId = :carId AND checkType = :type")
    suspend fun markDone(carId: Long, type: HealthCheckType, timestamp: Long, odometer: Double, notes: String?)

    @Query("UPDATE health_checks SET intervalKm = :intervalKm, lastCheckedAtOdometer = :odometer WHERE carId = :carId AND checkType = :type")
    suspend fun setKmData(carId: Long, type: HealthCheckType, intervalKm: Int, odometer: Double)
}
