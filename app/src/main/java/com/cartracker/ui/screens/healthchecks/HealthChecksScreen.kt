package com.cartracker.ui.screens.healthchecks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.cartracker.data.db.entities.CustomHealthCheck
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

    val customChecks by viewModel.customChecks.observeAsState(emptyList())

    var showOilSheet       by remember { mutableStateOf<HealthCheckUi?>(null) }
    var showServiceSheet   by remember { mutableStateOf<HealthCheckUi?>(null) }
    var editIntervalFor    by remember { mutableStateOf<HealthCheckUi?>(null) }
    var showAddCustomSheet by remember { mutableStateOf(false) }
    var deleteCustomTarget by remember { mutableStateOf<CustomHealthCheck?>(null) }

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
            val ok      = checks.count { it.status == CheckStatus.OK }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip(overdue, "Attention",  ErrorRed,     Modifier.weight(1f))
                SummaryChip(dueSoon, "Due Soon",   WarnAmber,    Modifier.weight(1f))
                SummaryChip(ok,      "Good",       SuccessGreen, Modifier.weight(1f))
            }
        }

        items(checks, key = { it.check.checkType.name }) { ui ->
            HealthCheckCard(
                ui = ui,
                onEditInterval = { editIntervalFor = ui },
                onAction = {
                    when {
                        ui.check.checkType == HealthCheckType.ENGINE_OIL -> showOilSheet = ui
                        ui.isMaintenanceBacked -> showServiceSheet = ui
                        else -> viewModel.markDone(ui.check.carId, ui.check.checkType)
                    }
                }
            )
        }

        // Custom checks section
        item {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("MY CUSTOM CHECKS", color = NeonCyan, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(NeonCyanGlow).border(1.dp, NeonCyanBorder, RoundedCornerShape(8.dp))
                        .clickable { showAddCustomSheet = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Add, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Text("Add Check", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (customChecks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(20.dp), Alignment.Center) {
                    Text("Add custom checks for things specific to your car — timing belt, AC, etc.",
                        color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        val nowMs = System.currentTimeMillis()
        items(customChecks, key = { "custom_${it.id}" }) { check ->
            CustomCheckCard(
                check = check,
                nowMs = nowMs,
                onMarkDone = { viewModel.markCustomDone(check) },
                onDelete = { deleteCustomTarget = check }
            )
        }
    }

    // Oil Change sheet (ENGINE_OIL — specialized)
    showOilSheet?.let { ui ->
        OilChangeSheet(
            onDismiss = { showOilSheet = null },
            onSave = { brand, spec, liters, cost ->
                viewModel.logOilChange(ui.check.carId, brand, spec, liters, cost)
                showOilSheet = null
            }
        )
    }

    // Generic service sheet (BRAKE_FLUID, BATTERY, AIR_FILTER, WIPER_BLADES)
    showServiceSheet?.let { ui ->
        ServiceSheet(
            type = ui.check.checkType,
            onDismiss = { showServiceSheet = null },
            onSave = { brand, cost, notes ->
                viewModel.logServiceEntry(ui.check.carId, ui.check.checkType, brand, cost, notes)
                showServiceSheet = null
            }
        )
    }

    // Edit Interval sheet
    editIntervalFor?.let { ui ->
        EditIntervalSheet(
            check = ui.check,
            onDismiss = { editIntervalFor = null },
            onSave = { days, km ->
                viewModel.updateInterval(ui.check, days, km)
                editIntervalFor = null
            }
        )
    }

    // Add custom check sheet
    if (showAddCustomSheet && carId != null) {
        AddCustomCheckSheet(
            onDismiss = { showAddCustomSheet = false },
            onSave = { name, desc, days, km ->
                viewModel.addCustomCheck(carId, name, desc, days, km)
                showAddCustomSheet = false
            }
        )
    }

    // Delete custom check confirmation
    deleteCustomTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCustomTarget = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Remove '${target.name}'?", color = OnSurfacePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This custom check will be permanently deleted.", color = OnSurfaceSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCustomCheck(target); deleteCustomTarget = null }) {
                    Text("Remove", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { deleteCustomTarget = null }) { Text("Cancel", color = OnSurfaceSecondary) } }
        )
    }
}

// ─── Custom Check Card ───────────────────────────────────────────────────────

@Composable
private fun CustomCheckCard(
    check: CustomHealthCheck,
    nowMs: Long,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    val daysSince = check.lastCheckedAt?.let { (nowMs - it) / (1000L * 60 * 60 * 24) }
    val daysLeft = if (daysSince != null) check.intervalDays - daysSince else -check.intervalDays.toLong()
    val status = when {
        daysSince == null -> CheckStatus.NEVER_DONE
        daysLeft < 0 -> CheckStatus.OVERDUE
        daysLeft <= 7 -> CheckStatus.DUE_SOON
        else -> CheckStatus.OK
    }
    val accent = when (status) {
        CheckStatus.NEVER_DONE, CheckStatus.OVERDUE -> ErrorRed
        CheckStatus.DUE_SOON -> WarnAmber
        CheckStatus.OK -> SuccessGreen
    }
    val progress = if (daysSince != null) (daysSince.toFloat() / check.intervalDays).coerceIn(0f, 1f) else 1f

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer).border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.13f)), Alignment.Center) {
                        Icon(Icons.Filled.CheckCircle, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(check.name, color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        if (check.description.isNotBlank()) {
                            Text(check.description, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall, lineHeight = 14.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.13f))
                            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            when (status) {
                                CheckStatus.NEVER_DONE -> "Never done"
                                CheckStatus.OVERDUE -> "${(-daysLeft)}d overdue"
                                CheckStatus.DUE_SOON -> if (daysLeft == 0L) "Due today" else "Due in ${daysLeft}d"
                                CheckStatus.OK -> "Due in ${daysLeft}d"
                            },
                            color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, "Delete", tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = accent, trackColor = accent.copy(alpha = 0.12f)
            )
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    when {
                        daysSince == null -> "Never checked"
                        daysSince == 0L -> "Today"
                        else -> "${daysSince}d ago"
                    },
                    color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "Every ${check.intervalDays}d" + (check.intervalKm?.let { " / %,d km".format(it) } ?: ""),
                    color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall
                )
            }
            Button(
                onClick = onMarkDone,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.15f), contentColor = accent),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mark Done Today", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// ─── Add Custom Check Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomCheckSheet(onDismiss: () -> Unit, onSave: (String, String, Int, Int?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var intervalDays by remember { mutableStateOf("30") }
    var intervalKm by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceContainerHighest)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Add Custom Check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Check Name (e.g. Timing Belt)") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())
            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = intervalDays, onValueChange = { intervalDays = it },
                    label = { Text("Check every (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = intervalKm, onValueChange = { intervalKm = it },
                    label = { Text("Or every (km)") },
                    placeholder = { Text("optional", color = OnSurfaceSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val days = intervalDays.toIntOrNull()?.coerceAtLeast(1) ?: 30
                    onSave(name.trim(), description.trim(), days, intervalKm.toIntOrNull())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Add Check", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

// ─── Summary Chip ────────────────────────────────────────────────────────────

@Composable
private fun SummaryChip(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
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
private fun HealthCheckCard(ui: HealthCheckUi, onEditInterval: () -> Unit, onAction: () -> Unit) {
    val check = ui.check
    val accent = when (ui.status) {
        CheckStatus.NEVER_DONE, CheckStatus.OVERDUE -> ErrorRed
        CheckStatus.DUE_SOON                        -> WarnAmber
        CheckStatus.OK                              -> SuccessGreen
    }
    val icon = checkIcons[check.checkType] ?: Icons.Filled.CheckCircle

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Header
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.13f)), Alignment.Center) {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(check.checkType.displayName, color = OnSurfacePrimary,
                            fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(check.checkType.description, color = OnSurfaceSecondary,
                            style = MaterialTheme.typography.labelSmall, lineHeight = 14.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Edit interval button
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerHigh).clickable(onClick = onEditInterval),
                        Alignment.Center
                    ) {
                        Icon(Icons.Filled.Tune, "Edit interval", tint = OnSurfaceSecondary, modifier = Modifier.size(14.dp))
                    }
                    // Status badge
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.13f))
                            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(statusLabel(ui), color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Progress bar + labels
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { ui.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = accent, trackColor = accent.copy(alpha = 0.12f)
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(lastCheckedLabel(ui), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(intervalLabel(check), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                }
                // Next service km target
                if (ui.nextServiceKm != null) {
                    Text(
                        "Next at %,.0f km".format(ui.nextServiceKm),
                        color = accent.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Last service notes
            if (!ui.lastServiceNotes.isNullOrBlank()) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.07f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Opacity, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                        Text(ui.lastServiceNotes, color = accent.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Action button
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.15f), contentColor = accent),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(if (ui.isMaintenanceBacked) Icons.Filled.Opacity else Icons.Filled.Check,
                    null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (ui.isMaintenanceBacked) check.checkType.serviceLabel else "Mark Done Today",
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// ─── Oil Change Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OilChangeSheet(onDismiss: () -> Unit, onSave: (String, String, Double?, Double?) -> Unit) {
    var brand  by remember { mutableStateOf("") }
    var spec   by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var cost   by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceContainerHighest)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Log Oil Change", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
            InfoBanner("Also saves to Service log automatically")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") },
                    placeholder = { Text("e.g. Shell Helix", color = OnSurfaceSecondary) },
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Viscosity") },
                    placeholder = { Text("e.g. 5W-30", color = OnSurfaceSecondary) },
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = liters, onValueChange = { liters = it }, label = { Text("Liters") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost (BD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }
            Button(onClick = { onSave(brand.trim(), spec.trim(), liters.toDoubleOrNull(), cost.toDoubleOrNull()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)) {
                Text("Save Oil Change", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ─── Generic Service Sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceSheet(type: HealthCheckType, onDismiss: () -> Unit, onSave: (String, Double?, String) -> Unit) {
    var brand by remember { mutableStateOf("") }
    var cost  by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceContainerHighest)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(type.serviceLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
            InfoBanner("Also saves to Service log automatically")
            OutlinedTextField(value = brand, onValueChange = { brand = it },
                label = { Text("Brand / Part") },
                placeholder = { Text("e.g. Bosch, OEM…", color = OnSurfaceSecondary) },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost (BD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") },
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }
            Button(onClick = { onSave(brand.trim(), cost.toDoubleOrNull(), notes.trim()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)) {
                Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ─── Edit Interval Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditIntervalSheet(check: HealthCheck, onDismiss: () -> Unit, onSave: (Int, Int?) -> Unit) {
    var days by remember { mutableStateOf(check.intervalDays.toString()) }
    var km   by remember { mutableStateOf(check.intervalKm?.toString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceContainer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceContainerHighest)) }
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Edit Interval · ${check.checkType.displayName}",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("Every (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
                OutlinedTextField(value = km, onValueChange = { km = it }, label = { Text("Every (km)") },
                    placeholder = { Text("optional", color = OnSurfaceSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), colors = sheetFieldColors())
            }
            Text("Whichever trigger fires first will flag this check.",
                color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
            Button(
                onClick = {
                    val d = days.toIntOrNull() ?: return@Button
                    onSave(d, km.toIntOrNull())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Save Interval", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

// ─── Shared banner ───────────────────────────────────────────────────────────

@Composable
private fun InfoBanner(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(NeonCyanGlow).border(1.dp, NeonCyanBorder, RoundedCornerShape(10.dp))
        .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, null, tint = NeonCyan, modifier = Modifier.size(14.dp))
            Text(text, color = NeonCyan, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun statusLabel(ui: HealthCheckUi): String = when (ui.status) {
    CheckStatus.NEVER_DONE -> "Never done"
    CheckStatus.OVERDUE    -> ui.kmUntilDue?.let { if (it < 0) "${(-it).toInt()} km over" else null }
        ?: "${abs(ui.daysUntilDue)}d overdue"
    CheckStatus.DUE_SOON   -> ui.kmUntilDue?.let { if (it in 0.0..500.0) "${it.toInt()} km left" else null }
        ?: if (ui.daysUntilDue == 0L) "Due today" else "Due in ${ui.daysUntilDue}d"
    CheckStatus.OK         -> ui.kmUntilDue?.let { "${it.toInt()} km left" }
        ?: "Due in ${ui.daysUntilDue}d"
}

private fun lastCheckedLabel(ui: HealthCheckUi): String {
    val daysLabel = when {
        ui.daysSinceLast == null -> "Never done"
        ui.daysSinceLast == 0L   -> "Today"
        ui.daysSinceLast == 1L   -> "Yesterday"
        else                     -> "${ui.daysSinceLast}d ago"
    }
    return ui.kmSinceLast?.let { String.format(Locale.US, "%,.0f km · $daysLabel", it) } ?: daysLabel
}

private fun intervalLabel(check: HealthCheck): String {
    val timeLabel = when {
        check.intervalDays < 14  -> "${check.intervalDays} days"
        check.intervalDays < 60  -> "${check.intervalDays / 7} weeks"
        check.intervalDays < 365 -> "${check.intervalDays / 30} months"
        else                     -> "${check.intervalDays / 365} yr"
    }
    return if (check.intervalKm != null)
        "Every ${String.format(Locale.US, "%,d", check.intervalKm)} km"
    else
        "Every $timeLabel"
}
