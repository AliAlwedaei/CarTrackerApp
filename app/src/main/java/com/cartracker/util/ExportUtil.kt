package com.cartracker.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.cartracker.data.db.entities.Car
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

    fun buildPdfReport(
        context: Context,
        car: Car?,
        fuelLogs: List<FuelLog>,
        maintLogs: List<MaintenanceLog>,
        expenses: List<Expense>,
        totalFuel: Double,
        totalMaint: Double,
        totalExpense: Double,
        currency: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 20f; color = Color.BLACK; isFakeBoldText = true }
        val headPaint  = Paint().apply { textSize = 13f; color = Color.BLACK; isFakeBoldText = true }
        val bodyPaint  = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val smallPaint = Paint().apply { textSize = 9f;  color = Color.GRAY }
        val linePaint  = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        var y = 60f
        val left = 48f; val right = 547f

        fun drawLine() { canvas.drawLine(left, y, right, y, linePaint); y += 10f }
        fun drawText(text: String, paint: Paint, indent: Float = left) { canvas.drawText(text, indent, y, paint); y += paint.textSize + 4f }
        fun spacer(h: Float = 8f) { y += h }

        // Header
        drawText("Vehicle Report", titlePaint)
        car?.let { drawText("${it.name}  ·  ${it.make} ${it.model} ${it.year}  ·  Plate: ${it.plateNumber.ifBlank { "–" }}", bodyPaint) }
        drawText("Generated: ${sdf.format(Date())}", smallPaint)
        spacer(); drawLine(); spacer()

        // Cost of ownership
        drawText("TOTAL COST OF OWNERSHIP", headPaint)
        spacer(4f)
        val total = totalFuel + totalMaint + totalExpense
        drawText("Total:              $currency ${"%.3f".format(total)}", bodyPaint)
        drawText("  Fuel:             $currency ${"%.3f".format(totalFuel)}   (${if (total > 0) (totalFuel/total*100).toInt() else 0}%)", bodyPaint)
        drawText("  Maintenance:      $currency ${"%.3f".format(totalMaint)}   (${if (total > 0) (totalMaint/total*100).toInt() else 0}%)", bodyPaint)
        drawText("  Other Expenses:   $currency ${"%.3f".format(totalExpense)}   (${if (total > 0) (totalExpense/total*100).toInt() else 0}%)", bodyPaint)
        spacer(); drawLine(); spacer()

        // Fuel summary
        drawText("FUEL LOG (last 10)", headPaint)
        spacer(4f)
        fuelLogs.sortedByDescending { it.date }.take(10).forEach { log ->
            drawText("${sdf.format(Date(log.date))}  ${log.odometer.toInt()} km  ${log.liters}L  ${if (log.fuelEfficiency > 0) "%.1f km/L".format(log.fuelEfficiency) else "–"}  $currency ${"%.3f".format(log.totalCost)}", smallPaint)
        }
        spacer(); drawLine(); spacer()

        // Maintenance summary
        drawText("MAINTENANCE LOG (last 10)", headPaint)
        spacer(4f)
        maintLogs.sortedByDescending { it.date }.take(10).forEach { log ->
            drawText("${sdf.format(Date(log.date))}  ${log.category.displayName}  ${log.serviceType}  $currency ${"%.3f".format(log.cost)}", smallPaint)
        }

        document.finishPage(page)
        val file = File(context.externalCacheDir, "vehicle_report.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Vehicle Report — ${car?.name ?: ""}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export PDF Report"))
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
