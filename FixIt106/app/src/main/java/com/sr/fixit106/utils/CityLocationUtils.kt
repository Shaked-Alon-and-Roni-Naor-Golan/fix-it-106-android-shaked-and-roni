package com.sr.fixit106.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.util.Locale

object CityLocationUtils {

    private val israelLocale = Locale("en", "IL")

    @Suppress("DEPRECATION")
    fun getCityNameFromCoordinates(
        context: Context,
        lat: Double,
        lng: Double
    ): String {
        return try {
            val geocoder = Geocoder(context, israelLocale)
            val results = geocoder.getFromLocation(lat, lng, 5).orEmpty()

            val best = results.firstOrNull { it.countryCode.equals("IL", ignoreCase = true) }
                ?: results.firstOrNull()

            best?.locality
                ?: best?.subAdminArea
                ?: best?.adminArea
                ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    @Suppress("DEPRECATION")
    fun getCoordinatesForCity(
        context: Context,
        city: String
    ): Pair<Double, Double>? {
        if (city.isBlank()) return null

        val normalizedCity = city.trim()
        val queries = listOf(
            "$normalizedCity, Israel",
            "$normalizedCity, ישראל",
            normalizedCity
        )

        return try {
            val geocoder = Geocoder(context, israelLocale)

            for (query in queries) {
                val results = geocoder.getFromLocationName(query, 5).orEmpty()
                val best = pickBestIsraeliAddress(results, normalizedCity)
                if (best != null) {
                    return best.latitude to best.longitude
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun pickBestIsraeliAddress(
        results: List<Address>,
        requestedCity: String
    ): Address? {
        if (results.isEmpty()) return null

        val requested = requestedCity.trim().lowercase()

        return results.firstOrNull { address ->
            address.countryCode.equals("IL", ignoreCase = true) &&
                    (
                            address.locality?.trim()?.lowercase() == requested ||
                                    address.subAdminArea?.trim()?.lowercase() == requested ||
                                    address.adminArea?.trim()?.lowercase() == requested
                            )
        } ?: results.firstOrNull { it.countryCode.equals("IL", ignoreCase = true) }
        ?: results.firstOrNull()
    }
}