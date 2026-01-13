package ru.itmo.dws.calendar.repository.extension

import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.stereotype.Component
import ru.itmo.dws.calendar.model.MeetingRoomBookingEntity

@Component
class MeetingRoomBookingRepositoryExtensionImpl(
    private val template: JdbcAggregateTemplate,
) : MeetingRoomBookingRepositoryExtension {

    override fun insert(booking: MeetingRoomBookingEntity): MeetingRoomBookingEntity {
        return template.insert(booking)
    }
}
