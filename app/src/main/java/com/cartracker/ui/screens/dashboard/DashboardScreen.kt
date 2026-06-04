package com.cartracker.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cartracker.data.db.entities.Car
import com.cartracker.data.db.entities.FuelLog
import com.cartracker.ui.components.CarLogoImage
import com.cartracker.ui.components.CarPickerSheet
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.DashboardStats
import com.cartracker.ui.viewmodel.DashboardViewModel
import com.cartracker.ui.viewmodel.DashboardViewModelFactory
import com.cartracker.ui.viewmodel.MonthlyFuelStat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    carId: Long?,
    cars: List<Car>,
    onCarSelected: (Long) -> Unit,
    onAddFuel: () -> Unit,
    onAddTrip: () -> Unit,
    onAddService: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(context.applicationContext as android.app.Application)
    )
    val stats by viewModel.stats.observeAsState()
    var showCarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(carId) {
        carId?.let { viewModel.loadStats(it) }
    }

    val selectedCar = cars.firstOrNull { it.id == carId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 104.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedCar != null) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerHigh),
                            Alignment.Center
                        ) {
                            CarLogoImage(
                                make = selectedCar.make,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "MY GARAGE",
                            color = OnSurfaceSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = selectedCar?.name
                                ?: if (cars.isEmpty()) "Add a Car" else "Select Car",
                            style = MaterialTheme.typography.titleLarge,
                            color = OnSurfacePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (cars.size > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHigh)
                            .clickable { showCarPicker = true }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "Switch car",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (carId == null || cars.isEmpty()) {
                EmptyState(modifier = Modifier.padding(horizontal = 20.dp))
            } else {
                // ── TOP 25%: Glassmorphic hero card ───────────────────────
                HeroCard(
                    odometer = stats?.totalMileage ?: 0.0,
                    fuelPercent = stats?.fuelPercent ?: 0.72f,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(12.dp))

                // ── MIDDLE 40%: Bento grid ────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FuelEconomyCard(
                        avgEfficiency = stats?.avgFuelEfficiency ?: 0.0,
                        monthlyCost = stats?.monthlyFuelCost ?: 0.0,
                        recentLogs = stats?.recentFuelLogs ?: emptyList(),
                        modifier = Modifier.weight(1.15f)
                    )
                    MaintenanceCard(
                        lastServiceDate = stats?.lastServiceDate,
                        modifier = Modifier.weight(0.85f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── ANALYTICS ────────────────────────────────────────────
                AnalyticsSection(
                    stats = stats,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // ── BOTTOM 35%: Sticky quick-add dock ─────────────────────────────
        QuickAddDock(
            onAddFuel = onAddFuel,
            onAddTrip = onAddTrip,
            onAddService = onAddService,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ── Car picker sheet ──────────────────────────────────────────────────
    if (showCarPicker) {
        CarPickerSheet(
            cars = cars,
            selectedCarId = carId,
            onSelect = { onCarSelected(it); showCarPicker = false },
            onDismiss = { showCarPicker = false }
        )
    }
}

// ─── Hero Card ────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    odometer: Double,
    fuelPercent: Float,
    modifier: Modifier = Modifier
) {
    val formattedOdo = remember(odometer) {
        DecimalFormat("#,###").format(odometer.roundToInt())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF13131F), Color(0xFF0A0A0D)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(1.dp, NeonCyanBorder, RoundedCornerShape(24.dp))
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Odometer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "ODOMETER",
                        color = OnSurfaceSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                    Text(
                        formattedOdo,
                        color = OnSurfacePrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 48.sp,
                        lineHeight = 50.sp
                    )
                    Text(
                        "miles",
                        color = OnSurfaceSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyanGlow)
                        .border(1.dp, NeonCyanBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "LIVE",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Fuel gauge row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocalGasStation,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "FUEL LEVEL",
                            color = OnSurfaceSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Text(
                        "${(fuelPercent * 100).roundToInt()}%",
                        color = NeonCyan,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                FuelGaugeLine(fuelPercent = fuelPercent)
            }
        }
    }
}

@Composable
private fun FuelGaugeLine(fuelPercent: Float) {
    val animated by animateFloatAsState(
        targetValue = fuelPercent,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "fuelGauge"
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        val trackColor = Color(0xFF252530)
        val trackH = size.height

        drawLine(
            color = trackColor,
            start = Offset(0f, trackH / 2),
            end = Offset(size.width, trackH / 2),
            strokeWidth = trackH,
            cap = StrokeCap.Round
        )

        val fillEnd = size.width * animated
        if (fillEnd > 0f) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(NeonCyanDim, NeonCyan),
                    startX = 0f,
                    endX = fillEnd
                ),
                start = Offset(0f, trackH / 2),
                end = Offset(fillEnd, trackH / 2),
                strokeWidth = trackH,
                cap = StrokeCap.Round
            )
            // Glowing endpoint dot
            drawCircle(
                color = Color(0x4000E5FF),
                radius = trackH * 3f,
                center = Offset(fillEnd, trackH / 2)
            )
            drawCircle(
                color = NeonCyan,
                radius = trackH * 1.2f,
                center = Offset(fillEnd, trackH / 2)
            )
        }
    }
}

// ─── Fuel Economy Card ───────────────────────────────────────────────────────

@Composable
private fun FuelEconomyCard(
    avgEfficiency: Double,
    monthlyCost: Double,
    recentLogs: List<FuelLog>,
    modifier: Modifier = Modifier
) {
    BentoCard(modifier = modifier.height(190.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "ECONOMY",
                    color = OnSurfaceSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
                Text(
                    if (avgEfficiency > 0) "%.1f".format(avgEfficiency) else "--",
                    color = OnSurfacePrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                    lineHeight = 36.sp
                )
                Text(
                    "km / L avg",
                    color = OnSurfaceSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            SparkLine(
                logs = recentLogs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.AttachMoney,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    if (monthlyCost > 0) "BD %.3f / mo".format(monthlyCost) else "No data",
                    color = OnSurfaceSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SparkLine(logs: List<FuelLog>, modifier: Modifier = Modifier) {
    val points = remember(logs) {
        logs.map { it.fuelEfficiency.toFloat() }.filter { it > 0f }
    }

    if (points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No trend data", color = OnSurfaceSecondary, fontSize = 10.sp)
        }
        return
    }

    val minV = points.min()
    val maxV = points.max()
    val range = (maxV - minV).coerceAtLeast(0.1f)

    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1)
        val offsets = points.mapIndexed { i, v ->
            Offset(
                x = i * stepX,
                y = size.height - ((v - minV) / range) * size.height * 0.85f - size.height * 0.075f
            )
        }

        // Filled area under curve
        val areaPath = Path().apply {
            moveTo(offsets.first().x, size.height)
            offsets.forEach { lineTo(it.x, it.y) }
            lineTo(offsets.last().x, size.height)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x2000E5FF), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )

        // Line
        val linePath = Path().apply {
            offsets.forEachIndexed { i, o ->
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
        }
        drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(
                colors = listOf(NeonCyanDim, NeonCyan),
                startX = 0f,
                endX = size.width
            ),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // End dot
        offsets.lastOrNull()?.let { last ->
            drawCircle(Color(0x5000E5FF), radius = 5.dp.toPx(), center = last)
            drawCircle(NeonCyan, radius = 2.5.dp.toPx(), center = last)
        }
    }
}

// ─── Maintenance Card ─────────────────────────────────────────────────────────

@Composable
private fun MaintenanceCard(
    lastServiceDate: Long?,
    modifier: Modifier = Modifier
) {
    val intervalDays = 90
    val daysSince = lastServiceDate?.let {
        ((System.currentTimeMillis() - it) / (1000L * 60 * 60 * 24)).toInt()
    }
    val daysLeft = daysSince?.let { (intervalDays - it).coerceAtLeast(0) }
    val urgencyColor = when {
        daysLeft == null -> OnSurfaceSecondary
        daysLeft <= 7 -> ErrorRed
        daysLeft <= 21 -> WarnAmber
        else -> SuccessGreen
    }
    val progress = daysLeft?.let {
        (it.toFloat() / intervalDays).coerceIn(0f, 1f)
    } ?: 0f

    BentoCard(modifier = modifier.height(190.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "OIL CHANGE",
                    color = OnSurfaceSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
                Text(
                    daysLeft?.toString() ?: "--",
                    color = urgencyColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 40.sp,
                    lineHeight = 42.sp
                )
                Text(
                    if (daysLeft != null) "days left" else "no data",
                    color = OnSurfaceSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = urgencyColor,
                    trackColor = SurfaceContainerHighest
                )
                lastServiceDate?.let {
                    Text(
                        "Last: ${
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(Date(it))
                        }",
                        color = OnSurfaceSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// ─── Quick-Add Dock ───────────────────────────────────────────────────────────

@Composable
private fun QuickAddDock(
    onAddFuel: () -> Unit,
    onAddTrip: () -> Unit,
    onAddService: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, TrueBlack),
                    startY = 0f,
                    endY = 32f
                )
            )
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainer)
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockAction(label = "+ Fuel", icon = Icons.Filled.LocalGasStation, onClick = onAddFuel)

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(GlassBorder)
            )

            DockAction(label = "+ Trip", icon = Icons.Filled.DirectionsCar, onClick = onAddTrip)

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(GlassBorder)
            )

            DockAction(label = "+ Service", icon = Icons.Filled.Build, onClick = onAddService)
        }
    }
}

@Composable
private fun DockAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NeonCyanGlow)
                .border(1.dp, NeonCyanBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = NeonCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            label,
            color = OnSurfacePrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Shared primitives ────────────────────────────────────────────────────────

@Composable
private fun BentoCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainer)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
        content = content
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainer)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = NeonCyan
            )
            Text(
                "No Cars Yet",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfacePrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Add your first car from the Cars tab",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceSecondary
            )
        }
    }
}

// ─── Analytics Section ────────────────────────────────────────────────────────

@Composable
private fun AnalyticsSection(stats: DashboardStats?, modifier: Modifier = Modifier) {
    if (stats == null) return
    val hasData = stats.avgKmPerTank > 0 || stats.monthlyStats.any { it.cost > 0 }
    if (!hasData) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "PERFORMANCE",
            color = OnSurfaceSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )

        // Stat snapshot row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SnapStat(
                label = "Avg Range",
                value = if (stats.avgKmPerTank > 0) "%.0f km".format(stats.avgKmPerTank) else "--",
                sub = "per tank",
                modifier = Modifier.weight(1f)
            )
            SnapStat(
                label = "Fill-up",
                value = if (stats.avgDaysBetweenFillups > 0) "%.1f d".format(stats.avgDaysBetweenFillups) else "--",
                sub = "avg interval",
                modifier = Modifier.weight(1f)
            )
            SnapStat(
                label = "Cost / km",
                value = if (stats.costPerKm > 0) "BD %.3f".format(stats.costPerKm) else "--",
                sub = "fuel cost",
                modifier = Modifier.weight(1f)
            )
        }

        // Monthly spend bar chart
        if (stats.monthlyStats.any { it.cost > 0 }) {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Monthly Spend", color = OnSurfacePrimary,
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val totalSpend = stats.monthlyStats.sumOf { it.cost }
                        if (totalSpend > 0) {
                            Text("BD %.3f total".format(totalSpend), color = OnSurfaceSecondary,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    MonthlyBarChart(
                        months = stats.monthlyStats,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            }
        }

        // Fuel economy trend (enlarged)
        if (stats.recentFuelLogs.size >= 3) {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Fuel Economy Trend", color = OnSurfacePrimary,
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (stats.avgFuelEfficiency > 0) {
                            Text("%.1f km/L avg".format(stats.avgFuelEfficiency), color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    EconomyTrendChart(
                        logs = stats.recentFuelLogs,
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SnapStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    BentoCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, color = OnSurfaceSecondary, fontSize = 10.sp,
                fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            Text(value, color = OnSurfacePrimary, fontWeight = FontWeight.Black,
                fontSize = 18.sp, lineHeight = 20.sp)
            Text(sub, color = OnSurfaceSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MonthlyBarChart(months: List<MonthlyFuelStat>, modifier: Modifier = Modifier) {
    val maxCost = remember(months) { months.maxOfOrNull { it.cost }?.coerceAtLeast(1.0) ?: 1.0 }
    val barMaxDp = 80.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        months.forEach { stat ->
            val barH = if (stat.cost > 0)
                ((stat.cost / maxCost) * barMaxDp.value).dp.coerceAtLeast(4.dp)
            else 2.dp

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (stat.cost > 0) {
                    Text("BD %.3f".format(stat.cost), fontSize = 8.sp, color = OnSurfaceSecondary, lineHeight = 10.sp)
                    Spacer(Modifier.height(3.dp))
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(barH)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            if (stat.cost > 0)
                                Brush.verticalGradient(listOf(NeonCyan, NeonCyanDim))
                            else
                                Brush.verticalGradient(listOf(SurfaceContainerHighest, SurfaceContainerHighest))
                        )
                )
                Spacer(Modifier.height(5.dp))
                Text(stat.label, fontSize = 9.sp, color = OnSurfaceSecondary)
            }
        }
    }
}

@Composable
private fun EconomyTrendChart(logs: List<com.cartracker.data.db.entities.FuelLog>, modifier: Modifier = Modifier) {
    val points = remember(logs) {
        logs.sortedBy { it.date }.map { it.fuelEfficiency.toFloat() }.filter { it > 0f }
    }
    if (points.size < 2) return

    val minV = points.min(); val maxV = points.max()
    val range = (maxV - minV).coerceAtLeast(0.1f)

    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1)
        val offsets = points.mapIndexed { i, v ->
            Offset(
                x = i * stepX,
                y = size.height - ((v - minV) / range) * size.height * 0.82f - size.height * 0.09f
            )
        }

        val areaPath = Path().apply {
            moveTo(offsets.first().x, size.height)
            offsets.forEach { lineTo(it.x, it.y) }
            lineTo(offsets.last().x, size.height); close()
        }
        drawPath(areaPath, brush = Brush.verticalGradient(
            colors = listOf(Color(0x3000E5FF), Color.Transparent), startY = 0f, endY = size.height))

        val linePath = Path().apply {
            offsets.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
        }
        drawPath(linePath,
            brush = Brush.horizontalGradient(listOf(NeonCyanDim, NeonCyan), 0f, size.width),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        offsets.lastOrNull()?.let {
            drawCircle(Color(0x5000E5FF), radius = 5.dp.toPx(), center = it)
            drawCircle(NeonCyan, radius = 2.5.dp.toPx(), center = it)
        }
    }
}
