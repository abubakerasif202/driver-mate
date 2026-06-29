package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.time.*
import java.util.*

enum class DateFilterRange {
    ALL, TODAY, WEEK, MONTH
}

class DriverViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = TripRepository(db.tripDao())
    private val settingsManager = SettingsManager(application)

    // Flow of all trips
    val allTrips: StateFlow<List<Trip>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States
    private val _currentRules = MutableStateFlow(settingsManager.getRules())
    val currentRules: StateFlow<DriverRules> = _currentRules.asStateFlow()

    private val _isDarkMode = MutableStateFlow(settingsManager.isDarkModeEnabled())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Filters for Log screen
    private val _filterPlatform = MutableStateFlow<String?>(null)
    val filterPlatform: StateFlow<String?> = _filterPlatform.asStateFlow()

    private val _filterDateRange = MutableStateFlow(DateFilterRange.ALL)
    val filterDateRange: StateFlow<DateFilterRange> = _filterDateRange.asStateFlow()

    // Filtered Trips Flow
    val filteredTrips: StateFlow<List<Trip>> = combine(
        allTrips,
        _filterPlatform,
        _filterDateRange
    ) { trips, platform, dateRange ->
        trips.filter { trip ->
            val matchesPlatform = platform == null || trip.platform.equals(platform, ignoreCase = true)
            val matchesDate = when (dateRange) {
                DateFilterRange.ALL -> true
                DateFilterRange.TODAY -> isToday(trip.timestamp)
                DateFilterRange.WEEK -> isThisWeek(trip.timestamp)
                DateFilterRange.MONTH -> isThisMonth(trip.timestamp)
            }
            matchesPlatform && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shift/Online Timer State
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _onlineSeconds = MutableStateFlow(0L)
    val onlineSeconds: StateFlow<Long> = _onlineSeconds.asStateFlow()
    private var timerJob: Job? = null

    init {
        // Load persisted online time/status from shared preferences if desired,
        // or just keep it in memory for the shift. Let's load today's online time.
        val sharedPrefs = application.getSharedPreferences("drivermate_shift", Context.MODE_PRIVATE)
        _onlineSeconds.value = sharedPrefs.getLong("online_seconds_today", 0L)
        
        // Also verify and reset the timer if it's a new day
        val lastSavedDay = sharedPrefs.getLong("last_saved_day", 0L)
        val todayEpochDay = LocalDate.now().toEpochDay()
        if (todayEpochDay != lastSavedDay) {
            _onlineSeconds.value = 0L
            sharedPrefs.edit().putLong("last_saved_day", todayEpochDay).putLong("online_seconds_today", 0L).apply()
        }
    }

    fun toggleOnline() {
        if (_isOnline.value) {
            // Stop timer
            _isOnline.value = false
            timerJob?.cancel()
            saveOnlineTime()
        } else {
            // Start timer
            _isOnline.value = true
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _onlineSeconds.value += 1
                    if (_onlineSeconds.value % 10 == 0L) { // Save every 10 seconds
                        saveOnlineTime()
                    }
                }
            }
        }
    }

    private fun saveOnlineTime() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("drivermate_shift", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putLong("online_seconds_today", _onlineSeconds.value)
            .putLong("last_saved_day", LocalDate.now().toEpochDay())
            .apply()
    }

    fun adjustOnlineTime(seconds: Long) {
        _onlineSeconds.value = (_onlineSeconds.value + seconds).coerceAtLeast(0L)
        saveOnlineTime()
    }

    fun resetOnlineTime() {
        _onlineSeconds.value = 0L
        saveOnlineTime()
    }

    // Dashboard Statistics (Calculated reactively from All Trips & Timer)
    val todayTrips: StateFlow<List<Trip>> = allTrips.map { trips ->
        trips.filter { isToday(it.timestamp) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats = combine(todayTrips, _onlineSeconds) { trips, seconds ->
        val totalEarnings = trips.sumOf { it.fare }
        val tripsCount = trips.size
        val totalDistance = trips.sumOf { it.totalDistanceKm }
        val activeDrivingMinutes = trips.sumOf { it.durationMinutes }

        val hours = seconds / 3600.0
        val avgPerHour = if (hours > 0) totalEarnings / hours else 0.0
        val avgPerKm = if (totalDistance > 0) totalEarnings / totalDistance else 0.0

        DashboardStats(
            todayEarnings = totalEarnings,
            tripCount = tripsCount,
            avgEarningsPerHour = avgPerHour,
            avgEarningsPerKm = avgPerKm,
            onlineTimeSeconds = seconds,
            totalDistance = totalDistance,
            activeDrivingMinutes = activeDrivingMinutes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Calculations & Evaluations Helpers
    fun evaluateTrip(
        pickupDistance: Double,
        dropoffDistance: Double,
        fare: Double,
        durationMinutes: Double,
        rules: DriverRules = _currentRules.value
    ): TripEvaluation {
        val totalDistance = pickupDistance + dropoffDistance
        val earningsPerKm = if (totalDistance > 0) fare / totalDistance else 0.0
        val earningsPerHour = if (durationMinutes > 0) (fare / durationMinutes) * 60.0 else 0.0

        // Rule Checks
        val passKmRate = earningsPerKm >= rules.minEarningsPerKm
        val passFare = fare >= rules.minFare
        val passPickup = pickupDistance <= rules.maxPickupDistance
        val passHourRate = earningsPerHour >= rules.minEarningsPerHour

        val totalPassed = (if (passKmRate) 1 else 0) +
                (if (passFare) 1 else 0) +
                (if (passPickup) 1 else 0) +
                (if (passHourRate) 1 else 0)

        val verdict = when {
            // Critical failures or less than 2 rules passed -> REJECT
            !passKmRate || totalPassed <= 1 -> EvaluationVerdict.REJECT
            // All passed -> GOOD
            totalPassed == 4 -> EvaluationVerdict.GOOD
            // 2-3 passed -> OKAY
            else -> EvaluationVerdict.OKAY
        }

        return TripEvaluation(
            earningsPerKm = earningsPerKm,
            earningsPerHour = earningsPerHour,
            verdict = verdict,
            passKmRate = passKmRate,
            passFare = passFare,
            passPickup = passPickup,
            passHourRate = passHourRate
        )
    }

    // CRUD operations
    fun saveTrip(
        platform: String,
        pickupDistance: Double,
        dropoffDistance: Double,
        fare: Double,
        durationMinutes: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val trip = Trip(
                timestamp = System.currentTimeMillis(),
                platform = platform,
                pickupDistanceKm = pickupDistance,
                dropoffDistanceKm = dropoffDistance,
                fare = fare,
                durationMinutes = durationMinutes,
                notes = notes
            )
            repository.insert(trip)
        }
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch {
            repository.update(trip)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.delete(trip)
        }
    }

    // Filters updates
    fun setFilterPlatform(platform: String?) {
        _filterPlatform.value = platform
    }

    fun setFilterDateRange(range: DateFilterRange) {
        _filterDateRange.value = range
    }

    // Settings modifications
    fun updateRules(newRules: DriverRules) {
        settingsManager.saveRules(newRules)
        _currentRules.value = newRules
    }

    fun setDarkMode(enabled: Boolean) {
        settingsManager.setDarkModeEnabled(enabled)
        _isDarkMode.value = enabled
    }

    fun resetData() {
        viewModelScope.launch {
            repository.clearAll()
            settingsManager.resetToDefaults()
            _currentRules.value = settingsManager.getRules()
            _isDarkMode.value = settingsManager.isDarkModeEnabled()
            resetOnlineTime()
        }
    }

    // CSV Export
    fun exportToCSV(): Uri? {
        val tripsList = allTrips.value
        val context = getApplication<Application>()
        if (tripsList.isEmpty()) return null

        try {
            val folder = File(context.cacheDir, "exports")
            if (!folder.exists()) folder.mkdirs()

            val file = File(folder, "DriverMate_Trips_Export_${System.currentTimeMillis()}.csv")
            val writer = FileWriter(file)

            // Header
            writer.append("ID,Date,Platform,Pickup Distance (km),Dropoff Distance (km),Total Distance (km),Fare (AUD),Duration (mins),Earnings Per Km,Earnings Per Hour,Notes\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            for (trip in tripsList) {
                val formattedDate = dateFormat.format(Date(trip.timestamp))
                writer.append("${trip.id},")
                writer.append("$formattedDate,")
                writer.append("${trip.platform.replace(",", " ")},")
                writer.append("${trip.pickupDistanceKm},")
                writer.append("${trip.dropoffDistanceKm},")
                writer.append("${trip.totalDistanceKm},")
                writer.append("${trip.fare},")
                writer.append("${trip.durationMinutes},")
                writer.append("${String.format(Locale.US, "%.2f", trip.earningsPerKm)},")
                writer.append("${String.format(Locale.US, "%.2f", trip.earningsPerHour)},")
                writer.append("${trip.notes.replace(",", " ").replace("\n", " ")}\n")
            }

            writer.flush()
            writer.close()

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Helper functions for date matching
    private fun isToday(timestamp: Long): Boolean {
        val instant = Instant.ofEpochMilli(timestamp)
        val tripDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return tripDate.isEqual(LocalDate.now())
    }

    private fun isThisWeek(timestamp: Long): Boolean {
        val instant = Instant.ofEpochMilli(timestamp)
        val tripDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val startOfWeek = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
        return !tripDate.isBefore(startOfWeek) && !tripDate.isAfter(LocalDate.now())
    }

    private fun isThisMonth(timestamp: Long): Boolean {
        val instant = Instant.ofEpochMilli(timestamp)
        val tripDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return tripDate.year == today.year && tripDate.monthValue == today.monthValue
    }
}

// Data holder classes
data class DashboardStats(
    val todayEarnings: Double = 0.0,
    val tripCount: Int = 0,
    val avgEarningsPerHour: Double = 0.0,
    val avgEarningsPerKm: Double = 0.0,
    val onlineTimeSeconds: Long = 0L,
    val totalDistance: Double = 0.0,
    val activeDrivingMinutes: Double = 0.0
)

enum class EvaluationVerdict {
    GOOD, OKAY, REJECT
}

data class TripEvaluation(
    val earningsPerKm: Double,
    val earningsPerHour: Double,
    val verdict: EvaluationVerdict,
    val passKmRate: Boolean,
    val passFare: Boolean,
    val passPickup: Boolean,
    val passHourRate: Boolean
)
