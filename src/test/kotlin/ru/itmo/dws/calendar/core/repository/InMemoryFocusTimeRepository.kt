package ru.itmo.dws.calendar.core.repository

import java.util.concurrent.ConcurrentHashMap
import ru.itmo.dws.calendar.core.domain.model.FocusTime
import ru.itmo.dws.calendar.core.domain.valueobject.FocusTimeId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.FocusTimeRepository

open class InMemoryFocusTimeRepository : FocusTimeRepository {
    private val focusTimes = ConcurrentHashMap<FocusTimeId, FocusTime>()

    override fun saveFocusTime(focusTime: FocusTime): FocusTimeId {
        focusTimes[focusTime.id] = focusTime
        return focusTime.id
    }

    override fun findFocusTime(focusTimeId: FocusTimeId): FocusTime? = focusTimes[focusTimeId]

    override fun findFocusTimes(userId: UserId, timeRange: TimeSlot): List<FocusTime> =
        focusTimes.values.filter { focusTime ->
            focusTime.userId == userId &&
                focusTime.timeSlot.overlapsWith(timeRange)
        }

    override fun deleteFocusTime(focusTimeId: FocusTimeId): Boolean =
        focusTimes.remove(focusTimeId) != null
}
