package com.cartracker.data.db.dao

import androidx.room.*
import com.cartracker.data.db.entities.FuelLog
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_logs WHERE carId = :carId ORDER BY date DESC")
    fun getFuelLogsForCar(carId: Long): Flow<List<FuelLog>>

    @Query("SELECT * FROM fuel_logs WHERE carId = :carId ORDER BY odometer DESC LIMIT 1")
    suspend fun getLatestFuelLog(carId: Long): FuelLog?

    @Query("SELECT * FROM fuel_logs WHERE carId = :carId AND odometer < :odo ORDER BY odometer DESC LIMIT 1")
    suspend fun getPrevFuelLog(carId: Long, odo: Double): FuelLog?

    @Query("SELECT * FROM fuel_logs WHERE carId = :carId AND date >= :fromDate ORDER BY date ASC")
    suspend fun getFuelLogsFrom(carId: Long, fromDate: Long): List<FuelLog>

    @Query("SELECT * FROM fuel_logs WHERE carId = :carId ORDER BY odometer ASC")
    suspend fun getAllFuelLogsSorted(carId: Long): List<FuelLog>

    @Query("SELECT SUM(totalCost) FROM fuel_logs WHERE carId = :carId AND date >= :fromDate")
    suspend fun getMonthlyCost(carId: Long, fromDate: Long): Double?

    @Query("SELECT AVG(fuelEfficiency) FROM fuel_logs WHERE carId = :carId AND fuelEfficiency > 0 AND isFullTank = 1")
    suspend fun getAverageFuelEfficiency(carId: Long): Double?

    @Query("SELECT SUM(totalCost) FROM fuel_logs WHERE carId = :carId")
    suspend fun getTotalCostAllTime(carId: Long): Double?

    @Query("SELECT costPerLiter FROM fuel_logs WHERE carId = :carId ORDER BY date DESC LIMIT 1")
    suspend fun getLastPricePerLiter(carId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelLog(fuelLog: FuelLog): Long

    @Update
    suspend fun updateFuelLog(fuelLog: FuelLog)

    @Delete
    suspend fun deleteFuelLog(fuelLog: FuelLog)
}
