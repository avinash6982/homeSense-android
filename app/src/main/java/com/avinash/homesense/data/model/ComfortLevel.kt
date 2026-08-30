package com.avinash.homesense.data.model

enum class ComfortLevel(val label: String) {
    OPTIMAL("Optimal"),
    TOO_HOT("Too Hot"),
    TOO_COLD("Too Cold"),
    HIGH_HUMIDITY("High Humidity"),
    LOW_HUMIDITY("Low Humidity");

    companion object {
        private const val HOT_THRESHOLD_C = 27.0
        private const val COLD_THRESHOLD_C = 18.0
        private const val HIGH_HUMIDITY_THRESHOLD_PCT = 65.0
        private const val LOW_HUMIDITY_THRESHOLD_PCT = 30.0

        fun from(reading: Reading): ComfortLevel = when {
            reading.temperatureCelsius > HOT_THRESHOLD_C -> TOO_HOT
            reading.temperatureCelsius < COLD_THRESHOLD_C -> TOO_COLD
            reading.humidityPercent > HIGH_HUMIDITY_THRESHOLD_PCT -> HIGH_HUMIDITY
            reading.humidityPercent < LOW_HUMIDITY_THRESHOLD_PCT -> LOW_HUMIDITY
            else -> OPTIMAL
        }
    }
}
