package ru.itmo.dws.calendar.core.service

import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import ru.itmo.dws.calendar.core.domain.exception.MeetingRoomNotFound
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom.MeetingRoomStatus
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking.BookingStatus
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomFeature
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomFeatures
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomLocation
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomBookingProvider
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomProvider

class MeetingRoomServiceTest {

    private val roomProvider = mockk<MeetingRoomProvider>()
    private val bookingProvider = mockk<MeetingRoomBookingProvider>()

    private val step = Duration.ofMinutes(15)
    private val service = MeetingRoomService(roomProvider, bookingProvider, slotStep = step)

    private val moscow = ZoneId.of("Europe/Moscow")

    private fun room(roomId: MeetingRoomId) = MeetingRoom(
        id = roomId,
        name = "R1",
        capacity = 10,
        location = RoomLocation(timeZoneId = moscow),
        features = RoomFeatures.EMPTY,
        status = MeetingRoomStatus.ACTIVE
    )

    private fun booking(roomId: MeetingRoomId, start: ZonedDateTime, end: ZonedDateTime) =
        MeetingRoomBooking(
            id = MeetingRoomBookingId(UUID.randomUUID()),
            roomId = roomId,
            organizerId = UserId.generate(),
            purpose = "p",
            status = BookingStatus.CONFIRMED,
            timeSlot = TimeSlot(start, end)
        )

    @Test
    fun `findAvailableSlots throws MeetingRoomNotFound when room does not exist`() {
        val roomId = MeetingRoomId(UUID.randomUUID())
        every { roomProvider.findById(roomId) } returns null

        assertThatThrownBy {
            service.findAvailableSlots(roomId, LocalDate.of(2025, 1, 10), Duration.ofHours(1))
        }.isInstanceOf(MeetingRoomNotFound::class.java)
    }

    @Test
    fun `findAvailableSlots returns all day slots when no bookings`() {
        val roomId = MeetingRoomId(UUID.randomUUID())
        every { roomProvider.findById(roomId) } returns room(roomId)
        every { bookingProvider.findBookingsInRange(any(), any(), any()) } returns emptyList()

        val date = LocalDate.of(2025, 1, 10)
        val duration = Duration.ofHours(1)

        val slots = service.findAvailableSlots(roomId, date, duration)

        // 24/7: сутки = 24ч, длительность=1ч, шаг=15м => (24-1)/0.25 + 1 = 93 слота
        assertThat(slots).hasSize(93)

        val dayStart = ZonedDateTime.of(date, java.time.LocalTime.MIDNIGHT, moscow)
        assertThat(slots.first()).isEqualTo(TimeSlot(dayStart, dayStart.plusHours(1)))

        val lastStart = dayStart.plusHours(23) // 23:00 -> 24:00
        assertThat(slots.last()).isEqualTo(TimeSlot(lastStart, lastStart.plusHours(1)))
    }

    @Test
    fun `findAvailableSlots busy interval removes all overlapping 1h slots`() {
        val roomId = MeetingRoomId(UUID.randomUUID())
        every { roomProvider.findById(roomId) } returns room(roomId)

        val date = LocalDate.of(2025, 1, 10)
        val dayStart = ZonedDateTime.of(date, java.time.LocalTime.MIDNIGHT, moscow)

        val busyStart = dayStart.plusHours(10) // 10:00
        val busyEnd = dayStart.plusHours(11) // 11:00

        every { bookingProvider.findBookingsInRange(roomId, any(), any()) } returns listOf(
            booking(roomId, busyStart, busyEnd)
        )

        val slots = service.findAvailableSlots(roomId, date, Duration.ofHours(1))

        // Проверим ключевые слоты вокруг занятости.
        assertThat(slots).contains(TimeSlot(dayStart.plusHours(9), dayStart.plusHours(10))) // 09:00-10:00 OK
        assertThat(slots).doesNotContain(
            TimeSlot(
                dayStart.plusHours(9).plusMinutes(15),
                dayStart.plusHours(10).plusMinutes(15)
            )
        ) // 09:15-10:15 overlaps
        assertThat(slots).doesNotContain(TimeSlot(dayStart.plusHours(10), dayStart.plusHours(11))) // полностью занято
        assertThat(slots).contains(TimeSlot(dayStart.plusHours(11), dayStart.plusHours(12))) // 11:00-12:00 OK

        // Дополнительно: убедимся, что ни один возвращённый слот не пересекает busy.
        val busySlot = TimeSlot(busyStart, busyEnd)
        assertThat(slots.any { it.overlapsWith(busySlot) }).isFalse()
    }

    @Test
    fun `findAvailableSlots booking crossing midnight affects end of day`() {
        val roomId = MeetingRoomId(UUID.randomUUID())
        every { roomProvider.findById(roomId) } returns room(roomId)

        val date = LocalDate.of(2025, 1, 10)
        val dayStart = ZonedDateTime.of(date, java.time.LocalTime.MIDNIGHT, moscow)

        val busyStart = dayStart.plusHours(23).plusMinutes(30) // 23:30
        val busyEnd = dayStart.plusDays(1).plusMinutes(30) // 00:30 next day (пересекает границу суток)

        every { bookingProvider.findBookingsInRange(roomId, any(), any()) } returns listOf(
            booking(roomId, busyStart, busyEnd)
        )

        val slots = service.findAvailableSlots(roomId, date, Duration.ofMinutes(30))

        // 23:00-23:30 должно быть доступно (end == busy.start => не пересекается)
        assertThat(slots).contains(TimeSlot(dayStart.plusHours(23), dayStart.plusHours(23).plusMinutes(30)))

        // 23:15-23:45 пересекается
        assertThat(slots).doesNotContain(
            TimeSlot(
                dayStart.plusHours(23).plusMinutes(15),
                dayStart.plusHours(23).plusMinutes(45)
            )
        )

        // 23:30-00:00 пересекается
        assertThat(slots).doesNotContain(TimeSlot(dayStart.plusHours(23).plusMinutes(30), dayStart.plusDays(1)))
    }

    @Test
    fun `findAvailableRooms returns only free rooms when some are busy`() {
        // Подготовка
        val room1 = room(MeetingRoomId(UUID.randomUUID()))
        val room2 = room(MeetingRoomId(UUID.randomUUID()))
        val room3 = room(MeetingRoomId(UUID.randomUUID()))

        val timeSlot = TimeSlot(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1))

        // Подготовка: 2 комнаты активны, одна занята
        every { roomProvider.findAllByCriteria(any()) } returns listOf(room1, room2, room3)
        every { bookingProvider.findBusyRoomIds(setOf(room1.id, room2.id, room3.id), timeSlot) } returns setOf(room2.id)

        // Вызов
        val result = service.findAvailableRooms(timeSlot, null)

        // Проверка
        assertThat(result).containsExactlyInAnyOrder(room1, room3)
    }

    @Test
    fun `findAvailableRooms returns empty list when all rooms are busy`() {
        val room1 = room(MeetingRoomId(UUID.randomUUID()))
        val room2 = room(MeetingRoomId(UUID.randomUUID()))

        val timeSlot = TimeSlot(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1))

        every { roomProvider.findAllByCriteria(any()) } returns listOf(room1, room2)
        every { bookingProvider.findBusyRoomIds(setOf(room1.id, room2.id), timeSlot) } returns setOf(room1.id, room2.id)

        val result = service.findAvailableRooms(timeSlot, null)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findAvailableRooms returns all rooms when none are busy`() {
        val room1 = room(MeetingRoomId(UUID.randomUUID()))
        val room2 = room(MeetingRoomId(UUID.randomUUID()))

        val timeSlot = TimeSlot(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1))

        every { roomProvider.findAllByCriteria(any()) } returns listOf(room1, room2)
        every { bookingProvider.findBusyRoomIds(setOf(room1.id, room2.id), timeSlot) } returns emptySet()

        val result = service.findAvailableRooms(timeSlot, null)

        assertThat(result).containsExactlyInAnyOrder(room1, room2)
    }

    @Test
    fun `findAvailableRooms filters by_criteria_when_criteria_provided`() {
        val criteria = MeetingRoomSearchCriteria(
            minCapacity = 10,
            requiredFeatures = setOf(RoomFeature.PROJECTOR, RoomFeature.PROJECTOR),
            status = MeetingRoomStatus.ACTIVE
        )

        val room1 = room(MeetingRoomId(UUID.randomUUID())).copy(
            capacity = 10,
            features = RoomFeatures(setOf(RoomFeature.PROJECTOR, RoomFeature.PROJECTOR))
        )
        val room2 = room(MeetingRoomId(UUID.randomUUID())).copy(
            capacity = 8,
            features = RoomFeatures(setOf(RoomFeature.PROJECTOR))
        )

        val timeSlot = TimeSlot(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1))

        every { roomProvider.findAllByCriteria(criteria) } returns listOf(room1, room2)
        every { bookingProvider.findBusyRoomIds(setOf(room1.id, room2.id), timeSlot) } returns emptySet()

        val result = service.findAvailableRooms(timeSlot, criteria)
        assertThat(result).containsExactlyInAnyOrder(room1, room2)
    }

    @Test
    fun `findAvailableRooms filters only active rooms when no criteria`() {
        val room1 = room(MeetingRoomId(UUID.randomUUID())).copy(status = MeetingRoomStatus.ACTIVE)

        val timeSlot = TimeSlot(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1))

        every { roomProvider.findAllByCriteria(MeetingRoomSearchCriteria(status = MeetingRoomStatus.ACTIVE)) } returns listOf(
            room1
        )
        every { bookingProvider.findBusyRoomIds(setOf(room1.id), timeSlot) } returns emptySet()

        val result = service.findAvailableRooms(timeSlot, null)

        assertThat(result).containsExactly(room1)
    }
}
