package com.cartracker.data.repository

import com.cartracker.data.db.dao.*
import com.cartracker.data.db.entities.*
import kotlinx.coroutines.flow.Flow

class CarTrackerRepository(
    private val carDao: CarDao,
    private val fuelLogDao: FuelLogDao,
    private val maintenanceLogDao: MaintenanceLogDao,
    private val tripDao: TripDao,
    private val reminderDao: ReminderDao,
    private val healthCheckDao: HealthCheckDao
) {
    // Cars
    val allCars: Flow<List<Car>> = carDao.getAllCars()
    suspend fun getCarById(id: Long) = carDao.getCarById(id)
    fun getCarFlow(id: Long) = carDao.getCarFlow(id)
    suspend fun getAllCarsOnce() = carDao.getAllCarsOnce()
    suspend fun insertCar(car: Car) = carDao.insertCar(car)
    suspend fun updateCar(car: Car) = carDao.updateCar(car)
    suspend fun deleteCar(car: Car) = carDao.deleteCar(car)
    suspend fun updateOdometer(carId: Long, odometer: Double) = carDao.updateOdometer(carId, odometer)

    // Fuel Logs
    fun getFuelLogsForCar(carId: Long): Flow<List<FuelLog>> = fuelLogDao.getFuelLogsForCar(carId)
    suspend fun getLatestFuelLog(carId: Long) = fuelLogDao.getLatestFuelLog(carId)
    suspend fun getPrevFuelLog(carId: Long, odo: Double) = fuelLogDao.getPrevFuelLog(carId, odo)
    suspend fun getAllFuelLogsSorted(carId: Long) = fuelLogDao.getAllFuelLogsSorted(carId)
    suspend fun insertFuelLog(log: FuelLog) = fuelLogDao.insertFuelLog(log)
    suspend fun updateFuelLog(log: FuelLog) = fuelLogDao.updateFuelLog(log)
    suspend fun deleteFuelLog(log: FuelLog) = fuelLogDao.deleteFuelLog(log)
    suspend fun getMonthlyCost(carId: Long, fromDate: Long) = fuelLogDao.getMonthlyCost(carId, fromDate)
    suspend fun getFuelLogsFrom(carId: Long, fromDate: Long) = fuelLogDao.getFuelLogsFrom(carId, fromDate)
    suspend fun getAverageFuelEfficiency(carId: Long) = fuelLogDao.getAverageFuelEfficiency(carId)

    // Maintenance Logs
    fun getMaintenanceLogsForCar(carId: Long): Flow<List<MaintenanceLog>> = maintenanceLogDao.getMaintenanceLogsForCar(carId)
    suspend fun getLastService(carId: Long) = maintenanceLogDao.getLastServiceForCar(carId)
    suspend fun insertMaintenanceLog(log: MaintenanceLog) = maintenanceLogDao.insertMaintenanceLog(log)
    suspend fun updateMaintenanceLog(log: MaintenanceLog) = maintenanceLogDao.updateMaintenanceLog(log)
    suspend fun deleteMaintenanceLog(log: MaintenanceLog) = maintenanceLogDao.deleteMaintenanceLog(log)

    // Trips
    fun getTripsForCar(carId: Long): Flow<List<Trip>> = tripDao.getTripsForCar(carId)
    suspend fun getTotalMileage(carId: Long) = tripDao.getTotalMileage(carId)
    suspend fun getLastTrip(carId: Long) = tripDao.getLastTripForCar(carId)
    suspend fun insertTrip(trip: Trip) = tripDao.insertTrip(trip)
    suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)
    suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)

    // Health Checks
    fun getHealthChecksForCar(carId: Long): Flow<List<HealthCheck>> = healthCheckDao.getHealthChecksForCar(carId)
    suspend fun getHealthCheck(carId: Long, type: HealthCheckType) = healthCheckDao.getHealthCheck(carId, type)
    suspend fun insertHealthCheck(check: HealthCheck) = healthCheckDao.insertHealthCheck(check)
    suspend fun markHealthCheckDone(carId: Long, type: HealthCheckType, timestamp: Long, odometer: Double, notes: String? = null) = healthCheckDao.markDone(carId, type, timestamp, odometer, notes)
    suspend fun setHealthCheckKmData(carId: Long, type: HealthCheckType, intervalKm: Int, odometer: Double) = healthCheckDao.setKmData(carId, type, intervalKm, odometer)

    // Reminders
    fun getRemindersForCar(carId: Long): Flow<List<Reminder>> = reminderDao.getRemindersForCar(carId)
    val allActiveReminders: Flow<List<Reminder>> = reminderDao.getAllActiveReminders()
    suspend fun insertReminder(reminder: Reminder) = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
    suspend fun markReminderCompleted(reminderId: Long) = reminderDao.markCompleted(reminderId)
}
