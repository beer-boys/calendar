package ru.itmo.dws.calendar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import ru.itmo.dws.calendar.integration.AbstractIntegrationTest

@SpringBootTest
class CalendarApplicationTest : AbstractIntegrationTest() {

    @Test
    fun contextLoads() {
        assertThat(1).isEqualTo(1)
    }
}
