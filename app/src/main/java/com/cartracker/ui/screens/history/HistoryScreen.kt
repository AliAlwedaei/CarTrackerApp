package com.cartracker.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cartracker.data.db.entities.Car
import com.cartracker.ui.components.CarPickerSheet
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.HistoryEvent
import com.cartracker.ui.viewmodel.HistoryViewModel
import com.cartracker.ui.viewmodel.HistoryViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(carId: Long?, cars: List<Car> = emptyList(), onCarSelected: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(context.applicationContext as android.app.Application)
    )
    val events by viewModel.events.observeAsState(emptyList())
    val selectedCar = cars.firstOrNull { it.id == carId }
    var showCarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(carId) { carId?.let { viewModel.setCarId(it) } }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("History", fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
                        if (selectedCar != null)
                            Text(selectedCar.name, color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    if (cars.size > 1) {
                        IconButton(onClick = { showCarPicker = true }) {
                            Icon(Icons.Filled.SwapHoriz, "Switch car", tint = NeonCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer)
            )
        }
    ) { padding ->
        if (carId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Select a car to view history", color = OnSurfaceSecondary)
            }
            return@Scaffold
        }

        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.History, null, Modifier.size(40.dp), tint = OnSurfaceSecondary)
                    Text("No history yet", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold)
                    Text("Fuel logs and service records will appear here", color = OnSurfaceSecondary, fontSize = 13.sp)
                }
            }
            return@Scaffold
        }

        // Group by month
        val sdfMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
        val grouped = remember(events) {
            events.groupBy { sdfMonth.format(Date(it.date)) }
                .entries.toList()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            grouped.forEach { (month, monthEvents) ->
                item(key = month) {
                    Text(month.uppercase(), color = NeonCyan, fontSize = 10.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                }
                items(monthEvents, key = { it.hashCode() }) { event ->
                    HistoryEventRow(event)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showCarPicker && cars.size > 1) {
        CarPickerSheet(cars = cars, selectedCarId = carId,
            onSelect = { onCarSelected(it); showCarPicker = false },
            onDismiss = { showCarPicker = false })
    }
}

@Composable
private fun HistoryEventRow(event: HistoryEvent) {
    val (icon, accent, title, subtitle, amount) = when (event) {
        is HistoryEvent.Fuel -> EventDisplay(
            icon   = Icons.Filled.LocalGasStation,
            accent = NeonCyan,
            title  = "Fill-up — %.1f L".format(event.log.liters),
            sub    = "%.0f km".format(event.log.odometer) +
                (if (event.log.fuelEfficiency > 0) " · %.1f km/L".format(event.log.fuelEfficiency) else ""),
            amount = "BD %.3f".format(event.log.totalCost)
        )
        is HistoryEvent.Service -> EventDisplay(
            icon   = Icons.Filled.Build,
            accent = WarnAmber,
            title  = event.log.serviceType,
            sub    = event.log.category.displayName +
                (if (event.log.notes.isNotBlank()) " · ${event.log.notes}" else ""),
            amount = if (event.log.cost > 0) "BD %.3f".format(event.log.cost) else ""
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date bubble
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            val cal = Calendar.getInstance().apply { timeInMillis = event.date }
            Text("%02d".format(cal.get(Calendar.DAY_OF_MONTH)),
                color = OnSurfacePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 18.sp)
            Text(SimpleDateFormat("MMM", Locale.getDefault()).format(Date(event.date)),
                color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
        }

        Box(Modifier.width(1.dp).height(36.dp).background(GlassBorder))

        // Icon
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.13f)),
            Alignment.Center
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }

        // Text
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall)
            if (subtitle.isNotBlank())
                Text(subtitle, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall,
                    maxLines = 1)
        }

        // Amount
        if (amount.isNotBlank())
            Text(amount, color = accent, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall)
    }
}

private data class EventDisplay(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: androidx.compose.ui.graphics.Color,
    val title: String,
    val sub: String,
    val amount: String
)
