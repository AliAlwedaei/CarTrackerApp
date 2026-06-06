package com.cartracker.ui.screens.expenses

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
import com.cartracker.data.db.entities.Car
import com.cartracker.data.db.entities.Expense
import com.cartracker.data.db.entities.ExpenseCategory
import com.cartracker.ui.components.CarPickerSheet
import com.cartracker.ui.screens.fuellog.DatePickerField
import com.cartracker.ui.screens.fuellog.sheetFieldColors
import com.cartracker.ui.theme.*
import com.cartracker.ui.viewmodel.ExpenseViewModel
import com.cartracker.ui.viewmodel.ExpenseViewModelFactory
import com.cartracker.util.CurrencyPrefs
import java.text.SimpleDateFormat
import java.util.*

private val expenseIcons: Map<ExpenseCategory, ImageVector> = mapOf(
    ExpenseCategory.INSURANCE    to Icons.Filled.Security,
    ExpenseCategory.REGISTRATION to Icons.Filled.Description,
    ExpenseCategory.PARKING      to Icons.Filled.LocalParking,
    ExpenseCategory.TOLL         to Icons.Filled.Money,
    ExpenseCategory.CAR_WASH     to Icons.Filled.LocalCarWash,
    ExpenseCategory.ACCESSORIES  to Icons.Filled.ShoppingCart,
    ExpenseCategory.LOAN_PAYMENT to Icons.Filled.CreditCard,
    ExpenseCategory.OTHER        to Icons.Filled.Receipt
)

private val expenseColors: Map<ExpenseCategory, Color> = mapOf(
    ExpenseCategory.INSURANCE    to Color(0xFF4CAF50),
    ExpenseCategory.REGISTRATION to Color(0xFF2196F3),
    ExpenseCategory.PARKING      to Color(0xFFFF9800),
    ExpenseCategory.TOLL         to Color(0xFFF44336),
    ExpenseCategory.CAR_WASH     to Color(0xFF00BCD4),
    ExpenseCategory.ACCESSORIES  to Color(0xFF9C27B0),
    ExpenseCategory.LOAN_PAYMENT to Color(0xFFFF5722),
    ExpenseCategory.OTHER        to Color(0xFF607D8B)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(carId: Long?, cars: List<Car> = emptyList(), onCarSelected: (Long) -> Unit = {}) {
    val context = LocalContext.current
    val currency = remember { CurrencyPrefs.getSymbol(context) }
    val viewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(context.applicationContext as android.app.Application)
    )
    val expenses by viewModel.expenses.observeAsState(emptyList())

    val selectedCar = cars.firstOrNull { it.id == carId }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Expense?>(null) }
    var showCarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(carId) { carId?.let { viewModel.setCarId(it) } }

    // Total for current month
    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val monthlyTotal = remember(expenses) { expenses.filter { it.date >= monthStart }.sumOf { it.amount } }
    val allTimeTotal = remember(expenses) { expenses.sumOf { it.amount } }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Expenses", fontWeight = FontWeight.Bold, color = OnSurfacePrimary)
                        if (selectedCar != null) {
                            Text(selectedCar.name, color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                actions = {
                    if (cars.size > 1) {
                        IconButton(onClick = { showCarPicker = true }) {
                            Icon(Icons.Filled.SwapHoriz, "Switch car", tint = NeonCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer, titleContentColor = OnSurfacePrimary)
            )
        },
        floatingActionButton = {
            if (carId != null) {
                FloatingActionButton(
                    onClick = { editingExpense = null; showSheet = true },
                    containerColor = NeonCyan, contentColor = TrueBlack, shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Filled.Add, "Add Expense") }
            }
        }
    ) { padding ->
        if (carId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Select a car to view expenses", color = OnSurfaceSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                // Summary header
                if (expenses.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SummaryCard("THIS MONTH", if (monthlyTotal > 0) "$currency %.3f".format(monthlyTotal) else "--", Modifier.weight(1f))
                            SummaryCard("ALL TIME", if (allTimeTotal > 0) "$currency %.3f".format(allTimeTotal) else "--", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (expenses.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.Receipt, null, Modifier.size(40.dp), tint = OnSurfaceSecondary)
                                Text("No expenses yet", color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold)
                                Text("Track insurance, parking, and other vehicle costs", color = OnSurfaceSecondary, fontSize = 13.sp)
                                Button(
                                    onClick = { showSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Add First Expense", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }

                items(expenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        currency = currency,
                        onEdit = { editingExpense = expense; showSheet = true },
                        onDelete = { deleteTarget = expense }
                    )
                }
            }
        }
    }

    if (showSheet && carId != null) {
        ExpenseSheet(
            existingExpense = editingExpense,
            currency = currency,
            onDismiss = { showSheet = false; editingExpense = null },
            onSave = { cat, desc, date, amount, isRecurring, recDays, notes ->
                if (editingExpense != null) viewModel.updateExpense(editingExpense!!, cat, desc, date, amount, isRecurring, recDays, notes)
                else viewModel.addExpense(carId, cat, desc, date, amount, isRecurring, recDays, notes)
                showSheet = false; editingExpense = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SurfaceContainerHigh,
            title = { Text("Delete expense?", color = OnSurfacePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this expense record.", color = OnSurfaceSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteExpense(target); deleteTarget = null }) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = OnSurfaceSecondary) } }
        )
    }

    if (showCarPicker && cars.size > 1) {
        CarPickerSheet(cars = cars, selectedCarId = carId, onSelect = { onCarSelected(it); showCarPicker = false }, onDismiss = { showCarPicker = false })
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(SurfaceContainer)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = OnSurfaceSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
            Text(value, color = OnSurfacePrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ExpenseCard(expense: Expense, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val icon = expenseIcons[expense.category] ?: Icons.Filled.Receipt
    val color = expenseColors[expense.category] ?: OnSurfaceSecondary

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer).border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.15f)),
                        Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(expense.description, color = OnSurfacePrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(expense.category.displayName, color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                            if (expense.isRecurring) {
                                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(NeonCyanGlow).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                    Text("Recurring", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$currency %.3f".format(expense.amount), color = OnSurfacePrimary,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, "Edit", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(sdf.format(Date(expense.date)), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
            if (expense.recurrenceDays != null) {
                Text("Repeats every ${expense.recurrenceDays} days", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
            }
            if (expense.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(expense.notes, color = OnSurfaceSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseSheet(
    existingExpense: Expense?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategory, String, Long, Double, Boolean, Int?, String) -> Unit
) {
    val isEdit = existingExpense != null
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var selectedCategory by remember { mutableStateOf(existingExpense?.category ?: ExpenseCategory.INSURANCE) }
    var description by remember { mutableStateOf(existingExpense?.description ?: "") }
    var dateMs by remember { mutableStateOf(existingExpense?.date ?: System.currentTimeMillis()) }
    var amount by remember { mutableStateOf(existingExpense?.amount?.let { String.format(Locale.US, "%.3f", it) } ?: "") }
    var isRecurring by remember { mutableStateOf(existingExpense?.isRecurring ?: false) }
    var recurrenceDays by remember { mutableStateOf(existingExpense?.recurrenceDays?.toString() ?: "") }
    var notes by remember { mutableStateOf(existingExpense?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory) {
        if (!isEdit && description.isBlank()) description = selectedCategory.displayName
    }

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
            Text(if (isEdit) "Edit Expense" else "Log Expense",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurfacePrimary)

            DatePickerField(dateMs = dateMs, sdf = sdf) { showDatePicker = true }

            // Category chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category", color = OnSurfaceSecondary, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseCategory.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { cat ->
                                val selected = selectedCategory == cat
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) NeonCyanGlow else SurfaceContainerHigh)
                                        .border(1.dp, if (selected) NeonCyanBorder else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedCategory = cat; if (!isEdit) description = cat.displayName }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.displayName, color = if (selected) NeonCyan else OnSurfaceSecondary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 2
                                    )
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            OutlinedTextField(value = amount, onValueChange = { amount = it },
                label = { Text("Amount ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            // Recurring toggle
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh).border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { isRecurring = !isRecurring }.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Recurring Expense", color = OnSurfacePrimary, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                    Text("e.g. monthly insurance, parking subscription", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = TrueBlack, checkedTrackColor = NeonCyan))
            }

            if (isRecurring) {
                OutlinedTextField(value = recurrenceDays, onValueChange = { recurrenceDays = it },
                    label = { Text("Repeat every (days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("30 = monthly, 365 = annually", color = OnSurfaceSecondary, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), colors = sheetFieldColors())

            Button(
                onClick = {
                    if (description.isBlank()) return@Button
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    val recDays = if (isRecurring) recurrenceDays.toIntOrNull() else null
                    onSave(selectedCategory, description, dateMs, amt, isRecurring, recDays, notes)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (isEdit) "Update" else "Save", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { dateMs = it }; showDatePicker = false }) {
                    Text("OK", color = NeonCyan, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = OnSurfaceSecondary) } }
        ) { DatePicker(state = state) }
    }
}
