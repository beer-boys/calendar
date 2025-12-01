package ru.itmo.dws.calendar.core.domain.valueobject

@JvmInline
value class Priority(val value: Int) {
    init {
        require(value in MIN_VALUE..MAX_VALUE) {
            "Priority must be between $MIN_VALUE and $MAX_VALUE, got: $value"
        }
    }

    fun isHigherThan(other: Priority): Boolean = value > other.value

    fun isLowerThan(other: Priority): Boolean = value < other.value

    fun isEqualTo(other: Priority): Boolean = value == other.value

    companion object {
        const val MIN_VALUE = 0
        const val MAX_VALUE = 10

        val LOWEST = Priority(0)
        val LOW = Priority(2)
        val NORMAL = Priority(5)
        val HIGH = Priority(8)
        val HIGHEST = Priority(10)

        fun forHabit(): Priority = LOW
        fun forMeeting(): Priority = NORMAL
        fun forFocusTime(): Priority = HIGH
    }
}
