package com.cartracker.ui.screens.healthchecks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cartracker.data.db.entities.HealthCheck
import com.cartracker.data.db.entities.HealthCheckType
import com.cartracker.ui.screens.fuellog.sheetFieldColors
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.CheckStatus
import com.cartracker.ui.viewmodel.HealthCheckUi
import com.cartracker.ui.viewmodel.HealthChecksViewModel
import com.cartracker.ui.viewmodel.HealthChecksViewModelFactory
import java.util.Locale
import kotlin.math.abs

private val checkIcons: Map<HealthCheckType, ImageVector> = mapOf(
    HealthCheckType.ENGINE_OIL    to Icons.Filled.Opacity,
    HealthCheckType.TYRE_PRESSURE to Icons.Filled.RadioButtonUnchecked,
    HealthCheckType.COOLANT       to Icons.Filled.WaterDrop,
    HealthCheckType.WASHER_FLUID  to Icons.Filled.Wash,
    HealthCheckType.LIGHTS        to Icons.Filled.Highlight,
    HealthCheckType.BRAKE_FLUID   to Icons.Filled.StopCircle,
    HealthCheckType.BATTERY       to Icons.Filled.BatteryChargingFull,
    HealthCheckType.AIR_FILTER    to Icons.Filled.Air,
    HealthCheckType.WIPER_BLADES  to Icons.Filled.Visibility
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthChecksScreen(carId: Long?) {
    val context = LocalContext.current
    val viewModel: HealthChecksViewModel = viewModel(
        factory = HealthChecksViewModelFactory(context.applicationContext as android.app.Application)
    )
    val checks by viewModel.checks.observeAsState(emptyList())

    var showOilSheet by remember { mutableStateOf<HealthCheckUi?>(null) }

    LaunchedEffect(carId) { carId?.let { viewModel.setCarId(it) } }

    if (carId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a car to view health checks", color = OnSurfaceSecondary)
        }
        return
    }

    if (checks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                Text("Loading checks…", color = OnSurfaceSecondary, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        item {
            val overdue = checks.count { it.status == CheckStatus.NEVER_DONE || it.status == CheckStatus.OVERDUE }
            val dueSoon = checks.count { it.status == CheckStatus.DUE_SOON }
            val ok = checks.count { it.status == CheckStatus.OK }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip(count = overdue, label = "Attention",  color = ErrorRed,     modifier = Modifier.weight(1f))
                SummaryChip(count = dueSoon, label = "Due Soon",   color = WarnAmber,    modifier = Modifier.weight(1f))
                SummaryChip(count = ok,      label = "Good",       color = SuccessGreen, modifier = Modifier.weight(1f))
            }
        }

        items(checks, key = { it.check.checkType.name }) { ui ->
            HealthCheckCard(
                ui = ui,
                onMarkDone = {
                    if (ui.check.checkType == HealthCheckType.ENGINE_OIL) {
                        showOilSheet = ui
                    } else {
                        viewModel.markDone(ui.check.carId, ui.check.checkType)
                    }
                }
            )
        }
    }

    showOilSheet?.let { ui ->
        OilChangeSheet(
            onDismiss = { showOilSheet = null },
            onSave = { brand, spec, liters, cost ->
                viewModel.logOilChange(ui.check.carId, brand, spec, liters, cost)
                showOilSheet = null
            }
        )
    }
}

// ─── Summary Chip ────────────────────────────────────────────────────────────

@Composable
private fun SummaryChip(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(count.toString(), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, color = color.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ─── Check Card ──────────────────────────────────────────────────────────────

@Composable
private fun HealthCheckCard(ui: HealthCheckUi, onMarkDone: () -> Unit) {
    val check = ui.check
    val accent = when (ui.status) {
        CheckStatus.NEVER_DONE, CheckStatus.OVERDUE -> ErrorRed
        CheckStatus.DUE_SOON                        -> WarnAmber
        CheckStatus.OK                              -> SuccessGreen
    }
    val icon = checkIcons[check.checkType] ?: Icons.Filled.CheckCircle
    val isOil = check.checkType == HealthCheckType.ENGINE_OIL

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.13f)),
                        Alignment.Center
                    ) {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(check.checkType.displayName, color = OnSurfacePrimary,
                            fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(check.checkType.description, color = OnSurfaceSecondary,
                            style = MaterialTheme.typography.labelSmall, lineHeight = 14.sp)
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.13f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel(ui), color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { ui.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.12f)
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(lastCheckedLabel(ui), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(intervalLabel(check), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Last oil change details (shown after logging)
            if (!check.notes.isNullOrBlank()) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.07f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Opacity, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                        Text(check.notes, color = accent.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Action button
            Button(
                onClick = onMarkDone,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.15f),
                    contentColor = accent
                ),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(if (isOil) Icons.Filled.Opacity else Icons.Filled.Check,
                    null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isOil) "Log Oil Change" else "Mark Done Today",
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// ─── Oil Change Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OilChangeSheet(
    onDismiss: () -> Unit,
    onSave: (brand: String, spec: String, liters: Double?, cost: Double?) -> Unit
) {
    var brand  by remember { mutableStateOf("") }
    var spec   by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var cost   by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp)).background(SurfaceContainerHighest))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Log Oil Change", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = OnSurfacePrimary)

            // Subtle note that it auto-creates a service log
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(NeonCyanGlow).border(1.dp, NeonCyanBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Text("Also saves to Service log automatically", color = NeonCyan,
                        style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = brand, onValueChange = { brand = it },
                    label = { Text("Brand") },
                    placeholder = { Text("e.g. Shell Helix", color = OnSurfaceSecondary) },
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = spec, onValueChange = { spec = it },
                    label = { Text("Viscosity") },
                    placeholder = { Text("e.g. 5W-30", color = OnSurfaceSecondary) },
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = liters, onValueChange = { liters = it },
                    label = { Text("Liters") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = cost, onValueChange = { cost = it },
                    label = { Text("Cost (BD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }

            Button(
                onClick = {
                    onSave(brand.trim(), spec.trim(), liters.toDoubleOrNull(), cost.toDoubleOrNull())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Save Oil Change", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun statusLabel(ui: HealthCheckUi): String = when (ui.status) {
    CheckStatus.NEVER_DONE -> "Never done"
    CheckStatus.OVERDUE    -> {
        val kmOver = ui.kmUntilDue?.let { if (it < 0) "${(-it).toInt()} km over" else null }
        kmOver ?: "${abs(ui.daysUntilDue)}d overdue"
    }
    CheckStatus.DUE_SOON   -> {
        val kmSoon = ui.kmUntilDue?.let { if (it in 0.0..500.0) "${it.toInt()} km left" else null }
        kmSoon ?: if (ui.daysUntilDue == 0L) "Due today" else "Due in ${ui.daysUntilDue}d"
    }
    CheckStatus.OK         -> {
        val kmLabel = ui.kmUntilDue?.let { "${it.toInt()} km left" }
        kmLabel ?: "Due in ${ui.daysUntilDue}d"
    }
}

private fun lastCheckedLabel(ui: HealthCheckUi): String {
    val daysLabel = when {
        ui.daysSinceLast == null -> "Never checked"
        ui.daysSinceLast == 0L   -> "Today"
        ui.daysSinceLast == 1L   -> "Yesterday"
        else                     -> "${ui.daysSinceLast}d ago"
    }
    val kmLabel = ui.kmSinceLast?.let { "%.0f km since".format(it) }
    return if (kmLabel != null) "$kmLabel · $daysLabel" else daysLabel
}

private fun intervalLabel(check: HealthCheck): String {
    val timeLabel = when {
        check.intervalDays < 14  -> "${check.intervalDays} days"
        check.intervalDays < 60  -> "${check.intervalDays / 7} weeks"
        check.intervalDays < 365 -> "${check.intervalDays / 30} months"
        else                     -> "${check.intervalDays / 365} year"
    }
    return if (check.intervalKm != null) {
        "Every ${String.format(Locale.US, "%,d", check.intervalKm)} km"
    } else {
        "Every $timeLabel"
    }
}
