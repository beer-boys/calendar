package ru.itmo.dws.calendar.core.service.provider

import io.mockk.clearAllMocks
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.itmo.dws.calendar.core.domain.model.MeetingRoom.MeetingRoomStatus
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomId
import ru.itmo.dws.calendar.core.domain.valueobject.room.MeetingRoomSearchCriteria
import ru.itmo.dws.calendar.core.domain.valueobject.room.RoomFeature
import ru.itmo.dws.calendar.integration.AbstractIntegrationTest
import ru.itmo.dws.calendar.model.MeetingRoomEntity
import ru.itmo.dws.calendar.model.types.JsonbString
import ru.itmo.dws.calendar.repository.MeetingRoomRepository

class DatabaseMeetingRoomProviderTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var repository: MeetingRoomRepository

    @Autowired
    private lateinit var provider: DatabaseMeetingRoomProvider

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @AfterEach
    fun down() {
        clearAllMocks()
    }

    @Test
    fun `findById returns room with parsed features and location`() {
        val roomId = UUID.randomUUID()

        repository.insert(
            MeetingRoomEntity(
                id = roomId,
                name = "Omega",
                capacity = 10,
                status = "ACTIVE",
                address = "Nevsky 1",
                building = "A",
                floor = 3,
                wing = "West",
                roomNumber = "3-12",
                city = "SPB",
                timeZone = "Europe/Moscow",
                featuresJson = JsonbString(
                    """{"features":["PROJECTOR","WHITEBOARD"],"attributes":{"tvInches":"75"}}"""
                ),
            )
        )

        val room = provider.findById(MeetingRoomId(roomId))

        assertThat(room).isNotNull
        assertThat(room!!.name).isEqualTo("Omega")
        assertThat(room.capacity).isEqualTo(10)
        assertThat(room.status).isEqualTo(MeetingRoomStatus.ACTIVE)
        assertThat(room.location.floor).isEqualTo(3)
        assertThat(room.location.timeZoneId!!.id).isEqualTo("Europe/Moscow")
        assertThat(room.features.features).contains(RoomFeature.PROJECTOR, RoomFeature.WHITEBOARD)
        assertThat(room.features.attributes).containsEntry("tvInches", "75")
    }

    @Test
    fun `findAllByCriteria filters by minCapacity and locationQuery`() {
        val r1 = UUID.randomUUID()
        val r2 = UUID.randomUUID()

        repository.insert(
            MeetingRoomEntity(
                id = r1, name = "Small", capacity = 4, status = "ACTIVE",
                address = "Lenina 10", building = "B", floor = 2, wing = null,
                roomNumber = "2-01", city = "SPB", timeZone = "Europe/Moscow",
                featuresJson = JsonbString("""{"features":["WHITEBOARD"],"attributes":{}}""")
            )
        )
        repository.insert(
            MeetingRoomEntity(
                id = r2, name = "Big Nevsky", capacity = 12, status = "ACTIVE",
                address = "Nevsky 1", building = "A", floor = 3, wing = null,
                roomNumber = "3-02", city = "SPB", timeZone = "Europe/Moscow",
                featuresJson = JsonbString("""{"features":["PROJECTOR"],"attributes":{}}""")
            )
        )

        val found = provider.findAllByCriteria(
            MeetingRoomSearchCriteria(
                minCapacity = 10,
                locationQuery = "Nevsky"
            )
        )

        assertThat(found.map { it.id.value }).containsExactly(r2)
    }

    @Test
    fun `findAllByCriteria filters by requiredFeatures`() {
        val r1 = UUID.randomUUID()
        val r2 = UUID.randomUUID()

        repository.insert(
            MeetingRoomEntity(
                id = r1, name = "Proj only", capacity = 8, status = "ACTIVE",
                address = null, building = "A", floor = 1, wing = null,
                roomNumber = null, city = null, timeZone = "UTC",
                featuresJson = JsonbString("""{"features":["PROJECTOR"],"attributes":{}}""")
            )
        )
        repository.insert(
            MeetingRoomEntity(
                id = r2, name = "Proj+VC", capacity = 8, status = "ACTIVE",
                address = null, building = "A", floor = 1, wing = null,
                roomNumber = null, city = null, timeZone = "UTC",
                featuresJson = JsonbString("""{"features":["PROJECTOR","VIDEO_CONFERENCING"],"attributes":{}}""")
            )
        )

        val found = provider.findAllByCriteria(
            MeetingRoomSearchCriteria(requiredFeatures = setOf(RoomFeature.PROJECTOR, RoomFeature.VIDEO_CONFERENCING))
        )

        assertThat(found.map { it.id.value }).containsExactly(r2)
    }
}
