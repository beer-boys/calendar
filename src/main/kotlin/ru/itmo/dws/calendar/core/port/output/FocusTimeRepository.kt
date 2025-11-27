package ru.itmo.dws.calendar.core.port.output

import ru.itmo.dws.calendar.core.domain.model.FocusTime
import ru.itmo.dws.calendar.core.domain.valueobject.FocusTimeId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId

interface FocusTimeRepository {
    fun saveFocusTime(focusTime: FocusTime): FocusTimeId

    fun findFocusTime(focusTimeId: FocusTimeId): FocusTime?

    fun findFocusTimes(userId: UserId, timeRange: TimeSlot): List<FocusTime>

    fun deleteFocusTime(focusTimeId: FocusTimeId): Boolean
}
