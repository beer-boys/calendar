package ru.itmo.dws.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CalendarApplicationTests {

    @Test
    fun `context loads`() {
        assertEquals(1, 1)
    }
}
