package com.cartracker.ui.screens.healthchecks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cartracker.data.db.entities.HealthCheckType
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.CheckStatus
import com.cartracker.ui.viewmodel.HealthCheckUi
import com.cartracker.ui.viewmodel.HealthChecksViewModel
import com.cartracker.ui.viewmodel.HealthChecksViewModelFactory
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

@Composable
fun HealthChecksScreen(carId: Long?) {
    val context = LocalContext.current
    val viewModel: HealthChecksViewModel = viewModel(
        factory = HealthChecksViewModelFactory(context.applicationContext as android.app.Application)
    )
    val checks by viewModel.checks.observeAsState(emptyList())

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
        // Summary row
        item {
            val overdue = checks.count { it.status == CheckStatus.NEVER_DONE || it.status == CheckStatus.OVERDUE }
            val dueSoon = checks.count { it.status == CheckStatus.DUE_SOON }
            val ok = checks.count { it.status == CheckStatus.OK }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(count = overdue, label = "Attention", color = ErrorRed, modifier = Modifier.weight(1f))
                SummaryChip(count = dueSoon,  label = "Due Soon",  color = WarnAmber, modifier = Modifier.weight(1f))
                SummaryChip(count = ok,       label = "Good",      color = SuccessGreen, modifier = Modifier.weight(1f))
            }
        }

        items(checks, key = { it.check.checkType.name }) { ui ->
            HealthCheckCard(
                ui = ui,
                onMarkDone = { viewModel.markDone(ui.check.carId, ui.check.checkType) }
            )
        }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.13f)),
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
                // Status badge
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
                    Text("Every ${intervalLabel(check.intervalDays)}", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Mark Done button (always visible — lets you re-confirm too)
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
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mark Done Today", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun statusLabel(ui: HealthCheckUi): String = when (ui.status) {
    CheckStatus.NEVER_DONE -> "Never done"
    CheckStatus.OVERDUE    -> "${abs(ui.daysUntilDue)}d overdue"
    CheckStatus.DUE_SOON   -> if (ui.daysUntilDue == 0L) "Due today" else "Due in ${ui.daysUntilDue}d"
    CheckStatus.OK         -> "Due in ${ui.daysUntilDue}d"
}

private fun lastCheckedLabel(ui: HealthCheckUi): String = when {
    ui.daysSinceLast == null -> "Never checked"
    ui.daysSinceLast == 0L   -> "Checked today"
    ui.daysSinceLast == 1L   -> "Checked yesterday"
    else                     -> "Checked ${ui.daysSinceLast}d ago"
}

private fun intervalLabel(days: Int): String = when {
    days < 14  -> "$days days"
    days < 60  -> "${days / 7} weeks"
    days < 365 -> "${days / 30} months"
    else       -> "${days / 365} year"
}
