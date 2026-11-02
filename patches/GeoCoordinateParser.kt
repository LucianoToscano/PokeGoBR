package br.com.pokegobr

import java.net.URLDecoder

internal data class GeoPoint(val latitude: Double, val longitude: Double) {
    fun asText(): String = latitude.toString() + "," + longitude.toString()
}

/**
 * Strict no-network coordinate parser. Supports direct pairs, geo: and Google
 * Maps text links; does NOT follow shortened links or read GPS/PGSharp state.
 */
internal object GeoCoordinateParser {
    private val coordinates = Regex(
        "(?:^|geo:|@|[?&](?:q|query|ll)=|coords?\\s*[:：]|coordenadas\\s*[:：])\\s*" +
            "([-+]?\\d{1,2}(?:\\.\\d{1,8})?)\\s*[,;]\\s*" +
            "([-+]?\\d{1,3}(?:\\.\\d{1,8})?)(?=$|[^\\d.])",
        RegexOption.IGNORE_CASE
    )
    fun parse(input: String): GeoPoint? {
        if (input.length !in 3..2000) return null
        val normalized = runCatching {
            URLDecoder.decode(input.trim(), "UTF-8")
        }.getOrDefault(input.trim())
        val match = coordinates.find(normalized) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        if (!lat.isFinite() || !lon.isFinite() ||
            lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return GeoPoint(lat, lon)
    }
}
