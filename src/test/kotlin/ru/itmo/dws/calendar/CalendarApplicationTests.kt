package ru.itmo.dws.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import ru.itmo.dws.calendar.integration.AbstractIntegrationTest

@SpringBootTest
class CalendarApplicationTests : AbstractIntegrationTest() {

    @Test
    fun `context loads`() {
        assertEquals(1, 1)
    }
}
