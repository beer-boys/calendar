package ru.itmo.dws.calendar.repository.adapter

import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.core.domain.model.FocusTime
import ru.itmo.dws.calendar.core.domain.valueobject.FocusTimeId
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.port.output.FocusTimeRepository

@Component
class StubFocusTimeRepositoryAdapter : FocusTimeRepository {

    override fun saveFocusTime(focusTime: FocusTime): FocusTimeId {
        throw UnsupportedOperationException("FocusTime management not implemented in MVP")
    }

    override fun findFocusTime(focusTimeId: FocusTimeId): FocusTime? = null

    override fun findFocusTimes(userId: UserId, timeRange: TimeSlot): List<FocusTime> = emptyList()

    override fun deleteFocusTime(focusTimeId: FocusTimeId): Boolean {
        throw UnsupportedOperationException("FocusTime management not implemented in MVP")
    }
}
