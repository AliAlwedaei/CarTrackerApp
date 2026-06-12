package com.cartracker.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.cartracker.util.CurrencyPrefs
import java.text.SimpleDateFormat
import java.util.*

private val ExpensePurple = Color(0xFF9C6ADE)

private enum class HistoryFilter(val label: String, val color: Color) {
    ALL("All", Color.White),
    FUEL("Fuel", NeonCyan),
    SERVICE("Service", WarnAmber),
    EXPENSE("Expense", ExpensePurple)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(carId: Long?, cars: List<Car> = emptyList(), onCarSelected: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val currency = remember { CurrencyPrefs.getSymbol(context) }
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(context.applicationContext as android.app.Application)
    )
    val events by viewModel.events.observeAsState(emptyList())
    val selectedCar = cars.firstOrNull { it.id == carId }
    var showCarPicker by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    LaunchedEffect(carId) { carId?.let { viewModel.setCarId(it) } }

    val filteredEvents = remember(events, activeFilter) {
        when (activeFilter) {
            HistoryFilter.FUEL    -> events.filterIsInstance<HistoryEvent.Fuel>()
            HistoryFilter.SERVICE -> events.filterIsInstance<HistoryEvent.Service>()
            HistoryFilter.EXPENSE -> events.filterIsInstance<HistoryEvent.ExpenseEntry>()
            HistoryFilter.ALL     -> events
        }
    }

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

        Column(Modifier.fillMaxSize().padding(padding)) {
            // ── Filter chips ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { filter ->
                    val selected = activeFilter == filter
                    val chipColor = filter.color
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) chipColor.copy(alpha = 0.15f) else SurfaceContainerHigh)
                            .border(1.dp, if (selected) chipColor.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(20.dp))
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            filter.label,
                            color = if (selected) chipColor else OnSurfaceSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.History, null, Modifier.size(40.dp), tint = OnSurfaceSecondary)
                        Text("No history yet", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold)
                        Text("Fuel logs and service records will appear here", color = OnSurfaceSecondary, fontSize = 13.sp)
                    }
                }
                return@Column
            }

            if (filteredEvents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ${activeFilter.label.lowercase()} entries yet", color = OnSurfaceSecondary)
                }
                return@Column
            }

            val sdfMonth = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
            val grouped = remember(filteredEvents) {
                filteredEvents.groupBy { sdfMonth.format(Date(it.date)) }
                    .entries.toList()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(bottom = 14.dp)
            ) {
                grouped.forEach { (month, monthEvents) ->
                    item(key = month) {
                        Text(
                            month.uppercase(), color = NeonCyan, fontSize = 10.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    items(monthEvents, key = { "${it::class.simpleName}_${it.date}_${it.hashCode()}" }) { event ->
                        HistoryEventRow(event = event, currency = currency)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showCarPicker && cars.size > 1) {
        CarPickerSheet(
            cars = cars, selectedCarId = carId,
            onSelect = { onCarSelected(it); showCarPicker = false },
            onDismiss = { showCarPicker = false }
        )
    }
}

@Composable
private fun HistoryEventRow(event: HistoryEvent, currency: String) {
    val display = when (event) {
        is HistoryEvent.Fuel -> EventDisplay(
            icon   = Icons.Filled.LocalGasStation,
            accent = NeonCyan,
            title  = "Fill-up — %.1f L".format(event.log.liters),
            sub    = String.format(Locale.US, "%,.0f km", event.log.odometer) +
                (if (event.log.fuelEfficiency > 0) " · %.1f km/L".format(event.log.fuelEfficiency) else ""),
            amount = if (event.log.totalCost > 0) String.format(Locale.US, "$currency %,.3f", event.log.totalCost) else ""
        )
        is HistoryEvent.Service -> EventDisplay(
            icon   = Icons.Filled.Build,
            accent = WarnAmber,
            title  = event.log.serviceType,
            sub    = event.log.category.displayName +
                (if (event.log.notes.isNotBlank()) " · ${event.log.notes}" else ""),
            amount = if (event.log.cost > 0) String.format(Locale.US, "$currency %,.3f", event.log.cost) else ""
        )
        is HistoryEvent.ExpenseEntry -> EventDisplay(
            icon   = Icons.Filled.Receipt,
            accent = ExpensePurple,
            title  = event.log.description.ifBlank { event.log.category.displayName },
            sub    = event.log.category.displayName +
                (if (event.log.notes.isNotBlank()) " · ${event.log.notes}" else ""),
            amount = if (event.log.amount > 0) String.format(Locale.US, "$currency %,.3f", event.log.amount) else ""
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .border(1.dp, display.accent.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date bubble
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
            val cal = Calendar.getInstance().apply { timeInMillis = event.date }
            Text(
                String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH)),
                color = OnSurfacePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 18.sp
            )
            Text(
                SimpleDateFormat("MMM", Locale.getDefault()).format(Date(event.date)),
                color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall
            )
        }

        Box(Modifier.width(1.dp).height(36.dp).background(GlassBorder))

        // Icon
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(display.accent.copy(alpha = 0.13f)),
            Alignment.Center
        ) { Icon(display.icon, null, tint = display.accent, modifier = Modifier.size(18.dp)) }

        // Text
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                display.title, color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
            if (display.sub.isNotBlank())
                Text(
                    display.sub, color = OnSurfaceSecondary,
                    style = MaterialTheme.typography.labelSmall, maxLines = 1
                )
        }

        // Amount
        if (display.amount.isNotBlank())
            Text(
                display.amount, color = display.accent, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
    }
}

private data class EventDisplay(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val sub: String,
    val amount: String
)
