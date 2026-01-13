package ru.itmo.dws.calendar.core.service.provider

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.itmo.dws.calendar.core.domain.exception.TimeSlotNotAvailable
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom
import ru.itmo.dws.calendar.core.domain.model.MeetingRoomBooking
import ru.itmo.dws.calendar.core.domain.valueobject.TimeSlot
import ru.itmo.dws.calendar.core.domain.valueobject.UserId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomBookingId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomFeatures
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomLocation
import ru.itmo.dws.calendar.core.port.output.room.MeetingRoomProvider
import ru.itmo.dws.calendar.integration.AbstractIntegrationTest
import ru.itmo.dws.calendar.model.MeetingRoomEntity
import ru.itmo.dws.calendar.model.types.JsonbString
import ru.itmo.dws.calendar.repository.MeetingRoomBookingRepository
import ru.itmo.dws.calendar.repository.MeetingRoomRepository

class DatabaseMeetingRoomBookingProviderTest : AbstractIntegrationTest() {

    @MockkBean
    lateinit var meetingRoomProvider: MeetingRoomProvider

    @Autowired
    private lateinit var roomRepository: MeetingRoomRepository

    @Autowired
    lateinit var bookingRepository: MeetingRoomBookingRepository

    @Autowired
    lateinit var provider: DatabaseMeetingRoomBookingProvider

    private val utc: ZoneId = ZoneId.of("UTC")
    private val moscow: ZoneId = ZoneId.of("Europe/Moscow")

    @BeforeEach
    fun setup() {
        bookingRepository.deleteAll()
        roomRepository.deleteAll()
    }

    @AfterEach
    fun down() {
        clearAllMocks()
    }

    private fun createRoom(roomId: UUID, zone: String = "Europe/Moscow") {
        roomRepository.insert(
            MeetingRoomEntity(
                id = roomId,
                name = "Room-$roomId",
                capacity = 10,
                status = "ACTIVE",
                address = null, building = null, floor = null, wing = null,
                roomNumber = null, city = null,
                timeZone = zone,
                featuresJson = JsonbString("""{"features":[],"attributes":{}}""")
            )
        )
    }

    private fun mockRoom(roomId: UUID, zone: ZoneId): MeetingRoom = MeetingRoom(
        id = MeetingRoomId(roomId),
        name = "Room-$roomId",
        capacity = 10,
        status = MeetingRoom.MeetingRoomStatus.ACTIVE,
        location = RoomLocation(timeZoneId = zone),
        features = RoomFeatures.EMPTY
    )

    @Test
    fun `create inserts booking`() {
        val roomId = UUID.randomUUID()
        createRoom(roomId)

        every { meetingRoomProvider.findById(MeetingRoomId(roomId)) } returns mockRoom(roomId, moscow)

        val booking = MeetingRoomBooking(
            id = MeetingRoomBookingId(UUID.randomUUID()),
            roomId = MeetingRoomId(roomId),
            organizerId = UserId.generate(),
            purpose = "Sync",
            status = MeetingRoomBooking.BookingStatus.CONFIRMED,
            timeSlot = TimeSlot(
                start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, moscow),
                end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow)
            )
        )

        val created = provider.create(booking)

        assertThat(created).isEqualTo(booking)
        val inDb = bookingRepository.findById(booking.id.value).orElse(null)
        assertThat(inDb).isNotNull
        assertThat(inDb!!.status).isEqualTo("CONFIRMED")
        assertThat(inDb.roomId).isEqualTo(roomId)
    }

    @Test
    fun `create throws TimeSlotNotAvailable on overlap (exclusion constraint)`() {
        val roomId = UUID.randomUUID()
        createRoom(roomId)
        every { meetingRoomProvider.findById(MeetingRoomId(roomId)) } returns mockRoom(roomId, moscow)

        val slot1 = TimeSlot(
            start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, moscow),
            end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow),
        )
        val slotOverlap = TimeSlot(
            start = ZonedDateTime.of(2025, 1, 10, 10, 30, 0, 0, moscow),
            end = ZonedDateTime.of(2025, 1, 10, 11, 30, 0, 0, moscow),
        )

        provider.create(
            MeetingRoomBooking(
                id = MeetingRoomBookingId(UUID.randomUUID()),
                roomId = MeetingRoomId(roomId),
                organizerId = UserId.generate(),
                purpose = "A",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = slot1
            )
        )

        val booking2 = MeetingRoomBooking(
            id = MeetingRoomBookingId(UUID.randomUUID()),
            roomId = MeetingRoomId(roomId),
            organizerId = UserId.generate(),
            purpose = "B",
            status = MeetingRoomBooking.BookingStatus.CONFIRMED,
            timeSlot = slotOverlap
        )

        assertThatThrownBy { provider.create(booking2) }.isInstanceOf(TimeSlotNotAvailable::class.java)
    }

    @Test
    fun `create allows booking starting exactly at previous end (semi-open interval)`() {
        val roomId = UUID.randomUUID()
        createRoom(roomId)
        every { meetingRoomProvider.findById(MeetingRoomId(roomId)) } returns mockRoom(roomId, moscow)

        val slot1 = TimeSlot(
            start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, moscow),
            end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow),
        )
        val slot2 = TimeSlot(
            start = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow),
            end = ZonedDateTime.of(2025, 1, 10, 12, 0, 0, 0, moscow),
        )

        provider.create(
            MeetingRoomBooking(
                id = MeetingRoomBookingId(UUID.randomUUID()),
                roomId = MeetingRoomId(roomId),
                organizerId = UserId.generate(),
                purpose = "A",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = slot1
            )
        )

        assertThatNoException().isThrownBy {
            provider.create(
                MeetingRoomBooking(
                    id = MeetingRoomBookingId(UUID.randomUUID()),
                    roomId = MeetingRoomId(roomId),
                    organizerId = UserId.generate(),
                    purpose = "B",
                    status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                    timeSlot = slot2
                )
            )
        }
    }

    @Test
    fun `findById returns null when booking not found`() {
        val result = provider.findById(MeetingRoomBookingId(UUID.randomUUID()))
        assertThat(result).isNull()
    }

    @Test
    fun `findById returns null when room not found`() {
        val roomId = UUID.randomUUID()
        createRoom(roomId)

        // room provider returns null -> provider.findById must return null
        every { meetingRoomProvider.findById(MeetingRoomId(roomId)) } returns null

        val bookingId = MeetingRoomBookingId(UUID.randomUUID())
        provider.create(
            MeetingRoomBooking(
                id = bookingId,
                roomId = MeetingRoomId(roomId),
                organizerId = UserId.generate(),
                purpose = "X",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = TimeSlot(
                    start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, utc),
                    end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, utc),
                )
            )
        )

        val loaded = provider.findById(bookingId)
        assertThat(loaded).isNull()
    }

    @Test
    fun `findById maps time slot to room timezone`() {
        val roomId = UUID.randomUUID()
        createRoom(roomId, zone = "Europe/Moscow")
        every { meetingRoomProvider.findById(MeetingRoomId(roomId)) } returns mockRoom(roomId, moscow)

        val startMsk = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, moscow)
        val endMsk = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow)

        val bookingId = MeetingRoomBookingId(UUID.randomUUID())
        provider.create(
            MeetingRoomBooking(
                id = bookingId,
                roomId = MeetingRoomId(roomId),
                organizerId = UserId.generate(),
                purpose = "TZ check",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = TimeSlot(startMsk, endMsk)
            )
        )

        val loaded = provider.findById(bookingId)!!
        assertThat(loaded.timeSlot.start.zone).isEqualTo(moscow)
        assertThat(loaded.timeSlot.end.zone).isEqualTo(moscow)
        assertThat(loaded.timeSlot.start.toInstant()).isEqualTo(startMsk.toInstant())
        assertThat(loaded.timeSlot.end.toInstant()).isEqualTo(endMsk.toInstant())
    }

    @Test
    fun `findBookingsInRange returns empty when room not found`() {
        val roomId = MeetingRoomId(UUID.randomUUID())
        every { meetingRoomProvider.findById(roomId) } returns null

        val res = provider.findBookingsInRange(
            roomId = roomId,
            fromInclusive = ZonedDateTime.now(utc).minusDays(1),
            toExclusive = ZonedDateTime.now(utc).plusDays(1)
        )

        assertThat(res).isEmpty()
    }

    @Test
    fun `findBookingsInRange returns only confirmed overlapping bookings`() {
        val roomId = UUID.randomUUID()
        createRoom(roomId)
        every { meetingRoomProvider.findById(MeetingRoomId(roomId)) } returns mockRoom(roomId, moscow)

        val insideId = MeetingRoomBookingId(UUID.randomUUID())

        provider.create(
            MeetingRoomBooking(
                id = insideId,
                roomId = MeetingRoomId(roomId),
                organizerId = UserId.generate(),
                purpose = "inside",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = TimeSlot(
                    start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, moscow),
                    end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow),
                )
            )
        )
        // booking outside query range
        provider.create(
            MeetingRoomBooking(
                id = MeetingRoomBookingId(UUID.randomUUID()),
                roomId = MeetingRoomId(roomId),
                organizerId = UserId.generate(),
                purpose = "outside",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = TimeSlot(
                    start = ZonedDateTime.of(2025, 1, 10, 12, 0, 0, 0, moscow),
                    end = ZonedDateTime.of(2025, 1, 10, 13, 0, 0, 0, moscow),
                )
            )
        )

        val from = ZonedDateTime.of(2025, 1, 10, 9, 30, 0, 0, moscow)
        val to = ZonedDateTime.of(2025, 1, 10, 11, 30, 0, 0, moscow)

        val found = provider.findBookingsInRange(MeetingRoomId(roomId), from, to)

        assertThat(found).hasSize(1)
        assertThat(found.single().id).isEqualTo(insideId)
        assertThat(found.single().timeSlot.start.zone).isEqualTo(moscow)
    }

    @Test
    fun `findBookingsInRange does not include canceled bookings`() {
        val roomId = UUID.randomUUID()
        createRoom(roomId)
        every { meetingRoomProvider.findById(MeetingRoomId(roomId)) } returns mockRoom(roomId, moscow)

        val bookingId = MeetingRoomBookingId(UUID.randomUUID())
        provider.create(
            MeetingRoomBooking(
                id = bookingId,
                roomId = MeetingRoomId(roomId),
                organizerId = UserId.generate(),
                purpose = "will cancel",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = TimeSlot(
                    start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, moscow),
                    end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow),
                )
            )
        )

        provider.cancel(bookingId)

        val from = ZonedDateTime.of(2025, 1, 10, 9, 0, 0, 0, moscow)
        val to = ZonedDateTime.of(2025, 1, 10, 12, 0, 0, 0, moscow)

        val found = provider.findBookingsInRange(MeetingRoomId(roomId), from, to)
        assertThat(found).isEmpty()
    }

    @Test
    fun `findBusyRoomIds returns empty for empty input`() {
        val slot = TimeSlot(
            start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, utc),
            end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, utc),
        )
        assertThat(provider.findBusyRoomIds(emptySet(), slot)).isEmpty()
    }

    @Test
    fun `findBusyRoomIds returns ids of rooms with overlapping confirmed bookings`() {
        val room1 = UUID.randomUUID()
        val room2 = UUID.randomUUID()

        createRoom(room1)
        createRoom(room2)

        // findBusyRoomIds не трогает meetingRoomProvider, можно не мокать

        provider.create(
            MeetingRoomBooking(
                id = MeetingRoomBookingId(UUID.randomUUID()),
                roomId = MeetingRoomId(room1),
                organizerId = UserId.generate(),
                purpose = "busy",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = TimeSlot(
                    start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, moscow),
                    end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow),
                )
            )
        )

        val querySlot = TimeSlot(
            start = ZonedDateTime.of(2025, 1, 10, 10, 30, 0, 0, moscow),
            end = ZonedDateTime.of(2025, 1, 10, 10, 45, 0, 0, moscow),
        )

        val busy = provider.findBusyRoomIds(
            roomIds = setOf(MeetingRoomId(room1), MeetingRoomId(room2)),
            timeSlot = querySlot
        )

        assertThat(busy).containsExactly(MeetingRoomId(room1))
    }

    @Test
    fun `cancel does nothing when booking not found`() {
        assertThatNoException().isThrownBy {
            provider.cancel(MeetingRoomBookingId(UUID.randomUUID()))
        }
    }

    @Test
    fun `cancel marks booking as CANCELED and frees slot for new booking`() {
        val roomId = UUID.randomUUID()
        createRoom(roomId)
        every { meetingRoomProvider.findById(MeetingRoomId(roomId)) } returns mockRoom(roomId, moscow)

        val slot = TimeSlot(
            start = ZonedDateTime.of(2025, 1, 10, 10, 0, 0, 0, moscow),
            end = ZonedDateTime.of(2025, 1, 10, 11, 0, 0, 0, moscow),
        )

        val bookingId = MeetingRoomBookingId(UUID.randomUUID())
        provider.create(
            MeetingRoomBooking(
                id = bookingId,
                roomId = MeetingRoomId(roomId),
                organizerId = UserId.generate(),
                purpose = "will cancel",
                status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                timeSlot = slot
            )
        )

        provider.cancel(bookingId)

        val updated = bookingRepository.findById(bookingId.value).orElseThrow()
        assertThat(updated.status).isEqualTo("CANCELED")

        assertThatNoException().isThrownBy {
            provider.create(
                MeetingRoomBooking(
                    id = MeetingRoomBookingId(UUID.randomUUID()),
                    roomId = MeetingRoomId(roomId),
                    organizerId = UserId.generate(),
                    purpose = "new",
                    status = MeetingRoomBooking.BookingStatus.CONFIRMED,
                    timeSlot = slot
                )
            )
        }
    }
}
