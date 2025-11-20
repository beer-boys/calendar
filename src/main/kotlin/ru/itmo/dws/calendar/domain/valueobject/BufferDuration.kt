package ru.itmo.dws.calendar.domain.valueobject

import java.time.Duration

data class BufferDuration(
    val before: Duration,
    val after: Duration
) {
    init {
        require(!before.isNegative) { "Buffer before cannot be negative" }
        require(!after.isNegative) { "Buffer after cannot be negative" }
    }

    fun total(): Duration = before.plus(after)

    fun hasBuffer(): Boolean = !before.isZero || !after.isZero

    companion object {
        val NONE = BufferDuration(Duration.ZERO, Duration.ZERO)

        fun symmetric(duration: Duration): BufferDuration {
            return BufferDuration(duration, duration)
        }

        fun before(duration: Duration): BufferDuration {
            return BufferDuration(duration, Duration.ZERO)
        }

        fun after(duration: Duration): BufferDuration {
            return BufferDuration(Duration.ZERO, duration)
        }
    }
}
