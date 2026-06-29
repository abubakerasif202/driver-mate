package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DriverViewModel

enum class AppScreen {
    DASHBOARD, CALCULATOR, TRIPS, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DriverViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { currentScreen = AppScreen.DASHBOARD },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                modifier = Modifier.testTag("nav_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.CALCULATOR,
                                onClick = { currentScreen = AppScreen.CALCULATOR },
                                icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator") },
                                label = { Text("Evaluator") },
                                modifier = Modifier.testTag("nav_calculator")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.TRIPS,
                                onClick = { currentScreen = AppScreen.TRIPS },
                                icon = { Icon(Icons.Default.History, contentDescription = "Trips") },
                                label = { Text("Logs") },
                                modifier = Modifier.testTag("nav_trips")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SETTINGS,
                                onClick = { currentScreen = AppScreen.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToCalculator = { currentScreen = AppScreen.CALCULATOR }
                            )
                            AppScreen.CALCULATOR -> CalculatorScreen(
                                viewModel = viewModel,
                                onTripSaved = {
                                    // Successfully saved trip! Take driver straight to their logs to view it
                                    currentScreen = AppScreen.TRIPS
                                }
                            )
                            AppScreen.TRIPS -> TripsScreen(
                                viewModel = viewModel
                            )
                            AppScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
