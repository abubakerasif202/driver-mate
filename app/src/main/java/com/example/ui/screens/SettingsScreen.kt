package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverRules
import com.example.service.OverlayService
import com.example.viewmodel.DriverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: DriverViewModel) {
    val context = LocalContext.current
    val rules by viewModel.currentRules.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // Form states
    var minEarningsPerKmStr by remember(rules) { mutableStateOf(rules.minEarningsPerKm.toString()) }
    var minFareStr by remember(rules) { mutableStateOf(rules.minFare.toString()) }
    var maxPickupDistanceStr by remember(rules) { mutableStateOf(rules.maxPickupDistance.toString()) }
    var minEarningsPerHourStr by remember(rules) { mutableStateOf(rules.minEarningsPerHour.toString()) }

    var isRulesModified by remember { mutableStateOf(false) }

    // Overlay active status
    var isOverlayEnabled by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    // Reset Confirmation Dialog
    var showResetConfirm by remember { mutableStateOf(false) }

    // Re-sync overlay status when returning to screen
    LaunchedEffect(Unit) {
        isOverlayEnabled = Settings.canDrawOverlays(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Rules", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Overlay Helper Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.8f)) {
                                Text(
                                    text = "Floating Assistant Bubble",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Show a draggable calculator bubble over other rideshare apps.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Switch(
                                checked = isOverlayEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (Settings.canDrawOverlays(context)) {
                                            isOverlayEnabled = true
                                            val intent = Intent(context, OverlayService::class.java)
                                            context.startService(intent)
                                            Toast.makeText(context, "Helper Bubble Started!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Request permission
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(intent)
                                            Toast.makeText(context, "Enable 'Display over other apps' permission", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        isOverlayEnabled = false
                                        context.stopService(Intent(context, OverlayService::class.java))
                                        Toast.makeText(context, "Helper Bubble Stopped", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("overlay_switch")
                            )
                        }
                    }
                }
            }

            // 2. Personal Rules Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Trip Valuation Thresholds",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "DriverMate evaluates incoming trips against these personal rules to give you a Good/Okay/Reject verdict.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Min $/Km Rule
                        OutlinedTextField(
                            value = minEarningsPerKmStr,
                            onValueChange = {
                                minEarningsPerKmStr = it
                                isRulesModified = true
                            },
                            label = { Text("Minimum rate per Km ($)") },
                            prefix = { Text("$") },
                            suffix = { Text("/km") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Min Fare Rule
                        OutlinedTextField(
                            value = minFareStr,
                            onValueChange = {
                                minFareStr = it
                                isRulesModified = true
                            },
                            label = { Text("Minimum trip base fare ($)") },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Max Pickup Dist Rule
                        OutlinedTextField(
                            value = maxPickupDistanceStr,
                            onValueChange = {
                                maxPickupDistanceStr = it
                                isRulesModified = true
                            },
                            label = { Text("Maximum pickup distance (km)") },
                            suffix = { Text("km") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Min Hourly Rate Rule
                        OutlinedTextField(
                            value = minEarningsPerHourStr,
                            onValueChange = {
                                minEarningsPerHourStr = it
                                isRulesModified = true
                            },
                            label = { Text("Minimum target hourly rate ($)") },
                            prefix = { Text("$") },
                            suffix = { Text("/hr") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isRulesModified) {
                            Button(
                                onClick = {
                                    val kmVal = minEarningsPerKmStr.toDoubleOrNull() ?: rules.minEarningsPerKm
                                    val fareVal = minFareStr.toDoubleOrNull() ?: rules.minFare
                                    val pickVal = maxPickupDistanceStr.toDoubleOrNull() ?: rules.maxPickupDistance
                                    val hrVal = minEarningsPerHourStr.toDoubleOrNull() ?: rules.minEarningsPerHour

                                    viewModel.updateRules(
                                        DriverRules(
                                            minEarningsPerKm = kmVal,
                                            minFare = fareVal,
                                            maxPickupDistance = pickVal,
                                            minEarningsPerHour = hrVal
                                        )
                                    )
                                    isRulesModified = false
                                    Toast.makeText(context, "Personal rules updated successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Apply Rule Thresholds", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Theme & Preferences
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Theme Scheme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Toggle application dark theme background.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.setDarkMode(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Standard Currency", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("App uses AUD for financial inputs and outputs.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                            Text(
                                text = "AUD ($)",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // 4. Data Tools (Export to CSV, Reset)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Data Operations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Export to CSV
                        Button(
                            onClick = {
                                val csvUri = viewModel.exportToCSV()
                                if (csvUri != null) {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_SUBJECT, "DriverMate Trips Export")
                                        putExtra(Intent.EXTRA_STREAM, csvUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Trip Log CSV")
                                    context.startActivity(shareIntent)
                                } else {
                                    Toast.makeText(context, "No saved trips found to export!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Trip History to CSV", fontWeight = FontWeight.Bold)
                        }

                        // Danger Reset
                        Button(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Reset Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset All Data & Defaults", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Reset Confirmation
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Confirm Total Reset") },
            text = { Text("Are you sure you want to delete all saved trip history logs, reset all driver thresholds to system defaults, and stop tracking? This action is permanent and cannot be reversed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetData()
                        showResetConfirm = false
                        Toast.makeText(context, "All data wiped, defaults restored.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
