package com.cartracker.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cartracker.data.db.entities.Car
import com.cartracker.data.db.dao.MaintenanceCategoryTotal
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.ReportsData
import com.cartracker.ui.viewmodel.ReportsViewModel
import com.cartracker.ui.viewmodel.ReportsViewModelFactory
import com.cartracker.util.CurrencyPrefs
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    carId: Long?,
    cars: List<Car>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currency = remember { CurrencyPrefs.getSymbol(context) }
    val viewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModelFactory(context.applicationContext as android.app.Application)
    )
    val data by viewModel.data.observeAsState(ReportsData())
    val selectedCar = cars.firstOrNull { it.id == carId }

    LaunchedEffect(carId) { carId?.let { viewModel.loadReports(it) } }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Cost & Insights Report", fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
                        if (selectedCar != null) Text(selectedCar.name, color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer)
            )
        }
    ) { padding ->
        if (carId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Select a car to view reports", color = OnSurfaceSecondary)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Total Cost of Ownership ────────────────────────────────
                SectionHeader("TOTAL COST OF OWNERSHIP", Icons.Filled.AccountBalance)
                TotalOwnershipCard(data = data, currency = currency)

                // ── Monthly Averages ───────────────────────────────────────
                SectionHeader("MONTHLY AVERAGES", Icons.Filled.CalendarMonth)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportStatCard("Avg Fuel/mo", if (data.avgMonthlyFuel > 0) "$currency %.3f".format(data.avgMonthlyFuel) else "--", Icons.Filled.LocalGasStation, NeonCyan, Modifier.weight(1f))
                    ReportStatCard("Avg Service/mo", if (data.avgMonthlyMaint > 0) "$currency %.3f".format(data.avgMonthlyMaint) else "--", Icons.Filled.Build, WarnAmber, Modifier.weight(1f))
                }

                // ── Fuel Performance ──────────────────────────────────────
                SectionHeader("FUEL PERFORMANCE", Icons.Filled.LocalGasStation)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportStatCard("Total Fill-ups", data.totalFillUps.toString(), Icons.Filled.Numbers, NeonCyan, Modifier.weight(1f))
                    ReportStatCard("Avg Economy", if (data.avgEfficiency > 0) "%.1f km/L".format(data.avgEfficiency) else "--", Icons.Filled.Speed, SuccessGreen, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportStatCard("Total Fuel Cost", if (data.totalFuelCost > 0) "$currency %.3f".format(data.totalFuelCost) else "--", Icons.Filled.AttachMoney, NeonCyan, Modifier.weight(1f))
                    ReportStatCard("Cost / km", if (data.costPerKm > 0) "$currency %.3f".format(data.costPerKm) else "--", Icons.Filled.Route, WarnAmber, Modifier.weight(1f))
                }

                // ── Mileage Breakdown ─────────────────────────────────────
                if (data.workKm > 0 || data.personalKm > 0) {
                    SectionHeader("MILEAGE BREAKDOWN", Icons.Filled.Route)
                    MileageCard(data = data)
                }

                // ── Maintenance by Category ────────────────────────────────
                if (data.maintenanceByCategory.isNotEmpty()) {
                    SectionHeader("MAINTENANCE SPEND BY CATEGORY", Icons.Filled.Build)
                    MaintenanceCategoryBreakdown(categories = data.maintenanceByCategory, totalMaint = data.totalMaintenanceCost, currency = currency)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
        Text(label, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun TotalOwnershipCard(data: ReportsData, currency: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Total highlight
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("TOTAL OWNERSHIP COST", color = OnSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                Text(
                    if (data.totalOwnershipCost > 0) "$currency %.3f".format(data.totalOwnershipCost) else "--",
                    color = OnSurfacePrimary, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 38.sp
                )
                if (data.totalKmDriven > 0) {
                    Text("$currency %.3f per km driven".format(data.costPerKm), color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

            // Breakdown
            CostRow("Fuel", data.totalFuelCost, data.totalOwnershipCost, NeonCyan, currency)
            CostRow("Maintenance & Service", data.totalMaintenanceCost, data.totalOwnershipCost, WarnAmber, currency)
            CostRow("Other Expenses", data.totalExpenseCost, data.totalOwnershipCost, Color(0xFF9C6ADE), currency)
        }
    }
}

@Composable
private fun CostRow(label: String, amount: Double, total: Double, color: Color, currency: String) {
    val pct = if (total > 0) (amount / total * 100).toInt() else 0
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Text(label, color = OnSurfacePrimary, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$pct%", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
            Text(if (amount > 0) "$currency %.3f".format(amount) else "--", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ReportStatCard(label: String, value: String, icon: ImageVector, tintColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(SurfaceContainer)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = tintColor, modifier = Modifier.size(18.dp))
            Text(value, color = OnSurfacePrimary, fontWeight = FontWeight.Black, fontSize = 18.sp, lineHeight = 20.sp)
            Text(label, color = OnSurfaceSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MileageCard(data: ReportsData) {
    val total = data.workKm + data.personalKm
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(18.dp)).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                MileageStatItem("WORK", data.workKm, total, Color(0xFF2196F3))
                MileageStatItem("PERSONAL", data.personalKm, total, SuccessGreen)
                MileageStatItem("TOTAL", total, total, NeonCyan)
            }
            if (data.workKm > 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1A2196F3)).border(1.dp, Color(0x402196F3), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Info, null, tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                        Text("%.0f km may be tax-deductible as business mileage".format(data.workKm),
                            color = Color(0xFF2196F3), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MileageStatItem(label: String, km: Double, total: Double, color: Color) {
    val pct = if (total > 0) (km / total * 100).toInt() else 0
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = OnSurfaceSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        Text("%.0f".format(km), color = color, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Text("km · $pct%", color = OnSurfaceSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun MaintenanceCategoryBreakdown(categories: List<MaintenanceCategoryTotal>, totalMaint: Double, currency: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(18.dp)).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            categories.forEach { item ->
                val pct = if (totalMaint > 0) (item.total / totalMaint * 100).toInt() else 0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(item.category.displayName, color = OnSurfacePrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("$pct%", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                            Text("$currency %.3f".format(item.total), color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = WarnAmber, trackColor = SurfaceContainerHighest
                    )
                }
            }
        }
    }
}
