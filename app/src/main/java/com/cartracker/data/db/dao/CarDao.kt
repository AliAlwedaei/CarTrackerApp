package com.cartracker.data.db.dao

import androidx.room.*
import com.cartracker.data.db.entities.Car
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY createdAt DESC")
    fun getAllCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE id = :carId")
    suspend fun getCarById(carId: Long): Car?

    @Query("SELECT * FROM cars WHERE id = :carId")
    fun getCarFlow(carId: Long): Flow<Car?>

    @Query("SELECT * FROM cars")
    suspend fun getAllCarsOnce(): List<Car>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car): Long

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)

    @Query("UPDATE cars SET currentOdometer = :odometer WHERE id = :carId")
    suspend fun updateOdometer(carId: Long, odometer: Double)
}
