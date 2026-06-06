package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

enum class CheckStatus { NEVER_DONE, OVERDUE, DUE_SOON, OK }

data class HealthCheckUi(
    val check: HealthCheck,
    val status: CheckStatus,
    val daysSinceLast: Long?,
    val daysUntilDue: Long,
    val kmSinceLast: Double?,
    val kmUntilDue: Double?,
    val nextServiceKm: Double?,      // absolute km target for next service
    val progress: Float,
    val lastServiceNotes: String?,   // from maintenance log (backed types) or check.notes
    val isMaintenanceBacked: Boolean
)

private const val DAY_MS = 1000L * 60 * 60 * 24

class HealthChecksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val checks: LiveData<List<HealthCheckUi>> = _carId.switchMap { carId ->
        combine(
            repository.getCarFlow(carId),
            repository.getHealthChecksForCar(carId),
            repository.getMaintenanceLogsForCar(carId)
        ) { car, checks, maintenanceLogs ->
            val odo = car?.currentOdometer ?: 0.0
            val latestByCategory = maintenanceLogs
                .groupBy { it.category }
                .mapValues { (_, logs) -> logs.maxByOrNull { it.date } }
            checks.map { it.toUi(odo, latestByCategory) }.sortedWith(
                compareBy({ statusSortOrder(it.status) }, { it.daysUntilDue })
            )
        }.asLiveData()
    }

    fun setCarId(carId: Long) {
        _carId.value = carId
        viewModelScope.launch { ensureAllChecksExist(carId) }
    }

    // For types not backed by maintenance logs (TYRE_PRESSURE, COOLANT, WASHER_FLUID, LIGHTS)
    fun markDone(carId: Long, type: HealthCheckType) {
        viewModelScope.launch {
            val odometer = repository.getCarById(carId)?.currentOdometer ?: 0.0
            repository.markHealthCheckDone(carId, type, System.currentTimeMillis(), odometer, null)
        }
    }

    // For ENGINE_OIL — specialized with brand, spec, liters
    fun logOilChange(carId: Long, brand: String, spec: String, liters: Double?, cost: Double?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val odometer = repository.getCarById(carId)?.currentOdometer ?: 0.0
            val serviceType = listOfNotNull(
                brand.takeIf { it.isNotBlank() },
                spec.takeIf { it.isNotBlank() }
            ).joinToString(" ").ifBlank { "Oil Change" }
            val notes = liters?.let { String.format(Locale.US, "%.1f L", it) } ?: ""
            repository.insertMaintenanceLog(
                MaintenanceLog(
                    carId = carId, category = MaintenanceCategory.OIL_CHANGE,
                    serviceType = serviceType, date = now, mileage = odometer,
                    cost = cost ?: 0.0, notes = notes
                )
            )
        }
    }

    // For other maintenance-backed types (BRAKE_FLUID, BATTERY, AIR_FILTER, WIPER_BLADES)
    fun logServiceEntry(carId: Long, type: HealthCheckType, brand: String, cost: Double?, notes: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val odometer = repository.getCarById(carId)?.currentOdometer ?: 0.0
            val category = type.maintenanceCategory ?: return@launch
            val serviceType = brand.takeIf { it.isNotBlank() } ?: type.displayName
            repository.insertMaintenanceLog(
                MaintenanceLog(
                    carId = carId, category = category,
                    serviceType = serviceType, date = now, mileage = odometer,
                    cost = cost ?: 0.0, notes = notes
                )
            )
        }
    }

    fun updateInterval(check: HealthCheck, intervalDays: Int, intervalKm: Int?) {
        viewModelScope.launch {
            repository.updateHealthCheck(check.copy(intervalDays = intervalDays, intervalKm = intervalKm))
        }
    }

    private suspend fun ensureAllChecksExist(carId: Long) {
        HealthCheckType.entries.forEach { type ->
            if (repository.getHealthCheck(carId, type) == null) {
                repository.insertHealthCheck(
                    HealthCheck(carId = carId, checkType = type, intervalDays = type.defaultIntervalDays)
                )
            }
        }
    }

    private fun HealthCheck.toUi(
        currentOdometer: Double,
        latestByCategory: Map<MaintenanceCategory, MaintenanceLog?>
    ): HealthCheckUi {
        val now = System.currentTimeMillis()
        val isBacked = checkType.maintenanceCategory != null
        val backingLog = checkType.maintenanceCategory?.let { latestByCategory[it] }

        // Effective last-checked timestamps (prefer maintenance log for backed types)
        val effectiveLastCheckedAt = backingLog?.date ?: lastCheckedAt
        val effectiveLastOdometer  = backingLog?.mileage ?: lastCheckedAtOdometer

        // Time-based
        val daysSince = effectiveLastCheckedAt?.let { (now - it) / DAY_MS }
        val daysUntilDue = if (daysSince != null) intervalDays - daysSince else -intervalDays.toLong()
        val timeProgress = if (daysSince != null) (daysSince.toFloat() / intervalDays).coerceIn(0f, 1f) else 1f

        // Km-based
        val kmSinceLast = if (intervalKm != null && effectiveLastOdometer != null)
            currentOdometer - effectiveLastOdometer else null
        val kmUntilDue = if (intervalKm != null && kmSinceLast != null)
            intervalKm - kmSinceLast else null
        val kmProgress = if (intervalKm != null && kmSinceLast != null)
            (kmSinceLast.toFloat() / intervalKm).coerceIn(0f, 1f) else null
        val nextServiceKm = if (intervalKm != null && effectiveLastOdometer != null)
            effectiveLastOdometer + intervalKm else null

        val progress = maxOf(timeProgress, kmProgress ?: 0f)

        val timeStatus = when {
            effectiveLastCheckedAt == null -> CheckStatus.NEVER_DONE
            daysUntilDue < 0              -> CheckStatus.OVERDUE
            daysUntilDue <= 7             -> CheckStatus.DUE_SOON
            else                          -> CheckStatus.OK
        }
        val kmStatus = when {
            kmUntilDue == null -> null
            kmUntilDue < 0     -> CheckStatus.OVERDUE
            kmUntilDue <= 500  -> CheckStatus.DUE_SOON
            else               -> CheckStatus.OK
        }
        val status = listOfNotNull(timeStatus, kmStatus).minByOrNull { statusSortOrder(it) } ?: timeStatus

        // Notes: from maintenance log for backed types, from health_check.notes otherwise
        val lastServiceNotes = if (backingLog != null) {
            listOfNotNull(
                backingLog.serviceType.takeIf { it.isNotBlank() && it != checkType.displayName },
                backingLog.notes.takeIf { it.isNotBlank() },
                if (backingLog.cost > 0) String.format(Locale.US, "BD %.3f", backingLog.cost) else null
            ).joinToString(" · ").ifBlank { null }
        } else notes

        return HealthCheckUi(
            this, status, daysSince, daysUntilDue,
            kmSinceLast, kmUntilDue, nextServiceKm,
            progress, lastServiceNotes, isBacked
        )
    }

    private fun statusSortOrder(s: CheckStatus) = when (s) {
        CheckStatus.NEVER_DONE -> 0
        CheckStatus.OVERDUE    -> 1
        CheckStatus.DUE_SOON   -> 2
        CheckStatus.OK         -> 3
    }
}

class HealthChecksViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HealthChecksViewModel(application) as T
    }
}
