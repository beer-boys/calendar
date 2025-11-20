package ru.itmo.dws.calendar.port.output

import ru.itmo.dws.calendar.domain.model.FocusTime
import ru.itmo.dws.calendar.domain.valueobject.FocusTimeId
import ru.itmo.dws.calendar.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.domain.valueobject.UserId

interface FocusTimeRepository {
    fun saveFocusTime(focusTime: FocusTime): FocusTimeId

    fun findFocusTime(focusTimeId: FocusTimeId): FocusTime?

    fun findFocusTimes(userId: UserId, timeRange: TimeSlot): List<FocusTime>

    fun deleteFocusTime(focusTimeId: FocusTimeId): Boolean
}
