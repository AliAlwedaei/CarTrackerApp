package com.cartracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cartracker.CarTrackerApp
import com.cartracker.data.db.entities.FuelLog
import com.cartracker.data.db.entities.MaintenanceLog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class MonthlySpendStat(
    val label: String,
    val fuelCost: Double,
    val maintCost: Double,
    val expenseCost: Double
) {
    val totalCost get() = fuelCost + maintCost + expenseCost
}

// Keep alias so existing callers still compile
typealias MonthlyFuelStat = MonthlySpendStat

data class NextDueItem(
    val title: String,
    val subtitle: String,
    val isOverdue: Boolean,
    val isDueSoon: Boolean
)

data class DashboardStats(
    val totalMileage: Double = 0.0,
    val lastServiceDate: Long? = null,
    val avgFuelEfficiency: Double = 0.0,
    val monthlyFuelCost: Double = 0.0,
    val monthlyMaintCost: Double = 0.0,
    val monthlyExpenseCost: Double = 0.0,
    val recentFuelLogs: List<FuelLog> = emptyList(),
    val avgKmPerTank: Double = 0.0,
    val avgDaysBetweenFillups: Double = 0.0,
    val costPerKm: Double = 0.0,
    val monthlyStats: List<MonthlySpendStat> = emptyList(),
    val fuelPercent: Float = 0.72f,
    val fuelPriceHistory: List<Float> = emptyList(),
    val ytdTotalCost: Double = 0.0,
    val overdueChecksCount: Int = 0,
    val dueSoonChecksCount: Int = 0,
    val projectedAnnualTotalCost: Double = 0.0,
    val nextDueItems: List<NextDueItem> = emptyList()
) {
    val monthlyTotalCost get() = monthlyFuelCost + monthlyMaintCost + monthlyExpenseCost
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CarTrackerApp).repository

    private val _stats = MutableLiveData(DashboardStats())
    val stats: LiveData<DashboardStats> = _stats

    fun loadStats(carId: Long) {
        viewModelScope.launch {
            val totalMileage = repository.getTotalMileage(carId) ?: 0.0
            val lastService: MaintenanceLog? = repository.getLastService(carId)
            val avgEfficiency = repository.getAverageFuelEfficiency(carId) ?: 0.0

            val now = System.currentTimeMillis()

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val yearStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val monthlyCost = repository.getMonthlyCost(carId, monthStart) ?: 0.0
            val monthlyMaintCost = repository.getMaintenanceTotalFrom(carId, monthStart) ?: 0.0
            val monthlyExpenseCost = repository.getExpenseTotalFrom(carId, monthStart) ?: 0.0

            val ytdFuel = repository.getMonthlyCost(carId, yearStart) ?: 0.0
            val ytdMaint = repository.getMaintenanceTotalFrom(carId, yearStart) ?: 0.0
            val ytdExpense = repository.getExpenseTotalFrom(carId, yearStart) ?: 0.0
            val ytdTotalCost = ytdFuel + ytdMaint + ytdExpense

            val recentFrom = monthStart - 6L * 30 * 24 * 60 * 60 * 1000
            val recentLogs = repository.getFuelLogsFrom(carId, recentFrom)
            val allLogs = repository.getAllFuelLogsSorted(carId)

            val odometerDiffs = allLogs.zipWithNext { a, b -> b.odometer - a.odometer }.filter { it > 0 }
            val avgKmPerTank = if (odometerDiffs.isNotEmpty()) odometerDiffs.average() else 0.0

            val dateDiffs = allLogs.sortedBy { it.date }
                .zipWithNext { a, b -> (b.date - a.date) / (1000.0 * 60 * 60 * 24) }.filter { it > 0 }
            val avgDaysBetweenFillups = if (dateDiffs.isNotEmpty()) dateDiffs.average() else 0.0

            val totalFuelCost = allLogs.sumOf { it.totalCost }
            val totalKmFromFuel = allLogs.sumOf { it.fuelEfficiency * it.liters }
            val costPerKm = if (totalKmFromFuel > 0) totalFuelCost / totalKmFromFuel else 0.0

            // Monthly stats — last 6 months with stacked fuel + maintenance + expense
            val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
            val accurateMonthlyStats = buildList {
                for (offset in 5 downTo 0) {
                    val startCal = Calendar.getInstance().apply {
                        add(Calendar.MONTH, -offset)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val endCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    val label = monthFmt.format(startCal.time)
                    val fuelInMonth = allLogs.filter {
                        it.date >= startCal.timeInMillis && it.date < endCal.timeInMillis
                    }.sumOf { it.totalCost }
                    // For maintenance and expenses we use the dao directly for each month range
                    // getTotalFrom = SUM WHERE date >= fromDate (running total to now).
                    // To isolate a month: subtract the next month's running total from this month's.
                    val maintFrom = repository.getMaintenanceTotalFrom(carId, startCal.timeInMillis) ?: 0.0
                    val maintEnd  = repository.getMaintenanceTotalFrom(carId, endCal.timeInMillis)   ?: 0.0
                    val maintInMonth = (maintFrom - maintEnd).coerceAtLeast(0.0)
                    val expenseFrom = repository.getExpenseTotalFrom(carId, startCal.timeInMillis) ?: 0.0
                    val expenseEnd  = repository.getExpenseTotalFrom(carId, endCal.timeInMillis)   ?: 0.0
                    val expenseInMonth = (expenseFrom - expenseEnd).coerceAtLeast(0.0)
                    add(MonthlySpendStat(label = label, fuelCost = fuelInMonth, maintCost = maintInMonth, expenseCost = expenseInMonth))
                }
            }

            val car = repository.getCarById(carId)
            val lastFuelOdo = allLogs.lastOrNull()?.odometer
            val fuelPercent = if (car != null && lastFuelOdo != null && avgKmPerTank > 0) {
                val kmSince = (car.currentOdometer - lastFuelOdo).coerceAtLeast(0.0)
                (1.0 - kmSince / avgKmPerTank).toFloat().coerceIn(0f, 1f)
            } else 0.72f

            val fuelPriceHistory = allLogs.takeLast(8)
                .map { it.costPerLiter.toFloat() }.filter { it > 0f }

            // Health check action-required counts
            val healthChecks = repository.getHealthChecksOnce(carId)
            val odo = car?.currentOdometer ?: 0.0
            var overdueCount = 0
            var dueSoonCount = 0
            healthChecks.forEach { check ->
                val daysSince = check.lastCheckedAt?.let { (now - it) / (1000L * 60 * 60 * 24) }
                val daysLeft = if (daysSince != null) check.intervalDays - daysSince else -check.intervalDays.toLong()
                val kmSince = if (check.intervalKm != null && check.lastCheckedAtOdometer != null)
                    odo - check.lastCheckedAtOdometer else null
                val kmLeft = if (check.intervalKm != null && kmSince != null)
                    check.intervalKm - kmSince else null

                val isOverdue = daysSince == null || daysLeft < 0 || (kmLeft != null && kmLeft < 0)
                val isDueSoon = !isOverdue && (daysLeft <= 7 || (kmLeft != null && kmLeft <= 500))
                when {
                    isOverdue -> overdueCount++
                    isDueSoon -> dueSoonCount++
                }
            }

            // Annual projection: extrapolate YTD spend over remaining months
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR).toDouble()
            val projectedAnnualTotalCost = if (dayOfYear > 0 && ytdTotalCost > 0)
                (ytdTotalCost / dayOfYear) * 365.0 else 0.0

            // Next-due reminders: rank active reminders by urgency for the dashboard card
            val currentOdo = car?.currentOdometer ?: 0.0
            val activeReminders = repository.getActiveRemindersForCar(carId)
            data class ReminderScore(val item: NextDueItem, val urgencyScore: Double)
            val scoredReminders = activeReminders.mapNotNull { r ->
                when (r.type) {
                    com.cartracker.data.db.entities.ReminderType.MILEAGE -> {
                        val km = r.targetMileage ?: return@mapNotNull null
                        val kmLeft = km - currentOdo
                        val isOverdue = kmLeft < 0
                        val isDueSoon = !isOverdue && kmLeft <= 500
                        val sub = if (isOverdue) "%.0f km overdue".format(-kmLeft)
                                  else "in %.0f km (at %,.0f km)".format(kmLeft, km)
                        ReminderScore(
                            NextDueItem(r.title, sub, isOverdue, isDueSoon),
                            if (isOverdue) kmLeft else kmLeft + 100_000
                        )
                    }
                    com.cartracker.data.db.entities.ReminderType.DATE -> {
                        val target = r.targetDate ?: return@mapNotNull null
                        val daysLeft = (target - now) / (1000L * 60 * 60 * 24)
                        val isOverdue = daysLeft < 0
                        val isDueSoon = !isOverdue && daysLeft <= 7
                        val sub = if (isOverdue) "${-daysLeft}d overdue"
                                  else if (daysLeft == 0L) "Due today"
                                  else "in ${daysLeft}d"
                        ReminderScore(
                            NextDueItem(r.title, sub, isOverdue, isDueSoon),
                            if (isOverdue) daysLeft.toDouble() else daysLeft.toDouble() + 100_000
                        )
                    }
                }
            }.sortedBy { it.urgencyScore }.take(3).map { it.item }

            _stats.value = DashboardStats(
                totalMileage = totalMileage,
                lastServiceDate = lastService?.date,
                avgFuelEfficiency = avgEfficiency,
                monthlyFuelCost = monthlyCost,
                monthlyMaintCost = monthlyMaintCost,
                monthlyExpenseCost = monthlyExpenseCost,
                recentFuelLogs = recentLogs,
                avgKmPerTank = avgKmPerTank,
                avgDaysBetweenFillups = avgDaysBetweenFillups,
                costPerKm = costPerKm,
                monthlyStats = accurateMonthlyStats,
                fuelPercent = fuelPercent,
                fuelPriceHistory = fuelPriceHistory,
                ytdTotalCost = ytdTotalCost,
                overdueChecksCount = overdueCount,
                dueSoonChecksCount = dueSoonCount,
                projectedAnnualTotalCost = projectedAnnualTotalCost,
                nextDueItems = scoredReminders
            )
        }
    }
}

class DashboardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(application) as T
    }
}
