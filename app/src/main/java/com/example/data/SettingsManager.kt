package com.example.data

import android.content.Context
import android.content.SharedPreferences

data class DriverRules(
    val minEarningsPerKm: Double,
    val minFare: Double,
    val maxPickupDistance: Double,
    val minEarningsPerHour: Double
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("drivermate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MIN_KM_RATE = "min_earnings_per_km"
        private const val KEY_MIN_FARE = "min_fare"
        private const val KEY_MAX_PICKUP = "max_pickup_distance"
        private const val KEY_MIN_HOUR_RATE = "min_earnings_per_hour"
        private const val KEY_DARK_MODE = "dark_mode"
        
        // Defaults
        const val DEFAULT_MIN_KM_RATE = 1.20
        const val DEFAULT_MIN_FARE = 6.00
        const val DEFAULT_MAX_PICKUP = 5.0
        const val DEFAULT_MIN_HOUR_RATE = 25.0
    }

    fun getRules(): DriverRules {
        return DriverRules(
            minEarningsPerKm = prefs.getFloat(KEY_MIN_KM_RATE, DEFAULT_MIN_KM_RATE.toFloat()).toDouble(),
            minFare = prefs.getFloat(KEY_MIN_FARE, DEFAULT_MIN_FARE.toFloat()).toDouble(),
            maxPickupDistance = prefs.getFloat(KEY_MAX_PICKUP, DEFAULT_MAX_PICKUP.toFloat()).toDouble(),
            minEarningsPerHour = prefs.getFloat(KEY_MIN_HOUR_RATE, DEFAULT_MIN_HOUR_RATE.toFloat()).toDouble()
        )
    }

    fun saveRules(rules: DriverRules) {
        prefs.edit().apply {
            putFloat(KEY_MIN_KM_RATE, rules.minEarningsPerKm.toFloat())
            putFloat(KEY_MIN_FARE, rules.minFare.toFloat())
            putFloat(KEY_MAX_PICKUP, rules.maxPickupDistance.toFloat())
            putFloat(KEY_MIN_HOUR_RATE, rules.minEarningsPerHour.toFloat())
            apply()
        }
    }

    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
    
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
