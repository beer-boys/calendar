package ru.itmo.dws.calendar.core.domain.valueobject

enum class ConflictResolutionStrategy {
    CREATOR_SCHEDULE_PRIORITY,
    MINIMIZE_CHANGES,
    FLEXIBILITY_FIRST,
    EARLIEST_AVAILABLE,
    FAIR_DISTRIBUTION,
    MANUAL
}
