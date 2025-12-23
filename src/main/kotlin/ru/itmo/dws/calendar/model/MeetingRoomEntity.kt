package ru.itmo.dws.calendar.model

import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import ru.itmo.dws.calendar.model.types.JsonbString

@Table("meeting_rooms")
data class MeetingRoomEntity(
    @Id
    val id: UUID,
    val name: String,
    val capacity: Int,
    val status: String,

    val address: String?,
    val building: String?,
    val floor: Int?,
    val wing: String?,

    @Column("room_number")
    val roomNumber: String?,

    val city: String?,

    @Column("time_zone")
    val timeZone: String?,

    @Column("features_json")
    val featuresJson: JsonbString,
)
