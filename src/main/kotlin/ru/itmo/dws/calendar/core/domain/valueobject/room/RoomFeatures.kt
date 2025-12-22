package ru.itmo.dws.calendar.core.domain.valueobject.room

data class RoomFeatures(
    val features: Set<RoomFeature> = emptySet(),
    val attributes: Map<String, String> = emptyMap(),
) {
    fun has(feature: RoomFeature): Boolean = features.contains(feature)

    companion object {
        val EMPTY = RoomFeatures()
    }
}
