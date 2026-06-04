package com.cartracker.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.cartracker.data.db.entities.FuelLog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FuelSpendChart(fuelLogs: List<FuelLog>, modifier: Modifier = Modifier) {
    val darkTheme = isSystemInDarkTheme()
    val textColor = if (darkTheme) AndroidColor.WHITE else AndroidColor.DKGRAY

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setDrawGridBackground(false)
                legend.textColor = textColor
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    this.textColor = textColor
                    valueFormatter = object : ValueFormatter() {
                        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
                        override fun getFormattedValue(value: Float): String =
                            sdf.format(Date(value.toLong()))
                    }
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    this.textColor = textColor
                }
                axisRight.isEnabled = false
                setNoDataText("No fuel data yet")
                setNoDataTextColor(textColor)
            }
        },
        update = { chart ->
            if (fuelLogs.isEmpty()) {
                chart.clear()
                return@AndroidView
            }
            val entries = fuelLogs
                .sortedBy { it.date }
                .map { Entry(it.date.toFloat(), it.totalCost.toFloat()) }

            val dataSet = LineDataSet(entries, "Fuel Cost").apply {
                color = AndroidColor.parseColor("#1A6BD4")
                setCircleColor(AndroidColor.parseColor("#1A6BD4"))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                fillColor = AndroidColor.parseColor("#331A6BD4")
                setDrawFilled(true)
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        },
        modifier = modifier
    )
}
