package ru.itmo.dws.calendar.domain.valueobject

enum class MeetingPriority(val level: Int) {
    CRITICAL(5),
    HIGH(4),
    NORMAL(3),
    LOW(2),
    FLEXIBLE(1);

    fun isHigherThan(other: MeetingPriority): Boolean {
        return this.level > other.level
    }

    fun isLowerThan(other: MeetingPriority): Boolean {
        return this.level < other.level
    }

    companion object {
        fun forHabit(): MeetingPriority = FLEXIBLE

        fun forMeeting(): MeetingPriority = NORMAL

        fun forFocusTime(): MeetingPriority = HIGH
    }
}
