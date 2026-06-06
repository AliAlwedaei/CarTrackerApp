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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cartracker.data.db.entities.Car
import com.cartracker.data.db.entities.FuelLog
import com.cartracker.ui.components.CarLogoImage
import java.io.File
import com.cartracker.ui.components.CarPickerSheet
import com.cartracker.R
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.DashboardStats
import com.cartracker.ui.viewmodel.DashboardViewModel
import com.cartracker.ui.viewmodel.DashboardViewModelFactory
import com.cartracker.ui.viewmodel.MonthlySpendStat
import com.cartracker.util.BudgetPrefs
import com.cartracker.util.CurrencyPrefs
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
    onAddService: () -> Unit,
    onSettings: () -> Unit = {},
    onViewReports: () -> Unit = {},
    onViewAlerts: () -> Unit = {},
) {
    val context = LocalContext.current
    val currency = remember { CurrencyPrefs.getSymbol(context) }
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
                            if (selectedCar.photoUri.isNotBlank() && File(selectedCar.photoUri).exists()) {
                                AsyncImage(
                                    model = File(selectedCar.photoUri),
                                    contentDescription = selectedCar.name,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                CarLogoImage(make = selectedCar.make, modifier = Modifier.size(28.dp))
                            }
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
                            text = selectedCar?.name ?: if (cars.isEmpty()) "Add a Car" else "Select Car",
                            style = MaterialTheme.typography.titleLarge,
                            color = OnSurfacePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cars.size > 1) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHigh)
                                .clickable { showCarPicker = true }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SwapHoriz, "Switch car", tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerHigh)
                            .clickable(onClick = onSettings)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Settings, "Settings", tint = OnSurfaceSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (carId == null || cars.isEmpty()) {
                EmptyState(modifier = Modifier.padding(horizontal = 20.dp))
            } else {
                // ── ZONE 1: Action Required ───────────────────────────────
                val overdueCount = stats?.overdueChecksCount ?: 0
                val dueSoonCount = stats?.dueSoonChecksCount ?: 0
                if (overdueCount > 0 || dueSoonCount > 0) {
                    ActionRequiredBanner(
                        overdueCount = overdueCount,
                        dueSoonCount = dueSoonCount,
                        onClick = onViewAlerts,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // ── ZONE 2: Hero card + spend overview ────────────────────
                HeroCard(
                    odometer = stats?.totalMileage ?: 0.0,
                    fuelPercent = stats?.fuelPercent ?: 0.72f,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(12.dp))

                // YTD spend + monthly fuel+maintenance cards
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    YtdCard(
                        ytdTotal = stats?.ytdTotalCost ?: 0.0,
                        currency = currency,
                        modifier = Modifier.weight(1f)
                    )
                    MonthlyCard(
                        fuelCost = stats?.monthlyFuelCost ?: 0.0,
                        maintCost = stats?.monthlyMaintCost ?: 0.0,
                        expenseCost = stats?.monthlyExpenseCost ?: 0.0,
                        currency = currency,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── ZONE 3: Bento grid ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FuelEconomyCard(
                        avgEfficiency = stats?.avgFuelEfficiency ?: 0.0,
                        recentLogs = stats?.recentFuelLogs ?: emptyList(),
                        modifier = Modifier.weight(1.15f)
                    )
                    MaintenanceCard(
                        lastServiceDate = stats?.lastServiceDate,
                        modifier = Modifier.weight(0.85f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Fuel price trend ──────────────────────────────────────
                val priceHistory = stats?.fuelPriceHistory ?: emptyList()
                if (priceHistory.size >= 2) {
                    FuelPriceCard(
                        priceHistory = priceHistory,
                        currency = currency,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // ── ZONE 4: Analytics ────────────────────────────────────
                AnalyticsSection(
                    stats = stats,
                    currency = currency,
                    onViewReports = onViewReports,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        // ── Sticky quick-add dock ──────────────────────────────────────────
        QuickAddDock(
            onAddFuel = onAddFuel,
            onAddService = onAddService,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showCarPicker) {
        CarPickerSheet(
            cars = cars,
            selectedCarId = carId,
            onSelect = { onCarSelected(it); showCarPicker = false },
            onDismiss = { showCarPicker = false }
        )
    }
}

// ─── Action Required Banner ───────────────────────────────────────────────────

@Composable
private fun ActionRequiredBanner(
    overdueCount: Int,
    dueSoonCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrgent = overdueCount > 0
    val bannerColor = if (isUrgent) ErrorRedGlow else Color(0x2DFF9800)
    val borderColor = if (isUrgent) ErrorRed.copy(alpha = 0.4f) else WarnAmber.copy(alpha = 0.4f)
    val textColor = if (isUrgent) ErrorRed else WarnAmber
    val icon = if (isUrgent) Icons.Filled.Error else Icons.Filled.Warning

    val label = buildString {
        if (overdueCount > 0) append("$overdueCount overdue")
        if (overdueCount > 0 && dueSoonCount > 0) append(" · ")
        if (dueSoonCount > 0) append("$dueSoonCount due soon")
    }

    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bannerColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Health Check Alert", color = textColor, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
                Text(label, color = textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = textColor, modifier = Modifier.size(18.dp))
    }
}

// ─── YTD Card ─────────────────────────────────────────────────────────────────

@Composable
private fun YtdCard(ytdTotal: Double, currency: String, modifier: Modifier = Modifier) {
    BentoCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("YEAR TO DATE", color = OnSurfaceSecondary, fontSize = 9.sp,
                fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
            Text(
                if (ytdTotal > 0) String.format(Locale.US, "$currency %,.3f", ytdTotal) else "--",
                color = if (ytdTotal > 0) OnSurfacePrimary else OnSurfaceSecondary,
                fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 22.sp
            )
            Text("total spend", color = OnSurfaceSecondary, fontSize = 10.sp)
        }
    }
}

// ─── Monthly Card ─────────────────────────────────────────────────────────────

@Composable
private fun MonthlyCard(
    fuelCost: Double, maintCost: Double, expenseCost: Double,
    currency: String, modifier: Modifier = Modifier
) {
    val total = fuelCost + maintCost + expenseCost
    BentoCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("THIS MONTH", color = OnSurfaceSecondary, fontSize = 9.sp,
                fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
            Text(
                if (total > 0) String.format(Locale.US, "$currency %,.3f", total) else "--",
                color = if (total > 0) OnSurfacePrimary else OnSurfaceSecondary,
                fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 22.sp
            )
            if (total > 0) {
                Text("fuel + service", color = OnSurfaceSecondary, fontSize = 10.sp)
            } else {
                Text("no spend logged", color = OnSurfaceSecondary, fontSize = 10.sp)
            }
        }
    }
}

// ─── Hero Card ────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(odometer: Double, fuelPercent: Float, modifier: Modifier = Modifier) {
    val formattedOdo = remember(odometer) { DecimalFormat("#,###").format(odometer.roundToInt()) }

    Box(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(
                colors = listOf(Color(0xFF13131F), Color(0xFF0A0A0D)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ))
            .border(1.dp, NeonCyanBorder, RoundedCornerShape(24.dp))
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("ODOMETER", color = OnSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                    Text(formattedOdo, color = OnSurfacePrimary, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 50.sp)
                    Text("km", color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.LocalGasStation, null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                        Text("FUEL LEVEL (ESTIMATED)", color = OnSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                    }
                    Text("${(fuelPercent * 100).roundToInt()}%", color = NeonCyan,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
    Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
        val trackColor = Color(0xFF252530)
        val trackH = size.height
        drawLine(color = trackColor, start = Offset(0f, trackH / 2), end = Offset(size.width, trackH / 2), strokeWidth = trackH, cap = StrokeCap.Round)
        val fillEnd = size.width * animated
        if (fillEnd > 0f) {
            drawLine(
                brush = Brush.horizontalGradient(colors = listOf(NeonCyanDim, NeonCyan), startX = 0f, endX = fillEnd),
                start = Offset(0f, trackH / 2), end = Offset(fillEnd, trackH / 2), strokeWidth = trackH, cap = StrokeCap.Round
            )
            drawCircle(color = Color(0x4000E5FF), radius = trackH * 3f, center = Offset(fillEnd, trackH / 2))
            drawCircle(color = NeonCyan, radius = trackH * 1.2f, center = Offset(fillEnd, trackH / 2))
        }
    }
}

// ─── Fuel Economy Card ────────────────────────────────────────────────────────

@Composable
private fun FuelEconomyCard(avgEfficiency: Double, recentLogs: List<FuelLog>, modifier: Modifier = Modifier) {
    BentoCard(modifier = modifier.height(160.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("ECONOMY", color = OnSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                Text(if (avgEfficiency > 0) String.format(Locale.US, "%.1f", avgEfficiency) else "--",
                    color = OnSurfacePrimary, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 36.sp)
                Text("km / L avg", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
            }
            SparkLine(logs = recentLogs, modifier = Modifier.fillMaxWidth().height(40.dp))
        }
    }
}

@Composable
private fun SparkLine(logs: List<FuelLog>, modifier: Modifier = Modifier) {
    val points = remember(logs) { logs.filter { it.isFullTank }.map { it.fuelEfficiency.toFloat() }.filter { it > 0f } }
    if (points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No trend data", color = OnSurfaceSecondary, fontSize = 10.sp)
        }
        return
    }
    val minV = points.min(); val maxV = points.max()
    val range = (maxV - minV).coerceAtLeast(0.1f)
    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1)
        val offsets = points.mapIndexed { i, v ->
            Offset(x = i * stepX, y = size.height - ((v - minV) / range) * size.height * 0.85f - size.height * 0.075f)
        }
        val areaPath = Path().apply {
            moveTo(offsets.first().x, size.height)
            offsets.forEach { lineTo(it.x, it.y) }
            lineTo(offsets.last().x, size.height); close()
        }
        drawPath(areaPath, brush = Brush.verticalGradient(colors = listOf(Color(0x2000E5FF), Color.Transparent), startY = 0f, endY = size.height))
        val linePath = Path().apply { offsets.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(linePath, brush = Brush.horizontalGradient(colors = listOf(NeonCyanDim, NeonCyan), startX = 0f, endX = size.width),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        offsets.lastOrNull()?.let { last ->
            drawCircle(Color(0x5000E5FF), radius = 5.dp.toPx(), center = last)
            drawCircle(NeonCyan, radius = 2.5.dp.toPx(), center = last)
        }
    }
}

// ─── Maintenance Card ─────────────────────────────────────────────────────────

@Composable
private fun MaintenanceCard(lastServiceDate: Long?, modifier: Modifier = Modifier) {
    val intervalDays = 90
    val daysSince = lastServiceDate?.let { ((System.currentTimeMillis() - it) / (1000L * 60 * 60 * 24)).toInt() }
    val daysLeft = daysSince?.let { (intervalDays - it).coerceAtLeast(0) }
    val urgencyColor = when { daysLeft == null -> OnSurfaceSecondary; daysLeft <= 7 -> ErrorRed; daysLeft <= 21 -> WarnAmber; else -> SuccessGreen }
    val progress = daysLeft?.let { (it.toFloat() / intervalDays).coerceIn(0f, 1f) } ?: 0f

    BentoCard(modifier = modifier.height(160.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("OIL CHANGE", color = OnSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                Text(daysLeft?.toString() ?: "--", color = urgencyColor, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 42.sp)
                Text(if (daysLeft != null) "days left" else "no data", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = urgencyColor, trackColor = SurfaceContainerHighest
                )
                lastServiceDate?.let {
                    Text("Last: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))}",
                        color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ─── Quick-Add Dock ───────────────────────────────────────────────────────────

@Composable
private fun QuickAddDock(onAddFuel: () -> Unit, onAddService: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, TrueBlack), startY = 0f, endY = 32f))
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            DockAction(label = "+ Fuel", icon = Icons.Filled.LocalGasStation, onClick = onAddFuel)
            Box(Modifier.width(1.dp).height(44.dp).background(GlassBorder))
            DockAction(label = "+ Service", icon = Icons.Filled.Build, onClick = onAddService)
        }
    }
}

// ─── Fuel Price Card ─────────────────────────────────────────────────────────

@Composable
private fun FuelPriceCard(priceHistory: List<Float>, currency: String, modifier: Modifier = Modifier) {
    val latest = priceHistory.lastOrNull() ?: return
    val oldest = priceHistory.firstOrNull() ?: return
    val trend = latest - oldest
    val trendColor = when { trend > 0.005f -> ErrorRed; trend < -0.005f -> SuccessGreen; else -> OnSurfaceSecondary }
    val trendIcon = when { trend > 0.005f -> Icons.Filled.ArrowUpward; trend < -0.005f -> Icons.Filled.ArrowDownward; else -> Icons.Filled.Remove }

    BentoCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("FUEL PRICE", color = OnSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(String.format(Locale.US, "$currency %.3f / L", latest), color = OnSurfacePrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Icon(trendIcon, null, tint = trendColor, modifier = Modifier.size(18.dp))
                }
                Text(
                    if (kotlin.math.abs(trend) < 0.005f) "Stable"
                    else String.format(Locale.US, "%+.3f over last ${priceHistory.size} fill-ups", trend),
                    color = trendColor, style = MaterialTheme.typography.labelSmall
                )
            }
            PriceSparkLine(prices = priceHistory, modifier = Modifier.size(width = 100.dp, height = 44.dp))
        }
    }
}

@Composable
private fun PriceSparkLine(prices: List<Float>, modifier: Modifier = Modifier) {
    if (prices.size < 2) return
    Canvas(modifier = modifier) {
        val minP = prices.min(); val maxP = prices.max()
        val range = (maxP - minP).coerceAtLeast(0.001f)
        val pts = prices.mapIndexed { i, p -> Offset(x = i / (prices.size - 1f) * size.width, y = size.height - ((p - minP) / range) * size.height) }
        val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(path, color = NeonCyan, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = NeonCyan, radius = 4.dp.toPx(), center = pts.last())
    }
}

@Composable
private fun DockAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(NeonCyanGlow).border(1.dp, NeonCyanBorder, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = NeonCyan, modifier = Modifier.size(22.dp))
        }
        Text(label, color = OnSurfacePrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Shared primitives ────────────────────────────────────────────────────────

@Composable
private fun BentoCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(18.dp)), content = content)
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.DirectionsCar, null, modifier = Modifier.size(48.dp), tint = NeonCyan)
            Text("No Cars Yet", style = MaterialTheme.typography.titleMedium, color = OnSurfacePrimary, fontWeight = FontWeight.Bold)
            Text("Add your first car from the Cars tab", style = MaterialTheme.typography.bodySmall, color = OnSurfaceSecondary)
        }
    }
}

// ─── Analytics Section ────────────────────────────────────────────────────────

@Composable
private fun BudgetProgressBar(
    label: String,
    actual: Double,
    budget: Double,
    currency: String
) {
    val pct = (actual / budget).coerceIn(0.0, 1.0).toFloat()
    val overBudget = actual > budget
    val color = if (overBudget) ErrorRed else if (pct > 0.85f) WarnAmber else SuccessGreen
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(label, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
            Text(
                String.format(Locale.US, "$currency %,.3f / $currency %,.3f", actual, budget),
                color = if (overBudget) ErrorRed else OnSurfacePrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (overBudget) FontWeight.Bold else FontWeight.Normal
            )
        }
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
            color = color, trackColor = SurfaceContainerHighest
        )
    }
}

@Composable
private fun AnalyticsSection(
    stats: DashboardStats?,
    currency: String,
    onViewReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (stats == null) return
    val hasData = stats.avgKmPerTank > 0 || stats.monthlyStats.any { it.totalCost > 0 }
    if (!hasData) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("PERFORMANCE", color = OnSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(NeonCyanGlow).border(1.dp, NeonCyanBorder, RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewReports).padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("View Reports", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SnapStat(
                label = "Avg Range", value = if (stats.avgKmPerTank > 0) String.format(Locale.US, "%,.0f km", stats.avgKmPerTank) else "--",
                sub = "per tank", modifier = Modifier.weight(1f)
            )
            SnapStat(
                label = "Fill-up", value = if (stats.avgDaysBetweenFillups > 0) String.format(Locale.US, "%.1f d", stats.avgDaysBetweenFillups) else "--",
                sub = "avg interval", modifier = Modifier.weight(1f)
            )
            SnapStat(
                label = "Cost / km", value = if (stats.costPerKm > 0) String.format(Locale.US, "$currency %.3f", stats.costPerKm) else "--",
                sub = "fuel only", modifier = Modifier.weight(1f)
            )
        }

        // Budget bars (if budgets are set)
        val context = LocalContext.current
        val fuelBudget = remember { BudgetPrefs.getMonthlyFuelBudget(context) }
        val totalBudget = remember { BudgetPrefs.getMonthlyTotalBudget(context) }
        if (fuelBudget != null || totalBudget != null) {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MONTHLY BUDGET", color = OnSurfaceSecondary, fontSize = 10.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                    fuelBudget?.let {
                        BudgetProgressBar("Fuel spend", stats.monthlyFuelCost, it, currency)
                    }
                    totalBudget?.let {
                        BudgetProgressBar("Total spend", stats.monthlyTotalCost, it, currency)
                    }
                }
            }
        }

        // Annual projection
        if (stats.projectedAnnualTotalCost > 0) {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("PROJECTED ANNUAL SPEND", color = OnSurfaceSecondary, fontSize = 10.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
                        Text(String.format(Locale.US, "$currency %,.3f", stats.projectedAnnualTotalCost), color = OnSurfacePrimary,
                            fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("based on year-to-date rate", color = OnSurfaceSecondary, fontSize = 10.sp)
                    }
                    Icon(Icons.Filled.TrendingUp, null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Stacked monthly spend bar chart
        if (stats.monthlyStats.any { it.totalCost > 0 }) {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Monthly Spend", color = OnSurfacePrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val totalSpend = stats.monthlyStats.sumOf { it.totalCost }
                        if (totalSpend > 0) Text(String.format(Locale.US, "$currency %,.3f total", totalSpend), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    // Legend
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendDot(color = NeonCyan, label = "Fuel")
                        LegendDot(color = WarnAmber, label = "Service")
                        LegendDot(color = Color(0xFF9C6ADE), label = "Other")
                    }
                    StackedMonthlyBarChart(months = stats.monthlyStats, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            }
        }

        // Fuel economy trend
        if (stats.recentFuelLogs.size >= 3) {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Fuel Economy Trend", color = OnSurfacePrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (stats.avgFuelEfficiency > 0) {
                            Text(String.format(Locale.US, "%.1f km/L avg", stats.avgFuelEfficiency), color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    EconomyTrendChart(logs = stats.recentFuelLogs, modifier = Modifier.fillMaxWidth().height(80.dp))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, color = OnSurfaceSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun SnapStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    BentoCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = OnSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
            Text(value, color = OnSurfacePrimary, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 20.sp)
            Text(sub, color = OnSurfaceSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StackedMonthlyBarChart(months: List<MonthlySpendStat>, modifier: Modifier = Modifier) {
    val maxTotal = remember(months) { months.maxOfOrNull { it.totalCost }?.coerceAtLeast(1.0) ?: 1.0 }
    val barMaxDp = 80.dp

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            months.forEach { stat ->
                val totalH = if (stat.totalCost > 0) ((stat.totalCost / maxTotal) * barMaxDp.value).dp.coerceAtLeast(4.dp) else 2.dp
                val fuelRatio = if (stat.totalCost > 0) (stat.fuelCost / stat.totalCost).toFloat() else 0f
                val maintRatio = if (stat.totalCost > 0) (stat.maintCost / stat.totalCost).toFloat() else 0f

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (stat.totalCost > 0) {
                        Text(String.format(Locale.US, "%,.0f", stat.totalCost), fontSize = 8.sp, color = OnSurfaceSecondary, lineHeight = 10.sp)
                        Spacer(Modifier.height(3.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(totalH).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))) {
                        if (stat.totalCost > 0) {
                            // Stacked: fuel at bottom, maint in middle, expense at top
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.fillMaxWidth().weight(if (stat.expenseCost > 0) (stat.expenseCost / stat.totalCost).toFloat().coerceAtLeast(0.05f) else 0.001f).background(Color(0xFF9C6ADE)))
                                Box(Modifier.fillMaxWidth().weight(maintRatio.coerceAtLeast(if (stat.maintCost > 0) 0.05f else 0.001f)).background(WarnAmber))
                                Box(Modifier.fillMaxWidth().weight(fuelRatio.coerceAtLeast(if (stat.fuelCost > 0) 0.05f else 0.001f)).background(NeonCyan))
                            }
                        } else {
                            Box(Modifier.fillMaxSize().background(SurfaceContainerHighest))
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(stat.label, fontSize = 9.sp, color = OnSurfaceSecondary)
                }
            }
        }
    }
}

@Composable
private fun EconomyTrendChart(logs: List<FuelLog>, modifier: Modifier = Modifier) {
    val points = remember(logs) {
        logs.sortedBy { it.date }.filter { it.isFullTank }.map { it.fuelEfficiency.toFloat() }.filter { it > 0f }
    }
    if (points.size < 2) return
    val minV = points.min(); val maxV = points.max()
    val range = (maxV - minV).coerceAtLeast(0.1f)
    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1)
        val offsets = points.mapIndexed { i, v -> Offset(x = i * stepX, y = size.height - ((v - minV) / range) * size.height * 0.82f - size.height * 0.09f) }
        val areaPath = Path().apply { moveTo(offsets.first().x, size.height); offsets.forEach { lineTo(it.x, it.y) }; lineTo(offsets.last().x, size.height); close() }
        drawPath(areaPath, brush = Brush.verticalGradient(colors = listOf(Color(0x3000E5FF), Color.Transparent), startY = 0f, endY = size.height))
        val linePath = Path().apply { offsets.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
        drawPath(linePath, brush = Brush.horizontalGradient(listOf(NeonCyanDim, NeonCyan), 0f, size.width), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        offsets.lastOrNull()?.let { drawCircle(Color(0x5000E5FF), radius = 5.dp.toPx(), center = it); drawCircle(NeonCyan, radius = 2.5.dp.toPx(), center = it) }
    }
}
