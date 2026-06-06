package com.cartracker.util

import com.cartracker.data.db.entities.MaintenanceCategory

data class ServiceTemplate(
    val category: MaintenanceCategory,
    val defaultServiceType: String,
    val intervalKm: Int,
    val intervalLabel: String
)

object ServiceTemplates {
    val all = listOf(
        ServiceTemplate(MaintenanceCategory.OIL_CHANGE,   "Engine Oil Change",       5_000,  "Every 5,000 km"),
        ServiceTemplate(MaintenanceCategory.FILTERS,      "Air Filter Replacement",  15_000, "Every 15,000 km"),
        ServiceTemplate(MaintenanceCategory.TIRES,        "Tyre Rotation",           10_000, "Every 10,000 km"),
        ServiceTemplate(MaintenanceCategory.BRAKES,       "Brake Inspection",        30_000, "Every 30,000 km"),
        ServiceTemplate(MaintenanceCategory.BATTERY,      "Battery Check",           50_000, "Every 50,000 km"),
        ServiceTemplate(MaintenanceCategory.WIPERS,       "Wiper Blade Replacement", 20_000, "Every 20,000 km"),
        ServiceTemplate(MaintenanceCategory.SPARK_PLUGS,  "Spark Plug Replacement",  40_000, "Every 40,000 km"),
        ServiceTemplate(MaintenanceCategory.TRANSMISSION, "Transmission Fluid",      60_000, "Every 60,000 km"),
        ServiceTemplate(MaintenanceCategory.TIMING_BELT,  "Timing Belt/Chain",       80_000, "Every 80,000 km"),
        ServiceTemplate(MaintenanceCategory.AC_SERVICE,   "AC Service & Recharge",   30_000, "Every 30,000 km"),
        ServiceTemplate(MaintenanceCategory.ALIGNMENT,    "Wheel Alignment",         20_000, "Every 20,000 km"),
    )

    private val byCategory = all.associateBy { it.category }

    fun forCategory(category: MaintenanceCategory): ServiceTemplate? = byCategory[category]
}
