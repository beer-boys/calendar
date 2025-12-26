@file:Suppress("MatchingDeclarationName")

package ru.itmo.dws.calendar.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.ZoneId
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom.MeetingRoomStatus
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking.BookingStatus
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomFeature
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomFeatures
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomLocation
import ru.itmo.dws.calendar.dto.TimeSlotDto
import ru.itmo.dws.calendar.dto.room.MeetingRoomResponseDto
import ru.itmo.dws.calendar.dto.room.MeetingRoomStatusDto
import ru.itmo.dws.calendar.dto.room.RoomLocationResponseDto
import ru.itmo.dws.calendar.dto.room.booking.BookingStatusDto
import ru.itmo.dws.calendar.dto.room.booking.RoomBookingResponseDto
import ru.itmo.dws.calendar.model.MeetingRoomBookingEntity
import ru.itmo.dws.calendar.model.MeetingRoomEntity
import ru.itmo.dws.calendar.model.types.JsonbString

data class RoomFeaturesJson(
    val features: Set<String> = emptySet(),
    val attributes: Map<String, String> = emptyMap()
)

fun MeetingRoomEntity.toDomain(objectMapper: ObjectMapper): MeetingRoom {
    val parsed = runCatching {
        objectMapper.readValue(featuresJson.value, RoomFeaturesJson::class.java)
    }.getOrDefault(RoomFeaturesJson())

    val features = parsed.features
        .mapNotNull { runCatching { RoomFeature.valueOf(it) }.getOrNull() }
        .toSet()

    return MeetingRoom(
        id = MeetingRoomId(id),
        name = name,
        capacity = capacity,
        status = MeetingRoomStatus.valueOf(status),
        location = RoomLocation(
            address = address,
            building = building,
            floor = floor,
            wing = wing,
            roomNumber = roomNumber,
            city = city,
            timeZoneId = timeZone?.let(ZoneId::of)
        ),
        features = RoomFeatures(features = features, attributes = parsed.attributes)
    )
}

fun MeetingRoom.toEntity(objectMapper: ObjectMapper): MeetingRoomEntity {
    val json = objectMapper.writeValueAsString(
        RoomFeaturesJson(
            features = features.features.map { it.name }.toSet(),
            attributes = features.attributes,
        )
    )

    return MeetingRoomEntity(
        id = id.value,
        name = name,
        capacity = capacity,
        status = status.name,
        address = location.address,
        building = location.building,
        floor = location.floor,
        wing = location.wing,
        roomNumber = location.roomNumber,
        city = location.city,
        timeZone = location.timeZoneId?.id,
        featuresJson = JsonbString(json),
    )
}

fun MeetingRoom.toResponseDto(): MeetingRoomResponseDto {
    return MeetingRoomResponseDto(
        id = id.value,
        name = name,
        capacity = capacity,
        location = RoomLocationResponseDto(
            address = location.address,
            building = location.building,
            floor = location.floor,
            wing = location.wing,
            roomNumber = location.roomNumber,
            city = location.city,
            timeZoneId = location.timeZoneId?.id
        ),
        features = features.features.map(RoomFeature::name).toSet(),
        attributes = features.attributes,
        status = MeetingRoomStatusDto.fromEntity(status),
    )
}

fun MeetingRoomBookingEntity.toDomain(roomZoneId: ZoneId) = MeetingRoomBooking(
    id = MeetingRoomBookingId(id),
    roomId = MeetingRoomId(roomId),
    organizerId = UserId(organizerId),
    purpose = purpose,
    status = BookingStatus.valueOf(status),
    timeSlot = TimeSlot(
        start = startTime.atZone(roomZoneId),
        end = endTime.atZone(roomZoneId),
    )
)

fun MeetingRoomBooking.toEntity() = MeetingRoomBookingEntity(
    id = id.value,
    roomId = roomId.value,
    organizerId = organizerId.value,
    purpose = purpose,
    status = status.name,
    startTime = timeSlot.start.toInstant(),
    endTime = timeSlot.end.toInstant(),
)

fun MeetingRoomBooking.toResponseDto() = RoomBookingResponseDto(
    id = id.value,
    roomId = roomId.value,
    organizerId = organizerId.value,
    purpose = purpose,
    status = BookingStatusDto.from(status),
    timeSlot = TimeSlotDto.from(timeSlot),
)
