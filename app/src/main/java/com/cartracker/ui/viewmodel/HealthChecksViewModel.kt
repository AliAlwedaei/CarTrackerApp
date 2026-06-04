package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.HealthCheck
import com.cartracker.data.db.entities.HealthCheckType
import kotlinx.coroutines.launch

enum class CheckStatus { NEVER_DONE, OVERDUE, DUE_SOON, OK }

data class HealthCheckUi(
    val check: HealthCheck,
    val status: CheckStatus,
    val daysSinceLast: Long?,  // null = never done
    val daysUntilDue: Long,    // negative = overdue
    val progress: Float        // 0 = just done, 1.0 = due now, >1 capped at 1 for display
)

private const val DAY_MS = 1000L * 60 * 60 * 24

class HealthChecksViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _carId = MutableLiveData<Long>()

    val checks: LiveData<List<HealthCheckUi>> = _carId.switchMap { carId ->
        repository.getHealthChecksForCar(carId).asLiveData().map { list ->
            list.map { it.toUi() }.sortedWith(
                compareBy(
                    { statusSortOrder(it.status) },
                    { it.daysUntilDue }
                )
            )
        }
    }

    fun setCarId(carId: Long) {
        _carId.value = carId
        viewModelScope.launch { ensureAllChecksExist(carId) }
    }

    fun markDone(carId: Long, type: HealthCheckType) {
        viewModelScope.launch {
            repository.markHealthCheckDone(carId, type, System.currentTimeMillis())
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

    private fun HealthCheck.toUi(): HealthCheckUi {
        val now = System.currentTimeMillis()
        val daysSince = lastCheckedAt?.let { (now - it) / DAY_MS }
        val daysUntilDue = if (daysSince != null) intervalDays - daysSince else -intervalDays.toLong()
        val progress = if (daysSince != null) (daysSince.toFloat() / intervalDays).coerceIn(0f, 1f) else 1f
        val status = when {
            lastCheckedAt == null -> CheckStatus.NEVER_DONE
            daysUntilDue < 0     -> CheckStatus.OVERDUE
            daysUntilDue <= 7    -> CheckStatus.DUE_SOON
            else                 -> CheckStatus.OK
        }
        return HealthCheckUi(this, status, daysSince, daysUntilDue, progress)
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
