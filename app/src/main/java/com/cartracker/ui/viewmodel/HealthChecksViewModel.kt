package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.HealthCheck
import com.cartracker.data.db.entities.HealthCheckType
import com.cartracker.data.db.entities.MaintenanceCategory
import com.cartracker.data.db.entities.MaintenanceLog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class CheckStatus { NEVER_DONE, OVERDUE, DUE_SOON, OK }

data class HealthCheckUi(
    val check: HealthCheck,
    val status: CheckStatus,
    val daysSinceLast: Long?,
    val daysUntilDue: Long,
    val kmSinceLast: Double?,
    val kmUntilDue: Double?,
    val progress: Float
)

private const val DAY_MS = 1000L * 60 * 60 * 24

class HealthChecksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val checks: LiveData<List<HealthCheckUi>> = _carId.switchMap { carId ->
        combine(
            repository.getCarFlow(carId),
            repository.getHealthChecksForCar(carId)
        ) { car, list ->
            val odo = car?.currentOdometer ?: 0.0
            list.map { it.toUi(odo) }.sortedWith(
                compareBy(
                    { statusSortOrder(it.status) },
                    { it.daysUntilDue }
                )
            )
        }.asLiveData()
    }

    fun setCarId(carId: Long) {
        _carId.value = carId
        viewModelScope.launch { ensureAllChecksExist(carId) }
    }

    fun markDone(carId: Long, type: HealthCheckType, notes: String? = null) {
        viewModelScope.launch {
            val odometer = repository.getCarById(carId)?.currentOdometer ?: 0.0
            repository.markHealthCheckDone(carId, type, System.currentTimeMillis(), odometer, notes)
        }
    }

    fun logOilChange(carId: Long, brand: String, spec: String, liters: Double?, cost: Double?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val odometer = repository.getCarById(carId)?.currentOdometer ?: 0.0

            val serviceType = listOfNotNull(
                brand.takeIf { it.isNotBlank() },
                spec.takeIf { it.isNotBlank() }
            ).joinToString(" ").ifBlank { "Oil Change" }

            val noteParts = mutableListOf<String>()
            if (liters != null) noteParts += "%.1f L".format(liters)
            val notes = noteParts.joinToString(" · ").ifBlank { null }

            // Mark the health check done
            repository.markHealthCheckDone(carId, HealthCheckType.ENGINE_OIL, now, odometer,
                listOfNotNull(
                    brand.takeIf { it.isNotBlank() },
                    spec.takeIf { it.isNotBlank() },
                    liters?.let { "%.1f L".format(it) },
                    cost?.let { "BD %.3f".format(it) }
                ).joinToString(" · ").ifBlank { null }
            )

            // Also create a Maintenance Log entry
            repository.insertMaintenanceLog(
                MaintenanceLog(
                    carId = carId,
                    category = MaintenanceCategory.OIL_CHANGE,
                    serviceType = serviceType,
                    date = now,
                    mileage = odometer,
                    cost = cost ?: 0.0,
                    notes = notes ?: ""
                )
            )
        }
    }

    private suspend fun ensureAllChecksExist(carId: Long) {
        HealthCheckType.entries.forEach { type ->
            val existing = repository.getHealthCheck(carId, type)
            if (existing == null) {
                repository.insertHealthCheck(
                    HealthCheck(carId = carId, checkType = type, intervalDays = type.defaultIntervalDays)
                )
            }
        }
    }

    private fun HealthCheck.toUi(currentOdometer: Double): HealthCheckUi {
        val now = System.currentTimeMillis()

        // Time-based
        val daysSince = lastCheckedAt?.let { (now - it) / DAY_MS }
        val daysUntilDue = if (daysSince != null) intervalDays - daysSince else -intervalDays.toLong()
        val timeProgress = if (daysSince != null) (daysSince.toFloat() / intervalDays).coerceIn(0f, 1f) else 1f

        // Km-based (only when intervalKm is configured)
        val kmSinceLast = if (intervalKm != null && lastCheckedAtOdometer != null)
            currentOdometer - lastCheckedAtOdometer else null
        val kmUntilDue = if (intervalKm != null && kmSinceLast != null)
            intervalKm - kmSinceLast else null
        val kmProgress = if (intervalKm != null && kmSinceLast != null)
            (kmSinceLast.toFloat() / intervalKm).coerceIn(0f, 1f) else null

        val progress = maxOf(timeProgress, kmProgress ?: 0f)

        val timeStatus = when {
            lastCheckedAt == null -> CheckStatus.NEVER_DONE
            daysUntilDue < 0     -> CheckStatus.OVERDUE
            daysUntilDue <= 7    -> CheckStatus.DUE_SOON
            else                 -> CheckStatus.OK
        }
        val kmStatus = when {
            kmUntilDue == null -> null
            kmUntilDue < 0     -> CheckStatus.OVERDUE
            kmUntilDue <= 500  -> CheckStatus.DUE_SOON
            else               -> CheckStatus.OK
        }

        val status = listOfNotNull(timeStatus, kmStatus).minByOrNull { statusSortOrder(it) } ?: timeStatus

        return HealthCheckUi(this, status, daysSince, daysUntilDue, kmSinceLast, kmUntilDue, progress)
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
