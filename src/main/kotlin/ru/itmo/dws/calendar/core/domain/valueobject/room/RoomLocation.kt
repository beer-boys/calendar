package ru.itmo.dws.calendar.core.domain.valueobject.room

import java.time.ZoneId
import java.util.Locale

data class RoomLocation(
    val address: String? = null,
    val building: String? = null,
    val floor: Int? = null,
    val wing: String? = null,
    val roomNumber: String? = null,
    val city: String? = null,
    val timeZoneId: ZoneId? = null,
) {

    init {
        require(floor == null || floor in -10..300) { "Invalid floor: $floor" }
    }

    fun normalizedKey(): String =
        listOfNotNull(city, address, building, floor?.toString(), wing, roomNumber)
            .joinToString(separator = "|") { it.trim().lowercase(Locale.getDefault()) }
}
