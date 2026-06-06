package com.cartracker.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.cartracker.data.db.entities.Expense
import com.cartracker.data.db.entities.FuelLog
import com.cartracker.data.db.entities.MaintenanceLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ExportUtil {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun buildFuelCsv(logs: List<FuelLog>, currency: String): String = buildString {
        appendLine("Date,Odometer (km),Liters,Price/L ($currency),Total ($currency),km/L,Full Tank,Fuel Type,Notes")
        logs.sortedByDescending { it.date }.forEach { log ->
            val eff = if (log.fuelEfficiency > 0) "%.2f".format(log.fuelEfficiency) else ""
            appendLine("${sdf.format(Date(log.date))},${log.odometer.toInt()},%.2f,%.3f,%.3f,$eff,${if (log.isFullTank) "Yes" else "No"},${log.fuelType.displayName},\"${log.notes.replace("\"", "\"\"")}\"".format(log.liters, log.costPerLiter, log.totalCost))
        }
    }

    fun buildMaintenanceCsv(logs: List<MaintenanceLog>, currency: String): String = buildString {
        appendLine("Date,Category,Service,Mileage (km),Cost ($currency),Garage,Next Service (km),Warranty Expiry,Notes")
        logs.sortedByDescending { it.date }.forEach { log ->
            val warranty = log.warrantyExpiryDate?.let { sdf.format(Date(it)) } ?: ""
            val nextKm = log.nextServiceKm?.toInt()?.toString() ?: ""
            appendLine("${sdf.format(Date(log.date))},${log.category.displayName},\"${log.serviceType}\",${log.mileage.toInt()},%.3f,\"${log.garage}\",${nextKm},${warranty},\"${log.notes.replace("\"", "\"\"")}\"".format(log.cost))
        }
    }

    fun buildExpenseCsv(expenses: List<Expense>, currency: String): String = buildString {
        appendLine("Date,Category,Description,Amount ($currency),Recurring,Recurrence (days),Notes")
        expenses.sortedByDescending { it.date }.forEach { exp ->
            appendLine("${sdf.format(Date(exp.date))},${exp.category.displayName},\"${exp.description}\",%.3f,${if (exp.isRecurring) "Yes" else "No"},${exp.recurrenceDays ?: ""},\"${exp.notes.replace("\"", "\"\"")}\"".format(exp.amount))
        }
    }

    fun shareText(context: Context, filename: String, content: String, mimeType: String = "text/csv") {
        val file = File(context.externalCacheDir, filename)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export $filename"))
    }
}
