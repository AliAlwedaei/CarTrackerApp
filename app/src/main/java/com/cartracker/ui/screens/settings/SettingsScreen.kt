package com.cartracker.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.cartracker.R
import com.cartracker.ui.screens.fuellog.sheetFieldColors
import com.cartracker.ui.theme.*
import com.cartracker.util.BudgetPrefs
import com.cartracker.util.CurrencyPrefs

data class LangOption(val tag: String, val label: String, val labelNative: String)

private val languages = listOf(
    LangOption("en", "English", "English"),
    LangOption("ar", "Arabic",  "العربية"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onViewReports: () -> Unit = {}) {
    val context = LocalContext.current
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentTag = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language ?: "en"

    var currentCurrency by remember { mutableStateOf(CurrencyPrefs.getSymbol(context)) }
    var fuelBudgetText by remember { mutableStateOf(BudgetPrefs.getMonthlyFuelBudget(context)?.let { String.format(Locale.US, "%.3f", it) } ?: "") }
    var totalBudgetText by remember { mutableStateOf(BudgetPrefs.getMonthlyTotalBudget(context)?.let { String.format(Locale.US, "%.3f", it) } ?: "") }

    Scaffold(
        containerColor = TrueBlack,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings), fontWeight = FontWeight.Bold, color = OnSurfacePrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // ── Reports shortcut ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(NeonCyanGlow).border(1.dp, NeonCyanBorder, RoundedCornerShape(14.dp))
                    .clickable(onClick = onViewReports).padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.BarChart, null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Cost & Insights Report", color = NeonCyan, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text("Ownership cost, fuel trends, work mileage", color = NeonCyan.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
            }

            // ── Monthly Budget ────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MONTHLY BUDGET", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fuelBudgetText,
                        onValueChange = { fuelBudgetText = it },
                        label = { Text("Fuel budget ($currentCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = sheetFieldColors()
                    )
                    OutlinedTextField(
                        value = totalBudgetText,
                        onValueChange = { totalBudgetText = it },
                        label = { Text("Total budget ($currentCurrency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = sheetFieldColors()
                    )
                }
                Button(
                    onClick = {
                        BudgetPrefs.setMonthlyFuelBudget(context, fuelBudgetText.toDoubleOrNull())
                        BudgetPrefs.setMonthlyTotalBudget(context, totalBudgetText.toDoubleOrNull())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = TrueBlack),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save Budgets", fontWeight = FontWeight.SemiBold) }
                Text("Leave blank to disable budget tracking.", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
            }

            // ── Currency ──────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CURRENCY", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyPrefs.supported.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { symbol ->
                                val selected = currentCurrency == symbol
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) NeonCyanGlow else SurfaceContainer)
                                        .border(1.dp, if (selected) NeonCyanBorder else GlassBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            CurrencyPrefs.setSymbol(context, symbol)
                                            currentCurrency = symbol
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(symbol, color = if (selected) NeonCyan else OnSurfacePrimary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Text("Currency is used for display only. Changes take effect on next screen load.", color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall, lineHeight = 16.sp)
            }

            // ── Language ──────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_language).uppercase(), color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                languages.forEach { lang ->
                    val selected = currentTag == lang.tag
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) NeonCyanGlow else SurfaceContainer)
                            .border(1.dp, if (selected) NeonCyanBorder else GlassBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                if (!selected) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(lang.labelNative, color = if (selected) NeonCyan else OnSurfacePrimary,
                                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            if (lang.tag != "en") {
                                Text(lang.label, color = if (selected) NeonCyan.copy(alpha = 0.7f) else OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (selected) Icon(Icons.Filled.Check, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                }
                Text(stringResource(R.string.settings_numbers_note), color = OnSurfaceSecondary, style = MaterialTheme.typography.labelSmall, lineHeight = 16.sp)
            }
        }
    }
}
